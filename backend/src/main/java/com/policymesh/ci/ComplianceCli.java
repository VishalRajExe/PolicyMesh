package com.policymesh.ci;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.policymesh.common.ApiException;
import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.compiler.PolicyCompiler;
import com.policymesh.policy.PolicyRuleEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standalone, database-free compliance check:
 *
 *   java -jar policy-mesh-backend.jar check \
 *     --policies policies/EU --policies policies/GLOBAL \
 *     --services examples/services/services.json \
 *     --dataflows examples/dataflows/valid-flow.json \
 *     --report compliance-report.json
 *
 * Uses the exact same compiler and rule evaluator as the running service, so the gate in
 * GitHub Actions and runtime enforcement can never diverge.
 *
 * Exit codes: 0 = PASS, 1 = violations found, 2 = configuration/input error.
 */
public final class ComplianceCli {
  private ComplianceCli() {}

  private record CliViolation(String source, String destination, String sourceRegion, String destinationRegion,
                              String dataClass, String policyCode, String reason) {}

  public static boolean isFilesMode(String[] args) {
    return hasOption(args, "--services") || hasOption(args, "--dataflows") || hasOption(args, "--policies");
  }

  public static int runFiles(String[] args) {
    try {
      List<Path> policyDirs = optionValues(args, "--policies").stream().map(Path::of).toList();
      String servicesFile = optionValue(args, "--services");
      String dataflowsFile = optionValue(args, "--dataflows");
      String reportFile = optionValue(args, "--report");
      boolean json = "json".equals(optionValue(args, "--output"));

      if (policyDirs.isEmpty() || servicesFile == null || dataflowsFile == null) {
        System.err.println("Usage: check --policies <dir> [--policies <dir>...] --services <services.json> --dataflows <flows.json> [--report <file>] [--output text|json]");
        return 2;
      }

      List<CompiledPolicy> policies = new ArrayList<>();
      for (Path dir : policyDirs) {
        if (!Files.isDirectory(dir)) {
          System.err.println("ERROR: policy directory not found: " + dir);
          return 2;
        }
        try (Stream<Path> files = Files.walk(dir)) {
          files.filter(f -> {
            String name = f.getFileName().toString().toLowerCase();
            return name.endsWith(".yaml") || name.endsWith(".yml");
          }).sorted().forEach(f -> policies.add(readPolicy(f)));
        }
      }

      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> serviceRegions = readServices(mapper, Path.of(servicesFile));
      List<Map<String, Object>> flows = readDataflows(mapper, Path.of(dataflowsFile));

      List<CliViolation> violations = new ArrayList<>();
      int checked = 0;
      for (Map<String, Object> flow : flows) {
        String source = String.valueOf(flow.get("source"));
        String destination = String.valueOf(flow.get("destination"));
        String sourceRegion = regionOf(serviceRegions, source);
        String destinationRegion = regionOf(serviceRegions, destination);
        for (String dataClass : dataClassesOf(flow)) {
          checked++;
          var evaluation = PolicyRuleEvaluator.evaluate(
              PolicyRuleEvaluator.applicable(policies, sourceRegion), sourceRegion, destinationRegion, dataClass);
          if (evaluation.decision().name().equals("DENY") || evaluation.decision().name().equals("REROUTE")) {
            violations.add(new CliViolation(source, destination, sourceRegion, destinationRegion,
                dataClass, evaluation.policyId(), evaluation.reason()));
          }
        }
      }

      String result = violations.isEmpty() ? "PASS" : "FAIL";
      ObjectNode report = buildReport(mapper, result, checked, violations);
      String reportJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

      if (json) {
        System.out.println(reportJson);
      } else {
        if (violations.isEmpty()) {
          System.out.println("========================================");
          System.out.println("PolicyMesh Compliance Check");
          System.out.println("========================================\n");
          System.out.println("Result: PASSED\n");
          System.out.println("Flows checked: " + checked);
          System.out.println("Passed: " + checked);
          System.out.println("Failed: 0\n");
          System.out.println("✅ PolicyMesh Compliance Passed");
          System.out.println("========================================\n");
        } else {
          for (CliViolation v : violations) {
            System.out.println("::error title=PolicyMesh Compliance::" + (v.policyCode() != null ? v.policyCode() : "POLICY-VIOLATION") + " violation: "
                + v.source() + " (" + v.sourceRegion() + ") → " + v.destination() + " (" + v.destinationRegion() + ") carrying " + v.dataClass());
          }
          System.out.println("========================================");
          System.out.println("PolicyMesh Compliance Check");
          System.out.println("========================================\n");
          System.out.println("Result: FAILED\n");
          System.out.println("Violations: " + violations.size() + "\n");
          for (int i = 0; i < violations.size(); i++) {
            CliViolation v = violations.get(i);
            System.out.println("[" + (i + 1) + "] " + (v.policyCode() != null ? v.policyCode() : "DEFAULT_DENY"));
            System.out.println("Source: " + v.source() + " (" + v.sourceRegion() + ")");
            System.out.println("Destination: " + v.destination() + " (" + v.destinationRegion() + ")");
            System.out.println("Data Class: " + v.dataClass() + "\n");
            System.out.println("Reason:\n" + v.reason() + "\n");
            System.out.println("Remediation:");
            System.out.println("- Reroute the data flow");
            System.out.println("- Remove the sensitive data from the flow");
            System.out.println("- Mask/tokenize the data");
            System.out.println("- Change the destination only if legally permitted");
            System.out.println("- Update the governance policy only when legitimately authorized\n");
          }
          System.out.println("========================================\n");
        }
      }
      if (reportFile != null) {
        Files.writeString(Path.of(reportFile), reportJson);
        System.err.println("Report written to " + reportFile);
      }
      return violations.isEmpty() ? 0 : 1;
    } catch (ApiException e) {
      System.err.println("ERROR: invalid policy input: " + e.getMessage());
      return 2;
    } catch (IOException e) {
      System.err.println("ERROR: cannot read input: " + e.getMessage());
      return 2;
    } catch (RuntimeException e) {
      System.err.println("ERROR: " + e.getMessage());
      return 2;
    }
  }

