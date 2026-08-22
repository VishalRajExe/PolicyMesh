package com.policymesh.servicegraph.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util List;

/**
 * DTO for creating/updating data flow edge requests.
 * Contains validation constraints for required fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFlowEdgeRequest {

    @NotNull(message = "Source service ID is required")
    private Long sourceServiceId;

    @NotNull(message = "Destination service ID is required")
    private Long destinationServiceId;

    @NotEmpty(message = "At least one data class is required")
    private List<String> dataClasses;
}