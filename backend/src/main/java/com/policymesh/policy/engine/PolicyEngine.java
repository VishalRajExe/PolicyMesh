package com.policymesh.policy.engine;

import com.policymesh.enforcement.dto.EnforcementRequest;

/**
 * Interface for the policy engine that makes centralized decisions about data transfers.
 * The policy engine evaluates whether a data transfer is allowed based on policies
 * considering source/destination services, regions, data classification, and tags.
 */
public interface PolicyEngine {

    /**
     * Evaluates if a data transfer between services is allowed based on policies.
     *
     * @param sourceService the source service initiating the data transfer
     * @param destinationService the destination service for the data transfer
     * @param sourceRegion the region of the source service
     * @param destinationRegion the region of the destination service
     * @param dataClass the classification of the data being transferred
     * @param tags additional tags associated with the data
     * @return the enforcement decision (ALLOW, DENY, or REROUTE)
     */
    Decision evaluate(String sourceService, String destinationService,
                      String sourceRegion, String destinationRegion,
                      String dataClass, java.util.List<String> tags);
}