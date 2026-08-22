package com.policymesh.policy.engine;

/**
 * Enum representing the possible decisions that can be made by the policy engine.
 * ALLOW: The data transfer is permitted.
 * DENY: The data transfer is prohibited.
 * REROUTE: The data transfer should be rerouted to comply with policies.
 */
public enum Decision {
    ALLOW,
    DENY,
    REROUTE
}