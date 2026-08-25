package com.policymesh.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class GitHubWebhookController {
  private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);
  private static final String ZERO_SHA = "0000000000000000000000000000000000000000";

  private final GitHubWebhookVerifier verifier;
  private final WebhookDeliveryRepository deliveryRepository;
  private final GitHubWebhookAsyncService asyncService;
  private final ObjectMapper mapper;

  public GitHubWebhookController(
      GitHubWebhookVerifier verifier,
      WebhookDeliveryRepository deliveryRepository,
      GitHubWebhookAsyncService asyncService,
      ObjectMapper mapper
  ) {
    this.verifier = verifier;
    this.deliveryRepository = deliveryRepository;
    this.asyncService = asyncService;
    this.mapper = mapper;
  }

  @PostMapping({"/api/webhooks/github", "/api/v1/webhooks/github"})
  public ResponseEntity<Map<String, Object>> handleWebhook(
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
      @RequestHeader(value = "X-GitHub-Event", defaultValue = "push") String eventType,
      @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryIdHeader,
      @RequestBody(required = false) byte[] payloadBytes
  ) {
    String deliveryId = (deliveryIdHeader != null && !deliveryIdHeader.isBlank())
        ? deliveryIdHeader.trim()
        : UUID.randomUUID().toString();

    // 1. Cryptographic HMAC-SHA256 Signature Verification
    if (!verifier.verify(signature, payloadBytes)) {
      log.warn("Rejected unauthorized GitHub webhook request [deliveryId: {}]", deliveryId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
          "status", "ERROR",
          "error", "Unauthorized: Invalid or missing X-Hub-Signature-256 signature."
      ));
    }

    // 2. Replay Protection & Idempotency Check
    if (deliveryRepository.existsByDeliveryId(deliveryId)) {
      log.info("Replay detected for webhook deliveryId: {}. Skipping duplicate execution.", deliveryId);
      return ResponseEntity.ok(Map.of(
          "status", "ALREADY_PROCESSED",
          "deliveryId", deliveryId,
          "message", "This delivery has already been processed or is currently in progress."
      ));
    }

    // 3. Handle 'ping' event (sent upon webhook creation in GitHub)
    if ("ping".equalsIgnoreCase(eventType)) {
      log.info("Received GitHub webhook 'ping' event [deliveryId: {}]", deliveryId);
      WebhookDelivery delivery = new WebhookDelivery(deliveryId, "ping", null, null, null, "github");
      delivery.setStatus("COMPLETED");
      delivery.setSummary("GitHub Webhook ping acknowledged successfully.");
      deliveryRepository.save(delivery);

      return ResponseEntity.ok(Map.of(
          "status", "PONG",
          "deliveryId", deliveryId,
          "message", "PolicyMesh GitHub webhook is active and verified."
      ));
    }

    // 4. Handle 'push' event
    if ("push".equalsIgnoreCase(eventType)) {
      if (payloadBytes == null || payloadBytes.length == 0) {
        return ResponseEntity.badRequest().body(Map.of("error", "Empty payload for push event"));
      }

      String branch = "main";
      String commitSha = null;
      String repository = null;
      String sender = "github";

      try {
        JsonNode root = mapper.readTree(payloadBytes);

        if (root.has("ref")) {
          String rawRef = root.get("ref").asText();
          branch = rawRef.startsWith("refs/heads/") ? rawRef.substring("refs/heads/".length()) : rawRef;
        }

        if (root.has("after") && !root.get("after").asText().isBlank()) {
          commitSha = root.get("after").asText();
        } else if (root.has("head_commit") && root.get("head_commit").has("id")) {
          commitSha = root.get("head_commit").get("id").asText();
        }

        if (root.has("repository") && root.get("repository").has("full_name")) {
          repository = root.get("repository").get("full_name").asText();
        }

        if (root.has("sender") && root.get("sender").has("login")) {
          sender = root.get("sender").get("login").asText();
        } else if (root.has("head_commit") && root.get("head_commit").has("author") && root.get("head_commit").get("author").has("name")) {
          sender = root.get("head_commit").get("author").get("name").asText();
        }
      } catch (Exception e) {
        log.warn("Failed parsing JSON payload for delivery {}: {}", deliveryId, e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed JSON payload"));
      }

      // Ignore branch deletion events (after = 0000000000000000000000000000000000000000)
      if (commitSha == null || ZERO_SHA.equals(commitSha)) {
        WebhookDelivery delivery = new WebhookDelivery(deliveryId, "push", repository, branch, commitSha, sender);
        delivery.setStatus("IGNORED");
        delivery.setSummary("Ignored branch deletion or non-commit event.");
        deliveryRepository.save(delivery);

        return ResponseEntity.ok(Map.of(
            "status", "IGNORED",
            "deliveryId", deliveryId,
            "reason", "Branch deletion or empty commit SHA"
        ));
      }

      // 5. Store pending delivery record
      WebhookDelivery delivery = new WebhookDelivery(deliveryId, "push", repository, branch, commitSha, sender);
      deliveryRepository.save(delivery);

      // 6. Schedule asynchronous analysis (non-blocking)
      asyncService.processPushEventAsync(deliveryId, branch, commitSha);

      return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
          "status", "ACCEPTED",
          "deliveryId", deliveryId,
          "commit", commitSha,
          "branch", branch,
          "repository", repository != null ? repository : "unknown",
          "message", "Commit policy compliance scan scheduled in background."
      ));
    }

    // 7. Other unhandled events
    WebhookDelivery delivery = new WebhookDelivery(deliveryId, eventType, null, null, null, "github");
    delivery.setStatus("IGNORED");
    delivery.setSummary("Ignored unhandled event type: " + eventType);
    deliveryRepository.save(delivery);

    return ResponseEntity.ok(Map.of(
        "status", "IGNORED",
        "deliveryId", deliveryId,
        "event", eventType,
        "message", "Event acknowledged but not tracked."
    ));
  }
}