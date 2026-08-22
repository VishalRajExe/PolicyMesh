# ARCHITECTURE.md

This document explains how PolicyMesh's components fit together. See [TECH_STACK.md](./TECH_STACK.md) for technology choices and [GLOSSARY.md](./GLOSSARY.md) for terminology.

## High-Level Architecture

```mermaid
flowchart TD
    UI[React Frontend] --> API[Spring Boot API]
    API --> PE[Policy Engine]
    API --> GE[Graph Engine]
    API --> RT[Runtime Enforcement]
    API --> CI[CI Service]
    API --> LIN[Lineage Ledger]
    PE --> DB[(PostgreSQL)]
    GE --> DB
    RT --> DB
    LIN --> DB
    API --> REDIS[(Redis Cache)]
    API --> KAFKA[(Kafka - async events)]
    API --> AISVC[AI Classification Service]
```

- **React Frontend** — dashboard, policy editor, graph visualization (React Flow), charts (Recharts). See [FRONTEND_CONTRACT.md](./FRONTEND_CONTRACT.md).
- **Spring Boot API** — the single backend entry point; exposes all REST APIs (see [API_SPEC.md](./API_SPEC.md)).
- **Policy Engine** — evaluates a data flow against compiled policy (see [POLICY_COMPILER.md](./POLICY_COMPILER.md)).
- **Graph Engine** — models services/data flows and runs CI-time analysis (see [GRAPH_ENGINE.md](./GRAPH_ENGINE.md)).
- **Runtime Enforcement** — evaluates live requests (see [RUNTIME_ENFORCEMENT.md](./RUNTIME_ENFORCEMENT.md)).
- **CI Service** — the API surface GitHub Actions calls (see [CI_INTEGRATION.md](./CI_INTEGRATION.md)).
- **Lineage Ledger** — hash-chained decision record (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md)).
- **PostgreSQL** — source of truth for all entities (see [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)).
- **Redis** — cache for compiled policies (see [REDIS.md](./REDIS.md)).
- **Kafka** — asynchronous event bus for non-blocking notifications (see [KAFKA.md](./KAFKA.md)).
- **AI Classification Service** — Python FastAPI service suggesting data classifications (see [AI_CLASSIFICATION.md](./AI_CLASSIFICATION.md)).

## CI Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub PR
    participant GA as GitHub Actions
    participant PM as PolicyMesh CI Checker
    participant GA2 as Graph Analyzer
    participant PE as Policy Engine
    Dev->>GH: git push / open PR
    GH->>GA: trigger workflow
    GA->>PM: POST /ci/check
    PM->>GA2: analyze graph
    GA2->>PE: evaluate each edge
    PE-->>GA2: ALLOW / violation
    GA2-->>PM: PASS / FAIL + violations
    PM-->>GA: result
    GA-->>GH: check status
```

## Runtime Flow

```mermaid
sequenceDiagram
    participant App as Application Request
    participant RI as Runtime Interceptor (simulated)
    participant EA as Enforcement API
    participant PE as Policy Engine
    participant LIN as Lineage
    App->>RI: attempt data flow
    RI->>EA: POST /enforce/check
    EA->>PE: evaluate
    PE-->>EA: ALLOW / DENY / REROUTE
    EA->>LIN: record decision
    EA-->>RI: decision
    RI-->>App: allow / block
```

## AI Flow

```mermaid
flowchart LR
    Schema[Schema Field] --> AI[AI Classifier]
    AI --> Sugg[Suggested Classification + Confidence]
    Sugg --> Human[Human Approval]
    Human -->|approve| PE[Policy Engine]
    Human -->|reject| Discard[Discarded]
```

## What Is Real vs Simulated in the MVP

| Component | MVP Status |
|---|---|
| Policy Engine, Graph Engine, CI checker | Real — implemented in Spring Boot |
| Lineage hash chaining | Real — SHA-256 chain in PostgreSQL |
| Runtime Enforcement API | Real API, but the "interceptor" is a **simulator** UI/endpoint that submits example requests rather than a live service-mesh sidecar |
| AI Classification | Real API surface; underlying model call can run in **mock mode** for demo reliability (see [AI_CLASSIFICATION.md](./AI_CLASSIFICATION.md)) |
| Kafka | Real broker in Docker Compose, used for non-blocking event notification only — never required for a synchronous decision |
| Redis | Real cache; system must degrade gracefully to PostgreSQL if unavailable |

Future architecture (Istio/Envoy sidecars, multi-region, Kubernetes) is described in [DEPLOYMENT.md](./DEPLOYMENT.md) and [ROADMAP.md](./ROADMAP.md), and is explicitly not implemented in the MVP.
