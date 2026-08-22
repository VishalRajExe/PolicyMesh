package com.policymesh.enforcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EnforcementCheckRequest(
        @NotBlank(message = "sourceService is required") String sourceService,
        @NotBlank(message = "destinationService is required") String destinationService,
        @NotBlank(message = "sourceRegion is required") String sourceRegion,
        @NotBlank(message = "destinationRegion is required") String destinationRegion,
        @NotEmpty(message = "dataClassTags must contain at least one classification") List<String> dataClassTags
) {
}
