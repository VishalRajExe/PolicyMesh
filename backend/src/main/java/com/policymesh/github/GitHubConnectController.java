package com.policymesh.github;

import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.common.ApiException;
import com.policymesh.common.EncryptionService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubConnectController {
  private static final Logger log = LoggerFactory.getLogger(GitHubConnectController.class);

  private final GitHubClientService gitHubClient;
  private final OAuthStateManager stateManager;
  private final EncryptionService encryptionService;
  private final GitHubConnectionRepository connectionRepository;
  private final MonitoredRepositoryRepository monitoredRepository;
  private final UserRepository userRepository;
  private final String frontendUrl;
  private final String webhookUrl;
  private final String webhookSecret;

  public GitHubConnectController(
      GitHubClientService gitHubClient,
      OAuthStateManager stateManager,
      EncryptionService encryptionService,
      GitHubConnectionRepository connectionRepository,
      MonitoredRepositoryRepository monitoredRepository,
      UserRepository userRepository,
      @Value("${app.frontend.url:${FRONTEND_URL:http://localhost:5173}}") String frontendUrl,
      @Value("${github.webhook.url:${GITHUB_WEBHOOK_URL:}}") String webhookUrl,
      @Value("${github.webhook.secret:${GITHUB_WEBHOOK_SECRET:}}") String webhookSecret
  ) {
    this.gitHubClient = gitHubClient;
    this.stateManager = stateManager;
    this.encryptionService = encryptionService;
    this.connectionRepository = connectionRepository;
    this.monitoredRepository = monitoredRepository;
    this.userRepository = userRepository;
    this.frontendUrl = frontendUrl != null ? frontendUrl.replaceAll("/+$", "") : "http://localhost:5173";
    this.webhookUrl = webhookUrl != null ? webhookUrl.trim() : "";
    this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
  }

  /**
   * Initiates GitHub OAuth authorization flow for the authenticated user.
   */
  @GetMapping("/connect")
  public ResponseEntity<Map<String, String>> connect(Principal principal) {
    User user = getAuthenticatedUser(principal);

    if (!gitHubClient.isOAuthConfigured()) {
      throw ApiException.badRequest("GitHub OAuth is not configured on this server. Please set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET.");
    }

    String state = stateManager.generateState(user.getId());
    String redirectUriEncoded = URLEncoder.encode(gitHubClient.getRedirectUri(), StandardCharsets.UTF_8);

    String authUrl = String.format(
        "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=read:user,repo&state=%s&prompt=consent",
        gitHubClient.getClientId(),
        redirectUriEncoded,
        state
    );

    return ResponseEntity.ok(Map.of(
        "authorizationUrl", authUrl,
        "state", state
    ));
  }

  /**
   * GitHub OAuth callback redirect handler (publicly reachable, validates state parameter).
   */
  @GetMapping("/callback")
  @Transactional
  public void handleCallback(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "error_description", required = false) String errorDesc,
      HttpServletResponse response
  ) throws IOException {
    if (error != null || code == null || state == null) {
      log.warn("GitHub OAuth callback error: {} ({})", error, errorDesc);
      response.sendRedirect(frontendUrl + "/settings?tab=github&error=" + URLEncoder.encode(errorDesc != null ? errorDesc : "Authorization denied", StandardCharsets.UTF_8));
      return;
    }

    Optional<Long> userIdOpt = stateManager.validateAndConsume(state);
    if (userIdOpt.isEmpty()) {
      log.warn("GitHub OAuth callback rejected: invalid or expired state token");
      response.sendRedirect(frontendUrl + "/settings?tab=github&error=" + URLEncoder.encode("Invalid or expired OAuth state. Please try connecting again.", StandardCharsets.UTF_8));
      return;
    }

    Long userId = userIdOpt.get();
    try {
      GitHubClientService.OAuthTokenResponse tokenRes = gitHubClient.exchangeCode(code);
      GitHubClientService.GitHubUserProfile profile = gitHubClient.fetchUserProfile(tokenRes.accessToken());

      String encryptedToken = encryptionService.encrypt(tokenRes.accessToken());

      GitHubConnection connection = connectionRepository.findByUserId(userId)
          .orElse(new GitHubConnection());

      connection.setUserId(userId);
      connection.setGithubUserId(profile.id());
      connection.setGithubUsername(profile.login());
      connection.setGithubEmail(profile.email());
      connection.setAvatarUrl(profile.avatarUrl());
      connection.setEncryptedAccessToken(encryptedToken);
      connection.setTokenType(tokenRes.tokenType());
      connection.setScope(tokenRes.scope());
      connection.setUpdatedAt(Instant.now());

      connectionRepository.save(connection);

      log.info("Successfully established GitHub connection for userId: {} (GitHub: @{})", userId, profile.login());
      response.sendRedirect(frontendUrl + "/settings?tab=github&status=connected&username=" + URLEncoder.encode(profile.login(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      log.error("Failed completing GitHub OAuth callback for userId {}: {}", userId, e.getMessage(), e);
      response.sendRedirect(frontendUrl + "/settings?tab=github&error=" + URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "OAuth token exchange failed", StandardCharsets.UTF_8));
    }
  }

  /**
   * Returns connection status and safe profile information for the authenticated user.
   */
  @GetMapping("/account")
  public ResponseEntity<Map<String, Object>> getAccount(Principal principal) {
    User user = getAuthenticatedUser(principal);
    Optional<GitHubConnection> connOpt = connectionRepository.findByUserId(user.getId());

    if (connOpt.isEmpty()) {
      return ResponseEntity.ok(Map.of(
          "connected", false,
          "oauthConfigured", gitHubClient.isOAuthConfigured()
      ));
    }

    GitHubConnection conn = connOpt.get();
    return ResponseEntity.ok(Map.of(
        "connected", true,
        "oauthConfigured", gitHubClient.isOAuthConfigured(),
        "username", conn.getGithubUsername(),
        "email", conn.getGithubEmail() != null ? conn.getGithubEmail() : "",
        "avatarUrl", conn.getAvatarUrl() != null ? conn.getAvatarUrl() : "",
        "scope", conn.getScope() != null ? conn.getScope() : "",
        "connectedAt", conn.getConnectedAt().toString()
    ));
  }

  /**
   * Returns accessible GitHub repositories for the authenticated user with monitoring state.
   */
  @GetMapping("/repositories")
  public ResponseEntity<List<Map<String, Object>>> getRepositories(Principal principal) {
    User user = getAuthenticatedUser(principal);
    Optional<String> tokenOpt = gitHubClient.getDecryptedTokenForUser(user.getId());

    if (tokenOpt.isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    List<GitHubClientService.GitHubRepoDto> remoteRepos = gitHubClient.fetchUserRepositories(tokenOpt.get());
    List<MonitoredRepository> localMonitored = monitoredRepository.findByUserIdOrderByRepoFullNameAsc(user.getId());

    Map<String, MonitoredRepository> monitoredMap = new HashMap<>();
    for (MonitoredRepository m : localMonitored) {
      monitoredMap.put(m.getRepoFullName().toLowerCase(), m);
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (GitHubClientService.GitHubRepoDto r : remoteRepos) {
      MonitoredRepository m = monitoredMap.get(r.fullName().toLowerCase());
      boolean isMonitored = m != null && m.isMonitored();

      Map<String, Object> map = new HashMap<>();
      map.put("id", r.id());
      map.put("name", r.name());
      map.put("fullName", r.fullName());
      map.put("ownerLogin", r.ownerLogin());
      map.put("isPrivate", r.isPrivate());
      map.put("defaultBranch", r.defaultBranch());
      map.put("description", r.description());
      map.put("htmlUrl", r.htmlUrl());
      map.put("isMonitored", isMonitored);
      map.put("lastCommitSha", m != null ? m.getLastCommitSha() : null);
      map.put("lastCommitMessage", m != null ? m.getLastCommitMessage() : null);
      map.put("lastScanStatus", m != null ? m.getLastScanStatus() : null);
      map.put("lastScanId", m != null ? m.getLastScanId() : null);
      map.put("lastScanTime", m != null && m.getLastScanTime() != null ? m.getLastScanTime().toString() : null);

      result.add(map);
    }

    return ResponseEntity.ok(result);
  }

  /**
   * Enables monitoring for a specific repository and automatically provisions the GitHub webhook.
   */
  @PostMapping("/repositories/{repoId}/monitor")
  @Transactional
  public ResponseEntity<Map<String, Object>> enableMonitoring(
      @PathVariable Long repoId,
      @RequestBody(required = false) Map<String, String> payload,
      Principal principal
  ) {
    User user = getAuthenticatedUser(principal);

    String fullName = payload != null ? payload.get("fullName") : null;
    String name = payload != null ? payload.get("name") : null;
    String owner = payload != null ? payload.get("ownerLogin") : null;
    String branch = payload != null ? payload.get("defaultBranch") : "main";
    boolean isPrivate = payload != null && Boolean.parseBoolean(payload.get("isPrivate"));

    MonitoredRepository repo = monitoredRepository.findByUserIdAndGithubRepoId(user.getId(), repoId)
        .orElse(new MonitoredRepository(user.getId(), repoId, fullName != null ? fullName : "repo-" + repoId,
            name != null ? name : "repo-" + repoId, owner != null ? owner : user.getEmail(), branch, isPrivate));

    // Auto-provision webhook on GitHub if user has OAuth token
    Optional<String> tokenOpt = gitHubClient.getDecryptedTokenForUser(user.getId());
    if (tokenOpt.isPresent() && repo.getOwnerLogin() != null && repo.getRepoName() != null) {
      gitHubClient.ensureRepositoryWebhook(tokenOpt.get(), repo.getOwnerLogin(), repo.getRepoName(), webhookUrl, webhookSecret)
          .ifPresent(repo::setWebhookHookId);
    }

    repo.setMonitored(true);
    repo.setUpdatedAt(Instant.now());
    monitoredRepository.save(repo);

    log.info("User {} enabled monitoring for repository: {} (Webhook Hook ID: {})",
        user.getEmail(), repo.getRepoFullName(), repo.getWebhookHookId());
    return ResponseEntity.ok(Map.of(
        "status", "SUCCESS",
        "repoId", repoId,
        "isMonitored", true,
        "webhookConfigured", repo.getWebhookHookId() != null,
        "message", "Repository monitoring enabled successfully. Webhook is active."
    ));
  }

  /**
   * Disables monitoring for a specific repository and deactivates GitHub webhook.
   */
  @DeleteMapping("/repositories/{repoId}/monitor")
  @Transactional
  public ResponseEntity<Map<String, Object>> disableMonitoring(
      @PathVariable Long repoId,
      Principal principal
  ) {
    User user = getAuthenticatedUser(principal);

    monitoredRepository.findByUserIdAndGithubRepoId(user.getId(), repoId).ifPresent(repo -> {
      // Remove webhook from GitHub if we created one
      Optional<String> tokenOpt = gitHubClient.getDecryptedTokenForUser(user.getId());
      if (tokenOpt.isPresent() && repo.getWebhookHookId() != null && repo.getOwnerLogin() != null && repo.getRepoName() != null) {
        gitHubClient.removeRepositoryWebhook(tokenOpt.get(), repo.getOwnerLogin(), repo.getRepoName(), repo.getWebhookHookId());
        repo.setWebhookHookId(null);
      }

      repo.setMonitored(false);
      repo.setUpdatedAt(Instant.now());
      monitoredRepository.save(repo);
    });

    log.info("User {} disabled monitoring for repository ID: {}", user.getEmail(), repoId);
    return ResponseEntity.ok(Map.of(
        "status", "SUCCESS",
        "repoId", repoId,
        "isMonitored", false,
        "message", "Repository monitoring disabled."
    ));
  }

  /**
   * Disconnects GitHub account and clears stored tokens and monitored repositories for the user.
   */
  @DeleteMapping("/disconnect")
  @Transactional
  public ResponseEntity<Map<String, Object>> disconnect(Principal principal) {
    User user = getAuthenticatedUser(principal);

    connectionRepository.deleteByUserId(user.getId());
    monitoredRepository.deleteByUserId(user.getId());

    log.info("User {} disconnected GitHub account and deleted integration tokens.", user.getEmail());
    return ResponseEntity.ok(Map.of(
        "status", "SUCCESS",
        "message", "GitHub account disconnected and tokens securely deleted."
    ));
  }

  private User getAuthenticatedUser(Principal principal) {
    if (principal == null || principal.getName() == null) {
      throw ApiException.unauthorized("Authentication required");
    }
    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> ApiException.unauthorized("User session not found"));
  }
}