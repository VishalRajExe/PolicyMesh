package com.policymesh.enforcement.engine;

import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyEngineTest {

    @Mock
    private PolicyService policyService;

    private PolicyEngine policyEngine;

    private static final CompiledPolicy EU_PII_POLICY = new CompiledPolicy(
            "EU-PII-001", "EU PII Protection", "EU", "PII",
            new LinkedHashSet<>(Set.of("EU")),
            new LinkedHashSet<>(Set.of("US", "CN"))
    );

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngine(policyService);
        // default-decision field is set via reflection since @Value isn't processed outside Spring context
        setDefaultDecision(policyEngine, "ALLOW");
    }

    private void setDefaultDecision(PolicyEngine engine, String value) {
        try {
            var field = PolicyEngine.class.getDeclaredField("defaultDecision");
            field.setAccessible(true);
            field.set(engine, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void euPiiToEu_isAllowed() {
        when(policyService.resolveCompiledPolicy("EU", "PII")).thenReturn(Optional.of(EU_PII_POLICY));

        PolicyDecisionResult result = policyEngine.evaluate("EU", "EU", "PII", "orders-api", "payments-api");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.ALLOW);
        assertThat(result.policyId()).isEqualTo("EU-PII-001");
    }

    @Test
    void euPiiToUs_isDenied() {
        when(policyService.resolveCompiledPolicy("EU", "PII")).thenReturn(Optional.of(EU_PII_POLICY));

        PolicyDecisionResult result = policyEngine.evaluate("EU", "US", "PII", "orders-api", "analytics-api");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.DENY);
        assertThat(result.policyId()).isEqualTo("EU-PII-001");
    }

    @Test
    void euPiiToCn_isDenied() {
        when(policyService.resolveCompiledPolicy("EU", "PII")).thenReturn(Optional.of(EU_PII_POLICY));

        PolicyDecisionResult result = policyEngine.evaluate("EU", "CN", "PII", "orders-api", "some-cn-service");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.DENY);
    }

    @Test
    void euPublicToUs_isAllowed_whenNoPolicyApplies() {
        when(policyService.resolveCompiledPolicy("EU", "PUBLIC")).thenReturn(Optional.empty());

        PolicyDecisionResult result = policyEngine.evaluate("EU", "US", "PUBLIC", "web-app", "cdn");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.ALLOW);
        assertThat(result.policyId()).isEqualTo("NO-POLICY");
    }

    @Test
    void noApplicablePolicy_usesDenyDefault_whenConfigured() {
        setDefaultDecision(policyEngine, "DENY");
        when(policyService.resolveCompiledPolicy(anyString(), anyString())).thenReturn(Optional.empty());

        PolicyDecisionResult result = policyEngine.evaluate("EU", "US", "UNKNOWN", "svc-a", "svc-b");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.DENY);
    }

    @Test
    void policyAppliesButRegionNeitherAllowedNorDenied_defaultsToDeny() {
        CompiledPolicy narrowPolicy = new CompiledPolicy(
                "APAC-PII-001", "APAC PII", "APAC", "PII",
                new LinkedHashSet<>(Set.of("SG")),
                new LinkedHashSet<>()
        );
        when(policyService.resolveCompiledPolicy("APAC", "PII")).thenReturn(Optional.of(narrowPolicy));

        PolicyDecisionResult result = policyEngine.evaluate("APAC", "JP", "PII", "svc-a", "svc-b");

        assertThat(result.decision()).isEqualTo(PolicyDecisionResult.Decision.DENY);
    }
}
