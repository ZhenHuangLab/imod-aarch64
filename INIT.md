# IMOD Linux aarch64 Downstream Fork Plan

This document describes how to build one or more IMOD versions natively on this machine:

- Host: Ubuntu 24.04.4 LTS
- Architecture: `aarch64`
- First target version: IMOD 4.11.25
- Candidate later versions: IMOD 5.2.x, including 5.2.0 if an exact upstream tag exists
- First build target per version: CPU-only native build
- Later build target per version: optional CUDA build
- Project/repo directory: `~/software/imod-aarch64/`

The plan intentionally does not use the official Linux x86_64 installer and does not use the macOS arm64 installer. The goal is a real Linux/aarch64 build from source.

This repository is intended to become a source-bearing downstream fork/port of IMOD for Linux aarch64, not merely a build notebook. The repository should therefore import the upstream IMOD source into Git, keep local aarch64 changes as normal source commits, and use Git history to explain exactly how each supported version differs from upstream.

This changes the earlier source policy deliberately:

- The tracked `IMOD/` tree is the maintained source tree for the currently checked-out version branch.
- Exact upstream source imports are committed and tagged.
- Local aarch64 fixes are direct edits to tracked source files.
- Build products, install trees, archives, Mercurial metadata, and large logs remain ignored.
- Multi-version support is represented primarily by long-lived Git branches and release tags, not by vendoring full independent source copies under `versions/<version>/src/` on the same branch.

For each IMOD version, the first CPU-only pass is still a clean rebuild baseline. It answers whether that upstream version already builds on Ubuntu/aarch64 with no source changes. If that baseline fails, the next phase is source-level porting with small, signed, convention-compliant commits. If the baseline succeeds, the repository still contains the imported upstream source and records that no aarch64 source changes were needed for that version.

## 1. Directory and GitHub Strategy

Use `~/software/imod-aarch64/` as the project root and Git repository root.

Recommended layout:

```text
~/software/imod-aarch64/
├── README.md
├── INIT.md
├── .gitignore
├── IMOD/
│   └── ... tracked IMOD source for the checked-out version branch ...
├── scripts/
│   ├── 00-install-deps.sh
│   ├── 10-fetch-version.sh
│   ├── 20-configure-cpu.sh
│   ├── 30-build-cpu.sh
│   └── 40-smoke-test.sh
├── versions/
│   ├── 4.11.25/
│   │   ├── BUILD_NOTES.md
│   │   ├── env-cpu.sh
│   │   ├── install/
│   │   │   ├── cpu/
│   │   │   └── cuda/
│   │   └── build-logs/
│   └── 5.2.0/
│       └── ...
└── upstream-hg/
    └── IMOD/
```

Track in Git:

- `README.md`
- `INIT.md`
- scripts under `scripts/`
- the imported upstream IMOD source under `IMOD/`
- per-version notes such as `versions/<version>/BUILD_NOTES.md`
- small per-version build logs or summarized failure notes
- explicit notes on whether the successful build required source changes
- local aarch64 source changes as normal Git commits

Do not track in Git:

- installed binaries
- libraries
- object files
- Qt-generated Makefiles
- large archives
- Mercurial metadata from the upstream clone
- CUDA toolkit files

The important design choice is this: make the GitHub repo a real downstream source fork, but not a binary distribution repo. Source belongs in Git; generated build products and installed binaries do not. If binaries need to be shared later, use GitHub Releases, not normal Git commits.

Use these branches:

- `main`: primary maintained source line. Initially this should follow the IMOD 4.11.25 Linux/aarch64 port.
- `upstream/<version>`: exact upstream import for one IMOD version, with the source tree under `IMOD/` and provenance in `versions/<version>/BUILD_NOTES.md`. Repo helper files such as `INIT.md` and `scripts/` remain downstream metadata; the exactness claim is about the `IMOD/` subtree and recorded Mercurial tag/node id.
- `aarch64/<version>`: maintained Linux/aarch64 branch for that IMOD version.
- `fix/<version>-<topic>`: focused source-fix branches based on `aarch64/<version>`.
- `cuda/<version>-aarch64-build`: optional CUDA build branch based on `aarch64/<version>`.

Use these tags:

- `upstream/imod-<version>`: exact upstream source import.
- `imod-<version>-aarch64.<n>`: local Linux/aarch64 source release tag after smoke tests pass.

## 2. Initialize the Local Git Repository

Create the project directory:

```bash
mkdir -p ~/software/imod-aarch64
cd ~/software/imod-aarch64
git init
```

Create the basic folders:

```bash
mkdir -p scripts versions upstream-hg
mkdir -p versions/4.11.25/install/cpu versions/4.11.25/build-logs
touch scripts/.gitkeep
```

Use Bash strict mode for any checked-in helper script. Several snippets use Bash's `PIPESTATUS` to preserve the failing command's exit code through `tee`.

```bash
#!/usr/bin/env bash
set -euo pipefail
```

Create a `.gitignore`:

