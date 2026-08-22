package com.policymesh.enforcement.engine;

import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The single centralized policy decision engine. ALL enforcement and
 * graph-validation decisions must go through {@link #evaluate}. Logic is
 * deliberately deterministic and side-effect free so it is trivially
 * unit-testable.
 *
 * Rules (per spec section 10):
 *  - If no policy applies to the data class/jurisdiction: configurable default.
 *  - If the destination region is explicitly denied: DENY.
 *  - If the destination region is explicitly allowed: ALLOW.
 *  - If a policy applies and the destination is neither allowed nor denied
 *    (i.e. there's no allowed route for it): DENY.
 *  - Otherwise: ALLOW.
 */
@Service
@RequiredArgsConstructor
public class PolicyEngine {

    private final PolicyService policyService;

    @Value("${policymesh.policy.default-decision:ALLOW}")
    private String defaultDecision;

    public PolicyDecisionResult evaluate(String sourceRegion,
                                          String destinationRegion,
                                          String dataClass,
                                          String sourceService,
                                          String destinationService) {

        String jurisdiction = normalize(sourceRegion);
        String destRegion = normalize(destinationRegion);
        String normalizedDataClass = normalize(dataClass);

        Optional<CompiledPolicy> policyOpt = policyService.resolveCompiledPolicy(jurisdiction, normalizedDataClass);

        if (policyOpt.isEmpty()) {
            PolicyDecisionResult.Decision fallback = "DENY".equalsIgnoreCase(defaultDecision)
                    ? PolicyDecisionResult.Decision.DENY
                    : PolicyDecisionResult.Decision.ALLOW;
            String reason = "No policy applies to jurisdiction=" + jurisdiction + " dataClass=" + normalizedDataClass
                    + "; default decision (" + fallback + ") applied";
            return new PolicyDecisionResult(fallback, "NO-POLICY", reason);
        }

        CompiledPolicy policy = policyOpt.get();

        if (policy.isRegionDenied(destRegion)) {
            return PolicyDecisionResult.deny(policy.id(),
                    jurisdiction + " " + normalizedDataClass + " cannot be transferred to " + destRegion);
        }

        if (policy.isRegionAllowed(destRegion)) {
            return PolicyDecisionResult.allow(policy.id(), "Destination region permitted");
        }

        // Policy applies but destination is neither explicitly allowed nor denied:
        // there is no allowed route, so default to DENY for safety.
        return PolicyDecisionResult.deny(policy.id(),
                "No allowed route exists from " + jurisdiction + " to " + destRegion + " for " + normalizedDataClass);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
