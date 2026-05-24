# IMOD 4.11.25 Build Notes

## Source

- Upstream: http://bio3d.colorado.edu/imod/nightlyBuilds/IMOD
- Requested version: IMOD 4.11.25
- Mercurial revision requested from upstream: `IMOD_4-11-25`
- Mercurial node id: `2cc6c43b0551bed0845bd4f7534d15addb3b5604`
- Mercurial branch: `IMOD_4-11`
- Local imported source tree: `IMOD/`
- Source version checks:
  - `IMOD/.version`: `4.11.25`
  - `IMOD/Etomo/src/etomo/type/ImodVersion.java`: `4.11.25`
- Host: Ubuntu 24.04.4 LTS aarch64
- First target: CPU-only native aarch64 build
- Second target: CUDA-enabled native aarch64 build
- Source changes required: yes

Note: the upstream revision name `IMOD_4-11-25` resolves on the remote repository to node `2cc6c43b0551bed0845bd4f7534d15addb3b5604`. After cloning with `hg clone -r IMOD_4-11-25`, the local narrow clone lists this node as `tip`; its local tag listing does not include `IMOD_4-11-25` because Mercurial tag metadata for a tag can live in a later changeset outside the cloned ancestor set. The source tree itself confirms version `4.11.25`.

## Dependency Snapshot

System `apt` installation could not be used from this non-interactive session because `sudo` requires a password. Build dependencies were therefore installed in a user-space conda environment:

```text
/home/zhenh/software/imod-aarch64/.conda/imod-build-4.11.25
```

Recorded package snapshots:

- Conda package versions: `build-logs/conda-packages.tsv`
- System compiler/runtime package versions: `build-logs/system-packages.tsv`

Key dependency choices after the successful build:

- Qt: conda-forge `qt-main` 5.15.15
- Java: conda-forge `openjdk` 17.0.18
- Mercurial: conda-forge `mercurial` 7.1.2
- C/C++/Fortran compilers: system `gcc`, `g++`, and `gfortran`
- OpenGL/X11/HDF5/FFTW/TIFF/JPEG headers and libraries: conda-forge environment

OpenJDK was pinned back to 17 because IMOD 4.11.25's Etomo build uses `javac -source 1.7 -target 1.7`; newer OpenJDK 25 rejects that source/target level.

Selected conda package versions:

```text
fftw             3.3.11
hdf5             2.1.0
libglu           9.0.3
libglvnd         1.7.0
libjpeg-turbo    3.1.4.1
libtiff          4.7.1
mercurial        7.1.2
openjdk          17.0.18
qt-main          5.15.15
```

Selected system package versions:

```text
build-essential  12.10ubuntu1
gcc/g++/gfortran 4:13.2.0-7ubuntu1
make             4.3-4.1build2
tcsh             6.24.10-4build1
```

### CUDA dependency snapshot

System CUDA 13.0.88 is installed under `/usr/local/cuda`, but IMOD 4.11.25's
CUDA code still uses legacy texture references removed by CUDA 13. The
successful CUDA build therefore uses a user-space CUDA 11.8 toolchain:

```text
/home/zhenh/software/imod-aarch64/.conda/cuda-11.8
```

Recorded CUDA toolchain package versions:

- CUDA conda package versions: `build-logs/cuda-11.8-conda-packages.tsv`

Key CUDA dependency choices:

```text
cuda-nvcc       11.8.89
cuda-cudart     11.8.89
libcufft        10.9.0.58
gcc/g++         conda-forge 11.4.0 cross compiler wrappers
sysroot         sysroot_linux-aarch64 2.17
```

The conda GCC 11 wrapper and `sysroot_linux-aarch64=2.17` are used as the
`nvcc` host compiler/sysroot because CUDA 11.8's front-end does not parse the
host's Ubuntu 24.04 glibc 2.39 `_Float*` header declarations cleanly.

CUDA hardware/runtime observed during validation:

```text
GPU:     NVIDIA GB10
Driver:  580.142
Compute: 12.1
```

## Configure Attempts

### Clean baseline

- Command: `./setup -c gfortran -i "$IMOD_CPU_PREFIX"`
- Log: `build-logs/setup-cpu.log`
- Result: failed before Makefile generation completed.
- Failure:

```text
WARNING: no definition for aarch64__Linux__6.17.0-1018-nvidia__all
aarch64__Linux__6.17.0-1018-nvidia__all error
You need to specify a machine; the generic settings are not usable
```

