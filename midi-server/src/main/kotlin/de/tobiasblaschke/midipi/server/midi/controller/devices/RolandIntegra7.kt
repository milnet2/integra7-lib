package de.tobiasblaschke.midipi.server.midi.controller.devices

import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.controller.GenericMidiController
import de.tobiasblaschke.midipi.server.midi.controller.devices.RolandIntegra7.SystemExclusiveMessage.Companion.lsb
import de.tobiasblaschke.midipi.server.midi.controller.devices.RolandIntegra7.SystemExclusiveMessage.Companion.mlsb
import de.tobiasblaschke.midipi.server.midi.controller.devices.RolandIntegra7.SystemExclusiveMessage.Companion.mmsb
import de.tobiasblaschke.midipi.server.midi.controller.devices.RolandIntegra7.SystemExclusiveMessage.Companion.msb

class RolandIntegra7(readable: MidiDeviceDescriptor.MidiInDeviceInfo, writable: MidiDeviceDescriptor.MidiOutDeviceInfo): GenericMidiController(readable) {
    companion object {
        private const val ROLAND: UByte = 0x41u
        private const val INTEGRA7: UShort = 0x6402u

        fun matches(identityRequestResponse: UByteArray): Boolean {
            val identity = SystemExclusiveMessage.SystemExclusiveRealtimeResponse.IdentityReply.read(identityRequestResponse)

            return if (identity == null) {
                false
            } else {
                return identity.manufacturerId == ROLAND &&
                        identity.deviceFamily == INTEGRA7 &&
                        identity.deviceFamilyNumber == 0x0000u.toUShort()
            }
        }
    }

    enum class DeviceId(val value: UByte) {
        BROADCAST(0x7Fu),
        DEV01(0x10u),
        DEV02(0x11u),
        DEV03(0x12u),
        DEV04(0x13u),
        DEV05(0x14u),
        DEV06(0x15u),
        DEV07(0x16u),
        DEV08(0x17u),
        DEV09(0x18u),
        DEV10(0x19u),
        DEV11(0x1Au),
        DEV12(0x1Bu),
        DEV13(0x1Cu),
        DEV14(0x1Du),
        DEV15(0x1Eu),
        DEV16(0x1Fu);

        companion object {
            fun read(value: UByte) =
                DeviceId.values()
                    .first { it.value == value }
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
                        ubyteArrayOf(value))

        }
    }

    data class IntegraAddressRequest(val startAddress: IntegraStartAddress, val offsetAddress: UInt, val payload: UByteArray) {
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
    }

    sealed class SystemExclusiveMessage {
        companion object {
            private const val EXCLUSIVE: UByte = 0xF0u
            private const val END: UByte = 0xF7u

            private val INTEGRA7 = ubyteArrayOf(0x00u, 0x00u, 0x64u)

            private const val DEVICE_CONTROL: UByte = 0x04u
            private const val GENERAL_INFORMATION: UByte = 0x06u

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
        }

        sealed class SystemExclusiveRealtimeRequest {
            companion object {
                private const val UNIVERSAL_REQUEST: UByte = 0x7Fu
            }

            data class MasterVolume(val value: UShort) {
                val bytes = ubyteArrayOf(
                    EXCLUSIVE,
                    UNIVERSAL_REQUEST,
                    DeviceId.BROADCAST.value,
                    DEVICE_CONTROL,
                    0x01u,
                    value.lsb(),
                    value.msb(),
                    END
                )
            }

            data class MasterFineTuning(val value: UShort) {
                val bytes = ubyteArrayOf(
                    EXCLUSIVE,
                    UNIVERSAL_REQUEST,
                    DeviceId.BROADCAST.value,
                    DEVICE_CONTROL,
                    0x03u,
                    value.lsb(),
                    value.msb(),
                    END
                )
            }

            data class MasterCoarseTuning(val value: UShort) {
                val bytes = ubyteArrayOf(
                    EXCLUSIVE,
                    UNIVERSAL_REQUEST,
                    DeviceId.BROADCAST.value,
                    DEVICE_CONTROL,
                    0x04u,
                    value.lsb(),
                    value.msb(),
                    END
                )
            }

            data class DataRequest1(val deviceId: DeviceId, val address: UInt, val size: UInt, val checkSum: UByte) {
                val bytes = ubyteArrayOf(
                    EXCLUSIVE, ROLAND, deviceId.value, INTEGRA7[0], INTEGRA7[1], INTEGRA7[2], 0x11u,
                    address.msb(), address.mmsb(), address.mlsb(), address.lsb(),
                    size.msb(), size.mmsb(), size.mlsb(), size.lsb(),
                    checkSum, END
                )

                data class DataSet1(
                    val deviceId: DeviceId,
                    val address: UInt,
                    val `data`: UByteArray,
                    val checkSum: UByte
                ) {
                    val bytes = ubyteArrayOf(
                        EXCLUSIVE, ROLAND, deviceId.value, INTEGRA7[0], INTEGRA7[1], INTEGRA7[2], 0x12u,
                        address.msb(), address.mmsb(), address.mlsb(), address.lsb()
                    ) +
                            `data` + ubyteArrayOf(checkSum, END)
                }
            }
        }


        sealed class SystemExclusiveRealtimeResponse {
            companion object {
                private const val UNIVERSAL_RESPONSE: UByte = 0x7Eu
            }

            data class IdentityReply(
                val manufacturerId: UByte,
                val deviceId: DeviceId,
                val deviceFamily: UShort,
                val deviceFamilyNumber: UShort,
                val softwareRev: UInt
            ) {
                companion object {
                    fun read(bytes: UByteArray): IdentityReply? {
                        val deviceId = DeviceId.read(bytes[1])
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
                val deviceId: DeviceId,
                val address: UInt,
                val bytes: UByteArray,
                val checkSum: UByte
            ) {
                companion object {
                    fun read(bytes: UByteArray): DataSet1Reply? =
                        if (bytes.size == 14 &&
                            bytes[0] == EXCLUSIVE &&
                            bytes[1] == ROLAND &&
                            DeviceId.read(bytes[2]) != null &&
                            bytes[3] == INTEGRA7[0] &&
                            bytes[4] == INTEGRA7[4] &&
                            bytes[5] == INTEGRA7[5] &&
                            bytes[6] == 0x12U.toUByte()
                        ) {
                            val payload = bytes.drop(10).dropLast(2)
                            DataSet1Reply(
                                deviceId = (DeviceId.read(bytes[2])!!),
                                address = (((((bytes[7]) * 0x100u + bytes[8]) * 0x100u) + bytes[9]) * 0x100u + bytes[10]),
                                bytes = payload.toUByteArray(),
                                checkSum = bytes[bytes.size - 2])
                        } else {
                            null
                        }
                }
            }
        }
    }
}