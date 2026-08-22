package com.policymesh.enforcement.engine;

public record PolicyDecisionResult(
        Decision decision,
        String policyId,
        String reason
) {
    public enum Decision {
        ALLOW,
        DENY,
        REROUTE
    }

    public static PolicyDecisionResult allow(String policyId, String reason) {
        return new PolicyDecisionResult(Decision.ALLOW, policyId, reason);
    }

    public static PolicyDecisionResult deny(String policyId, String reason) {
        return new PolicyDecisionResult(Decision.DENY, policyId, reason);
    }

    public static PolicyDecisionResult reroute(String policyId, String reason) {
        return new PolicyDecisionResult(Decision.REROUTE, policyId, reason);
    }
}
