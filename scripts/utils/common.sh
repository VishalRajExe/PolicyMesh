#!/usr/bin/env bash
# ==============================================================================
# PolicyMesh — Common Bash Utilities
# ==============================================================================
# Shared helpers used by all PolicyMesh scripts.
# Source this file: source "$(dirname "$0")/utils/common.sh"
# ==============================================================================

set -euo pipefail

# ------------------------------------------------------------------------------
# Repository root detection
# ------------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ------------------------------------------------------------------------------
# Color support (respects NO_COLOR and non-TTY)
# ------------------------------------------------------------------------------
_setup_colors() {
    if [[ "${NO_COLOR:-}" == "true" ]] || [[ "${NO_COLOR:-}" == "1" ]]; then
        COLOR_GREEN="" COLOR_RED="" COLOR_YELLOW="" COLOR_CYAN="" COLOR_BOLD="" COLOR_RESET=""
    elif [[ -t 1 ]]; then
        COLOR_GREEN="\033[0;32m"
        COLOR_RED="\033[0;31m"
        COLOR_YELLOW="\033[0;33m"
        COLOR_CYAN="\033[0;36m"
        COLOR_BOLD="\033[1m"
        COLOR_RESET="\033[0m"
    else
        COLOR_GREEN="" COLOR_RED="" COLOR_YELLOW="" COLOR_CYAN="" COLOR_BOLD="" COLOR_RESET=""
    fi
}
_setup_colors

# ------------------------------------------------------------------------------
# Logging helpers
# ------------------------------------------------------------------------------
log_info()    { printf "${COLOR_CYAN}[INFO]${COLOR_RESET}  %s\n" "$*"; }
log_success() { printf "${COLOR_GREEN}[OK]${COLOR_RESET}    %s\n" "$*"; }
log_warning() { printf "${COLOR_YELLOW}[WARN]${COLOR_RESET}  %s\n" "$*"; }
log_error()   { printf "${COLOR_RED}[ERROR]${COLOR_RESET} %s\n" "$*" >&2; }

log_header() {
    local title="$1"
    printf "\n"
    printf "${COLOR_BOLD}====================================${COLOR_RESET}\n"
    printf "${COLOR_BOLD}       %s${COLOR_RESET}\n" "$title"
    printf "${COLOR_BOLD}====================================${COLOR_RESET}\n"
}

log_check_pass() { printf "  ${COLOR_GREEN}✅${COLOR_RESET} %-20s\n" "$*"; }
log_check_fail() { printf "  ${COLOR_RED}✗${COLOR_RESET}  %-20s\n" "$*"; }
log_check_skip() { printf "  ${COLOR_YELLOW}⏭${COLOR_RESET}  %-20s (not installed)\n" "$*"; }

# ------------------------------------------------------------------------------
# Command detection
# ------------------------------------------------------------------------------
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# ------------------------------------------------------------------------------
# Port checking
# ------------------------------------------------------------------------------
port_in_use() {
    local port="$1"
    if command_exists ss; then
        ss -tlnp 2>/dev/null | grep -q ":${port} " && return 0
    elif command_exists lsof; then
        lsof -i ":${port}" -sTCP:LISTEN >/dev/null 2>&1 && return 0
    elif command_exists netstat; then
        netstat -tlnp 2>/dev/null | grep -q ":${port} " && return 0
    fi
    return 1
}

# ------------------------------------------------------------------------------
# Wait helpers
# ------------------------------------------------------------------------------
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-120}"

wait_for_port() {
    local host="${1:-localhost}"
    local port="$2"
    local label="${3:-Port $port}"
    local timeout="${4:-$WAIT_TIMEOUT_SECONDS}"
    local elapsed=0

    printf "  Waiting for %s (%s:%s)..." "$label" "$host" "$port"
    while ! (echo >/dev/tcp/"$host"/"$port") 2>/dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [[ $elapsed -ge $timeout ]]; then
            printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
            log_error "$label did not become ready within ${timeout}s."
            return 1
        fi
    done
    printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
    return 0
}