```bash
cat > .gitignore <<'EOF'
# Upstream Mercurial clone
/upstream-hg/

# IMOD generated build files and binaries inside the tracked source tree
/IMOD/bin/
/IMOD/buildlib/
/IMOD/configure
/IMOD/.options
/IMOD/.distname
/IMOD/**/*.o
/IMOD/**/*.a
/IMOD/**/*.so
/IMOD/**/qconfigure

# Per-version install products and large logs
/versions/*/install/
/versions/*/build-logs/*.full.log

# Archives and downloads
/downloads/
*.tar
*.tar.gz
*.tgz
*.zip
*.pkg
imod_*.sh
IMOD*.sh

# Editor and OS noise
*~
.DS_Store
.vscode/
AGENTS.md
EOF
```

Create an initial README:

````bash
cat > README.md <<'EOF'
# IMOD Linux aarch64 Downstream Fork

This repository tracks a source-bearing downstream port of IMOD for Ubuntu Linux aarch64.

The tracked `IMOD/` tree contains the upstream source for the checked-out version branch plus any local aarch64 fixes.

The first milestone is a CPU-only build installed under:

```text
~/software/imod-aarch64/versions/4.11.25/install/cpu
```

CUDA support and newer IMOD 5.2.x builds will be attempted only after the first CPU build is working.
EOF
````

Do not make the first commit yet if every commit should show GitHub's `Verified` badge. Configure commit signing first in Section 4, then make the initial commit.

## 3. Connect the Existing GitHub Repository

The GitHub repository already exists:

```text
git@github.com:ZhenHuangLab/imod-aarch64.git
```

`gh` is already installed on this machine, so the remaining setup is to authenticate GitHub CLI if needed, attach the local repository to the existing remote, and set a GitHub CLI default repo for convenience.

Confirm `gh` availability:

```bash
command -v gh
```

Check auth state:

```bash
gh auth status
```

If not logged in yet:

```bash
gh auth login
```

Attach the existing remote:

```bash
cd ~/software/imod-aarch64
git remote add origin git@github.com:ZhenHuangLab/imod-aarch64.git
```

If `origin` already exists, update it instead:

```bash
cd ~/software/imod-aarch64
git remote set-url origin git@github.com:ZhenHuangLab/imod-aarch64.git
```

Verify remote configuration:

```bash
git remote -v
```

Set the GitHub CLI default repository to match:

```bash
gh repo set-default ZhenHuangLab/imod-aarch64
```

## 4. Enable Verified Commits on GitHub

Installing and logging into `gh` is not enough to make commits show the GitHub `Verified` badge. GitHub marks local commits as `Verified` only when they are signed with a verifiable GPG, SSH, or S/MIME signature. For this project, use SSH commit signing because this machine has Git 2.43.0, which supports SSH signing.

Recommended approach: use a dedicated SSH key for commit signing.

Create a signing key:

```bash
ssh-keygen -t ed25519 -C "zhen.victor.huang@gmail.com" -f ~/.ssh/id_ed25519_github_signing
```

Log into GitHub CLI:

```bash
gh auth login
gh auth status
```

Upload the public key to GitHub as a signing key, not just as an authentication key:

```bash
gh ssh-key add ~/.ssh/id_ed25519_github_signing.pub \
  --type signing \
  --title "$(hostname)-imod-signing"
```

Configure only this IMOD repository to sign every commit:

```bash
cd ~/software/imod-aarch64

git config user.name "ZhenHuangLab"
git config user.email "zhen.victor.huang@gmail.com"
git config gpg.format ssh
git config user.signingkey ~/.ssh/id_ed25519_github_signing.pub
git config commit.gpgsign true
git config tag.gpgsign true
```

For this repository, use the verified GitHub email `zhen.victor.huang@gmail.com`.

Make the initial signed commit:

```bash
cd ~/software/imod-aarch64

git add README.md INIT.md .gitignore scripts
git commit -m "chore(repo): initialize build repo"
git log -1 --show-signature

git branch -M main
git push -u origin main
```

Then open the initial commit on GitHub and confirm that it shows `Verified`.

If Git reports an SSH signing error, first make sure the private key exists and is readable:

```bash
ls -l ~/.ssh/id_ed25519_github_signing ~/.ssh/id_ed25519_github_signing.pub
```

If local signature verification prints an `allowedSignersFile` warning, that affects local verification display, not whether GitHub can verify the pushed commit. GitHub verification depends on the public signing key uploaded to the GitHub account and the pushed commit's signature.

From this point forward, normal commits in this repository should be signed automatically:

```bash
git commit -m "chore(repo): initialize build repo"
```

Expected GitHub result:

- commits made after this setup should show `Verified`;
- commits made before this setup will not become verified unless they are rewritten and force-pushed;
- commits made through the GitHub web UI are signed by GitHub separately;
- unsigned commits from scripts or other Git clients will not show `Verified`.

## 4.1 Commit Message Convention

Always use this commit message format in this repository:

```text
<type>(<scope>): <subject>
```

Rules:

