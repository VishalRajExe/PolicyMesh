# MySQL Container Setup

PolicyMesh uses MySQL 8.4 as its primary persistent database.

## Container Specification
- **Image**: `mysql:8.4`
- **Container Name**: `policymesh-mysql`
- **Host Port**: `3306:3306`
- **Internal Hostname**: `mysql:3306`
- **Persistent Volume**: `policymesh-mysql-data:/var/lib/mysql`
- **Healthcheck**: `mysqladmin ping -h localhost -u root -p<password>`

## Initialization
Initial schemas and databases (`policymesh` and `policymeshdb`) are initialized via `init/001-init.sql` on first boot.
Application tables and migrations are managed automatically by Spring Boot JPA / DDL.
