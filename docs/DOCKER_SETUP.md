# DOCKER_SETUP.md

See [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md) for the full startup flow.

## Services

| Service | Port | Internal Name |
|---|---|---|
| Backend (Spring Boot) | 8080 | `backend:8080` |
| MySQL | 3306 | `mysql:3306` |
| Redis | 6379 | `redis:6379` |
| Kafka | 9092 | `kafka:9092` |
| AI service (FastAPI) | 8000 | `ai-service:8000` |
| Frontend | 5173 | - |

## docker-compose.yml Structure

```yaml
name: policymesh

services:
  mysql:
    image: mysql:8.4
    container_name: policymesh-mysql
    environment:
      MYSQL_DATABASE: ${DB_NAME:-policymeshdb}
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-admin}
    ports: ["3306:3306"]
    volumes:
      - policymesh-mysql-data:/var/lib/mysql

  redis:
    image: redis:7
    container_name: policymesh-redis
    ports: ["127.0.0.1:6379:6379"]
    volumes:
      - policymesh-redis-data:/data

  kafka:
    image: apache/kafka:3.8.0
    container_name: policymesh-kafka
    ports: ["9092:29092"]

  backend:
    build: ./backend
    container_name: policymesh-backend
    ports: ["8080:8080"]
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }

  ai-service:
    build: ./ai-service
    container_name: policymesh-ai-service
    ports: ["8000:8000"]
```

## Volumes

- `policymesh-mysql-data`: Persists MySQL database across restarts.
- `policymesh-redis-data`: Persists Redis cache across restarts.
- `policymesh-kafka-data`: Persists Kafka topics and event logs.

## Reset Commands

```bash
docker compose down -v      # Stop and delete containers and persistent volumes
docker compose up -d        # Start fresh stack
```
