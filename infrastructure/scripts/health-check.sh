#!/usr/bin/env bash
set -u
mode="${1:-default}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
infra_dir="$(cd "$script_dir/.." && pwd)"
env_file="$infra_dir/env/.env"
[[ -f "$env_file" ]] || env_file="$infra_dir/env/.env.example"
compose=(docker compose --env-file "$env_file" -f "$infra_dir/compose/docker-compose.yml")
[[ "$mode" != "default" ]] && compose+=(-f "$infra_dir/compose/docker-compose.$mode.yml")

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for PolicyMesh health checks." >&2
  exit 1
fi
failed=0
status() { printf '%-16s %s\n' "$1" "$2"; }
echo "================================"
echo "     POLICYMESH HEALTH CHECK"
echo "================================"
if "${compose[@]}" exec -T postgres pg_isready -U "$(grep '^POSTGRES_USER=' "$env_file" | cut -d= -f2)" -d "$(grep '^POSTGRES_DB=' "$env_file" | cut -d= -f2)" >/dev/null 2>&1; then status PostgreSQL "UP"; else status PostgreSQL "DOWN"; failed=1; fi
if "${compose[@]}" exec -T redis redis-cli ping 2>/dev/null | grep -qx PONG; then status Redis "UP"; else status Redis "DOWN"; failed=1; fi
if "${compose[@]}" exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then status Kafka "UP"; else status Kafka "DOWN"; failed=1; fi
if curl -fsS --max-time 5 http://localhost:"$(grep '^AI_SERVICE_PORT=' "$env_file" | cut -d= -f2)"/health >/dev/null 2>&1; then status "AI Service" "UP"; else status "AI Service" "DOWN"; failed=1; fi
status Backend "NOT CONFIGURED (no Dockerfile)"
status Frontend "NOT CONFIGURED (no Dockerfile)"
echo "================================"
exit "$failed"
