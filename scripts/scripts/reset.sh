#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Reset
# ==============================================================================
# Destructive reset: stop services, remove containers, optionally remove volumes.
# Usage: ./scripts/reset.sh --force [--no-color] [--verbose] [--help]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/common.sh"

FORCE=false
REMOVE_VOLUMES=false
VERBOSE=false

# ------------------------------------------------------------------------------
# Argument parsing
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --force)           FORCE=true; shift ;;
        --volumes)         REMOVE_VOLUMES=true; shift ;;
        --no-color)        export NO_COLOR=true; _setup_colors; shift ;;
        --verbose)         VERBOSE=true; shift ;;
        --help|-h)
            log_header "POLICYMESH RESET"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Destructive reset: stop services and remove containers."
            echo ""
            echo "Options:"
            echo "  --force       Required to perform the reset"
            echo "  --volumes     Also remove data volumes (destroys all data)"
            echo "  --no-color    Disable colored output"
            echo "  --verbose     Enable verbose output"
            echo "  --help        Show this help message"
            echo ""
            echo "WARNING: --volumes will destroy all PostgreSQL, Redis, and Kafka data."
            echo ""
            exit 0
            ;;
        *) log_error "Unknown option: $1"; exit 1 ;;
    esac
done

log_header "POLICYMESH RESET"
echo ""

# --- Safety check ---
if [[ "$FORCE" != "true" ]]; then
    log_warning "This will stop all PolicyMesh services and remove development containers."
    if [[ "$REMOVE_VOLUMES" == "true" ]]; then
        log_warning "⚠  Data volumes WILL be removed (all data will be lost)."
    fi
    echo ""
    echo "Run with --force to execute:"
    echo "  $0 --force"
    echo ""
    if [[ "$REMOVE_VOLUMES" == "true" ]]; then
        echo "To also remove data volumes:"
        echo "  $0 --force --volumes"
    fi
    echo ""
    log_info "Dry run — nothing was changed."
    exit 0
fi

# --- Load environment ---
load_env_file "$REPO_ROOT/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/backend/.env" 2>/dev/null || true
load_env_file "$REPO_ROOT/infrastructure/env/.env.dev" 2>/dev/null || true

# --- Step 1: Stop application processes ---
echo ""
log_info "Step 1: Stopping application processes..."
# Delegate to stop script
"$SCRIPT_DIR/stop.sh" --no-color 2>/dev/null || true
echo ""

# --- Step 2: Docker Compose down ---
COMPOSE_FILE=$(detect_compose_file 2>/dev/null || true)
COMPOSE_CMD=$(detect_compose_cmd 2>/dev/null || true)

if [[ -n "$COMPOSE_FILE" ]] && [[ -n "$COMPOSE_CMD" ]]; then
    log_info "Step 2: Removing Docker containers..."
    echo ""

    DOWN_ARGS="down --remove-orphans"
    if [[ "$REMOVE_VOLUMES" == "true" ]]; then
        DOWN_ARGS="$DOWN_ARGS -v"
        log_warning "Removing data volumes..."
    fi

    cd "$REPO_ROOT"
    if [[ "$VERBOSE" == "true" ]]; then
        $COMPOSE_CMD -f "$COMPOSE_FILE" $DOWN_ARGS
    else
        $COMPOSE_CMD -f "$COMPOSE_FILE" $DOWN_ARGS 2>/dev/null
    fi

    if [[ $? -eq 0 ]]; then
        log_success "Docker containers removed"
    else
        log_warning "Some containers may not have been removed cleanly"
    fi
else
    log_warning "No Docker Compose found — skipping container cleanup"
fi

echo ""

# --- Step 3: Clean temp files ---
log_info "Step 3: Cleaning temporary files..."
rm -f /tmp/policymesh-backend.log 2>/dev/null || true
rm -f /tmp/policymesh-ai.log 2>/dev/null || true
rm -f /tmp/policymesh-frontend.log 2>/dev/null || true
log_success "Temp files cleaned"

echo ""
log_header "POLICYMESH RESET COMPLETE"
echo ""
if [[ "$REMOVE_VOLUMES" == "true" ]]; then
    echo "All containers and data volumes have been removed."
else
    echo "Containers removed. Data volumes preserved."
    echo "Use --volumes to also remove data."
fi
echo ""
echo "Next: ./scripts/start.sh"
echo ""
exit 0
