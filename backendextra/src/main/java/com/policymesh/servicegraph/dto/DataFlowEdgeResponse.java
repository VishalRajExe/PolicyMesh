package com.policymesh.servicegraph.dto;

import com.policymesh.servicegraph.entity.DataFlowEdge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataFlowEdgeResponse(
        UUID id,
        UUID sourceServiceId,
        String sourceServiceName,
        UUID destinationServiceId,
        String destinationServiceName,
        List<String> dataClasses,
        Instant createdAt
) {
    public static DataFlowEdgeResponse from(DataFlowEdge e) {
        return new DataFlowEdgeResponse(
                e.getId(),
                e.getSource().getId(), e.getSource().getName(),
                e.getDestination().getId(), e.getDestination().getName(),
                e.dataClassList(), e.getCreatedAt());
    }
}
