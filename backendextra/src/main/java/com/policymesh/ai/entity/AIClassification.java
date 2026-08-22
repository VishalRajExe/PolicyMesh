package com.policymesh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_classifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIClassification {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "sample_value")
    private String sampleValue;

    @Column(name = "suggested_class", nullable = false)
    private String suggestedClass;

    @Column(nullable = false)
    private double confidence;

    @Builder.Default
    @Column(nullable = false)
    private boolean approved = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean rejected = false;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
