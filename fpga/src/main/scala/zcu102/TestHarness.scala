package chipyard.fpga.zcu102

import chisel3._

import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.config.{Parameters}

import sifive.fpgashells.shell.xilinx.zcu102shell.{ZCU102Shell}

import chipyard.{BuildTop, HasHarnessSignalReferences, HasTestHarnessFunctions}
import chipyard.harness.{ApplyHarnessBinders}
import chipyard.iobinders.{HasIOBinders}

class ZCU102FPGATestHarness(override implicit val p: Parameters) extends ZCU102Shell with HasHarnessSignalReferences {

  val lazyDut = LazyModule(p(BuildTop)(p)).suggestName("chiptop")

  // Convert harness resets from Bool to Reset type.
  val hReset = Wire(Reset())
  hReset := reset_n

  val dReset = Wire(AsyncReset())
  dReset := reset_core.asAsyncReset

  // default to 32MHz clock
  withClockAndReset(clock_32MHz, hReset) {
    val dut = Module(lazyDut.module)
  }

  val buildtopClock = clock_32MHz
  val buildtopReset = hReset
  val success = false.B

  val dutReset = dReset

  // must be after HasHarnessSignalReferences assignments
  lazyDut match { case d: HasTestHarnessFunctions =>
    d.harnessFunctions.foreach(_(this))
  }
  lazyDut match { case d: HasIOBinders =>
    ApplyHarnessBinders(this, d.lazySystem, d.portMap)
  }
}

