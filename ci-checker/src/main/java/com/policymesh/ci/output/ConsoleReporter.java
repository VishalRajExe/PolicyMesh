package com.policymesh.ci.output;

import com.policymesh.ci.model.ComplianceResult;
import com.policymesh.ci.model.ComplianceViolation;

import java.util.List;

/**
 * Formats compliance results for the terminal and GitHub Actions.
 *
 * Features:
 * - ANSI color support (can be disabled with --no-color)
 * - Clear visual hierarchy
 * - GitHub Actions workflow annotation integration (::error title=...)
 * - Actionable remediation guidance
 */
public class ConsoleReporter {

    private final boolean useColor;

    // ANSI color codes
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    public ConsoleReporter(boolean noColor) {
        this.useColor = !noColor && System.console() != null;
    }

    private String color(String colorCode, String text) {
        return useColor ? colorCode + text + RESET : text;
    }

    private String bold(String text) {
        return useColor ? BOLD + text + RESET : text;
    }

    /**
     * Prints the full compliance report header to stdout.
     */
    public void report(ComplianceResult result, int policiesLoaded, int servicesLoaded, int flowsLoaded) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(color(CYAN, "========================================"));
        sb.append("\n");
        sb.append(color(CYAN, bold("      POLICYMESH COMPLIANCE CHECK")));
        sb.append("\n");
        sb.append(color(CYAN, "========================================"));
        sb.append("\n\n");

        sb.append("Policies loaded: ").append(policiesLoaded).append("\n");
        sb.append("Services loaded: ").append(servicesLoaded).append("\n");
        sb.append("Data flows loaded: ").append(flowsLoaded).append("\n\n");

        sb.append(color(BOLD, "Checking data flows...")).append("\n\n");

        System.out.print(sb);
    }

    /**
     * Prints a passing flow.
     */
    public void reportPass(String sourceId, String sourceRegion,
                           String destId, String destRegion,
                           String dataClasses) {
        StringBuilder sb = new StringBuilder();
        sb.append(color(GREEN, bold("[PASS] "))).append(sourceId)
          .append(" [").append(sourceRegion).append("]").append("\n");
        sb.append(color(GREEN, "       ↓")).append("\n");
        sb.append("       ").append(destId).append(" [").append(destRegion).append("]").append("\n");
        sb.append("       Data: ").append(dataClasses).append("\n\n");
        System.out.print(sb);
    }

    /**
     * Prints a failing flow with violation details and emits GitHub Actions workflow command.
     */
    public void reportFail(String sourceId, String sourceRegion,
                           String destId, String destRegion,
                           String dataClasses, ComplianceViolation violation) {
        // Emit GitHub Actions annotation command if running in CI environment
        System.out.println("::error title=PolicyMesh Compliance::" + violation.getPolicyId() + " violation: "
            + sourceId + " (" + sourceRegion + ") → " + destId + " (" + destRegion + ") carrying " + dataClasses);

        StringBuilder sb = new StringBuilder();
        sb.append(color(RED, bold("[FAIL] "))).append(sourceId)
          .append(" [").append(sourceRegion).append("]").append("\n");
        sb.append(color(RED, "       ↓")).append("\n");
        sb.append("       ").append(destId).append(" [").append(destRegion).append("]").append("\n");
        sb.append("       Data: ").append(dataClasses).append("\n\n");
        sb.append("       Policy: ").append(violation.getPolicyId()).append("\n");
        sb.append("       Reason: ").append(violation.getReason()).append("\n\n");
        System.out.print(sb);
    }

    /**
     * Prints the final summary with formatted remediation and exit results.
     */
    public void reportSummary(ComplianceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("========================================\n");
        sb.append("PolicyMesh Compliance Check\n");
        sb.append("========================================\n\n");

        if (result.getStatus() == ComplianceResult.Status.PASSED) {
            sb.append("Result: PASSED\n\n");
            sb.append("Flows checked: ").append(result.getTotalFlows()).append("\n");
            sb.append("Passed: ").append(result.getPassedFlows()).append("\n");
            sb.append("Failed: 0\n\n");
            sb.append(color(GREEN, bold("✅ PolicyMesh Compliance Passed\n")));
        } else if (result.getStatus() == ComplianceResult.Status.FAILED) {
            sb.append("Result: FAILED\n\n");
            sb.append("Violations: ").append(result.getViolations().size()).append("\n\n");

            List<ComplianceViolation> violations = result.getViolations();
            for (int i = 0; i < violations.size(); i++) {
                ComplianceViolation v = violations.get(i);
                sb.append("[").append(i + 1).append("] ").append(v.getPolicyId()).append("\n");
                sb.append("Source: ").append(v.getSourceService()).append(" (").append(v.getSourceRegion()).append(")\n");
                sb.append("Destination: ").append(v.getDestinationService()).append(" (").append(v.getDestinationRegion()).append(")\n");
                sb.append("Data Class: ").append(v.getDataClass()).append("\n\n");
                sb.append("Reason:\n").append(v.getReason()).append("\n\n");
                sb.append("Remediation:\n");
                sb.append("- Reroute the data flow\n");
                sb.append("- Remove the sensitive data from the flow\n");
                sb.append("- Mask/tokenize the data\n");
                sb.append("- Change the destination only if legally permitted\n");
                sb.append("- Update the governance policy only when legitimately authorized\n\n");
            }

            sb.append("Flows checked: ").append(result.getTotalFlows()).append("\n");
            sb.append("Passed: ").append(result.getPassedFlows()).append("\n");
            sb.append("Failed: ").append(result.getFailedFlows()).append("\n\n");
            sb.append(color(RED, bold("❌ PolicyMesh Compliance FAILED\n")));
        } else {
            sb.append("Result: ERROR\n\n");
            if (result.getErrorMessage() != null) {
                sb.append("Error: ").append(result.getErrorMessage()).append("\n\n");
            }
        }

        sb.append("========================================\n\n");
        System.out.print(sb);
    }
}
