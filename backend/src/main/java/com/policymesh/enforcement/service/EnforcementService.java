package com.policymesh.enforcement.service;

import com.policymesh.enforcement.dto.EnforcementRequest;
import com.policymesh.enforcement.dto.EnforcementResponse;
import com.policymesh.policy.engine.Decision;

/**
 * Service interface for enforcement operations.
 * Handles checking if data transfers are allowed based on policies.
 */
public interface EnforcementService {

    /**
     * Checks if a data transfer between services is allowed based on policies.
     *
     * @param request the enforcement check request
     * @return the enforcement response containing the decision and details
     */
    EnforcementResponse checkEnforcement(EnforcementRequest request);
}