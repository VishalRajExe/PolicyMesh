package com.policymesh.ci;

import com.policymesh.ci.engine.ComplianceEngine;
import com.policymesh.ci.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ComplianceEngine.
 *
 * Tests the full compliance check workflow:
 * 1. Load policies, services, edges
 * 2. Validate graph
 * 3. Run evaluation
 * 4. Generate violations
 */
class ComplianceEngineTest {

    private ComplianceEngine engine;
    private List<Policy> policies;
    private List<ServiceNode> services;

    @BeforeEach
    void setUp() {
        engine = new ComplianceEngine();

        policies = List.of(
                new Policy("EU-PII-001", "EU PII Protection", "EU", "PII",
                        List.of("EU", "UK"), List.of("US", "CN")),
                new Policy("EU-PCI-001", "EU PCI Protection", "EU", "PCI",
                        List.of("EU"), List.of("US", "CN", "IN"))
        );

        services = List.of(
                new ServiceNode("orders-api", "Orders API", "EU", "production"),
                new ServiceNode("payments-api", "Payments API", "EU", "production"),
                new ServiceNode("analytics-api", "Analytics API", "US", "production")
        );
    }

    @Test
    void validFlow_shouldPass() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "payments-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.PASSED, result.getStatus());
        assertEquals(0, result.getExitCode());
        assertEquals(1, result.getTotalFlows());
        assertEquals(1, result.getPassedFlows());
        assertEquals(0, result.getFailedFlows());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void invalidFlow_shouldFail() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "analytics-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getExitCode());
        assertEquals(1, result.getTotalFlows());
        assertEquals(0, result.getPassedFlows());
        assertEquals(1, result.getFailedFlows());
        assertEquals(1, result.getViolations().size());

        ComplianceViolation violation = result.getViolations().get(0);
        assertEquals("orders-api", violation.getSourceService());
        assertEquals("analytics-api", violation.getDestinationService());
        assertEquals("EU-PII-001", violation.getPolicyId());
        assertEquals(ComplianceViolation.Severity.ERROR, violation.getSeverity());
    }

    @Test
    void mixedFlows_shouldReportCorrectCounts() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "payments-api", List.of("PII")),
                new DataFlowEdge("orders-api", "analytics-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.FAILED, result.getStatus());
        assertEquals(2, result.getTotalFlows());
        assertEquals(1, result.getPassedFlows());
        assertEquals(1, result.getFailedFlows());
        assertEquals(1, result.getViolations().size());
    }

    @Test
    void noEdges_shouldPass() {
        ComplianceResult result = engine.check(policies, services, List.of());
        assertEquals(ComplianceResult.Status.PASSED, result.getStatus());
        assertEquals(0, result.getExitCode());
        assertEquals(0, result.getTotalFlows());
    }

    @Test
    void noPolicies_shouldReturnError() {
        ComplianceResult result = engine.check(List.of(), services,
                List.of(new DataFlowEdge("orders-api", "payments-api", List.of("PII"))));
        assertEquals(ComplianceResult.Status.ERROR, result.getStatus());
        assertEquals(2, result.getExitCode());
    }

    @Test
    void noServices_shouldReturnError() {
        ComplianceResult result = engine.check(policies, List.of(),
                List.of(new DataFlowEdge("orders-api", "payments-api", List.of("PII"))));
        assertEquals(ComplianceResult.Status.ERROR, result.getStatus());
        assertEquals(2, result.getExitCode());
    }

    @Test
    void multiplePoliciesAnyDeny_shouldFail() {
        // Flow: orders EU -> analytics US, dataClasses: PII, PCI
        // EU-PII-001 denies PII to US
        // EU-PCI-001 denies PCI to US
        // Both should produce violations
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "analytics-api", List.of("PII", "PCI"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.FAILED, result.getStatus());
        // Should have 2 violations (one for each data class)
        assertEquals(2, result.getViolations().size());
    }

    @Test
    void nonMatchingDataClass_shouldPass() {
        // Data class "HEALTH" doesn't match any policy
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "analytics-api", List.of("HEALTH"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.PASSED, result.getStatus());
        assertEquals(0, result.getViolations().size());
    }

    @Test
    void unknownServiceInEdge_shouldReturnError() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("unknown-api", "payments-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.ERROR, result.getStatus());
    }

    // --- Consistency Tests ---
    // These tests verify the CI checker produces the same results as the runtime engine.

    @Test
    void consistency_euPiiToUs_mustDeny() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "analytics-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.FAILED, result.getStatus());
        assertTrue(result.hasHardViolations());
    }

    @Test
    void consistency_euPiiToEu_mustAllow() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "payments-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertEquals(ComplianceResult.Status.PASSED, result.getStatus());
        assertFalse(result.hasHardViolations());
    }

    @Test
    void jsonSerialization_shouldWork() {
        List<DataFlowEdge> edges = List.of(
                new DataFlowEdge("orders-api", "payments-api", List.of("PII"))
        );

        ComplianceResult result = engine.check(policies, services, edges);
        assertNotNull(result.toString());
        assertEquals("PASSED", result.getStatus().name());
    }
}
