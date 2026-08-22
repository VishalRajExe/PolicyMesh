package com.policymesh.enforcement.service;

import com.policymesh.enforcement.dto.EnforcementRequest;
import com.policymesh.enforcement.dto.EnforcementResponse;
import com.policymesh.lineage.service.LineageService;
import com.policymesh.policy.engine.Decision;
import com.policymesh.policy.engine.PolicyEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of the EnforcementService interface.
 * Uses PolicyEngine to make enforcement decisions and integrates with lineage tracking.
 */
@Service
@RequiredArgsConstructor
public class EnforcementServiceImpl implements EnforcementService {

    private final PolicyEngine policyEngine;
    private final LineageService lineageService;

    /**
     * Checks if a data transfer between services is allowed based on policies.
     *
     * @param request the enforcement check request
     * @return the enforcement response containing the decision and details
     */
    @Override
    public EnforcementResponse checkEnforcement(EnforcementRequest request) {
        // Use the policy engine to make a decision
        Decision decision = policyEngine.evaluate(
                request.getSourceService(),
                request.getDestinationService(),
                request.getSourceRegion(),
                request.getDestinationRegion(),
                request.getDataClass(),
                request.getTags()
        );

        // Create a lineage record for this enforcement check
        String lineageHash = lineageService.createEnforcementRecord(
                request.getSourceService(),
                request.getDestinationService(),
                request.getSourceRegion(),
                request.getDestinationRegion(),
                request.getDataClass(),
                request.getTags(),
                decision
        );

        // Build and return the response
        return new EnforcementResponse(
                decision,
                decision.name(), // Using decision name as policyId for now - could be enhanced
                getReasonForDecision(decision, request),
                lineageHash
        );
    }

    /**
     * Generates a human-readable reason for the enforcement decision.
     *
     * @param decision the enforcement decision
     * @param request the enforcement request
     * @return a string explaining the decision
     */
    private String getReasonForDecision(Decision decision, EnforcementRequest request) {
        switch (decision) {
            case ALLOW:
                return "Data transfer from " + request.getSourceService() + " to " +
                       request.getDestinationService() + " is allowed based on current policies.";
            case DENY:
                return "Data transfer from " + request.getSourceService() + " to " +
                       request.getDestinationService() + " is denied due to policy restrictions.";
            case REROUTE:
                return "Data transfer from " + request.getSourceService() + " to " +
                       request.getDestinationService() + " should be rerouted to comply with data residency policies.";
            default:
                return "Unable to determine policy decision for data transfer.";
        }
    }
}