package com.policymesh.servicegraph.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for service node responses.
 * Contains the data that will be returned to clients in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceNodeResponse {

    private Long id;
    private String name;
    private String region;
    private String meshZone;
    private String environment;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}