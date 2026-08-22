#!/usr/bin/env bash
set -euo pipefail
if [[ "${1:-}" != "--confirm" ]]; then
  echo "This deletes the local PostgreSQL, Redis, and Kafka volumes. Re-run with --confirm." >&2
  exit 2
fi
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
infra_dir="$(cd "$script_dir/.." && pwd)"
env_file="$infra_dir/env/.env"
[[ -f "$env_file" ]] || cp "$infra_dir/env/.env.example" "$env_file"
docker compose --env-file "$env_file" -f "$infra_dir/compose/docker-compose.yml" down --volumes --remove-orphans
bash "$script_dir/start.sh"
