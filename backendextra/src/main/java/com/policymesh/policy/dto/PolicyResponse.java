package com.policymesh.policy.dto;

import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.entity.PolicyStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String policyCode,
        String name,
        String jurisdiction,
        String dataClass,
        List<String> allowedRegions,
        List<String> deniedRegions,
        PolicyStatus status,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static PolicyResponse from(Policy p) {
        return new PolicyResponse(
                p.getId(), p.getPolicyCode(), p.getName(), p.getJurisdiction(), p.getDataClass(),
                p.allowedRegionsList(), p.deniedRegionsList(), p.getStatus(), p.getVersion(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