wait_for_http() {
    local url="$1"
    local label="${2:-$url}"
    local timeout="${3:-$WAIT_TIMEOUT_SECONDS}"
    local elapsed=0

    printf "  Waiting for %s..." "$label"
    while true; do
        local http_code
        http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")
        if [[ "$http_code" =~ ^(200|201|204)$ ]]; then
            printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        if [[ $elapsed -ge $timeout ]]; then
            printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
            log_error "$label did not become ready within ${timeout}s."
            return 1
        fi
    done
}

wait_for_postgres() {
    local host="${1:-localhost}"
    local port="${2:-5432}"
    local timeout="${3:-$WAIT_TIMEOUT_SECONDS}"
    local elapsed=0

    printf "  Waiting for PostgreSQL (%s:%s)..." "$host" "$port"
    if command_exists pg_isready; then
        while ! pg_isready -h "$host" -p "$port" -q 2>/dev/null; do
            sleep 2
            elapsed=$((elapsed + 2))
            if [[ $elapsed -ge $timeout ]]; then
                printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
                return 1
            fi
        done
    else
        while ! (echo >/dev/tcp/"$host"/"$port") 2>/dev/null; do
            sleep 2
            elapsed=$((elapsed + 2))
            if [[ $elapsed -ge $timeout ]]; then
                printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
                return 1
            fi
        done
    fi
    printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
    return 0
}

wait_for_redis() {
    local host="${1:-localhost}"
    local port="${2:-6379}"
    local timeout="${3:-$WAIT_TIMEOUT_SECONDS}"
    local elapsed=0

    printf "  Waiting for Redis (%s:%s)..." "$host" "$port"
    while true; do
        if command_exists redis-cli; then
            if redis-cli -h "$host" -p "$port" ping 2>/dev/null | grep -q "PONG"; then
                printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
                return 0
            fi
        else
            if (echo >/dev/tcp/"$host"/"$port") 2>/dev/null; then
                printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
                return 0
            fi
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        if [[ $elapsed -ge $timeout ]]; then
            printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
            return 1
        fi
    done
}

wait_for_kafka() {
    local host="${1:-localhost}"
    local port="${2:-9092}"
    local timeout="${3:-$WAIT_TIMEOUT_SECONDS}"
    local elapsed=0

    printf "  Waiting for Kafka (%s:%s)..." "$host" "$port"
    while ! (echo >/dev/tcp/"$host"/"$port") 2>/dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [[ $elapsed -ge $timeout ]]; then
            printf " ${COLOR_RED}TIMEOUT${COLOR_RESET}\n"
            return 1
        fi
    done
    printf " ${COLOR_GREEN}UP${COLOR_RESET} (${elapsed}s)\n"
    return 0
}

# ------------------------------------------------------------------------------
# Docker Compose detection
# ------------------------------------------------------------------------------
detect_compose_file() {
    local candidates=(
        "infrastructure/compose/docker-compose.yml"
        "infrastructure/docker-compose.yml"
        "docker-compose.yml"
    )
    for candidate in "${candidates[@]}"; do
        if [[ -f "$REPO_ROOT/$candidate" ]]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

detect_compose_cmd() {
    if command_exists docker && docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    elif command_exists docker-compose; then
        echo "docker-compose"
    else
        return 1
    fi
}

# ------------------------------------------------------------------------------
# Environment loading
# ------------------------------------------------------------------------------
load_env_file() {
    local env_file="$1"
    if [[ -f "$env_file" ]]; then
        log_info "Loading $env_file"
        set -a
        # shellcheck disable=SC1090
        source "$env_file"
        set +a
    fi
}

# ------------------------------------------------------------------------------
# CI detection
# ------------------------------------------------------------------------------
is_ci() {
    [[ "${CI:-}" == "true" ]] || [[ "${GITHUB_ACTIONS:-}" == "true" ]] || [[ "${JENKINS:-}" == "true" ]]
}

# ------------------------------------------------------------------------------
# Progress helpers
# ------------------------------------------------------------------------------
step() {
    local step_num="$1"
    local total="$2"
    local label="$3"
    printf "\n${COLOR_BOLD}[%s/%s] %s${COLOR_RESET}\n" "$step_num" "$total" "$label"
}

# ------------------------------------------------------------------------------
# Help text helpers
# ------------------------------------------------------------------------------
print_common_help() {
    printf "Options:\n"
    printf "  --help        Show this help message\n"
    printf "  --no-color    Disable colored output\n"
    printf "  --verbose     Enable verbose output\n"
}
