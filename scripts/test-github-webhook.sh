#!/usr/bin/env bash
# PolicyMesh GitHub Webhook Test Tool (POSIX Shell)
set -e

BASE_URL="${1:-http://127.0.0.1:8080}"
SECRET="${2:-change-this-webhook-secret-at-least-32-chars}"
BRANCH="${3:-main}"
COMMIT_SHA="${4:-40905bd}"
REPO="${5:-VishalRajExe/PolicyMesh}"

echo "=== PolicyMesh GitHub Webhook Test Tool ==="
echo "Target: $BASE_URL/api/webhooks/github"
echo "Commit: $COMMIT_SHA ($BRANCH)"

# Compute HMAC-SHA256 signature
compute_sig() {
  local payload="$1"
  local hex=$(printf '%s' "$payload" | openssl dgst -sha256 -hmac "$SECRET" -hex | sed 's/^.* //')
  echo "sha256=$hex"
}

# 1. Ping test
PING_BODY='{"zen":"Design for failure."}'
PING_SIG=$(compute_sig "$PING_BODY")
DELIVERY_ID=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "del-12345")

echo -n "[1/2] Sending Ping event... "
curl -s -X POST "$BASE_URL/api/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $PING_SIG" \
  -H "X-GitHub-Event: ping" \
  -H "X-GitHub-Delivery: $DELIVERY_ID" \
  -d "$PING_BODY"
echo ""

# 2. Push test
PUSH_BODY=$(cat <<EOF
{
  "ref": "refs/heads/$BRANCH",
  "after": "$COMMIT_SHA",
  "repository": { "full_name": "$REPO" },
  "sender": { "login": "test-dev" }
}
EOF
)
PUSH_SIG=$(compute_sig "$PUSH_BODY")
PUSH_DELIVERY=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "push-12345")

echo -n "[2/2] Sending Push event... "
curl -s -X POST "$BASE_URL/api/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $PUSH_SIG" \
  -H "X-GitHub-Event: push" \
  -H "X-GitHub-Delivery: $PUSH_DELIVERY" \
  -d "$PUSH_BODY"
echo ""