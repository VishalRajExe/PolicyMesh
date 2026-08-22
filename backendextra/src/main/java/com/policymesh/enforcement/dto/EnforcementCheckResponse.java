package com.policymesh.enforcement.dto;

public record EnforcementCheckResponse(
        String decision,
        String policyId,
        String reason,
        String lineageHash
) {
}
