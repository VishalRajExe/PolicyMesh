package com.policymesh;

import com.policymesh.ci.CIScanRepository;
import com.policymesh.ci.CiDtos;
import com.policymesh.ci.CiService;
import com.policymesh.enforcement.EnforcementDtos;
import com.policymesh.enforcement.EnforcementService;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.lineage.LineageDtos;
import com.policymesh.lineage.LineageService;
import com.policymesh.policy.Decision;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyEngine;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.GraphDtos;
import com.policymesh.servicegraph.ServiceGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end acceptance scenario through the real beans: policy -> graph -> CI -> enforcement
 * -> lineage, including the documented fix-and-repass flow.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:policymesh;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false"})
@Transactional
class PolicyMeshIntegrationTest {
  @Autowired PolicyRepository policies;
  @Autowired PolicyEngine engine;
  @Autowired ServiceGraphService services;
  @Autowired GraphAnalyzer graph;
  @Autowired CiService ci;
  @Autowired EnforcementService enforcement;
  @Autowired LineageService lineage;
  @Autowired CIScanRepository scans;

  private Long ordersId;
  private Long paymentsId;
  private Long analyticsId;

  @BeforeEach
  void seed() {
    savePolicy("EU-PII-001", "EU PII Protection", "EU", Set.of("EU"), Set.of("US", "CN"));
    savePolicy("IN-PII-001", "India PII Protection", "IN", Set.of("IN"), Set.of("US", "CN"));

    GraphDtos.ServiceResponse orders = service("orders-api", "EU");
    GraphDtos.ServiceResponse payments = service("payments-api", "EU");
    GraphDtos.ServiceResponse analytics = service("analytics-api", "US");
    ordersId = orders.id();
    paymentsId = payments.id();
    analyticsId = analytics.id();
    edge(ordersId, paymentsId);
    edge(ordersId, analyticsId);
  }

  @Test
  void policyEngineDeterministicBehavior() {
    assertThat(engine.evaluate("orders", "payments", "EU", "EU", "PII", Set.of()).decision()).isEqualTo(Decision.ALLOW);
    assertThat(engine.evaluate("orders", "analytics", "EU", "US", "PII", Set.of()).decision()).isEqualTo(Decision.DENY);
    assertThat(engine.evaluate("orders", "analytics", "EU", "CN", "PII", Set.of()).decision()).isEqualTo(Decision.DENY);
    assertThat(engine.evaluate("orders", "analytics", "EU", "US", "PUBLIC", Set.of()).decision()).isEqualTo(Decision.ALLOW);
    assertThat(engine.evaluate("orders", "analytics", "EU", "US", "PHI", Set.of()).decision()).isEqualTo(Decision.DENY); // no policy -> deny by default
    // The India policy must not leak into EU transfers.
    assertThat(engine.evaluate("orders", "payments", "EU", "EU", "PII", Set.of()).decision()).isEqualTo(Decision.ALLOW);
  }

  @Test
  void graphAnalysisFindsExactlyTheDocumentedViolation() {
    var result = graph.validate();
    assertThat(result.result()).isEqualTo("FAIL");
    assertThat(result.violations()).hasSize(1);
    var violation = result.violations().getFirst();
    assertThat(violation.sourceService()).isEqualTo("orders-api");
    assertThat(violation.destinationService()).isEqualTo("analytics-api");
    assertThat(violation.dataClass()).isEqualTo("PII");
    assertThat(violation.policyCode()).isEqualTo("EU-PII-001");
  }

  @Test
  void ciPersistsScansAndFailsThenPassesAfterFix() {
    CiDtos.Response failed = ci.run("main", "abc123");
    assertThat(failed.result()).isEqualTo("FAIL");
    assertThat(failed.violationCount()).isEqualTo(1);
    assertThat(failed.passed()).isFalse();
    assertThat(failed.completedAt()).isNotNull();
    assertThat(scans.count()).isEqualTo(1);
    assertThat(scans.findAll().getFirst().getCommitHash()).isEqualTo("abc123");
    assertThat(scans.findAll().getFirst().getBranch()).isEqualTo("main");

    // Persisted violations survive retrieval.
    CiDtos.Response stored = ci.one(failed.id());
    assertThat(stored.violations()).hasSize(1);
    assertThat(stored.violations().getFirst().policyCode()).isEqualTo("EU-PII-001");

    // Fix: move analytics-api to the EU and rescan.
    services.updateService(analyticsId, new GraphDtos.ServiceRequest("analytics-api", "EU", "zone", "production", null));
    assertThat(graph.validate().result()).isEqualTo("PASS");

    CiDtos.Response passed = ci.run("main", "def456");
    assertThat(passed.result()).isEqualTo("PASS");
    assertThat(passed.violationCount()).isEqualTo(0);
    assertThat(passed.passed()).isTrue();
    assertThat(scans.count()).isEqualTo(2);
  }

