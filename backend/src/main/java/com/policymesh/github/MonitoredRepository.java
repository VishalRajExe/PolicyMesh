package com.policymesh.github;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "monitored_repositories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_repo_id", columnNames = {"userId", "githubRepoId"}),
        @UniqueConstraint(name = "uk_user_repo_name", columnNames = {"userId", "repoFullName"})
    },
    indexes = {
        @Index(name = "idx_monitored_repo_user", columnList = "userId"),
        @Index(name = "idx_monitored_repo_name", columnList = "repoFullName")
    }
)
public class MonitoredRepository {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long githubRepoId;

  @Column(nullable = false, length = 255)
  private String repoFullName; // e.g. "VishalRajExe/PolicyMesh"

  @Column(nullable = false, length = 150)
  private String repoName; // e.g. "PolicyMesh"

  @Column(nullable = false, length = 150)
  private String ownerLogin; // e.g. "VishalRajExe"

  @Column(length = 100)
  private String defaultBranch = "main";

  private boolean isPrivate = false;

  private boolean isMonitored = true;

  @Column(length = 100)
  private String lastCommitSha;

  @Column(length = 500)
  private String lastCommitMessage;

  @Column(length = 50)
  private String lastScanStatus; // PASS, FAIL, PENDING

  private Long lastScanId;

  private Instant lastScanTime;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  public MonitoredRepository() {}

  public MonitoredRepository(Long userId, Long githubRepoId, String repoFullName, String repoName,
                             String ownerLogin, String defaultBranch, boolean isPrivate) {
    this.userId = userId;
    this.githubRepoId = githubRepoId;
    this.repoFullName = repoFullName;
    this.repoName = repoName;
    this.ownerLogin = ownerLogin;
    this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
    this.isPrivate = isPrivate;
    this.isMonitored = true;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getGithubRepoId() { return githubRepoId; }
  public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }
  public String getRepoFullName() { return repoFullName; }
  public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
  public String getRepoName() { return repoName; }
  public void setRepoName(String repoName) { this.repoName = repoName; }
  public String getOwnerLogin() { return ownerLogin; }
  public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
  public String getDefaultBranch() { return defaultBranch; }
  public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
  public boolean isPrivate() { return isPrivate; }
  public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
  public boolean isMonitored() { return isMonitored; }
  public void setMonitored(boolean monitored) { isMonitored = monitored; }
  public String getLastCommitSha() { return lastCommitSha; }
  public void setLastCommitSha(String lastCommitSha) { this.lastCommitSha = lastCommitSha; }
  public String getLastCommitMessage() { return lastCommitMessage; }
  public void setLastCommitMessage(String lastCommitMessage) { this.lastCommitMessage = lastCommitMessage; }
  public String getLastScanStatus() { return lastScanStatus; }
  public void setLastScanStatus(String lastScanStatus) { this.lastScanStatus = lastScanStatus; }
  public Long getLastScanId() { return lastScanId; }
  public void setLastScanId(Long lastScanId) { this.lastScanId = lastScanId; }
  public Instant getLastScanTime() { return lastScanTime; }
  public void setLastScanTime(Instant lastScanTime) { this.lastScanTime = lastScanTime; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}