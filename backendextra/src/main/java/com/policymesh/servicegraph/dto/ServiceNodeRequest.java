package com.policymesh.servicegraph.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceNodeRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "region is required") String region,
        String meshZone,
        String environment,
        String description
) {
}
