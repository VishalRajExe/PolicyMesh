-- =========================================================
-- PolicyMesh initial schema (MySQL 8.x Compatible)
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255)        NOT NULL UNIQUE,
    password_hash   VARCHAR(255)        NOT NULL,
    role            VARCHAR(64)         NOT NULL,
    status          VARCHAR(50)         NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_code     VARCHAR(100)        NOT NULL UNIQUE,
    name            VARCHAR(255)        NOT NULL,
    jurisdiction    VARCHAR(100)        NOT NULL,
    data_class      VARCHAR(100)        NOT NULL,
    status          VARCHAR(50)         NOT NULL DEFAULT 'DRAFT',
    version         INT                 NOT NULL DEFAULT 1,
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_policies_policy_code (policy_code),
    INDEX idx_policies_jurisdiction (jurisdiction),
    INDEX idx_policy_data_class (data_class)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_allowed_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    INDEX idx_policy_allowed_regions (policy_id),
    CONSTRAINT fk_allowed_policy FOREIGN KEY (policy_id) REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_denied_regions (
    policy_id       BIGINT              NOT NULL,
    region          VARCHAR(100)        NOT NULL,
    INDEX idx_policy_denied_regions (policy_id),
    CONSTRAINT fk_denied_policy FOREIGN KEY (policy_id) REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS service_nodes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL UNIQUE,
    region          VARCHAR(100)        NOT NULL,
    mesh_zone       VARCHAR(100),
    environment     VARCHAR(50)         NOT NULL DEFAULT 'production',
    description     VARCHAR(1000),
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_service_nodes_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_flow_edges (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_service_id        BIGINT NOT NULL,
    destination_service_id   BIGINT NOT NULL,
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_edges_source (source_service_id),
    INDEX idx_edges_dest (destination_service_id),
    UNIQUE KEY uk_source_dest (source_service_id, destination_service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_flow_edge_classes (
    edge_id                  BIGINT NOT NULL,
    data_class               VARCHAR(100) NOT NULL,
    INDEX idx_edge_classes (edge_id),
    CONSTRAINT fk_edge_classes FOREIGN KEY (edge_id) REFERENCES data_flow_edges (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS decisions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_service          VARCHAR(255) NOT NULL,
    destination_service     VARCHAR(255) NOT NULL,
    source_region           VARCHAR(100) NOT NULL,
    destination_region      VARCHAR(100) NOT NULL,
    data_class              VARCHAR(255) NOT NULL,
    decision                VARCHAR(20)  NOT NULL,
    policy_id               VARCHAR(100),
    reason                  VARCHAR(1000) NOT NULL,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_decisions_decision (decision),
    INDEX idx_decision_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lineage_records (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    decision_id             BIGINT NOT NULL UNIQUE,
    source_service          VARCHAR(255) NOT NULL,
    destination_service     VARCHAR(255) NOT NULL,
    source_region           VARCHAR(100) NOT NULL,
    destination_region      VARCHAR(100) NOT NULL,
    data_class              VARCHAR(255) NOT NULL,
    decision                VARCHAR(50)  NOT NULL,
    reason                  VARCHAR(1000) NOT NULL,
    policy_id               VARCHAR(100),
    previous_hash           VARCHAR(128),
    current_hash            VARCHAR(128) NOT NULL,
    signature               VARCHAR(512),
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_lineage_current_hash (current_hash),
    INDEX idx_lineage_decision (decision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ci_scans (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    commit_hash             VARCHAR(100) NOT NULL,
    branch                  VARCHAR(255) NOT NULL,
    status                  VARCHAR(50)  NOT NULL,
    violation_count         INT          NOT NULL DEFAULT 0,
    started_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at            DATETIME(6),
    violations_json         TEXT,
    INDEX idx_ci_scans_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_classifications (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name              VARCHAR(255) NOT NULL,
    sample_value            VARCHAR(2000),
    classification          VARCHAR(100) NOT NULL,
    confidence              DOUBLE       NOT NULL,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    reviewed_by             VARCHAR(255),
    provider                VARCHAR(50)  NOT NULL DEFAULT 'heuristic',
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ai_class_field (field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
