# LOCAL_DEVELOPMENT.md

See [DOCKER_SETUP.md](./DOCKER_SETUP.md) for infrastructure details and [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) if something doesn't start.

## Prerequisites

- Java 21
- Maven
- Node.js (LTS)
- Docker & Docker Compose
- Git

## Initial Setup

```bash
git clone <repo-url>
cd PolicyMesh
```

## Start Infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Kafka, and the AI service (see [DOCKER_SETUP.md](./DOCKER_SETUP.md)).

## Start the Backend

```bash
cd backend
mvn spring-boot:run
```

## Environment Variables

| Variable | Purpose | Example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/policymesh` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `policymesh` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `policymesh` |
| `SPRING_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap | `localhost:9092` |
| `JWT_SECRET` | JWT signing key | (random 256-bit secret) |
| `AI_SERVICE_URL` | AI FastAPI service base URL | `http://localhost:8000` |
| `AI_SERVICE_MODE` | `live` or `mock` | `mock` |

Copy `.env.example` to `.env` and adjust as needed (see [CONTRIBUTING.md](./CONTRIBUTING.md) for what must never be committed).

## Database Connection

The backend applies JPA/Hibernate schema management against the PostgreSQL instance started by Docker Compose. Ensure `docker compose up -d` has fully started PostgreSQL (check with `docker compose ps`) before running `mvn spring-boot:run`.

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Seeding Demo Data

A seed script/endpoint should create: the `EU-PII-001` policy, the `orders-api`/`payments-api`/`analytics-api` services, and their data-flow edges, matching the example scenario in [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) — used directly by [DEMO_FLOW.md](./DEMO_FLOW.md).

```bash
curl -X POST http://localhost:8080/api/v1/dev/seed
```

## Verifying APIs

```bash
# Register and log in
curl -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" \
  -d '{"email":"admin@policymesh.dev","password":"Passw0rd!23","role":"ADMIN"}'

curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@policymesh.dev","password":"Passw0rd!23"}'

# Use the returned token
curl http://localhost:8080/api/v1/policies -H "Authorization: Bearer <token>"
```

See [API_SPEC.md](./API_SPEC.md) for the full endpoint list.
