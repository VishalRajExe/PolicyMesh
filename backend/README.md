# PolicyMesh Backend

PolicyMesh is a Java 21 / Spring Boot 3 modular monolith for **policy-as-code data residency compliance**.
One authoritative policy engine drives runtime enforcement, service-graph validation and CI checks;
every runtime decision is persisted and appended to a tamper-evident SHA-256 lineage chain.

```
auth  policy  compiler  servicegraph  graph  enforcement  ci  lineage  dashboard  audit  ai  events  common
```

- **auth** — registration/login, BCrypt, JWT (1 h), endpoint-level RBAC
- **policy / compiler** — policy CRUD + YAML DSL pipeline (parse -> validate -> CompiledPolicy)
- **servicegraph / graph** — services, data-flow edges, graph compliance analysis
- **enforcement / lineage** — runtime decisions, DecisionRecord + SHA-256 hash chain
- **ci** — CI checker (GraphAnalyzer -> PolicyEngine), persisted scans, standalone CLI gate
- **dashboard / audit / ai** — aggregates, decision history, AI classification with human approval
- **events** — optional Kafka notifications; **common** — RFC 7807 error handling

## Requirements

Java 21, Maven 3.9+, Docker (for PostgreSQL/Redis/Kafka). Redis and Kafka are optional:
Redis accelerates compiled-policy lookups (PostgreSQL always answers on miss) and Kafka
publishes async events only — the application starts and stays correct without either.

## Quick start

```sh
cd backend
docker compose up -d          # PostgreSQL 5432, Redis 6379, Kafka 9092
mvn spring-boot:run           # backend on 8080
```

Seed the documented demo scenario (EU-PII-001 + IN-PII-001, orders/payments/analytics):

```sh
./scripts/seed-demo-data      # registers a demo ADMIN, seeds, verifies ALLOW/DENY/lineage
```

or start the app with demo data already loaded: `POLICYMESH_DEMO_SEED=true mvn spring-boot:run`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `jdbc:postgresql://localhost:5432/policymesh` / `policymesh` / `policymesh` | PostgreSQL connection |
| `JWT_SECRET` | dev-only fallback | 32+ chars; **must** be set in any real environment |
| `JWT_EXPIRATION_MS` | `3600000` | token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | comma-separated browser origins |
| `POLICYMESH_REDIS_ENABLED` | `true` | set `false` to skip Redis entirely |
| `POLICYMESH_KAFKA_ENABLED` | `false` | set `true` to publish events |
| `AI_SERVICE_MODE` | `local` | `local` heuristic or `remote` FastAPI service (falls back to local when unreachable) |
| `AI_SERVICE_URL` | `http://localhost:8000` | external AI service base URL |
| `POLICYMESH_DEMO_SEED` | `false` | seed demo data at startup |

Kafka topics (only when enabled): `policymesh.policy.updated`, `policymesh.decision.created`,
`policymesh.lineage.created`, `policymesh.ci.completed`. Publication failures are logged and swallowed.

## Authentication and roles

`POST /api/v1/auth/register` `{email, password (min 8), role?}` -> 201 `{id, email, role}`
(role defaults to ENGINEER; the four roles are ADMIN, COMPLIANCE_OFFICER, ENGINEER, VIEWER).
`POST /api/v1/auth/login` `{email, password}` -> 200 `{token, expiresIn, tokenType, role}`.

Only `/api/v1/auth/**` is public. RBAC (docs/AUTHENTICATION.md): policy writes need
ADMIN/COMPLIANCE_OFFICER (delete: ADMIN), service/edge writes need ADMIN/ENGINEER
(delete: ADMIN), enforcement needs ADMIN/ENGINEER, CI checks ADMIN/COMPLIANCE_OFFICER/ENGINEER,
AI approve/reject ADMIN/COMPLIANCE_OFFICER, everything else any authenticated role.
Failed logins are rate-limited (10 per 15 min per email -> 429).

## APIs (all under `/api/v1`, bearer token required unless noted)

| Endpoint | Purpose |
|---|---|
| `GET/POST /policies`, `GET/PUT/DELETE /policies/{id}` | policy CRUD; POST creates `DRAFT`, PUT may activate and bumps `version`, DELETE soft-deletes to `INACTIVE` |
| `GET/POST /services`, `GET/PUT/DELETE /services/{id}` | service registry CRUD |
| `GET/POST /edges`, `GET/PUT/DELETE /edges/{id}` | data-flow edge CRUD |
| `GET /graph`, `POST /graph/validate` | graph projection; compliance result `{result: PASS\|FAIL, violationCount, violations}` |
| `POST /enforce/check` | runtime decision + DecisionRecord + lineage record |
| `POST /ci/check`, `GET /ci/scans/{id}` | CI scan (always HTTP 200; PASS/FAIL in body) with persisted violations |
| `GET /lineage`, `GET /lineage/{id}`, `GET /lineage/verify` | lineage records and chain verification |
| `GET /dashboard/summary` | complianceScore, totals, transfers, activeViolations, decisionsToday, lineageValid |
| `GET /audit/decisions` | recent runtime decisions |
| `POST /ai/classify`, `POST /ai/classify/{id}/approve\|reject` | AI classification with mandatory human approval |
| `POST /compiler/compile` | validate/compile YAML DSL without persisting |
| `POST /dev/seed` (ADMIN) | idempotent demo data |

Errors are RFC 7807 `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`);
validation failures are 422 with an `errors` array, malformed bodies 400, conflicts 409.
Compliance outcomes (DENY, FAIL) are business results and return HTTP 200 — never 500.

### Curl walkthrough

