package com.policymesh.lineage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lineage_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineageRecord {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    /** Monotonically increasing chain position, starting at 1. Used to walk the chain in order. */
    @Column(name = "sequence_no", nullable = false, unique = true)
    private long sequenceNo;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "current_hash", nullable = false)
    private String currentHash;

    /** Reserved for future cryptographic signing; left null until a signer is wired in. */
    private String signature;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LineageStatus status = LineageStatus.UNSIGNED;

    @Builder.Default
    @Column(nullable = false)
    private Instant timestamp = Instant.now();
}
