# ── Stage 1: Build Spring Boot Backend JAR ──────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /build
COPY backend/pom.xml .
RUN mvn --batch-mode -q dependency:go-offline
COPY backend/src ./src
RUN mvn --batch-mode -q clean package -DskipTests

# ── Stage 2: Install Python AI Service Dependencies ─────────────────────────
FROM python:3.11-slim-bookworm AS ai-builder
WORKDIR /ai-build
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
COPY ai-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ── Stage 3: Final Unified Production Container ──────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Install Python 3.11 runtime & curl for health checks
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      python3 \
      python3-distutils \
      curl \
      procps \
    && rm -rf /var/lib/apt/lists/*

# Create non-root system user
RUN groupadd -g 10001 policymesh && \
    useradd -u 10001 -g policymesh -m -s /bin/bash policymesh

WORKDIR /app

# Copy Python virtual environment & AI service code
COPY --from=ai-builder /opt/venv /opt/venv
COPY --chown=policymesh:policymesh ai-service/ /app/ai-service/

# Copy Spring Boot Backend JAR
COPY --from=backend-builder --chown=policymesh:policymesh /build/target/policy-mesh-backend-*.jar /app/backend/app.jar

# Copy Entrypoint Startup Script
COPY --chown=policymesh:policymesh scripts/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

USER policymesh

EXPOSE 8080 8000

ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    AI_SERVICE_URL="http://127.0.0.1:8000" \
    AI_SERVICE_MODE="remote"

ENTRYPOINT ["/app/entrypoint.sh"]
