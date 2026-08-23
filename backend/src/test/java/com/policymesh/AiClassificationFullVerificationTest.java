package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ai.AIClassification;
import com.policymesh.ai.AIClassificationRepository;
import com.policymesh.ai.AiDtos;
import com.policymesh.auth.JwtService;
import com.policymesh.auth.Role;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.enforcement.EnforcementDtos;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiClassificationFullVerificationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private AIClassificationRepository aiRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private PolicyRepository policyRepo;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder encoder;

  private String adminToken;
  private String complianceToken;
  private String engineerToken;
  private String viewerToken;

  @BeforeEach
  void setup() {
    aiRepo.deleteAll();
    userRepo.deleteAll();
    policyRepo.deleteAll();

    Policy p = new Policy();
    p.setPolicyCode("EU-PII-001");
    p.setName("EU Personal Data Residency");
    p.setJurisdiction("EU");
    p.setDataClass("PII");
    p.setAllowedRegions(Set.of("EU"));
    p.setDeniedRegions(Set.of("US"));
    p.setStatus(PolicyStatus.ACTIVE);
    policyRepo.save(p);

    adminToken = "Bearer " + jwtService.issue(createUser("admin@test.io", Role.ADMIN));
    complianceToken = "Bearer " + jwtService.issue(createUser("compliance@test.io", Role.COMPLIANCE_OFFICER));
    engineerToken = "Bearer " + jwtService.issue(createUser("dev@test.io", Role.ENGINEER));
    viewerToken = "Bearer " + jwtService.issue(createUser("viewer@test.io", Role.VIEWER));
  }

  private User createUser(String email, Role role) {
    User u = new User();
    u.setEmail(email);
    u.setPasswordHash(encoder.encode("secret123"));
    u.setRole(role);
    return userRepo.save(u);
  }

  @Test
  @DisplayName("Full E2E AI Classification Flow: Classify -> PENDING -> APPROVE -> Policy Engine Enforcement")
  void testFullClassificationAndApprovalFlow() throws Exception {
    // 1. Classify customer_email
    var req = new AiDtos.Request("customer_email", "alice@example.com");
    String responseJson = mvc.perform(post("/api/v1/ai/classify")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fieldName").value("customer_email"))
        .andExpect(jsonPath("$.classification").value("PII"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.requiresHumanApproval").value(true))
        .andReturn().getResponse().getContentAsString();

    AiDtos.Response classified = mapper.readValue(responseJson, AiDtos.Response.class);
    Long id = classified.id();
    assertThat(id).isNotNull();

    // 2. Verify Database State after classification
    AIClassification inDb = aiRepo.findById(id).orElseThrow();
    assertThat(inDb.getStatus()).isEqualTo("PENDING");
    assertThat(inDb.getReviewedBy()).isNull();

    // 3. Security: Viewer and Engineer cannot approve
    mvc.perform(post("/api/v1/ai/classify/" + id + "/approve")
            .header("Authorization", viewerToken))
        .andExpect(status().isForbidden());

    mvc.perform(post("/api/v1/ai/classify/" + id + "/approve")
            .header("Authorization", engineerToken))
        .andExpect(status().isForbidden());

    // 4. Compliance Officer approves
    mvc.perform(post("/api/v1/ai/classify/" + id + "/approve")
            .header("Authorization", complianceToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.reviewedBy").value("compliance@test.io"))
        .andExpect(jsonPath("$.requiresHumanApproval").value(false));

    // 5. Verify Database State after approval
    AIClassification approvedInDb = aiRepo.findById(id).orElseThrow();
    assertThat(approvedInDb.getStatus()).isEqualTo("APPROVED");
    assertThat(approvedInDb.getReviewedBy()).isEqualTo("compliance@test.io");

    // 6. Test Idempotency: second approve returns 200
    mvc.perform(post("/api/v1/ai/classify/" + id + "/approve")
            .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    // 7. Test Conflict: cannot reject already approved classification
    mvc.perform(post("/api/v1/ai/classify/" + id + "/reject")
            .header("Authorization", adminToken))
        .andExpect(status().isConflict());

    // 8. Policy Engine Integration: runtime check using fields = ["customer_email"]
    // EU -> US with customer_email (approved PII) -> DENY
    var enforceReqDeny = new EnforcementDtos.Request(
        "orders-api", "analytics-api", "EU", "US",
        null, null, null, null, null,
        Set.of("customer_email"), null, null);

    mvc.perform(post("/api/v1/enforce/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(enforceReqDeny)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY"))
        .andExpect(jsonPath("$.policyId").value("EU-PII-001"));

    // EU -> EU with customer_email (approved PII) -> ALLOW
    var enforceReqAllow = new EnforcementDtos.Request(
        "orders-api", "payments-api", "EU", "EU",
        null, null, null, null, null,
        Set.of("customer_email"), null, null);

    mvc.perform(post("/api/v1/enforce/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(enforceReqAllow)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("ALLOW"));
  }

  @Test
  @DisplayName("Rejection Flow and Preservation of Audit History")
  void testRejectionAndReclassificationFlow() throws Exception {
    // 1. Classify customer_reference
    var req1 = new AiDtos.Request("customer_reference", "REF-9921");
    String res1Json = mvc.perform(post("/api/v1/ai/classify")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn().getResponse().getContentAsString();

    AiDtos.Response res1 = mapper.readValue(res1Json, AiDtos.Response.class);

    // 2. Reject classification
    mvc.perform(post("/api/v1/ai/classify/" + res1.id() + "/reject")
            .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.reviewedBy").value("admin@test.io"));

    // 3. Database verification: record is preserved with REJECTED
    AIClassification rejectedInDb = aiRepo.findById(res1.id()).orElseThrow();
    assertThat(rejectedInDb.getStatus()).isEqualTo("REJECTED");
    assertThat(rejectedInDb.getReviewedBy()).isEqualTo("admin@test.io");

    // 4. Policy Engine does not treat rejected classification as PII
    var enforceRejected = new EnforcementDtos.Request(
        "orders-api", "payments-api", "EU", "EU",
        null, null, null, null, null,
        Set.of("customer_reference"), null, null);

    mvc.perform(post("/api/v1/enforce/check")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(enforceRejected)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY")); // UNKNOWN data class defaults to safe DENY

    // 5. Reclassify customer_reference -> creates a NEW PENDING record
    var req2 = new AiDtos.Request("customer_reference", "SKU-123");
    String res2Json = mvc.perform(post("/api/v1/ai/classify")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn().getResponse().getContentAsString();

    AiDtos.Response res2 = mapper.readValue(res2Json, AiDtos.Response.class);
    assertThat(res2.id()).isNotEqualTo(res1.id());

    // Both records exist in database
    assertThat(aiRepo.count()).isEqualTo(2);

    // 6. List endpoint returns all classifications for viewer
    mvc.perform(get("/api/v1/ai/classifications")
            .header("Authorization", viewerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Input Validation Security on Field Name and Sample Value")
  void testInputValidation() throws Exception {
    // Malformed field name with script tags
    var badReq1 = new AiDtos.Request("<script>alert(1)</script>", "sample");
    mvc.perform(post("/api/v1/ai/classify")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(badReq1)))
        .andExpect(status().isUnprocessableEntity());

    // Empty field name
    var badReq2 = new AiDtos.Request("", "sample");
    mvc.perform(post("/api/v1/ai/classify")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(badReq2)))
        .andExpect(status().isUnprocessableEntity());
  }
}
