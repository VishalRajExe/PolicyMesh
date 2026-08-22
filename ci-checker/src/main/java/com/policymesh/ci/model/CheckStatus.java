package com.policymesh.ci.model;

/**
 * Represents the result of evaluating a single policy against a data flow.
 * 
 * Consistent with the Spring Boot backend PolicyEngine semantics:
 * - If the policy does not apply (data class mismatch), return NOT_APPLICABLE.
 * - If the destination region is explicitly allowed, return ALLOW.
 * - If the destination region is explicitly denied, return DENY.
 * - If the policy applies but the destination region is not in the allowed list, return DENY.
 */
public enum CheckStatus {
    ALLOW,
    DENY,
    NOT_APPLICABLE
}
