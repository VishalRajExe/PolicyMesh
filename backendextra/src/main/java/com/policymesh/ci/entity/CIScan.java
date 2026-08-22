package com.policymesh.ci.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ci_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CIScan {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "commit_hash")
    private String commitHash;

    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CIScanStatus status;

    @Column(name = "violation_count", nullable = false)
    @Builder.Default
    private int violationCount = 0;

    @Column(name = "report_json", columnDefinition = "TEXT")
    private String reportJson;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
