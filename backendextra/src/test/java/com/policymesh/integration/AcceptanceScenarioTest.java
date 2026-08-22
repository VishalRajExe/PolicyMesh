package com.policymesh.integration;

import com.policymesh.enforcement.dto.EnforcementCheckRequest;
import com.policymesh.enforcement.dto.EnforcementCheckResponse;
import com.policymesh.enforcement.repository.DecisionRepository;
import com.policymesh.enforcement.service.EnforcementService;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.model.GraphCheckStatus;
import com.policymesh.graph.service.GraphService;
import com.policymesh.lineage.repository.LineageRecordRepository;
import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.service.PolicyService;
import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.service.DataFlowEdgeService;
import com.policymesh.servicegraph.service.ServiceNodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the "FIRST ACCEPTANCE TEST" scenario from the spec end to end:
 * policy creation, service/edge setup, runtime enforcement ALLOW/DENY,
 * lineage evidence creation, CI graph check, and the region-fix PASS case.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcceptanceScenarioTest {

    @Autowired private PolicyService policyService;
    @Autowired private ServiceNodeService serviceNodeService;
    @Autowired private DataFlowEdgeService dataFlowEdgeService;
    @Autowired private EnforcementService enforcementService;
    @Autowired private GraphService graphService;
    @Autowired private DecisionRepository decisionRepository;
    @Autowired private LineageRecordRepository lineageRecordRepository;

    private ServiceNodeResponse ordersApi;
    private ServiceNodeResponse paymentsApi;
    private ServiceNodeResponse analyticsApi;

    @BeforeEach
    void setUp() {
        policyService.create(new PolicyRequest(
                "EU-PII-001", "EU PII Protection", "EU", "PII",
                List.of("EU"), List.of("US")));

        ordersApi = serviceNodeService.create(new ServiceNodeRequest("orders-api", "EU", null, "production", null));
        paymentsApi = serviceNodeService.create(new ServiceNodeRequest("payments-api", "EU", null, "production", null));
        analyticsApi = serviceNodeService.create(new ServiceNodeRequest("analytics-api", "US", null, "production", null));

        dataFlowEdgeService.create(new DataFlowEdgeRequest(ordersApi.id(), paymentsApi.id(), List.of("PII")));
        dataFlowEdgeService.create(new DataFlowEdgeRequest(ordersApi.id(), analyticsApi.id(), List.of("PII")));
    }

    @Test
    void test1_ordersToPayments_bothEu_isAllowed() {
        EnforcementCheckResponse response = enforcementService.check(new EnforcementCheckRequest(
                "orders-api", "payments-api", "EU", "EU", List.of("PII")));

        assertThat(response.decision()).isEqualTo("ALLOW");
    }

    @Test
    void test2_ordersToAnalytics_euToUs_isDenied() {
        EnforcementCheckResponse response = enforcementService.check(new EnforcementCheckRequest(
                "orders-api", "analytics-api", "EU", "US", List.of("PII")));

        assertThat(response.decision()).isEqualTo("DENY");
        assertThat(response.policyId()).isEqualTo("EU-PII-001");
    }

    @Test
    void test3_deniedRequest_createsDecisionAndLineageRecordWithHash() {
        long decisionsBefore = decisionRepository.count();
        long lineageBefore = lineageRecordRepository.count();

        EnforcementCheckResponse response = enforcementService.check(new EnforcementCheckRequest(
                "orders-api", "analytics-api", "EU", "US", List.of("PII")));

        assertThat(decisionRepository.count()).isEqualTo(decisionsBefore + 1);
        assertThat(lineageRecordRepository.count()).isEqualTo(lineageBefore + 1);
        assertThat(response.lineageHash()).isNotBlank();
    }

    @Test
    void test4_ciGraphCheck_identifiesInvalidRoute() {
        GraphCheckResult result = graphService.validate();

        assertThat(result.status()).isEqualTo(GraphCheckStatus.FAILED);
        assertThat(result.violations()).hasSize(1);
        assertThat(result.violations().get(0).sourceService()).isEqualTo("orders-api");
        assertThat(result.violations().get(0).destinationService()).isEqualTo("analytics-api");
    }

    @Test
    void test5_movingAnalyticsToEu_makesGraphCheckPass() {
        serviceNodeService.update(analyticsApi.id(),
                new ServiceNodeRequest("analytics-api", "EU", null, "production", null));

        GraphCheckResult result = graphService.validate();

        assertThat(result.status()).isEqualTo(GraphCheckStatus.PASSED);
        assertThat(result.violations()).isEmpty();
    }
}
