package com.policymesh.servicegraph.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Entity representing a data flow edge in the service mesh.
 * Defines a directed flow of data from a source service to a destination service
 * with associated data classifications.
 */
@Entity
@Table(name = "data_flow_edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFlowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service_id", nullable = false)
    private Long sourceServiceId;

    @Column(name = "destination_service_id", nullable = false)
    private Long destinationServiceId;

    @Column(name = "data_classes", nullable = false)
    @ElementCollection
    @CollectionTable(name = "edge_data_classes", joinColumns = @JoinColumn(name = "edge_id"))
    @Column(name = "data_class")
    private List<String> dataClasses;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}