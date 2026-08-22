-- =========================================================
-- PolicyMesh initial schema (PostgreSQL)
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255)        NOT NULL UNIQUE,
    password_hash   VARCHAR(255)        NOT NULL,
    role            VARCHAR(50)         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS policies (
    id              BIGSERIAL PRIMARY KEY,
    policy_code     VARCHAR(100)        NOT NULL UNIQUE,
    name            VARCHAR(255)        NOT NULL,
    jurisdiction    VARCHAR(100)        NOT NULL,
    data_class      VARCHAR(100)        NOT NULL,
    status          VARCHAR(50)         NOT NULL DEFAULT 'DRAFT',
    version         INTEGER             NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_policies_policy_code  ON policies (policy_code);
CREATE INDEX IF NOT EXISTS idx_policies_jurisdiction ON policies (jurisdiction);
CREATE INDEX IF NOT EXISTS idx_policy_data_class     ON policies (data_class);

CREATE TABLE IF NOT EXISTS policy_allowed_regions (
    policy_id       BIGINT              NOT NULL REFERENCES policies (id) ON DELETE CASCADE,
    region          VARCHAR(100)        NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_policy_allowed_regions ON policy_allowed_regions (policy_id);

CREATE TABLE IF NOT EXISTS policy_denied_regions (
    policy_id       BIGINT              NOT NULL REFERENCES policies (id) ON DELETE CASCADE,
    region          VARCHAR(100)        NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_policy_denied_regions ON policy_denied_regions (policy_id);

CREATE TABLE IF NOT EXISTS service_nodes (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL UNIQUE,
    region          VARCHAR(100)        NOT NULL,
    mesh_zone       VARCHAR(100),
    environment     VARCHAR(50)         NOT NULL DEFAULT 'production',
    description     VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_service_nodes_region ON service_nodes (region);

CREATE TABLE IF NOT EXISTS data_flow_edges (
    id                       BIGSERIAL PRIMARY KEY,
    source_service_id        BIGINT NOT NULL,
    destination_service_id   BIGINT NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_edges_source ON data_flow_edges (source_service_id);
CREATE INDEX IF NOT EXISTS idx_edges_dest   ON data_flow_edges (destination_service_id);

CREATE TABLE IF NOT EXISTS data_flow_edge_classes (
    edge_id                  BIGINT NOT NULL REFERENCES data_flow_edges (id) ON DELETE CASCADE,
    data_class               VARCHAR(100) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_edge_classes ON data_flow_edge_classes (edge_id);

CREATE TABLE IF NOT EXISTS decisions (
    id                      BIGSERIAL PRIMARY KEY,
    source_service          VARCHAR(255) NOT NULL,
    destination_service     VARCHAR(255) NOT NULL,
    source_region           VARCHAR(100) NOT NULL,
    destination_region      VARCHAR(100) NOT NULL,
    data_class              VARCHAR(255) NOT NULL,
    decision                VARCHAR(20)  NOT NULL,
    policy_id               VARCHAR(100),
    reason                  VARCHAR(1000) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_decisions_decision  ON decisions (decision);
CREATE INDEX IF NOT EXISTS idx_decision_created    ON decisions (created_at);

CREATE TABLE IF NOT EXISTS lineage_records (
    id                      BIGSERIAL PRIMARY KEY,
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
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_lineage_current_hash ON lineage_records (current_hash);
CREATE INDEX IF NOT EXISTS idx_lineage_decision     ON lineage_records (decision_id);

CREATE TABLE IF NOT EXISTS ci_scans (
    id                      BIGSERIAL PRIMARY KEY,
    commit_hash             VARCHAR(100) NOT NULL,
    branch                  VARCHAR(255) NOT NULL,
    status                  VARCHAR(50) NOT NULL,
    violation_count         INTEGER NOT NULL DEFAULT 0,
    started_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at            TIMESTAMP WITH TIME ZONE,
    violations_json         TEXT
);
CREATE INDEX IF NOT EXISTS idx_ci_scans_status ON ci_scans (status);

CREATE TABLE IF NOT EXISTS ai_classifications (
    id                      BIGSERIAL PRIMARY KEY,
    field_name              VARCHAR(255) NOT NULL,
    sample_value            VARCHAR(500),
    classification          VARCHAR(100) NOT NULL,
    confidence              DOUBLE PRECISION NOT NULL,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    reviewed_by             VARCHAR(255),
    provider                VARCHAR(50)  NOT NULL DEFAULT 'heuristic',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_class_field ON ai_classifications (field_name);
