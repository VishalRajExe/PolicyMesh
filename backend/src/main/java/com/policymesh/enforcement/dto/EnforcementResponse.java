package com.policymesh.enforcement.dto;

import com.policymesh.policy.engine.Decision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for enforcement check responses.
 * Contains the result of evaluating if a data transfer is allowed based on policies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementResponse {
    private Decision decision;
    private String policyId;
    private String reason;
    private String lineageHash;
}