# DOCKER_SETUP.md

See [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md) for the full startup flow.

## Services

| Service | Port |
|---|---|
| Backend (Spring Boot) | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| AI service (FastAPI) | 8000 |
| Frontend (Vite dev server) | 5173 |

## docker-compose.yml (representative)

```yaml
version: "3.9"
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: policymesh
      POSTGRES_USER: policymesh
      POSTGRES_PASSWORD: policymesh
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]

  redis:
    image: redis:7
    ports: ["6379:6379"]

  kafka:
    image: bitnami/kafka:latest
    ports: ["9092:9092"]
    environment:
      KAFKA_ENABLE_KRAFT: "yes"
      KAFKA_CFG_NODE_ID: "1"
      KAFKA_CFG_PROCESS_ROLES: "broker,controller"

  ai-service:
    build: ./ai-service
    ports: ["8000:8000"]
    environment:
      AI_SERVICE_MODE: mock

volumes:
  pgdata:
```

## Volumes

`pgdata` persists PostgreSQL data across restarts. Delete it (`docker volume rm`) to fully reset the database.

## Environment Variables

Set in a root `.env` file consumed by `docker-compose.yml` and referenced in [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md); never commit real secrets (see [CONTRIBUTING.md](./CONTRIBUTING.md)).

## Reset Commands

```bash
docker compose down -v      # stop and remove containers + volumes (full reset)
docker compose up -d        # start fresh
```

See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common startup failures.
