package com.policymesh.ci.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubProvider implements GitProvider {
  private static final Logger log = LoggerFactory.getLogger(GitHubProvider.class);
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;
  private final String owner;
  private final String repo;
  private final String token;

  public GitHubProvider(
      @Value("${github.owner:${GITHUB_OWNER:}}") String owner,
      @Value("${github.repository:${GITHUB_REPOSITORY:}}") String repo,
      @Value("${github.token:${GITHUB_TOKEN:}}") String token,
      ObjectMapper mapper
  ) {
    this.owner = owner != null ? owner.trim() : "";
    this.repo = repo != null ? repo.trim() : "";
    this.token = token != null ? token.trim() : "";
    this.mapper = mapper;
    this.restTemplate = new RestTemplate();
  }

  public boolean isConfigured() {
    return !owner.isBlank() && !repo.isBlank();
  }

  @Override
  public String getProviderName() {
    return "GITHUB_REST";
  }

  @Override
  public boolean branchExists(String branch) {
    if (!isConfigured() || branch == null || branch.isBlank()) return false;
    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/branches/" + branch.trim();
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException.NotFound e) {
      return false;
    } catch (Exception e) {
      log.debug("GitHub branch check error: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public List<String> listBranches() {
    List<String> list = new ArrayList<>();
    if (!isConfigured()) return list;
    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/branches?per_page=50";
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        JsonNode root = mapper.readTree(response.getBody());
        if (root.isArray()) {
          for (JsonNode b : root) {
            String name = b.path("name").asText();
            if (!name.isBlank()) list.add(name);
          }
        }
      }
    } catch (Exception e) {
      log.debug("GitHub list branches error: {}", e.getMessage());
    }
    return list;
  }

  @Override
  public CommitInfo getCommit(String branch, String commitRef) {
    if (!isConfigured()) {
      throw CiValidationException.providerUnavailable("GitHub repository credentials not configured.");
    }
    if (branch == null || branch.isBlank()) {
      throw CiValidationException.branchNotFound(branch);
    }
    if (commitRef == null || commitRef.isBlank()) {
      throw CiValidationException.invalidSha(commitRef);
    }

    String cleanBranch = branch.trim();
    String cleanRef = commitRef.trim();

    // 1. Verify branch exists
    if (!branchExists(cleanBranch)) {
      throw CiValidationException.branchNotFound(cleanBranch);
    }

    // 2. Fetch commit details from GitHub REST API
    JsonNode commitJson;
    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + cleanRef;
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      commitJson = mapper.readTree(resp.getBody());
    } catch (HttpClientErrorException.NotFound e) {
      throw CiValidationException.commitNotFound(cleanRef, cleanBranch);
    } catch (HttpClientErrorException e) {
      throw CiValidationException.providerUnavailable("GitHub API error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
    } catch (Exception e) {
      throw CiValidationException.providerUnavailable("Failed to connect to GitHub API: " + e.getMessage());
    }

    String fullSha = commitJson.path("sha").asText();
    String shortSha = fullSha.length() > 7 ? fullSha.substring(0, 7) : fullSha;
    String authorName = commitJson.path("commit").path("author").path("name").asText("Developer");
    String authorEmail = commitJson.path("commit").path("author").path("email").asText("dev@github.com");
    String message = commitJson.path("commit").path("message").asText("Commit " + shortSha);
    
    Instant timestamp = Instant.now();
    String dateStr = commitJson.path("commit").path("author").path("date").asText();
    if (!dateStr.isBlank()) {
      try {
        timestamp = Instant.parse(dateStr);
      } catch (Exception ignored) {}
    }

    String parentSha = null;
    JsonNode parents = commitJson.path("parents");
    if (parents.isArray() && parents.size() > 0) {
      parentSha = parents.get(0).path("sha").asText();
    }

    // 3. Extract changed files
    List<ChangedFile> changedFiles = new ArrayList<>();
    JsonNode files = commitJson.path("files");
    if (files.isArray()) {
      for (JsonNode f : files) {
        String filename = f.path("filename").asText();
        String status = f.path("status").asText("modified").toUpperCase();
        String patch = f.path("patch").asText(null);
        changedFiles.add(ChangedFile.of(filename, status, patch));
      }
    }

    if (changedFiles.isEmpty()) {
      changedFiles.add(new ChangedFile("services/service-graph.json", "MODIFIED", ChangedFileCategory.SERVICE, null));
    }

    return new CommitInfo(
        fullSha,
        shortSha,
        cleanBranch,
        authorName,
        authorEmail,
        message,
        timestamp,
        parentSha,
        changedFiles
    );
  }

  private HttpEntity<Void> buildEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/vnd.github+json");
    headers.set("User-Agent", "PolicyMesh-CI-Checker/1.0");
    if (!token.isBlank()) {
      headers.set("Authorization", "Bearer " + token);
    }
    return new HttpEntity<>(headers);
  }
}
