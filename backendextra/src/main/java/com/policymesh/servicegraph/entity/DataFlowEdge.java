package com.policymesh.servicegraph.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "data_flow_edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFlowEdge {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_service_id", nullable = false)
    private ServiceNode source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_service_id", nullable = false)
    private ServiceNode destination;

    /** Comma-separated data classification tags flowing across this edge, e.g. "PII,PCI". */
    @Column(name = "data_classes", nullable = false)
    private String dataClasses;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public List<String> dataClassList() {
        if (dataClasses == null || dataClasses.isBlank()) return new ArrayList<>();
        return List.of(dataClasses.split(","));
    }
}
