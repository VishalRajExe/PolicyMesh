# Redis Infrastructure & Setup

PolicyMesh provides Redis 7 via Docker Compose without requiring any local installation on the host machine.

## Container Specification
- **Image**: `redis:7` / `redis:7-alpine`
- **Container Name**: `policymesh-redis`
- **Port**: `127.0.0.1:6379:6379`
- **Volume**: `policymesh-redis-data:/data` (AOF persistence)
- **Healthcheck**: `redis-cli ping` (interval 5s, timeout 3s, retries 10, start_period 5s)

## Commands
```bash
# Start Redis and dependencies
docker compose up -d

# Verify health
docker exec policymesh-redis redis-cli ping

# Open Redis CLI
docker exec -it policymesh-redis redis-cli
```
