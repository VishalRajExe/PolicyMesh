package com.policymesh.lineage.service;

import java.util.List;

/**
 * Service interface for lineage tracking operations.
 * Handles creating and managing lineage records for data transfers and enforcement checks.
 */
public interface LineageService {

    /**
     * Creates a lineage record for an enforcement check.
     *
     * @param sourceService the source service initiating the data transfer
     * @param destinationService the destination service for the data transfer
     * @param sourceRegion the region of the source service
     * @param destinationRegion the region of the destination service
     * @param dataClass the classification of the data being transferred
     * @param tags additional tags associated with the data
     * @param decision the enforcement decision made
     * @return a unique hash representing the lineage record
     */
    String createEnforcementRecord(
            String sourceService,
            String destinationService,
            String sourceRegion,
            String destinationRegion,
            String dataClass,
            java.util.List<String> tags,
            com.policymesh.policy.engine.Decision decision);
}