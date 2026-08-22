package com.policymesh.common.constants;

public class AppConstants {
    public static final String API_BASE_PATH = "/api/v1";

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String ROLE_ENGINEER = "ENGINEER";
    public static final String ROLE_VIEWER = "VIEWER";

    // Decision types
    public static final String DECISION_ALLOW = "ALLOW";
    public static final String DECISION_DENY = "DENY";
    public static final String DECISION_REROUTE = "REROUTE";

    // Cache keys
    public static final String POLICY_CACHE_PREFIX = "policy:";

    // Kafka topics
    public static final String KAFKA_TOPIC_POLICY_UPDATED = "policymesh.policy.updated";
    public static final String KAFKA_TOPIC_DECISION_CREATED = "policymesh.decision.created";
    public static final String KAFKA_TOPIC_LINEAGE_CREATED = "policymesh.lineage.created";
    public static final String KAFKA_TOPIC_CI_COMPLETED = "policymesh.ci.completed";
}