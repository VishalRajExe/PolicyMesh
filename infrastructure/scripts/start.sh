#!/usr/bin/env bash
set -euo pipefail

mode="${1:-default}"
case "$mode" in default|dev|demo) ;; *) echo "Usage: $0 [default|dev|demo]" >&2; exit 2;; esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
infra_dir="$(cd "$script_dir/.." && pwd)"
env_file="$infra_dir/env/.env"
base_compose="$infra_dir/compose/docker-compose.yml"
compose_args=(--env-file "$env_file" -f "$base_compose")

command -v docker >/dev/null || { echo "Docker is required." >&2; exit 1; }
docker compose version >/dev/null || { echo "Docker Compose v2 is required." >&2; exit 1; }
if [[ ! -f "$env_file" ]]; then
  cp "$infra_dir/env/.env.example" "$env_file"
  echo "Created DEVELOPMENT ONLY $env_file; review its credentials before sharing."
fi
if [[ "$mode" != "default" ]]; then
  compose_args+=(-f "$infra_dir/compose/docker-compose.$mode.yml")
fi
docker compose "${compose_args[@]}" up -d --build --remove-orphans
ready=false
for _ in {1..30}; do
  if bash "$script_dir/health-check.sh" "$mode" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 3
done
bash "$script_dir/health-check.sh" "$mode"
if [[ "$ready" != true ]]; then
  echo "PolicyMesh services did not become healthy within 90 seconds." >&2
  exit 1
fi
echo "AI service: http://localhost:$(grep -E '^AI_SERVICE_PORT=' "$env_file" | cut -d= -f2 || echo 8000)"
echo "PostgreSQL: localhost:5432 | Redis: localhost:6379 | Kafka: localhost:9092"
