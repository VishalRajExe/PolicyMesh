package com.policymesh.ci;

import com.policymesh.ci.engine.PolicyEvaluator;
import com.policymesh.ci.model.CheckStatus;
import com.policymesh.ci.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PolicyEvaluator.
 *
 * Consistency requirement: These test cases must produce the same result
 * in both the CI checker and the Spring Boot runtime engine.
 */
class PolicyEvaluatorTest {

    private PolicyEvaluator evaluator;
    private Policy euPiiPolicy;
    private Policy euPciPolicy;
    private Policy indiaPiiPolicy;

    @BeforeEach
    void setUp() {
        evaluator = new PolicyEvaluator();

        euPiiPolicy = new Policy(
                "EU-PII-001",
                "EU PII Protection",
                "EU",
                "PII",
                List.of("EU", "UK"),
                List.of("US", "CN")
        );

        euPciPolicy = new Policy(
                "EU-PCI-001",
                "EU PCI Protection",
                "EU",
                "PCI",
                List.of("EU"),
                List.of("US", "CN", "IN")
        );

        indiaPiiPolicy = new Policy(
                "IN-PII-001",
                "India PII Protection",
                "IN",
                "PII",
                List.of("IN", "SG"),
                List.of("US", "CN")
        );
    }

    // --- EU PII Tests ---

    @Test
    void euPiiToEu_shouldAllow() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "EU", "PII");
        assertEquals(CheckStatus.ALLOW, result);
    }

    @Test
    void euPiiToUk_shouldAllow() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "UK", "PII");
        assertEquals(CheckStatus.ALLOW, result);
    }

    @Test
    void euPiiToUs_shouldDeny() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "US", "PII");
        assertEquals(CheckStatus.DENY, result);
    }

    @Test
    void euPiiToCn_shouldDeny() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "CN", "PII");
        assertEquals(CheckStatus.DENY, result);
    }

    @Test
    void euPiiToUnknownRegion_shouldDeny() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "BR", "PII");
        assertEquals(CheckStatus.DENY, result);
    }

    @Test
    void euPiiNonMatchingDataClass_shouldNotApply() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "US", "PCI");
        assertEquals(CheckStatus.NOT_APPLICABLE, result);
    }

    // --- EU PCI Tests ---

    @Test
    void euPciToEu_shouldAllow() {
        CheckStatus result = evaluator.evaluate(euPciPolicy, "EU", "EU", "PCI");
        assertEquals(CheckStatus.ALLOW, result);
    }

    @Test
    void euPciToUs_shouldDeny() {
        CheckStatus result = evaluator.evaluate(euPciPolicy, "EU", "US", "PCI");
        assertEquals(CheckStatus.DENY, result);
    }

    @Test
    void euPciToIn_shouldDeny() {
        CheckStatus result = evaluator.evaluate(euPciPolicy, "EU", "IN", "PCI");
        assertEquals(CheckStatus.DENY, result);
    }

    // --- India PII Tests ---

    @Test
    void indiaPiiToIn_shouldAllow() {
        CheckStatus result = evaluator.evaluate(indiaPiiPolicy, "IN", "IN", "PII");
        assertEquals(CheckStatus.ALLOW, result);
    }

    @Test
    void indiaPiiToSg_shouldAllow() {
        CheckStatus result = evaluator.evaluate(indiaPiiPolicy, "IN", "SG", "PII");
        assertEquals(CheckStatus.ALLOW, result);
    }

    @Test
    void indiaPiiToUs_shouldDeny() {
        CheckStatus result = evaluator.evaluate(indiaPiiPolicy, "IN", "US", "PII");
        assertEquals(CheckStatus.DENY, result);
    }

    // --- Edge Cases ---

    @Test
    void nullPolicy_shouldNotApply() {
        CheckStatus result = evaluator.evaluate(null, "EU", "US", "PII");
        assertEquals(CheckStatus.NOT_APPLICABLE, result);
    }

    @Test
    void nullDataClass_shouldNotApply() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "US", null);
        assertEquals(CheckStatus.NOT_APPLICABLE, result);
    }

    @Test
    void nullSourceRegion_shouldNotApply() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, null, "US", "PII");
        assertEquals(CheckStatus.NOT_APPLICABLE, result);
    }

    @Test
    void nullDestinationRegion_shouldNotApply() {
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", null, "PII");
        assertEquals(CheckStatus.NOT_APPLICABLE, result);
    }

    // --- Consistency Test ---
    // This test verifies the critical consistency requirement:
    // The CI checker must produce the same result as the runtime backend.

    @Test
    void consistency_euPiiToUs_mustAlwaysDeny() {
        // This specific scenario MUST always produce DENY
        // regardless of how the policy is configured.
        // EU PII -> US is explicitly denied by EU-PII-001.
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "US", "PII");
        assertEquals(CheckStatus.DENY, result,
                "CRITICAL: EU PII -> US must always be DENIED for consistency with runtime engine");
    }

    @Test
    void consistency_euPiiToEu_mustAlwaysAllow() {
        // EU PII -> EU is explicitly allowed by EU-PII-001.
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "EU", "PII");
        assertEquals(CheckStatus.ALLOW, result,
                "CRITICAL: EU PII -> EU must always be ALLOWED for consistency with runtime engine");
    }

    @Test
    void unknownDestination_notInAllowedList_shouldDeny() {
        // Default deny: if destination is not explicitly allowed, it's denied.
        CheckStatus result = evaluator.evaluate(euPiiPolicy, "EU", "AU", "PII");
        assertEquals(CheckStatus.DENY, result,
                "Default deny: AU is not in allowed regions");
    }
}
