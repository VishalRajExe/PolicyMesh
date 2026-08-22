package com.policymesh.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single compliance violation for a data flow against a policy.
 *
 * Severity levels:
 * - ERROR: Actual prohibited transfer. This is a hard violation.
 * - WARNING: Policy concern that may need review but is not automatically blocked.
 */
public class ComplianceViolation {

    public enum Severity {
        ERROR,
        WARNING
    }

    @JsonProperty("sourceService")
    private String sourceService;

    @JsonProperty("sourceRegion")
    private String sourceRegion;

    @JsonProperty("destinationService")
    private String destinationService;

    @JsonProperty("destinationRegion")
    private String destinationRegion;

    @JsonProperty("dataClass")
    private String dataClass;

    @JsonProperty("policyId")
    private String policyId;

    @JsonProperty("policyName")
    private String policyName;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private Severity severity;

    @JsonCreator
    public ComplianceViolation() {
        // default constructor for deserialization
    }

    public ComplianceViolation(String sourceService, String sourceRegion,
                                String destinationService, String destinationRegion,
                                String dataClass, String policyId, String policyName,
                                String reason, Severity severity) {
        this.sourceService = sourceService;
        this.sourceRegion = sourceRegion;
        this.destinationService = destinationService;
        this.destinationRegion = destinationRegion;
        this.dataClass = dataClass;
        this.policyId = policyId;
        this.policyName = policyName;
        this.reason = reason;
        this.severity = severity;
    }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getSourceRegion() { return sourceRegion; }
    public void setSourceRegion(String sourceRegion) { this.sourceRegion = sourceRegion; }

    public String getDestinationService() { return destinationService; }
    public void setDestinationService(String destinationService) { this.destinationService = destinationService; }

    public String getDestinationRegion() { return destinationRegion; }
    public void setDestinationRegion(String destinationRegion) { this.destinationRegion = destinationRegion; }

    public String getDataClass() { return dataClass; }
    public void setDataClass(String dataClass) { this.dataClass = dataClass; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) -> %s (%s) | Data: %s | Policy: %s | %s",
                severity, sourceService, sourceRegion, destinationService, destinationRegion,
                dataClass, policyId, reason);
    }
}
