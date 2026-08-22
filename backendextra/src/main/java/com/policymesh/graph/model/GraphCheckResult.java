package com.policymesh.graph.model;

import java.util.List;

public record GraphCheckResult(
        GraphCheckStatus status,
        int servicesAnalyzed,
        int edgesAnalyzed,
        List<GraphViolation> violations
) {
    public static GraphCheckResult of(int servicesAnalyzed, int edgesAnalyzed, List<GraphViolation> violations) {
        GraphCheckStatus status = violations.isEmpty() ? GraphCheckStatus.PASSED : GraphCheckStatus.FAILED;
        return new GraphCheckResult(status, servicesAnalyzed, edgesAnalyzed, violations);
    }
}
