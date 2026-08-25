package com.policymesh.ci;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persisted record of one CI compliance scan; violations and changed files are stored as JSON for later retrieval. */
@Entity
@Table(name = "ci_scans")
public class CIScan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "commit_hash", nullable = false)
  private String commitHash;
  @Column(nullable = false)
  private String branch;
  @Column(nullable = false)
  private String status;
  @Column(name = "violation_count", nullable = false)
  private int violationCount;
  @Column(name = "started_at", nullable = false)
  private Instant startedAt = Instant.now();
  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "commit_message")
  private String commitMessage;
  private String author;
  @Column(name = "parent_sha")
  private String parentSha;

  @Column(name = "flows_checked")
  private int flowsChecked;
  @Column(name = "passed_flows")
  private int passedFlows;
  @Column(name = "failed_flows")
  private int failedFlows;

  @Column(name = "violations_json", columnDefinition = "text")
  private String violationsJson = "[]";

  @Column(name = "changed_files_json", columnDefinition = "text")
  private String changedFilesJson = "[]";

  @Column(name = "github_overall_status")
  private String githubOverallStatus;
  @Column(name = "github_total_checks")
  private int githubTotalChecks;
  @Column(name = "github_passed_checks")
  private int githubPassedChecks;
  @Column(name = "github_failed_checks")
  private int githubFailedChecks;
  @Column(name = "github_skipped_checks")
  private int githubSkippedChecks;
  @Column(name = "github_pending_checks")
  private int githubPendingChecks;
  @Column(name = "merge_allowed")
  private Boolean mergeAllowed;

  @Column(name = "github_checks_json", columnDefinition = "mediumtext")
  private String githubChecksJson = "{}";

  @Column(name = "final_decision_json", columnDefinition = "text")
  private String finalDecisionJson = "{}";

  public Long getId() { return id; }
  public String getCommitHash() { return commitHash; }
  public String getBranch() { return branch; }
  public String getStatus() { return status; }
  public int getViolationCount() { return violationCount; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public String getViolationsJson() { return violationsJson; }
  public String getChangedFilesJson() { return changedFilesJson; }
  public String getCommitMessage() { return commitMessage; }
  public String getAuthor() { return author; }
  public String getParentSha() { return parentSha; }
  public int getFlowsChecked() { return flowsChecked; }
  public int getPassedFlows() { return passedFlows; }
  public int getFailedFlows() { return failedFlows; }
  public String getGithubOverallStatus() { return githubOverallStatus; }
  public int getGithubTotalChecks() { return githubTotalChecks; }
  public int getGithubPassedChecks() { return githubPassedChecks; }
  public int getGithubFailedChecks() { return githubFailedChecks; }
  public int getGithubSkippedChecks() { return githubSkippedChecks; }
  public int getGithubPendingChecks() { return githubPendingChecks; }
  public Boolean getMergeAllowed() { return mergeAllowed; }
  public String getGithubChecksJson() { return githubChecksJson; }
  public String getFinalDecisionJson() { return finalDecisionJson; }

  public void setCommitHash(String v) { commitHash = v; }
  public void setBranch(String v) { branch = v; }
  public void setStatus(String v) { status = v; }
  public void setViolationCount(int v) { violationCount = v; }
  public void setViolationsJson(String v) { violationsJson = v; }
  public void setChangedFilesJson(String v) { changedFilesJson = v; }
  public void setCommitMessage(String v) { commitMessage = v; }
  public void setAuthor(String v) { author = v; }
  public void setParentSha(String v) { parentSha = v; }
  public void setFlowsChecked(int v) { flowsChecked = v; }
  public void setPassedFlows(int v) { passedFlows = v; }
  public void setFailedFlows(int v) { failedFlows = v; }
  public void setGithubOverallStatus(String v) { githubOverallStatus = v; }
  public void setGithubTotalChecks(int v) { githubTotalChecks = v; }
  public void setGithubPassedChecks(int v) { githubPassedChecks = v; }
  public void setGithubFailedChecks(int v) { githubFailedChecks = v; }
  public void setGithubSkippedChecks(int v) { githubSkippedChecks = v; }
  public void setGithubPendingChecks(int v) { githubPendingChecks = v; }
  public void setMergeAllowed(Boolean v) { mergeAllowed = v; }
  public void setGithubChecksJson(String v) { githubChecksJson = v; }
  public void setFinalDecisionJson(String v) { finalDecisionJson = v; }
  public void complete() { completedAt = Instant.now(); }
}
