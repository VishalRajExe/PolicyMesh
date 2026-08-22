package com.policymesh.auth.entity;

/**
 * Roles:
 *  ADMIN               - full access
 *  COMPLIANCE_OFFICER   - create/update policy, view lineage, run checks
 *  ENGINEER             - manage services, view policies, run CI checks, runtime testing
 *  VIEWER               - dashboard, graph, lineage (read-only)
 */
public enum Role {
    ADMIN,
    COMPLIANCE_OFFICER,
    ENGINEER,
    VIEWER
}
