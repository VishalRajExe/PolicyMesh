# TECH_STACK.md

See [ARCHITECTURE.md](./ARCHITECTURE.md) for how these pieces interact.

## Backend

| Technology | Why |
|---|---|
| Java 21 | Modern LTS with records/pattern matching, good fit for a policy model with sealed types. |
| Spring Boot 3 | Fast REST API development, mature ecosystem. |
| Spring Web | REST controllers. |
| Spring Data JPA | Entity persistence to MySQL without hand-written SQL boilerplate. |
| Spring Security | JWT auth + RBAC. |
| Jakarta Validation | Request DTO validation. |
| Maven | Build tool and dependency management. |

## Database

| Technology | Why |
|---|---|
| MySQL 8.4 | Relational integrity and persistence for policies, services, edges, decisions, and lineage audit ledger. |

## Cache

| Technology | Why |
|---|---|
| Redis 7 | Cache compiled policies to avoid recompiling on every enforcement check (see [REDIS.md](./REDIS.md)). |

## Messaging

| Technology | Why |
|---|---|
| Kafka | Asynchronous fan-out of events (policy updated, decision created) to consumers such as dashboards, without blocking the synchronous decision path (see [KAFKA.md](./KAFKA.md)). |

## AI

| Technology | Why |
|---|---|
| Python FastAPI + LLM API abstraction | Isolates the AI classification concern from the core Java backend; can run in mock mode for demo reliability (see [AI_CLASSIFICATION.md](./AI_CLASSIFICATION.md)). |

## Frontend

| Technology | Why |
|---|---|
| React + TypeScript | Type-safe UI development. |
| React Flow | Visualizing the service/data-flow graph. |
| Recharts | Dashboard metrics charts. |

## Infrastructure

| Technology | Why |
|---|---|
| Docker / Docker Compose | Local one-command startup of all infra (see [DOCKER_SETUP.md](./DOCKER_SETUP.md)). |
| GitHub Actions | CI pipeline integration (see [CI_INTEGRATION.md](./CI_INTEGRATION.md)). |
