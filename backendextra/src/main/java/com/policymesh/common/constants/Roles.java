package com.policymesh.common.constants;

/**
 * Role name constants matching {@link com.policymesh.auth.entity.Role}.
 * Used with Spring Security's hasRole()/hasAuthority() checks.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String ENGINEER = "ENGINEER";
    public static final String VIEWER = "VIEWER";

    private Roles() {
    }
}