- `type` should be one of: `chore`, `docs`, `build`, `fix`, `test`, `source`
- `scope` should describe the area being changed, such as: `repo`, `init`, `cpu`, `cuda`, `aarch64`, `imod`
- `subject` should be short, imperative, and lower-case by default

Recommended examples:

```text
chore(repo): initialize build repo
source(imod): import IMOD 4.11.25 upstream source
source(imod): import IMOD <version> upstream source
build(cpu): document configure attempt
build(cpu): add runtime environment script
test(cpu): record smoke test results
fix(aarch64): handle HDF5 include detection
docs(init): record aarch64 build failure
```

Avoid examples like:

```text
Initialize IMOD 4.11.25 aarch64 build repo
WIP
try stuff
misc updates
```

## 5. Install Build Dependencies

This machine already has:

- `tcsh`
- `gcc`
- `g++`
- `gfortran`
- `java`

Install the missing source-control and Qt/OpenGL/HDF5 development dependencies:

```bash
sudo apt update
sudo apt install -y \
  mercurial tcsh build-essential gfortran default-jdk \
  qtbase5-dev qtbase5-dev-tools libqt5opengl5-dev \
  libtiff-dev libjpeg-dev libfftw3-dev libhdf5-dev \
  libgl1-mesa-dev libglu1-mesa-dev \
  libx11-dev libxext-dev libxmu-dev libxi-dev libxt-dev
```

Confirm key tools:

```bash
command -v hg
command -v tcsh
command -v qmake
command -v gcc
command -v g++
command -v gfortran
command -v javac
```

Expected:

- `hg` should exist.
- `qmake` should exist and should be Qt5, not Qt6.
- `gcc`, `g++`, and `gfortran` should exist.

Check Qt:

```bash
qmake -query QT_VERSION
qmake -query QT_INSTALL_HEADERS
qmake -query QT_INSTALL_LIBS
```

Record exact package versions for each IMOD version after the version directory and `BUILD_NOTES.md` exist. The `apt install` command is intentionally simple; reproducibility comes from recording the exact package versions used for a given build.

```bash
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
mkdir -p "$IMOD_VERSION_DIR/build-logs"

DEPS=(
  mercurial tcsh build-essential gfortran default-jdk
  qtbase5-dev qtbase5-dev-tools libqt5opengl5-dev
  libtiff-dev libjpeg-dev libfftw3-dev libhdf5-dev
  libgl1-mesa-dev libglu1-mesa-dev
  libx11-dev libxext-dev libxmu-dev libxi-dev libxt-dev
)

dpkg-query -W -f='${Package}\t${Version}\t${Architecture}\n' "${DEPS[@]}" \
  | tee "$IMOD_VERSION_DIR/build-logs/apt-packages.tsv"

cat >> "$IMOD_VERSION_DIR/BUILD_NOTES.md" <<'EOF'

## Dependency Snapshot

- Package versions: `build-logs/apt-packages.tsv`

EOF

git add "versions/$IMOD_VERSION/BUILD_NOTES.md" "versions/$IMOD_VERSION/build-logs/apt-packages.tsv"
git commit -m "build(cpu): record dependency versions"
```

## 6. Import IMOD Source and Add a Version

IMOD source is available from the official Mercurial repository:

```text
http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD
```

Use one upstream Mercurial clone as a local source mirror, then import each pinned version into the tracked Git source tree at `IMOD/`. The `IMOD/` directory is the actual downstream fork source tree.

Do not import build artifacts. The import commit should represent upstream source only.

```bash
cd ~/software/imod-aarch64
if [ ! -d upstream-hg/IMOD/.hg ]; then
  hg clone http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD upstream-hg/IMOD
else
  cd upstream-hg/IMOD
  hg pull
  cd ../..
fi
cd upstream-hg/IMOD
```

The `upstream-hg/IMOD` mirror is ignored by Git and can consume noticeable disk space because it includes Mercurial history. Check its size when needed:

```bash
du -sh ~/software/imod-aarch64/upstream-hg/IMOD
```

If disk pressure matters, it is safe to remove this mirror after builds are done because it is reproducible from the upstream URL:

```bash
rm -rf ~/software/imod-aarch64/upstream-hg/IMOD
```

Pick a target version. Start with 4.11.25:

```bash
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
```

For a later 5.2.x build, set `IMOD_VERSION` to the exact version after confirming the upstream tag exists:

```bash
export IMOD_VERSION=5.2.0
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
```

List tags related to the target version:

```bash
hg tags | grep -E "$(printf '%s' "$IMOD_VERSION" | sed 's/[.]/[.]/g')|$(printf '%s' "$IMOD_VERSION" | tr . _)"
```

Use this decision rule:

- If tag `imod_<version>` exists, use that tag.
- If tag `<version>` exists, use that tag.
- If only another obvious equivalent exists, such as `IMOD_<version>` or `imod_<version_with_underscores>`, use that exact tag and record it in `versions/<version>/BUILD_NOTES.md`.
- If no exact tag for the requested version exists, stop. Do not silently use the current nightly source as a substitute.

Update to the chosen tag:

