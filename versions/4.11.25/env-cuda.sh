export IMOD_VERSION="4.11.25"
export IMOD_REPO="$HOME/software/imod-aarch64"
export IMOD_SOURCE_DIR="$IMOD_REPO/IMOD"
export IMOD_VERSION_DIR="$IMOD_REPO/versions/$IMOD_VERSION"
export IMOD_CUDA_PREFIX="$IMOD_VERSION_DIR/install/cuda"
export IMOD_DIR="$IMOD_CUDA_PREFIX"

export IMOD_CONDA_ENV="$IMOD_REPO/.conda/imod-build-4.11.25"
export QTDIR="$IMOD_CONDA_ENV"
export QMAKESPEC="linux-g++"
export FFTW3_DIR="$IMOD_CONDA_ENV"
export TIFF4_DIR="$IMOD_CONDA_ENV"
export HDF5_DIR="$IMOD_CONDA_ENV"
export JAVA_HOME="$IMOD_CONDA_ENV"

if [ -d "$IMOD_REPO/.conda/cuda-11.8" ]; then
  default_cuda_dir="$IMOD_REPO/.conda/cuda-11.8"
else
  default_cuda_dir="/usr/local/cuda"
fi

export CUDA_DIR="${CUDA_DIR:-$default_cuda_dir}"
export IMOD_CUDA_ARCH_OPTS="${IMOD_CUDA_ARCH_OPTS:--gencode arch=compute_90,code=compute_90}"

if [ -z "${IMOD_CUDA_CCBIN:-}" ] && [ -x "$CUDA_DIR/bin/aarch64-conda-linux-gnu-g++" ]; then
  export IMOD_CUDA_CCBIN="$CUDA_DIR/bin/aarch64-conda-linux-gnu-g++"
fi

export PATH="$CUDA_DIR/bin:$IMOD_CONDA_ENV/bin:$IMOD_DIR/bin:$PATH"
export LD_LIBRARY_PATH="$CUDA_DIR/lib:$CUDA_DIR/lib64:$IMOD_CONDA_ENV/lib:$IMOD_DIR/lib:${LD_LIBRARY_PATH:-}"
export LIBRARY_PATH="$CUDA_DIR/lib:$CUDA_DIR/lib64:$IMOD_CONDA_ENV/lib:${LIBRARY_PATH:-}"
export CPATH="$IMOD_CONDA_ENV/include:${CPATH:-}"
export PKG_CONFIG_PATH="$IMOD_CONDA_ENV/lib/pkgconfig:$IMOD_CONDA_ENV/share/pkgconfig:${PKG_CONFIG_PATH:-}"

if [ -f "$IMOD_DIR/IMOD-linux.sh" ]; then
  source "$IMOD_DIR/IMOD-linux.sh"
fi
