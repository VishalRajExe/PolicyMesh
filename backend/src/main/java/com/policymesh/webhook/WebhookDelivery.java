package com.policymesh.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "webhook_deliveries",
    indexes = {
        @Index(name = "idx_webhook_delivery_id", columnList = "deliveryId", unique = true),
        @Index(name = "idx_webhook_commit_sha", columnList = "commitSha"),
        @Index(name = "idx_webhook_status", columnList = "status")
    }
)
public class WebhookDelivery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String deliveryId;

  @Column(nullable = false, length = 64)
  private String eventType;

  @Column(length = 255)
  private String repository;

  @Column(length = 255)
  private String branchRef;

  @Column(length = 100)
  private String commitSha;

  @Column(length = 255)
  private String sender;

  @Column(nullable = false, length = 50)
  private String status; // PENDING, COMPLETED, FAILED, IGNORED, ALREADY_PROCESSED

  @Column(length = 2000)
  private String summary;

  @Column(length = 2000)
  private String errorMessage;

  private Long scanId;

  @Column(nullable = false)
  private Instant receivedAt = Instant.now();

  private Instant completedAt;

  public WebhookDelivery() {}

  public WebhookDelivery(String deliveryId, String eventType, String repository, String branchRef, String commitSha, String sender) {
    this.deliveryId = deliveryId;
    this.eventType = eventType;
    this.repository = repository;
    this.branchRef = branchRef;
    this.commitSha = commitSha;
    this.sender = sender;
    this.status = "PENDING";
    this.receivedAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getDeliveryId() { return deliveryId; }
  public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getRepository() { return repository; }
  public void setRepository(String repository) { this.repository = repository; }
  public String getBranchRef() { return branchRef; }
  public void setBranchRef(String branchRef) { this.branchRef = branchRef; }
  public String getCommitSha() { return commitSha; }
  public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
  public String getSender() { return sender; }
  public void setSender(String sender) { this.sender = sender; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Long getScanId() { return scanId; }
  public void setScanId(Long scanId) { this.scanId = scanId; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}