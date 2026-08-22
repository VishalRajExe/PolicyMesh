# PostgreSQL container

PostgreSQL 16 stores persistent data in `policymesh-postgres-data`, exports port 5432, and reports healthy only after `pg_isready` accepts connections. `init/001-init.sql` is infrastructure-only (`pgcrypto`). Spring Boot owns application schema migrations and must be the only owner of PolicyMesh tables.
