package com.policymesh.servicegraph;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "data_flow_edges", uniqueConstraints = @UniqueConstraint(columnNames = {"source_service_id", "destination_service_id"}))
public class DataFlowEdge {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "source_service_id", nullable = false)
  private Long sourceServiceId;

  @Column(name = "destination_service_id", nullable = false)
  private Long destinationServiceId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "data_flow_edge_classes", joinColumns = @JoinColumn(name = "edge_id"))
  @Column(name = "data_class")
  private Set<String> dataClasses = new TreeSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void changed() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public Long getSourceServiceId() { return sourceServiceId; }
  public Long getDestinationServiceId() { return destinationServiceId; }
  public Set<String> getDataClasses() { return dataClasses; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setSourceServiceId(Long v) { sourceServiceId = v; }
  public void setDestinationServiceId(Long v) { destinationServiceId = v; }
  public void setDataClasses(Set<String> v) { dataClasses = v; }
}
