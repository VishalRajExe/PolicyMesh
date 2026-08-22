package com.policymesh.servicegraph.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util List;

/**
 * DTO for data flow edge responses.
 * Contains the data that will be returned to clients in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFlowEdgeResponse {

    private Long id;
    private Long sourceServiceId;
    private Long destinationServiceId;
    private List<String> dataClasses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}