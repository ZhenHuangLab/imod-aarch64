# IMOD Linux aarch64 Downstream Fork

This repository is a source-bearing downstream fork of IMOD for Linux aarch64.
The tracked `IMOD/` tree contains the upstream source for the checked-out
version branch plus the local aarch64 fixes needed by this fork.

## Source Checkout

Clone the source fork with:

```bash
git clone https://github.com/ZhenHuangLab/imod-aarch64.git
cd imod-aarch64
```

The prebuilt install trees are not stored in Git. A source checkout does not
include `versions/*/install/cpu` or `versions/*/install/cuda`; those install
products are distributed as GitHub Release assets.

## Prebuilt Downloads

Download prebuilt Linux aarch64 packages from the
[GitHub Releases](https://github.com/ZhenHuangLab/imod-aarch64/releases) page.

Current IMOD 4.11.25 assets:

```text
CPU release tag:   imod-4.11.25-aarch64.1
CPU asset:         imod-4.11.25-linux-aarch64-cpu.tar.gz

CUDA release tag:  imod-4.11.25-aarch64-cuda.1
CUDA asset:        imod-4.11.25-linux-aarch64-cuda.tar.gz

Checksums:         SHA256SUMS
```

The release packages include the non-CUDA runtime libraries used by the tested
build, plus a bundled JRE for ETomo. CUDA runtime libraries are handled
separately, as described below.

Verify a downloaded package with:

```bash
sha256sum -c SHA256SUMS
```

## CPU Package

Extract and use the CPU package with:

```bash
tar -xzf imod-4.11.25-linux-aarch64-cpu.tar.gz
cd cpu
source ./IMOD-linux.sh
newstack -help
3dmod -h
```

After sourcing `IMOD-linux.sh`, `$IMOD_DIR` points at the extracted package,
`bin` is added to `$PATH`, and `lib` is added to `$LD_LIBRARY_PATH`.

## CUDA Package

Extract and use the CUDA package with:

```bash
tar -xzf imod-4.11.25-linux-aarch64-cuda.tar.gz
cd cuda
source ./IMOD-linux.sh
tilt -help
gputilttest 0.1 0
```

The CUDA package expects CUDA 11.8 runtime libraries and cuFFT to be available
on the host, for example through a CUDA 11.8 conda environment or another local
CUDA 11.8 installation. The host also needs a compatible NVIDIA driver. Set
`CUDA_DIR` before sourcing `IMOD-linux.sh` if the CUDA runtime is not in a
standard location:

```bash
export CUDA_DIR=/path/to/cuda-11.8
source ./IMOD-linux.sh
```

IMOD 4.11.25 still uses legacy CUDA texture reference APIs that CUDA 13 no
longer compiles directly, so the current CUDA binary is built against CUDA 11.8
runtime/cuFFT.

## Verification

The IMOD 4.11.25 Linux aarch64 builds completed these checks:

```text
CPU smoke test:       pass
CUDA smoke test:      pass
CUDA gputilttest:     pass
```

Detailed build notes and smoke-test summaries are kept in:

```text
versions/4.11.25/BUILD_NOTES.md
versions/4.11.25/build-logs/smoke-cpu.log
versions/4.11.25/build-logs/smoke-cuda.log
```

Newer IMOD versions can use the same versioned layout under `versions/`.
