package com.policymesh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "management.health.redis.enabled=false",
    "management.health.kafka.enabled=false",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false"})
@AutoConfigureMockMvc
class PolicyApiSecurityTest {
  @Autowired MockMvc mvc;

  @Test
  void registrationHonorsRequestedRoleAndLoginIssuesToken() throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"compliance@example.com\",\"password\":\"a-strong-password\",\"role\":\"COMPLIANCE_OFFICER\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value("compliance@example.com"))
        .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"compliance@example.com\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"compliance@example.com\",\"password\":\"not-correct\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void registrationRejectsDuplicateEmailAndShortPasswords() throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"dup@example.com\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isCreated());
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"dup@example.com\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"short@example.com\",\"password\":\"short\"}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void protectedEndpointsRequireAuthenticationAndValidTokens() throws Exception {
    mvc.perform(get("/api/v1/policies")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/policies").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void roleMatrixIsEnforcedAtEndpointLevel() throws Exception {
    String viewer = registerAndLogin("viewer@example.com", "VIEWER");
    String engineer = registerAndLogin("engineer@example.com", "ENGINEER");
    String officer = registerAndLogin("officer2@example.com", "COMPLIANCE_OFFICER");

    String policy = "{\"policyCode\":\"EU-PII-001\",\"name\":\"EU PII\",\"jurisdiction\":\"EU\",\"dataClass\":\"PII\","
        + "\"allowedRegions\":[\"EU\"],\"deniedRegions\":[\"US\"],\"status\":\"ACTIVE\"}";

    // VIEWER cannot write policies; COMPLIANCE_OFFICER can.
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + viewer)
            .contentType(MediaType.APPLICATION_JSON).content(policy))
        .andExpect(status().isForbidden());
    MvcResult created = mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + officer)
            .contentType(MediaType.APPLICATION_JSON).content(policy))
        .andExpect(status().isCreated()).andReturn();
    long policyId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
        .readTree(created.getResponse().getContentAsString()).path("id").asLong();

    // ENGINEER cannot delete policies or run enforcement... enforcement IS allowed for engineers.
    mvc.perform(delete("/api/v1/policies/" + policyId).header("Authorization", "Bearer " + engineer))
        .andExpect(status().isForbidden());
    // COMPLIANCE_OFFICER cannot run enforcement checks.
    mvc.perform(post("/api/v1/enforce/check").header("Authorization", "Bearer " + officer)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceService\":\"a\",\"destinationService\":\"b\",\"sourceRegion\":\"EU\",\"destinationRegion\":\"US\",\"dataClassTags\":[\"PII\"]}"))
        .andExpect(status().isForbidden());
    // ENGINEER cannot approve AI classifications.
    mvc.perform(post("/api/v1/ai/classify/1/approve").header("Authorization", "Bearer " + engineer))
        .andExpect(status().isForbidden());
    // VIEWER may trigger read-only graph validation.
    mvc.perform(post("/api/v1/graph/validate").header("Authorization", "Bearer " + viewer))
        .andExpect(status().isOk());
    // ENGINEER may run CI checks.
    mvc.perform(post("/api/v1/ci/check").header("Authorization", "Bearer " + engineer)
            .contentType(MediaType.APPLICATION_JSON).content("{\"commitHash\":\"abc\",\"branch\":\"main\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void securityHeadersArePresentOnResponses() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().exists("Content-Security-Policy"));
  }

  @Test
  void inputValidationRejectsSqlInjectionAndXssPayloads() throws Exception {
    String token = registerAndLogin("admin-sec@example.com", "ADMIN");

    // SQL Injection attempt in service name
    mvc.perform(post("/api/v1/services").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"orders'; DROP TABLE users;--\",\"region\":\"EU\",\"environment\":\"production\"}"))
        .andExpect(status().isUnprocessableEntity());

    // XSS attempt in policyCode
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"policyCode\":\"<script>alert(1)</script>\",\"name\":\"XSS Policy\",\"jurisdiction\":\"EU\",\"dataClass\":\"PII\",\"allowedRegions\":[\"EU\"]}"))
        .andExpect(status().isUnprocessableEntity());

    // Path traversal attempt in service name
    mvc.perform(post("/api/v1/services").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"../../etc/passwd\",\"region\":\"EU\",\"environment\":\"production\"}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void bruteForceLoginTriggersRateLimit() throws Exception {
    String victim = "victim@example.com";
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + victim + "\",\"password\":\"a-strong-password\",\"role\":\"ENGINEER\"}"));

    // Attempt 10 bad logins
    for (int i = 0; i < 10; i++) {
      mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
              .content("{\"email\":\"" + victim + "\",\"password\":\"wrong-pass-" + i + "\"}"))
          .andExpect(status().isUnauthorized());
    }

    // 11th attempt must be rejected with 429 TOO_MANY_REQUESTS
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + victim + "\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  private String registerAndLogin(String email, String role) throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"a-strong-password\",\"role\":\"" + role + "\"}"));
    MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isOk()).andReturn();
    return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
        .readTree(login.getResponse().getContentAsString()).path("token").asText();
  }
}
