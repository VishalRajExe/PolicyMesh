# PolicyMesh — Backend

Policy-as-code platform for compliance-as-code and data-residency enforcement.

One policy, written once, is compiled and reused for three things:

```
One Policy
    ↓
Policy Compiler
    ↓
 ┌───────────────────────┐
 │                       │
 ▼                       ▼
CI / Build-Time       Runtime
Graph Verification    Enforcement
 │                       │
 ▼                       ▼
PASS / FAIL            ALLOW / DENY / REROUTE
 │                       │
 └──────────────┬────────┘
                ▼
        Hash-Chained Lineage
                ▼
          Audit Evidence
```

This repository is the **backend only** — a modular monolith built with
Spring Boot. There is no frontend here; the REST API below is what a
frontend (or CI pipeline, or another service) would consume.

---

## What is PolicyMesh?

Companies declare policies like *"EU PII must stay in the EU"* once, in a
small YAML DSL. PolicyMesh compiles that policy and uses it for:

1. **CI-time graph validation** — before a deployment ships, PolicyMesh
   walks the declared service graph and data-flow edges and fails the
   build if any edge would violate a policy.
2. **Runtime enforcement** — services (or a sidecar/gateway) can call
   `POST /api/v1/enforce/check` to get an ALLOW/DENY/REROUTE decision for
   an in-flight data transfer.
3. **Lineage** — every decision that needs audit evidence is written into
   a SHA-256 hash chain. Any tampering with historical evidence is
   detectable via `GET /api/v1/lineage/verify`.

---

## Architecture

Modular monolith (single deployable, cleanly separated packages so it can
be split into microservices later):

```
auth           - users, roles, JWT
policy         - Policy CRUD, backed by the compiler
compiler       - YAML policy DSL parser/validator/compiler (internal model only)
servicegraph   - ServiceNode + DataFlowEdge CRUD
graph          - graph analyzer/validator, evaluates every edge via the policy engine
enforcement    - PolicyEngine (central decision logic) + runtime /enforce/check
ci             - CI scan persistence + CLI-friendly checker (non-zero exit on failure)
lineage        - SHA-256 hash-chain writer + verifier
dashboard      - aggregate summary API
audit          - recent-activity read view
ai             - classification suggestion workflow (human approval required)
config         - security, CORS, Redis, Kafka, demo-data seeding
common         - shared exceptions, RFC 7807 error handling, hashing util, event publisher
```

**All policy decisions go through one place:** `enforcement.engine.PolicyEngine`.
Nothing else evaluates policy logic directly — not controllers, not the
graph analyzer (it also calls into `PolicyEngine`), not the CI checker.

**PostgreSQL is the source of truth.** Redis caches compiled policies for
hot lookups but the app works correctly (just slower) if Redis is down.
Kafka publishes async domain events but the app works correctly (just
without events) if Kafka is unreachable — both are enforced defensively
in `PolicyCacheService` and `EventPublisherService`.

---

## Requirements

- Java 21
- Maven 3.8+
- Docker + Docker Compose (for Postgres/Redis/Kafka locally)

---

## Environment variables

Copy `.env.example` to `.env` and adjust as needed (never commit a real `.env`):

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | `jdbc:postgresql://localhost:5432/policymesh` |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_ENABLED` | Redis cache | `localhost:6379`, enabled |
| `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_ENABLED` | Kafka events | `localhost:9092`, enabled |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | Auth token signing | must override in prod |
| `AI_SERVICE_URL` | External AI classification service | blank → local mock |
| `POLICY_DEFAULT_DECISION` | ALLOW/DENY when no policy applies | `ALLOW` |
| `SERVER_PORT` | HTTP port | `8080` |

---

## Running everything locally

### 1. Start infrastructure

```bash
docker compose up -d postgres redis kafka zookeeper
```

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API is now at `http://localhost:8080`.

Or run the whole stack (backend included) in Docker:

```bash
docker compose up -d
```

### 3. Seed demo data

With `policymesh.seed.enabled=true` (already set in the `dev` profile) the
app seeds demo data automatically on first boot. To seed explicitly:

```bash
./scripts/seed-demo-data
```

This loads:

- Policies: `EU-PII-001` (EU PII Protection), `IN-PII-001` (India PII Protection)
- Services: `web-app` (EU), `orders-api` (EU), `payments-api` (EU), `analytics-api` (US)
- Edges: `web-app → orders-api`, `orders-api → payments-api`, `orders-api → analytics-api`

### 4. Run tests

```bash
mvn test
```

Tests use an in-memory H2 database (Postgres-compatible mode) and run
without Redis/Kafka, so `mvn test` works with no external services running.