```bash
hg update -r <EXACT_VERSION_TAG>
export IMOD_HG_ID="$(hg id -i -r <EXACT_VERSION_TAG>)"
```

Create an upstream import branch for this exact version:

```bash
cd ~/software/imod-aarch64
git checkout main
git pull --ff-only origin main
git checkout -b "upstream/$IMOD_VERSION"
```

Archive the pinned source into the tracked `IMOD/` source tree:

```bash
cd ~/software/imod-aarch64
rm -rf IMOD
mkdir -p IMOD "$IMOD_VERSION_DIR/install/cpu" "$IMOD_VERSION_DIR/build-logs"
cd upstream-hg/IMOD
hg archive "$IMOD_REPO/IMOD"
```

Record source provenance:

```bash
cd ~/software/imod-aarch64
cat > "$IMOD_VERSION_DIR/BUILD_NOTES.md" <<EOF
# IMOD $IMOD_VERSION Build Notes

## Source

- Upstream: http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD
- Requested version: IMOD $IMOD_VERSION
- Mercurial tag used: <EXACT_VERSION_TAG>
- Mercurial node id: $IMOD_HG_ID
- Host: Ubuntu 24.04.4 LTS aarch64
- First target: CPU-only
- Source changes required: TODO

EOF
```

Commit the upstream source import and provenance together:

```bash
git add "versions/$IMOD_VERSION/BUILD_NOTES.md"
git add -f IMOD
git commit -m "source(imod): import IMOD $IMOD_VERSION upstream source"
git tag -m "source(imod): tag upstream IMOD $IMOD_VERSION import" "upstream/imod-$IMOD_VERSION"
git push -u origin "upstream/$IMOD_VERSION"
git push origin "refs/tags/upstream/imod-$IMOD_VERSION"
```

`git add -f IMOD` is intentional only for clean upstream imports, so ignored build-output patterns do not accidentally drop an upstream file with a generated-looking name. After the import, normal source fixes can be staged with ordinary `git add IMOD/<changed-files>`.

Create the downstream aarch64 branch from this exact upstream import:

```bash
git checkout -b "aarch64/$IMOD_VERSION"
git push -u origin "aarch64/$IMOD_VERSION"
```

For the first supported version, make `main` follow the downstream source branch so GitHub's default branch is source-bearing from the start. Build/test commits can continue on `aarch64/<version>` and be fast-forwarded into `main` when green:

```bash
git checkout main
git merge --ff-only "aarch64/$IMOD_VERSION"
git push origin main
git checkout "aarch64/$IMOD_VERSION"
```

For later versions, keep separate long-lived branches such as `aarch64/5.2.0`. Do not put full independent source copies for every version under `versions/<version>/src/` on the same branch.

## 7. Configure CPU-Only Build

Select the version to configure:

```bash
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
export IMOD_CPU_PREFIX="$IMOD_VERSION_DIR/install/cpu"
```

Set Qt and dependency paths:

```bash
export QTDIR=/usr
export QMAKESPEC=linux-g++
export FFTW3_DIR=/usr/lib/aarch64-linux-gnu
export TIFF4_DIR=/usr/lib/aarch64-linux-gnu
export HDF5_DIR=/usr
```

Create and source the version-specific CPU environment file before configure, build, install, and tests. This avoids hidden reliance on variables from an old terminal session.

```bash
cat > "$IMOD_VERSION_DIR/env-cpu.sh" <<EOF
export IMOD_VERSION="$IMOD_VERSION"
export IMOD_REPO="$IMOD_REPO"
export IMOD_SOURCE_DIR="\$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="\$IMOD_REPO/versions/\$IMOD_VERSION"
export IMOD_CPU_PREFIX="\$IMOD_VERSION_DIR/install/cpu"
export IMOD_DIR="\$IMOD_CPU_PREFIX"
export QTDIR="$QTDIR"
export QMAKESPEC="$QMAKESPEC"
export FFTW3_DIR="$FFTW3_DIR"
export TIFF4_DIR="$TIFF4_DIR"
export HDF5_DIR="$HDF5_DIR"
export PATH="\$IMOD_DIR/bin:\$PATH"
export LD_LIBRARY_PATH="\$IMOD_DIR/lib:\${LD_LIBRARY_PATH:-}"
if [ -f "\$IMOD_DIR/IMOD-linux.sh" ]; then
  source "\$IMOD_DIR/IMOD-linux.sh"
fi
EOF

source "$IMOD_VERSION_DIR/env-cpu.sh"
```

Run setup and preserve its exit status even while logging through `tee`:

```bash
cd "$IMOD_SOURCE_DIR"
setup_log="$IMOD_VERSION_DIR/build-logs/setup-cpu.log"
set +e
./setup -c gfortran -i "$IMOD_CPU_PREFIX" 2>&1 | tee "$setup_log"
setup_exit=${PIPESTATUS[0]}
set -e
if [ "$setup_exit" -ne 0 ]; then
  exit "$setup_exit"
fi
```

If HDF5 fails, retry with Ubuntu's serial HDF5 path:

