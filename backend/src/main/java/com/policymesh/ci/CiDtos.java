package com.policymesh.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.policymesh.ci.git.ChangedFile;
import com.policymesh.ci.git.ChangedFileCategory;
import com.policymesh.ci.git.CommitInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CiDtos {
  private CiDtos() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Request(
      @NotBlank @Size(min = 1, max = 64)
      @Pattern(regexp = "^(HEAD(~[0-9]+)?|HEAD\\^?|[0-9a-fA-F]{3,40})$",
               message = "commitHash must be a valid hexadecimal SHA-1 hash (3-40 chars) or 'HEAD'")
      String commitHash,

      @NotBlank @Size(min = 1, max = 255)
      @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9/_.-]*$",
               message = "branch contains invalid characters or does not start with an alphanumeric character")
      String branch) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChangedFileDto(
      String path,
      String status,
      ChangedFileCategory category,
      String patch
  ) {
    public static ChangedFileDto from(ChangedFile f) {
      return new ChangedFileDto(f.path(), f.status(), f.category(), f.patch());
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FlowState(String source, String destination, String decision) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record BeforeAfterFlow(FlowState previous, FlowState proposed) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ViolationDetail(
      String sourceService,
      String sourceRegion,
      String destinationService,
      String destinationRegion,
      String dataClass,
      String policyCode,
      String policyName,
      String reason,
      String whatChanged,
      String howToFix,
      List<String> visualFlow,
      BeforeAfterFlow beforeAfter
  ) {}

  /** Comprehensive, explainable CI Check Result */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Response(
      Long id,
      String status, // PASSED, BLOCKED
      String result, // PASS, FAIL
      @JsonProperty("passed") boolean passed,
      String branch,
      String commitHash,
      String commitShortSha,
      String commitMessage,
      String author,
      Instant timestamp,
      String parentCommit,
      List<ChangedFileDto> changedFiles,
      int totalFilesAnalyzed,
      int flowsChecked,
      int passedFlows,
      int failedFlows,
      int violationCount,
      Instant startedAt,
      Instant completedAt,
      List<ViolationDetail> violations,
      String humanReadable
  ) {}

  public static Response from(
      CIScan s,
      CommitInfo commit,
      int totalFilesAnalyzed,
      int flowsChecked,
      int passedFlows,
      int failedFlows,
      List<ViolationDetail> violations
  ) {
    boolean passed = "PASS".equalsIgnoreCase(s.getStatus()) || "PASSED".equalsIgnoreCase(s.getStatus());
    String status = passed ? "PASSED" : "BLOCKED";
    String result = passed ? "PASS" : "FAIL";

    List<ChangedFileDto> filesDto = commit.changedFiles() != null
        ? commit.changedFiles().stream().map(ChangedFileDto::from).toList()
        : List.of();

    return new Response(
        s.getId(),
        status,
        result,
        passed,
        s.getBranch(),
        s.getCommitHash(),
        commit.shortSha(),
        commit.message(),
        commit.authorName(),
        commit.timestamp(),
        commit.shortParentSha(),
        filesDto,
        totalFilesAnalyzed,
        flowsChecked,
        passedFlows,
        failedFlows,
        s.getViolationCount(),
        s.getStartedAt(),
        s.getCompletedAt(),
        violations,
        humanSummary(s, commit, passed)
    );
  }

  static String humanSummary(CIScan s, CommitInfo commit, boolean passed) {
    return "PolicyMesh CI " + (passed ? "PASSED" : "BLOCKED") + ": " + s.getViolationCount()
        + " violation(s) on branch " + s.getBranch() + " @ " + commit.shortSha()
        + " (\"" + commit.message() + "\")";
  }
}
