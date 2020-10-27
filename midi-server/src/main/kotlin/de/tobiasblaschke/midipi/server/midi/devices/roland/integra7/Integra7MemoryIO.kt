package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId




abstract class Integra7MemoryIO {
    internal abstract val deviceId: DeviceId
    internal abstract val address: Integra7Address
    internal abstract val size: UInt

    fun asDataRequest1(): RolandIntegra7MidiMessage {
        val payload = size.toByteArrayMsbFirst()
        return RolandIntegra7MidiMessage.IntegraSysExReadRequest(
            deviceId = deviceId,
            payload =
                    ubyteArrayOf(0x11u) +   // Command
                    address.bytes() +
                    payload +
                    checkSum(payload))
    }

    fun asDataSet1(payload: UByteArray): RolandIntegra7MidiMessage {
        return RolandIntegra7MidiMessage.IntegraSysExReadRequest(
            deviceId = deviceId,
            payload =
                    ubyteArrayOf(0x12u) +
                    address.bytes() +
                    payload +
                    checkSum(payload))
    }


    private fun checkSum(payload: UByteArray): UByte {
        val addrSum = ((address.lsb + address.mlsb + address.mmsb + address.msb) and 0xFFu).toUByte()
        val payloadSum = payload.reduce { a, b -> ((a + b) and 0xFFu).toUByte() }
        val totalSum = ((addrSum + payloadSum) and 0xFFu).toUByte()
        val reminder = (totalSum % 128u).toUByte()
        return if (reminder < 128u) (128u - reminder).toUByte() else (reminder - 128u).toUByte()
    }

    private fun UInt.toByteArrayMsbFirst(): UByteArray {
        val lsb = this.and(0xFFu).toUByte()
        val mlsb = (this / 0x100u).and(0xFFu).toUByte()
        val mmsb = (this / 0x10000u).and(0xFFu).toUByte()
        val msb = (this / 0x1000000u).toUByte()
        return ubyteArrayOf(msb, mmsb, mlsb, lsb)
    }
}


// ----------------------------------------------------

class AddressRequestBuilder(deviceId: DeviceId) {
    val tone1 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000))
    val test = TestDummy(deviceId, Integra7Address(0x19000000))
}

/* internal abstract */ data class ToneAddressRequestBuilder(private val deviceId: DeviceId, private val address: Integra7Address) {
    val pcmSynthTone = PcmSynthToneBuilder(deviceId, address.offsetBy(0x000000))
}

/* internal */ data class PcmSynthToneBuilder(private val deviceId: DeviceId, private val address: Integra7Address) {
    val common = PcmSynthToneCommonBuilder(deviceId, address.offsetBy(0x000000))
}

/* internal */ data class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x50u

    val name = PcmSynthToneCommonName(deviceId, address.offsetBy(0x000000))
}

/* internal */ data class PcmSynthToneCommonName(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x0Cu

}

data class TestDummy(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x200000u

    // F0 41 10 00 00 64 11 - 19 00 00 00 - 00 20 00 00 - 7F F7
}

// ----------------------------------------------------


//sealed class Integra7SysexResponse: MBMidiMessage {
//    companion object {
//        private const val UNIVERSAL_RESPONSE: UByte = 0x7Eu
//    }
//
//    data class IdentityReply(
//        val manufacturerId: UByte,
//        val deviceId: RolandIntegra7.DeviceId,
//        val deviceFamily: UShort,
//        val deviceFamilyNumber: UShort,
//        val softwareRev: UInt
//    ): Integra7SysexResponse() {
//        companion object {
//            fun read(bytes: UByteArray): IdentityReply? {
//                val deviceId = RolandIntegra7.DeviceId.read(bytes[1])
//                return if (
//                    bytes.size == 14 &&
//                    bytes[0] == UNIVERSAL_RESPONSE && // 0x7F
//                    deviceId != null &&
//                    bytes[2] == GENERAL_INFORMATION && // 0x
//                    bytes[3] == 0x02U.toUByte()
//                ) {
//                    IdentityReply(
//                        manufacturerId = bytes[4],
//                        deviceId = deviceId,
//                        deviceFamily = (bytes[5] * 0x100u + bytes[6]).toUShort(),
//                        deviceFamilyNumber = (bytes[7] * 0x100u + bytes[8]).toUShort(),
//                        softwareRev = (((((bytes[9]) * 0x100u + bytes[10]) * 0x100u) + bytes[11]) * 0x100u + bytes[12])
//                    )
//                } else {
//                    null
//                }
//            }
//        }
//
//        override fun toString(): String =
//            String.format(
//                "Identity(manufacturer = 0x%02X, deviceId = $deviceId, deviceFamily = 0x%04X, deviceFamilyNumber = 0x%04X, softwareRev = %d)",
//                manufacturerId.toInt(), deviceFamily.toInt(), deviceFamilyNumber.toInt(), softwareRev.toInt())
//    }
//
//    data class DataSet1Reply(
//        val deviceId: RolandIntegra7.DeviceId,
//        val address: UInt,
//        val bytes: UByteArray,
//        val checkSum: UByte
//    ): Integra7SysexResponse() {
//        companion object {
//            fun read(bytes: UByteArray): DataSet1Reply? =
//                if (bytes.size == 14 &&
//                    bytes[0] == EXCLUSIVE &&
//                    bytes[1] == RolandIntegra7.ROLAND &&
//                    RolandIntegra7.DeviceId.read(bytes[2]) != null &&
//                    bytes[3] == INTEGRA7[0] &&
//                    bytes[4] == INTEGRA7[4] &&
//                    bytes[5] == INTEGRA7[5] &&
//                    bytes[6] == 0x12U.toUByte()
//                ) {
//                    val payload = bytes.drop(10).dropLast(2)
//                    DataSet1Reply(
//                        deviceId = (RolandIntegra7.DeviceId.read(bytes[2])!!),
//                        address = (((((bytes[7]) * 0x100u + bytes[8]) * 0x100u) + bytes[9]) * 0x100u + bytes[10]),
//                        bytes = payload.toUByteArray(),
//                        checkSum = bytes[bytes.size - 2])
//                } else {
//                    null
//                }
//        }
//    }
//}

data class Integra7Address(val address: Int): UByteSerializable {
    val msb: UByte = (address / 0x1000000).toUByte()
    val mmsb: UByte = ((address / 0x10000).toUInt() and 0xFFu).toUByte()
    val mlsb: UByte = ((address / 0x100).toUInt() and 0xFFu).toUByte()
    val lsb: UByte = (address.toUInt() and 0xFFu).toUByte()

    override fun bytes(): UByteArray =
        ubyteArrayOf(msb, mmsb, mlsb, lsb)

    fun offsetBy(offset: Int): Integra7Address {
        // TODO: Add assertions
        return Integra7Address(address + offset)
    }
}