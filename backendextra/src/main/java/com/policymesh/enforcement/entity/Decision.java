package com.policymesh.enforcement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "source_service_id")
    private UUID sourceServiceId;

    @Column(name = "destination_service_id")
    private UUID destinationServiceId;

    @Column(name = "source_service_name", nullable = false)
    private String sourceServiceName;

    @Column(name = "destination_service_name", nullable = false)
    private String destinationServiceName;

    @Column(name = "source_region", nullable = false)
    private String sourceRegion;

    @Column(name = "destination_region", nullable = false)
    private String destinationRegion;

    @Column(name = "data_class", nullable = false)
    private String dataClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decision;

    private String reason;

    @Column(name = "policy_id")
    private UUID policyId;

    @Column(name = "policy_code")
    private String policyCode;

    @Builder.Default
    @Column(nullable = false)
    private Instant timestamp = Instant.now();
}
