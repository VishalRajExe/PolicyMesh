# REQUIREMENTS.md

This document defines functional and non-functional requirements for PolicyMesh. See [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) for context and [ROADMAP.md](./ROADMAP.md) for the MVP/Future split rationale.

## Functional Requirements

### Authentication & Users/Roles

| ID | Requirement |
|---|---|
| FR-AUTH-001 | The system shall allow a new user to register with email and password. |
| FR-AUTH-002 | The system shall allow a registered user to log in and receive a JWT. |
| FR-AUTH-003 | The system shall enforce role-based access control for every protected endpoint (see [AUTHENTICATION.md](./AUTHENTICATION.md)). |
| FR-AUTH-004 | The system shall support the roles ADMIN, COMPLIANCE_OFFICER, ENGINEER, VIEWER. |

### Policy Management

| ID | Requirement |
|---|---|
| FR-POL-001 | The system shall allow authorized users (ADMIN, COMPLIANCE_OFFICER) to create a policy. |
| FR-POL-002 | The system shall validate a policy against the Policy DSL rules before saving it. |
| FR-POL-003 | The system shall allow policies to be updated, versioned, and deactivated. |
| FR-POL-004 | The system shall compile a valid policy into an internal model usable by both the Graph Engine and Enforcement Engine. |

### Services & Data Flows

| ID | Requirement |
|---|---|
| FR-SVC-001 | The system shall allow authorized users to register a service (name, region, meshZone, environment). |
| FR-SVC-002 | The system shall allow authorized users to register a data flow edge between two services, including the data classes carried. |

### Graph Analysis

| ID | Requirement |
|---|---|
| FR-GRAPH-001 | The system shall construct a directed graph of services (nodes) and data flows (edges). |
| FR-GRAPH-002 | The system shall evaluate every edge against applicable policies and produce ALLOW or a violation. |
| FR-GRAPH-003 | The system shall report all violations found in a graph analysis, not just the first one. |

### CI Checks

| ID | Requirement |
|---|---|
| FR-CI-001 | The system shall expose an API that a CI pipeline calls to validate the current graph against policy. |
| FR-CI-002 | The system shall return a machine-readable PASS/FAIL result with a non-zero exit code convention for FAIL. |
| FR-CI-003 | The system shall persist every CI scan result. |

### Runtime Checks

| ID | Requirement |
|---|---|
| FR-RT-001 | The system shall expose an enforcement API that evaluates a single proposed data flow at request time. |
| FR-RT-002 | The system shall return one of ALLOW, DENY, or REROUTE for every enforcement check. |
| FR-RT-003 | Every enforcement decision shall be recorded as a lineage record. |

### Lineage

| ID | Requirement |
|---|---|
| FR-LIN-001 | The system shall record every CI and runtime decision as an immutable, hash-chained lineage record. |
| FR-LIN-002 | The system shall provide an API to verify the integrity of the lineage chain. |
| FR-LIN-003 | The system shall allow authorized users to query lineage records by service, policy, date range, or decision. |

### Dashboard

| ID | Requirement |
|---|---|
| FR-DASH-001 | The system shall provide a summary API showing counts of policies, services, violations, and recent decisions. |

### AI Classification

| ID | Requirement |
|---|---|
| FR-AI-001 | The system shall allow a schema field to be submitted for AI-suggested data classification. |
| FR-AI-002 | The system shall require human approval before an AI-suggested classification affects enforcement. |
| FR-AI-003 | The system shall allow a human to reject an AI classification suggestion. |

### Audit

| ID | Requirement |
|---|---|
| FR-AUD-001 | The system shall allow authorized users to query audit evidence (decisions + lineage) for any service or policy. |

## Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-PERF-001 | Performance | Enforcement checks should complete within interactive latency suitable for a demo (see [RUNTIME_ENFORCEMENT.md](./RUNTIME_ENFORCEMENT.md) for specific, non-guaranteed targets). |
| NFR-SEC-001 | Security | All protected endpoints require a valid JWT; passwords are stored hashed (see [SECURITY.md](./SECURITY.md)). |
| NFR-REL-001 | Reliability | The system shall continue enforcing policy using PostgreSQL as source of truth if Redis is unavailable. |
| NFR-MAIN-001 | Maintainability | Policy evaluation logic shall be centralized in the Policy Engine, not duplicated in controllers. |
| NFR-OBS-001 | Observability | The system shall log every policy decision with enough context to reconstruct it. |
| NFR-AUD-001 | Auditability | Every decision shall be traceable to the exact policy version that produced it. |
| NFR-SCALE-001 | Scalability | The MVP is designed for a single-instance demo; horizontal scaling is a future concern (see [DEPLOYMENT.md](./DEPLOYMENT.md)). |

## MVP vs Future Requirements

**MVP:** FR-AUTH-*, FR-POL-*, FR-SVC-*, FR-GRAPH-*, FR-CI-*, FR-RT-* (simulated), FR-LIN-*, FR-DASH-001, FR-AI-* (mock-capable), FR-AUD-001.

**Future:** Real sidecar/service-mesh interception, Kafka-driven async lineage fan-out at scale, advanced anomaly detection, multi-region deployment, WORM storage, digital signatures, SSO/SAML — see [ROADMAP.md](./ROADMAP.md).
