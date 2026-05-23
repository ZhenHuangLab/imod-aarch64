#!/usr/bin/env bash
set -euo pipefail

IMOD_VERSION="${IMOD_VERSION:-4.11.25}"
IMOD_REPO="${IMOD_REPO:-$HOME/software/imod-aarch64}"
IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"

source "$IMOD_VERSION_DIR/env-cpu.sh"

file "$IMOD_DIR/bin/3dmod"
file "$IMOD_DIR/bin/newstack"
file "$IMOD_DIR/bin/clip"

ldd "$IMOD_DIR/bin/3dmod" | grep 'not found' || true
ldd "$IMOD_DIR/bin/newstack" | grep 'not found' || true
ldd "$IMOD_DIR/bin/clip" | grep 'not found' || true

newstack -help
clip -help
header -help

if [ -n "${DISPLAY:-}" ]; then
  3dmod -h
else
  echo "Skipping 3dmod -h because DISPLAY is empty"
fi
