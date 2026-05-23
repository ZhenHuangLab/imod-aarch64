all: copy-tomo-coms generic-test-gui flatten-volume-test-gui flatten-volume-test-gui-std nad-test-gui-std serial-sections-test-gui-std serial-sections-montage-test-gui-std join-test-gui-std peet-test-gui single-test-gui single-fidless-test-gui single-simple-align-test-gui single-zero-bead-test-gui single-test-gui-raptor dual-test-gui dual-test-gui-std dual-test-gui-std-hdf dual-patch-tracking-test-gui single-montage-test-gui single-montage-test-gui-std single-montage-patch-tracking-test-gui dual-montage-patch-tracking-test-gui dual-montage-test-gui

temp: 

allplusgpu: gpu-test-gui all


all-tests: generic-tests gpu-tests flatten-tests serial-sections-tests nad-tests join-tests single-tests dual-tests single-montage-tests dual-montage-tests batch-tests


generic-tests: generic-test-gui generic

gpu-tests: gpu-test-gui gpu

flatten-tests: flatten-volume-test-gui flatten-volume-test-gui-std flatten-volume

serial-sections-tests: serial-sections-test-gui serial-sections-test-gui-std serial-sections-test-gui-std-hdf serial-sections serial-sections-montage-test-gui serial-sections-montage-test-gui-std serial-sections-montage-test-gui-std-hdf serial-sections-montage serial-sections-montage-std serial-sections-montage-std-hdf

nad-tests: nad-test-gui nad-test-gui-std nad-test-gui-std-hdf nad

join-tests: join-test-gui join-test-gui-std join

single-tests: single-test-gui single single-test-gui-plugin single-plugin single-test-gui-raptor single-fidless-test-gui single-fidless single-simple-align-test-gui single-simple-align single-patch-tracking-test-gui single-patch-tracking single-zero-bead-test-gui single-zero-bead

dual-tests: dual-test-gui dual-test-gui-std dual-test-gui-std-hdf dual-patch-tracking-test-gui dual dual-test-gui-plugin dual-plugin

single-montage-tests: single-montage-test-gui single-montage-test-gui-std single-montage

dual-montage-tests: dual-montage dual-montage-patch-tracking-test-gui dual-montage-patch-tracking

batch-tests: batch-test-gui batch



copy-tomo-coms: dummy
	$(IMOD_UITEST_SCRIPT)/uitest copy-tomo-coms
batch-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest batch-test-gui
batch: dummy
	$(IMOD_UITEST_SCRIPT)/uitest batch
single-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-test-gui
single-old-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-old-test-gui	
single: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single
single-test-gui-plugin: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-test-gui-plugin
single-plugin: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-plugin
dual-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-test-gui
dual-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-test-gui-std
dual-test-gui-std-hdf: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-test-gui-std-hdf
dual-patch-tracking-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-patch-tracking-test-gui
dual: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual
single-test-gui-raptor: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-test-gui-raptor
dual-test-gui-plugin: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-test-gui-plugin
dual-plugin: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-plugin
single-fidless-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-fidless-test-gui
single-fidless: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-fidless
single-simple-align-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-simple-align-test-gui
single-simple-align: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-simple-align
single-patch-tracking-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-patch-tracking-test-gui
single-patch-tracking: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-patch-tracking
single-zero-bead-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-zero-bead-test-gui
single-zero-bead: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-zero-bead
single-montage-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-montage-test-gui
single-montage-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-montage-test-gui-std
single-montage: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-montage
dual-montage-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-montage-test-gui
single-montage-patch-tracking-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest single-montage-patch-tracking-test-gui
dual-montage: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-montage
dual-montage-patch-tracking-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-montage-patch-tracking-test-gui
dual-montage-patch-tracking: dummy
	$(IMOD_UITEST_SCRIPT)/uitest dual-montage-patch-tracking
join-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest join-test-gui
join-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest join-test-gui-std
join: dummy
	$(IMOD_UITEST_SCRIPT)/uitest join
serial-sections-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-test-gui
serial-sections-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-test-gui-std
serial-sections-test-gui-std-hdf: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-test-gui-std-hdf
serial-sections: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections
serial-sections-montage-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage-test-gui
serial-sections-montage-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage-test-gui-std
serial-sections-montage-test-gui-std-hdf: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage-test-gui-std-hdf
serial-sections-montage: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage
serial-sections-montage-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage-std
serial-sections-montage-std-hdf: dummy
	$(IMOD_UITEST_SCRIPT)/uitest serial-sections-montage-std-hdf
nad-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest nad-test-gui
nad-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest nad-test-gui-std
nad-test-gui-std-hdf: dummy
	$(IMOD_UITEST_SCRIPT)/uitest nad-test-gui-std-hdf
nad: dummy
	$(IMOD_UITEST_SCRIPT)/uitest nad
peet-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest peet-test-gui
peet: dummy
	$(IMOD_UITEST_SCRIPT)/uitest peet
generic-test-gui: dummy
	 $(IMOD_UITEST_SCRIPT)/uitest generic-test-gui
generic: dummy
	 $(IMOD_UITEST_SCRIPT)/uitest generic
flatten-volume-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest flatten-volume-test-gui
flatten-volume-test-gui-std: dummy
	$(IMOD_UITEST_SCRIPT)/uitest flatten-volume-test-gui-std
flatten-volume: dummy
	$(IMOD_UITEST_SCRIPT)/uitest flatten-volume
gpu-test-gui: dummy
	$(IMOD_UITEST_SCRIPT)/uitest gpu-test-gui
gpu: dummy
	$(IMOD_UITEST_SCRIPT)/uitest gpu
dummy:

