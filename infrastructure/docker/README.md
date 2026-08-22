# Docker runtime

`../compose/docker-compose.yml` is the canonical local stack. It creates `policymesh-network`, persistent named volumes, PostgreSQL 16, Redis 7, a single KRaft Kafka 3.8 broker, an idempotent topic initializer, and the existing AI-service image. Application Dockerfiles remain beside application source; infrastructure does not duplicate them.
