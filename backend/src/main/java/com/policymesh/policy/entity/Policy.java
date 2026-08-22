package com.policymesh.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code", unique = true, nullable = false)
    private String policyCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String jurisdiction;

    @Column(name = "data_class", nullable = false)
    private String dataClass;

    @ElementCollection
    @CollectionTable(name = "policy_allowed_regions", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "region")
    private List<String> allowedRegions;

    @ElementCollection
    @CollectionTable(name = "policy_denied_regions", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "region")
    private List<String> deniedRegions;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}