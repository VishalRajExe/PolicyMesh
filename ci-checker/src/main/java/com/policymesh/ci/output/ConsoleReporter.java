package com.policymesh.ci.output;

import com.policymesh.ci.model.ComplianceResult;
import com.policymesh.ci.model.ComplianceViolation;

/**
 * Formats compliance results for the terminal.
 *
 * Features:
 * - ANSI color support (can be disabled with --no-color)
 * - Clear visual hierarchy
 * - Pass/fail indicators
 * - Summary section
 *
 * Example output:
 * <pre>
 * ========================================
 *        POLICYMESH COMPLIANCE CHECK
 * ========================================
 *
 * Policies loaded: 2
 * Services loaded: 3
 * Data flows loaded: 2
 *
 * Checking data flows...
 *
 * [PASS] orders-api [EU]
 *        ↓
 *        payments-api [EU]
 *        Data: PII
 *
 * [FAIL] orders-api [EU]
 *        ↓
 *        analytics-api [US]
 *        Data: PII
 *
 *        Policy: EU-PII-001
 *        Reason: EU PII cannot be transferred to US
 *
 * ----------------------------------------
 * RESULT: FAILED
 * Flows checked: 2
 * Passed: 1
 * Failed: 1
 * ----------------------------------------
 * </pre>
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
     * Prints the full compliance report to stdout.
     */
    public void report(ComplianceResult result, int policiesLoaded, int servicesLoaded, int flowsLoaded) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(color(CYAN, "========================================="));
        sb.append("\n");
        sb.append(color(CYAN, bold("        POLICYMESH COMPLIANCE CHECK")));
        sb.append("\n");
        sb.append(color(CYAN, "========================================="));
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
     * Prints a failing flow with violation details.
     */
    public void reportFail(String sourceId, String sourceRegion,
                           String destId, String destRegion,
                           String dataClasses, ComplianceViolation violation) {
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
     * Prints the final summary.
     */
    public void reportSummary(ComplianceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(color(CYAN, "----------------------------------------")).append("\n");

        if (result.getStatus() == ComplianceResult.Status.PASSED) {
            sb.append(color(GREEN, bold("RESULT: PASSED")));
        } else if (result.getStatus() == ComplianceResult.Status.FAILED) {
            sb.append(color(RED, bold("RESULT: FAILED")));
        } else {
            sb.append(color(YELLOW, bold("RESULT: ERROR")));
        }

        sb.append("\n");
        sb.append("Flows checked: ").append(result.getTotalFlows()).append("\n");
        sb.append("Passed: ").append(result.getPassedFlows()).append("\n");
        sb.append("Failed: ").append(result.getFailedFlows()).append("\n");

        if (result.getErrorMessage() != null) {
            sb.append("\n");
            sb.append(color(RED, "Error: ")).append(result.getErrorMessage()).append("\n");
        }

        sb.append(color(CYAN, "----------------------------------------")).append("\n\n");
        System.out.print(sb);
    }
}