```sh
BASE=http://localhost:8080
# register + login (public)
curl -X POST $BASE/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"a-strong-password","role":"ADMIN"}'
TOKEN=$(curl -sf -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"a-strong-password"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
AUTH="Authorization: Bearer $TOKEN"

# demo data, graph, CI
curl -X POST $BASE/api/v1/dev/seed -H "$AUTH"
curl -X POST $BASE/api/v1/graph/validate -H "$AUTH"           # FAIL: orders-api -> analytics-api (PII EU->US)
curl -X POST $BASE/api/v1/ci/check -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"commitHash":"abc123","branch":"main"}'                # result: FAIL, violationCount: 1

# runtime enforcement (EU -> EU allow, EU -> US deny)
curl -X POST $BASE/api/v1/enforce/check -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"sourceService":"orders-api","destinationService":"payments-api","dataClassTags":["PII"]}'
curl -X POST $BASE/api/v1/enforce/check -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"sourceService":"orders-api","destinationService":"analytics-api","dataClassTags":["PII"]}'

# lineage chain
curl $BASE/api/v1/lineage -H "$AUTH"
curl $BASE/api/v1/lineage/verify -H "$AUTH"                   # {"valid":true,"recordsChecked":2,...}

# AI classification with approval
curl -X POST $BASE/api/v1/ai/classify -H "$AUTH" -H 'Content-Type: application/json' -d '{"fieldName":"email"}'
curl -X POST $BASE/api/v1/ai/classify/1/approve -H "$AUTH"
```

## Policy DSL

```yaml
policy:
  id: EU-PII-001
  name: EU PII Protection
  jurisdiction: EU            # INDIAN alias INDIA is folded onto IN; GLOBAL applies everywhere
  dataClass: PII              # PII | PCI | PHI | PUBLIC | NON_SENSITIVE | UNKNOWN
  allowedRegions: [EU]        # required, >=1, must not overlap deniedRegions
  deniedRegions: [US, CN]     # optional
```

Pipeline: raw YAML -> parser (SnakeYAML, safe constructor) -> validation (422 on semantic
violations, 400 on unparsable YAML) -> `CompiledPolicy` -> PolicyEngine. Compilation is
isolated from runtime evaluation; the engine only ever sees compiled, validated policies.

**Evaluation semantics** (single authoritative path in `PolicyRuleEvaluator`):
applicable = ACTIVE + dataClass match + jurisdiction match (GLOBAL matches all);
no applicable policy -> **deny by default** (PUBLIC data is the exception and is allowed);
among applicable policies **deny wins**: an explicit `deniedRegions` hit denies outright and a
destination missing from any policy's `allowedRegions` is also denied. `REROUTE` is a defined
outcome reserved for future use. Example: with `EU-PII-001`, PII EU->EU = ALLOW, PII EU->US/CN = DENY,
and PUBLIC EU->US = ALLOW.

## Lineage ledger

Each enforcement decision appends one `LineageRecord`: `previousHash` = hash of the previous
record (`null` for the first), `currentHash` = SHA-256 over the canonical serialization
`decisionId|source|destination|sourceRegion|destinationRegion|dataClass|decision|reason|policy|timestamp|previousHash`.
`GET /lineage/verify` re-derives every hash and reports the first broken link — detecting
modified content, forged hashes, and deleted/reordered records. Digital signatures are a
reserved extension point (the column stays null in the MVP).

## CI checker

```sh
mvn clean test && mvn clean package
# database-free gate over the repo's own policies and fixtures (used by GitHub Actions):
java -jar target/policy-mesh-backend-1.0.0-SNAPSHOT.jar check \
  --policies ../policies/EU --policies ../policies/GLOBAL \
  --policies ../policies/INDIA --policies ../policies/US \
  --services ../examples/services/services.json \
  --dataflows ../examples/dataflows/valid-flow.json \
  --report compliance-report.json
echo $?   # 0 = PASS, 1 = violations, 2 = input/config error
```

The CLI shares the compiler and rule evaluator with the running service, so the CI gate and
runtime enforcement can never diverge. Without file arguments the same `check` command runs
against the configured database (GraphAnalyzer -> PolicyEngine) and persists a CIScan.
Do not point `--policies` at the whole `policies/` tree — it contains schemas and test-case
files that are not enforceable policies; pass the jurisdiction directories as shown.

## Docker

`backend/Dockerfile` is a multi-stage build (Maven+JDK 21 -> JRE 21). `docker compose up -d`
from `backend/` starts PostgreSQL, Redis and Kafka with health checks and the documented ports
(5432/6379/9092); the backend itself runs on the host via `mvn spring-boot:run` (port 8080).

## GitHub Actions

- `.github/workflows/policymesh-ci.yml` — the main compliance pipeline: policy-file lint,
  standalone ci-checker tests, **backend build + tests**, and the compliance gate run by both
  the standalone checker and the packaged backend JAR (fails on any violation).
- `.github/workflows/backend-ci.yml` — backend tests, packaging and the JAR-based compliance
  gate, including a negative control proving the gate actually blocks violating fixtures.

## Demo scenario (expected results)

Seeded graph: `web-app(EU) -> orders-api(EU) -> payments-api(EU)` and
`orders-api(EU) -> analytics-api(US)`, all carrying PII, under `EU-PII-001`.

1. `POST /graph/validate` -> FAIL, 1 violation (orders-api -> analytics-api, EU-PII-001)
2. `POST /ci/check` -> FAIL (same violation, scan persisted with commit/branch/timestamps)
3. `PUT /services/{analytics-api}` region `EU` -> `POST /ci/check` -> PASS, 0 violations
4. `POST /enforce/check` orders-api -> analytics-api PII -> DENY + DecisionRecord + lineage hash
5. `POST /enforce/check` orders-api -> payments-api PII -> ALLOW
6. `GET /lineage/verify` -> `{"valid":true,...}`
