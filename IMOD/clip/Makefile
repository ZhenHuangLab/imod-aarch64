#####################
# Makefile for clip
#
# $Id$
#
include ../configure

O = $(OBJEXT)
OBJS = clip.$(O) processing.$(O) file_io.$(O)\
	correlation.$(O) fft.$(O) filter.$(O) CorrectDefects.$(O) threshminsize.$(O)

PROG = clip$(BINEXT)

all : $(PROG)

# DNM 10/24/04: removed F77LIBS now that libifft is C
$(PROG) : $(OBJS)
	$(CXX) -o clip $(OBJS) $(LDFLAGS)  \
	$(LIIMOD) $(LCFSHR) $(LIMXML) $(LIFFT) $(TIFFLIBS) $(LIBS)

processing.$(O) : processing.cpp
	$(CXX) $(CXXFLAGS) -c -o processing.$(O) $(OPENMP) processing.cpp

install : $(PROG)
	$(STRIP) $(PROG)
	$(CP) $(PROG) $(INSTBINDIR)

clean : 
	$(FIND) . -type f -name "*.$(OBJEXT)" -exec rm "{}" \;
	$(FIND) . -name "*~" -exec rm "{}" \;
	$(FIND) . -type f -name "core*" -exec rm "{}" \;
	$(FIND) . -type f -name "clip" -exec rm "{}" \;
	$(FIND) . -type f -name "clip.exe" -exec rm "{}" \;

$(OBJS) : clip.h ../include/CorrectDefects.h
clip.$(O) : ../.version
