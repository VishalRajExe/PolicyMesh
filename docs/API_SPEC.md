# API_SPEC.md

Base path: `/api/v1`. All endpoints except `/auth/register` and `/auth/login` require a valid JWT (`Authorization: Bearer <token>`). See [AUTHENTICATION.md](./AUTHENTICATION.md) for the role matrix and [ERROR_HANDLING.md](./ERROR_HANDLING.md) for the error envelope.

## Authentication

### POST /auth/register
- **Purpose:** Create a new user account.
- **Auth:** None.
- **Request body:**
```json
{ "email": "compliance@acme.com", "password": "S3curePassw0rd!", "role": "COMPLIANCE_OFFICER" }
```
- **Validation:** email format, password min length 8, role must be one of the four defined roles.
- **Success (201):**
```json
{ "id": "u-001", "email": "compliance@acme.com", "role": "COMPLIANCE_OFFICER" }
```
- **Errors:** 409 (email already exists), 422 (validation failure).

### POST /auth/login
- **Purpose:** Authenticate and receive a JWT.
- **Auth:** None.
- **Request body:** `{ "email": "compliance@acme.com", "password": "S3curePassw0rd!" }`
- **Success (200):** `{ "token": "eyJhbGciOi...", "expiresIn": 3600 }`
- **Errors:** 401 (invalid credentials).

## Policies

### GET /policies
- **Purpose:** List all policies.
- **Auth:** Any authenticated role.
- **Success (200):**
```json
[ { "id": "p-1", "policyCode": "EU-PII-001", "name": "EU PII Protection", "status": "ACTIVE", "version": 1 } ]
```

### GET /policies/{id}
- **Purpose:** Fetch one policy.
- **Success (200):** full policy object (see [POLICY_DSL.md](./POLICY_DSL.md)).
- **Errors:** 404.

### POST /policies
- **Purpose:** Create a policy.
- **Auth:** ADMIN, COMPLIANCE_OFFICER.
- **Request body:**
```json
{
  "policyCode": "EU-PII-001",
  "name": "EU PII Protection",
  "jurisdiction": "EU",
  "dataClass": "PII",
  "allowedRegions": ["EU"],
  "deniedRegions": ["US", "CN"]
}
```
- **Validation:** see [POLICY_DSL.md](./POLICY_DSL.md) §Validation Rules.
- **Success (201):** created policy, `status: DRAFT`, `version: 1`.
- **Errors:** 403 (wrong role), 409 (duplicate policyCode), 422 (invalid regions/DSL).

### PUT /policies/{id}
- **Purpose:** Update a policy (creates a new version).
- **Auth:** ADMIN, COMPLIANCE_OFFICER.
- **Success (200):** updated policy with incremented `version`.
- **Errors:** 404, 409 (policy inactive), 422.

### DELETE /policies/{id}
- **Purpose:** Deactivate a policy (soft delete; sets `status: INACTIVE`).
- **Auth:** ADMIN.
- **Success (204).**
- **Errors:** 404.

## Services

### GET /services
- **Purpose:** List registered services.
- **Success (200):** `[ { "id": "s-1", "name": "orders-api", "region": "EU", "environment": "production" } ]`

### POST /services
- **Auth:** ADMIN, ENGINEER.
- **Request body:** `{ "name": "orders-api", "region": "EU", "meshZone": "core", "environment": "production" }`
- **Success (201):** created service.
- **Errors:** 409 (duplicate name), 422.

### PUT /services/{id}
- **Auth:** ADMIN, ENGINEER.
- **Success (200):** updated service.

### DELETE /services/{id}
- **Auth:** ADMIN.
- **Success (204).**

## Graph

### GET /graph
- **Purpose:** Return the full service/data-flow graph.
- **Success (200):**
```json
{
  "nodes": [ { "id": "s-1", "name": "orders-api", "region": "EU" } ],
  "edges": [ { "id": "e-1", "source": "s-1", "destination": "s-2", "dataClasses": ["PII"] } ]
}
```

### POST /graph/validate
- **Purpose:** Run the Graph Analyzer against current policies without a CI context.
- **Auth:** Any authenticated role.
- **Success (200):**
```json
{ "result": "FAIL", "violations": [ { "edge": "e-2", "reason": "PII from EU to US denied by EU-PII-001" } ] }
```

## Enforcement

### POST /enforce/check
- **Purpose:** Evaluate one proposed data flow at request time.
- **Auth:** ADMIN, ENGINEER (or service-to-service token in future).
- **Request body:**
```json
{ "source": "orders-api", "destination": "analytics-api", "dataClass": "PII" }
```
- **Success (200):**
```json
{ "decision": "DENY", "reason": "PII from EU to US denied by EU-PII-001", "policy": "EU-PII-001", "lineageId": "l-9" }
```

## CI

### POST /ci/check
- **Purpose:** Called from GitHub Actions (see [CI_INTEGRATION.md](./CI_INTEGRATION.md)).
- **Auth:** ADMIN, ENGINEER, or CI service token.
- **Success (200):**
```json
{ "scanId": "ci-42", "result": "PASS", "violations": [] }
```
- **Failure (200, result=FAIL):**
```json
{ "scanId": "ci-43", "result": "FAIL", "violations": [ { "edge": "orders-api -> analytics-api", "reason": "PII EU to US denied" } ] }
```

### GET /ci/scans/{id}
- **Purpose:** Fetch a past CI scan result.
- **Success (200):** scan object with `result` and `violations`.
- **Errors:** 404.

## Lineage

### GET /lineage
- **Purpose:** Query lineage records (filterable by service, policy, date range, decision — query params).
- **Success (200):** array of lineage records.

### GET /lineage/{id}
- **Success (200):** single lineage record with linked decision.
- **Errors:** 404.

### GET /lineage/verify
- **Purpose:** Verify the integrity of the hash chain (see [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md)).
- **Success (200):** `{ "valid": true, "recordsChecked": 128 }` or `{ "valid": false, "brokenAt": "l-57" }`

## Dashboard

### GET /dashboard/summary
- **Purpose:** Aggregate metrics for the dashboard.
- **Success (200):**
```json
{ "policies": 4, "services": 3, "activeViolations": 1, "decisionsToday": 12, "lineageValid": true }
```

## AI

### POST /ai/classify
- **Purpose:** Request an AI-suggested classification for a schema field.
- **Auth:** ADMIN, COMPLIANCE_OFFICER, ENGINEER.
- **Request body:** `{ "fieldName": "email" }`
- **Success (200):** `{ "id": "ai-7", "fieldName": "email", "suggestedClass": "PII", "confidence": 0.94, "status": "PENDING" }`

### POST /ai/classify/{id}/approve
- **Auth:** ADMIN, COMPLIANCE_OFFICER.
- **Success (200):** `{ "id": "ai-7", "status": "APPROVED" }`

### POST /ai/classify/{id}/reject
- **Auth:** ADMIN, COMPLIANCE_OFFICER.
- **Success (200):** `{ "id": "ai-7", "status": "REJECTED" }`

## Standard Error Responses

All endpoints use the `application/problem+json` shape defined in [ERROR_HANDLING.md](./ERROR_HANDLING.md) for 4xx/5xx responses.
