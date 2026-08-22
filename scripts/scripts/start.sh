#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Start
# ==============================================================================
# Start PolicyMesh locally.
# Usage: ./scripts/start.sh [--infra-only] [--backend-only] [--demo] [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

START_BACKEND=true
START_FRONTEND=true
START_AI_SERVICE=true
START_INFRA=true
DEMO_MODE=false
VERBOSE=false

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --infra-only)    START_BACKEND=false; START_FRONTEND=false; START_AI_SERVICE=false; shift ;;
        --backend-only)  START_FRONTEND=false; START_AI_SERVICE=false; shift ;;
        --demo)          DEMO_MODE=true; shift ;;
        --no-color)      export NO_COLOR=true; _setup_colors; shift ;;
        --verbose)       VERBOSE=true; shift ;;
        --help|-h)
            log_header "POLICYMESH START"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --infra-only    Start only Docker infrastructure"
            echo "  --backend-only  Start infra + backend only"
            echo "  --demo          Start the demo stack"
            echo "  --no-color      Disable colored output"
            echo "  --verbose       Enable verbose output"
            echo "  --help          Show this help message"
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

# If demo mode, start everything
if [[ "$DEMO_MODE" == "true" ]]; then
    START_BACKEND=true
    START_FRONTEND=true
    START_AI_SERVICE=true
fi

# --- Load environment ---
load_env_file "$REPO_ROOT/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/backend/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/infrastructure/env/.env.dev" 2>/dev/null || true

# --- Detect compose ---
COMPOSE_FILE=$(detect_compose_file 2>/dev/null || true)
COMPOSE_CMD=$(detect_compose_cmd 2>/dev/null || true)

if [[ -z "$COMPOSE_FILE" ]]; then
    log_warning "No docker-compose.yml found — Docker infrastructure will not be started."
fi
if [[ -z "$COMPOSE_CMD" ]]; then
    log_warning "Docker Compose not found — Docker infrastructure will not be started."
fi

# --- Port conflict detection ---
echo ""
log_info "Checking port availability..."
echo ""
PORT_CONFLICTS=0
declare -A PORT_LABELS=(
    [5432]="PostgreSQL"
    [6379]="Redis"
    [9092]="Kafka"
    [8080]="Backend"
    [8000]="AI Service"
    [5173]="Frontend"
)

for port in "${!PORT_LABELS[@]}"; do
    if port_in_use "$port"; then
        log_warning "Port $port (${PORT_LABELS[$port]}) is already in use."
        PORT_CONFLICTS=$((PORT_CONFLICTS + 1))
    fi
done

if [[ $PORT_CONFLICTS -gt 0 ]]; then
    echo ""
    log_warning "Some ports are in use. Services may already be running."
    log_info "Run ./scripts/health-check.sh to check status."
    echo ""
fi

# --- Start Docker infrastructure ---
TOTAL_STEPS=3
CURRENT_STEP=0

if [[ -n "$COMPOSE_FILE" ]] && [[ -n "$COMPOSE_CMD" ]]; then
    CURRENT_STEP=$((CURRENT_STEP + 1))
    step $CURRENT_STEP $TOTAL_STEPS "Starting Docker infrastructure"
    echo ""

    COMPOSE_CMD_FULL="$COMPOSE_CMD -f $REPO_ROOT/$COMPOSE_FILE"

    if [[ "$VERBOSE" == "true" ]]; then
        cd "$REPO_ROOT" && $COMPOSE_CMD_FULL up -d
    else
        cd "$REPO_ROOT" && $COMPOSE_CMD_FULL up -d 2>/dev/null
    fi

    if [[ $? -ne 0 ]]; then
        log_error "Docker Compose failed to start."
        echo ""
        echo "Diagnostics:"
        cd "$REPO_ROOT" && $COMPOSE_CMD_FULL ps 2>/dev/null || true
        echo ""
        echo "Check logs with: $COMPOSE_CMD_FULL logs"
        exit 1
    fi

    log_success "Docker infrastructure started"
    echo ""

    # Wait for infrastructure services
    CURRENT_STEP=$((CURRENT_STEP + 1))
    step $CURRENT_STEP $TOTAL_STEPS "Waiting for infrastructure services"
    echo ""

    POSTGRES_PORT="${POSTGRES_PORT:-5432}"
    REDIS_PORT="${REDIS_PORT:-6379}"
    KAFKA_PORT="${KAFKA_PORT:-9092}"

    wait_for_postgres "localhost" "$POSTGRES_PORT" || {
        log_error "PostgreSQL failed to start."
        log_info "Check: $COMPOSE_CMD_FULL logs postgres"
        exit 1
    }

    wait_for_redis "localhost" "$REDIS_PORT" || {
        log_error "Redis failed to start."
        log_info "Check: $COMPOSE_CMD_FULL logs redis"
        exit 1
    }

    wait_for_kafka "localhost" "$KAFKA_PORT" || {
        log_error "Kafka failed to start."
        log_info "Check: $COMPOSE_CMD_FULL logs kafka"
        exit 1
    }

    echo ""