```bash
export HDF5_DIR=/usr/lib/aarch64-linux-gnu/hdf5/serial
setup_log="$IMOD_VERSION_DIR/build-logs/setup-cpu.log"
set +e
./setup -c gfortran -i "$IMOD_CPU_PREFIX" 2>&1 | tee "$setup_log"
setup_exit=${PIPESTATUS[0]}
set -e
if [ "$setup_exit" -ne 0 ]; then
  exit "$setup_exit"
fi
```

If Qt fails, inspect qmake:

```bash
qmake -query
```

Keep `QTDIR=/usr` and `QMAKESPEC=linux-g++` as the default unless the configure output proves another path is required. If you change `HDF5_DIR` or other build variables, update `versions/<version>/env-cpu.sh` and source it again before continuing.

Commit useful setup notes, but avoid committing huge full logs:

```bash
cd "$IMOD_REPO"
git add "versions/$IMOD_VERSION/BUILD_NOTES.md" "versions/$IMOD_VERSION/build-logs/setup-cpu.log"
git commit -m "build(cpu): document configure attempt"
git push
```

## 8. Build and Install CPU Version

Build:

```bash
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
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
```

If build succeeds, install:

```bash
install_log="$IMOD_VERSION_DIR/build-logs/install-cpu.full.log"
set +e
make install 2>&1 | tee "$install_log"
install_exit=${PIPESTATUS[0]}
set -e
if [ "$install_exit" -ne 0 ]; then
  exit "$install_exit"
fi
```

Commit the environment script and summarized notes:

```bash
git add "versions/$IMOD_VERSION/env-cpu.sh" "versions/$IMOD_VERSION/BUILD_NOTES.md"
git commit -m "build(cpu): add runtime environment script"
git push
```

Do not commit `versions/<version>/install/cpu/`.

## 9. Smoke Test CPU Build

Load the environment:

```bash
source ~/software/imod-aarch64/versions/4.11.25/env-cpu.sh
```

Check architecture:

```bash
file "$IMOD_DIR/bin/3dmod"
file "$IMOD_DIR/bin/newstack"
file "$IMOD_DIR/bin/clip"
```

Expected result should contain:

```text
ELF 64-bit
ARM aarch64
```

Check missing dynamic libraries:

```bash
ldd "$IMOD_DIR/bin/3dmod" | grep 'not found' || true
ldd "$IMOD_DIR/bin/newstack" | grep 'not found' || true
ldd "$IMOD_DIR/bin/clip" | grep 'not found' || true
```

Expected:

- no `not found` output

Run basic command-line checks:

```bash
newstack -help
clip -help
header -help
if [ -n "${DISPLAY:-}" ]; then
  3dmod -h
else
  echo "Skipping 3dmod -h because DISPLAY is empty"
fi
```

GUI check:

```bash
if [ -n "${DISPLAY:-}" ]; then
  3dmod
else
  echo "Skipping 3dmod GUI launch because DISPLAY is empty"
fi
```

If `DISPLAY` is empty, skip GUI validation until a local desktop, X11 forwarding, or remote desktop session is available.

Record results:

```bash
cd ~/software/imod-aarch64
export IMOD_VERSION=4.11.25
cat >> "versions/$IMOD_VERSION/BUILD_NOTES.md" <<'EOF'

## CPU Smoke Test

- `file` confirmed native ARM aarch64: TODO
- `ldd` missing libraries: TODO
- `newstack -help`: TODO
- `clip -help`: TODO
- `header -help`: TODO
- `3dmod -h`: TODO
- GUI launch: TODO

EOF

git add "versions/$IMOD_VERSION/BUILD_NOTES.md"
git commit -m "test(cpu): record smoke test results"
git push
```

## 10. Handling Local Source Changes

Do not start by editing source. First run the clean CPU-only baseline build for the selected version from Sections 7-9. That baseline determines which of these two outcomes is true:

- If the selected IMOD version builds and passes smoke tests without source changes, record that result in `versions/<version>/BUILD_NOTES.md`, tag a local aarch64 release, and keep the source tree as the exact imported upstream version for that branch.
- If configure, compile, link, install, or smoke tests fail for architecture-specific reasons, start a source-level aarch64 porting branch and modify only the smallest necessary source files.

Likely source-level change areas:

- `setup2` and `machines/rhlinux` for architecture detection, library paths, compiler flags, and CUDA assumptions.
- `IMOD/` C/C++/Fortran files only when the failure points to actual source incompatibility.
- `scripts/` or repo-local helper scripts when the issue is reproducibility rather than upstream IMOD code.

Avoid source-level noise:

- Do not edit generated `Makefile` or `qconfigure` files as the durable fix.
- Do not commit `*.o`, `*.a`, `*.so`, install products, or full build logs.
- Do not mix dependency installation notes, source changes, and smoke-test results in one commit.
- Do not make speculative portability edits before a concrete failure identifies the need.

Before editing IMOD source, create a branch:

