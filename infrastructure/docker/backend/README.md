# Backend container interface

No backend Dockerfile currently exists, so no backend container is fabricated. When the backend adds one, place it under `../backend/` and configure `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/policymesh`, Redis `redis:6379`, Kafka `kafka:9092`, and AI `http://ai-service:8000`. Add `/actuator/health` before adding a Compose health check.
