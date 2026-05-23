#!/usr/bin/env bash
set -euo pipefail

sudo apt update
sudo apt install -y \
  mercurial tcsh build-essential gfortran default-jdk \
  qtbase5-dev qtbase5-dev-tools libqt5opengl5-dev \
  libtiff-dev libjpeg-dev libfftw3-dev libhdf5-dev \
  libgl1-mesa-dev libglu1-mesa-dev \
  libx11-dev libxext-dev libxmu-dev libxi-dev libxt-dev