```bash
cd ~/software/imod-aarch64
export IMOD_VERSION=4.11.25
export IMOD_VERSION_DIR="$HOME/software/imod-aarch64/versions/$IMOD_VERSION"
git checkout "aarch64/$IMOD_VERSION"
git pull --ff-only origin "aarch64/$IMOD_VERSION"
git checkout -b "fix/$IMOD_VERSION-aarch64-build"
```

For each failure, capture the minimal evidence before editing source:

```bash
mkdir -p "$IMOD_VERSION_DIR/build-logs/failures"
tail -n 160 "$IMOD_VERSION_DIR/build-logs/<relevant-log>" > "$IMOD_VERSION_DIR/build-logs/failures/<short-failure-name>.log"
git add "versions/$IMOD_VERSION/BUILD_NOTES.md" "versions/$IMOD_VERSION/build-logs/failures/<short-failure-name>.log"
git commit -m "docs(init): record <specific aarch64 failure>"
```

After each focused fix:

```bash
git status
git diff
git add "IMOD/<changed-files>"
git commit -m "fix(aarch64): <specific build issue>"
git push -u origin "fix/$IMOD_VERSION-aarch64-build"
```

After a fix, rerun the smallest command that failed first. For example:

```bash
source "$IMOD_VERSION_DIR/env-cpu.sh"
cd "$IMOD_SOURCE_DIR"
set +e
./setup -c gfortran -i "$IMOD_CPU_PREFIX" 2>&1 | tee "$IMOD_VERSION_DIR/build-logs/setup-cpu.log"
setup_exit=${PIPESTATUS[0]}
set -e
if [ "$setup_exit" -ne 0 ]; then exit "$setup_exit"; fi

set +e
make -j"$(nproc)" 2>&1 | tee "$IMOD_VERSION_DIR/build-logs/make-cpu.full.log"
make_exit=${PIPESTATUS[0]}
set -e
if [ "$make_exit" -ne 0 ]; then exit "$make_exit"; fi
```

When the build moves past the failure, record the result:

```bash
git add "versions/$IMOD_VERSION/BUILD_NOTES.md"
git commit -m "docs(init): record <specific aarch64 fix result>"
```

Merge strategy for source-fix branches:

- Default to a small PR per version/fix branch for review.
- Preserve signed local commits by rebasing the fix branch onto `origin/aarch64/<version>`, then fast-forward merging into the matching `aarch64/<version>` branch.
- If the fixed version is the primary supported version, fast-forward `main` to the updated `aarch64/<version>` branch after the version branch is green.
- Do not squash by default; a squash commit would collapse the per-fix signed history.

```bash
git fetch origin
git checkout "fix/$IMOD_VERSION-aarch64-build"
git rebase "origin/aarch64/$IMOD_VERSION"
git push --force-with-lease

gh pr create \
  --base "aarch64/$IMOD_VERSION" \
  --head "fix/$IMOD_VERSION-aarch64-build" \
  --title "fix(aarch64): <specific build issue>" \
  --body "Source-level aarch64 fix for IMOD $IMOD_VERSION."

# After review/approval, merge locally to preserve signed commits.
git checkout "aarch64/$IMOD_VERSION"
git pull --ff-only origin "aarch64/$IMOD_VERSION"
git merge --ff-only "fix/$IMOD_VERSION-aarch64-build"
git push origin "aarch64/$IMOD_VERSION"

# If this version is the primary supported line, update main as well.
git checkout main
git pull --ff-only origin main
git merge --ff-only "aarch64/$IMOD_VERSION"
git push origin main

git push origin --delete "fix/$IMOD_VERSION-aarch64-build"
git branch -d "fix/$IMOD_VERSION-aarch64-build"
```

Keep commits small. Good commit examples:

```text
fix(aarch64): handle HDF5 include detection on Ubuntu
fix(aarch64): avoid x86-only compiler flags on Linux
docs(init): document Qt5 setup for Ubuntu 24.04
```

Avoid committing:

```text
WIP
try stuff
massive build output
```

## 11. Later CUDA Build Plan

Only attempt CUDA after CPU build passes smoke tests.

Use a separate install prefix:

```bash
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
export IMOD_CUDA_PREFIX="$IMOD_VERSION_DIR/install/cuda"
export CUDA_DIR=/usr/local/cuda
```

Check CUDA:

```bash
command -v nvcc
nvcc --version
ls "$CUDA_DIR/lib64"
```

Create a branch:

```bash
cd "$IMOD_REPO"
git checkout "aarch64/$IMOD_VERSION"
git pull --ff-only origin "aarch64/$IMOD_VERSION"
git checkout -b "cuda/$IMOD_VERSION-aarch64-build"
```

CUDA work must start after the CPU baseline or CPU source-fix branch for the same `IMOD_VERSION` is green on `aarch64/<version>`. Because the source tree is tracked, CUDA work should branch from `aarch64/<version>` instead of regenerating a separate ignored source worktree.

Do not overwrite the working CPU install. CUDA installs go to `versions/<version>/install/cuda`.

Configure:

