-- =========================================================
-- PolicyMesh initial schema
-- =========================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL,
    email           VARCHAR(255)        NOT NULL UNIQUE,
    password_hash   VARCHAR(255)        NOT NULL,
    role            VARCHAR(50)         NOT NULL,
    enabled         BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE TABLE policies (
    id              UUID PRIMARY KEY,
    policy_code     VARCHAR(100)        NOT NULL UNIQUE,
    name            VARCHAR(255)        NOT NULL,
    jurisdiction    VARCHAR(100)        NOT NULL,
    data_class      VARCHAR(100)        NOT NULL,
    allowed_regions VARCHAR(1000),
    denied_regions  VARCHAR(1000),
    status          VARCHAR(50)         NOT NULL DEFAULT 'ACTIVE',
    version         INTEGER             NOT NULL DEFAULT 1,
    created_at      TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP           NOT NULL DEFAULT now()
);
CREATE INDEX idx_policies_policy_code  ON policies (policy_code);
CREATE INDEX idx_policies_jurisdiction ON policies (jurisdiction);
CREATE INDEX idx_policies_data_class   ON policies (data_class);

CREATE TABLE service_nodes (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL UNIQUE,
    region          VARCHAR(100)        NOT NULL,
    mesh_zone       VARCHAR(100),
    environment     VARCHAR(50)         NOT NULL DEFAULT 'production',
    description     VARCHAR(1000),
    created_at      TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP           NOT NULL DEFAULT now()
);
CREATE INDEX idx_service_nodes_region ON service_nodes (region);

CREATE TABLE data_flow_edges (
    id                       UUID PRIMARY KEY,
    source_service_id        UUID NOT NULL REFERENCES service_nodes (id) ON DELETE CASCADE,
    destination_service_id   UUID NOT NULL REFERENCES service_nodes (id) ON DELETE CASCADE,
    data_classes              VARCHAR(500) NOT NULL,
    created_at                TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_edges_source ON data_flow_edges (source_service_id);
CREATE INDEX idx_edges_dest   ON data_flow_edges (destination_service_id);

CREATE TABLE decisions (
    id                      UUID PRIMARY KEY,
    source_service_id       UUID REFERENCES service_nodes (id),
    destination_service_id  UUID REFERENCES service_nodes (id),
    source_service_name     VARCHAR(255) NOT NULL,
    destination_service_name VARCHAR(255) NOT NULL,
    source_region           VARCHAR(100) NOT NULL,
    destination_region      VARCHAR(100) NOT NULL,
    data_class              VARCHAR(100) NOT NULL,
    decision                VARCHAR(20)  NOT NULL,
    reason                  VARCHAR(1000),
    policy_id               UUID REFERENCES policies (id),
    policy_code             VARCHAR(100),
    timestamp                TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_decisions_source_service ON decisions (source_service_id);
CREATE INDEX idx_decisions_dest_service   ON decisions (destination_service_id);
CREATE INDEX idx_decisions_decision       ON decisions (decision);
CREATE INDEX idx_decisions_timestamp      ON decisions (timestamp);

CREATE TABLE lineage_records (
    id              UUID PRIMARY KEY,
    decision_id     UUID NOT NULL REFERENCES decisions (id),
    sequence_no     BIGINT NOT NULL,
    previous_hash   VARCHAR(128),
    current_hash    VARCHAR(128) NOT NULL,
    signature       VARCHAR(500),
    status          VARCHAR(50)  NOT NULL DEFAULT 'UNSIGNED',
    timestamp       TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_lineage_sequence ON lineage_records (sequence_no);
CREATE INDEX idx_lineage_decision ON lineage_records (decision_id);
CREATE INDEX idx_lineage_timestamp ON lineage_records (timestamp);

CREATE TABLE ci_scans (
    id               UUID PRIMARY KEY,
    commit_hash      VARCHAR(100),
    branch           VARCHAR(255),
    status           VARCHAR(50) NOT NULL,
    violation_count  INTEGER NOT NULL DEFAULT 0,
    report_json      TEXT,
    started_at       TIMESTAMP NOT NULL DEFAULT now(),
    completed_at     TIMESTAMP
);
CREATE INDEX idx_ci_scans_status ON ci_scans (status);

CREATE TABLE ai_classifications (
    id                UUID PRIMARY KEY,
    field_name        VARCHAR(255) NOT NULL,
    sample_value      VARCHAR(500),
    suggested_class   VARCHAR(100) NOT NULL,
    confidence        DOUBLE PRECISION NOT NULL,
    approved          BOOLEAN NOT NULL DEFAULT FALSE,
    rejected          BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by       UUID REFERENCES users (id),
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_class_field ON ai_classifications (field_name);
