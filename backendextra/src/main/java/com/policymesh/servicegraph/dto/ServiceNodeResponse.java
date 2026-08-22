package com.policymesh.servicegraph.dto;

import com.policymesh.servicegraph.entity.ServiceNode;

import java.time.Instant;
import java.util.UUID;

public record ServiceNodeResponse(
        UUID id,
        String name,
        String region,
        String meshZone,
        String environment,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServiceNodeResponse from(ServiceNode s) {
        return new ServiceNodeResponse(s.getId(), s.getName(), s.getRegion(), s.getMeshZone(),
                s.getEnvironment(), s.getDescription(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
