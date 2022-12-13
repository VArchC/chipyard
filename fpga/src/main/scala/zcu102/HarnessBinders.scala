package chipyard.fpga.zcu102

import chisel3._

import freechips.rocketchip.devices.debug._
import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4.{AXI4Bundle}

import sifive.blocks.devices.uart._
import sifive.blocks.devices.jtag._
import sifive.blocks.devices.pinctrl._

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}

import chipyard.harness.{ComposeHarnessBinder, OverrideHarnessBinder}
import chipyard.iobinders.JTAGChipIO

import testchipip.{ClockedAndResetIO}

class WithZCU102ResetHarnessBinder extends ComposeHarnessBinder({
  (system: HasPeripheryDebugModuleImp, th: ZCU102FPGATestHarness, ports: Seq[Bool]) => {
    require(ports.size == 2)

    withClockAndReset(th.clock_32MHz, th.reset_n) {
      // Debug module reset
      th.dut_ndreset := ports(0)

      // JTAG reset
      ports(1) := PowerOnResetFPGAOnly(th.clock_32MHz)
    }
  }
})

class WithZCU102JTAGHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryDebug, th: ZCU102FPGATestHarness, ports: Seq[Data]) => {
    ports.map {
      case j: JTAGChipIO =>
        withClockAndReset(th.buildtopClock, th.hReset) {
          val jtag_wire = Wire(new JTAGIO)
          jtag_wire.TDO.data := j.TDO
          jtag_wire.TDO.driven := true.B
          j.TCK := jtag_wire.TCK
          j.TMS := jtag_wire.TMS
          j.TDI := jtag_wire.TDI

          val io_jtag = Wire(new JTAGPins(() => new BasePin(), false)).suggestName("jtag")

          JTAGPinsFromPort(io_jtag, jtag_wire)

          io_jtag.TCK.i.ival := IBUFG(IOBUF(th.PMOD0_1).asClock).asBool

          IOBUF(th.PMOD0_5, io_jtag.TMS)
          PULLUP(th.PMOD0_5)

          IOBUF(th.PMOD0_0, io_jtag.TDI)
          PULLUP(th.PMOD0_0)

          IOBUF(th.PMOD0_3, io_jtag.TDO)

          //// mimic putting a pullup on this line (part of reset vote)
          //th.SRST_n := IOBUF(th.jd_6)
          //PULLUP(th.jd_6)

          // ignore the po input
          io_jtag.TCK.i.po.map(_ := DontCare)
          io_jtag.TDI.i.po.map(_ := DontCare)
          io_jtag.TMS.i.po.map(_ := DontCare)
          io_jtag.TDO.i.po.map(_ := DontCare)
        }
    }
  }
})

class WithZCU102UARTHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: ZCU102FPGATestHarness, ports: Seq[UARTPortIO]) => {
    withClockAndReset(th.clock_32MHz, th.reset_n) {
      IOBUF(th.UART2_RXD_I_FPGA_TXD,  ports.head.txd)
      ports.head.rxd := IOBUF(th.UART2_TXD_O_FPGA_RXD)
    }
  }
})

