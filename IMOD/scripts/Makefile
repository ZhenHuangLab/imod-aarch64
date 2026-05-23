include ../configure

#  $Id$

SCRIPTS = matchshifts onepatchcorr patchcorr3d patchcrawl3d recombine selfalign\
 setupcoms setuplong splitmrc  imodpsview settmpdir \
 prochunks.csh queuechunk $(GENERATED_SCRIPTS) $(QUEUE_SCRIPTS)

all:

install:  $(CMDWRAPPERS)
	$(CP) $(SCRIPTS) $(INSTBINDIR)

cmdwrappers:
	./copywrappers $(INSTBINDIR) . $(SCRIPTS)
	$(CP) ../dist/fixCygPython.sh $(INSTBINDIR)


clean:
	$(FIND) . -type f -name "*~"   -exec rm "{}" \;

