package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
    "spring.datasource.url=jdbc:h2:mem:userreportstest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "management.health.redis.enabled=false",
    "management.health.kafka.enabled=false",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false"})
@AutoConfigureMockMvc
class UserAndReportsSecurityTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;

  private String adminToken;
  private String viewerToken;

  @BeforeEach
  void setupTokens() throws Exception {
    adminToken = obtainToken("admin_test@policymesh.io", "adminPass123!", "ADMIN");
    viewerToken = obtainToken("viewer_test@policymesh.io", "viewerPass123!", "VIEWER");
  }

  private String obtainToken(String email, String password, String role) throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", email, password, role)));
    MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  @Test
  void testUserManagementAdminAndRbac() throws Exception {
    // 1. Roles endpoint accessible to authenticated users
    mvc.perform(get("/api/v1/users/roles").header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].role").value("ADMIN"))
        .andExpect(jsonPath("$[1].role").value("COMPLIANCE_OFFICER"));

    // 2. Admin can create user
    MvcResult createResult = mvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"eng_new@policymesh.io\",\"password\":\"securePass123!\",\"role\":\"ENGINEER\",\"status\":\"ACTIVE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("eng_new@policymesh.io"))
        .andExpect(jsonPath("$.role").value("ENGINEER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andReturn();

    long createdId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    // 3. Viewer cannot create user (403 Forbidden)
    mvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + viewerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"hacker@policymesh.io\",\"password\":\"securePass123!\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());

    // 4. Admin can list users
    mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    // 5. Admin can update user role/status
    mvc.perform(put("/api/v1/users/" + createdId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"COMPLIANCE_OFFICER\",\"status\":\"INACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    // 6. Viewer cannot delete user
    mvc.perform(delete("/api/v1/users/" + createdId).header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isForbidden());

    // 7. Admin can delete created user
    mvc.perform(delete("/api/v1/users/" + createdId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void testComplianceReportAndCsvExport() throws Exception {
    // 1. Get Compliance Report JSON
    mvc.perform(get("/api/v1/reports/compliance").header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.generatedAt").isNotEmpty())
        .andExpect(jsonPath("$.summary.complianceScore").isNumber())
        .andExpect(jsonPath("$.lineageStatus.algorithm").value("SHA-256"));

    // 2. Export Compliance CSV
    mvc.perform(get("/api/v1/reports/export/csv").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"policymesh-compliance-report.csv\""))
        .andExpect(content().contentTypeCompatibleWith("text/csv"));
  }

  @Test
  void testSettingsProfileAndSystemDiagnostics() throws Exception {
    // 1. Get Profile
    mvc.perform(get("/api/v1/settings/profile").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin_test@policymesh.io"))
        .andExpect(jsonPath("$.role").value("ADMIN"));

    // 2. Change password
    mvc.perform(post("/api/v1/settings/change-password")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"adminPass123!\",\"newPassword\":\"newAdminPass456!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // Login with new password
    mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin_test@policymesh.io\",\"password\":\"newAdminPass456!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());

    // 3. System diagnostics
    mvc.perform(get("/api/v1/settings/system").header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.api.status").value("HEALTHY"))
        .andExpect(jsonPath("$.governanceEngine.enforcementMode").value("STRICT_ENFORCE"))
        .andExpect(jsonPath("$.governanceEngine.lineageAlgorithm").value("SHA-256"));
  }
}
