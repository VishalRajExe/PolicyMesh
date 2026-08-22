# TECH_STACK.md

See [ARCHITECTURE.md](./ARCHITECTURE.md) for how these pieces interact.

## Backend

| Technology | Why |
|---|---|
| Java 21 | Modern LTS with records/pattern matching, good fit for a policy model with sealed types. |
| Spring Boot 3 | Fast REST API development, mature ecosystem. |
| Spring Web | REST controllers. |
| Spring Data JPA | Entity persistence to PostgreSQL without hand-written SQL boilerplate. |
| Spring Security | JWT auth + RBAC. |
| Jakarta Validation | Request DTO validation. |
| Maven | Build tool and dependency management. |

## Database

| Technology | Why |
|---|---|
| PostgreSQL | Relational integrity for policies/services/edges/decisions/lineage; strong JSON support for flexible fields (e.g., allowedRegions arrays). |

## Cache

| Technology | Why |
|---|---|
| Redis | Cache compiled policies to avoid recompiling on every enforcement check (see [REDIS.md](./REDIS.md)). |

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

## Required for MVP

Java 21, Spring Boot 3 (Web, Data JPA, Security), Maven, PostgreSQL, React + TypeScript, React Flow, Docker Compose.

## Optional for MVP

Redis (system must run without it — degraded caching only), Kafka (must run without it — synchronous decisions never depend on it), AI service (can run in mock mode without a live LLM key).

## Future Production Architecture

Kubernetes, Istio/Envoy service mesh sidecars for real interception, multi-region PostgreSQL, object storage with WORM retention, SSO/SAML — see [DEPLOYMENT.md](./DEPLOYMENT.md) and [ROADMAP.md](./ROADMAP.md).