```bash
cd "$IMOD_SOURCE_DIR"
cuda_setup_log="$IMOD_VERSION_DIR/build-logs/setup-cuda.log"
set +e
./setup -c gfortran -i "$IMOD_CUDA_PREFIX" 2>&1 | tee "$cuda_setup_log"
cuda_setup_exit=${PIPESTATUS[0]}
set -e
if [ "$cuda_setup_exit" -ne 0 ]; then
  exit "$cuda_setup_exit"
fi
```

Build and install:

```bash
cuda_make_log="$IMOD_VERSION_DIR/build-logs/make-cuda.full.log"
set +e
make -j"$(nproc)" 2>&1 | tee "$cuda_make_log"
cuda_make_exit=${PIPESTATUS[0]}
set -e
if [ "$cuda_make_exit" -ne 0 ]; then
  exit "$cuda_make_exit"
fi

cuda_install_log="$IMOD_VERSION_DIR/build-logs/install-cuda.full.log"
set +e
make install 2>&1 | tee "$cuda_install_log"
cuda_install_exit=${PIPESTATUS[0]}
set -e
if [ "$cuda_install_exit" -ne 0 ]; then
  exit "$cuda_install_exit"
fi
```

CUDA success criteria:

- CUDA-linked IMOD programs build successfully.
- `ldd` resolves `libcudart` and `libcufft`.
- CPU-only install remains usable.
- CUDA changes are isolated in the `cuda/<version>-aarch64-build` branch.

## 12. Recommended Workflow Summary

Use this sequence for the first source-bearing 4.11.25 import and CPU build:

