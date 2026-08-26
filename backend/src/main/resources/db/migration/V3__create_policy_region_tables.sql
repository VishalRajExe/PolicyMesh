-- V3: Create policy region tables if they don't exist.
-- Minimal DDL - no named constraints to avoid conflicts on re-runs.
CREATE TABLE IF NOT EXISTS policy_allowed_regions (
    policy_id BIGINT      NOT NULL,
    region    VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_denied_regions (
    policy_id BIGINT      NOT NULL,
    region    VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
