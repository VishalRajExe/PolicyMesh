package com.policymesh.servicegraph.graph;

import java.util.List;

/**
 * Represents the complete result of a graph compliance analysis.
 */
public class GraphCheckResult {
    private final String status; // "PASS" or "FAILED"
    private final List<GraphViolation> violations;

    public GraphCheckResult(String status, List<GraphViolation> violations) {
        this.status = status;
        this.violations = violations;
    }

    public String getStatus() {
        return status;
    }

    public List<GraphViolation> getViolations() {
        return violations;
    }

    public boolean isPass() {
        return "PASS".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "GraphCheckResult{" +
                "status='" + status + '\'' +
                ", violations=" + violations +
                '}';
    }
}