### 5. Run the CI checker independently

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=check
```

or, against a built jar:

```bash
java -jar target/policymesh.jar check
```

Exits `0` on a clean compliance check, non-zero when violations are found —
this is what `.github/workflows/policymesh-ci.yml` runs on every PR.

---

## Example policy (YAML DSL)

```yaml
policy:
  id: EU-PII-001
  name: EU PII Protection
  jurisdiction: EU
  dataClass: PII
  allowedRegions:
    - EU
  deniedRegions:
    - US
    - CN
```

Submit it directly:

```bash
curl -X POST http://localhost:8080/api/v1/policies/yaml \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"yaml": "policy:\n  id: EU-PII-001\n  name: EU PII Protection\n  jurisdiction: EU\n  dataClass: PII\n  allowedRegions: [EU]\n  deniedRegions: [US, CN]\n"}'
```

---

## Example runtime enforcement request

```bash
curl -X POST http://localhost:8080/api/v1/enforce/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceService":"orders-api",
    "destinationService":"analytics-api",
    "sourceRegion":"EU",
    "destinationRegion":"US",
    "dataClassTags":["PII"]
  }'
```

Response:

```json
{
  "decision": "DENY",
  "policyId": "EU-PII-001",
  "reason": "EU PII cannot be transferred to US",
  "lineageHash": "8ac1f7..."
}
```

And the allowed case:

```bash
curl -X POST http://localhost:8080/api/v1/enforce/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceService":"orders-api",
    "destinationService":"payments-api",
    "sourceRegion":"EU",
    "destinationRegion":"EU",
    "dataClassTags":["PII"]
  }'
```

```json
{
  "decision": "ALLOW",
  "policyId": "EU-PII-001",
  "reason": "Destination region permitted",
  "lineageHash": "..."
}
```

---

## Example CI violation

```text
PolicyMesh Compliance Check

Loading policies...
Loading service graph...
Analyzing 4 services...
Analyzing 3 data-flow edges...

[FAIL] orders-api EU -> analytics-api US

Policy: EU-PII-001
Data Class: PII

Reason:
EU PII cannot be transferred to US.

❌ POLICY VIOLATION

Compliance Check: FAILED
Violations: 1
```

---

## Authentication & roles

Get a token first:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@policymesh.io","password":"SecurePass123","role":"COMPLIANCE_OFFICER"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@policymesh.io","password":"SecurePass123"}'
```

Use the returned `token` as `Authorization: Bearer <token>` on subsequent calls.

| Role | Can do |
|---|---|
| `ADMIN` | everything |
| `COMPLIANCE_OFFICER` | create/update/delete policies, view lineage, run checks |
| `ENGINEER` | manage services/edges, view policies, run CI checks, runtime testing |
| `VIEWER` | dashboard, graph, lineage (read-only) |

---

## REST API summary

All endpoints are under `/api/v1`.

```
POST   /auth/register
POST   /auth/login

GET    /policies
GET    /policies/{id}
POST   /policies
POST   /policies/yaml
PUT    /policies/{id}
DELETE /policies/{id}

GET    /services
GET    /services/{id}
POST   /services
PUT    /services/{id}
DELETE /services/{id}

GET    /edges
POST   /edges
DELETE /edges/{id}

GET    /graph
POST   /graph/validate

POST   /enforce/check

POST   /ci/check
GET    /ci/scans/{id}

GET    /lineage
GET    /lineage/{id}
GET    /lineage/verify

GET    /dashboard/summary

GET    /audit/recent

POST   /ai/classify
POST   /ai/classify/{id}/approve
POST   /ai/classify/{id}/reject
```

Errors are returned as `application/problem+json` (RFC 7807):

```json
{
  "type": "https://policymesh/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/policies",
  "errors": { "dataClass": "dataClass is required" }
}
```

---

## Lineage verification

```bash
curl http://localhost:8080/api/v1/lineage/verify -H "Authorization: Bearer $TOKEN"
```

```json
{ "valid": true, "recordsChecked": 1042, "brokenAtRecord": null }
```

If a record has been tampered with, `valid` becomes `false` and
`brokenAtRecord` points at the sequence number where the chain broke.

---

## Notes on scope (per hackathon spec)

- Implemented as a **modular monolith**, structured so packages could be
  extracted into microservices later without a rewrite.
- No cryptographic *signing* is implemented yet (the `signature` field on
  lineage records is reserved and left `null`); only SHA-256 hash-chaining,
  as specified. No fake signatures are produced.
- No Kubernetes/Istio integration in this pass.
- The AI classification endpoint calls an external service if
  `AI_SERVICE_URL` is set, otherwise falls back to a transparent local
  heuristic classifier so the approval workflow stays testable without a
  real model. Suggestions never become enforcement-relevant until a human
  approves them via `/ai/classify/{id}/approve`.