class WithZCU102DDRHarnessBinder extends OverrideHarnessBinder({
  (system: CanHaveMasterAXI4MemPort, th: ZCU102FPGATestHarness, ports: Seq[ClockedAndResetIO[AXI4Bundle]]) => {
    require(ports.size == 1)

    th.ip_ddr_clkconv.io.s_axi_aclk     :=  ports(0).clock
    th.ip_ddr_clkconv.io.s_axi_aresetn  :=  ~(ports(0).reset.asBool)
    th.ip_ddr_clkconv.io.s_axi_awid     :=  ports(0).bits.aw.bits.id
    th.ip_ddr_clkconv.io.s_axi_awaddr   :=  ports(0).bits.aw.bits.addr(28,0)
    th.ip_ddr_clkconv.io.s_axi_awlen    :=  ports(0).bits.aw.bits.len
    th.ip_ddr_clkconv.io.s_axi_awsize   :=  ports(0).bits.aw.bits.size
    th.ip_ddr_clkconv.io.s_axi_awburst  :=  ports(0).bits.aw.bits.burst
    th.ip_ddr_clkconv.io.s_axi_awlock   :=  ports(0).bits.aw.bits.lock
    th.ip_ddr_clkconv.io.s_axi_awcache  :=  ports(0).bits.aw.bits.cache
    th.ip_ddr_clkconv.io.s_axi_awprot   :=  ports(0).bits.aw.bits.prot
    th.ip_ddr_clkconv.io.s_axi_awqos    :=  ports(0).bits.aw.bits.qos
    th.ip_ddr_clkconv.io.s_axi_awvalid  :=  ports(0).bits.aw.valid
    ports(0).bits.aw.ready              :=  th.ip_ddr_clkconv.io.s_axi_awready
    th.ip_ddr_clkconv.io.s_axi_wdata    :=  ports(0).bits.w.bits.data
    th.ip_ddr_clkconv.io.s_axi_wstrb    :=  ports(0).bits.w.bits.strb
    th.ip_ddr_clkconv.io.s_axi_wlast    :=  ports(0).bits.w.bits.last
    th.ip_ddr_clkconv.io.s_axi_wvalid   :=  ports(0).bits.w.valid
    ports(0).bits.w.ready               :=  th.ip_ddr_clkconv.io.s_axi_wready
    th.ip_ddr_clkconv.io.s_axi_bready   :=  ports(0).bits.b.ready
    ports(0).bits.b.bits.id             :=  th.ip_ddr_clkconv.io.s_axi_bid
    ports(0).bits.b.bits.resp           :=  th.ip_ddr_clkconv.io.s_axi_bresp
    ports(0).bits.b.valid               :=  th.ip_ddr_clkconv.io.s_axi_bvalid
    th.ip_ddr_clkconv.io.s_axi_arid     :=  ports(0).bits.ar.bits.id
    th.ip_ddr_clkconv.io.s_axi_araddr   :=  ports(0).bits.ar.bits.addr(28,0)
    th.ip_ddr_clkconv.io.s_axi_arlen    :=  ports(0).bits.ar.bits.len
    th.ip_ddr_clkconv.io.s_axi_arsize   :=  ports(0).bits.ar.bits.size
    th.ip_ddr_clkconv.io.s_axi_arburst  :=  ports(0).bits.ar.bits.burst
    th.ip_ddr_clkconv.io.s_axi_arlock   :=  ports(0).bits.ar.bits.lock
    th.ip_ddr_clkconv.io.s_axi_arcache  :=  ports(0).bits.ar.bits.cache
    th.ip_ddr_clkconv.io.s_axi_arprot   :=  ports(0).bits.ar.bits.prot
    th.ip_ddr_clkconv.io.s_axi_arqos    :=  ports(0).bits.ar.bits.qos
    th.ip_ddr_clkconv.io.s_axi_arvalid  :=  ports(0).bits.ar.valid
    ports(0).bits.ar.ready              :=  th.ip_ddr_clkconv.io.s_axi_arready
    th.ip_ddr_clkconv.io.s_axi_rready   :=  ports(0).bits.r.ready
    ports(0).bits.r.bits.id             :=  th.ip_ddr_clkconv.io.s_axi_rid
    ports(0).bits.r.bits.data           :=  th.ip_ddr_clkconv.io.s_axi_rdata
    ports(0).bits.r.bits.resp           :=  th.ip_ddr_clkconv.io.s_axi_rresp
    ports(0).bits.r.bits.last           :=  th.ip_ddr_clkconv.io.s_axi_rlast
    ports(0).bits.r.valid               :=  th.ip_ddr_clkconv.io.s_axi_rvalid

  }
})
