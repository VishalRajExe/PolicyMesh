#!/usr/bin/env bash
#
# setup-java.sh — PolicyMesh Java environment bootstrap
#
# In GitHub Actions the official action setup-java@v4 is used; this script
# documents the exact toolchain and also works for local / ad-hoc runners
# where the action is unavailable.
#
# Usage:  bash .github/scripts/setup-java.sh
#
set -euo pipefail

JAVA_MAJOR="${1:-21}"
JAVA_DIST="temurin"

echo "=== PolicyMesh Java Setup ==="
echo "Required: Java ${JAVA_MAJOR} (${JAVA_DIST})"

# --- GitHub Actions: rely on the action, nothing to do here ---------------
if [ -n "${GITHUB_ACTIONS:-}" ]; then
    echo "Running inside GitHub Actions — Java is provisioned by actions/setup-java."
    echo "  (workflow uses: distribution: ${JAVA_DIST}, java-version: ${JAVA_MAJOR})"
    if command -v java >/dev/null 2>&1; then
        java -version
    fi
    exit 0
fi

# --- Local / manual bootstrap ---------------------------------------------
# Try SDKMAN first
if command -v sdk >/dev/null 2>&1; then
    echo "Using SDKMAN to install Java ${JAVA_MAJOR} (${JAVA_DIST})"
    sdk install java "${JAVA_MAJOR}-${JAVA_DIST}"
    sdk use java "${JAVA_MAJOR}-${JAVA_DIST}"
# Fall back to a manual download
else
    echo "SDKMAN not found — downloading Temurin ${JAVA_MAJOR} manually."

    OS=$(uname -s)
    ARCH=$(uname -m)
    case "$ARCH" in
        x86_64|amd64)  J_ARCH="x64";;
        aarch64|arm64) J_ARCH="aarch64";;
        *) echo "Unsupported architecture: $ARCH"; exit 1;;
    esac
    case "$OS" in
        Linux*)  J_OS="linux"; J_EXT="tar.gz";;
        Darwin*) J_OS="mac";    J_EXT="tar.gz";;
        *) echo "Unsupported OS: $OS"; exit 1;;
    esac

    TMPDIR=$(mktemp -d)
    URL="https://github.com/adoptium/temurin${JAVA_MAJOR}-binaries/releases/latest/download/OpenJDK${JAVA_MAJOR}U-jdk_${J_ARCH}_${J_OS}.${J_EXT}"
    echo "Downloading: $URL"
    curl -fsSL "$URL" -o "${TMPDIR}/jdk.${J_EXT}"
    mkdir -p "$HOME/.local/opt"
    tar -xf "${TMPDIR}/jdk.${J_EXT}" -C "$HOME/.local/opt"
    JDK_DIR=$(find "$HOME/.local/opt" -maxdepth 1 -name "jdk-${JAVA_MAJOR}*" -type d | head -1)
    echo "Java installed to: $JDK_DIR"
    export JAVA_HOME="$JDK_DIR"
    export PATH="$JAVA_HOME/bin:$PATH"
    rm -rf "$TMPDIR"
fi

echo "Java version:"
java -version
