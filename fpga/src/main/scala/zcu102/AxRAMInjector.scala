package chipyard.fpga.zcu102

import Chisel._
import chipyard.AxRAMTopIOImp
import chisel3.util.random.{LFSR}

class AxRAMInjector() extends Module {
  val io = IO(new Bundle {
    val axi = new Bundle {
      val arid = Input(UInt(4.W))
      val araddr = Input(UInt(29.W))
      val arvalid = Input(Bool())

      val rid = Input(UInt(4.W))
      val rdata_mem = Input(UInt(128.W))
      val rdata_cpu = Output(UInt(128.W))
      val rvalid = Input(Bool())
    }
    val axram = Flipped(new AxRAMTopIOImp())
  })

  val idMatch = Reg(UInt(4.W))
  val validMatch = RegInit(0.U(2.W))
  val rand = LFSR(64)
  val matches = RegInit(0.U(64.W))
  val injections = RegInit(0.U(64.W))
  val err_inj = Reg(UInt(256.W))

  io.axram.reg_m := matches
  io.axram.reg_i := injections

  when(validMatch === 0.U &&
       io.axi.arvalid &&
       io.axram.reg_a(io.axi.araddr(28,23))) { 
    matches := matches + 1.U
  }

  when(validMatch === 0.U &&
       io.axi.arvalid &&
       io.axram.reg_a(io.axi.araddr(28,23)) && 
       rand <= io.axram.reg_x) {
    idMatch := io.axi.arid
    injections := injections + 1.U
    validMatch := 1.U
    err_inj := (1.U << rand(7,0))
  }

  when(io.axi.rvalid &&
       io.axi.rid === idMatch &&
       validMatch =/= 0.U ) {
    io.axi.rdata_cpu := io.axi.rdata_mem ^ err_inj(127,0)
    validMatch := 2.U
    err_inj := err_inj >> 128
  } .otherwise {
    io.axi.rdata_cpu := io.axi.rdata_mem
  }

  when(validMatch === 2.U && (!io.axi.rvalid || io.axi.rid =/= idMatch)) {
    validMatch := 0.U
  }

}
