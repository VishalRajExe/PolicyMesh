package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ai.AIClassification;
import com.policymesh.ai.AIClassificationRepository;
import com.policymesh.ai.AiDtos;
import com.policymesh.auth.AuthDtos;
import com.policymesh.auth.JwtService;
import com.policymesh.auth.Role;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.ci.CiDtos;
import com.policymesh.enforcement.DecisionRecord;
import com.policymesh.enforcement.DecisionRepository;
import com.policymesh.enforcement.EnforcementDtos;
import com.policymesh.lineage.LineageRecord;
import com.policymesh.lineage.LineageRepository;
import com.policymesh.lineage.LineageService;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyDtos;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.GraphDtos;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import com.policymesh.settings.SettingsDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterInputOutputAuditTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private UserRepository userRepo;
  @Autowired private PolicyRepository policyRepo;
  @Autowired private ServiceNodeRepository serviceRepo;
  @Autowired private DataFlowEdgeRepository edgeRepo;
  @Autowired private DecisionRepository decisionRepo;
  @Autowired private LineageRepository lineageRepo;
  @Autowired private AIClassificationRepository aiRepo;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder encoder;
  @Autowired private LineageService lineageService;

  private String adminToken;
  private String complianceToken;
  private String engineerToken;
  private String viewerToken;

  @BeforeEach
  void cleanAndSeedUsers() {
    edgeRepo.deleteAll();
    serviceRepo.deleteAll();
    policyRepo.deleteAll();
    decisionRepo.deleteAll();
    lineageRepo.deleteAll();
    aiRepo.deleteAll();
    userRepo.deleteAll();

    adminToken = "Bearer " + jwtService.issue(createUser("admin@policymesh.io", Role.ADMIN));
    complianceToken = "Bearer " + jwtService.issue(createUser("compliance@policymesh.io", Role.COMPLIANCE_OFFICER));
    engineerToken = "Bearer " + jwtService.issue(createUser("engineer@policymesh.io", Role.ENGINEER));
    viewerToken = "Bearer " + jwtService.issue(createUser("viewer@policymesh.io", Role.VIEWER));
  }

  private User createUser(String email, Role role) {
    User u = new User();
    u.setEmail(email);
    u.setPasswordHash(encoder.encode("password123"));
    u.setRole(role);
    return userRepo.save(u);
  }

  @Nested
  @DisplayName("1. Authentication & Security Audit")
  class AuthAudit {
    @Test
    void testRegistrationValidation() throws Exception {
      // 1. Empty email -> 422
      var bad1 = new AuthDtos.Register("", "password123", Role.ENGINEER, "John Doe");
      mvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(bad1)))
          .andExpect(status().isUnprocessableEntity());

      // 2. Short password (< 8 chars) -> 422
      var bad2 = new AuthDtos.Register("test@policymesh.io", "short", Role.ENGINEER, "John");
      mvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(bad2)))
          .andExpect(status().isUnprocessableEntity());

      // 3. Duplicate email -> 409 Conflict
      var valid = new AuthDtos.Register("newuser@policymesh.io", "securepass123", Role.ENGINEER, "New User");
      mvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(valid)))
          .andExpect(status().isCreated());

      mvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(valid)))
          .andExpect(status().isConflict());

      // 4. Bad credentials -> 401
      var badLogin = new AuthDtos.Login("newuser@policymesh.io", "wrongpassword");
      mvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(badLogin)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("2. Policy Lifecycle & Region Validation Audit")
  class PolicyAudit {
    @Test
    void testPolicyValidationRules() throws Exception {
      // 1. Contradictory regions (allowed and denied overlap) -> 422
      var contradictory = new PolicyDtos.Request(
          "EU-PII-001", "EU Personal Data Residency", "EU", "PII",
          Set.of("EU", "US"), Set.of("US"), PolicyStatus.ACTIVE);

      mvc.perform(post("/api/v1/policies")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(contradictory)))
          .andExpect(status().isUnprocessableEntity());

      // 2. Valid creation
      var valid = new PolicyDtos.Request(
          "EU-PII-001", "EU Personal Data Residency", "EU", "PII",
          Set.of("EU"), Set.of("US"), PolicyStatus.ACTIVE);

      mvc.perform(post("/api/v1/policies")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(valid)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.policyCode").value("EU-PII-001"));

      // 3. Duplicate code -> 409 Conflict
      mvc.perform(post("/api/v1/policies")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(valid)))
          .andExpect(status().isConflict());

      // 4. Viewer cannot create -> 403 Forbidden
      mvc.perform(post("/api/v1/policies")
              .header("Authorization", viewerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(valid)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("3. Services & Data Flow Topology Audit")
  class ServiceGraphAudit {
    @Test
    void testServiceAndFlowValidation() throws Exception {
      // 1. Create service with natural language description (including punctuation)
      var s1Req = new GraphDtos.ServiceRequest(
          "orders-api", "EU", "eu-west-1", "production",
          "Handles user orders, checkout flows, and payment triggers.");

      String s1Json = mvc.perform(post("/api/v1/services")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(s1Req)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("orders-api"))
          .andReturn().getResponse().getContentAsString();

      GraphDtos.ServiceResponse s1 = mapper.readValue(s1Json, GraphDtos.ServiceResponse.class);

      var s2Req = new GraphDtos.ServiceRequest(
          "analytics-api", "US", "us-east-1", "production",
          "Consolidates analytics telemetry and usage metrics.");

      String s2Json = mvc.perform(post("/api/v1/services")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(s2Req)))
          .andExpect(status().isCreated())
          .andReturn().getResponse().getContentAsString();

      GraphDtos.ServiceResponse s2 = mapper.readValue(s2Json, GraphDtos.ServiceResponse.class);

      // 2. Self-loop flow edge (source == destination) -> 422 Unprocessable
      var selfLoop = new GraphDtos.EdgeRequest(s1.id(), s1.id(), Set.of("PII"));
      mvc.perform(post("/api/v1/edges")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(selfLoop)))
          .andExpect(status().isUnprocessableEntity());

      // 3. Non-existent service ID -> 422 Unprocessable
      var nonExistent = new GraphDtos.EdgeRequest(s1.id(), 999999L, Set.of("PII"));
      mvc.perform(post("/api/v1/edges")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(nonExistent)))
          .andExpect(status().isUnprocessableEntity());

      // 4. Valid flow edge
      var validEdge = new GraphDtos.EdgeRequest(s1.id(), s2.id(), Set.of("PII"));
      mvc.perform(post("/api/v1/edges")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(validEdge)))
          .andExpect(status().isCreated());

      // 5. Re-evaluate graph endpoint returns real-time compliance results
      mvc.perform(post("/api/v1/graph/re-evaluate")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.checkedEdges").value(1))
          .andExpect(jsonPath("$.totalFlows").value(1));
    }
  }

  @Nested
  @DisplayName("4. Runtime Enforcement & Lineage Ledger Audit")
  class RuntimeAndLineageAudit {
    @Test
    void testEnforcementAndLineageVerification() throws Exception {
      // Seed active policy
      Policy p = new Policy();
      p.setPolicyCode("EU-PII-001");
      p.setName("EU Personal Data Residency");
      p.setJurisdiction("EU");
      p.setDataClass("PII");
      p.setAllowedRegions(Set.of("EU"));
      p.setDeniedRegions(Set.of("US"));
      p.setStatus(PolicyStatus.ACTIVE);
      policyRepo.save(p);

      // Seed services
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

      // 1. EU -> US with PII -> DENY
      var enforceReq = new EnforcementDtos.Request(
          "orders-api", "analytics-api", "EU", "US",
          Set.of("PII"), null, null, null, null, null, null, null);

      mvc.perform(post("/api/v1/enforce/check")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(enforceReq)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.decision").value("DENY"))
          .andExpect(jsonPath("$.policyId").value("EU-PII-001"))
          .andExpect(jsonPath("$.lineageId").isNotEmpty())
          .andExpect(jsonPath("$.lineageHash").isNotEmpty());

      // 2. Verify Lineage chain integrity
      mvc.perform(get("/api/v1/lineage/verify")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.valid").value(true))
          .andExpect(jsonPath("$.recordsChecked").value(1));
    }
  }

  @Nested
  @DisplayName("5. CI Compliance Check & Malformed Input Handling")
  class CiAudit {
    @Test
    void testCiCheckValidation() throws Exception {
      // 1. Malformed commit SHA -> 422 Unprocessable Entity
      var badSha = new CiDtos.Request("not-a-valid-sha!@#", "main");
      mvc.perform(post("/api/v1/ci/check")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(badSha)))
          .andExpect(status().isUnprocessableEntity());

      // 2. Empty branch -> 422
      var emptyBranch = new CiDtos.Request("a1b2c3d4e5f6", "");
      mvc.perform(post("/api/v1/ci/check")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(emptyBranch)))
          .andExpect(status().isUnprocessableEntity());

      // 3. Dynamic branch discovery returns real branches list
      mvc.perform(get("/api/v1/ci/branches")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }
  }

  @Nested
  @DisplayName("6. Settings & RFC 7807 Error Protocol Audit")
  class SettingsAndRfc7807Audit {
    @Test
    void testSettingsAndRfc7807ErrorFormat() throws Exception {
      // 1. System status telemetry
      mvc.perform(get("/api/v1/settings/system")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.api.status").value("HEALTHY"));

      // 2. Password change with wrong current password -> 400 Bad Request
      var badPass = new SettingsDtos.ChangePasswordRequest("wrongcurrent", "newpassword123");
      mvc.perform(post("/api/v1/settings/change-password")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(badPass)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("https://policymesh/errors/validation"))
          .andExpect(jsonPath("$.title").value("Validation failed"));

      // 3. Non-existent endpoint -> 404 RFC 7807 problem details
      mvc.perform(get("/api/v1/non-existent-endpoint")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.type").value("https://policymesh/errors/not-found"));
    }
  }
}
