package com.policymesh.enforcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for enforcement check requests.
 * Used to validate if a data transfer between services is allowed based on policies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementRequest {
    @NotBlank(message = "Source service is required")
    private String sourceService;

    @NotBlank(message = "Destination service is required")
    private String destinationService;

    @NotBlank(message = "Source region is required")
    private String sourceRegion;

    @NotBlank(message = "Destination region is required")
    private String destinationRegion;

    @NotBlank(message = "Data class is required")
    private String dataClass;

    private List<String> tags;
}