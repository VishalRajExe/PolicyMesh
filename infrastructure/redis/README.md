# Redis ownership

Infrastructure supplies Redis 7. Backend code owns cache semantics, serialization, TTL, and invalidation. See [key-schema.md](key-schema.md); PostgreSQL remains authoritative.
