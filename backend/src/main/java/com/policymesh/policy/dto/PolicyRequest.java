package com.policymesh.policy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRequest {
    @NotBlank
    private String policyCode;

    @NotBlank
    private String name;

    @NotBlank
    private String jurisdiction;

    @NotBlank
    private String dataClass;

    @NotEmpty
    private List<String> allowedRegions;

    private List<String> deniedRegions;

    @NotBlank
    private String status;

    @NotNull
    @Positive
    private Integer version;
}