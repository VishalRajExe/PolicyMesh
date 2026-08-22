<div align="center">
  <img src="docs/images/logo.png" alt="PolicyMesh Logo" width="120" height="120" />
  <h1>PolicyMesh</h1>
  <p><strong>Policy-as-code platform for data-residency and cross-border data-flow compliance.</strong></p>
</div>

PolicyMesh lets an organization declare data-residency rules once — in a single declarative YAML policy — and enforces those rules at two points in the software lifecycle:

- **CI / build-time** — fails a pull request when a disallowed data flow is introduced into the service graph.
- **Runtime** — evaluates live requests and returns `ALLOW / DENY / REROUTE`.

Every decision from both paths is recorded as **SHA-256 hash-chained lineage**, producing tamper-evident audit evidence.

> DoraHacks 2.0 Hackathon Project

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  One Declarative Policy (YAML)                                  │
│       │                                                         │
│       ▼                                                         │
│  Policy Compiler ──┬──► Graph Engine ──► CI Checker            │
│                    │         │               │                  │
│                    └──► Runtime Enforcement  │                  │
│                                │            │                  │
│                         Lineage Ledger ◄────┘                  │
│                         (SHA-256 chain)                         │
│                                │                               │
│                         Audit Evidence                          │
└─────────────────────────────────────────────────────────────────┘
```

| Component | Technology |
|---|---|
| Backend API | Java 21 · Spring Boot 3 · Spring Security (JWT/RBAC) · Spring Data JPA |
| Database | PostgreSQL (source of truth) |
| Cache | Redis (compiled-policy cache; optional) |
| Messaging | Kafka (async event fan-out; optional) |
| AI Classifier | Python · FastAPI (local heuristic or remote LLM) |
| Infrastructure | Docker Compose · GitHub Actions |

---

## Quick Start

### Prerequisites
- Docker & Docker Compose v2
- Java 21 + Maven 3.9 (for local backend development only)

### 1. Clone and configure

```bash
git clone https://github.com/VishalRajExe/PolicyMesh.git
cd PolicyMesh
cp infrastructure/env/.env.example infrastructure/compose/.env
```

Edit `infrastructure/compose/.env` — **at minimum set**:
```env
POSTGRES_PASSWORD=a-strong-password
JWT_SECRET=a-secret-that-is-at-least-32-characters-long
```

### 2. Start all services

```bash
cd infrastructure/compose
docker compose --env-file .env up -d
```

This starts: **PostgreSQL · Redis · Kafka · AI Service · Backend API**

### 3. Verify

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

### 4. Seed demo data and explore

```bash
# Register an admin user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"StrongPass123!","role":"ADMIN"}'

# Log in and grab the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"StrongPass123!"}' | jq -r .token)

# Seed the demo scenario (EU PII policy + services + edges)
curl -s -X POST http://localhost:8080/api/v1/dev/seed \
  -H "Authorization: Bearer $TOKEN"

# Run a CI compliance check
curl -s -X POST http://localhost:8080/api/v1/ci/check \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"branch":"main","commitHash":"HEAD"}'

# Runtime enforcement: EU → US PII transfer (should DENY)
curl -s -X POST http://localhost:8080/api/v1/enforce/check \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"source":"orders-api","destination":"analytics-api","dataClass":"PII"}'

# Verify lineage chain integrity
curl -s http://localhost:8080/api/v1/lineage/verify \
  -H "Authorization: Bearer $TOKEN"
```

---

## API Surface

Base path: `POST /api/v1` — all endpoints except auth require `Authorization: Bearer <token>`.

| Endpoint | Method | Purpose |
|---|---|---|
| `/auth/register` | POST | Register a new user |
| `/auth/login` | POST | Authenticate → JWT |
| `/policies` | GET · POST · PUT · DELETE | Policy CRUD |
| `/services` | GET · POST · PUT · DELETE | Service node management |
| `/edges` | GET · POST · PUT · DELETE | Data-flow edge management |
| `/graph` | GET | Full service/flow graph |
| `/graph/validate` | POST | Ad-hoc graph compliance check |
| `/enforce/check` | POST | Runtime enforcement decision |
| `/ci/check` | POST | CI compliance scan (GitHub Actions gate) |
| `/ci/scans/{id}` | GET | Retrieve past CI scan |
| `/lineage` | GET | Query lineage records |
| `/lineage/{id}` | GET | Single lineage record |
| `/lineage/verify` | GET | Verify hash-chain integrity |
| `/dashboard/summary` | GET | Aggregate metrics |
| `/ai/classify` | POST | AI-suggested data classification |
| `/ai/classify/{id}/approve` | POST | Human approval of suggestion |
| `/ai/classify/{id}/reject` | POST | Human rejection of suggestion |
| `/audit/decisions` | GET | Recent 100 enforcement decisions |

See [`docs/API_SPEC.md`](docs/API_SPEC.md) for full request/response schemas.

---

## Standalone CI Checker

The backend JAR also runs as a **database-free** compliance checker — ideal for CI pipelines:

```bash
java -jar backend/target/policy-mesh-backend-*.jar check \
  --policies policies/EU --policies policies/GLOBAL \
  --services examples/services/services.json \
  --dataflows examples/dataflows/valid-flow.json

# exit 0 = PASS, exit 1 = violations found, exit 2 = input error
```

---

## Running Backend Tests

```bash
cd backend
mvn --batch-mode clean test
```

Tests run against an in-memory H2 database — no Docker required.

---

## Documentation

| Document | Description |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Component interaction diagrams |
| [`docs/API_SPEC.md`](docs/API_SPEC.md) | Full REST API specification |
| [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) | Entity/table definitions |
| [`docs/POLICY_DSL.md`](docs/POLICY_DSL.md) | Policy YAML format |
| [`docs/LINEAGE_LEDGER.md`](docs/LINEAGE_LEDGER.md) | Hash-chain design |
| [`docs/LOCAL_DEVELOPMENT.md`](docs/LOCAL_DEVELOPMENT.md) | Local dev walkthrough |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Production/Kubernetes notes |

---

## License

[MIT](LICENSE)
