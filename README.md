# IMOD Linux aarch64 Downstream Fork

This repository tracks a source-bearing downstream port of IMOD for Ubuntu Linux aarch64.

The tracked `IMOD/` tree contains the upstream source for the checked-out version branch plus any local aarch64 fixes.

The first milestone is complete: IMOD 4.11.25 builds and installs as a native
Linux aarch64 CPU-only build under:

```text
~/software/imod-aarch64/versions/4.11.25/install/cpu
```

Use it from a shell with:

```bash
source ~/software/imod-aarch64/versions/4.11.25/env-cpu.sh
3dmod -h
newstack -help
```

Build notes and smoke-test results are recorded in:

```text
versions/4.11.25/BUILD_NOTES.md
versions/4.11.25/build-logs/smoke-cpu.log
```

CUDA support and newer IMOD 5.2.x builds will be attempted only after the first CPU build is working.
