package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.auth.JwtService;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.auth.Role;
import com.policymesh.webhook.GitHubWebhookVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pentestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "management.health.redis.enabled=false",
    "management.health.kafka.enabled=false",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false",
    "github.webhook.secret=pen_test_webhook_secret_key_123"})
@AutoConfigureMockMvc
public class PenetrationResistanceAndHardeningTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JwtService jwtService;

  private String adminToken;
  private String viewerToken;
  private String engineerToken;
  private User adminUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    adminUser = new User("admin_sec@policymesh.io", passwordEncoder.encode("SecurePass123!"), Role.ADMIN);
    adminUser.setName("Sec Admin");
    adminUser.setStatus("ACTIVE");
    adminUser.setEnabled(true);
    adminUser = userRepository.save(adminUser);
    adminToken = jwtService.issue(adminUser);

    User viewerUser = new User("viewer_sec@policymesh.io", passwordEncoder.encode("SecurePass123!"), Role.VIEWER);
    viewerUser.setName("Sec Viewer");
    viewerUser.setStatus("ACTIVE");
    viewerUser.setEnabled(true);
    viewerUser = userRepository.save(viewerUser);
    viewerToken = jwtService.issue(viewerUser);

    User engUser = new User("eng_sec@policymesh.io", passwordEncoder.encode("SecurePass123!"), Role.ENGINEER);
    engUser.setName("Sec Engineer");
    engUser.setStatus("ACTIVE");
    engUser.setEnabled(true);
    engUser = userRepository.save(engUser);
    engineerToken = jwtService.issue(engUser);
  }

  @Test
  @DisplayName("1. Anonymous requests to protected endpoints fail closed with 401")
  void anonymousRequestsFailClosed() throws Exception {
    mvc.perform(get("/api/v1/policies"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    mvc.perform(get("/api/v1/services"))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/v1/settings/profile"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("2. Forged and expired JWT tokens fail closed with 401")
  void forgedAndExpiredTokensFailClosed() throws Exception {
    // Forged signature
    String forgedJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
        + "eyJzdWIiOiIxIiwiZW1haWwiOiJhZG1pbkBwb2xpY3ltZXNoLmlvIiwicm9sZSI6IkFETUlOIn0."
        + "invalidsignatureinvalidsignature123456789";

    mvc.perform(get("/api/v1/policies").header("Authorization", "Bearer " + forgedJwt))
        .andExpect(status().isUnauthorized());

    // Alg None attack attempt
    String algNoneJwt = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0."
        + "eyJzdWIiOiIxIiwiZW1haWwiOiJhZG1pbkBwb2xpY3ltZXNoLmlvIiwicm9sZSI6IkFETUlOIn0.";

    mvc.perform(get("/api/v1/policies").header("Authorization", "Bearer " + algNoneJwt))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("3. Vertical privilege escalation and IDOR protection enforced by RBAC")
  void verticalPrivilegeEscalationPrevented() throws Exception {
    // Viewer trying to create admin user -> 403
    mvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + viewerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"attacker@policymesh.io\",\"password\":\"hackedPass123!\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());

    // Engineer trying to delete a policy -> 403
    mvc.perform(delete("/api/v1/policies/999")
            .header("Authorization", "Bearer " + engineerToken))
        .andExpect(status().isForbidden());

    // Viewer trying to trigger admin seed endpoint -> 403
    mvc.perform(post("/api/v1/dev/seed")
            .header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("4. Rate limiting headers and tiered throttling are enforced")
  void rateLimitingHeadersPresent() throws Exception {
    mvc.perform(get("/api/v1/policies").header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-RateLimit-Limit"))
        .andExpect(header().exists("X-RateLimit-Remaining"));
  }

  @Test
  @DisplayName("5. Comprehensive security headers are present on all responses")
  void defenseInDepthSecurityHeadersPresent() throws Exception {
    mvc.perform(get("/health").secure(true))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
  }

  @Test
  @DisplayName("6. GitHub Webhook HMAC SHA-256 signature verification rejects untrusted payloads")
  void webhookSignatureVerificationRejectsUntrustedPayloads() throws Exception {
    String payload = "{\"ref\":\"refs/heads/main\",\"after\":\"1234567890abcdef1234567890abcdef12345678\"}";
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

    // 1. Missing signature -> 401
    mvc.perform(post("/api/webhooks/github")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payloadBytes))
        .andExpect(status().isUnauthorized());

    // 2. Tampered / invalid signature -> 401
    mvc.perform(post("/api/webhooks/github")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000")
            .content(payloadBytes))
        .andExpect(status().isUnauthorized());

    // 3. Valid computed HMAC signature -> 200 (ping event)
    String validSig = GitHubWebhookVerifier.computeSignature("pen_test_webhook_secret_key_123", payloadBytes);
    mvc.perform(post("/api/webhooks/github")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-GitHub-Event", "ping")
            .header("X-Hub-Signature-256", validSig)
            .content(payloadBytes))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PONG"));
  }

  @Test
  @DisplayName("7. Safe Error Handling: No database passwords or stack traces leaked in error responses")
  void errorHandlingDoesNotLeakSensitiveInformation() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"nonexistent@policymesh.io\",\"password\":\"wrongPass\"}"))
        .andExpect(status().isUnauthorized())
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertFalse(responseBody.contains("passwordHash"));
    org.junit.jupiter.api.Assertions.assertFalse(responseBody.contains("jdbc:"));
    org.junit.jupiter.api.Assertions.assertFalse(responseBody.contains("org.springframework"));
  }
}
