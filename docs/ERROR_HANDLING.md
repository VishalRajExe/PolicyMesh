# ERROR_HANDLING.md

PolicyMesh uses the `application/problem+json` convention (RFC 7807) for all error responses across [API_SPEC.md](./API_SPEC.md).

## Common Error Structure

```json
{
  "type": "https://policymesh.dev/errors/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "allowedRegions and deniedRegions must not overlap",
  "instance": "/api/v1/policies"
}
```

## Status Codes

| Code | Meaning | Example |
|---|---|---|
| 400 | Malformed request (e.g., invalid JSON) | Unparseable request body |
| 401 | Missing/invalid/expired JWT | No `Authorization` header |
| 403 | Authenticated but not authorized for this role | VIEWER attempts `POST /policies` |
| 404 | Resource not found | `GET /policies/{unknown-id}` |
| 409 | Conflict with existing state | Duplicate `policyCode` |
| 422 | Semantic validation failure | Regions overlap, invalid DSL |
| 429 | Rate limit exceeded | Too many `/auth/login` attempts |
| 500 | Unexpected server error | Unhandled exception |
| 503 | Dependent service unavailable | AI classification service down |

## Rules

- Stack traces, internal exception messages, and secrets must **never** be included in `detail` or any response field.
- `detail` should be actionable and specific enough for a developer to fix the request, without leaking internal implementation.
- Every error response includes `type`, `title`, `status`, `detail`, and `instance` at minimum.
- Validation errors (422) may include an additional `errors` array listing per-field problems.

## Example: Validation Error with Field Details

```json
{
  "type": "https://policymesh.dev/errors/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/policies",
  "errors": [
    { "field": "allowedRegions", "message": "must not be empty" }
  ]
}
```
