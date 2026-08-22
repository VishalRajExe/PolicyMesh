package com.policymesh.servicegraph.graph;

import com.policymesh.servicegraph.entity.ServiceNode;

/**
 * Represents a policy violation found during graph analysis.
 */
public class GraphViolation {
    private final String sourceService;
    private final String destinationService;
    private final String sourceRegion;
    private final String destinationRegion;
    private final String dataClass;
    private final String policyCode;
    private final String reason;

    public GraphViolation(String sourceService, String destinationService,
                          String sourceRegion, String destinationRegion,
                          String dataClass, String policyCode, String reason) {
        this.sourceService = sourceService;
        this.destinationService = destinationService;
        this.sourceRegion = sourceRegion;
        this.destinationRegion = destinationRegion;
        this.dataClass = dataClass;
        this.policyCode = policyCode;
        this.reason = reason;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getDestinationService() {
        return destinationService;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public String getDestinationRegion() {
        return destinationRegion;
    }

    public String getDataClass() {
        return dataClass;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "GraphViolation{" +
                "sourceService='" + sourceService + '\'' +
                ", destinationService='" + destinationService + '\'' +
                ", sourceRegion='" + sourceRegion + '\'' +
                ", destinationRegion='" + destinationRegion + '\'' +
                ", dataClass='" + dataClass + '\'' +
                ", policyCode='" + policyCode + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}