  private static CompiledPolicy readPolicy(Path file) {
    try {
      return new PolicyCompiler().compile(Files.readString(file));
    } catch (IOException e) {
      throw new IllegalArgumentException("cannot read policy file " + file + ": " + e.getMessage());
    }
  }

  private static ObjectNode buildReport(ObjectMapper mapper, String result, int checked, List<CliViolation> violations) {
    ObjectNode report = mapper.createObjectNode();
    report.put("result", result);
    report.put("violationCount", violations.size());
    report.put("checkedDataClassFlows", checked);
    ArrayNode list = report.putArray("violations");
    for (CliViolation v : violations) {
      ObjectNode node = list.addObject();
      node.put("source", v.source());
      node.put("destination", v.destination());
      node.put("sourceRegion", v.sourceRegion());
      node.put("destinationRegion", v.destinationRegion());
      node.put("dataClass", v.dataClass());
      if (v.policyCode() != null) node.put("policy", v.policyCode());
      node.put("reason", v.reason());
    }
    return report;
  }

  private static Map<String, String> readServices(ObjectMapper mapper, Path file) throws IOException {
    JsonNode root = mapper.readTree(file.toFile());
    Map<String, String> regions = new LinkedHashMap<>();
    for (JsonNode service : root.path("services")) {
      String id = firstText(service, "id", "name");
      String region = firstText(service, "region", "sourceRegion");
      if (id != null && region != null) regions.put(id, region);
    }
    return regions;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> readDataflows(ObjectMapper mapper, Path file) throws IOException {
    JsonNode root = mapper.readTree(file.toFile());
    JsonNode flows = root.has("dataFlows") ? root.get("dataFlows") : root;
    return mapper.convertValue(flows, List.class);
  }

  private static String regionOf(Map<String, String> serviceRegions, String serviceName) {
    String region = serviceRegions.get(serviceName);
    if (region == null) {
      throw new IllegalArgumentException("data flow references unknown service '" + serviceName + "'");
    }
    return region;
  }

  @SuppressWarnings("unchecked")
  private static List<String> dataClassesOf(Map<String, Object> flow) {
    Object raw = flow.get("dataClasses");
    if (raw instanceof List<?> list) return (List<String>) list;
    throw new IllegalArgumentException("data flow '" + flow.get("source") + " -> " + flow.get("destination")
        + "' has no dataClasses list");
  }

  private static String firstText(JsonNode node, String... keys) {
    for (String key : keys) {
      JsonNode value = node.get(key);
      if (value != null && value.isTextual()) return value.asText();
    }
    return null;
  }

  private static boolean hasOption(String[] args, String name) {
    for (String arg : args) if (arg.equals(name)) return true;
    return false;
  }

  private static String optionValue(String[] args, String name) {
    for (int i = 0; i < args.length - 1; i++) {
      if (args[i].equals(name)) return args[i + 1];
    }
    return null;
  }

  private static List<String> optionValues(String[] args, String name) {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < args.length - 1; i++) {
      if (args[i].equals(name)) values.add(args[i + 1]);
    }
    return values;
  }
}
