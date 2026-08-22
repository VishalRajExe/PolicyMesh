package com.policymesh.compiler;

import org.springframework.stereotype.Component;

@Component
public class PolicyValidator {

    public void validate(CompiledPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }

        if (policy.getPolicyCode() == null || policy.getPolicyCode().isBlank()) {
            throw new IllegalArgumentException("Policy code is required");
        }

        if (policy.getName() == null || policy.getName().isBlank()) {
            throw new IllegalArgumentException("Policy name is required");
        }

        if (policy.getJurisdiction() == null || policy.getJurisdiction().isBlank()) {
            throw new IllegalArgumentException("Policy jurisdiction is required");
        }

        if (policy.getDataClass() == null || policy.getDataClass().isBlank()) {
            throw new IllegalArgumentException("Policy data class is required");
        }

        // Validate that allowedRegions and deniedRegions don't overlap
        if (policy.getAllowedRegions() != null && policy.getDeniedRegions() != null) {
            for (String allowedRegion : policy.getAllowedRegions()) {
                if (policy.getDeniedRegions().contains(allowedRegion)) {
                    throw new IllegalArgumentException(
                            "Region cannot be both allowed and denied: " + allowedRegion);
                }
            }
        }
    }
}