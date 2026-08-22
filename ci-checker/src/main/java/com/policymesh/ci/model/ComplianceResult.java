package com.policymesh.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Final result of a compliance check.
 * 
 * Contains the overall status and all violations found.
 */
public class ComplianceResult {

    public enum Status {
        PASSED,
        FAILED,
        ERROR
    }

    @JsonProperty("status")
    private Status status;

    @JsonProperty("totalFlows")
    private int totalFlows;

    @JsonProperty("passedFlows")
    private int passedFlows;

    @JsonProperty("failedFlows")
    private int failedFlows;

    @JsonProperty("violations")
    private List<ComplianceViolation> violations;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonCreator
    public ComplianceResult() {
        this.violations = new ArrayList<>();
        this.status = Status.PASSED;
    }

    public ComplianceResult(Status status, int totalFlows, int passedFlows, int failedFlows,
                            List<ComplianceViolation> violations) {
        this.status = status;
        this.totalFlows = totalFlows;
        this.passedFlows = passedFlows;
        this.failedFlows = failedFlows;
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }

    /**
     * Creates an error result for configuration or input errors.
     */
    public static ComplianceResult error(String message) {
        ComplianceResult result = new ComplianceResult();
        result.setStatus(Status.ERROR);
        result.setErrorMessage(message);
        return result;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getTotalFlows() { return totalFlows; }
    public void setTotalFlows(int totalFlows) { this.totalFlows = totalFlows; }

    public int getPassedFlows() { return passedFlows; }
    public void setPassedFlows(int passedFlows) { this.passedFlows = passedFlows; }

    public int getFailedFlows() { return failedFlows; }
    public void setFailedFlows(int failedFlows) { this.failedFlows = failedFlows; }

    public List<ComplianceViolation> getViolations() {
        return violations != null ? List.copyOf(violations) : List.of();
    }

    public void addViolation(ComplianceViolation violation) {
        if (this.violations == null) {
            this.violations = new ArrayList<>();
        }
        this.violations.add(violation);
    }

    public void setViolations(List<ComplianceViolation> violations) {
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * Returns the exit code for this result.
     * 0 = passed, 1 = violations, 2 = configuration error
     */
    public int getExitCode() {
        return switch (status) {
            case PASSED -> 0;
            case FAILED -> 1;
            case ERROR -> 2;
        };
    }

    public boolean hasHardViolations() {
        return violations != null && violations.stream()
                .anyMatch(v -> v.getSeverity() == ComplianceViolation.Severity.ERROR);
    }

    @Override
    public String toString() {
        return String.format("ComplianceResult{status=%s, total=%d, passed=%d, failed=%d, violations=%d}",
                status, totalFlows, passedFlows, failedFlows,
                violations != null ? violations.size() : 0);
    }
}
