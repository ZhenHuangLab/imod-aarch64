#!/usr/bin/env bash
set -euo pipefail

IMOD_VERSION="${IMOD_VERSION:-4.11.25}"
IMOD_REPO="${IMOD_REPO:-$HOME/software/imod-aarch64}"
IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"

source "$IMOD_VERSION_DIR/env-cpu.sh"
cd "$IMOD_SOURCE_DIR"

setup_log="$IMOD_VERSION_DIR/build-logs/setup-cpu.log"
set +e
./setup -c gfortran -i "$IMOD_CPU_PREFIX" 2>&1 | tee "$setup_log"
setup_exit=${PIPESTATUS[0]}
set -e
exit "$setup_exit"
