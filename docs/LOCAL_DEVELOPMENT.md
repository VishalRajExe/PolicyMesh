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

This starts MySQL, Redis, Kafka, and the AI service (see [DOCKER_SETUP.md](./DOCKER_SETUP.md)).

## Start the Backend

```bash
cd backend
mvn spring-boot:run
```

## Environment Variables

| Variable | Purpose | Example |
|---|---|---|
| `DB_HOST` | MySQL host | `localhost` (or `mysql` in Docker) |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL database | `policymeshdb` |
| `DB_USERNAME` | DB username | `root` |
| `DB_PASSWORD` | DB password | (set in `.env`) |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap | `localhost:9092` |
| `JWT_SECRET` | JWT signing key | (random 256-bit secret) |
| `AI_SERVICE_URL` | AI FastAPI service base URL | `http://localhost:8000` |
| `AI_SERVICE_MODE` | `live` or `mock` | `mock` |

Copy `.env.example` to `.env` and adjust as needed.

## Database Connection

The backend applies JPA/Hibernate schema management against MySQL. Ensure MySQL is running locally or via Docker Compose before running `mvn spring-boot:run`.

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Seeding Demo Data

A seed endpoint populates the `EU-PII-001` policy, the `orders-api`/`payments-api`/`analytics-api` services, and their data-flow edges:

```bash
curl -X POST http://localhost:8080/api/v1/dev/seed
```
