package com.policymesh.policy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "policy_code", nullable = false, unique = true)
    private String policyCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String jurisdiction;

    @Column(name = "data_class", nullable = false)
    private String dataClass;

    /** Comma-separated region codes. Kept simple for the hackathon; a join table would be the production evolution. */
    @Column(name = "allowed_regions")
    @Builder.Default
    private String allowedRegions = "";

    @Column(name = "denied_regions")
    @Builder.Default
    private String deniedRegions = "";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Builder.Default
    private int version = 1;

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
    public List<String> allowedRegionsList() {
        if (allowedRegions == null || allowedRegions.isBlank()) return new ArrayList<>();
        return List.of(allowedRegions.split(","));
    }

    @Transient
    public List<String> deniedRegionsList() {
        if (deniedRegions == null || deniedRegions.isBlank()) return new ArrayList<>();
        return List.of(deniedRegions.split(","));
    }
}
