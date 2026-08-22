package com.policymesh.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for creating/updating a policy. Regions are expressed as
 * simple lists at the API boundary; internally they are compiled via the
 * PolicyCompiler / PolicyValidator.
 */
public record PolicyRequest(
        @NotBlank(message = "policyCode is required") String policyCode,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "jurisdiction is required") String jurisdiction,
        @NotBlank(message = "dataClass is required") String dataClass,
        List<String> allowedRegions,
        List<String> deniedRegions
) {
}
