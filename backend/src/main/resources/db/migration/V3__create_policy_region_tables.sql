-- =========================================================
-- V3: Ensure policy_allowed_regions and policy_denied_regions
--     tables exist. These were defined in V1 but may have
--     been absent from the live database if V1 was applied
--     before these tables were added to the schema.
--
-- Using CREATE TABLE IF NOT EXISTS makes this migration
-- idempotent and safe to run on databases that already have
-- the tables.
-- =========================================================

CREATE TABLE IF NOT EXISTS policy_allowed_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    INDEX idx_policy_allowed_regions (policy_id),
    CONSTRAINT fk_allowed_policy FOREIGN KEY (policy_id)
        REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_denied_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    INDEX idx_policy_denied_regions (policy_id),
    CONSTRAINT fk_denied_policy FOREIGN KEY (policy_id)
        REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
