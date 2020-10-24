package de.tobiasblaschke.midipi.server.midi.controller.devices.integra7

import de.tobiasblaschke.midipi.server.midi.controller.GenericMidiController
import de.tobiasblaschke.midipi.server.midi.controller.SystemExclusiveMessage
import javax.sound.midi.SysexMessage


fun UShort.lsb(): UByte =
    this.and(0xFFu).toUByte()

fun UShort.msb(): UByte =
    (this / 0x100u).toUByte()

fun UInt.lsb(): UByte =
    this.and(0xFFu).toUByte()

fun UInt.mlsb(): UByte =
    (this / 0x100u).and(0xFFu).toUByte()

fun UInt.mmsb(): UByte =
    (this / 0x10000u).and(0xFFu).toUByte()

fun UInt.msb(): UByte =
    (this / 0x1000000u).toUByte()


object Integra7MessageMapper: GenericMidiController.GenericMessageMapper() {
    override fun dispatch(message: SysexMessage): SystemExclusiveMessage? {
        val responseReaders = listOf(
            Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.IdentityReply::read,
            Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.DataSet1Reply::read
        )

        val response: Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse? =
            responseReaders
                .map { it(message.data.toUByteArray()) }
                .firstOrNull()

        println("Received $response")
        return response
    }
}


sealed class Integra7SystemExclusiveMessage: SystemExclusiveMessage {
    companion object {
        private const val EXCLUSIVE: UByte = 0xF0u
        private const val END: UByte = 0xF7u

        private val INTEGRA7 = ubyteArrayOf(0x00u, 0x00u, 0x64u)

        private const val DEVICE_CONTROL: UByte = 0x04u
        private const val GENERAL_INFORMATION: UByte = 0x06u
    }

    sealed class SystemExclusiveRealtimeRequest {
        companion object {
            private const val UNIVERSAL_REQUEST: UByte = 0x7Fu
        }

        abstract val bytes: UByteArray

        data class MasterVolume(val value: UShort): SystemExclusiveRealtimeRequest() {
            override val bytes = ubyteArrayOf(
                EXCLUSIVE,
                UNIVERSAL_REQUEST,
                RolandIntegra7.DeviceId.BROADCAST.value,
                DEVICE_CONTROL,
                0x01u,
                value.lsb(),
                value.msb(),
                END
            )
        }

        data class MasterFineTuning(val value: UShort): SystemExclusiveRealtimeRequest() {
            override val bytes = ubyteArrayOf(
                EXCLUSIVE,
                UNIVERSAL_REQUEST,
                RolandIntegra7.DeviceId.BROADCAST.value,
                DEVICE_CONTROL,
                0x03u,
                value.lsb(),
                value.msb(),
                END
            )
        }

        data class MasterCoarseTuning(val value: UShort): SystemExclusiveRealtimeRequest() {
            override val bytes = ubyteArrayOf(
                EXCLUSIVE,
                UNIVERSAL_REQUEST,
                RolandIntegra7.DeviceId.BROADCAST.value,
                DEVICE_CONTROL,
                0x04u,
                value.lsb(),
                value.msb(),
                END
            )
        }

        data class DataRequest1(val deviceId: RolandIntegra7.DeviceId, val address: UInt, val size: UInt, val checkSum: UByte): SystemExclusiveRealtimeRequest() {
            override val bytes = ubyteArrayOf(
                EXCLUSIVE, RolandIntegra7.ROLAND, deviceId.value, INTEGRA7[0], INTEGRA7[1], INTEGRA7[2], 0x11u,
                address.msb(), address.mmsb(), address.mlsb(), address.lsb(),
                size.msb(), size.mmsb(), size.mlsb(), size.lsb(),
                checkSum, END
            )
        }

        data class DataSet1(
            val deviceId: RolandIntegra7.DeviceId,
            val address: UInt,
            val `data`: UByteArray,
            val checkSum: UByte
        ): SystemExclusiveRealtimeRequest() {
            override val bytes = ubyteArrayOf(
                EXCLUSIVE, RolandIntegra7.ROLAND, deviceId.value, INTEGRA7[0], INTEGRA7[1], INTEGRA7[2], 0x12u,
                address.msb(), address.mmsb(), address.mlsb(), address.lsb()
            ) +
                    `data` + ubyteArrayOf(checkSum, END)
        }

        fun asSysexMessage() =
            SysexMessage(bytes.toByteArray(), bytes.size)
    }


    sealed class SystemExclusiveRealtimeResponse: Integra7SystemExclusiveMessage() {
        companion object {
            private const val UNIVERSAL_RESPONSE: UByte = 0x7Eu
        }

