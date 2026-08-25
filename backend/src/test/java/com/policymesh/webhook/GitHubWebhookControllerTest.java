package com.policymesh.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "github.webhook.secret=test-secret-key-for-webhook-tests-12345"
})
class GitHubWebhookControllerTest {

  private static final String SECRET = "test-secret-key-for-webhook-tests-12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WebhookDeliveryRepository deliveryRepository;

  @BeforeEach
  void setUp() {
    deliveryRepository.deleteAll();
  }

  @Test
  void acknowledgesPingEvent() throws Exception {
    byte[] payload = "{\"zen\":\"Design for failure.\"}".getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature(SECRET, payload);
    String deliveryId = UUID.randomUUID().toString();

    mockMvc.perform(post("/api/webhooks/github")
            .header("X-Hub-Signature-256", signature)
            .header("X-GitHub-Event", "ping")
            .header("X-GitHub-Delivery", deliveryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PONG"))
        .andExpect(jsonPath("$.deliveryId").value(deliveryId));
  }

  @Test
  void acceptsValidPushEvent() throws Exception {
    byte[] payload = """
        {
          "ref": "refs/heads/main",
          "after": "1234567890abcdef1234567890abcdef12345678",
          "repository": { "full_name": "VishalRajExe/PolicyMesh" },
          "sender": { "login": "test-dev" }
        }
        """.getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature(SECRET, payload);
    String deliveryId = UUID.randomUUID().toString();

    mockMvc.perform(post("/api/webhooks/github")
            .header("X-Hub-Signature-256", signature)
            .header("X-GitHub-Event", "push")
            .header("X-GitHub-Delivery", deliveryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.deliveryId").value(deliveryId))
        .andExpect(jsonPath("$.branch").value("main"));
  }

  @Test
  void rejectsInvalidSignature() throws Exception {
    byte[] payload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);

    mockMvc.perform(post("/api/webhooks/github")
            .header("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000")
            .header("X-GitHub-Event", "push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"));
  }

  @Test
  void preventsReplayOnDuplicateDeliveryId() throws Exception {
    byte[] payload = "{\"zen\":\"Hello\"}".getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature(SECRET, payload);
    String deliveryId = "test-delivery-id-" + UUID.randomUUID();

    // First request
    mockMvc.perform(post("/api/webhooks/github")
            .header("X-Hub-Signature-256", signature)
            .header("X-GitHub-Event", "ping")
            .header("X-GitHub-Delivery", deliveryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PONG"));

    // Duplicate replay request with same delivery ID
    mockMvc.perform(post("/api/webhooks/github")
            .header("X-Hub-Signature-256", signature)
            .header("X-GitHub-Event", "ping")
            .header("X-GitHub-Delivery", deliveryId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ALREADY_PROCESSED"));
  }
}