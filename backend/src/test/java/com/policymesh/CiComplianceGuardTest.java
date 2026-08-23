package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.auth.AuthDtos;
import com.policymesh.auth.Role;
import com.policymesh.ci.CiDtos;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CiComplianceGuardTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private PolicyRepository policyRepo;
  @Autowired private ServiceNodeRepository serviceRepo;
  @Autowired private DataFlowEdgeRepository edgeRepo;

  private String adminToken;

  @BeforeEach
  void setup() throws Exception {
    edgeRepo.deleteAll();
    serviceRepo.deleteAll();
    policyRepo.deleteAll();

    // Register & login admin
    String email = "ci-admin-" + System.currentTimeMillis() + "@policymesh.io";
    mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AuthDtos.Register(email, "admin12345", Role.ADMIN, "CI Admin"))))
        .andExpect(status().isCreated());

    MvcResult loginRes = mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AuthDtos.Login(email, "admin12345"))))
        .andExpect(status().isOk())
        .andReturn();

    AuthDtos.Token auth = mapper.readValue(loginRes.getResponse().getContentAsString(), AuthDtos.Token.class);
    this.adminToken = "Bearer " + auth.token();

    // Setup EU-PII-001 Policy
    Policy p = new Policy();
    p.setPolicyCode("EU-PII-001");
    p.setName("EU GDPR Data Residency Protection");
    p.setJurisdiction("EU");
    p.setDataClass("PII");
    p.setAllowedRegions(Set.of("EU"));
    p.setDeniedRegions(Set.of("US", "CN", "RU"));
    p.setStatus(PolicyStatus.ACTIVE);
    policyRepo.save(p);
  }

  @Test
  @DisplayName("1. Non-existent branch returns 422 with BRANCH_NOT_FOUND")
  void testNonExistentBranchFails() throws Exception {
    var req = new CiDtos.Request("HEAD", "non-existent-branch-xyz");
    mvc.perform(post("/api/v1/ci/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errorCode").value("BRANCH_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  @DisplayName("2. Malformed SHA returns 422 with INVALID_SHA_FORMAT")
  void testMalformedShaFails() throws Exception {
    var req = new CiDtos.Request("not-valid-sha!@#", "main");
    mvc.perform(post("/api/v1/ci/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @DisplayName("3. Valid compliant commit passes with 0 violations and MERGE ALLOWED")
  void testValidCompliantCommitPasses() throws Exception {
    // Setup compliant topology: orders-api (EU) -> payments-api (EU) [PII]
    ServiceNode s1 = new ServiceNode();
    s1.setName("orders-api");
    s1.setRegion("EU");
    s1.setEnvironment("production");
    serviceRepo.save(s1);

    ServiceNode s2 = new ServiceNode();
    s2.setName("payments-api");
    s2.setRegion("EU");
    s2.setEnvironment("production");
    serviceRepo.save(s2);

    DataFlowEdge edge = new DataFlowEdge();
    edge.setSourceServiceId(s1.getId());
    edge.setDestinationServiceId(s2.getId());
    edge.setDataClasses(Set.of("PII"));
    edgeRepo.save(edge);

    var req = new CiDtos.Request("HEAD", "main");
    mvc.perform(post("/api/v1/ci/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PASSED"))
        .andExpect(jsonPath("$.result").value("PASS"))
        .andExpect(jsonPath("$.passed").value(true))
        .andExpect(jsonPath("$.violationCount").value(0))
        .andExpect(jsonPath("$.violations").isEmpty())
        .andExpect(jsonPath("$.changedFiles").isArray())
        .andExpect(jsonPath("$.author").isNotEmpty())
        .andExpect(jsonPath("$.commitMessage").isNotEmpty());
  }

  @Test
  @DisplayName("4. Valid violating commit is BLOCKED with rich violation details & howToFix")
  void testViolatingCommitIsBlockedWithDetails() throws Exception {
    // Setup violating topology: orders-api (EU) -> analytics-api (US) [PII]
    ServiceNode s1 = new ServiceNode();
    s1.setName("orders-api");
    s1.setRegion("EU");
    s1.setEnvironment("production");
    serviceRepo.save(s1);

    ServiceNode s2 = new ServiceNode();
    s2.setName("analytics-api");
    s2.setRegion("US");
    s2.setEnvironment("production");
    serviceRepo.save(s2);

    DataFlowEdge edge = new DataFlowEdge();
    edge.setSourceServiceId(s1.getId());
    edge.setDestinationServiceId(s2.getId());
    edge.setDataClasses(Set.of("PII"));
    edgeRepo.save(edge);

    var req = new CiDtos.Request("HEAD", "main");
    mvc.perform(post("/api/v1/ci/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"))
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.passed").value(false))
        .andExpect(jsonPath("$.violationCount").value(1))
        .andExpect(jsonPath("$.violations[0].sourceService").value("orders-api"))
        .andExpect(jsonPath("$.violations[0].destinationService").value("analytics-api"))
        .andExpect(jsonPath("$.violations[0].sourceRegion").value("EU"))
        .andExpect(jsonPath("$.violations[0].destinationRegion").value("US"))
        .andExpect(jsonPath("$.violations[0].policyCode").value("EU-PII-001"))
        .andExpect(jsonPath("$.violations[0].policyName").isNotEmpty())
        .andExpect(jsonPath("$.violations[0].reason").isNotEmpty())
        .andExpect(jsonPath("$.violations[0].whatChanged").isNotEmpty())
        .andExpect(jsonPath("$.violations[0].howToFix").isNotEmpty())
        .andExpect(jsonPath("$.violations[0].visualFlow").isArray())
        .andExpect(jsonPath("$.violations[0].beforeAfter").isNotEmpty());
  }

  @Test
  @DisplayName("5. Paginated scan history & single scan lookup")
  void testScanHistoryAndLookup() throws Exception {
    var req = new CiDtos.Request("HEAD", "main");
    String res = mvc.perform(post("/api/v1/ci/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    CiDtos.Response created = mapper.readValue(res, CiDtos.Response.class);
    assertThat(created.id()).isNotNull();

    // List scans
    mvc.perform(get("/api/v1/ci/scans")
            .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(created.id()));

    // Get single scan
    mvc.perform(get("/api/v1/ci/scans/" + created.id())
            .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.id()))
        .andExpect(jsonPath("$.branch").value("main"));
  }
}
