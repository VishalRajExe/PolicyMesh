#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Seed Demo Data
# ==============================================================================
# Populate PolicyMesh with the canonical demo scenario.
# Usage: ./scripts/seed-demo.sh [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-color) export NO_COLOR=true; _setup_colors; shift ;;
        --help|-h)
            log_header "POLICYMESH DEMO SEED"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Populate PolicyMesh with the canonical demo scenario:"
            echo "  - EU PII policy"
            echo "  - orders-api (EU), payments-api (EU), analytics-api (US)"
            echo "  - Data flows: orders→payments, orders→analytics"
            echo ""
            print_common_help
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

# --- Load environment ---
load_env_file "$REPO_ROOT/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/backend/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/infrastructure/env/.env.dev" 2>/dev/null || true

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
BACKEND_TOKEN="${BACKEND_TOKEN:-}"

# --- Auth helper ---
auth_header() {
    if [[ -n "$BACKEND_TOKEN" ]]; then
        echo "-H \"Authorization: Bearer $BACKEND_TOKEN\""
    fi
}

api_post() {
    local endpoint="$1"
    local data="$2"
    local auth=$(auth_header)
    if [[ -n "$auth" ]]; then
        curl -s -X POST "$BACKEND_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $BACKEND_TOKEN" \
            -d "$data" 2>/dev/null
    else
        curl -s -X POST "$BACKEND_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" 2>/dev/null
    fi
}

api_get() {
    local endpoint="$1"
    if [[ -n "$BACKEND_TOKEN" ]]; then
        curl -s "$BACKEND_URL$endpoint" \
            -H "Authorization: Bearer $BACKEND_TOKEN" 2>/dev/null
    else
        curl -s "$BACKEND_URL$endpoint" 2>/dev/null
    fi
}

# --- Check backend ---
log_header "POLICYMESH DEMO SEED"
echo ""

log_info "Checking backend availability..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BACKEND_URL" 2>/dev/null || echo "000")
if [[ ! "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
    log_error "Backend is not reachable at $BACKEND_URL"
    echo ""
    echo "Start the backend first:"
    echo "  ./scripts/start.sh"
    exit 1
fi
log_success "Backend is up"
echo ""

# --- Try demo/seed endpoint first (if backend provides one) ---
SEED_RESPONSE=$(api_post "/api/v1/demo/seed" '{"scenario":"hackathon"}' 2>/dev/null)
SEED_CODE=$(echo "$SEED_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null || echo "")

if [[ "$SEED_CODE" == "ok" ]] || [[ "$SEED_CODE" == "created" ]]; then
    log_success "Demo seed endpoint available — using backend seed"
    echo ""
    echo "$SEED_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$SEED_RESPONSE"
    echo ""
    log_success "Demo data loaded via backend API."
    exit 0
fi

# --- Manual seeding via individual APIs ---
log_info "Using individual API endpoints..."
echo ""

# Step 1: Create policy
log_info "Creating EU PII policy..."
POLICY_RESP=$(api_post "/api/v1/policies" '{
    "id": "EU-PII-001",
    "name": "EU PII Data Protection",
    "description": "Restricts EU PII from leaving EU jurisdictions",
    "type": "data-classification",
    "rules": [
        {
            "condition": "source.region == \"EU\" AND data.classification == \"PII\"",
            "action": "DENY",
            "reason": "EU PII must not be transferred to non-EU regions"
        },
        {
            "condition": "source.region == \"EU\" AND target.region == \"EU\"",
            "action": "ALLOW",
            "reason": "EU to EU PII transfer allowed"
        }
    ]
}' 2>/dev/null)

if echo "$POLICY_RESP" | grep -qi "error\|already exist"; then
    log_success "Policy EU-PII-001 already exists. Skipping."
else
    log_success "✅ EU-PII-001"
fi

# Step 2: Create services
log_info "Creating services..."

api_post "/api/v1/services" '{"id":"orders-api","name":"Orders API","region":"EU","description":"Order management service"}' 2>/dev/null
log_success "✅ orders-api (EU)"

api_post "/api/v1/services" '{"id":"payments-api","name":"Payments API","region":"EU","description":"Payment processing service"}' 2>/dev/null
log_success "✅ payments-api (EU)"

api_post "/api/v1/services" '{"id":"analytics-api","name":"Analytics API","region":"US","description":"Analytics and reporting service"}' 2>/dev/null
log_success "✅ analytics-api (US)"

# Step 3: Create data flows
log_info "Creating data flows..."

api_post "/api/v1/data-flows" '{
    "source": "orders-api",
    "target": "payments-api",
    "dataClassification": "PII",
    "description": "Order data sent to payments"
}' 2>/dev/null
log_success "✅ orders → payments"

api_post "/api/v1/data-flows" '{
    "source": "orders-api",
    "target": "analytics-api",
    "dataClassification": "PII",
    "description": "Order data sent to analytics"
}' 2>/dev/null
log_success "✅ orders → analytics"

# --- Done ---
echo ""
log_header "POLICYMESH DEMO SEED COMPLETE"
echo ""
echo "Scenario:"
echo "  EU-PII-001   EU PII Protection Policy"
echo ""
echo "  orders-api     EU"
echo "  payments-api   EU"
echo "  analytics-api  US"
echo ""
echo "  orders → payments   (EU→EU, should ALLOW)"
echo "  orders → analytics  (EU→US, should DENY)"
echo ""
echo "Next: ./scripts/run-demo.sh"
echo ""
exit 0
