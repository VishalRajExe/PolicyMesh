# DATABASE_SCHEMA.md

Authoritative Source of Truth: **MySQL 8.4**.
Cache: **Redis 7** (see [REDIS.md](./REDIS.md) for what is cached vs authoritative).
Policy Semantics: [POLICY_DSL.md](./POLICY_DSL.md).

## ER Diagram

```mermaid
erDiagram
    USER ||--o{ POLICY : creates
    POLICY ||--o{ DECISION : evaluated_by
    SERVICE_NODE ||--o{ DATA_FLOW_EDGE : source
    SERVICE_NODE ||--o{ DATA_FLOW_EDGE : destination
    DATA_FLOW_EDGE ||--o{ DECISION : produces
    DECISION ||--|| LINEAGE_RECORD : recorded_as
    POLICY ||--o{ CI_SCAN : validated_by
    AI_CLASSIFICATION }o--|| USER : approved_by

    USER {
        bigint id PK
        string email
        string passwordHash
        string role
        datetime createdAt
    }
    POLICY {
        bigint id PK
        string policyCode
        string name
        string jurisdiction
        string dataClass
        string status
        int version
        datetime createdAt
        datetime updatedAt
    }
    SERVICE_NODE {
        bigint id PK
        string name
        string region
        string meshZone
        string environment
        string description
        datetime createdAt
        datetime updatedAt
    }
    DATA_FLOW_EDGE {
        bigint id PK
        bigint sourceServiceId
        bigint destinationServiceId
        datetime createdAt
        datetime updatedAt
    }
    DECISION {
        bigint id PK
        string sourceService
        string destinationService
        string sourceRegion
        string destinationRegion
        string dataClass
        string decision
        string reason
        string policyId
        datetime createdAt
    }
    LINEAGE_RECORD {
        bigint id PK
        bigint decisionId FK
        string previousHash
        string currentHash
        string signature
        datetime createdAt
    }
    CI_SCAN {
        bigint id PK
        string commitHash
        string branch
        string status
        int violationCount
        datetime startedAt
        datetime completedAt
        text violationsJson
    }
    AI_CLASSIFICATION {
        bigint id PK
        string fieldName
        string sampleValue
        string classification
        double confidence
        string status
        string provider
        string reviewedBy
        datetime createdAt
    }
```

## Tables Overview

### 1. `users`
**Purpose:** Authentication and role-based access control.
- `id` BIGINT AUTO_INCREMENT PK
- `email` VARCHAR(255) UNIQUE NOT NULL
- `password_hash` VARCHAR(255) NOT NULL (BCrypt hash)
- `role` VARCHAR(50) NOT NULL (`ADMIN`, `SECURITY_ADMIN`, `AUDITOR`, `VIEWER`)
- `created_at` DATETIME(6) NOT NULL

### 2. `policies` & Region Tables
**Purpose:** Declarative data-residency compliance rules.
- `policies`: `id` BIGINT PK, `policy_code` VARCHAR(100) UNIQUE, `name` VARCHAR(255), `jurisdiction` VARCHAR(100), `data_class` VARCHAR(100), `status` VARCHAR(50), `version` INT, `created_at`, `updated_at`.
- `policy_allowed_regions`: `policy_id` BIGINT FK, `region` VARCHAR(100).
- `policy_denied_regions`: `policy_id` BIGINT FK, `region` VARCHAR(100).

### 3. `service_nodes`
**Purpose:** Nodes in the service mesh graph topology.
- `id` BIGINT AUTO_INCREMENT PK
- `name` VARCHAR(255) UNIQUE NOT NULL
- `region` VARCHAR(100) NOT NULL (`EU`, `US`, `IN`, `APAC`, `GLOBAL`)
- `mesh_zone` VARCHAR(100)
- `environment` VARCHAR(50) NOT NULL
- `description` VARCHAR(1000)

### 4. `data_flow_edges` & `data_flow_edge_classes`
**Purpose:** Directed communications between services with tagged data classes.
- `data_flow_edges`: `id` BIGINT PK, `source_service_id` BIGINT, `destination_service_id` BIGINT.
- `data_flow_edge_classes`: `edge_id` BIGINT FK, `data_class` VARCHAR(100).

### 5. `decisions`
**Purpose:** Real-time enforcement evaluation audit log.
- `id` BIGINT AUTO_INCREMENT PK
- `source_service`, `destination_service`, `source_region`, `destination_region`, `data_class`
- `decision` VARCHAR(20) (`ALLOW`, `DENY`, `REROUTE`)
- `policy_id` VARCHAR(100), `reason` VARCHAR(1000), `created_at` DATETIME(6)

### 6. `lineage_records`
**Purpose:** Cryptographically chained, tamper-evident audit ledger.
- `id` BIGINT AUTO_INCREMENT PK
- `decision_id` BIGINT UNIQUE NOT NULL
- `previous_hash` VARCHAR(128)
- `current_hash` VARCHAR(128) NOT NULL (SHA-256)
- `signature` VARCHAR(512)

### 7. `ci_scans`
**Purpose:** History of standalone CI compliance evaluation runs.

### 8. `ai_classifications`
**Purpose:** AI-assisted data field discovery and sensitivity tagging.
