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
- Source changes required: TODO

Note: the upstream revision name `IMOD_4-11-25` resolves on the remote repository to node `2cc6c43b0551bed0845bd4f7534d15addb3b5604`. After cloning with `hg clone -r IMOD_4-11-25`, the local narrow clone lists this node as `tip`; its local tag listing does not include `IMOD_4-11-25` because Mercurial tag metadata for a tag can live in a later changeset outside the cloned ancestor set. The source tree itself confirms version `4.11.25`.

## Dependency Snapshot

System `apt` installation could not be used from this non-interactive session because `sudo` requires a password. Build dependencies were therefore installed in a user-space conda environment:

```text
/home/zhenh/software/imod-aarch64/.conda/imod-build-4.11.25
```

Recorded package snapshots:

- Conda package versions: `build-logs/conda-packages.tsv`
- System compiler/runtime package versions: `build-logs/system-packages.tsv`

Key dependency choices:

- Qt: conda-forge `qt-main` 5.15.15
- Java: conda-forge `openjdk` 25.0.2
- Mercurial: conda-forge `mercurial` 7.1.2
- C/C++/Fortran compilers: system `gcc`, `g++`, and `gfortran`
- OpenGL/X11/HDF5/FFTW/TIFF/JPEG headers and libraries: conda-forge environment

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
