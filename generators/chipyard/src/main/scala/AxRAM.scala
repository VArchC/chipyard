package chipyard

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule, IO}
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf
import chipyard.iobinders.{OverrideIOBinder}

case class AxRAMParams(
  address: BigInt = 0x8000)

case object AxRAMKey extends Field[Option[AxRAMParams]](None)

trait AxRAMTopIO extends Bundle {
  val reg_a = Output(UInt(64.W))
  val reg_x = Output(UInt(64.W))
  val reg_m = Input(UInt(64.W))
  val reg_i = Input(UInt(64.W))
}

class AxRAMTopIOImp extends AxRAMTopIO

trait AxRAMModule extends HasRegMap {
  val io: AxRAMTopIO

  implicit val p: Parameters
  def params: AxRAMParams

  val a = Reg(UInt(64.W))
  val x = Reg(UInt(64.W))

  io.reg_a := a
  io.reg_x := x

  regmap(
    0x00 -> Seq(
      RegField(64, a)),
    0x08 -> Seq(
      RegField(64, x)),
    0x10 -> Seq(
      RegField.r(64, io.reg_m)),
    0x18 -> Seq(
      RegField.r(64, io.reg_i)))
}

class AxRAMTL(params: AxRAMParams, beatBytes: Int)(implicit p: Parameters)
  extends TLRegisterRouter(
    params.address, "axram", Seq("varchc,axram"),
    beatBytes = beatBytes)(
      new TLRegBundle(params, _) with AxRAMTopIO)(
      new TLRegModule(params, _, _) with AxRAMModule)

trait CanHavePeripheryAxRAM { this: BaseSubsystem =>
  private val portName = "axram"

  val axram = p(AxRAMKey) match {
    case Some(params) => {
        val axram = LazyModule(new AxRAMTL(params, pbus.beatBytes)(p))
        pbus.toVariableWidthSlave(Some(portName)) { axram.node }
        Some(axram)
    }
    case None => None
  }
}

trait CanHavePeripheryAxRAMModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryAxRAM

  val axram = outer.axram match {
    case Some(axram) => {
      val io = IO(new AxRAMTopIOImp())
      io.reg_a := axram.module.io.reg_a
      io.reg_x := axram.module.io.reg_x
      axram.module.io.reg_m := io.reg_m
      axram.module.io.reg_i := io.reg_i
      Some(io)
    }
    case None => None
  }
}

class WithAxRAMIOPassthrough extends OverrideIOBinder({
  (system: CanHavePeripheryAxRAMModuleImp) => {
    val axram_port : AxRAMTopIO = system.axram match {
      case Some(axram) => {
        val io = IO(new AxRAMTopIOImp()).suggestName("axram")
        io.reg_a := axram.reg_a
        io.reg_x := axram.reg_x
        axram.reg_m := io.reg_m
        axram.reg_i := io.reg_i
        io
      }
    }
    (Seq(axram_port), Nil)
  }
})

class WithAxRAM() extends Config((site, here, up) => {
  case AxRAMKey => Some(AxRAMParams())
})
