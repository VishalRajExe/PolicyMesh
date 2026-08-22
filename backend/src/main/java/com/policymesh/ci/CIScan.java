package com.policymesh.ci;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persisted record of one CI compliance scan; violations are stored as JSON for later retrieval. */
@Entity
@Table(name = "ci_scans")
public class CIScan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String commitHash;
  @Column(nullable = false)
  private String branch;
  @Column(nullable = false)
  private String status;
  @Column(nullable = false)
  private int violationCount;
  @Column(nullable = false)
  private Instant startedAt = Instant.now();
  private Instant completedAt;
  @Column(columnDefinition = "text")
  private String violationsJson = "[]";

  public Long getId() { return id; }
  public String getCommitHash() { return commitHash; }
  public String getBranch() { return branch; }
  public String getStatus() { return status; }
  public int getViolationCount() { return violationCount; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public String getViolationsJson() { return violationsJson; }

  public void setCommitHash(String v) { commitHash = v; }
  public void setBranch(String v) { branch = v; }
  public void setStatus(String v) { status = v; }
  public void setViolationCount(int v) { violationCount = v; }
  public void setViolationsJson(String v) { violationsJson = v; }
  public void complete() { completedAt = Instant.now(); }
}
