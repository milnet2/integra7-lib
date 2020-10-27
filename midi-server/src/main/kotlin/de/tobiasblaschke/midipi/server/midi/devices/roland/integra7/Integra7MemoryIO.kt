package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBMidiMessage
import de.tobiasblaschke.midipi.server.midi.controller.devices.integra7.RolandIntegra7

sealed class Integra7MemoryIO {
    abstract val deviceId: DeviceId
    abstract val address: Integra7Address

    fun asDataRequest1(size: UInt): RolandIntegra7MidiMessage {
        val payload = size.toByteArrayMsbFirst()
        return RolandIntegra7MidiMessage.IntegraSysExRequest(
            deviceId = deviceId,
            payload =
                    ubyteArrayOf(0x11u) +   // Command
                    address.bytes() +
                    payload +
                    checkSum(payload))
    }

    fun asDataSet1(payload: UByteArray): RolandIntegra7MidiMessage {
        return RolandIntegra7MidiMessage.IntegraSysExRequest(
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
    val msb: UByte = TODO()
    val mmsb: UByte = TODO()
    val mlsb: UByte = TODO()
    val lsb: UByte = TODO()

    override fun bytes(): UByteArray =
        ubyteArrayOf(msb, mmsb, mlsb, lsb)
}