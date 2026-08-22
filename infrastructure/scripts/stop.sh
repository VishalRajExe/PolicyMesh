#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
infra_dir="$(cd "$script_dir/.." && pwd)"
env_file="$infra_dir/env/.env"
[[ -f "$env_file" ]] || cp "$infra_dir/env/.env.example" "$env_file"
docker compose --env-file "$env_file" -f "$infra_dir/compose/docker-compose.yml" down
