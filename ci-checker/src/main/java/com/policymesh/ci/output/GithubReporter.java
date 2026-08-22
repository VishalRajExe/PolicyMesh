package com.policymesh.ci.output;

import com.policymesh.ci.model.ComplianceResult;
import com.policymesh.ci.model.ComplianceViolation;

/**
 * Generates a GitHub-flavored Markdown report.
 *
 * Designed to be written to $GITHUB_STEP_SUMMARY.
 * Can also be posted as a PR comment via the GitHub API.
 */
public class GithubReporter {

    /**
     * Generates a Markdown summary of the compliance check.
     *
     * @param result the compliance result
     * @return Markdown string suitable for GitHub job summary
     */
    public String generateMarkdown(ComplianceResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("## PolicyMesh Compliance Check\n\n");

        // Status badge
        sb.append("### Result\n\n");
        if (result.getStatus() == ComplianceResult.Status.PASSED) {
            sb.append("✅ **PASSED**\n\n");
        } else if (result.getStatus() == ComplianceResult.Status.FAILED) {
            sb.append("❌ **FAILED**\n\n");
        } else {
            sb.append("⚠️ **ERROR**\n\n");
            if (result.getErrorMessage() != null) {
                sb.append("**Error:** ").append(result.getErrorMessage()).append("\n\n");
            }
        }

        // Violations table
        if (result.hasHardViolations()) {
            sb.append("### Violations\n\n");
            sb.append("| Source | Destination | Data | Policy | Reason |\n");
            sb.append("|---|---|---|---|---|\n");

            for (ComplianceViolation v : result.getViolations()) {
                sb.append("| ")
                  .append(v.getSourceService()).append(" (").append(v.getSourceRegion()).append(") | ")
                  .append(v.getDestinationService()).append(" (").append(v.getDestinationRegion()).append(") | ")
                  .append(v.getDataClass()).append(" | ")
                  .append(v.getPolicyId()).append(" | ")
                  .append(v.getReason()).append(" |\n");
            }
            sb.append("\n");
        }

        // Summary
        sb.append("### Summary\n\n");
        sb.append("- Flows checked: ").append(result.getTotalFlows()).append("\n");
        sb.append("- Passed: ").append(result.getPassedFlows()).append("\n");
        sb.append("- Failed: ").append(result.getFailedFlows()).append("\n");

        return sb.toString();
    }

    /**
     * Prints the Markdown report to stdout.
     */
    public void report(ComplianceResult result) {
        System.out.println(generateMarkdown(result));
    }
}
