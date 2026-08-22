#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "$script_dir/start.sh" demo
echo "No backend seed endpoint exists in this repository, so no data was fabricated."
echo "Integration point: after the backend exposes an authenticated demo-seed endpoint, call it here only after its health check passes."
