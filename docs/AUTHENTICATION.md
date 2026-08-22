# AUTHENTICATION.md

See [API_SPEC.md](./API_SPEC.md) §Authentication for endpoint contracts and [SECURITY.md](./SECURITY.md) for broader controls.

## Registration & Login

1. `POST /auth/register` creates a `User` row with a BCrypt-hashed password and an assigned role.
2. `POST /auth/login` verifies credentials and, on success, issues a JWT.

## JWT Creation

- Signed with an HMAC secret (or RSA key pair in future) supplied via environment variable.
- Claims include: `sub` (user id), `email`, `role`, `iat`, `exp`.
- Default expiration: 1 hour (`expiresIn: 3600` in the login response).

## JWT Validation

- A Spring Security filter validates the signature and expiration on every request to a protected endpoint.
- An invalid or expired token results in `401 Unauthorized` (see [ERROR_HANDLING.md](./ERROR_HANDLING.md)).

## Roles & Authorities

Each JWT's `role` claim maps to a single Spring Security authority used for method/URL-level access rules.

## Protected Routes

All routes under `/api/v1/**` are protected except `POST /auth/register` and `POST /auth/login`.

## Password Storage

BCrypt with a standard work factor; plaintext is never persisted or logged (see [SECURITY.md](./SECURITY.md)).

## Token Expiration

Tokens expire after 1 hour in the MVP; there is no session/token revocation list — a compromised token is only invalidated by expiry.

## Refresh Strategy

**Not implemented in the MVP.** A user must log in again after expiry. A refresh-token flow is listed as future work in [ROADMAP.md](./ROADMAP.md).

## Role Matrix

| Action | ADMIN | COMPLIANCE_OFFICER | ENGINEER | VIEWER |
|---|---|---|---|---|
| Create/update/deactivate policy | ✅ | ✅ | ❌ | ❌ |
| Register/update service | ✅ | ❌ | ✅ | ❌ |
| Register data flow edge | ✅ | ❌ | ✅ | ❌ |
| Run graph validation | ✅ | ✅ | ✅ | ✅ (read-only trigger) |
| Trigger CI check | ✅ | ✅ | ✅ | ❌ |
| Run enforcement check | ✅ | ❌ | ✅ | ❌ |
| View lineage / verify chain | ✅ | ✅ | ✅ | ✅ |
| Approve/reject AI classification | ✅ | ✅ | ❌ | ❌ |
| Request AI classification | ✅ | ✅ | ✅ | ❌ |
| View dashboard | ✅ | ✅ | ✅ | ✅ |
| Manage users | ✅ | ❌ | ❌ | ❌ |

This matrix is the authoritative source for role checks; controllers must not diverge from it (see [BACKEND_GUIDELINES.md](./BACKEND_GUIDELINES.md)).
