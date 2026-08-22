# Redis container

Redis 7 persists append-only local cache data in `policymesh-redis-data` and is checked with `redis-cli ping`. It is a cache, not a source of truth: backend code must tolerate cache loss/unavailability and read PostgreSQL.
