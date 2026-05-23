include ../../configure

FFLAGS = $(LIBFFLAGS)

O        = $(OBJEXT)
OBJS = obj_manager.$(O) read_mod.$(O) readw_or_imod.$(O) repack_mod.$(O) \
 store_mod.$(O) write_wmod.$(O) iobjfromcont.$(O) scale_model.$(O)


all: $(OBJS)

clean:
	$(FIND) . -type f -name "*.$(OBJEXT)" -exec rm "{}" \;
	$(FIND) . -type f -name "*~" -exec rm "{}" \;
	$(FIND) . -type f -name "#*#" -exec rm "{}" \;

$(OBJS):	$(INCDIR)/fortmodel.$(O)
$(INCDIR)/fortmodel.$(O): $(INCDIR)/fortmodel.f90
	cd $(INCDIR) ; $(FC) $(FFLAGS) -c -o fortmodel.$(O) fortmodel.f90
