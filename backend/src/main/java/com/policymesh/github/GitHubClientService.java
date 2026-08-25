package com.policymesh.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.common.ApiException;
import com.policymesh.common.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GitHubClientService {
  private static final Logger log = LoggerFactory.getLogger(GitHubClientService.class);

  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final EncryptionService encryptionService;
  private final GitHubConnectionRepository connectionRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  public GitHubClientService(
      @Value("${github.oauth.client-id:${GITHUB_CLIENT_ID:}}") String clientId,
      @Value("${github.oauth.client-secret:${GITHUB_CLIENT_SECRET:}}") String clientSecret,
      @Value("${github.oauth.redirect-uri:${GITHUB_REDIRECT_URI:http://localhost:8080/api/v1/github/callback}}") String redirectUri,
      EncryptionService encryptionService,
      GitHubConnectionRepository connectionRepository,
      ObjectMapper mapper
  ) {
    this.clientId = clientId != null ? clientId.trim() : "";
    this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
    this.redirectUri = redirectUri != null ? redirectUri.trim() : "http://localhost:8080/api/v1/github/callback";
    this.encryptionService = encryptionService;
    this.connectionRepository = connectionRepository;
    this.mapper = mapper;
    this.restTemplate = new RestTemplate();
  }

  public boolean isOAuthConfigured() {
    return !clientId.isEmpty() && !clientSecret.isEmpty();
  }

  public String getClientId() { return clientId; }
  public String getRedirectUri() { return redirectUri; }

  public record OAuthTokenResponse(String accessToken, String tokenType, String scope) {}
  public record GitHubUserProfile(Long id, String login, String name, String email, String avatarUrl) {}
  public record GitHubRepoDto(Long id, String name, String fullName, String ownerLogin, boolean isPrivate,
                              String defaultBranch, String description, String htmlUrl, boolean isMonitored,
                              String lastCommitSha, String lastScanStatus) {}

  /**
   * Exchanges an OAuth authorization code for a GitHub access token.
   */
  public OAuthTokenResponse exchangeCode(String code) {
    if (!isOAuthConfigured()) {
      throw ApiException.badRequest("GitHub OAuth is not configured on this server (missing GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET).");
    }

    String url = "https://github.com/login/oauth/access_token";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    Map<String, String> body = Map.of(
        "client_id", clientId,
        "client_secret", clientSecret,
        "code", code,
        "redirect_uri", redirectUri
    );

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw ApiException.badRequest("Failed exchanging authorization code with GitHub.");
      }

      JsonNode json = mapper.readTree(response.getBody());
      if (json.has("error")) {
        String errorDesc = json.has("error_description") ? json.get("error_description").asText() : json.get("error").asText();
        throw ApiException.badRequest("GitHub OAuth error: " + errorDesc);
      }

      String token = json.path("access_token").asText();
      String tokenType = json.path("token_type").asText("bearer");
      String scope = json.path("scope").asText("read:user,repo");

      if (token == null || token.isBlank()) {
        throw ApiException.badRequest("No access token returned by GitHub.");
      }

      return new OAuthTokenResponse(token, tokenType, scope);
    } catch (HttpClientErrorException e) {
      log.error("GitHub OAuth exchange failed HTTP {}: {}", e.getStatusCode(), e.getMessage());
      throw ApiException.badRequest("GitHub OAuth exchange failed: " + e.getStatusCode());
    } catch (Exception e) {
      log.error("GitHub OAuth exchange unexpected error: {}", e.getMessage(), e);
      throw ApiException.badRequest("Failed to complete GitHub authorization.");
    }
  }

  /**
   * Fetches the user profile from GitHub using their access token.
   */
  public GitHubUserProfile fetchUserProfile(String accessToken) {
    String url = "https://api.github.com/user";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      JsonNode json = mapper.readTree(response.getBody());

      Long id = json.path("id").asLong();
      String login = json.path("login").asText("unknown");
      String name = json.has("name") && !json.get("name").isNull() ? json.get("name").asText() : login;
      String email = json.has("email") && !json.get("email").isNull() ? json.get("email").asText() : null;
      String avatarUrl = json.path("avatar_url").asText(null);

      return new GitHubUserProfile(id, login, name, email, avatarUrl);
    } catch (Exception e) {
      log.error("Failed fetching GitHub user profile: {}", e.getMessage(), e);
      throw ApiException.badRequest("Failed fetching GitHub user profile.");
    }
  }

  /**
   * Fetches repositories accessible to the user using their access token.
   */
  public List<GitHubRepoDto> fetchUserRepositories(String accessToken) {
    String url = "https://api.github.com/user/repos?per_page=100&sort=updated";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    List<GitHubRepoDto> repos = new ArrayList<>();
    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      JsonNode array = mapper.readTree(response.getBody());

      if (array.isArray()) {
        for (JsonNode item : array) {
          Long id = item.path("id").asLong();
          String name = item.path("name").asText();
          String fullName = item.path("full_name").asText();
          String ownerLogin = item.path("owner").path("login").asText();
          boolean isPrivate = item.path("private").asBoolean(false);
          String defaultBranch = item.path("default_branch").asText("main");
          String description = item.path("description").asText("");
          String htmlUrl = item.path("html_url").asText();

          repos.add(new GitHubRepoDto(id, name, fullName, ownerLogin, isPrivate, defaultBranch, description, htmlUrl, false, null, null));
        }
      }
    } catch (Exception e) {
      log.warn("Failed fetching GitHub user repositories: {}", e.getMessage());
    }
    return repos;
  }

  public Optional<String> getDecryptedTokenForUser(Long userId) {
    return connectionRepository.findByUserId(userId)
        .map(conn -> encryptionService.decrypt(conn.getEncryptedAccessToken()));
  }
}