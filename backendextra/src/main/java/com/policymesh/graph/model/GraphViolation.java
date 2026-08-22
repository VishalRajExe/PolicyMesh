package com.policymesh.graph.model;

public record GraphViolation(
        String sourceService,
        String destinationService,
        String sourceRegion,
        String destinationRegion,
        String dataClass,
        String policyCode,
        String reason
) {
}
