#!/usr/bin/env bash
set -euo pipefail

IMOD_VERSION="${IMOD_VERSION:-4.11.25}"
IMOD_REPO="${IMOD_REPO:-$HOME/software/imod-aarch64}"

cd "$IMOD_REPO"

if [ ! -d upstream-hg/IMOD/.hg ]; then
  hg clone http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD upstream-hg/IMOD
else
  hg -R upstream-hg/IMOD pull
fi

cd upstream-hg/IMOD
hg tags | grep -E "$(printf '%s' "$IMOD_VERSION" | sed 's/[.]/[.]/g')|$(printf '%s' "$IMOD_VERSION" | tr . _)"
