#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Stop
# ==============================================================================
# Stop PolicyMesh services. Non-destructive (does not remove volumes).
# Usage: ./scripts/stop.sh [--help]
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
            log_header "POLICYMESH STOP"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Stop all PolicyMesh services."
            echo "This is non-destructive: data volumes are preserved."
            echo ""
            print_common_help
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

log_header "POLICYMESH STOP"
echo ""

# --- Load environment ---
load_env_file "$REPO_ROOT/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/backend/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/infrastructure/env/.env.dev" 2>/dev/null || true

# --- Kill application processes ---
log_info "Stopping application processes..."
echo ""

KILLED=0

# Backend (Maven Spring Boot)
if pgrep -f "spring-boot:run" >/dev/null 2>&1; then
    pkill -f "spring-boot:run" 2>/dev/null || true
    log_success "Stopped backend (Maven)"
    KILLED=$((KILLED + 1))
fi

# Backend (Java process on port 8080)
if command_exists lsof; then
    BACKEND_PIDS=$(lsof -ti :8080 2>/dev/null || true)
    if [[ -n "$BACKEND_PIDS" ]]; then
        echo "$BACKEND_PIDS" | xargs kill 2>/dev/null || true
        log_success "Stopped process on port 8080"
        KILLED=$((KILLED + 1))
    fi
fi

# AI Service (uvicorn / python)
if pgrep -f "uvicorn.*main:app" >/dev/null 2>&1; then
    pkill -f "uvicorn.*main:app" 2>/dev/null || true
    log_success "Stopped AI service"
    KILLED=$((KILLED + 1))
fi

# Frontend (npm dev server)
if pgrep -f "vite|next dev|react-scripts start" >/dev/null 2>&1; then
    pkill -f "vite|next dev|react-scripts start" 2>/dev/null || true
    log_success "Stopped frontend"
    KILLED=$((KILLED + 1))
fi

if [[ $KILLED -eq 0 ]]; then
    log_info "No application processes found running"
fi

echo ""

# --- Stop Docker Compose ---
COMPOSE_FILE=$(detect_compose_file 2>/dev/null || true)
COMPOSE_CMD=$(detect_compose_cmd 2>/dev/null || true)

if [[ -n "$COMPOSE_FILE" ]] && [[ -n "$COMPOSE_CMD" ]]; then
    log_info "Stopping Docker infrastructure..."
    echo ""

    cd "$REPO_ROOT" && $COMPOSE_CMD -f "$COMPOSE_FILE" stop 2>/dev/null && \
        log_success "Docker services stopped" || \
        log_warning "Docker Compose stop encountered issues"
else
    log_warning "No Docker Compose configuration found — skipping"
fi

echo ""
log_success "PolicyMesh stopped."
echo ""
echo "Volumes and data are preserved."
echo "Use ./scripts/reset.sh --force to remove data."
echo ""
exit 0