        data class IdentityReply(
            val manufacturerId: UByte,
            val deviceId: RolandIntegra7.DeviceId,
            val deviceFamily: UShort,
            val deviceFamilyNumber: UShort,
            val softwareRev: UInt
        ): SystemExclusiveRealtimeResponse() {
            companion object {
                fun read(bytes: UByteArray): IdentityReply? {
                    val deviceId = RolandIntegra7.DeviceId.read(bytes[1])
                    return if (
                        bytes.size == 14 &&
                        bytes[0] == UNIVERSAL_RESPONSE && // 0x7F
                        deviceId != null &&
                        bytes[2] == GENERAL_INFORMATION && // 0x
                        bytes[3] == 0x02U.toUByte()
                    ) {
                        IdentityReply(
                            manufacturerId = bytes[4],
                            deviceId = deviceId,
                            deviceFamily = (bytes[5] * 0x100u + bytes[6]).toUShort(),
                            deviceFamilyNumber = (bytes[7] * 0x100u + bytes[8]).toUShort(),
                            softwareRev = (((((bytes[9]) * 0x100u + bytes[10]) * 0x100u) + bytes[11]) * 0x100u + bytes[12])
                        )
                    } else {
                        null
                    }
                }
            }

            override fun toString(): String =
                String.format(
                    "Identity(manufacturer = 0x%02X, deviceId = $deviceId, deviceFamily = 0x%04X, deviceFamilyNumber = 0x%04X, softwareRev = %d)",
                    manufacturerId.toInt(), deviceFamily.toInt(), deviceFamilyNumber.toInt(), softwareRev.toInt())
        }

        data class DataSet1Reply(
            val deviceId: RolandIntegra7.DeviceId,
            val address: UInt,
            val bytes: UByteArray,
            val checkSum: UByte
        ): SystemExclusiveRealtimeResponse() {
            companion object {
                fun read(bytes: UByteArray): DataSet1Reply? =
                    if (bytes.size == 14 &&
                        bytes[0] == EXCLUSIVE &&
                        bytes[1] == RolandIntegra7.ROLAND &&
                        RolandIntegra7.DeviceId.read(bytes[2]) != null &&
                        bytes[3] == INTEGRA7[0] &&
                        bytes[4] == INTEGRA7[4] &&
                        bytes[5] == INTEGRA7[5] &&
                        bytes[6] == 0x12U.toUByte()
                    ) {
                        val payload = bytes.drop(10).dropLast(2)
                        DataSet1Reply(
                            deviceId = (RolandIntegra7.DeviceId.read(bytes[2])!!),
                            address = (((((bytes[7]) * 0x100u + bytes[8]) * 0x100u) + bytes[9]) * 0x100u + bytes[10]),
                            bytes = payload.toUByteArray(),
                            checkSum = bytes[bytes.size - 2])
                    } else {
                        null
                    }
            }
        }
    }


    enum class IntegraStartAddress(val address: UInt) {
        SETUP(0x01000000u),
        SYSTEM(0x01000000u),
        TEMPORARY_STUDIO_SET(0x18000000u),
        TEMPORARY_TONE_C1(0x19000000u)
    }

    sealed class IntegraAddressRequests {
        object TemporaryStudioSet: IntegraAddressRequests() {
            enum class TemporaryStudioSetOffset(val offset: UInt) {
                COMMON(0x000000u),
                COMMON_CHORUS(0x000400u),
                COMMON_REVERB(0x000600u),
                COMMON_MOTIONAL_SURROUND(0x000800u),
                MASTER_EQ(0x000900u)
            }

            fun reverbType(value: UByte): IntegraAddressRequest =
                IntegraAddressRequest(
                    IntegraStartAddress.TEMPORARY_STUDIO_SET,
                    TemporaryStudioSetOffset.COMMON_REVERB.offset +
                            0x00u, // ReverbType
                    ubyteArrayOf(value), 0x01u)

        }
    }

    data class IntegraAddressRequest(val startAddress: IntegraStartAddress, val offsetAddress: UInt, val payload: UByteArray, val size: UInt) {
        val effectiveAddress: UInt

        init {
            assert(offsetAddress < 0x1000000u)
            this.effectiveAddress = startAddress.address + offsetAddress
        }

        fun checkSum(): UByte {
            val addrSum = ((effectiveAddress.lsb() + effectiveAddress.mlsb() + effectiveAddress.mmsb() + effectiveAddress.msb()) and 0xFFu).toUByte()
            val payloadSum = payload.reduce { a, b -> ((a + b) and 0xFFu).toUByte() }
            val totalSum = ((addrSum + payloadSum) and 0xFFu).toUByte()
            val reminder = (totalSum % 128u).toUByte()
            return if (reminder < 128u) (128u - reminder).toUByte() else (reminder - 128u).toUByte()
        }

        fun asSysExSet(deviceId: RolandIntegra7.DeviceId): SystemExclusiveRealtimeRequest.DataSet1 =
            SystemExclusiveRealtimeRequest.DataSet1(
                deviceId, effectiveAddress, payload, checkSum())

        fun asSysExGet(deviceId: RolandIntegra7.DeviceId): SystemExclusiveRealtimeRequest.DataRequest1 =
            SystemExclusiveRealtimeRequest.DataRequest1(
                deviceId, effectiveAddress, size, checkSum())
    }
}