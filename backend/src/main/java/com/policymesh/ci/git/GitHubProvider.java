package com.policymesh.ci.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.CiDtos;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubProvider implements GitProvider {
  private static final Logger log = LoggerFactory.getLogger(GitHubProvider.class);
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;
  private final String owner;
  private final String repo;
  private String token;

  public GitHubProvider(
      @Value("${github.owner:${GITHUB_OWNER:VishalRajExe}}") String owner,
      @Value("${github.repository:${GITHUB_REPOSITORY:PolicyMesh}}") String repo,
      @Value("${github.token:${GITHUB_TOKEN:}}") String token,
      ObjectMapper mapper
  ) {
    this.owner = owner != null && !owner.isBlank() ? owner.trim() : "VishalRajExe";
    this.repo = repo != null && !repo.isBlank() ? repo.trim() : "PolicyMesh";
    this.token = resolveGitHubToken(token);
    this.mapper = mapper;
    this.restTemplate = new RestTemplate();
  }

  private String resolveGitHubToken(String configuredToken) {
    if (configuredToken != null && !configuredToken.isBlank()) {
      return configuredToken.trim();
    }
    String envToken = System.getenv("GITHUB_TOKEN");
    if (envToken != null && !envToken.isBlank()) {
      return envToken.trim();
    }
    String ghEnvToken = System.getenv("GH_TOKEN");
    if (ghEnvToken != null && !ghEnvToken.isBlank()) {
      return ghEnvToken.trim();
    }
    // Attempt local git credential helper discovery
    try {
      ProcessBuilder pb = new ProcessBuilder("git", "credential", "fill");
      pb.redirectErrorStream(true);
      Process p = pb.start();
      try (var out = p.getOutputStream()) {
        out.write("protocol=https\nhost=github.com\n\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
      }
      try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("password=")) {
            String pass = line.substring("password=".length()).trim();
            if (!pass.isBlank()) {
              log.info("Successfully discovered GitHub credentials from local Git Credential Manager.");
              return pass;
            }
          }
        }
      }
      p.waitFor();
    } catch (Exception ignored) {}

    return "";
  }

  public boolean isConfigured() {
    return !owner.isBlank() && !repo.isBlank();
  }

  @Override
  public String getFileContentAtCommit(String commitSha, String filePath) {
    if (!isConfigured() || commitSha == null || filePath == null) return null;
    try {
      String url = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + commitSha.trim() + "/" + filePath.trim();
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        return response.getBody();
      }
    } catch (Exception e) {
      log.debug("GitHub raw file fetch error: {}", e.getMessage());
    }
    return null;
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

  @Override
  public CiDtos.GitHubChecksSummary getGitHubChecks(String commitSha) {
    if (!isConfigured() || commitSha == null || commitSha.isBlank()) {
      return new CiDtos.GitHubChecksSummary("UNAVAILABLE", 0, 0, 0, 0, 0, "GitHub credentials not configured.", List.of(), List.of());
    }

    String cleanSha = commitSha.trim();
    String shortSha = cleanSha.length() > 7 ? cleanSha.substring(0, 7) : cleanSha;

    try {
      // 1. Query workflow runs for this commit SHA
      List<CiDtos.GitHubWorkflowRunItem> workflowRuns = fetchWorkflowRuns(cleanSha);
      Map<Long, String> workflowRunNames = new HashMap<>();
      for (var wr : workflowRuns) {
        if (wr.id() != null && wr.name() != null) {
          workflowRunNames.put(wr.id(), wr.name());
        }
      }

      // 2. Query check runs for this commit SHA
      String checkRunsUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + cleanSha + "/check-runs?per_page=100";
      ResponseEntity<String> response = restTemplate.exchange(checkRunsUrl, HttpMethod.GET, buildEntity(), String.class);
      
      List<CiDtos.GitHubCheckItem> items = new ArrayList<>();
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode checkRuns = root.path("check_runs");
        if (checkRuns.isArray()) {
          for (JsonNode cr : checkRuns) {
            long checkRunId = cr.path("id").asLong();
            String name = cr.path("name").asText("Check");
            String status = cr.path("status").asText("completed");
            String conclusion = cr.path("conclusion").isNull() ? null : cr.path("conclusion").asText();
            String htmlUrl = cr.path("html_url").asText("");
            if (htmlUrl.isBlank()) htmlUrl = cr.path("details_url").asText("");

            String title = cr.path("output").path("title").asText("");
            String summary = cr.path("output").path("summary").asText("");
            String text = cr.path("output").path("text").asText("");
            String details = !title.isBlank() ? title : (!summary.isBlank() ? summary : conclusion);

            // Compute duration
            Long durationSeconds = null;
            String startedAtStr = cr.path("started_at").asText("");
            String completedAtStr = cr.path("completed_at").asText("");
            if (!startedAtStr.isBlank() && !completedAtStr.isBlank()) {
              try {
                Instant start = Instant.parse(startedAtStr);
                Instant end = Instant.parse(completedAtStr);
                durationSeconds = Math.max(0, Duration.between(start, end).toSeconds());
              } catch (Exception ignored) {}
            }

            // Derive workflow name
            String workflowName = null;
            if (htmlUrl.contains("/actions/runs/")) {
              try {
                String runIdStr = htmlUrl.substring(htmlUrl.indexOf("/actions/runs/") + "/actions/runs/".length());
                if (runIdStr.contains("/")) runIdStr = runIdStr.substring(0, runIdStr.indexOf("/"));
                long runId = Long.parseLong(runIdStr);
                workflowName = workflowRunNames.get(runId);
              } catch (Exception ignored) {}
            }
            if (workflowName == null || workflowName.isBlank()) {
              workflowName = normalizeWorkflowName(name);
            }

            // Fetch real error snippet from annotations if check failed
            String errorSnippet = null;
            if ("failure".equalsIgnoreCase(conclusion) || "cancelled".equalsIgnoreCase(conclusion) || "timed_out".equalsIgnoreCase(conclusion)) {
              errorSnippet = fetchCheckRunFailureSnippet(checkRunId, text, summary);
            }

            items.add(new CiDtos.GitHubCheckItem(
                name,
                workflowName,
                name,
                status,
                conclusion != null ? conclusion : status,
                details,
                errorSnippet,
                htmlUrl,
                durationSeconds,
                List.of()
            ));
          }
        }
      }

      // If check-runs returned 0 items but workflow runs exist, fetch jobs from workflow runs
      if (items.isEmpty() && !workflowRuns.isEmpty()) {
        for (var wr : workflowRuns) {
          if (wr.id() != null) {
            List<CiDtos.GitHubCheckItem> jobItems = fetchWorkflowRunJobs(wr.id(), wr.name());
            items.addAll(jobItems);
          }
        }
      }

      // If genuinely no workflow runs and no check runs exist for this commit
      if (items.isEmpty() && workflowRuns.isEmpty()) {
        return new CiDtos.GitHubChecksSummary(
            "NO_RUNS",
            0,
            0,
            0,
            0,
            0,
            "No GitHub Actions workflow was triggered for commit " + shortSha + ". Push a commit to trigger CI.",
            List.of(),
            List.of()
        );
      }

      // Calculate totals
      int passed = 0;
      int failed = 0;
      int skipped = 0;
      int pending = 0;
      List<String> failedNames = new ArrayList<>();
      List<String> skippedNames = new ArrayList<>();
      List<String> pendingNames = new ArrayList<>();

      for (var item : items) {
        String c = item.conclusion() != null ? item.conclusion().toLowerCase() : "";
        String s = item.status() != null ? item.status().toLowerCase() : "";

        if ("success".equals(c) || "neutral".equals(c)) {
          passed++;
        } else if ("failure".equals(c) || "timed_out".equals(c) || "action_required".equals(c) || "cancelled".equals(c)) {
          failed++;
          failedNames.add(item.name());
        } else if ("skipped".equals(c)) {
          skipped++;
          skippedNames.add(item.name());
        } else if ("in_progress".equals(s) || "queued".equals(s) || "waiting".equals(s) || "requested".equals(s) || c.isBlank()) {
          pending++;
          pendingNames.add(item.name());
        } else {
          failed++;
          failedNames.add(item.name());
        }
      }

      String overall;
      String failureReason = null;
      if (failed > 0) {
        overall = "FAILURE";
        failureReason = failed + " check(s) failed: " + String.join(", ", failedNames) + (skipped > 0 ? " (" + skipped + " skipped)" : "");
      } else if (skipped > 0) {
        overall = "SKIPPED";
        failureReason = skipped + " check(s) skipped: " + String.join(", ", skippedNames);
      } else if (pending > 0) {
        overall = "PENDING";
        failureReason = pending + " check(s) in progress: " + String.join(", ", pendingNames);
      } else if (passed == items.size() && items.size() > 0) {
        overall = "SUCCESS";
        failureReason = null;
      } else {
        overall = "UNAVAILABLE";
        failureReason = "No conclusive check runs found.";
      }

      return new CiDtos.GitHubChecksSummary(
          overall,
          items.size(),
          passed,
          failed,
          skipped,
          pending,
          failureReason,
          items,
          workflowRuns
      );

    } catch (HttpClientErrorException.Forbidden e) {
      log.warn("GitHub API Forbidden (Rate limit or permissions): {}", e.getMessage());
      String msg = e.getResponseBodyAsString();
      if (msg != null && msg.toLowerCase().contains("rate limit")) {
        return new CiDtos.GitHubChecksSummary(
            "GITHUB_RATE_LIMIT",
            0, 0, 0, 0, 0,
            "GitHub REST API rate limit reached for repository " + owner + "/" + repo + ". Please set GITHUB_TOKEN for higher rate limits.",
            List.of(),
            List.of()
        );
      }
      return new CiDtos.GitHubChecksSummary(
          "GITHUB_AUTH_ERROR",
          0, 0, 0, 0, 0,
          "GitHub API access forbidden: " + e.getMessage(),
          List.of(),
          List.of()
      );
    } catch (HttpClientErrorException.Unauthorized e) {
      log.warn("GitHub API Unauthorized: {}", e.getMessage());
      return new CiDtos.GitHubChecksSummary(
          "GITHUB_AUTH_ERROR",
          0, 0, 0, 0, 0,
          "GitHub API authentication token is invalid or expired.",
          List.of(),
          List.of()
      );
    } catch (Exception e) {
      log.error("Failed fetching GitHub Actions for commit {}: {}", commitSha, e.getMessage());
      return new CiDtos.GitHubChecksSummary(
          "UNAVAILABLE",
          0, 0, 0, 0, 0,
          "Failed to connect to GitHub Actions API: " + e.getMessage(),
          List.of(),
          List.of()
      );
    }
  }

  private List<CiDtos.GitHubWorkflowRunItem> fetchWorkflowRuns(String commitSha) {
    List<CiDtos.GitHubWorkflowRunItem> list = new ArrayList<>();
    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs?head_sha=" + commitSha + "&per_page=50";
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode runs = root.path("workflow_runs");
        if (runs.isArray()) {
          for (JsonNode wr : runs) {
            long id = wr.path("id").asLong();
            String name = wr.path("name").asText("Workflow");
            String status = wr.path("status").asText("completed");
            String conclusion = wr.path("conclusion").isNull() ? null : wr.path("conclusion").asText();
            String event = wr.path("event").asText("push");
            String htmlUrl = wr.path("html_url").asText("");
            int runNumber = wr.path("run_number").asInt(1);
            String createdAt = wr.path("created_at").asText("");
            list.add(new CiDtos.GitHubWorkflowRunItem(id, name, status, conclusion, event, htmlUrl, runNumber, createdAt));
          }
        }
      }
    } catch (Exception e) {
      log.debug("Failed fetching workflow runs: {}", e.getMessage());
    }
    return list;
  }

  private List<CiDtos.GitHubCheckItem> fetchWorkflowRunJobs(long runId, String workflowName) {
    List<CiDtos.GitHubCheckItem> items = new ArrayList<>();
    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs?per_page=100";
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode jobs = root.path("jobs");
        if (jobs.isArray()) {
          for (JsonNode j : jobs) {
            String name = j.path("name").asText("Job");
            String status = j.path("status").asText("completed");
            String conclusion = j.path("conclusion").isNull() ? null : j.path("conclusion").asText();
            String htmlUrl = j.path("html_url").asText("");

            List<CiDtos.GitHubCheckStep> steps = new ArrayList<>();
            String failureStepName = null;
            JsonNode stepsNode = j.path("steps");
            if (stepsNode.isArray()) {
              for (JsonNode st : stepsNode) {
                String stepName = st.path("name").asText();
                String stepStatus = st.path("status").asText();
                String stepConclusion = st.path("conclusion").asText();
                int num = st.path("number").asInt();
                steps.add(new CiDtos.GitHubCheckStep(stepName, stepStatus, stepConclusion, num));
                if ("failure".equalsIgnoreCase(stepConclusion) && failureStepName == null) {
                  failureStepName = stepName;
                }
              }
            }

            String errorSnippet = null;
            if (failureStepName != null) {
              errorSnippet = "Failed on step: \"" + failureStepName + "\" (exit code 1)";
            }

            items.add(new CiDtos.GitHubCheckItem(
                name,
                workflowName,
                name,
                status,
                conclusion != null ? conclusion : status,
                failureStepName != null ? ("Failed at step: " + failureStepName) : (conclusion != null ? conclusion : status),
                errorSnippet,
                htmlUrl,
                null,
                steps
            ));
          }
        }
      }
    } catch (Exception e) {
      log.debug("Failed fetching jobs for run {}: {}", runId, e.getMessage());
    }
    return items;
  }

  private String fetchCheckRunFailureSnippet(long checkRunId, String text, String summary) {
    if (summary != null && !summary.isBlank() && summary.length() > 5) return summary.trim();
    if (text != null && !text.isBlank() && text.length() > 5) return text.trim();

    try {
      String url = "https://api.github.com/repos/" + owner + "/" + repo + "/check-runs/" + checkRunId + "/annotations?per_page=10";
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        JsonNode root = mapper.readTree(resp.getBody());
        if (root.isArray()) {
          for (JsonNode a : root) {
            String level = a.path("annotation_level").asText("");
            String msg = a.path("message").asText("");
            String title = a.path("title").asText("");
            if ("failure".equalsIgnoreCase(level) && !msg.isBlank()) {
              return !title.isBlank() ? (title + ": " + msg) : msg;
            }
          }
        }
      }
    } catch (Exception ignored) {}

    return "Process completed with exit code 1. Check build logs on GitHub.";
  }

  private String normalizeWorkflowName(String checkName) {
    if (checkName == null) return "CI Pipeline";
    String lower = checkName.toLowerCase();
    if (lower.contains("backend")) return "Backend CI";
    if (lower.contains("frontend")) return "Frontend CI";
    if (lower.contains("docker")) return "Docker Build";
    if (lower.contains("policy") || lower.contains("compliance")) return "PolicyMesh";
    if (lower.contains("status")) return "PolicyMesh";
    return "GitHub Actions";
  }

  private HttpEntity<Void> buildEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/vnd.github+json");
    headers.set("User-Agent", "PolicyMesh-CI-Checker/1.0");
    if (token != null && !token.isBlank()) {
      headers.set("Authorization", "Bearer " + token);
    }
    return new HttpEntity<>(headers);
  }
}

