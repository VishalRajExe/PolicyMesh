package com.policymesh.servicegraph.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record DataFlowEdgeRequest(
        @NotNull(message = "sourceServiceId is required") UUID sourceServiceId,
        @NotNull(message = "destinationServiceId is required") UUID destinationServiceId,
        @NotEmpty(message = "dataClasses must contain at least one classification") List<String> dataClasses
) {
}
