# BACKEND_GUIDELINES.md

Coding conventions for the Spring Boot backend. See [TECH_STACK.md](./TECH_STACK.md) and [ARCHITECTURE.md](./ARCHITECTURE.md) for context.

## Package Naming

`com.policymesh.<module>` — e.g. `com.policymesh.policy`, `com.policymesh.graph`, `com.policymesh.lineage`, `com.policymesh.auth`.

## Class Naming

- Entities: singular noun (`Policy`, `ServiceNode`, `DataFlowEdge`, `Decision`, `LineageRecord`).
- DTOs: suffix `Request`/`Response` (`CreatePolicyRequest`, `PolicyResponse`).
- Controllers: suffix `Controller` (`PolicyController`).
- Services: suffix `Service` (`PolicyService`, `GraphAnalysisService`).
- Repositories: suffix `Repository` (`PolicyRepository`).

## Layering

```text
Controller → DTO → Service → Repository
```

- **Controllers must not contain business logic.** They validate input (via DTO annotations), call a service method, and map the result to a response DTO.
- **Policy evaluation must remain centralized** in a single `PolicyEngine` used by both the Graph Engine and Runtime Enforcement (see [POLICY_COMPILER.md](./POLICY_COMPILER.md)) — never re-implemented per-controller.

## DTO Usage

Never expose JPA entities directly over the API; always map to/from a DTO to keep the API contract (see [API_SPEC.md](./API_SPEC.md)) decoupled from the schema (see [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)).

## Entity Rules

Entities hold persistence-relevant fields and relationships only; no HTTP-layer concerns (no `@RequestBody` etc. on entities).

## Exception Handling

A single `@ControllerAdvice` translates exceptions to the `application/problem+json` shape defined in [ERROR_HANDLING.md](./ERROR_HANDLING.md). Services throw typed exceptions (`PolicyNotFoundException`, `ValidationException`, etc.) rather than returning nulls or generic exceptions.

## Logging

Structured logging (e.g., JSON via Logback) for every decision and auth event; never log secrets, passwords, tokens, or raw request bodies containing credentials (see [SECURITY.md](./SECURITY.md)).

## Transactions

Any operation that writes both a `Decision` and its `LineageRecord` must be wrapped in a single `@Transactional` boundary so the two are always created together, never one without the other.

## Validation

Jakarta Validation annotations on all request DTOs; service-layer validation for cross-field/DSL-level rules that annotations can't express (see [POLICY_DSL.md](./POLICY_DSL.md) §Validation Rules).

## Security

Role checks via `@PreAuthorize` matching the matrix in [AUTHENTICATION.md](./AUTHENTICATION.md); never re-derive authorization logic ad hoc in a controller method body.

## Testing

Every service class has corresponding unit tests; every controller has MockMvc tests covering both success and the relevant 4xx cases (see [TESTING.md](./TESTING.md)).
