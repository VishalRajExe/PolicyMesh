package com.policymesh.lineage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One link of the tamper-evident SHA-256 hash chain. Every runtime decision appends exactly
 * one record; currentHash covers the canonical serialization of the decision metadata plus
 * the previous record's hash. Digital signatures are a reserved extension point (signature
 * stays null in the MVP).
 */
@Entity
@Table(name = "lineage_records")
public class LineageRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "decision_id", nullable = false, unique = true)
  private Long decisionId;
  @Column(name = "source_service", nullable = false)
  private String sourceService;
  @Column(name = "destination_service", nullable = false)
  private String destinationService;
  @Column(name = "source_region", nullable = false)
  private String sourceRegion;
  @Column(name = "destination_region", nullable = false)
  private String destinationRegion;
  @Column(name = "data_class", nullable = false)
  private String dataClass;
  @Column(nullable = false)
  private String decision;
  @Column(nullable = false)
  private String reason;
  @Column(name = "policy_id")
  private String policyId;
  @Column(name = "previous_hash", length = 128)
  private String previousHash;
  @Column(name = "current_hash", nullable = false, length = 128)
  private String currentHash;
  @Column(length = 512)
  private String signature;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

  public Long getId() { return id; }
  public Long getDecisionId() { return decisionId; }
  public String getSourceService() { return sourceService; }
  public String getDestinationService() { return destinationService; }
  public String getSourceRegion() { return sourceRegion; }
  public String getDestinationRegion() { return destinationRegion; }
  public String getDataClass() { return dataClass; }
  public String getDecision() { return decision; }
  public String getReason() { return reason; }
  public String getPolicyId() { return policyId; }
  public String getPreviousHash() { return previousHash; }
  public String getCurrentHash() { return currentHash; }
  public String getSignature() { return signature; }
  public Instant getCreatedAt() { return createdAt; }

  public void setDecisionId(Long v) { decisionId = v; }
  public void setSourceService(String v) { sourceService = v; }
  public void setDestinationService(String v) { destinationService = v; }
  public void setSourceRegion(String v) { sourceRegion = v; }
  public void setDestinationRegion(String v) { destinationRegion = v; }
  public void setDataClass(String v) { dataClass = v; }
  public void setDecision(String v) { decision = v; }
  public void setReason(String v) { reason = v; }
  public void setPolicyId(String v) { policyId = v; }
  public void setPreviousHash(String v) { previousHash = v; }
  public void setCurrentHash(String v) { currentHash = v; }
}
