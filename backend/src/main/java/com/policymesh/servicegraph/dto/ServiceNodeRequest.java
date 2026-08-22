package com.policymesh.servicegraph.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating/updating service node requests.
 * Contains validation constraints for required fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceNodeRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    @NotBlank(message = "Service region is required")
    private String region;

    @NotBlank(message = "Mesh zone is required")
    private String meshZone;

    @NotBlank(message = "Environment is required")
    private String environment;

    private String description;
}