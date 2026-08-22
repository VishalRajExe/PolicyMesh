package com.policymesh.policy.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponse {
    private Long id;
    private String policyCode;
    private String name;
    private String jurisdiction;
    private String dataClass;
    private List<String> allowedRegions;
    private List<String> deniedRegions;
    private String status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}