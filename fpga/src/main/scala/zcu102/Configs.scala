// See LICENSE for license details.
package chipyard.fpga.zcu102

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{DTSModel, DTSTimebase}
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.uart._

import testchipip.{SerialTLKey}

import chipyard.{BuildSystem}

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    //UARTParams(address = 0x10013000))
    UARTParams(address = 0x10013000),
    UARTParams(address = 0x10014000))
  case DTSTimebase => BigInt(32768)
  case JtagDTMKey => new JtagDTMConfig (
    idcodeVersion = 2,
    idcodePartNum = 0x000,
    idcodeManufId = 0x489,
    debugIdleCycles = 5)
  case SerialTLKey => None // remove serialized tl port
})
// DOC include start: AbstractZCU102 and Rocket
class WithZCU102Tweaks extends Config(
  new chipyard.WithAxRAM ++
  new WithZCU102AxRAMHarnessBinder ++
  new WithZCU102JTAGHarnessBinder ++
  new WithZCU102UARTHarnessBinder ++
  new WithZCU102ResetHarnessBinder ++
  new WithZCU102DDRHarnessBinder ++
  new WithDebugResetPassthrough ++
  new WithDefaultPeripherals ++
  new freechips.rocketchip.subsystem.WithNBreakpoints(2))

class TinyRocketZCU102Config extends Config(
  new WithZCU102Tweaks ++
  new chipyard.TinyRocketConfig)

class RocketZCU102Config extends Config(
  new WithZCU102Tweaks ++
  new WithEdgeDataBits(128) ++
  new freechips.rocketchip.subsystem.WithExtMemSize((1<<29) * 1L) ++ // use 512MB external memory
  new chipyard.RocketConfig)

class QuadRocketZCU102Config extends Config(
  new WithZCU102Tweaks ++

  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 4, capacityKB = 128) ++
  new freechips.rocketchip.subsystem.WithCacheBlockBytes(32) ++
  new freechips.rocketchip.subsystem.WithL1ICacheSets(256) ++
  new freechips.rocketchip.subsystem.WithL1ICacheWays(4) ++
  new freechips.rocketchip.subsystem.WithL1DCacheSets(128) ++
  new freechips.rocketchip.subsystem.WithL1DCacheWays(8) ++

  new WithEdgeDataBits(128) ++
  new freechips.rocketchip.subsystem.WithExtMemSize((1<<29) * 1L) ++ // use 512MB external memory
  new freechips.rocketchip.subsystem.WithNBigCores(4) ++

  //new chipyard.config.WithMemoryBusFrequency(32.5) ++
  //new chipyard.config.WithPeripheryBusFrequency(32.5) ++

  new chipyard.config.AbstractConfig)

class TripleRocketZCU102WithSingleMulConfig extends Config(
  new WithZCU102Tweaks ++

  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 4, capacityKB = 128) ++
  new freechips.rocketchip.subsystem.WithCacheBlockBytes(32) ++
  new freechips.rocketchip.subsystem.WithL1ICacheSets(256) ++
  new freechips.rocketchip.subsystem.WithL1ICacheWays(4) ++
  new freechips.rocketchip.subsystem.WithL1DCacheSets(128) ++
  new freechips.rocketchip.subsystem.WithL1DCacheWays(8) ++

  new WithEdgeDataBits(128) ++
  new freechips.rocketchip.subsystem.WithExtMemSize((1<<29) * 1L) ++ // use 512MB external memory
  new freechips.rocketchip.subsystem.WithNBigCores(3, mulUnroll=64) ++

  //new chipyard.config.WithMemoryBusFrequency(32.5) ++
  //new chipyard.config.WithPeripheryBusFrequency(32.5) ++

  new chipyard.config.AbstractConfig)

class QuadHeteroZCU102WithSingleMulConfig extends Config(
  new WithZCU102Tweaks ++

  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 4, capacityKB = 128) ++
  new freechips.rocketchip.subsystem.WithCacheBlockBytes(32) ++
  new freechips.rocketchip.subsystem.WithL1ICacheSets(256) ++
  new freechips.rocketchip.subsystem.WithL1ICacheWays(4) ++
  new freechips.rocketchip.subsystem.WithL1DCacheSets(128) ++
  new freechips.rocketchip.subsystem.WithL1DCacheWays(8) ++

  new WithEdgeDataBits(128) ++
  new freechips.rocketchip.subsystem.WithExtMemSize((1<<29) * 1L) ++ // use 512MB external memory
  new freechips.rocketchip.subsystem.WithNBigCores(2, mulUnroll=64) ++
  new freechips.rocketchip.subsystem.WithNBigCores(2) ++

  //new chipyard.config.WithMemoryBusFrequency(32.5) ++
  //new chipyard.config.WithPeripheryBusFrequency(32.5) ++

  new chipyard.config.AbstractConfig)
