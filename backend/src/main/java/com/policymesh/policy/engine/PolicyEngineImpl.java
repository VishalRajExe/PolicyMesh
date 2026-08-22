package com.policymesh.policy.engine;

import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the PolicyEngine that evaluates data transfer policies.
 * Checks policies based on data class and region restrictions to make authorization decisions.
 */
@Service
@RequiredArgsConstructor
public class PolicyEngineImpl implements PolicyEngine {

    private final PolicyRepository policyRepository;

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
    @Override
    public Decision evaluate(String sourceService, String destinationService,
                             String sourceRegion, String destinationRegion,
                             String dataClass, java.util.List<String> tags) {
        // Find all policies that apply to this data class
        List<Policy> applicablePolicies = policyRepository.findByDataClass(dataClass);

        // If no policies apply, allow by default (permissive default)
        if (applicablePolicies.isEmpty()) {
            return Decision.ALLOW;
        }

        // Check if any policy explicitly denies this transfer
        boolean deniesTransfer = applicablePolicies.stream()
                .anyMatch(policy ->
                    (policy.getDeniedRegions() != null &&
                     (policy.getDeniedRegions().contains(sourceRegion) ||
                      policy.getDeniedRegions().contains(destinationRegion)))
                );

        if (deniesTransfer) {
            return Decision.DENY;
        }

        // Check if all policies with allowed regions specified allow this transfer
        boolean allAllowingPoliciesPermit = applicablePolicies.stream()
                .filter(policy -> policy.getAllowedRegions() != null && !policy.getAllowedRegions().isEmpty())
                .allMatch(policy ->
                    policy.getAllowedRegions().contains(sourceRegion) &&
                    policy.getAllowedRegions().contains(destinationRegion)
                );

        // If there are policies with allowed regions specified and they all permit, allow
        boolean hasAllowedRegionPolicies = applicablePolicies.stream()
                .anyMatch(policy -> policy.getAllowedRegions() != null && !policy.getAllowedRegions().isEmpty());

        if (hasAllowedRegionPolicies && allAllowingPoliciesPermit) {
            return Decision.ALLOW;
        }

        // If we have policies but they don't explicitly allow or deny, suggest reroute
        // This could indicate incomplete policy configuration requiring manual review
        return Decision.REROUTE;
    }
}