fi

# --- Start backend ---
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
if [[ "$START_BACKEND" == "true" ]] && [[ -d "$REPO_ROOT/backend" ]]; then
    CURRENT_STEP=$((CURRENT_STEP + 1))
    step $CURRENT_STEP $TOTAL_STEPS "Starting backend"
    echo ""

    if [[ -f "$REPO_ROOT/backend/pom.xml" ]] && command_exists mvn; then
        (cd "$REPO_ROOT/backend" && mvn spring-boot:run -q > /tmp/policymesh-backend.log 2>&1 &)
        log_info "Backend starting in background..."
        wait_for_http "$BACKEND_URL/actuator/health" "Backend" || {
            log_warning "Backend health endpoint not reachable — it may still be starting."
            log_info "Check logs: tail -50 /tmp/policymesh-backend.log"
        }
    elif [[ -f "$REPO_ROOT/backend/package.json" ]] && command_exists npm; then
        (cd "$REPO_ROOT/backend" && npm start > /tmp/policymesh-backend.log 2>&1 &)
        log_info "Backend starting in background..."
        wait_for_http "$BACKEND_URL" "Backend" || {
            log_warning "Backend not reachable."
        }
    else
        log_warning "No runnable backend found (need pom.xml or package.json)"
    fi
    echo ""
fi

# --- Start AI service ---
AI_SERVICE_URL="${AI_SERVICE_URL:-http://localhost:8000}"
if [[ "$START_AI_SERVICE" == "true" ]] && [[ -d "$REPO_ROOT/ai-service" ]]; then
    CURRENT_STEP=$((CURRENT_STEP + 1))
    step $CURRENT_STEP $TOTAL_STEPS "Starting AI service"
    echo ""

    if [[ -f "$REPO_ROOT/ai-service/main.py" ]] || [[ -f "$REPO_ROOT/ai-service/app.py" ]] || [[ -f "$REPO_ROOT/ai-service/pyproject.toml" ]]; then
        if [[ -f "$REPO_ROOT/ai-service/pyproject.toml" ]] && command_exists python3; then
            (cd "$REPO_ROOT/ai-service" && python3 -m uvicorn main:app --host 0.0.0.0 --port 8000 > /tmp/policymesh-ai.log 2>&1 &)
        elif command_exists python3; then
            local main_file="main.py"
            [[ -f "$REPO_ROOT/ai-service/app.py" ]] && main_file="app.py"
            (cd "$REPO_ROOT/ai-service" && python3 "$main_file" > /tmp/policymesh-ai.log 2>&1 &)
        fi
        log_info "AI service starting in background..."
        wait_for_http "$AI_SERVICE_URL/health" "AI Service" || \
            wait_for_http "$AI_SERVICE_URL" "AI Service" || {
            log_warning "AI service not reachable — it may still be starting."
        }
    else
        log_warning "No runnable AI service found"
    fi
    echo ""
fi

# --- Start frontend ---
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"
if [[ "$START_FRONTEND" == "true" ]] && [[ -d "$REPO_ROOT/frontend" ]]; then
    CURRENT_STEP=$((CURRENT_STEP + 1))
    step $CURRENT_STEP $TOTAL_STEPS "Starting frontend"
    echo ""

    if [[ -f "$REPO_ROOT/frontend/package.json" ]] && command_exists npm; then
        (cd "$REPO_ROOT/frontend" && npm run dev > /tmp/policymesh-frontend.log 2>&1 &)
        log_info "Frontend starting in background..."
        wait_for_http "$FRONTEND_URL" "Frontend" || {
            log_warning "Frontend not reachable — it may still be starting."
        }
    else
        log_warning "No runnable frontend found"
    fi
    echo ""
fi

# --- Final status ---
log_header "POLICYMESH READY"
echo ""
echo "Frontend:     $FRONTEND_URL"
echo "Backend:      $BACKEND_URL"
echo "AI Service:   $AI_SERVICE_URL"
echo ""
echo "PostgreSQL:   ✅"
echo "Redis:        ✅"
echo "Kafka:        ✅"
echo ""
echo "Next: ./scripts/health-check.sh"
echo ""
exit 0