Conclusion: upstream IMOD 4.11.25 does not recognize Linux/aarch64 in `setup2`. The next step is a minimal source-level porting patch for a Linux aarch64 target that reuses the existing `machines/rhlinux` settings without adding x86-only `-m64` flags.

### Successful CPU configure

- Command: `./setup -c gfortran -i "$IMOD_CPU_PREFIX"`
- Log: `build-logs/setup-cpu.log`
- Result: success.
- Target detected as:

```text
aarch64__Linux__6.17.0-1018-nvidia__all is 64-bit Arm Linux
```

The setup log still contains nonfatal `find: '/usr/lib64': No such file or directory` messages from legacy Linux library probing.

### CUDA 13 configure/build attempt

- Toolchain: system `/usr/local/cuda`, CUDA 13.0.88.
- Result: configure succeeded after allowing an explicit CUDA architecture,
  but build failed in `flib/tilt/gpubp.cu`.
- Failure cause: IMOD 4.11.25 uses CUDA legacy texture references such as
  `texture<float, ...>` and `tex2D(...)`; CUDA 13 has removed support for this
  API.
- Conclusion: a CUDA 13 build would require a larger source port to texture
  objects. For this native aarch64 release, the lower-risk path is CUDA 11.8.

### Successful CUDA 11.8 configure

- Command: `./setup -c gfortran -i "$IMOD_CUDA_PREFIX"`
- Environment: `source versions/4.11.25/env-cuda.sh`
- Log: `build-logs/setup-cuda.log`
- Result: success, `setup_exit=0`.

Generated CUDA settings in `IMOD/configure`:

```text
NVCC_FLAGS = -gencode arch=compute_90,code=compute_90 ... -ccbin /home/zhenh/software/imod-aarch64/.conda/cuda-11.8/bin/aarch64-conda-linux-gnu-g++
CUDALIBS = -L/home/zhenh/software/imod-aarch64/.conda/cuda-11.8/lib -lcudart -lcufft
TILTGPUOBJ = gpubp.$(OBJEXT)
CTFGPUOBJ = gpuctf.$(OBJEXT)
FRAMEGPUOBJ = gpuframe.$(OBJEXT)
```

CUDA 11.8 does not know Blackwell/GB10 `sm_121`, so this build emits
`compute_90` PTX. The NVIDIA 580.142 driver successfully JIT-compiled and ran
that PTX on the local GB10 GPU during `gputilttest`.

## Local Source Fixes

The successful native aarch64 build required these downstream source changes:

- `setup2`: recognize Linux `aarch64`/`arm64` and map it to a new `aarch64linux` setup target based on the existing `rhlinux` machine settings without x86 `-m64` flags.
- `setup2`: support conda-forge's Qt include layout under `$QTDIR/include/qt`.
- `machines/rhlinux`: add `/usr/lib/aarch64-linux-gnu` as an alternate system library path for Linux aarch64.
- `machines/rhlinux`: define `H5_USE_110_API` so IMOD 4.11.25 builds against newer conda-forge HDF5 2.x headers.
- `machines/rhlinux`: add `-fallow-argument-mismatch` for modern `gfortran` 10+.
- `librgctf/matrix.cpp`: restrict the x87 `fsincos` inline assembly path to x86 and use portable `sin`/`cos` on aarch64.
- `plugs/drawingtools/livewire/general.h`: move the `byte` typedef into the `Livewire` namespace to avoid ambiguity with `std::byte` in modern C++ headers.
- `setup`, `setup2`, and `manpages/convert`: use `/bin/tcsh` shebangs; Ubuntu's `/bin/csh` points to `bsd-csh`, while these scripts use tcsh-compatible behavior.
- `pysrc/`, `html/makeqhp`, `manpages/adocdefaults`, and `manpages/csvtohtml`: update legacy Python shebangs to Python 3.
- `machines/rhlinux`: allow `IMOD_CUDA_ARCH`, `IMOD_CUDA_ARCH_OPTS`, and
  `IMOD_CUDA_CCBIN` overrides; detect CUDA conda environments that use `lib`
  rather than `lib64`; add a CUDA 13 `sm_121` fallback for future experiments.
- `plugs/drawingtools/livewire/Colors.h` and `FilterBase.h`: explicitly use
  `Livewire::byte` so the drawingtools livewire plugin builds with modern
  C++ headers.
