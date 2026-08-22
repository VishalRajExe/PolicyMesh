package com.policymesh.enforcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persisted audit trail of every runtime compliance decision; 1:1 with a LineageRecord. */
@Entity
@Table(name = "decisions", indexes = @Index(name = "idx_decision_created", columnList = "createdAt"))
public class DecisionRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String sourceService;
  @Column(nullable = false)
  private String destinationService;
  @Column(nullable = false)
  private String sourceRegion;
  @Column(nullable = false)
  private String destinationRegion;
  @Column(nullable = false)
  private String dataClass;
  @Column(nullable = false)
  private String decision;
  private String policyId;
  @Column(nullable = false)
  private String reason;
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getSourceService() { return sourceService; }
  public String getDestinationService() { return destinationService; }
  public String getSourceRegion() { return sourceRegion; }
  public String getDestinationRegion() { return destinationRegion; }
  public String getDataClass() { return dataClass; }
  public String getDecision() { return decision; }
  public String getPolicyId() { return policyId; }
  public String getReason() { return reason; }
  public Instant getCreatedAt() { return createdAt; }

  public void setSourceService(String v) { sourceService = v; }
  public void setDestinationService(String v) { destinationService = v; }
  public void setSourceRegion(String v) { sourceRegion = v; }
  public void setDestinationRegion(String v) { destinationRegion = v; }
  public void setDataClass(String v) { dataClass = v; }
  public void setDecision(String v) { decision = v; }
  public void setPolicyId(String v) { policyId = v; }
  public void setReason(String v) { reason = v; }
}
