package com.policymesh.ci.engine;

import com.policymesh.ci.model.CheckStatus;
import com.policymesh.ci.model.Policy;

/**
 * Evaluates a single policy against a data flow.
 *
 * Decision logic (deterministic, consistent with Spring Boot backend):
 *
 * 1. If the policy does not apply (data class mismatch) -> NOT_APPLICABLE
 * 2. If the destination region is in the deniedRegions list -> DENY
 * 3. If the destination region is in the allowedRegions list -> ALLOW
 * 4. If the destination region is NOT in the allowedRegions list -> DENY
 *
 * This means:
 * - DENIED regions take precedence as an explicit block.
 * - If a region is not explicitly allowed, it is denied.
 * - Only regions on the allow-list pass.
 *
 * This logic is intentionally simple and deterministic to ensure
 * consistency between the CI checker and the runtime backend.
 */
public class PolicyEvaluator {

    /**
     * Evaluates whether a data flow complies with a given policy.
     *
     * @param policy the policy to evaluate
     * @param sourceRegion the region of the source service
     * @param destinationRegion the region of the destination service
     * @param dataClass the data class being transferred (e.g., "PII")
     * @return PolicyDecision indicating ALLOW, DENY, or NOT_APPLICABLE
     */
    public CheckStatus evaluate(Policy policy, String sourceRegion, String destinationRegion, String dataClass) {
        if (policy == null || sourceRegion == null || destinationRegion == null || dataClass == null) {
            return CheckStatus.NOT_APPLICABLE;
        }

        // Step 1: Check if the policy applies to this data class
        if (!policy.appliesToDataClass(dataClass)) {
            return CheckStatus.NOT_APPLICABLE;
        }

        // Step 1b: Check if the policy applies based on jurisdiction/source region.
        // A policy with jurisdiction "EU" should only apply when the source is in the EU.
        // If jurisdiction is not set, the policy applies universally for the data class.
        if (policy.getJurisdiction() != null && !policy.getJurisdiction().isBlank()) {
            if (!sourceRegion.equalsIgnoreCase(policy.getJurisdiction())) {
                return CheckStatus.NOT_APPLICABLE;
            }
        }

        // Step 2: Check explicitly denied regions (takes precedence)
        if (policy.getDeniedRegions().contains(destinationRegion)) {
            return CheckStatus.DENY;
        }

        // Step 3: Check if the destination region is explicitly allowed
        if (policy.getAllowedRegions().contains(destinationRegion)) {
            return CheckStatus.ALLOW;
        }

        // Step 4: If the destination is not explicitly allowed, deny
        // This is the "default deny" principle
        return CheckStatus.DENY;
    }
}
