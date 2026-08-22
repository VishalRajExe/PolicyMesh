package com.policymesh.servicegraph;

import com.policymesh.policy.PolicyVocabulary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "service_nodes", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class ServiceNode {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String region;
  private String meshZone;
  @Column(nullable = false)
  private String environment;
  private String description;
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();
  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void changed() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getRegion() { return region; }
  public String getMeshZone() { return meshZone; }
  public String getEnvironment() { return environment; }
  public String getDescription() { return description; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setName(String v) { name = v; }
  public void setRegion(String v) { region = PolicyVocabulary.canonicalRegion(v); }
  public void setMeshZone(String v) { meshZone = v == null || v.isBlank() ? null : v.trim(); }
  public void setEnvironment(String v) { environment = v; }
  public void setDescription(String v) { description = v; }
}
