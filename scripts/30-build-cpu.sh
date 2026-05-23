#!/usr/bin/env bash
set -euo pipefail

IMOD_VERSION="${IMOD_VERSION:-4.11.25}"
IMOD_REPO="${IMOD_REPO:-$HOME/software/imod-aarch64}"
IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"

source "$IMOD_VERSION_DIR/env-cpu.sh"
cd "$IMOD_SOURCE_DIR"

make_log="$IMOD_VERSION_DIR/build-logs/make-cpu.full.log"
set +e
make -j"$(nproc)" 2>&1 | tee "$make_log"
make_exit=${PIPESTATUS[0]}
set -e
if [ "$make_exit" -ne 0 ]; then
  exit "$make_exit"
fi

install_log="$IMOD_VERSION_DIR/build-logs/install-cpu.full.log"
set +e
make install 2>&1 | tee "$install_log"
install_exit=${PIPESTATUS[0]}
set -e
exit "$install_exit"