- `pysrc/gputilttest`: add a local uuencode decoder fallback because Python
  3.13 removed the standard-library `uu` module.

## Build and Install

### CPU build

- Command: `make -j"$(nproc)"`
- Full log: `build-logs/make-cpu.full.log` (ignored by Git)
- Result: success, `make_exit=0`.

### CPU install

- Command: `make install`
- Install prefix: `/home/zhenh/software/imod-aarch64/versions/4.11.25/install/cpu`
- Full log: `build-logs/install-cpu.full.log` (ignored by Git)
- Result: success, `install_exit=0`.

Nonfatal warnings observed during install:

- Python 3 `SyntaxWarning: invalid escape sequence` from legacy regular-expression strings in `pip.py` and `pysed.py`.
- `QStandardPaths: XDG_RUNTIME_DIR not set` while building Qt compressed help.

After installation, `make clean` was run in the source tree to remove in-tree build products. The installed CPU prefix is the retained runnable artifact.

### CUDA build

- Command: `make -j"$(nproc)"`
- Initial CUDA 13 full log: `build-logs/make-cuda.full.log` (ignored by Git)
- Successful CUDA 11.8 retry log: `build-logs/make-cuda.retry1.log`
- Result: success, `make_exit=0`.

### CUDA install

- Command: `make install`
- Install prefix: `/home/zhenh/software/imod-aarch64/versions/4.11.25/install/cuda`
- Full log: `build-logs/install-cuda.full.log` (ignored by Git)
- Result: success, `install_exit=0`.

The installed CUDA-enabled binaries include `tilt`, `ctfphaseflip`, and
`alignframes`; all resolve `libcudart.so.11.0` and `libcufft.so.10` from
`.conda/cuda-11.8/lib` when `env-cuda.sh` is sourced.

## Smoke Tests

- Log: `build-logs/smoke-cpu.log`
- Result: pass.

Summary from the smoke log:

```text
missing_libs=0
legacy_python_shebang_count=0
data_workflow_exit=0
smoke_result=PASS
```

Smoke coverage:

- `file` reports installed compiled programs such as `3dmod`, `newstack`, `clip`, `header`, and `raw2mrc` as `ELF 64-bit ... ARM aarch64`.
- `ldd` reports no missing dynamic libraries for representative CLI, Qt, and Etomo entry points.
- Help/usage checks were run for `header`, `newstack`, `clip`, `imodinfo`, `etomo`, and `3dmod`.
- A small generated 16x16 raw byte stack was converted with `raw2mrc`, inspected with `header`, copied with `newstack`, and summarized with `clip stats`.

During this run, `$DISPLAY` was set to `localhost:11.0`; `3dmod -h` printed usage successfully and exited with its normal help-style nonzero status.

### CUDA smoke tests

- Log: `build-logs/smoke-cuda.log`
- GPU validation log: `build-logs/gputilttest-cuda.validation.log`
- Result: pass.

Summary from the CUDA smoke log:

```text
missing_libs=0
tilt: CUDA libraries resolved
ctfphaseflip: CUDA libraries resolved
alignframes: CUDA libraries resolved
data_workflow_exit=0
gputilttest_exit=0
smoke_result=PASS
```

CUDA smoke coverage:

- `file` reports representative installed programs as `ELF 64-bit ... ARM aarch64`.
- `ldd` reports no missing dynamic libraries for representative CLI, Qt, and
  CUDA-linked entry points.
- `tilt`, `ctfphaseflip`, and `alignframes` resolve CUDA runtime/cuFFT from
  the user-local CUDA 11.8 conda environment.
- Help/usage checks were run for `header`, `newstack`, `tilt`,
  `ctfphaseflip`, `alignframes`, `gputilttest`, and `3dmod`.
- `3dmod -h` was checked with `QT_QPA_PLATFORM=offscreen` because the current
  shell had a stale `DISPLAY=localhost:11.0`.
- A small generated 16x16 raw byte stack was converted with `raw2mrc`,
  inspected with `header`, copied with `newstack`, and summarized with
  `clip stats`.
- `gputilttest 0.1 0` ran real GPU `tilt UseGPU` tests on NVIDIA GB10:

```text
All 14 runs gave reproducible results; this GPU seems reliable
gputilttest_exit=0
```
