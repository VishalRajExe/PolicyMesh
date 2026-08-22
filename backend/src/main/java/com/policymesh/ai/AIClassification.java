package com.policymesh.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** An AI-suggested classification; enforcement-irlevant until a human approves it. */
@Entity
@Table(name = "ai_classifications")
public class AIClassification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String fieldName;
  @Column(length = 2000)
  private String sampleValue;
  @Column(nullable = false)
  private String classification;
  @Column(nullable = false)
  private double confidence;
  @Column(nullable = false)
  private String status = "PENDING";
  @Column(nullable = false)
  private String provider;
  private String reviewedBy;
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getFieldName() { return fieldName; }
  public String getSampleValue() { return sampleValue; }
  public String getClassification() { return classification; }
  public double getConfidence() { return confidence; }
  public String getStatus() { return status; }
  public String getProvider() { return provider; }
  public String getReviewedBy() { return reviewedBy; }
  public Instant getCreatedAt() { return createdAt; }

  public void setFieldName(String v) { fieldName = v; }
  public void setSampleValue(String v) { sampleValue = v; }
  public void setClassification(String v) { classification = v; }
  public void setConfidence(double v) { confidence = v; }
  public void setStatus(String v) { status = v; }
  public void setProvider(String v) { provider = v; }
  public void setReviewedBy(String v) { reviewedBy = v; }
}
