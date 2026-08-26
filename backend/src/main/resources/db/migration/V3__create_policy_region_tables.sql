-- =========================================================
-- V3: Ensure policy_allowed_regions and policy_denied_regions
--     tables exist. Applied when Flyway first runs on the
--     live database (baseline-on-migrate=true, baseline=V2).
-- =========================================================

CREATE TABLE IF NOT EXISTS policy_allowed_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    CONSTRAINT fk_par_policy FOREIGN KEY (policy_id)
        REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_denied_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    CONSTRAINT fk_pdr_policy FOREIGN KEY (policy_id)
        REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
