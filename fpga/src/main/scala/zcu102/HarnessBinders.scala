package chipyard.fpga.zcu102

import chisel3._

import freechips.rocketchip.devices.debug._
import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem._

import sifive.blocks.devices.uart._
import sifive.blocks.devices.jtag._
import sifive.blocks.devices.pinctrl._

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}

import chipyard.harness.{ComposeHarnessBinder, OverrideHarnessBinder}
import chipyard.iobinders.JTAGChipIO

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
      IOBUF(th.UART2_TXD_O_FPGA_RXD,  ports.head.txd)
      ports.head.rxd := IOBUF(th.UART2_RXD_I_FPGA_TXD)
    }
  }
})
