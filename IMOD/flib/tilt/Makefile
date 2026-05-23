# Makefile for tilt program
#
# configure supplies TILTOBJS which specifies fortran subroutines or 
# ancient hand-optimized assembly routines for PC
# hand optimized routines were prepared by compiling g77 -S -c -O1, then
# knocking out appropriate mode change code
#
#  $Id$
#

include ../configure

O        = $(OBJEXT)
PROG = tilt$(BINEXT)

TILTOBJS = bpsumlocal.$(O) bpsumnox.$(O) bpsumxtilt.$(O) projsumlocal.$(O) \
set_projection_rays.$(O) #frameutil.$(O)

all: $(PROG)

$(PROG): tilt.$(O)  $(TILTOBJS) $(TILTGPUOBJ)
	$(CUDALINK) -o tilt tilt.$(O) $(TILTOBJS) $(TILTGPUOBJ) $(CUDA_LDFLAGS) $(LIMOD) \
	$(LIIMOD) $(LIMOD) $(LIFFT) $(LCFSHR) $(LIMXML) $(TIFFLIBS) $(CLIBS) $(CUDALIBS)

set_projection_rays.$(O) : ../subrs/hvem/set_projection_rays.f90
	$(FC) $(TILTFFLAGS) -c -o set_projection_rays.$(O) \
	../subrs/hvem/set_projection_rays.f90 

projsumlocal.$(O) : projsumlocal.f90
	$(FC) $(TILTFFLAGS) -c -o projsumlocal.$(O) projsumlocal.f90
bpsumlocal.$(O) : bpsumlocal.f90
	$(FC) $(TILTFFLAGS) -c -o bpsumlocal.$(O) bpsumlocal.f90
bpsumnox.$(O) : bpsumnox.f90
	$(FC) $(TILTFFLAGS) -c -o bpsumnox.$(O) bpsumnox.f90
bpsumxtilt.$(O) : bpsumxtilt.f90
	$(FC) $(TILTFFLAGS) -c -o bpsumxtilt.$(O) bpsumxtilt.f90

gpubp.$(O): gpubp.cu gpubp.h
	$(NVCC_TOOL_ENV) nvcc -c $(NVCC_FLAGS)  gpubp.cu

frameutil.$(O) : ../../mrc/frameutil.cpp ../../mrc/frameutil.h
	$(CXX) $(CXXFLAGS) -c -o frameutil.$(O) ../../mrc/frameutil.cpp

tilt.$(O): tilt.cpp tilt.h gpubp.h

install: $(PROG)
	$(STRIP) $(PROG)
	$(CP) $(PROG) $(INSTBINDIR)

clean:
	-\rm -f *.$(OBJEXT) *.exe *.mod *.linkinfo \#* *~ tilt

