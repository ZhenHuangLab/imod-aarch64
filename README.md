# IMOD Linux aarch64 Downstream Fork

This repository tracks a source-bearing downstream port of IMOD for Ubuntu Linux aarch64.

The tracked `IMOD/` tree contains the upstream source for the checked-out version branch plus any local aarch64 fixes.

The first two IMOD 4.11.25 milestones are complete:

```text
CPU-only:      ~/software/imod-aarch64/versions/4.11.25/install/cpu
CUDA-enabled:  ~/software/imod-aarch64/versions/4.11.25/install/cuda
```

Use the CPU build from a shell with:

```bash
source ~/software/imod-aarch64/versions/4.11.25/env-cpu.sh
3dmod -h
newstack -help
```

Use the CUDA build from a shell with:

```bash
source ~/software/imod-aarch64/versions/4.11.25/env-cuda.sh
tilt -help
gputilttest 0.1 0
```

The CUDA build uses a user-local CUDA 11.8 conda toolchain under
`.conda/cuda-11.8`, because IMOD 4.11.25 still uses CUDA texture references
that CUDA 13 no longer supports. The generated GPU code is PTX for
`compute_90`; on this host, NVIDIA driver 580.142 successfully JIT-runs it on
the NVIDIA GB10 GPU with compute capability 12.1.

Build notes and smoke-test results are recorded in:

```text
versions/4.11.25/BUILD_NOTES.md
versions/4.11.25/build-logs/smoke-cpu.log
versions/4.11.25/build-logs/smoke-cuda.log
```

Newer IMOD 5.2.x builds can use the same versioned layout under `versions/`.
