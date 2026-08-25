package com.policymesh.settings;

import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.common.ApiException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final DataSource dataSource;
  private final org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisProvider;
  private final String aiServiceUrl;
  private final String aiMode;
  private final boolean redisEnabled;
  private final long redisTtl;
  private final boolean kafkaEnabled;

  public SettingsController(
      UserRepository users,
      PasswordEncoder encoder,
      DataSource dataSource,
      org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisProvider,
      @Value("${policymesh.ai.service-url:http://localhost:8000}") String aiServiceUrl,
      @Value("${policymesh.ai.mode:remote}") String aiMode,
      @Value("${policymesh.redis.enabled:true}") boolean redisEnabled,
      @Value("${policymesh.redis.ttl-seconds:600}") long redisTtl,
      @Value("${policymesh.kafka.enabled:false}") boolean kafkaEnabled
  ) {
    this.users = users;
    this.encoder = encoder;
    this.dataSource = dataSource;
    this.redisProvider = redisProvider;
    this.aiServiceUrl = aiServiceUrl;
    this.aiMode = aiMode;
    this.redisEnabled = redisEnabled;
    this.redisTtl = redisTtl;
    this.kafkaEnabled = kafkaEnabled;
  }

  @GetMapping("/profile")
  public SettingsDtos.ProfileResponse getProfile(Principal principal) {
    User user = getAuthenticatedUser(principal);
    return new SettingsDtos.ProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getRole().name(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }

  @PostMapping("/change-password")
  public Map<String, String> changePassword(@Valid @RequestBody SettingsDtos.ChangePasswordRequest req, Principal principal) {
    User user = getAuthenticatedUser(principal);
    if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
      throw ApiException.badRequest("Current password does not match");
    }
    if (req.currentPassword().equals(req.newPassword())) {
      throw ApiException.badRequest("New password must be different from current password");
    }
    user.setPasswordHash(encoder.encode(req.newPassword()));
    users.save(user);
    return Map.of("status", "success", "message", "Password changed successfully");
  }

  @GetMapping("/system")
  public SettingsDtos.SystemSettings getSystemSettings() {
    // Check DB
    boolean dbUp = false;
    try (var conn = dataSource.getConnection()) {
      dbUp = conn.isValid(2);
    } catch (Exception ignored) {}

    // Check Redis
    String redisStatus = "LOCAL_FALLBACK";
    String redisDetails = "In-memory fast cache active (TTL: " + redisTtl + "s)";
    if (redisEnabled) {
      try {
        StringRedisTemplate template = redisProvider.getIfAvailable();
        if (template != null) {
          String pong = template.getConnectionFactory().getConnection().ping();
          if ("PONG".equalsIgnoreCase(pong)) {
            redisStatus = "CONNECTED";
            redisDetails = "L2 Redis connected with L1 in-memory fast-path (TTL: " + redisTtl + "s)";
          }
        }
      } catch (Exception e) {
        redisStatus = "LOCAL_FALLBACK";
        redisDetails = "Redis connection offline; resilient in-memory fallback active";
      }
    }

    // Check AI Service
    String aiStatus = "LOCAL_HEURISTIC";
    String aiDetails = "Local deterministic classification active";
    try {
      RestClient client = RestClient.builder().baseUrl(aiServiceUrl).build();
      var resp = client.get().uri("/health").retrieve().toBodilessEntity();
      if (resp.getStatusCode().is2xxSuccessful()) {
        aiStatus = "HEALTHY";
        aiDetails = "Remote FastAPI service connected (" + aiMode + " mode)";
      }
    } catch (Exception ignored) {}

    // Components
    SettingsDtos.ComponentStatus api = new SettingsDtos.ComponentStatus(
        "PolicyMesh REST API",
        "HEALTHY",
        "Spring Boot 3.3 (Java 21)",
        "Stateless JWT RBAC, RFC 7807 problem details"
    );

    SettingsDtos.ComponentStatus db = new SettingsDtos.ComponentStatus(
        "Relational Store",
        dbUp ? "HEALTHY" : "DEGRADED",
        "MySQL 8.4",
        dbUp ? "HikariCP connection pool active, schema synchronized" : "Database connection issues detected"
    );

    SettingsDtos.ComponentStatus redis = new SettingsDtos.ComponentStatus(
        "Policy Cache Layer",
        redisStatus,
        "Redis 7 / In-Memory",
        redisDetails
    );

    SettingsDtos.ComponentStatus kafka = new SettingsDtos.ComponentStatus(
        "Async Event Stream",
        kafkaEnabled ? "HEALTHY" : "STANDBY",
        "Apache Kafka 3.8",
        kafkaEnabled ? "Producer configured on policy & lineage topics" : "Kafka opt-in channel currently disabled"
    );

    SettingsDtos.ComponentStatus ai = new SettingsDtos.ComponentStatus(
        "AI Sensitivity Service",
        aiStatus,
        "FastAPI / Python 3.13",
        aiDetails
    );

    Map<String, Object> governanceEngine = Map.of(
        "enforcementMode", "STRICT_ENFORCE",
        "defaultDecision", "DENY",
        "lineageAlgorithm", "SHA-256",
        "policyCompiler", "Graph Topology & Jurisdictional DSL",
        "humanReviewRequired", true
    );

    return new SettingsDtos.SystemSettings(
        Instant.now(),
        api,
        db,
        redis,
        kafka,
        ai,
        governanceEngine
    );
  }

  private User getAuthenticatedUser(Principal principal) {
    if (principal == null) {
      throw ApiException.unauthorized("Authentication required");
    }
    return users.findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> ApiException.unauthorized("User account not found"));
  }
}