```bash
cd ~/software
mkdir -p imod-aarch64
cd imod-aarch64
set -euo pipefail

git init
mkdir -p scripts versions upstream-hg
mkdir -p versions/4.11.25/install/cpu versions/4.11.25/build-logs
touch scripts/.gitkeep

# Create INIT.md, README.md, and .gitignore.

# Authenticate GitHub CLI if needed and attach the existing remote.
gh auth status || gh auth login
git remote add origin git@github.com:ZhenHuangLab/imod-aarch64.git
gh repo set-default ZhenHuangLab/imod-aarch64

# Configure SSH commit signing before the first commit.
ssh-keygen -t ed25519 -C "zhen.victor.huang@gmail.com" -f ~/.ssh/id_ed25519_github_signing
gh ssh-key add ~/.ssh/id_ed25519_github_signing.pub --type signing --title "$(hostname)-imod-signing"
git config user.name "ZhenHuangLab"
git config user.email "zhen.victor.huang@gmail.com"
git config gpg.format ssh
git config user.signingkey ~/.ssh/id_ed25519_github_signing.pub
git config commit.gpgsign true
git config tag.gpgsign true

# Make and push the first signed commit.
git add README.md INIT.md .gitignore scripts
git commit -m "chore(repo): initialize build repo"
git log -1 --show-signature
git branch -M main
git push -u origin main

# Install dependencies.
sudo apt update
sudo apt install -y mercurial tcsh build-essential gfortran default-jdk \
  qtbase5-dev qtbase5-dev-tools libqt5opengl5-dev \
  libtiff-dev libjpeg-dev libfftw3-dev libhdf5-dev \
  libgl1-mesa-dev libglu1-mesa-dev \
  libx11-dev libxext-dev libxmu-dev libxi-dev libxt-dev

# Fetch, pin, and import upstream source into the tracked IMOD/ tree.
export IMOD_VERSION=4.11.25
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"

hg clone http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD upstream-hg/IMOD
cd "$IMOD_REPO/upstream-hg/IMOD"
hg tags | grep -E '4[.]11[.]25|4_11_25'
hg update -r <EXACT_VERSION_TAG>
export IMOD_HG_ID="$(hg id -i -r <EXACT_VERSION_TAG>)"

cd "$IMOD_REPO"
git checkout main
git pull --ff-only origin main
git checkout -b "upstream/$IMOD_VERSION"
rm -rf IMOD
mkdir -p IMOD "$IMOD_VERSION_DIR/install/cpu" "$IMOD_VERSION_DIR/build-logs"
cd "$IMOD_REPO/upstream-hg/IMOD"
hg archive "$IMOD_REPO/IMOD"

cd "$IMOD_REPO"
mkdir -p "$IMOD_VERSION_DIR/install/cpu" "$IMOD_VERSION_DIR/build-logs"
cat > "$IMOD_VERSION_DIR/BUILD_NOTES.md" <<EOF
# IMOD $IMOD_VERSION Build Notes

## Source

- Upstream: http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD
- Requested version: IMOD $IMOD_VERSION
- Mercurial tag used: <EXACT_VERSION_TAG>
- Mercurial node id: $IMOD_HG_ID
- Host: Ubuntu 24.04.4 LTS aarch64
- First target: CPU-only
- Source changes required: TODO

EOF

git add -f IMOD
git add "versions/$IMOD_VERSION/BUILD_NOTES.md"
git commit -m "source(imod): import IMOD $IMOD_VERSION upstream source"
git tag -m "source(imod): tag upstream IMOD $IMOD_VERSION import" "upstream/imod-$IMOD_VERSION"
git push -u origin "upstream/$IMOD_VERSION"
git push origin "refs/tags/upstream/imod-$IMOD_VERSION"

git checkout -b "aarch64/$IMOD_VERSION"
git push -u origin "aarch64/$IMOD_VERSION"

git checkout main
git merge --ff-only "aarch64/$IMOD_VERSION"
git push origin main
git checkout "aarch64/$IMOD_VERSION"

DEPS=(
  mercurial tcsh build-essential gfortran default-jdk
  qtbase5-dev qtbase5-dev-tools libqt5opengl5-dev
  libtiff-dev libjpeg-dev libfftw3-dev libhdf5-dev
  libgl1-mesa-dev libglu1-mesa-dev
  libx11-dev libxext-dev libxmu-dev libxi-dev libxt-dev
)
dpkg-query -W -f='${Package}\t${Version}\t${Architecture}\n' "${DEPS[@]}" \
  | tee "$IMOD_VERSION_DIR/build-logs/apt-packages.tsv"

cat >> "$IMOD_VERSION_DIR/BUILD_NOTES.md" <<'EOF'

## Dependency Snapshot

- Package versions: `build-logs/apt-packages.tsv`

EOF

git add "versions/$IMOD_VERSION/BUILD_NOTES.md" "versions/$IMOD_VERSION/build-logs/apt-packages.tsv"
git commit -m "build(cpu): record dependency versions"

# Build CPU-only.
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
export IMOD_CPU_PREFIX="$IMOD_VERSION_DIR/install/cpu"
export QTDIR=/usr
export QMAKESPEC=linux-g++
export FFTW3_DIR=/usr/lib/aarch64-linux-gnu
export TIFF4_DIR=/usr/lib/aarch64-linux-gnu
export HDF5_DIR=/usr

cat > "$IMOD_VERSION_DIR/env-cpu.sh" <<EOF
export IMOD_VERSION="$IMOD_VERSION"
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="\$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="\$IMOD_REPO/versions/\$IMOD_VERSION"
export IMOD_CPU_PREFIX="\$IMOD_VERSION_DIR/install/cpu"
export IMOD_DIR="\$IMOD_VERSION_DIR/install/cpu"
export QTDIR="$QTDIR"
export QMAKESPEC="$QMAKESPEC"
export FFTW3_DIR="$FFTW3_DIR"
export TIFF4_DIR="$TIFF4_DIR"
export HDF5_DIR="$HDF5_DIR"
export PATH="\$IMOD_DIR/bin:\$PATH"
export LD_LIBRARY_PATH="\$IMOD_DIR/lib:\${LD_LIBRARY_PATH:-}"
if [ -f "\$IMOD_DIR/IMOD-linux.sh" ]; then
  source "\$IMOD_DIR/IMOD-linux.sh"
fi
EOF
source "$IMOD_VERSION_DIR/env-cpu.sh"

cd "$IMOD_SOURCE_DIR"
set +e
./setup -c gfortran -i "$IMOD_CPU_PREFIX" 2>&1 | tee "$IMOD_VERSION_DIR/build-logs/setup-cpu.log"
setup_exit=${PIPESTATUS[0]}
set -e
if [ "$setup_exit" -ne 0 ]; then exit "$setup_exit"; fi

set +e
make -j"$(nproc)" 2>&1 | tee "$IMOD_VERSION_DIR/build-logs/make-cpu.full.log"
make_exit=${PIPESTATUS[0]}
set -e
if [ "$make_exit" -ne 0 ]; then exit "$make_exit"; fi

set +e
make install 2>&1 | tee "$IMOD_VERSION_DIR/build-logs/install-cpu.full.log"
install_exit=${PIPESTATUS[0]}
set -e
if [ "$install_exit" -ne 0 ]; then exit "$install_exit"; fi

# Test.
file "$IMOD_DIR/bin/3dmod"
newstack -help
clip -help
if [ -n "${DISPLAY:-}" ]; then
  3dmod -h
else
  echo "Skipping 3dmod -h because DISPLAY is empty"
fi
```

## 13. Stop Conditions

Stop and record the exact error in `versions/<version>/BUILD_NOTES.md` if any of these happen:

- No exact tag for the requested IMOD version can be found in the official Mercurial repo.
- `setup` does not recognize the host as `arm64 running Linux`.
- `setup` injects x86-only compiler flags such as `-m64` on aarch64.
- Qt5 detection fails after `qtbase5-dev` and `qtbase5-dev-tools` are installed.
- HDF5/FFTW/TIFF paths cannot be resolved with Ubuntu arm64 package paths.
- Build fails in a way that appears architecture-specific.

For each stop, collect:

```bash
uname -a
lsb_release -a
qmake -query
gcc --version
gfortran --version
hg summary
tail -n 120 "versions/<version>/build-logs/<relevant-log>"
```

Then commit the notes:

```bash
git add "versions/<version>/BUILD_NOTES.md" "versions/<version>/build-logs/<relevant-log>"
git commit -m "docs(init): record <specific aarch64 failure>"
git push
```

Only commit short logs or curated logs. Keep very large logs ignored as `*.full.log`.