  @Test
  void enforcementDeniesCreatesDecisionAndLineage() {
    var denied = enforcement.check(EnforcementDtos.Request.of("orders-api", "analytics-api", "EU", "US", Set.of("PII")));
    assertThat(denied.decision()).isEqualTo("DENY");
    assertThat(denied.policyId()).isEqualTo("EU-PII-001");
    assertThat(denied.reason()).contains("denied");
    assertThat(denied.lineageHash()).hasSize(64);
    assertThat(denied.lineageId()).isNotNull();
    assertThat(denied.decisionId()).isNotNull();

    var allowed = enforcement.check(EnforcementDtos.Request.of("orders-api", "payments-api", "EU", "EU", Set.of("PII")));
    assertThat(allowed.decision()).isEqualTo("ALLOW");
    assertThat(allowed.policyId()).isEqualTo("EU-PII-001");

    LineageDtos.Verification verification = lineage.verify();
    assertThat(verification.valid()).isTrue();
    assertThat(verification.recordsChecked()).isEqualTo(2);
  }

  @Test
  void enforcementEvaluatesEveryDataClassTagAndTakesTheWorstOutcome() {
    // PCI has no active policy here -> deny by default; PII alone would be ALLOW to the EU.
    var response = enforcement.check(EnforcementDtos.Request.of("orders-api", "payments-api", "EU", "EU", Set.of("PII", "PCI")));
    assertThat(response.decision()).isEqualTo("DENY");
    assertThat(response.reason()).contains("PCI");
  }

  @Test
  void enforcementResolvesRegionsFromRegisteredServices() {
    var response = enforcement.check(EnforcementDtos.Request.of("orders-api", "analytics-api", null, null, Set.of("PII")));
    assertThat(response.decision()).isEqualTo("DENY"); // orders EU -> analytics US
  }

  @Test
  void multipleViolationsAreAllReported() {
    GraphDtos.ServiceResponse china = service("china-api", "CN");
    edge(ordersId, china.id());
    var result = graph.validate();
    assertThat(result.result()).isEqualTo("FAIL");
    assertThat(result.violationCount()).isEqualTo(2);
    assertThat(result.checkedEdges()).isEqualTo(3);
  }

  @Test
  void serviceGraphValidatesEdges() {
    assertThatThrownBy(() -> services.createEdge(new GraphDtos.EdgeRequest(ordersId, ordersId, Set.of("PII"))))
        .isInstanceOf(com.policymesh.common.ApiException.class);
    assertThatThrownBy(() -> services.createEdge(new GraphDtos.EdgeRequest(ordersId, 999999L, Set.of("PII"))))
        .isInstanceOf(com.policymesh.common.ApiException.class);
    // Deleting a service removes its edges.
    services.deleteService(analyticsId);
    assertThat(graph.validate().result()).isEqualTo("PASS");
  }

  private void savePolicy(String code, String name, String jurisdiction, Set<String> allowed, Set<String> denied) {
    Policy p = new Policy();
    p.setPolicyCode(code);
    p.setName(name);
    p.setJurisdiction(jurisdiction);
    p.setDataClass("PII");
    p.setAllowedRegions(new TreeSet<>(allowed));
    p.setDeniedRegions(new TreeSet<>(denied));
    p.setStatus(PolicyStatus.ACTIVE);
    policies.save(p);
  }

  private GraphDtos.ServiceResponse service(String name, String region) {
    return services.createService(new GraphDtos.ServiceRequest(name, region, "zone", "production", null));
  }

  private void edge(Long from, Long to) {
    services.createEdge(new GraphDtos.EdgeRequest(from, to, Set.of("PII")));
  }
}
