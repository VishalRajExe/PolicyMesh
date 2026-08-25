#!/bin/bash
set -e

echo "================================================================="
echo "  Starting PolicyMesh Unified Enterprise Backend Engine"
echo "  Components: Spring Boot (Java 21) + FastAPI AI (Python 3.11)"
echo "================================================================="

# Trap termination signals to stop both processes cleanly
cleanup() {
  echo "[PolicyMesh] Received shutdown signal. Terminating child processes..."
  if [ -n "$AI_PID" ] && kill -0 "$AI_PID" 2>/dev/null; then
    kill -TERM "$AI_PID" 2>/dev/null || true
  fi
  if [ -n "$SPRING_PID" ] && kill -0 "$SPRING_PID" 2>/dev/null; then
    kill -TERM "$SPRING_PID" 2>/dev/null || true
  fi
  wait
  echo "[PolicyMesh] Unified engine shutdown complete."
  exit 0
}

trap cleanup SIGTERM SIGINT SIGQUIT

# ── 1. Start Embedded Python FastAPI AI Service on 127.0.0.1:8000 ─────────────
echo "[PolicyMesh] Launching embedded Python AI Classification Service on 127.0.0.1:8000..."
export PYTHONPATH="/app/ai-service:$PYTHONPATH"
export APP_ENV="${APP_ENV:-production}"
export AI_PROVIDER="${AI_PROVIDER:-mock}"
export AI_MODEL="${AI_MODEL:-gpt-4o-mini}"

/opt/venv/bin/uvicorn app.main:app \
  --host 127.0.0.1 \
  --port 8000 \
  --app-dir /app/ai-service &
AI_PID=$!

# Quick health check wait for AI service loopback readiness
echo "[PolicyMesh] Verifying AI Service readiness on loopback..."
for i in $(seq 1 30); do
  if curl -sf http://127.0.0.1:8000/health >/dev/null 2>&1; then
    echo "[PolicyMesh] ✓ AI Classification Service is healthy on http://127.0.0.1:8000"
    break
  fi
  sleep 0.4
done

# ── 2. Configure Spring Boot Environment ──────────────────────────────────────
export PORT="${PORT:-8080}"
export AI_SERVICE_MODE="remote"
export AI_SERVICE_URL="http://127.0.0.1:8000"

echo "[PolicyMesh] Launching Spring Boot Policy Engine on port ${PORT}..."

# ── 3. Start Spring Boot Engine ───────────────────────────────────────────────
java -Dserver.port="${PORT}" \
     -Dpolicymesh.ai.mode="remote" \
     -Dpolicymesh.ai.service-url="http://127.0.0.1:8000" \
     -jar /app/backend/app.jar &
SPRING_PID=$!

# Wait for any process to exit
wait -n "$AI_PID" "$SPRING_PID"
cleanup
