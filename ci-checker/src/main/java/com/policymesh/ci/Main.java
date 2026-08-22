package com.policymesh.ci;

import com.policymesh.ci.backend.BackendAccessException;
import com.policymesh.ci.backend.PolicyMeshBackendClient;
import com.policymesh.ci.config.CheckerConfig;
import com.policymesh.ci.engine.ComplianceEngine;
import com.policymesh.ci.model.*;
import com.policymesh.ci.output.ConsoleReporter;
import com.policymesh.ci.output.GithubReporter;
import com.policymesh.ci.output.JsonReporter;
import com.policymesh.ci.parser.DataFlowParser;
import com.policymesh.ci.parser.PolicyParseException;
import com.policymesh.ci.parser.PolicyParser;
import com.policymesh.ci.parser.ServiceParser;

import java.util.List;

/**
 * PolicyMesh CI Checker — command-line entry point.
 *
 * Usage:
 *   java -jar policymesh-ci.jar check [options]
 *
 * Options:
 *   --policy-dir DIR    Directory containing policy YAML files
 *   --services FILE     Path to services JSON file
 *   --dataflows FILE    Path to data flows JSON file
 *   --output FORMAT     Output format: console (default), json
 *   --backend-url URL   Backend URL for backend-assisted mode
 *   --token TOKEN       API token for backend authentication
 *   --no-color          Disable ANSI color output
 *   --strict            Enable strict mode (warnings become errors)
 *
 * Exit codes:
 *   0 = Compliance passed
 *   1 = Compliance violations found
 *   2 = Configuration/input error
 *   3 = Backend integration error
 *   4 = Unexpected internal error
 */
