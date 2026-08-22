package com.policymesh;

import com.policymesh.ai.AIClassification;
import com.policymesh.ai.AIClassificationRepository;
import com.policymesh.auth.Role;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.ci.CIScan;
import com.policymesh.ci.CIScanRepository;
import com.policymesh.enforcement.DecisionRecord;
import com.policymesh.enforcement.DecisionRepository;
import com.policymesh.lineage.LineageRecord;
import com.policymesh.lineage.LineageRepository;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mysql_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class MySqlPersistenceIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private ServiceNodeRepository serviceNodeRepository;
  @Autowired private DataFlowEdgeRepository dataFlowEdgeRepository;
  @Autowired private DecisionRepository decisionRepository;
  @Autowired private LineageRepository lineageRepository;
  @Autowired private CIScanRepository ciScanRepository;
  @Autowired private AIClassificationRepository aiClassificationRepository;

  @Test
  @DisplayName("1. User Entity Persistence and CRUD")
  void testUserCrud() {
    User user = new User();
    user.setEmail("admin@policymesh.io");
    user.setPasswordHash("$2a$10$hashedPasswordSample");
    user.setRole(Role.ADMIN);
    User saved = userRepository.save(user);

    assertNotNull(saved.getId());
    assertEquals("admin@policymesh.io", saved.getEmail());

    saved.setRole(Role.COMPLIANCE_OFFICER);
    userRepository.save(saved);

    Optional<User> found = userRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(Role.COMPLIANCE_OFFICER, found.get().getRole());
  }

  @Test
  @DisplayName("2. Policy Entity Persistence with Collections and Status")
  void testPolicyCrud() {
    Policy policy = new Policy();
    policy.setPolicyCode("EU-PII-TEST-001");
    policy.setName("EU General Data Protection");
    policy.setJurisdiction("EU");
    policy.setDataClass("PII");
    policy.setStatus(PolicyStatus.ACTIVE);
    policy.setVersion(1);
    policy.setAllowedRegions(Set.of("EU", "IN"));
    policy.setDeniedRegions(Set.of("US", "CN"));

    Policy saved = policyRepository.save(policy);
    assertNotNull(saved.getId());

    Optional<Policy> found = policyRepository.findByPolicyCodeIgnoreCase("EU-PII-TEST-001");
    assertTrue(found.isPresent());
    assertEquals(2, found.get().getAllowedRegions().size());
    assertTrue(found.get().getAllowedRegions().contains("EU"));
    assertTrue(found.get().getDeniedRegions().contains("US"));

    // Update
    found.get().setVersion(2);
    policyRepository.save(found.get());

    Policy updated = policyRepository.findByPolicyCodeIgnoreCase("EU-PII-TEST-001").orElseThrow();
    assertEquals(2, updated.getVersion());
  }

  @Test
  @DisplayName("3. ServiceNode and DataFlowEdge Graph Persistence")
  void testServiceGraphPersistence() {
    ServiceNode orders = new ServiceNode();
    orders.setName("orders-api-test");
    orders.setRegion("EU");
    orders.setEnvironment("production");
    orders.setDescription("Orders service");
    ServiceNode savedOrders = serviceNodeRepository.save(orders);

    ServiceNode payments = new ServiceNode();
    payments.setName("payments-api-test");
    payments.setRegion("EU");
    payments.setEnvironment("production");
    payments.setDescription("Payments service");
    ServiceNode savedPayments = serviceNodeRepository.save(payments);

    DataFlowEdge edge = new DataFlowEdge();
    edge.setSourceServiceId(savedOrders.getId());
    edge.setDestinationServiceId(savedPayments.getId());
    edge.setDataClasses(Set.of("PII", "PCI"));
    DataFlowEdge savedEdge = dataFlowEdgeRepository.save(edge);

    assertNotNull(savedEdge.getId());
    assertEquals(2, savedEdge.getDataClasses().size());

    Optional<DataFlowEdge> foundEdge = dataFlowEdgeRepository.findById(savedEdge.getId());
    assertTrue(foundEdge.isPresent());
    assertEquals(savedOrders.getId(), foundEdge.get().getSourceServiceId());
  }

  @Test
  @DisplayName("4. Decision and Lineage Record Audit Persistence")
  void testDecisionAndLineagePersistence() {
    DecisionRecord decision = new DecisionRecord();
    decision.setSourceService("orders-api");
    decision.setDestinationService("analytics-api");
    decision.setSourceRegion("EU");
    decision.setDestinationRegion("US");
    decision.setDataClass("PII");
    decision.setDecision("DENY");
    decision.setPolicyId("EU-PII-001");
    decision.setReason("Cross-border transfer of EU PII to US is disallowed");
    DecisionRecord savedDecision = decisionRepository.save(decision);

    assertNotNull(savedDecision.getId());

    LineageRecord lineage = new LineageRecord();
    lineage.setDecisionId(savedDecision.getId());
    lineage.setSourceService("orders-api");
    lineage.setDestinationService("analytics-api");
    lineage.setSourceRegion("EU");
    lineage.setDestinationRegion("US");
    lineage.setDataClass("PII");
    lineage.setDecision("DENY");
    lineage.setReason("Cross-border transfer of EU PII to US is disallowed");
    lineage.setPolicyId("EU-PII-001");
    lineage.setPreviousHash("GENESIS");
    lineage.setCurrentHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    LineageRecord savedLineage = lineageRepository.save(lineage);

    assertNotNull(savedLineage.getId());
    assertEquals(savedDecision.getId(), savedLineage.getDecisionId());
  }

  @Test
  @DisplayName("5. CIScan and AIClassification Persistence")
  void testCiAndAiPersistence() {
    CIScan scan = new CIScan();
    scan.setCommitHash("abc12345def");
    scan.setBranch("feature/policy-v2");
    scan.setStatus("FAILED");
    scan.setViolationCount(1);
    scan.setViolationsJson("[{\"policyId\":\"EU-PII-001\",\"reason\":\"Violation\"}]");
    scan.complete();
    CIScan savedScan = ciScanRepository.save(scan);

    assertNotNull(savedScan.getId());
    assertNotNull(savedScan.getCompletedAt());

    AIClassification ai = new AIClassification();
    ai.setFieldName("credit_card_number");
    ai.setSampleValue("4111-XXXX-XXXX-1111");
    ai.setClassification("PCI");
    ai.setConfidence(0.98);
    ai.setProvider("heuristic");
    ai.setStatus("APPROVED");
    AIClassification savedAi = aiClassificationRepository.save(ai);

    assertNotNull(savedAi.getId());
    assertEquals("PCI", savedAi.getClassification());
  }
}
