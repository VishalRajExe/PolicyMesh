#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Health Check
# ==============================================================================
# Check the health of all PolicyMesh services.
# Usage: ./scripts/health-check.sh [--help]
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
            log_header "POLICYMESH HEALTH CHECK"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Check the health of all PolicyMesh services."
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

# --- Service URLs ---
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
AI_SERVICE_URL="${AI_SERVICE_URL:-http://localhost:8000}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"

POSTGRES_PORT="${POSTGRES_PORT:-5432}"
REDIS_PORT="${REDIS_PORT:-6379}"
KAFKA_PORT="${KAFKA_PORT:-9092}"

# --- Health checks ---
TOTAL=0
PASSED=0
FAILED=0

check_ok()   { TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1)); }
check_fail() { TOTAL=$((TOTAL + 1)); FAILED=$((FAILED + 1)); }

log_header "POLICYMESH HEALTH CHECK"
echo ""

# PostgreSQL
TOTAL=$((TOTAL + 1))
if command_exists pg_isready; then
    if pg_isready -h localhost -p "$POSTGRES_PORT" -q 2>/dev/null; then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP\n" "PostgreSQL"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} %-16s DOWN\n" "PostgreSQL"
        FAILED=$((FAILED + 1))
    fi
elif (echo >/dev/tcp/localhost/"$POSTGRES_PORT") 2>/dev/null; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (port reachable)\n" "PostgreSQL"
    PASSED=$((PASSED + 1))
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "PostgreSQL"
    TOTAL=$((TOTAL - 1))
fi

# Redis
TOTAL=$((TOTAL + 1))
if command_exists redis-cli; then
    if redis-cli -h localhost -p "$REDIS_PORT" ping 2>/dev/null | grep -q "PONG"; then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP\n" "Redis"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_RED}❌${COLOR_RESET} %-16s DOWN\n" "Redis"
        FAILED=$((FAILED + 1))
    fi
elif (echo >/dev/tcp/localhost/"$REDIS_PORT") 2>/dev/null; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (port reachable)\n" "Redis"
    PASSED=$((PASSED + 1))
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "Redis"
    TOTAL=$((TOTAL - 1))
fi

# Kafka
TOTAL=$((TOTAL + 1))
if (echo >/dev/tcp/localhost/"$KAFKA_PORT") 2>/dev/null; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP\n" "Kafka"
    PASSED=$((PASSED + 1))
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "Kafka"
    TOTAL=$((TOTAL - 1))
fi

# Backend
TOTAL=$((TOTAL + 1))
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BACKEND_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (HTTP %s)\n" "Backend" "$HTTP_CODE"
    PASSED=$((PASSED + 1))
else
    # Try root endpoint
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BACKEND_URL" 2>/dev/null || echo "000")
    if [[ "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (HTTP %s)\n" "Backend" "$HTTP_CODE"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "Backend"
        TOTAL=$((TOTAL - 1))
    fi
fi

# AI Service
TOTAL=$((TOTAL + 1))
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$AI_SERVICE_URL/health" 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (HTTP %s)\n" "AI Service" "$HTTP_CODE"
    PASSED=$((PASSED + 1))
else
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$AI_SERVICE_URL" 2>/dev/null || echo "000")
    if [[ "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
        printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (HTTP %s)\n" "AI Service" "$HTTP_CODE"
        PASSED=$((PASSED + 1))
    else
        printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "AI Service"
        TOTAL=$((TOTAL - 1))
    fi
fi

# Frontend
TOTAL=$((TOTAL + 1))
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$FRONTEND_URL" 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" =~ ^(200|201|204)$ ]]; then
    printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-16s UP (HTTP %s)\n" "Frontend" "$HTTP_CODE"
    PASSED=$((PASSED + 1))
else
    printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-16s NOT RUNNING\n" "Frontend"
    TOTAL=$((TOTAL - 1))
fi

# --- Summary ---
echo ""
if [[ $FAILED -eq 0 ]] && [[ $TOTAL -gt 0 ]]; then
    log_success "All $TOTAL service(s) healthy."
elif [[ $TOTAL -eq 0 ]]; then
    log_warning "No services detected."
else
    log_warning "$PASSED/$TOTAL services healthy. ($FAILED down)"
fi

echo ""

# --- URLs ---
echo "Service URLs:"
echo "  Frontend:     $FRONTEND_URL"
echo "  Backend:      $BACKEND_URL"
echo "  AI Service:   $AI_SERVICE_URL"
echo ""

if [[ $FAILED -gt 0 ]]; then
    exit 1
fi
exit 0