public class Main {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(4);
        }
    }

    public static int run(String[] args) {
        // Parse CLI arguments
        CheckerConfig config = parseArgs(args);

        if (config == null) {
            printUsage();
            return 2;
        }

        // Handle version/help
        if (config.getOutputFormat().equals("help")) {
            printUsage();
            return 0;
        }

        // Route to "check" subcommand (for now, the only command)
        try {
            return executeCheck(config);
        } catch (Exception e) {
            System.err.println("Internal error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 4;
        }
    }

    private static int executeCheck(CheckerConfig config) throws Exception {
        ConsoleReporter console = new ConsoleReporter(config.isNoColor());
        JsonReporter jsonReporter = new JsonReporter();
        GithubReporter githubReporter = new GithubReporter();
        boolean isJsonOutput = "json".equals(config.getOutputFormat());
        boolean isGithubOutput = "github".equals(config.getOutputFormat());

        try {
            // Load data based on mode
            List<Policy> policies;
            List<ServiceNode> services;
            List<DataFlowEdge> edges;

            if (config.hasBackendUrl()) {
                // Backend-assisted mode
                if (!isJsonOutput) {
                    System.out.println("Connecting to PolicyMesh backend: " + config.getBackendUrl());
                } else {
                    jsonReporter.logToStderr("Connecting to backend: " + config.getBackendUrl());
                }

                PolicyMeshBackendClient client = new PolicyMeshBackendClient(
                        config.getBackendUrl(), config.getApiToken());

                policies = client.fetchPolicies();
                services = client.fetchServices();
                edges = client.fetchDataFlows();

            } else {
                // Offline mode (default)
                PolicyParser policyParser = new PolicyParser();
                policies = policyParser.parseDirectoryRecursive(config.getPolicyDir());

                ServiceParser serviceParser = new ServiceParser();
                services = serviceParser.parseFile(config.getServicesFile());

                DataFlowParser dataFlowParser = new DataFlowParser();
                edges = dataFlowParser.parseFile(config.getDataflowsFile());
            }

            // Log loading summary for JSON mode (console mode prints it in printConsoleResult)
            if (isJsonOutput) {
                jsonReporter.logToStderr("Policies loaded: " + policies.size());
                jsonReporter.logToStderr("Services loaded: " + services.size());
                jsonReporter.logToStderr("Data flows loaded: " + edges.size());
            }

            // Run compliance engine
            ComplianceEngine engine = new ComplianceEngine();
            ComplianceResult result = engine.check(policies, services, edges);

            // Report results
            switch (config.getOutputFormat()) {
                case "json" -> jsonReporter.report(result);
                case "github" -> githubReporter.report(result);
                default -> printConsoleResult(result, policies, services, edges, console);
            }

            return result.getExitCode();

        } catch (PolicyParseException e) {
            String error = "Configuration error: " + e.getMessage();
            if (isJsonOutput) {
                jsonReporter.logToStderr(error);
                jsonReporter.report(ComplianceResult.error(error));
            } else {
                System.err.println(error);
            }
            return 2;

        } catch (BackendAccessException e) {
            String error = "Backend error: " + e.getMessage();
            if (isJsonOutput) {
                jsonReporter.logToStderr(error);
                jsonReporter.report(ComplianceResult.error(error));
            } else {
                System.err.println(error);
            }
            return 3;

        } catch (Exception e) {
            String error = "Unexpected error: " + e.getMessage();
            if (isJsonOutput) {
                jsonReporter.logToStderr(error);
                jsonReporter.report(ComplianceResult.error(error));
            } else {
                System.err.println(error);
                e.printStackTrace(System.err);
            }
            return 4;
        }
    }

    private static void printConsoleResult(ComplianceResult result,
                                           List<Policy> policies,
                                           List<ServiceNode> services,
                                           List<DataFlowEdge> edges,
                                           ConsoleReporter console) {
        // Re-print loading summary since we consumed it earlier
        console.report(null, policies.size(), services.size(), edges.size());

        // Print each flow
        for (DataFlowEdge edge : edges) {
            // Find violation for this edge
            ComplianceViolation violation = result.getViolations().stream()
                    .filter(v -> v.getSourceService().equals(edge.getSource())
                            && v.getDestinationService().equals(edge.getDestination()))
                    .findFirst()
                    .orElse(null);

            String dataClasses = String.join(", ", edge.getDataClasses());

            if (violation != null) {
                console.reportFail(
                        edge.getSource(), violation.getSourceRegion(),
                        edge.getDestination(), violation.getDestinationRegion(),
                        dataClasses, violation);
            } else {
                // Resolve regions for pass output
                String srcRegion = resolveRegion(edge.getSource(), services);
                String dstRegion = resolveRegion(edge.getDestination(), services);
                console.reportPass(edge.getSource(), srcRegion,
                        edge.getDestination(), dstRegion, dataClasses);
            }
        }

        console.reportSummary(result);
    }

    private static String resolveRegion(String serviceId, List<ServiceNode> services) {
        for (ServiceNode s : services) {
            if (s.getId().equals(serviceId)) {
                return s.getRegion();
            }
        }
        return "?";
    }

    /**
     * Parses command-line arguments into a CheckerConfig.
     * Returns null if arguments are invalid or help is requested.
     */
    private static CheckerConfig parseArgs(String[] args) {
        if (args.length == 0) {
            return null;
        }

        CheckerConfig config = new CheckerConfig();
        int i = 0;

        // First arg might be "check" subcommand
        if ("check".equals(args[i])) {
            i++;
        } else if ("--help".equals(args[i]) || "-h".equals(args[i]) || "help".equals(args[i])) {
            return null; // triggers help
        } else if ("--version".equals(args[i]) || "-v".equals(args[i])) {
            System.out.println("PolicyMesh CI Checker v" + VERSION);
            config.setOutputFormat("help");
            return config;
        } else {
            return null; // unknown subcommand
        }

        while (i < args.length) {
            String arg = args[i];
            switch (arg) {
                case "--policy-dir":
                    if (i + 1 < args.length) {
                        config.setPolicyDir(java.nio.file.Paths.get(args[++i]));
                    } else {
                        System.err.println("--policy-dir requires a value");
                        return null;
                    }
                    break;
                case "--services":
                    if (i + 1 < args.length) {
                        config.setServicesFile(java.nio.file.Paths.get(args[++i]));
                    } else {
                        System.err.println("--services requires a value");
                        return null;
                    }
                    break;
                case "--dataflows":
                    if (i + 1 < args.length) {
                        config.setDataflowsFile(java.nio.file.Paths.get(args[++i]));
                    } else {
                        System.err.println("--dataflows requires a value");
                        return null;
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        config.setOutputFormat(args[++i]);
                    } else {
                        System.err.println("--output requires a value (console, json, github)");
                        return null;
                    }
                    break;
                case "--backend-url":
                    if (i + 1 < args.length) {
                        config.setBackendUrl(args[++i]);
                    } else {
                        System.err.println("--backend-url requires a value");
                        return null;
                    }
                    break;
                case "--token":
                    if (i + 1 < args.length) {
                        config.setApiToken(args[++i]);
                    } else {
                        System.err.println("--token requires a value");
                        return null;
                    }
                    break;
                case "--no-color":
                    config.setNoColor(true);
                    break;
                case "--strict":
                    config.setStrictMode(true);
                    break;
                case "--help":
                case "-h":
                    return null;
                default:
                    System.err.println("Unknown option: " + arg);
                    return null;
            }
            i++;
        }

        return config;
    }

    private static void printUsage() {
        System.out.println("""
                PolicyMesh CI Checker v%s
                ========================

                USAGE:
                  java -jar policymesh-ci.jar check [options]

                OPTIONS:
                  --policy-dir DIR    Directory with policy YAML files (default: ./policies)
                  --services FILE     Path to services JSON file (default: ./examples/services.json)
                  --dataflows FILE    Path to data flows JSON file (default: ./examples/dataflows.json)
                  --output FORMAT     Output format: console, json, github (default: console)
                  --backend-url URL   Backend URL for backend-assisted mode
                  --token TOKEN       API token for backend authentication
                  --no-color          Disable ANSI color output
                  --strict            Enable strict mode (warnings become errors)
                  --help, -h          Show this help message
                  --version, -v       Show version

                EXIT CODES:
                  0  Compliance passed
                  1  Compliance violations found
                  2  Configuration/input error
                  3  Backend integration error
                  4  Unexpected internal error

                EXAMPLES:
                  java -jar policymesh-ci.jar check \\
                    --policy-dir ./policies \\
                    --services ./examples/services.json \\
                    --dataflows ./examples/dataflows-valid.json

                  java -jar policymesh-ci.jar check \\
                    --policy-dir ./policies \\
                    --services ./examples/services.json \\
                    --dataflows ./examples/dataflows-invalid.json \\
                    --output json

                  java -jar policymesh-ci.jar check \\
                    --backend-url https://api.example.com \\
                    --token <token>
                """.formatted(VERSION));
    }
}
