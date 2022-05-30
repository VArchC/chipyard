
FPGA_SHELLS ?= ../fpga-shells
fpga_common_script_dir := $(FPGA_SHELLS)/$(FPGA_BRAND)/common/tcl

#########################################################################################
# vivado rules
#########################################################################################
# combine all sources into single .f
synth_list_f := $(long_name).tarball.vsrcs.f

BIT_FILE := obj/$(MODEL).bit
$(BIT_FILE): $(synth_list_f)
	vivado \
		-nojournal -mode batch \
		-source $(fpga_common_script_dir)/vivado.tcl \
		-tclargs \
			-top-module "$(MODEL)" \
			-F "$(synth_list_f)" \
			-ip-vivado-tcls "$(shell find -name '*.vivado.tcl')" \
			-board "$(BOARD)"

.PHONY: bitstream
bitstream: $(BIT_FILE)

.PHONY: debug-bitstream
debug-bitstream: obj/post_synth.dcp
	vivado \
		-nojournal -mode batch \
		-source run_impl_bitstream.tcl \
		-tclargs \
			obj/post_synth.dcp \
			$(BOARD) \
			debug_obj \
			$(fpga_common_script_dir)
