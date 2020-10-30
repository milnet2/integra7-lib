package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain.*
import de.tobiasblaschke.midipi.server.midi.toAsciiString
import de.tobiasblaschke.midipi.server.midi.utils.SparseUByteArray
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*
import kotlin.Comparator
import kotlin.NoSuchElementException


abstract class Integra7MemoryIO<T> {
    internal abstract val deviceId: DeviceId
    internal abstract val address: Integra7Address
    internal abstract val size: Integra7Size

    fun isCovering(address: Integra7Address) =
        address >= this.address && address <= this.address.offsetBy(size)

    fun asDataRequest1(): RolandIntegra7MidiMessage {
        println(" ## Requesting $address to ${address.offsetBy(size)}...")
        return RolandIntegra7MidiMessage.IntegraSysExReadRequest(
            deviceId = deviceId,
            address = address,
            size = size,
            checkSum = checkSum(size.bytes()))
    }

    fun asDataSet1(payload: UByteArray): RolandIntegra7MidiMessage {
        return RolandIntegra7MidiMessage.IntegraSysExWriteRequest(
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

    abstract fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): T

    @ExperimentalUnsignedTypes
    data class AsciiStringField(override val deviceId: DeviceId, override val address: Integra7Address, val length: Int): Integra7MemoryIO<String>() {
        override val size = Integra7Size(length.toUInt())

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): String {
            assert(startAddress >= address)

            try {
                return payload[IntRange(startAddress.fullByteAddress(), startAddress.fullByteAddress() + this.length)].toAsciiString().trim()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading range $startAddress..${startAddress.offsetBy(this.length)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedValueField(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..127): Integra7MemoryIO<Int>() {
        override val size = Integra7Size.ONE_BYTE

        init {
            assert(range.first >= 0 && range.last <= 127) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val ret = payload[startAddress.fullByteAddress()].toInt()
                if (!range.contains(ret)) {
                    throw IllegalStateException("Value $ret not in $range When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}")
                } else {
                    return ret
                }
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading range $startAddress..${startAddress.offsetBy(this.size)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class EnumValueField<T: Enum<T>>(override val deviceId: DeviceId, override val address: Integra7Address, val values: Array<T>): Integra7MemoryIO<T>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): T {
            val elem = delegate.interpret(startAddress, length, payload)
            return try {
                values[elem]
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw NoSuchElementException("When reading address ${startAddress}: No element $elem in ${Arrays.toString(values)}")
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFF): Integra7MemoryIO<Int>() {
        override val size = Integra7Size(lsb = 0x02u)

        init {
            assert(range.first >= 0 && range.last <= 0xFF) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val ret = payload[startAddress.fullByteAddress()].toInt() * 0x10 +
                        payload[startAddress.fullByteAddress() + 1].toInt()
                if (range.contains(ret)) {
                    return ret
                } else {
                    throw IllegalStateException("Unsupported value $ret not in $range, When reading address ${startAddress} (sa=$startAddress, len=$length)")
                }
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address ${startAddress} (sa=$startAddress, len=$length)", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedLsbMsbBytes(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Int>() {
        override val size = Integra7Size(lsb = 0x02u)

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                return payload[startAddress.fullByteAddress()].toInt() +
                        payload[startAddress.fullByteAddress() + 1].toInt() * 0x80
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address ${startAddress} (sa=$startAddress, len=$length)", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFFFF): Integra7MemoryIO<Int>() {
        override val size = Integra7Size(lsb = 0x04u)

        init {
            assert(range.first >= 0 && range.last <= 0xFFFF) { "Impossible range $range for this datatype" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val msb = payload[startAddress.fullByteAddress()].toInt()
                val mmsb = payload[startAddress.fullByteAddress() + 1].toInt()
                val mlsb = payload[startAddress.fullByteAddress() + 2].toInt()
                val lsb = payload[startAddress.fullByteAddress() + 3].toInt()

                val ret = ((((msb * 0x10) + mmsb) * 0x10) + mlsb) * 0x10 + lsb
                if (range.contains(ret)) {
                    return ret
                } else {
                    throw IllegalStateException("Value $ret not in $range, When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}")
                }
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class SignedValueField(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val range: IntRange = -63..63
    ): Integra7MemoryIO<Int>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        init {
            assert(range.first >= -64 && range.endInclusive <= 63) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val ret = delegate.interpret(startAddress, length, payload) - 64
            if (!range.contains(ret)) {
                throw IllegalStateException("Value $ret not in $range When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}")
            } else {
                return ret
            }
        }
    }

    data class SignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Int>() {
        private val delegate = UnsignedMsbLsbFourNibbles(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int =
            delegate.interpret(startAddress, length, payload) - 0x7FFF
    }

    @ExperimentalUnsignedTypes
    data class BooleanValueField(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Boolean>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Boolean =
            delegate.interpret(startAddress, length, payload) > 0
    }

}

// ----------------------------------------------------

class AddressRequestBuilder(private val deviceId: DeviceId) {
//    val undocumented = UndocumentedRequestBuilder(deviceId, Integra7Address(0x0F000402))    // TODO: This is not a typical address-request!!
    val setup = SetupRequestBuilder(deviceId, Integra7Address(0x01000000))
    val system = SystemCommonRequestBuilder(deviceId, Integra7Address(0x02000000))
    val studioSet = StudioSetAddressRequestBuilder(deviceId, Integra7Address(0x18000000))

    val tones: Map<IntegraPart, ToneAddressRequestBuilder> = IntegraPart.values()
        .map { it to ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000).offsetBy(0x200000, it.zeroBased), it) }
        .toMap()

    fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Values {
        //assert(length <= size.toInt())
        assert(payload.size <= length)

        return Values(
            tone = IntegraPart.values()
                .map { it to this.tones.getValue(it).interpret(startAddress, length, payload) } // .subRange(from = 0x200000 * it.zeroBased, to = 0x200000 * it.zeroBased + 0x303C)) }
                .toMap()
        )
    }

    data class Values(
        val tone: Map<IntegraPart, ToneAddressRequestBuilder.TemporaryTone>
    )
}

//data class UndocumentedRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Setup>() {
//    // TODO: This field works different here!
//    override val size = Integra7Size(0x5Fu, 0x40u, 0x00u, 0x40u)
//
//    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Setup {
//        //assert(this.isCovering(startAddress)) { "Error reading field ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }
//
//        return Setup(
//            soundMode = when(payload[0].toUInt()) {
//                0x01u -> SoundMode.STUDIO
//                0x02u -> SoundMode.GM1
//                0x03u -> SoundMode.GM2
//                0x04u -> SoundMode.GS
//                else -> throw IllegalArgumentException()
//            },
//            studioSetBsMsb = payload[0x04],
//            studioSetBsLsb = payload[0x05],
//            studioSetPc = payload[0x06]
//        )
//    }
//}

data class SetupRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Setup>() {
    override val size = Integra7Size(38u)

    val soundMode = UnsignedValueField(deviceId, address)
    val studioSetBs = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x04u))
    val studioSetPc = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Setup {
        assert(this.isCovering(startAddress)) { "Expected Setup-range ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

        return Setup(
            soundMode = when(val sm = soundMode.interpret(startAddress, length, payload)) {
                0x01 -> SoundMode.STUDIO
                0x02 -> SoundMode.GM1
                0x03 -> SoundMode.GM2
                0x04 -> SoundMode.GS
                else -> throw IllegalArgumentException("Unsupported sound-mode $sm")
            },
            studioSetBs = studioSetBs.interpret(startAddress.offsetBy(0x04), length, payload),
            studioSetPc = studioSetPc.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload))
    }
}

data class SystemCommonRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<SystemCommon>() {
    override val size= Integra7Size(0x2Fu)

    val masterKeyShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x04u))
    val masterLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
    val scaleTuneSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x06u))
    val systemControl1Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x20u))
    val systemControl2Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x21u))
    val systemControl3Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x22u))
    val systemControl4Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x23u))
    val controlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x24u), ControlSourceType.values())
    val systemClockSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x25u), ClockSource.values())
    val systemTempo = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x26u))
    val tempoAssignSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x28u), ControlSourceType.values())
    val receiveProgramChange = BooleanValueField(deviceId, address.offsetBy(lsb = 0x29u))
    val receiveBankSelect = BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Au))
    val centerSpeakerSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Bu))
    val subWooferSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Cu))
    val twoChOutputMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Du), TwoChOutputMode.values())

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SystemCommon {
        assert(this.isCovering(startAddress)) { "Expected System-common ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

        return SystemCommon(
            // TODO masterTune = (startAddress, min(payload.size, 0x0F), payload.copyOfRange(0, min(payload.size, 0x0F)))
            masterKeyShift = masterKeyShift.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
            masterLevel = masterLevel.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
            scaleTuneSwitch = scaleTuneSwitch.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
            studioSetControlChannel = null, // TODO: if (payload[0x11] < 0x0Fu) payload[0x11].toInt() else null,
            systemControl1Source = systemControl1Source.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
            systemControl2Source = systemControl2Source.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
            systemControl3Source = systemControl3Source.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
            systemControl4Source = systemControl4Source.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
            controlSource = controlSource.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload),
            systemClockSource = systemClockSource.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
            systemTempo = systemTempo.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload),
            tempoAssignSource = tempoAssignSource.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
            receiveProgramChange = receiveProgramChange.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload),
            receiveBankSelect = receiveBankSelect.interpret(startAddress.offsetBy(lsb = 0x2Au), length, payload),
            centerSpeakerSwitch = centerSpeakerSwitch.interpret(startAddress.offsetBy(lsb = 0x2Bu), length, payload),
            subWooferSwitch = subWooferSwitch.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
            twoChOutputMode = twoChOutputMode.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
        )
    }



    data class SystemControlSourceAddress(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<ControlSource>() {
        override val size = Integra7Size(0x01u)

        override fun  interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): ControlSource {
            assert(startAddress >= address)

            return ControlSource.values()
                .first { it.hex == payload[0] }!!
        }
    }
}

// -----------------------------------------------------

data class StudioSetAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetAddressRequestBuilder.StudioSet>() {
    override val size = Integra7Size(0x1u, 0x00u, 0x00u, 0x00u)

    val common = StudioSetCommonAddressRequestBuilder(deviceId, address.offsetBy(0x000000))
    // commonChorus
    // commonReverb
    // commonMotionalSurround
    // masterEQ
    // midi01
    // ..
    // midi16
    // part01
    // ..
    // part16
    // partEQ01
    // ..
    // partEQ16

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSet {
        assert(this.isCovering(startAddress)) { "Expected Studio-Set address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

        return StudioSet(
            common = common.interpret(startAddress, length, payload)
        )
    }

    data class StudioSet(
        val common: StudioSetCommon
    )

    data class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommon>() {
        override val size = Integra7Size(54u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), 0x0F)
        val voiceReserve01 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u), 0..64)
        val voiceReserve02 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u), 0..64)
        val voiceReserve03 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au), 0..64)
        val voiceReserve04 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..64)
        val voiceReserve05 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Cu), 0..64)
        val voiceReserve06 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du), 0..64)
        val voiceReserve07 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Eu), 0..64)
        val voiceReserve08 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Fu), 0..64)
        val voiceReserve09 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u), 0..64)
        val voiceReserve10 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u), 0..64)
        val voiceReserve11 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x22u), 0..64)
        val voiceReserve12 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x23u), 0..64)
        val voiceReserve13 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x24u), 0..64)
        val voiceReserve14 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x25u), 0..64)
        val voiceReserve15 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x26u), 0..64)
        val voiceReserve16 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u), 0..64)
        val tone1ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), ControlSource.values())
        val tone2ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Au), ControlSource.values())
        val tone3ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Bu), ControlSource.values())
        val tone4ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), ControlSource.values())
        val tempo = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x3Du), 20..250)
        val reverbSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x40u))
        val chorusSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val masterEQSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x42u))
        val drumCompEQSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val extPartLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val extPartChorusSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val extPartReverbSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))
        val extPartReverbMuteSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x4Fu))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetCommon {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set common address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetCommon(
                name = name.interpret(startAddress, length, payload),
                voiceReserve01 = voiceReserve01.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                voiceReserve02 = voiceReserve02.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                voiceReserve03 = voiceReserve03.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                voiceReserve04 = voiceReserve04.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                voiceReserve05 = voiceReserve05.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                voiceReserve06 = voiceReserve06.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                voiceReserve07 = voiceReserve07.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),
                voiceReserve08 = voiceReserve08.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                voiceReserve09 = voiceReserve09.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                voiceReserve10 = voiceReserve10.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                voiceReserve11 = voiceReserve11.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                voiceReserve12 = voiceReserve12.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                voiceReserve13 = voiceReserve13.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload),
                voiceReserve14 = voiceReserve14.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                voiceReserve15 = voiceReserve15.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload),
                voiceReserve16 = voiceReserve16.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                tone1ControlSource = tone1ControlSource.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                tone2ControlSource = tone2ControlSource.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                tone3ControlSource = tone3ControlSource.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                tone4ControlSource = tone4ControlSource.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
                tempo = tempo.interpret(startAddress.offsetBy(lsb =0x3Du), length, payload),
                // TODO: soloPart
                reverbSwitch = reverbSwitch.interpret(startAddress.offsetBy(lsb = 0x40u), length, payload),
                chorusSwitch = chorusSwitch.interpret(startAddress.offsetBy(lsb = 0x41u), length, payload),
                masterEQSwitch = masterEQSwitch.interpret(startAddress.offsetBy(lsb = 0x42u), length, payload),
                drumCompEQSwitch = drumCompEQSwitch.interpret(startAddress.offsetBy(lsb = 0x43u), length, payload),
                // TODO: drumCompEQPart
                // Drum Comp/EQ 1 Output Assign
                // Drum Comp/EQ 2 Output Assign
                // Drum Comp/EQ 3 Output Assign
                // Drum Comp/EQ 4 Output Assign
                // Drum Comp/EQ 5 Output Assign
                // Drum Comp/EQ 6 Output Assign
                extPartLevel = extPartLevel.interpret(startAddress.offsetBy(lsb = 0x4Cu), length, payload),
                extPartChorusSendLevel = extPartChorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x4Du), length, payload),
                extPartReverbSendLevel = extPartReverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x4Eu), length, payload),
                extPartReverbMuteSwitch = extPartReverbMuteSwitch.interpret(startAddress.offsetBy(lsb = 0x4Fu), length, payload),
            )
        }
    }
}

data class ToneAddressRequestBuilder(
    override val deviceId: DeviceId,
    override val address: Integra7Address,
    val part: IntegraPart
): Integra7MemoryIO<ToneAddressRequestBuilder.TemporaryTone>() {
    override val size: Integra7Size = Integra7Size(0x00u, 0x20u, 0x00u, 0x00u)

    val pcmSynthTone = IntegraToneBuilder.PcmSynthToneBuilder(deviceId, address.offsetBy(0x000000), part)
    val snaSynthTone = IntegraToneBuilder.SuperNaturalSynthToneBuilder(deviceId, address.offsetBy(0x010000), part)
    val snaAcousticTone = IntegraToneBuilder.SuperNaturalAcousticToneBuilder(deviceId, address.offsetBy(0x020000), part)
    val snaDrumKit = IntegraToneBuilder.SuperNaturalDrumKitBuilder(deviceId, address.offsetBy(0x030000), part)
    val pcmDrumKit = IntegraToneBuilder.PcmDrumKitBuilder(deviceId, address.offsetBy(0x100000), part)

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): TemporaryTone {
        assert(this.isCovering(startAddress)) { "Not a tone definition ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

        return when {
            pcmSynthTone.isCovering(startAddress) -> TemporaryTone(
                tone = pcmSynthTone.interpret(startAddress, length, payload))
            snaSynthTone.isCovering(startAddress) -> TemporaryTone(
                tone = snaSynthTone.interpret(startAddress, length, payload))
            snaAcousticTone.isCovering(startAddress) -> TemporaryTone(
                tone = snaAcousticTone.interpret(startAddress, length, payload))
            snaDrumKit.isCovering(startAddress) -> TemporaryTone(
                tone = snaDrumKit.interpret(startAddress, length, payload))
            pcmDrumKit.isCovering(startAddress) -> TemporaryTone(
                tone = pcmDrumKit.interpret(startAddress, length, payload))
            else -> throw IllegalArgumentException("Unsupported tone $startAddress ${startAddress.rangeName()} for part $part")
        }
    }

    data class TemporaryTone(
        val tone: IntegraTone
    )
}

sealed class IntegraToneBuilder<T: IntegraTone>: Integra7MemoryIO<T>() {
    data class PcmSynthToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<PcmSynthTone>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x30u, 0x3Cu)

        val common = PcmSynthToneCommonBuilder(deviceId, address.offsetBy(0x000000))
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u))
        val partialMixTable = PcmSynthTonePartialMixTableBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u))
        val partial1 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u))
        val partial2 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x22u, lsb = 0x00u))
        val partial3 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x24u, lsb = 0x00u))
        val partial4 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x26u, lsb = 0x00u))
        val common2 = PcmSynthToneCommon2Builder(deviceId, address.offsetBy(mlsb = 0x30u, lsb = 0x00u))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): PcmSynthTone {
            assert(this.isCovering(startAddress)) { "Not a PCM synth tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmSynthTone(
                common = common.interpret(startAddress, 0x50, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), length, payload),
                partialMixTable = partialMixTable.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u), length, payload),
                partial1 = // if (payload.size >= startAddress.offsetBy(msb = 0x20u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                    partial1.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u), length, payload), // else null,
                partial2 = // if (payload.size >= startAddress.offsetBy(msb = 0x22u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                    partial2.interpret(startAddress.offsetBy(mlsb = 0x22u, lsb = 0x00u), length, payload), // else null,
                partial3 = // if (payload.size >= startAddress.offsetBy(msb = 0x24u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                    partial3.interpret(startAddress.offsetBy(mlsb = 0x24u, lsb = 0x00u), length, payload), // else null,
                partial4 = // if (payload.size >= startAddress.offsetBy(msb = 0x26u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                    partial4.interpret(startAddress.offsetBy(mlsb = 0x26u, lsb = 0x00u), length, payload), // else null,
                common2 = // if (payload.size >= startAddress.offsetBy(msb = 0x30u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                    common2.interpret(startAddress.offsetBy(mlsb = 0x30u, lsb = 0x00u), length, payload), //else null,
            )
        }
    }

    data class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommon>() {
        override val size = Integra7Size(0x50u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pan = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val priority = EnumValueField(deviceId, address.offsetBy(lsb = 0x10u), Priority.values())
        val coarseTuning = SignedValueField(deviceId, address.offsetBy(lsb = 0x11u), -48..48)
        val fineTuning = SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -50..50)
        val ocataveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val stretchTuneDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x14u), 0..3)
        val analogFeel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x15u))
        val monoPoly = EnumValueField(deviceId, address.offsetBy(lsb = 0x16u), MonoPoly.values())
        val legatoSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val legatoRetrigger = BooleanValueField(deviceId, address.offsetBy(lsb = 0x18u))
        val portamentoSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val portamentoMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Au), PortamentoMode.values())
        val portamentoType = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Bu), PortamentoType.values())
        val portamentoStart = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), PortamentoStart.values())
        val portamentoTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du))

        val cutoffOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val resonanceOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x23u))
        val attackTimeOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x24u))
        val releaseTimeOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x25u))
        val velocitySensOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x26u))

        val pmtControlSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x28u))
        val pitchBendRangeUp = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x29u), 0..48)
        val pitchBendRangeDown = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Au), 0..48)

        val matrixControl1Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Bu), MatrixControlSource.values())
        val matrixControl1Destination1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Cu), MatrixControlDestination.values())
        val matrixControl1Sens1 = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val matrixControl1Destination2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Eu), MatrixControlDestination.values())
        val matrixControl1Sens2 = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu))
        val matrixControl1Destination3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x30u), MatrixControlDestination.values())
        val matrixControl1Sens3 = SignedValueField(deviceId, address.offsetBy(lsb = 0x31u))
        val matrixControl1Destination4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x32u), MatrixControlDestination.values())
        val matrixControl1Sens4 = SignedValueField(deviceId, address.offsetBy(lsb = 0x33u))

        val matrixControl2Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), MatrixControlSource.values())
        val matrixControl2Destination1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x35u), MatrixControlDestination.values())
        val matrixControl2Sens1 = SignedValueField(deviceId, address.offsetBy(lsb = 0x36u))
        val matrixControl2Destination2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x37u), MatrixControlDestination.values())
        val matrixControl2Sens2 = SignedValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val matrixControl2Destination3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), MatrixControlDestination.values())
        val matrixControl2Sens3 = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Au))
        val matrixControl2Destination4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Bu), MatrixControlDestination.values())
        val matrixControl2Sens4 = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu))

        val matrixControl3Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Du), MatrixControlSource.values())
        val matrixControl3Destination1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Eu), MatrixControlDestination.values())
        val matrixControl3Sens1 = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val matrixControl3Destination2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x40u), MatrixControlDestination.values())
        val matrixControl3Sens2 = SignedValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val matrixControl3Destination3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x42u), MatrixControlDestination.values())
        val matrixControl3Sens3 = SignedValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val matrixControl3Destination4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x44u), MatrixControlDestination.values())
        val matrixControl3Sens4 = SignedValueField(deviceId, address.offsetBy(lsb = 0x45u))

        val matrixControl4Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x46u), MatrixControlSource.values())
        val matrixControl4Destination1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x47u), MatrixControlDestination.values())
        val matrixControl4Sens1 = SignedValueField(deviceId, address.offsetBy(lsb = 0x48u))
        val matrixControl4Destination2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x49u), MatrixControlDestination.values())
        val matrixControl4Sens2 = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Au))
        val matrixControl4Destination3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x4Bu), MatrixControlDestination.values())
        val matrixControl4Sens3 = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val matrixControl4Destination4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x4Du), MatrixControlDestination.values())
        val matrixControl4Sens4 = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthToneCommon {
            assert(startAddress >= address)

            try {
                return PcmSynthToneCommon(
                    name = name.interpret(startAddress, length, payload),
                    level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                    pan = pan.interpret(startAddress.offsetBy(lsb = 0x00Fu), length, payload),
                    priority = priority.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                    coarseTuning = coarseTuning.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                    fineTuning = fineTuning.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                    ocataveShift = ocataveShift.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                    stretchTuneDepth = stretchTuneDepth.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                    analogFeel = analogFeel.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                    monoPoly = monoPoly.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                    legatoSwitch = legatoSwitch.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                    legatoRetrigger = legatoRetrigger.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                    portamentoSwitch = portamentoSwitch.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                    portamentoMode = portamentoMode.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                    portamentoType = portamentoType.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                    portamentoStart = portamentoStart.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                    portamentoTime = portamentoTime.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),

                    cutoffOffset = cutoffOffset.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                    resonanceOffset = resonanceOffset.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                    attackTimeOffset = attackTimeOffset.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload),
                    releaseTimeOffset = releaseTimeOffset.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                    velocitySensOffset = velocitySensOffset.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload),

                    pmtControlSwitch = pmtControlSwitch.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                    pitchBendRangeUp = pitchBendRangeUp.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload),
                    pitchBendRangeDown = pitchBendRangeDown.interpret(startAddress.offsetBy(lsb = 0x2Au), length, payload),

                    matrixControl1Source = matrixControl1Source.interpret(startAddress.offsetBy(lsb = 0x2Bu), length, payload),
                    matrixControl1Destination1 = matrixControl1Destination1.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                    matrixControl1Sens1 = matrixControl1Sens1.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
                    matrixControl1Destination2 = matrixControl1Destination2.interpret(startAddress.offsetBy(lsb = 0x2Eu), length, payload),
                    matrixControl1Sens2 = matrixControl1Sens2.interpret(startAddress.offsetBy(lsb = 0x2Fu), length, payload),
                    matrixControl1Destination3 = matrixControl1Destination3.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                    matrixControl1Sens3 = matrixControl1Sens3.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),
                    matrixControl1Destination4 = matrixControl1Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x32u), length, payload),
                    matrixControl1Sens4 = matrixControl1Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x33u), length, payload),

                    matrixControl2Source = matrixControl2Source.interpret(
                        startAddress.offsetBy(lsb = 0x34u), length, payload),
                    matrixControl2Destination1 = matrixControl2Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x35u), length, payload),
                    matrixControl2Sens1 = matrixControl2Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x36u), length, payload),
                    matrixControl2Destination2 = matrixControl2Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x37u), length, payload),
                    matrixControl2Sens2 = matrixControl2Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x38u), length, payload),
                    matrixControl2Destination3 = matrixControl2Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x39u), length, payload),
                    matrixControl2Sens3 = matrixControl2Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x3Au), length, payload),
                    matrixControl2Destination4 = matrixControl2Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                    matrixControl2Sens4 = matrixControl2Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x3Cu), length, payload),

                    matrixControl3Source = matrixControl3Source.interpret(
                        startAddress.offsetBy(lsb = 0x3Du), length, payload),
                    matrixControl3Destination1 = matrixControl3Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x3Eu), length, payload),
                    matrixControl3Sens1 = matrixControl3Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x3Fu), length, payload),
                    matrixControl3Destination2 = matrixControl3Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x40u), length, payload),
                    matrixControl3Sens2 = matrixControl3Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x41u), length, payload),
                    matrixControl3Destination3 = matrixControl3Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x42u), length, payload),
                    matrixControl3Sens3 = matrixControl3Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x43u), length, payload),
                    matrixControl3Destination4 = matrixControl3Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x44u), length, payload),
                    matrixControl3Sens4 = matrixControl3Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x45u), length, payload),

                    matrixControl4Source = matrixControl1Source.interpret(
                        startAddress.offsetBy(lsb = 0x46u), length, payload),
                    matrixControl4Destination1 = matrixControl1Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x47u), length, payload),
                    matrixControl4Sens1 = matrixControl1Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x48u), length, payload),
                    matrixControl4Destination2 = matrixControl1Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x49u), length, payload),
                    matrixControl4Sens2 = matrixControl1Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x4Au), length, payload),
                    matrixControl4Destination3 = matrixControl1Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x4Bu), length, payload),
                    matrixControl4Sens3 = matrixControl1Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x4Cu), length, payload),
                    matrixControl4Destination4 = matrixControl4Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x4Du), length, payload),
                    matrixControl4Sens4 = matrixControl4Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x4Eu), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthToneMfxBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneMfx>() {
        override val size = Integra7Size(mlsb = 0x01u, lsb = 0x11u)

        val mfxType = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val mfxChorusSend = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u))
        val mfxReverbSend = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u))

        val mfxControl1Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x05u), MfxControlSource.values())
        val mfxControl1Sens = SignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val mfxControl2Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x07u), MfxControlSource.values())
        val mfxControl2Sens = SignedValueField(deviceId, address.offsetBy(lsb = 0x08u))
        val mfxControl3Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x09u), MfxControlSource.values())
        val mfxControl3Sens = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Au))
        val mfxControl4Source = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), MfxControlSource.values())
        val mfxControl4Sens = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

        val mfxControlAssign1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Du))
        val mfxControlAssign2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val mfxControlAssign3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val mfxControlAssign4 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))

        val mfxParameter1 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x11u))
        val mfxParameter2 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x15u))
        val mfxParameter3 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x19u))
        val mfxParameter4 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x1Du))
        val mfxParameter5 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x21u))
        val mfxParameter6 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x25u))
        val mfxParameter7 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x29u))
        val mfxParameter8 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Du))
        val mfxParameter9 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x31u))
        val mfxParameter10 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u))
        val mfxParameter11 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x39u))
        val mfxParameter12 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x3Du))
        val mfxParameter13 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x41u))
        val mfxParameter14 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x45u))
        val mfxParameter15 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x49u))
        val mfxParameter16 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x4Du))
        val mfxParameter17 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x51u))
        val mfxParameter18 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x55u))
        val mfxParameter19 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x59u))
        val mfxParameter20 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du))
        val mfxParameter21 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u))
        val mfxParameter22 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u))
        val mfxParameter23 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x69u))
        val mfxParameter24 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x6Du))
        val mfxParameter25 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x71u))
        val mfxParameter26 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x75u))
        val mfxParameter27 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x79u))
        val mfxParameter28 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Du))
        val mfxParameter29 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u))
        val mfxParameter30 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val mfxParameter31 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u))
        val mfxParameter32 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthToneMfx {
            assert(this.isCovering(startAddress)) { "Not a MFX definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()} in  ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}" }

            try {
            return PcmSynthToneMfx(
                mfxType = mfxType.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                mfxChorusSend = mfxChorusSend.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                mfxReverbSend = mfxReverbSend.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),

                mfxControl1Source = mfxControl1Source.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                mfxControl1Sens = mfxControl1Sens.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                mfxControl2Source = mfxControl2Source.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                mfxControl2Sens = mfxControl2Sens.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                mfxControl3Source = mfxControl3Source.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),
                mfxControl3Sens = mfxControl3Sens.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),
                mfxControl4Source = mfxControl4Source.interpret(startAddress.offsetBy(lsb = 0x0Bu), length, payload),
                mfxControl4Sens = mfxControl4Sens.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),

                mfxControlAssign1 = mfxControlAssign1.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                mfxControlAssign2 = mfxControlAssign2.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                mfxControlAssign3 = mfxControlAssign3.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                mfxControlAssign4 = mfxControlAssign4.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),

                mfxParameter1 = mfxParameter1.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                mfxParameter2 = mfxParameter2.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                mfxParameter3 = mfxParameter3.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                mfxParameter4 = mfxParameter4.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                mfxParameter5 = mfxParameter5.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                mfxParameter6 = mfxParameter6.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                mfxParameter7 = mfxParameter7.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload),
                mfxParameter8 = mfxParameter8.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
                mfxParameter9 = mfxParameter9.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),

                mfxParameter10 = mfxParameter10.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                mfxParameter11 = mfxParameter11.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                mfxParameter12 = mfxParameter12.interpret(startAddress.offsetBy(lsb = 0x3Du), length, payload),
                mfxParameter13 = mfxParameter13.interpret(startAddress.offsetBy(lsb = 0x41u), length, payload),
                mfxParameter14 = mfxParameter14.interpret(startAddress.offsetBy(lsb = 0x45u), length, payload),
                mfxParameter15 = mfxParameter15.interpret(startAddress.offsetBy(lsb = 0x49u), length, payload),
                mfxParameter16 = mfxParameter16.interpret(startAddress.offsetBy(lsb = 0x4Du), length, payload),
                mfxParameter17 = mfxParameter17.interpret(startAddress.offsetBy(lsb = 0x51u), length, payload),
                mfxParameter18 = mfxParameter18.interpret(startAddress.offsetBy(lsb = 0x55u), length, payload),
                mfxParameter19 = mfxParameter19.interpret(startAddress.offsetBy(lsb = 0x59u), length, payload),

                mfxParameter20 = mfxParameter20.interpret(startAddress.offsetBy(lsb = 0x5Du), length, payload),
                mfxParameter21 = mfxParameter21.interpret(startAddress.offsetBy(lsb = 0x61u), length, payload),
                mfxParameter22 = mfxParameter22.interpret(startAddress.offsetBy(lsb = 0x65u), length, payload),
                mfxParameter23 = mfxParameter23.interpret(startAddress.offsetBy(lsb = 0x69u), length, payload),
                mfxParameter24 = mfxParameter24.interpret(startAddress.offsetBy(lsb = 0x6Du), length, payload),
                mfxParameter25 = mfxParameter25.interpret(startAddress.offsetBy(lsb = 0x71u), length, payload),
                mfxParameter26 = mfxParameter26.interpret(startAddress.offsetBy(lsb = 0x75u), length, payload),
                mfxParameter27 = mfxParameter27.interpret(startAddress.offsetBy(lsb = 0x79u), length, payload),
                mfxParameter28 = mfxParameter28.interpret(startAddress.offsetBy(lsb = 0x7Du), length, payload),
                mfxParameter29 = mfxParameter29.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x01u), length, payload),

                mfxParameter30 = mfxParameter30.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x05u), length, payload),
                mfxParameter31 = mfxParameter31.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u), length, payload),
                mfxParameter32 = 0 // mfxParameter32.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), length, payload),  // TODO!!
            )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthTonePartialMixTableBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartialMixTable>() {
        override val size = Integra7Size(lsb = 0x29u)

        val structureType12 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val booster12 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x01u)) // ENUM
        val structureType34 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u))
        val booster34 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u)) // ENUM

        val velocityControl = EnumValueField(deviceId, address.offsetBy(lsb = 0x04u), VelocityControl.values())

        val pmt1PartialSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val pmt1KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x06u))
        val pmt1KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x08u))
        val pmt1VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Au))
        val pmt1VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Cu))

        val pmt2PartialSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pmt2KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Fu))
        val pmt2KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x11u))
        val pmt2VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x13u))
        val pmt2VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x15u))

        val pmt3PartialSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val pmt3KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x18u))
        val pmt3KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Au))
        val pmt3VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Cu))
        val pmt3VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Eu))

        val pmt4PartialSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val pmt4KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x21u))
        val pmt4KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x23u))
        val pmt4VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x25u))
        val pmt4VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x27u))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthTonePartialMixTable {
            assert(startAddress >= address)

            try {
                return PcmSynthTonePartialMixTable(
                    structureType12 = structureType12.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    booster12 = booster12.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                    structureType34 = structureType34.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                    booster34 = booster34.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),

                    velocityControl = velocityControl.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),

                    pmt1PartialSwitch = pmt1PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                    pmt1KeyboardRange  = pmt1KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                    pmt1KeyboardFadeWidth = pmt1KeyboardFadeWidth.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                    pmt1VelocityRange = pmt1VelocityRange.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),
                    pmt1VelocityFade = pmt1VelocityFade.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),

                    pmt2PartialSwitch = pmt2PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                    pmt2KeyboardRange  = pmt2KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                    pmt2KeyboardFadeWidth = pmt2KeyboardFadeWidth.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                    pmt2VelocityRange = pmt2VelocityRange.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                    pmt2VelocityFade = pmt2VelocityFade.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),

                    pmt3PartialSwitch = pmt3PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                    pmt3KeyboardRange  = pmt3KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                    pmt3KeyboardFadeWidth = pmt3KeyboardFadeWidth.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                    pmt3VelocityRange = pmt3VelocityRange.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                    pmt3VelocityFade = pmt3VelocityFade.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),

                    pmt4PartialSwitch = pmt4PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                    pmt4KeyboardRange  = pmt4KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                    pmt4KeyboardFadeWidth = pmt4KeyboardFadeWidth.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                    pmt4VelocityRange = pmt4VelocityRange.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                    pmt4VelocityFade = 0 // TODO = pmt4VelocityFade.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartial>() {
        override val size = Integra7Size(mlsb = 0x01u, lsb = 0x1Au)

        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -48..48)
        val fineTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -50..50)
        val randomPithDepth = EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), RandomPithDepth.values())
        val pan = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        // TODO val panKeyFollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -100..100)
        val panDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val alternatePanDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val envMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), EnvMode.values())
        val delayMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), DelayMode.values())
        val delayTime = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x00u), 0..149) // TODO: 0 - 127, MUSICAL-NOTES |

        val outputLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusSendLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val reverbSendLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))

        val receiveBender = BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val receiveExpression = BooleanValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val receiveHold1 = BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val redamper = BooleanValueField(deviceId, address.offsetBy(lsb = 0x16u))

        val partialControl1Switch1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x17u), OffOnReverse.values())
        val partialControl1Switch2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x18u), OffOnReverse.values())
        val partialControl1Switch3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x19u), OffOnReverse.values())
        val partialControl1Switch4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Au), OffOnReverse.values())
        val partialControl2Switch1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Bu), OffOnReverse.values())
        val partialControl2Switch2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), OffOnReverse.values())
        val partialControl2Switch3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Du), OffOnReverse.values())
        val partialControl2Switch4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Eu), OffOnReverse.values())
        val partialControl3Switch1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), OffOnReverse.values())
        val partialControl3Switch2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), OffOnReverse.values())
        val partialControl3Switch3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x21u), OffOnReverse.values())
        val partialControl3Switch4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x22u), OffOnReverse.values())
        val partialControl4Switch1 = EnumValueField(deviceId, address.offsetBy(lsb = 0x23u), OffOnReverse.values())
        val partialControl4Switch2 = EnumValueField(deviceId, address.offsetBy(lsb = 0x24u), OffOnReverse.values())
        val partialControl4Switch3 = EnumValueField(deviceId, address.offsetBy(lsb = 0x25u), OffOnReverse.values())
        val partialControl4Switch4 = EnumValueField(deviceId, address.offsetBy(lsb = 0x26u), OffOnReverse.values())

        val waveGroupType = EnumValueField(deviceId, address.offsetBy(lsb = 0x27u), WaveGroupType.values())
        val waveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x28u), 0..16384)
        val waveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Cu), 0..16384)
        val waveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x30u), 0..16384)
        val waveGain = EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveFXMSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x35u))
        val waveFXMColor = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x36u), 0..3)
        val waveFXMDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x37u), 0..16)
        val waveTempoSync = BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        // TODO val wavePitchKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x37u), -200..200)

        val pitchEnvDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Au), -12..12)
        val pitchEnvVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0xBAu))
        val pitchEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu))
        val pitchEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Du))
        // TODO val pitchEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Eu), -100..100)
        val pitchEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val pitchEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u))
        val pitchEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val pitchEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u))
        val pitchEnvLevel0 = SignedValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val pitchEnvLevel1 = SignedValueField(deviceId, address.offsetBy(lsb = 0x44u))
        val pitchEnvLevel2 = SignedValueField(deviceId, address.offsetBy(lsb = 0x45u))
        val pitchEnvLevel3 = SignedValueField(deviceId, address.offsetBy(lsb = 0x46u))
        val pitchEnvLevel4 = SignedValueField(deviceId, address.offsetBy(lsb = 0x47u))

        val tvfFilterType = EnumValueField(deviceId, address.offsetBy(lsb = 0x48u), TvfFilterType.values())
        val tvfCutoffFrequency = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x49u))
        // TODO val tvfCutoffKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Au), -200..200)
        val tvfCutoffVelocityCurve = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Bu), 0..7)
        val tvfCutoffVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val tvfResonance = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val tvfResonanceVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))
        val tvfEnvDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Fu))
        val tvfEnvVelocityCurve = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x50u), 0..7)
        val tvfEnvVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x51u))
        val tvfEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x52u))
        val tvfEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x53u))
        // TODO val tvfEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x54u), -100..100)
        val tvfEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x55u))
        val tvfEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x56u))
        val tvfEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x57u))
        val tvfEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x58u))
        val tvfEnvLevel0 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x59u))
        val tvfEnvLevel1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Au))
        val tvfEnvLevel2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val tvfEnvLevel3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Cu))
        val tvfEnvLevel4 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Du))

        // TODO val biasLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x5Eu), -100..100)
        val biasPosition = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Fu))
        val biasDirection = EnumValueField(deviceId, address.offsetBy(lsb = 0x60u), BiasDirection.values())
        val tvaLevelVelocityCurve = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x61u), 0..7)
        val tvaLevelVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x62u))
        val tvaEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x63u))
        val tvaEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x64u))
        // TODO val tvaEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x65u), -100.100)
        val tvaEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x66u))
        val tvaEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x67u))
        val tvaEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x68u))
        val tvaEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x69u))
        val tvaEnvLevel1 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Au))
        val tvaEnvLevel2 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Bu))
        val tvaEnvLevel3 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Cu))

        val lfo1WaveForm = EnumValueField(deviceId, address.offsetBy(lsb = 0x6Du), LfoWaveForm.values())
        val lfo1Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x6Eu), 0..149)
        val lfo1Offset = EnumValueField(deviceId, address.offsetBy(lsb = 0x70u), LfoOffset.values())
        val lfo1RateDetune = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x71u))
        val lfo1DelayTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x72u))
        // TODO val lfo1Keyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x73u), -100..100)
        val lfo1FadeMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x74u), LfoFadeMode.values())
        val lfo1FadeTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x75u))
        val lfo1KeyTrigger = BooleanValueField(deviceId, address.offsetBy(lsb = 0x76u))
        val lfo1PitchDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x77u))
        val lfo1TvfDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x78u))
        val lfo1TvaDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x79u))
        val lfo1PanDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x7Au))

        val lfo2WaveForm = EnumValueField(deviceId, address.offsetBy(lsb = 0x7Bu), LfoWaveForm.values())
        val lfo2Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x7Cu), 0..149)
        val lfo2Offset = EnumValueField(deviceId, address.offsetBy(lsb = 0x7Eu), LfoOffset.values())
        val lfo2RateDetune = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x7Fu))
        val lfo2DelayTime = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x00u))
        // TODO val lfo2Keyfollow = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u), -100..100)
        val lfo2FadeMode = EnumValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x02u), LfoFadeMode.values())
        val lfo2FadeTime = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x03u))
        val lfo2KeyTrigger = BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x04u))
        val lfo2PitchDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val lfo2TvfDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u))
        val lfo2TvaDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val lfo2PanDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u))

        val lfoStepType = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..1)
        val lfoStep1 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au), -36..36)
        val lfoStep2 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -36..36)
        val lfoStep3 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -36..36)
        val lfoStep4 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -36..36)
        val lfoStep5 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), -36..36)
        val lfoStep6 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), -36..36)
        val lfoStep7 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u), -36..36)
        val lfoStep8 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u), -36..36)
        val lfoStep9 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x12u), -36..36)
        val lfoStep10 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u), -36..36)
        val lfoStep11 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x14u), -36..36)
        val lfoStep12 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -36..36)
        val lfoStep13 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u), -36..36)
        val lfoStep14 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u), -36..36)
        val lfoStep15 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u), -36..36)
        val lfoStep16 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u), -36..36)

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthTonePartial {
            assert(startAddress >= address)

            try {
                return PcmSynthTonePartial(
                    level = level.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    chorusTune = chorusTune.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                    fineTune = fineTune.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                    randomPithDepth = randomPithDepth.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),
                    pan = pan.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                    // panKeyFollow = panKeyFollow.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                    panDepth = panDepth.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                    alternatePanDepth = alternatePanDepth.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                    envMode = envMode.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                    delayMode = delayMode.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),
                    delayTime = delayTime.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),

                    outputLevel = outputLevel.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                    chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                    reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),

                    receiveBender = receiveBender.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                    receiveExpression = receiveExpression.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                    receiveHold1 = receiveHold1.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                    redamper = redamper.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),

                    partialControl1Switch1 = partialControl1Switch1.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                    partialControl1Switch2 = partialControl1Switch2.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                    partialControl1Switch3 = partialControl1Switch3.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                    partialControl1Switch4 = partialControl1Switch4.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                    partialControl2Switch1 = partialControl2Switch1.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                    partialControl2Switch2 = partialControl2Switch2.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                    partialControl2Switch3 = partialControl2Switch3.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                    partialControl2Switch4 = partialControl2Switch4.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),
                    partialControl3Switch1 = partialControl3Switch1.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                    partialControl3Switch2 = partialControl3Switch2.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                    partialControl3Switch3 = partialControl3Switch3.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                    partialControl3Switch4 = partialControl3Switch4.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                    partialControl4Switch1 = partialControl4Switch1.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                    partialControl4Switch2 = partialControl4Switch2.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload),
                    partialControl4Switch3 = partialControl4Switch3.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                    partialControl4Switch4 = partialControl4Switch4.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload),

                    waveGroupType = waveGroupType.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                    waveGroupId = waveGroupId.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                    waveNumberL = waveNumberL.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                    waveNumberR = waveNumberR.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                    waveGain = waveGain.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                    waveFXMSwitch = waveFXMSwitch.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                    waveFXMColor = waveFXMColor.interpret(startAddress.offsetBy(lsb = 0x36u), length, payload),
                    waveFXMDepth = waveFXMDepth.interpret(startAddress.offsetBy(lsb = 0x37u), length, payload),
                    waveTempoSync = waveTempoSync.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                    // wavePitchKeyfollow = wavePitchKeyfollow.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),

                    pitchEnvDepth = pitchEnvDepth.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                    pitchEnvVelocitySens = pitchEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                    pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
                    pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.interpret(startAddress.offsetBy(lsb = 0x3Du), length, payload),
                    // pitchEnvTimeKeyfollow = pitchEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                    pitchEnvTime1 = pitchEnvTime1.interpret(startAddress.offsetBy(lsb = 0x3Fu), length, payload),
                    pitchEnvTime2 = pitchEnvTime2.interpret(startAddress.offsetBy(lsb = 0x40u), length, payload),
                    pitchEnvTime3 = pitchEnvTime3.interpret(startAddress.offsetBy(lsb = 0x41u), length, payload),
                    pitchEnvTime4 = pitchEnvTime4.interpret(startAddress.offsetBy(lsb = 0x42u), length, payload),
                    pitchEnvLevel0 = pitchEnvLevel0.interpret(startAddress.offsetBy(lsb = 0x43u), length, payload),
                    pitchEnvLevel1 = pitchEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x44u), length, payload),
                    pitchEnvLevel2 = pitchEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x45u), length, payload),
                    pitchEnvLevel3 = pitchEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x46u), length, payload),
                    pitchEnvLevel4 = pitchEnvLevel4.interpret(startAddress.offsetBy(lsb = 0x47u), length, payload),

                    tvfFilterType = tvfFilterType.interpret(startAddress.offsetBy(lsb = 0x48u), length, payload),
                    tvfCutoffFrequency = tvfCutoffFrequency.interpret(startAddress.offsetBy(lsb = 0x49u), length, payload),
                    // tvfCutoffKeyfollow = tvfCutoffKeyfollow.interpret(startAddress.offsetBy(lsb = 0x4Au), length, payload),
                    tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.interpret(startAddress.offsetBy(lsb = 0x4Bu), length, payload),
                    tvfCutoffVelocitySens = tvfCutoffVelocitySens.interpret(startAddress.offsetBy(lsb = 0x4Cu), length, payload),
                    tvfResonance = tvfResonance.interpret(startAddress.offsetBy(lsb = 0x4Du), length, payload),
                    tvfResonanceVelocitySens = tvfResonanceVelocitySens.interpret(startAddress.offsetBy(lsb = 0x4Eu), length, payload),
                    tvfEnvDepth = tvfEnvDepth.interpret(startAddress.offsetBy(lsb = 0x4Fu), length, payload),
                    tvfEnvVelocityCurve = tvfEnvVelocityCurve.interpret(startAddress.offsetBy(lsb = 0x50u), length, payload),
                    tvfEnvVelocitySens = tvfEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x51u), length, payload),
                    tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.interpret(startAddress.offsetBy(lsb = 0x52u), length, payload),
                    tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.interpret(startAddress.offsetBy(lsb = 0x53u), length, payload),
                    // tvfEnvTimeKeyfollow = tvfEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x54u), length, payload),
                    tvfEnvTime1 = tvfEnvTime1.interpret(startAddress.offsetBy(lsb = 0x55u), length, payload),
                    tvfEnvTime2 = tvfEnvTime2.interpret(startAddress.offsetBy(lsb = 0x56u), length, payload),
                    tvfEnvTime3 = tvfEnvTime3.interpret(startAddress.offsetBy(lsb = 0x57u), length, payload),
                    tvfEnvTime4 = tvfEnvTime4.interpret(startAddress.offsetBy(lsb = 0x58u), length, payload),
                    tvfEnvLevel0 = tvfEnvLevel0.interpret(startAddress.offsetBy(lsb = 0x59u), length, payload),
                    tvfEnvLevel1 = tvfEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x5Au), length, payload),
                    tvfEnvLevel2 = tvfEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x5Bu), length, payload),
                    tvfEnvLevel3 = tvfEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x5Cu), length, payload),
                    tvfEnvLevel4 = tvfEnvLevel4.interpret(startAddress.offsetBy(lsb = 0x5Du), length, payload),

                    //biasLevel = biasLevel.interpret(startAddress.offsetBy(lsb = 0x5Eu), length, payload),
                    biasPosition = biasPosition.interpret(startAddress.offsetBy(lsb = 0x5Fu), length, payload),
                    biasDirection = biasDirection.interpret(startAddress.offsetBy(lsb = 0x60u), length, payload),
                    tvaLevelVelocityCurve = tvaLevelVelocityCurve.interpret(startAddress.offsetBy(lsb = 0x61u), length, payload),
                    tvaLevelVelocitySens = tvaLevelVelocitySens.interpret(startAddress.offsetBy(lsb = 0x62u), length, payload),
                    tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.interpret(startAddress.offsetBy(lsb = 0x63u), length, payload),
                    tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.interpret(startAddress.offsetBy(lsb = 0x64u), length, payload),
                    // tvaEnvTimeKeyfollow = tvaEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x65u), length, payload),
                    tvaEnvTime1 = tvaEnvTime1.interpret(startAddress.offsetBy(lsb = 0x66u), length, payload),
                    tvaEnvTime2 = tvaEnvTime2.interpret(startAddress.offsetBy(lsb = 0x67u), length, payload),
                    tvaEnvTime3 = tvaEnvTime3.interpret(startAddress.offsetBy(lsb = 0x68u), length, payload),
                    tvaEnvTime4 = tvaEnvTime4.interpret(startAddress.offsetBy(lsb = 0x69u), length, payload),
                    tvaEnvLevel1 = tvaEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x6Au), length, payload),
                    tvaEnvLevel2 = tvaEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x6Bu), length, payload),
                    tvaEnvLevel3 = tvaEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x6Cu), length, payload),

                    lfo1WaveForm = lfo1WaveForm.interpret(startAddress.offsetBy(lsb = 0x6Du), length, payload),
                    lfo1Rate = lfo1Rate.interpret(startAddress.offsetBy(lsb = 0x6Eu), length, payload),
                    lfo1Offset = lfo1Offset.interpret(startAddress.offsetBy(lsb = 0x70u), length, payload),
                    lfo1RateDetune = lfo1RateDetune.interpret(startAddress.offsetBy(lsb = 0x71u), length, payload),
                    lfo1DelayTime = lfo1DelayTime.interpret(startAddress.offsetBy(lsb = 0x72u), length, payload),
                    // lfo1Keyfollow = lfo1Keyfollow.interpret(startAddress.offsetBy(lsb = 0x73u), length, payload),
                    lfo1FadeMode = lfo1FadeMode.interpret(startAddress.offsetBy(lsb = 0x74u), length, payload),
                    lfo1FadeTime = lfo1FadeTime.interpret(startAddress.offsetBy(lsb = 0x75u), length, payload),
                    lfo1KeyTrigger = lfo1KeyTrigger.interpret(startAddress.offsetBy(lsb = 0x76u), length, payload),
                    lfo1PitchDepth = lfo1PitchDepth.interpret(startAddress.offsetBy(lsb = 0x77u), length, payload),
                    lfo1TvfDepth = lfo1TvfDepth.interpret(startAddress.offsetBy(lsb = 0x78u), length, payload),
                    lfo1TvaDepth = lfo1TvaDepth.interpret(startAddress.offsetBy(lsb = 0x79u), length, payload),
                    lfo1PanDepth = lfo1PanDepth.interpret(startAddress.offsetBy(lsb = 0x7Au), length, payload),

                    lfo2WaveForm = lfo2WaveForm.interpret(startAddress.offsetBy(lsb = 0x7Bu), length, payload),
                    lfo2Rate = lfo2Rate.interpret(startAddress.offsetBy(lsb = 0x7Cu), length, payload),
                    lfo2Offset = lfo2Offset.interpret(startAddress.offsetBy(lsb = 0x7Eu), length, payload),
                    lfo2RateDetune = lfo2RateDetune.interpret(startAddress.offsetBy(lsb = 0x7Fu), length, payload),
                    lfo2DelayTime = lfo2DelayTime.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x00u), length, payload),
                    // lfo2Keyfollow = lfo2Keyfollow.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x01u), length, payload),
                    lfo2FadeMode = lfo2FadeMode.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x02u), length, payload),
                    lfo2FadeTime = lfo2FadeTime.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x03u), length, payload),
                    lfo2KeyTrigger = lfo2KeyTrigger.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x04u), length, payload),
                    lfo2PitchDepth = lfo2PitchDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x05u), length, payload),
                    lfo2TvfDepth = lfo2TvfDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x06u), length, payload),
                    lfo2TvaDepth = lfo2TvaDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x07u), length, payload),
                    lfo2PanDepth = lfo2PanDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x08u), length, payload),


                    lfoStepType = lfoStepType.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u), length, payload),
                    lfoStep1 = lfoStep1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Au), length, payload),
                    lfoStep2 = lfoStep2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), length, payload),
                    lfoStep3 = lfoStep3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), length, payload),
                    lfoStep4 = lfoStep4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), length, payload),
                    lfoStep5 = lfoStep5.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), length, payload),
                    lfoStep6 = lfoStep6.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), length, payload),
                    lfoStep7 = lfoStep7.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x10u), length, payload),
                    lfoStep8 = lfoStep8.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x11u), length, payload),
                    lfoStep9 = lfoStep9.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x12u), length, payload),
                    lfoStep10 = lfoStep10.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x13u), length, payload),
                    lfoStep11 = lfoStep11.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x14u), length, payload),
                    lfoStep12 = lfoStep12.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x15u), length, payload),
                    lfoStep13 = lfoStep13.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x16u), length, payload),
                    lfoStep14 = lfoStep14.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x17u), length, payload),
                    lfoStep15 = 0, // TODO: lfoStep15.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x18u), length, payload),
                    lfoStep16 = 0 // TODO: lfoStep16.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x19u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address to ${address.offsetBy(size)} size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address to ${address.offsetBy(size)} size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthToneCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommon2>() {
        override val size = Integra7Size(lsb = 0x3Cu)

        val toneCategory = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val undocumented = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x11u), 0..255)
        val phraseOctaveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val tfxSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x33u))
        val phraseNmber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x38u), 0..65535)


        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthToneCommon2 {
            assert(startAddress >= address)

            try {
                return PcmSynthToneCommon2(
                    toneCategory = toneCategory.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                    undocumented = undocumented.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                    phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                    tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x33u), length, payload),
                    phraseNmber = 0 // TODO: phraseNmber.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }
    }

    data class SuperNaturalSynthToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalSynthTone>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x22u, 0x7Fu)

        val common = SuperNaturalSynthToneCommonBuilder(deviceId, address.offsetBy(0x000000))
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u)) // Same as PCM
        val partial1 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u))
        val partial2 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x21u, lsb = 0x00u))
        val partial3 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x22u, lsb = 0x00u))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalSynthTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-S tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalSynthTone(
                common = common.interpret(startAddress, length, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), length, payload),
                partial1 = partial1.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u), length, payload),
                partial2 = partial2.interpret(startAddress.offsetBy(mlsb = 0x21u, lsb = 0x00u), length, payload),
                partial3 = partial3.interpret(startAddress.offsetBy(mlsb = 0x22u, lsb = 0x00u), length, payload),
            )
        }
    }

    data class SuperNaturalSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SupernaturalSynthToneCommon>() {
        override val size = Integra7Size(0x40u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))
        val portamentoSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val portamentoTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val monoSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val octaveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x15u), -3..3)
        val pithBendRangeUp = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u), 0..24)
        val pitchBendRangeDown = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x17u), 0..24)

        val partial1Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val partial1Select = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val partial2Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val partial2Select = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val partial3Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Du))
        val partial3Select = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Eu))

        val ringSwitch = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), RingSwitch.values())
        val tfxSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x20u))

        val unisonSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val portamentoMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x31u), PortamentoMode.values())
        val legatoSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x32u))
        val analogFeel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x34u))
        val waveShape = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x35u))
        val toneCategory = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x36u))
        val phraseNumber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x37u), 0..65535)
        val phraseOctaveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu), -3..3)
        val unisonSize = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), UnisonSize.values())

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalSynthToneCommon {
            assert(startAddress >= address)

            return SupernaturalSynthToneCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                portamentoSwitch = portamentoSwitch.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                portamentoTime = portamentoTime.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                monoSwitch = monoSwitch.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                octaveShift = octaveShift.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                pithBendRangeUp = pithBendRangeUp.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                pitchBendRangeDown = pitchBendRangeDown.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),

                partial1Switch = partial1Switch.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                partial1Select = partial1Select.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                partial2Switch = partial2Switch.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                partial2Select = partial2Select.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                partial3Switch = partial3Switch.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                partial3Select = partial3Select.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),

                ringSwitch = ringSwitch.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),

                unisonSwitch = unisonSwitch.interpret(startAddress.offsetBy(lsb = 0x2Eu), length, payload),
                portamentoMode = portamentoMode.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),
                legatoSwitch = legatoSwitch.interpret(startAddress.offsetBy(lsb = 0x32u), length, payload),
                analogFeel = analogFeel.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                waveShape = waveShape.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                toneCategory = toneCategory.interpret(startAddress.offsetBy(lsb = 0x36u), length, payload),
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x37u), length, payload),
                phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                unisonSize = unisonSize.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
            )
        }
    }

    data class SuperNaturalSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SuperNaturalSynthTonePartial>() {
        override val size = Integra7Size(0x3Du)

        val oscWaveForm = EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), SnSWaveForm.values())
        val oscWaveFormVariation = EnumValueField(deviceId, address.offsetBy(lsb = 0x01u), SnsWaveFormVariation.values())
        val oscPitch = SignedValueField(deviceId, address.offsetBy(lsb = 0x03u), -24..24)
        val oscDetune = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu), -50..50)
        val oscPulseWidthModulationDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val oscPulseWidth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val oscPitchAttackTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u))
        val oscPitchEnvDecay = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x08u))
        val oscPitchEnvDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x09u))

        val filterMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Au), SnsFilterMode.values())
        val filterSlope = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), SnsFilterSlope.values())
        val filterCutoff = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))
//        val filterCutoffKeyflow = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), -100..100)
        val filterEnvVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val filterResonance = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val filterEnvAttackTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val filterEnvDecayTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val filterEnvSustainLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val filterEnvReleaseTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val filterEnvDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val ampLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x15u))
        val ampVelocitySens = SignedValueField(deviceId, address.offsetBy(lsb = 0x16u))
        val ampEnvAttackTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val ampEnvDecayTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u))
        val ampEnvSustainLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val ampEnvReleaseTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val ampPan = SignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), -64..63)

        val lfoShape = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), SnsLfoShape.values())
        val lfoRate = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du))
        val lfoTempoSyncSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Eu))
        val lfoTempoSyncNote = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), SnsLfoTempoSyncNote.values())
        val lfoFadeTime = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val lfoKeyTrigger = BooleanValueField(deviceId, address.offsetBy(lsb = 0x21u))
        val lfoPitchDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val lfoFilterDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x23u))
        val lfoAmpDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x24u))
        val lfoPanDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x25u))

        val modulationShape = EnumValueField(deviceId, address.offsetBy(lsb = 0x26u), SnsLfoShape.values())
        val modulationLfoRate = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u))
        val modulationLfoTempoSyncSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x28u))
        val modulationLfoTempoSyncNote = EnumValueField(deviceId, address.offsetBy(lsb = 0x29u), SnsLfoTempoSyncNote.values())
        val oscPulseWidthShift = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Au))
        val modulationLfoPitchDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Cu))
        val modulationLfoFilterDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val modulationLfoAmpDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val modulationLfoPanDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu))

        val cutoffAftertouchSens = SignedValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val levelAftertouchSens = SignedValueField(deviceId, address.offsetBy(lsb = 0x31u))

        val waveGain = EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveNumber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u), 0..16384)
        val hpfCutoff = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x39u))
        val superSawDetune = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Au))
        val modulationLfoRateControl = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
//        val ampLevelKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu), 100..100)

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SuperNaturalSynthTonePartial {
            assert(this.isCovering(startAddress)) { "Not a SN-S tone definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalSynthTonePartial(
                oscWaveForm = oscWaveForm.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                oscWaveFormVariation = oscWaveFormVariation.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                oscPitch = oscPitch.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),
                oscDetune = oscDetune.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                oscPulseWidthModulationDepth = oscPulseWidthModulationDepth.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                oscPulseWidth = oscPulseWidth.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                oscPitchAttackTime = oscPitchAttackTime.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                oscPitchEnvDecay = oscPitchEnvDecay.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                oscPitchEnvDepth = oscPitchEnvDepth.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),

                filterMode = filterMode.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),
                filterSlope = filterSlope.interpret(startAddress.offsetBy(lsb = 0x0Bu), length, payload),
                filterCutoff = filterCutoff.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
//                filterCutoffKeyflow = filterCutoffKeyflow.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                filterEnvVelocitySens = filterEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                filterResonance = filterResonance.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                filterEnvAttackTime = filterEnvAttackTime.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                filterEnvDecayTime = filterEnvDecayTime.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                filterEnvSustainLevel = filterEnvSustainLevel.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                filterEnvReleaseTime = filterEnvReleaseTime.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                filterEnvDepth = filterEnvDepth.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                ampLevel = ampLevel.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                ampVelocitySens = ampVelocitySens.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                ampEnvAttackTime = ampEnvAttackTime.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                ampEnvDecayTime = ampEnvDecayTime.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                ampEnvSustainLevel = ampEnvSustainLevel.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                ampEnvReleaseTime = ampEnvReleaseTime.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                ampPan = ampPan.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),

                lfoShape = lfoShape.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                lfoRate = lfoRate.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                lfoTempoSyncSwitch = lfoTempoSyncSwitch.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),
                lfoTempoSyncNote = lfoTempoSyncNote.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                lfoFadeTime = lfoFadeTime.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                lfoKeyTrigger = lfoKeyTrigger.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                lfoPitchDepth = lfoPitchDepth.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                lfoFilterDepth = lfoFilterDepth.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                lfoAmpDepth = lfoAmpDepth.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload),
                lfoPanDepth = lfoPanDepth.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),

                modulationShape = modulationShape.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload),
                modulationLfoRate = modulationLfoRate.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                modulationLfoTempoSyncSwitch = modulationLfoTempoSyncSwitch.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                modulationLfoTempoSyncNote = modulationLfoTempoSyncNote.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload),
                oscPulseWidthShift = oscPulseWidthShift.interpret(startAddress.offsetBy(lsb = 0x2Au), length, payload),
                modulationLfoPitchDepth = modulationLfoPitchDepth.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                modulationLfoFilterDepth = modulationLfoFilterDepth.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
                modulationLfoAmpDepth = modulationLfoAmpDepth.interpret(startAddress.offsetBy(lsb = 0x2Eu), length, payload),
                modulationLfoPanDepth = modulationLfoPanDepth.interpret(startAddress.offsetBy(lsb = 0x2Fu), length, payload),

                cutoffAftertouchSens = cutoffAftertouchSens.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                levelAftertouchSens = levelAftertouchSens.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),

                waveGain = waveGain.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                waveNumber = waveNumber.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                hpfCutoff = hpfCutoff.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                superSawDetune = superSawDetune.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                modulationLfoRateControl = modulationLfoRateControl.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
//                ampLevelKeyfollow = ampLevelKeyfollow.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
            )
        }
    }

    data class SuperNaturalAcousticToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalAcousticTone>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x30u, 0x3Cu)

        val common = SuperNaturalAcousticToneCommonBuilder(deviceId, address.offsetBy(0x000000))
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u)) // Same as PCM

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalAcousticTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-A tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalAcousticTone(
                common = common.interpret(startAddress, length, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), length, payload)
            )
        }
    }

    data class SuperNaturalAcousticToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SupernaturalAcousticToneCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))

        val monoPoly = EnumValueField(deviceId, address.offsetBy(lsb = 0x11u), MonoPoly.values())
        val portamentoTimeOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val cutoffOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -64..63)
        val resonanceOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x14u), -64..63)
        val attackTimeOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x15u), -64..63)
        val releaseTimeOffset = SignedValueField(deviceId, address.offsetBy(lsb = 0x16u), -64..63)
        val vibratoRate = SignedValueField(deviceId, address.offsetBy(lsb = 0x17u), -64..63)
        val vibratoDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x18u), -64..63)
        val vibratorDelay = SignedValueField(deviceId, address.offsetBy(lsb = 0x19u), -64..63)
        val octaveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x1Au), -3..3)
        val category = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val phraseNumber = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x1Cu), 0..255)
        val phraseOctaveShift = SignedValueField(deviceId, address.offsetBy(lsb = 0x1Eu), -3..3)

        val tfxSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Fu))

        val instrumentVariation = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val instrumentNumber = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u))

        val modifyParameters = IntRange(0, 31).map {
            UnsignedValueField(deviceId, address.offsetBy(lsb = 0x22u).offsetBy(lsb = 0x01u, factor = it))
        }

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalAcousticToneCommon {
            assert(startAddress >= address)

            return SupernaturalAcousticToneCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                monoPoly = monoPoly.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                portamentoTimeOffset = portamentoTimeOffset.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                cutoffOffset = cutoffOffset.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                resonanceOffset = resonanceOffset.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                attackTimeOffset = attackTimeOffset.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                releaseTimeOffset = releaseTimeOffset.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                vibratoRate = vibratoRate.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                vibratoDepth = vibratoDepth.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                vibratorDelay = vibratorDelay.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                octaveShift = octaveShift.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                category = category.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),

                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),

                instrumentVariation = instrumentVariation.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                instrumentNumber = instrumentNumber.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                modifyParameters = modifyParameters
                    .mapIndexed { idx, fd -> fd.interpret(startAddress.offsetBy(lsb = 0x21u).offsetBy(lsb = 0x01u, factor = idx), length, payload) }
            )
        }
    }

    data class SuperNaturalDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalDrumKit>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x4Du, 0x7Fu)

        val common = SuperNaturalDrumKitCommonBuilder(deviceId, address.offsetBy(0x000000))
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u)) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u))
        val note27 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u))
        val note28 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 1))
        val note29 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 2))

        val note30 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 3))
        val note31 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 4))
        val note32 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 5))
        val note33 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 6))
        val note34 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 7))
        val note35 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 8))
        val note36 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 9))
        val note37 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 10))
        val note38 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 11))
        val note39 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 12))

        val note40 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 13))
        val note41 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 14))
        val note42 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 15))
        val note43 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 16))
        val note44 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 17))
        val note45 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 18))
        val note46 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 19))
        val note47 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 20))
        val note48 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 21))
        val note49 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 22))

        val note50 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 23))
        val note51 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 24))
        val note52 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 25))
        val note53 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 26))
        val note54 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 27))
        val note55 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 28))
        val note56 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 29))
        val note57 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 30))
        val note58 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 31))
        val note59 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 32))

        val note60 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 33))
        val note61 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 34))
        val note62 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 35))
        val note63 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 36))
        val note64 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 37))
        val note65 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 38))
        val note66 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 39))
        val note67 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 40))
        val note68 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 41))
        val note69 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 42))

        val note70 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 43))
        val note71 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 44))
        val note72 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 45))
        val note73 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 46))
        val note74 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 47))
        val note75 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 48))
        val note76 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 49))
        val note77 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 50))
        val note78 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 51))
        val note79 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 52))

        val note80 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 53))
        val note81 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 54))
        val note82 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 55))
        val note83 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 56))
        val note84 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 57))
        val note85 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 58))
        val note86 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 59))
        val note87 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 60))
        val note88 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 61))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalDrumKit(
                common = common.interpret(startAddress, length, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb=0x00u), length, payload),
                commonCompEq = commonCompEq.interpret(startAddress.offsetBy(mlsb = 0x08u, lsb = 0x00u), length, payload),
                notes = listOf(
                     note27.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u), length, payload),
                     note28.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 1), length, payload),
                     note29.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 2), length, payload),

                     note30.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 3), length, payload),
                     note31.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 4), length, payload),
                     note32.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 5), length, payload),
                     note33.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 6), length, payload),
                     note34.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 7), length, payload),
                     note35.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 8), length, payload),
                     note36.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 9), length, payload),
                     note37.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 10), length, payload),
                     note38.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 11), length, payload),
                     note39.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 12), length, payload),

                     note40.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 13), length, payload),
                     note41.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 14), length, payload),
                     note42.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 15), length, payload),
                     note43.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 16), length, payload),
                     note44.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 17), length, payload),
                     note45.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 18), length, payload),
                     note46.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 19), length, payload),
                     note47.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 20), length, payload),
                     note48.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 21), length, payload),
                     note49.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 22), length, payload),

                     note50.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 23), length, payload),
                     note51.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 24), length, payload),
                     note52.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 25), length, payload),
                     note53.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 26), length, payload),
                     note54.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 27), length, payload),
                     note55.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 28), length, payload),
                     note56.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 29), length, payload),
                     note57.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 30), length, payload),
                     note58.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 31), length, payload),
                     note59.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 32), length, payload),

                     note60.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 33), length, payload),
                     note61.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 34), length, payload),
                     note62.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 35), length, payload),
                     note63.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 36), length, payload),
                     note64.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 37), length, payload),
                     note65.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 38), length, payload),
                     note66.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 39), length, payload),
                     note67.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 40), length, payload),
                     note68.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 41), length, payload),
                     note69.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 42), length, payload),

                     note70.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 43), length, payload),
                     note71.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 44), length, payload),
                     note72.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 45), length, payload),
                     note73.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 46), length, payload),
                     note74.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 47), length, payload),
                     note75.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 48), length, payload),
                     note76.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 49), length, payload),
                     note77.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 50), length, payload),
                     note78.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 51), length, payload),
                     note79.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 52), length, payload),

                     note80.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 53), length, payload),
                     note81.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 54), length, payload),
                     note82.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 55), length, payload),
                     note83.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 56), length, payload),
                     note84.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 57), length, payload),
                     note85.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 58), length, payload),
                     note86.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 59), length, payload),
                     note87.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 60), length, payload),
                     note88.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 61), length, payload)
                )
            )
        }
    }

    data class SuperNaturalDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SupernaturalDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val ambienceLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val phraseNo = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val tfx = BooleanValueField(deviceId, address.offsetBy(lsb = 0x13u))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommon {
            assert(startAddress >= address)

            return SupernaturalDrumKitCommon(
                name = name.interpret(startAddress, 0x0C, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                ambienceLevel = ambienceLevel.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                phraseNo = phraseNo.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                tfx = tfx.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload)
            )
        }
    }

    data class SuperNaturalDrumKitCommonCompEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SupernaturalDrumKitCommonCompEq>() {
        override val size = Integra7Size(0x54u)

        val comp1Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val comp1AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x01u), SupernaturalDrumAttackTime.values())
        val comp1ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x02u), SupernaturalDrumReleaseTime.values())
        val comp1Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u))
        val comp1Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumRatio.values())
        val comp1OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u), 0..24)
        val eq1Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val eq1LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x07u), SupernaturalDrumLowFrequency.values())
        val eq1LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x08u), 0..30) // - 15
        val eq1MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x09u), SupernaturalDrumMidFrequency.values())
        val eq1MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Au), 0..30) // - 15
        val eq1MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), SupernaturalDrumMidQ.values())
        val eq1HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Cu), SupernaturalDrumHighFrequency.values())
        val eq1HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), 0..30) // - 15

        val comp2Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val comp2AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Fu), SupernaturalDrumAttackTime.values())
        val comp2ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x10u), SupernaturalDrumReleaseTime.values())
        val comp2Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val comp2Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x12u), SupernaturalDrumRatio.values())
        val comp2OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u), 0..24)
        val eq2Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val eq2LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x15u), SupernaturalDrumLowFrequency.values())
        val eq2LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u), 0..30) // - 15
        val eq2MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x17u), SupernaturalDrumMidFrequency.values())
        val eq2MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u), 0..30) // - 15
        val eq2MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x19u), SupernaturalDrumMidQ.values())
        val eq2HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Au), SupernaturalDrumHighFrequency.values())
        val eq2HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..30) // - 15

        val comp3Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val comp3AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Du), SupernaturalDrumAttackTime.values())
        val comp3ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Eu), SupernaturalDrumReleaseTime.values())
        val comp3Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Fu))
        val comp3Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), SupernaturalDrumRatio.values())
        val comp3OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u), 0..24)
        val eq3Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val eq3LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x23u), SupernaturalDrumLowFrequency.values())
        val eq3LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x24u), 0..30) // - 15
        val eq3MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x25u), SupernaturalDrumMidFrequency.values())
        val eq3MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x26u), 0..30) // - 15
        val eq3MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x27u), SupernaturalDrumMidQ.values())
        val eq3HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x28u), SupernaturalDrumHighFrequency.values())
        val eq3HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x29u), 0..30) // - 15

        val comp4Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Au))
        val comp4AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Bu), SupernaturalDrumAttackTime.values())
        val comp4ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Cu), SupernaturalDrumReleaseTime.values())
        val comp4Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val comp4Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Eu), SupernaturalDrumRatio.values())
        val comp4OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu), 0..24)
        val eq4Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val eq4LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x31u), SupernaturalDrumLowFrequency.values())
        val eq4LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x32u), 0..30) // - 15
        val eq4MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x33u), SupernaturalDrumMidFrequency.values())
        val eq4MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x34u), 0..30) // - 15
        val eq4MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x35u), SupernaturalDrumMidQ.values())
        val eq4HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x36u), SupernaturalDrumHighFrequency.values())
        val eq4HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x37u), 0..30) // - 15

        val comp5Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp5AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), SupernaturalDrumAttackTime.values())
        val comp5ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Au), SupernaturalDrumReleaseTime.values())
        val comp5Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp5Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio.values())
        val comp5OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq5Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq5LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Fu), SupernaturalDrumLowFrequency.values())
        val eq5LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq5MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x41u), SupernaturalDrumMidFrequency.values())
        val eq5MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq5MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ.values())
        val eq5HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x44u), SupernaturalDrumHighFrequency.values())
        val eq5HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

        val comp6Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp6AttackTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), SupernaturalDrumAttackTime.values())
        val comp6ReleaseTime = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Au), SupernaturalDrumReleaseTime.values())
        val comp6Threshold = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp6Ratio = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio.values())
        val comp6OutputGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq6Switch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq6LowFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Fu), SupernaturalDrumLowFrequency.values())
        val eq6LowGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq6MidFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x41u), SupernaturalDrumMidFrequency.values())
        val eq6MidGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq6MidQ = EnumValueField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ.values())
        val eq6HighFrequency = EnumValueField(deviceId, address.offsetBy(lsb = 0x44u), SupernaturalDrumHighFrequency.values())
        val eq6HighGain = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommonCompEq {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit comp/eq-definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SupernaturalDrumKitCommonCompEq(
                comp1Switch = comp1Switch.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                comp1AttackTime = comp1AttackTime.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                comp1ReleaseTime = comp1ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                comp1Threshold = comp1Threshold.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),
                comp1Ratio = comp1Ratio.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                comp1OutputGain = comp1OutputGain.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                eq1Switch = eq1Switch.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                eq1LowFrequency = eq1LowFrequency.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                eq1LowGain = eq1LowGain.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload) - 15,
                eq1MidFrequency = eq1MidFrequency.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),
                eq1MidGain = eq1MidGain.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload) - 15,
                eq1MidQ = eq1MidQ.interpret(startAddress.offsetBy(lsb = 0x0Bu), length, payload),
                eq1HighFrequency = eq1HighFrequency.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                eq1HighGain = eq1HighGain.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload) - 15,

                comp2Switch = comp2Switch.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                comp2AttackTime = comp2AttackTime.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                comp2ReleaseTime = comp2ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                comp2Threshold = comp2Threshold.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                comp2Ratio = comp2Ratio.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                comp2OutputGain = comp2OutputGain.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                eq2Switch = eq2Switch.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                eq2LowFrequency = eq2LowFrequency.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                eq2LowGain = eq2LowGain.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload) - 15,
                eq2MidFrequency = eq2MidFrequency.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                eq2MidGain = eq2MidGain.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload) - 15,
                eq2MidQ = eq2MidQ.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                eq2HighFrequency = eq2HighFrequency.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                eq2HighGain = eq2HighGain.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload) - 15,

                comp3Switch = comp3Switch.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                comp3AttackTime = comp3AttackTime.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                comp3ReleaseTime = comp3ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),
                comp3Threshold = comp3Threshold.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                comp3Ratio = comp3Ratio.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),
                comp3OutputGain = comp3OutputGain.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                eq3Switch = eq3Switch.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                eq3LowFrequency = eq3LowFrequency.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                eq3LowGain = eq3LowGain.interpret(startAddress.offsetBy(lsb = 0x24u), length, payload) - 15,
                eq3MidFrequency = eq3MidFrequency.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),
                eq3MidGain = eq3MidGain.interpret(startAddress.offsetBy(lsb = 0x26u), length, payload) - 15,
                eq3MidQ = eq3MidQ.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                eq3HighFrequency = eq3HighFrequency.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                eq3HighGain = eq3HighGain.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload) - 15,

                comp4Switch = comp4Switch.interpret(startAddress.offsetBy(lsb = 0x2Au), length, payload),
                comp4AttackTime = comp4AttackTime.interpret(startAddress.offsetBy(lsb = 0x2Bu), length, payload),
                comp4ReleaseTime = comp4ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                comp4Threshold = comp4Threshold.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
                comp4Ratio = comp4Ratio.interpret(startAddress.offsetBy(lsb = 0x2Eu), length, payload),
                comp4OutputGain = comp4OutputGain.interpret(startAddress.offsetBy(lsb = 0x2Fu), length, payload),
                eq4Switch = eq4Switch.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                eq4LowFrequency = eq4LowFrequency.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),
                eq4LowGain = eq4LowGain.interpret(startAddress.offsetBy(lsb = 0x32u), length, payload) - 15,
                eq4MidFrequency = eq4MidFrequency.interpret(startAddress.offsetBy(lsb = 0x33u), length, payload),
                eq4MidGain = eq4MidGain.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload) - 15,
                eq4MidQ = eq4MidQ.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                eq4HighFrequency = eq4HighFrequency.interpret(startAddress.offsetBy(lsb = 0x36u), length, payload),
                eq4HighGain = eq4HighGain.interpret(startAddress.offsetBy(lsb = 0x37u), length, payload) - 15,

                comp5Switch = comp5Switch.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                comp5AttackTime = comp5AttackTime.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                comp5ReleaseTime = comp5ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                comp5Threshold = comp5Threshold.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                comp5Ratio = comp5Ratio.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
                comp5OutputGain = comp5OutputGain.interpret(startAddress.offsetBy(lsb = 0x3Du), length, payload),
                eq5Switch = eq5Switch.interpret(startAddress.offsetBy(lsb = 0x3Eu), length, payload),
                eq5LowFrequency = eq5LowFrequency.interpret(startAddress.offsetBy(lsb = 0x3Fu), length, payload),
                eq5LowGain = eq5LowGain.interpret(startAddress.offsetBy(lsb = 0x40u), length, payload) - 15,
                eq5MidFrequency = eq5MidFrequency.interpret(startAddress.offsetBy(lsb = 0x41u), length, payload),
                eq5MidGain = eq5MidGain.interpret(startAddress.offsetBy(lsb = 0x42u), length, payload) - 15,
                eq5MidQ = eq5MidQ.interpret(startAddress.offsetBy(lsb = 0x43u), length, payload),
                eq5HighFrequency = eq5HighFrequency.interpret(startAddress.offsetBy(lsb = 0x44u), length, payload),
                eq5HighGain = eq5HighGain.interpret(startAddress.offsetBy(lsb = 0x45u), length, payload) - 15,

                comp6Switch = comp6Switch.interpret(startAddress.offsetBy(lsb = 0x46u), length, payload),
                comp6AttackTime = comp6AttackTime.interpret(startAddress.offsetBy(lsb = 0x47u), length, payload),
                comp6ReleaseTime = comp6ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x48u), length, payload),
                comp6Threshold = comp6Threshold.interpret(startAddress.offsetBy(lsb = 0x49u), length, payload),
                comp6Ratio = comp6Ratio.interpret(startAddress.offsetBy(lsb = 0x4Au), length, payload),
                comp6OutputGain = comp6OutputGain.interpret(startAddress.offsetBy(lsb = 0x4Bu), length, payload),
                eq6Switch = eq6Switch.interpret(startAddress.offsetBy(lsb = 0x4Cu), length, payload),
                eq6LowFrequency = eq6LowFrequency.interpret(startAddress.offsetBy(lsb = 0x4Du), length, payload),
                eq6LowGain = eq6LowGain.interpret(startAddress.offsetBy(lsb = 0x4Eu), length, payload) - 15,
                eq6MidFrequency = eq6MidFrequency.interpret(startAddress.offsetBy(lsb = 0x4Fu), length, payload),
                eq6MidGain = eq6MidGain.interpret(startAddress.offsetBy(lsb = 0x50u), length, payload) - 15,
                eq6MidQ = eq6MidQ.interpret(startAddress.offsetBy(lsb = 0x51u), length, payload),
                eq6HighFrequency = eq6HighFrequency.interpret(startAddress.offsetBy(lsb = 0x52u), length, payload),
                eq6HighGain = 0 // TODO eq6HighGain.interpret(startAddress.offsetBy(lsb = 0x53u), length, payload) - 15,
            )
        }
    }

    data class SuperNaturalDrumKitNoteBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SuperNaturalDrumKitNote>() {
        override val size = Integra7Size(0x13u)

        val instrumentNumber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x00u), 0..512)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x04u))
        val pan = SignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val chorusSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val reverbSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u))
        val tune = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x08u), 8..248) // TODO: convert!
        val attack = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..100)
        val decay = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), -63..0)
        val brilliance = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu), -15..12)
        val variation = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Fu), SuperNaturalDrumToneVariation.values())
        val dynamicRange = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u), 0..63)
        val stereoWidth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val outputAssign = EnumValueField(deviceId, address.offsetBy(lsb = 0x12u), SuperNaturalDrumToneOutput.values())

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SuperNaturalDrumKitNote {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit note-definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalDrumKitNote(
                instrumentNumber = instrumentNumber.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                pan = pan.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                tune = tune.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                attack = attack.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                decay = decay.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                brilliance = brilliance.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                variation = variation.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                dynamicRange = dynamicRange.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                stereoWidth = stereoWidth.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                outputAssign = SuperNaturalDrumToneOutput.PART // TODO outputAssign.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
            )
        }
    }

    data class PcmDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<PcmDrumKit>() {
        override val size = Integra7Size(0x00u, 0x02u, 0x7Fu, 0x7Fu)

        val common = PcmDrumKitCommonBuilder(deviceId, address.offsetBy(0x000000))
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u)) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u)) // Same as SN-D
        val keys = IntRange(0, 78) // key 21 .. 108
            .map { PcmDrumKitPartialBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = it))  }
//        | 00 10 00 | PCM Drum Kit Partial (Key # 21) |
//        | 00 12 00 | PCM Drum Kit Partial (Key # 22) |
//        | : | |
//        | 01 3E 00 | PCM Drum Kit Partial (Key # 108) |
//        | 02 00 00 | PCM Drum Kit Common 2

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): PcmDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM Drum kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmDrumKit(
                common = common.interpret(startAddress, length, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb=0x00u), length, payload),
                keys = keys
                    .mapIndexed { index, b -> b.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = index), length, payload) }
            )
        }
    }

    data class PcmDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmDrumKitCommon {
            assert(startAddress >= address)

            return PcmDrumKitCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
            )
        }
    }

    data class PcmDrumKitPartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmDrumKitPartial>() {
        override val size = Integra7Size(mlsb = 0x01u, lsb = 0x43u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)

        val assignType = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Cu), PcmDrumKitAssignType.values())
        val muteGroup = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0D0u), 0..31)

        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val coarseTune = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val fineTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x10u), -50..50)
        val randomPitchDepth = EnumValueField(deviceId, address.offsetBy(lsb = 0x11u), RandomPithDepth.values())
        val pan = SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val randomPanDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u), 0..63)
        val alternatePanDepth = SignedValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val envMode = EnumValueField(deviceId, address.offsetBy(lsb = 0x15u), EnvMode.values())

        val outputLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u))
        val chorusSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val reverbSendLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val outputAssign = EnumValueField(deviceId, address.offsetBy(lsb = 0x1Bu), SuperNaturalDrumToneOutput.values())

        val pitchBendRange = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..48)
        val receiveExpression = BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Du))
        val receiveHold1 = BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))

        val wmtVelocityControl = EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), WmtVelocityControl.values())

        val wmt1WaveSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x21u))
        val wmt1WaveGroupType = EnumValueField(deviceId, address.offsetBy(lsb = 0x22u), WaveGroupType.values())
        val wmt1WaveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x23u), 0..16384)
        val wmt1WaveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x27u), 0..16384)
        val wmt1WaveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Bu), 0..16384)
        val wmt1WaveGain = EnumValueField(deviceId, address.offsetBy(lsb = 0x2Fu), WaveGain.values())
        val wmt1WaveFxmSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val wmt1WaveFxmColor = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x31u), 0..3)
        val wmt1WaveFxmDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x32u), 0..16)
        val wmt1WaveTempoSync = BooleanValueField(deviceId, address.offsetBy(lsb = 0x33u))
        val wmt1WaveCoarseTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x34u), -48..48)
        val wmt1WaveFineTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x35u), -50..50)
        val wmt1WavePan = SignedValueField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
        val wmt1WaveRandomPanSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x37u))
        val wmt1WaveAlternatePanSwitch = EnumValueField(deviceId, address.offsetBy(lsb = 0x38u), OffOnReverse.values())
        val wmt1WaveLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x39u))
        val wmt1VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Au))
        val wmt1VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Cu))

        val wmt2WaveSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val wmt2WaveGroupType = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Fu), WaveGroupType.values())
        val wmt2WaveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x40u), 0..16384)
        val wmt2WaveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x44u), 0..16384)
        val wmt2WaveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x48u), 0..16384)
        val wmt2WaveGain = EnumValueField(deviceId, address.offsetBy(lsb = 0x4Cu), WaveGain.values())
        val wmt2WaveFxmSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val wmt2WaveFxmColor = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..3)
        val wmt2WaveFxmDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Fu), 0..16)
        val wmt2WaveTempoSync = BooleanValueField(deviceId, address.offsetBy(lsb = 0x50u))
        val wmt2WaveCoarseTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x51u), -48..48)
        val wmt2WaveFineTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x52u), -50..50)
        val wmt2WavePan = SignedValueField(deviceId, address.offsetBy(lsb = 0x53u), -64..63)
        val wmt2WaveRandomPanSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x54u))
        val wmt2WaveAlternatePanSwitch = EnumValueField(deviceId, address.offsetBy(lsb = 0x55u), OffOnReverse.values())
        val wmt2WaveLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x56u))
        val wmt2VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x57u))
        val wmt2VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x59u))

        val wmt3WaveSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val wmt3WaveGroupType = EnumValueField(deviceId, address.offsetBy(lsb = 0x0Cu), WaveGroupType.values())
        val wmt3WaveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du), 0..16384)
        val wmt3WaveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u), 0..16384)
        val wmt3WaveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u), 0..16384)
        val wmt3WaveGain = EnumValueField(deviceId, address.offsetBy(lsb = 0x69u), WaveGain.values())
        val wmt3WaveFxmSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x6Au))
        val wmt3WaveFxmColor = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Bu), 0..3)
        val wmt3WaveFxmDepth = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Cu), 0..16)
        val wmt3WaveTempoSync = BooleanValueField(deviceId, address.offsetBy(lsb = 0x6Du))
        val wmt3WaveCoarseTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x6Eu), -48..48)
        val wmt3WaveFineTune = SignedValueField(deviceId, address.offsetBy(lsb = 0x6Fu), -50..50)
        val wmt3WavePan = SignedValueField(deviceId, address.offsetBy(lsb = 0x70u), -64..63)
        val wmt3WaveRandomPanSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x71u))
        val wmt3WaveAlternatePanSwitch = EnumValueField(deviceId, address.offsetBy(lsb = 0x72u), OffOnReverse.values())
        val wmt3WaveLevel = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x73u))
        val wmt3VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x74u))
        val wmt3VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x76u))

        val wmt4WaveSwitch = BooleanValueField(deviceId, address.offsetBy(lsb = 0x78u))
        val wmt4WaveGroupType = EnumValueField(deviceId, address.offsetBy(lsb = 0x79u), WaveGroupType.values())
        val wmt4WaveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Au), 0..16384)
        val wmt4WaveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Eu), 0..16384)
        val wmt4WaveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x02u), 0..16384)
        val wmt4WaveGain = EnumValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u), WaveGain.values())
        val wmt4WaveFxmSwitch = BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val wmt4WaveFxmColor = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u), 0..3)
        val wmt4WaveFxmDepth = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..16)
        val wmt4WaveTempoSync = BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au))
        val wmt4WaveCoarseTune = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -48..48)
        val wmt4WaveFineTune = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -50..50)
        val wmt4WavePan = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -64..63)
        val wmt4WaveRandomPanSwitch = BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu))
        val wmt4WaveAlternatePanSwitch = EnumValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), OffOnReverse.values())
        val wmt4WaveLevel = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u))
        val wmt4VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u))
        val wmt4VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u))

        val pitchEnvDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -12..12)
        val pitchEnvVelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u))
        val pitchEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u))
        val pitchEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u))

        val pitchEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u))
        val pitchEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Au))
        val pitchEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Bu))
        val pitchEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Cu))

        val pitchEnvLevel0 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Du))
        val pitchEnvLevel1 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Eu))
        val pitchEnvLevel2 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Fu))
        val pitchEnvLevel3 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x20u))
        val pitchEnvLevel4 = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x21u))

        val tvfFilterType = EnumValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x22u), TvfFilterType.values())
        val tvfCutoffFrequency = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x23u))
        val tvfCutoffVelocityCurve = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x24u), 0..7)
        val tvfCutoffVelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x25u))
        val tvfResonance = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x26u))
        val tvfResonanceVelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x27u))
        val tvfEnvDepth = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x28u))
        val tvfEnvVelocityCurveType = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x29u), 0..7)
        val tvfEnvVelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Au))
        val tvfEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Bu))
        val tvfEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Cu))
        val tvfEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Du))
        val tvfEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Eu))
        val tvfEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Fu))
        val tvfEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x30u))
        val tvfEnvLevel0 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x31u))
        val tvfEnvLevel1 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x32u))
        val tvfEnvLevel2 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x33u))
        val tvfEnvLevel3 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x34u))
        val tvfEnvLevel4 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x35u))

        val tvaLevelVelocityCurve = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x36u), 0..7)
        val tvaLevelVelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x37u))
        val tvaEnvTime1VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x38u))
        val tvaEnvTime4VelocitySens = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x39u))
        val tvaEnvTime1 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Au))
        val tvaEnvTime2 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Bu))
        val tvaEnvTime3 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Cu))
        val tvaEnvTime4 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Du))
        val tvaEnvLevel1 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Eu))
        val tvaEnvLevel2 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Fu))
        val tvaEnvLevel3 = UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x40u))

        val oneShotMode = BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x41u))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmDrumKitPartial {
            assert(startAddress >= address)

            return PcmDrumKitPartial(
                name = name.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),

                assignType = assignType.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                muteGroup = muteGroup.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),

                level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                coarseTune = coarseTune.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                fineTune = fineTune.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                randomPitchDepth = randomPitchDepth.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                pan = pan.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
                randomPanDepth = randomPanDepth.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                alternatePanDepth = alternatePanDepth.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                envMode = envMode.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),

                outputLevel = outputLevel.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                outputAssign = outputAssign.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),

                pitchBendRange = pitchBendRange.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                receiveExpression = receiveExpression.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                receiveHold1 = receiveHold1.interpret(startAddress.offsetBy(lsb = 0x1Eu), length, payload),

                wmtVelocityControl = wmtVelocityControl.interpret(startAddress.offsetBy(lsb = 0x20u), length, payload),

                wmt1WaveSwitch = wmt1WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                wmt1WaveGroupType = wmt1WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x22u), length, payload),
                wmt1WaveGroupId = wmt1WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                wmt1WaveNumberL = wmt1WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                wmt1WaveNumberR = wmt1WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x2Bu), length, payload),
                wmt1WaveGain = wmt1WaveGain.interpret(startAddress.offsetBy(lsb = 0x2Fu), length, payload),
                wmt1WaveFxmSwitch = wmt1WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                wmt1WaveFxmColor = wmt1WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),
                wmt1WaveFxmDepth = wmt1WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x32u), length, payload),
                wmt1WaveTempoSync = wmt1WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x33u), length, payload),
                wmt1WaveCoarseTune = wmt1WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                wmt1WaveFineTune = wmt1WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                wmt1WavePan = wmt1WavePan.interpret(startAddress.offsetBy(lsb = 0x36u), length, payload),
                wmt1WaveRandomPanSwitch = wmt1WaveRandomPanSwitch.interpret(startAddress.offsetBy(lsb = 0x37u), length, payload),
                wmt1WaveAlternatePanSwitch = wmt1WaveAlternatePanSwitch.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                wmt1WaveLevel = wmt1WaveLevel.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                wmt1VelocityRange = wmt1VelocityRange.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                wmt1VelocityFadeWidth = wmt1VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),

                wmt2WaveSwitch = wmt2WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x3Eu), length, payload),
                wmt2WaveGroupType = wmt2WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x3Fu), length, payload),
                wmt2WaveGroupId = wmt2WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x40u), length, payload),
                wmt2WaveNumberL = wmt2WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x44u), length, payload),
                wmt2WaveNumberR = wmt2WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x48u), length, payload),
                wmt2WaveGain = wmt2WaveGain.interpret(startAddress.offsetBy(lsb = 0x4Cu), length, payload),
                wmt2WaveFxmSwitch = wmt2WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x4Du), length, payload),
                wmt2WaveFxmColor = wmt2WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x4Eu), length, payload),
                wmt2WaveFxmDepth = wmt2WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x4Fu), length, payload),
                wmt2WaveTempoSync = wmt2WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x50u), length, payload),
                wmt2WaveCoarseTune = wmt2WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x51u), length, payload),
                wmt2WaveFineTune = wmt2WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x52u), length, payload),
                wmt2WavePan = wmt2WavePan.interpret(startAddress.offsetBy(lsb = 0x53u), length, payload),
                wmt2WaveRandomPanSwitch = wmt2WaveRandomPanSwitch.interpret(startAddress.offsetBy(lsb = 0x54u), length, payload),
                wmt2WaveAlternatePanSwitch = wmt2WaveAlternatePanSwitch.interpret(startAddress.offsetBy(lsb = 0x55u), length, payload),
                wmt2WaveLevel = wmt2WaveLevel.interpret(startAddress.offsetBy(lsb = 0x56u), length, payload),
                wmt2VelocityRange = wmt2VelocityRange.interpret(startAddress.offsetBy(lsb = 0x57u), length, payload),
                wmt2VelocityFadeWidth = wmt2VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x59u), length, payload),

                wmt3WaveSwitch = wmt3WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x5Bu), length, payload),
                wmt3WaveGroupType = wmt3WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x5Cu), length, payload),
                wmt3WaveGroupId = wmt3WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x5Du), length, payload),
                wmt3WaveNumberL = wmt3WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x61u), length, payload),
                wmt3WaveNumberR = wmt3WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x65u), length, payload),
                wmt3WaveGain = wmt3WaveGain.interpret(startAddress.offsetBy(lsb = 0x69u), length, payload),
                wmt3WaveFxmSwitch = wmt3WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x6Au), length, payload),
                wmt3WaveFxmColor = wmt3WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x6Bu), length, payload),
                wmt3WaveFxmDepth = wmt3WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x6Cu), length, payload),
                wmt3WaveTempoSync = wmt3WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x6Du), length, payload),
                wmt3WaveCoarseTune = wmt3WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x6Eu), length, payload),
                wmt3WaveFineTune = wmt3WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x6Fu), length, payload),
                wmt3WavePan = wmt3WavePan.interpret(startAddress.offsetBy(lsb = 0x70u), length, payload),
                wmt3WaveRandomPanSwitch = wmt3WaveRandomPanSwitch.interpret(startAddress.offsetBy(lsb = 0x71u), length, payload),
                wmt3WaveAlternatePanSwitch = wmt3WaveAlternatePanSwitch.interpret(startAddress.offsetBy(lsb = 0x72u), length, payload),
                wmt3WaveLevel = wmt3WaveLevel.interpret(startAddress.offsetBy(lsb = 0x73u), length, payload),
                wmt3VelocityRange = wmt3VelocityRange.interpret(startAddress.offsetBy(lsb = 0x74u), length, payload),
                wmt3VelocityFadeWidth = wmt3VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x76u), length, payload),

                wmt4WaveSwitch = wmt4WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x78u), length, payload),
                wmt4WaveGroupType = wmt4WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x79u), length, payload),
                wmt4WaveGroupId = wmt4WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x7Au), length, payload),
                wmt4WaveNumberL = wmt4WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x7Eu), length, payload),
                wmt4WaveNumberR = wmt4WaveNumberR.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x02u), length, payload),
                wmt4WaveGain = wmt4WaveGain.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x06u), length, payload),
                wmt4WaveFxmSwitch = wmt4WaveFxmSwitch.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x07u), length, payload),
                wmt4WaveFxmColor = wmt4WaveFxmColor.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x08u), length, payload),
                wmt4WaveFxmDepth = wmt4WaveFxmDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u), length, payload),
                wmt4WaveTempoSync = wmt4WaveTempoSync.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Au), length, payload),
                wmt4WaveCoarseTune = wmt4WaveCoarseTune.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), length, payload),
                wmt4WaveFineTune = wmt4WaveFineTune.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), length, payload),
                wmt4WavePan = wmt4WavePan.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), length, payload),
                wmt4WaveRandomPanSwitch = wmt4WaveRandomPanSwitch.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), length, payload),
                wmt4WaveAlternatePanSwitch = wmt4WaveAlternatePanSwitch.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), length, payload),
                wmt4WaveLevel = wmt4WaveLevel.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x10u), length, payload),
                wmt4VelocityRange = wmt4VelocityRange.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x11u), length, payload),
                wmt4VelocityFadeWidth = wmt4VelocityFadeWidth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x13u), length, payload),

                pitchEnvDepth = pitchEnvDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x15u), length, payload),
                pitchEnvVelocitySens = pitchEnvVelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x16u), length, payload),
                pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x17u), length, payload),
                pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x18u), length, payload),

                pitchEnvTime1 = pitchEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x19u), length, payload),
                pitchEnvTime2 = pitchEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Au), length, payload),
                pitchEnvTime3 = pitchEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Bu), length, payload),
                pitchEnvTime4 = pitchEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Cu), length, payload),

                pitchEnvLevel0 = pitchEnvLevel0.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Du), length, payload),
                pitchEnvLevel1 = pitchEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Eu), length, payload),
                pitchEnvLevel2 = pitchEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Fu), length, payload),
                pitchEnvLevel3 = pitchEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x20u), length, payload),
                pitchEnvLevel4 = pitchEnvLevel4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x21u), length, payload),

                tvfFilterType = tvfFilterType.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x22u), length, payload),
                tvfCutoffFrequency = tvfCutoffFrequency.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x23u), length, payload),
                tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x24u), length, payload),
                tvfCutoffVelocitySens = tvfCutoffVelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x25u), length, payload),
                tvfResonance = tvfResonance.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x26u), length, payload),
                tvfResonanceVelocitySens = tvfResonanceVelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x27u), length, payload),
                tvfEnvDepth = tvfEnvDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x28u), length, payload),
                tvfEnvVelocityCurveType = tvfEnvVelocityCurveType.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x29u), length, payload),
                tvfEnvVelocitySens = tvfEnvVelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Au), length, payload),
                tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Bu), length, payload),
                tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Cu), length, payload),
                tvfEnvTime1 = tvfEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Du), length, payload),
                tvfEnvTime2 = tvfEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Eu), length, payload),
                tvfEnvTime3 = tvfEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Fu), length, payload),
                tvfEnvTime4 = tvfEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x30u), length, payload),
                tvfEnvLevel0 = tvfEnvLevel0.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x31u), length, payload),
                tvfEnvLevel1 = tvfEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x32u), length, payload),
                tvfEnvLevel2 = tvfEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x33u), length, payload),
                tvfEnvLevel3 = tvfEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x34u), length, payload),
                tvfEnvLevel4 = tvfEnvLevel4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x35u), length, payload),

                tvaLevelVelocityCurve = tvaLevelVelocityCurve.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x36u), length, payload),
                tvaLevelVelocitySens = tvaLevelVelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x37u), length, payload),
                tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x38u), length, payload),
                tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x39u), length, payload),
                tvaEnvTime1 = tvaEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Au), length, payload),
                tvaEnvTime2 = tvaEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Bu), length, payload),
                tvaEnvTime3 = tvaEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Cu), length, payload),
                tvaEnvTime4 = tvaEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Du), length, payload),
                tvaEnvLevel1 = tvaEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Eu), length, payload),
                tvaEnvLevel2 = tvaEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Fu), length, payload),
                tvaEnvLevel3 = tvaEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x40u), length, payload),

                oneShotMode = oneShotMode.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x41u), length, payload),
            )
        }
    }
}

// ----------------------------------------------------


data class Integra7Address(val msb: UByte, val mmsb: UByte, val mlsb: UByte, val lsb: UByte): UByteSerializable, Comparable<Integra7Address> {
    companion object {
        fun fromFullByteAddress(address: Int): Integra7Address {
            val msb: UByte = (address / (0x80 * 0x80 * 0x80)).toUByte()
            val mmsb: UByte = ((address / (0x80 * 0x80)).toUInt() and 0x7Fu).toUByte()
            val mlsb: UByte = ((address / 0x80).toUInt() and 0x7Fu).toUByte()
            val lsb: UByte = (address.toUInt() and 0x7Fu).toUByte()
            return Integra7Address(msb, mmsb, mlsb, lsb)
        }
    }

    constructor(address: Int): this(
        msb = (address / 0x1000000).toUByte(),
        mmsb = ((address / 0x10000).toUInt() and 0xFFu).toUByte(),
        mlsb = ((address / 0x100).toUInt() and 0xFFu).toUByte(),
        lsb = (address.toUInt() and 0xFFu).toUByte())

    init {
        assert(msb < 0x80u)
        assert(mmsb < 0x80u)
        assert(mlsb < 0x80u)
        assert(lsb < 0x80u)
    }

    override fun bytes(): UByteArray =
        ubyteArrayOf(msb, mmsb, mlsb, lsb)

    fun offsetBy(offset: Integra7Size, factor: Int = 1): Integra7Address =
        offsetBy(offset.msb, offset.mmsb, offset.mlsb, offset.lsb, factor)

    fun offsetBy(offset: Int, factor: Int = 1): Integra7Address =
        offsetBy(
            msb = (offset / 0x1000000).toUByte(),
            mmsb = ((offset / 0x10000).toUInt() and 0xFFu).toUByte(),
            mlsb = ((offset / 0x100).toUInt() and 0xFFu).toUByte(),
            lsb = (offset.toUInt() and 0xFFu).toUByte(),
            factor
        )

    fun offsetBy(msb: UByte = 0x00u, mmsb: UByte = 0x00u, mlsb: UByte = 0x00u, lsb: UByte, factor: Int = 1): Integra7Address {
        val newLsb: UByte = (this.lsb + lsb).toUByte()
        val newMlsb: UByte = (this.mlsb + mlsb + if (newLsb > 0x7Fu) 0x01u else 0x00u).toUByte()
        val newMmsb: UByte = (this.mmsb + mmsb + if (newMlsb > 0x7Fu) 0x01u else 0x00u).toUByte()
        val newMsb: UByte = (this.msb + msb + if (newMmsb > 0x7Fu) 0x01u else 0x00u).toUByte()
        assert(newMsb < 0x80u)

        return when {
            factor < 0 -> throw IllegalArgumentException()
            factor == 0 -> this
            factor == 1 -> Integra7Address(newMsb, newMmsb and 0x7Fu, newMlsb and 0x7Fu, newLsb and 0x7Fu)
            else -> Integra7Address(newMsb, newMmsb and 0x7Fu, newMlsb and 0x7Fu, newLsb and 0x7Fu)
                .offsetBy(msb, mmsb, mlsb, lsb, factor - 1)
        }
    }

    /*
     * Returns the size of the address-range
     */
    operator fun minus(other: Integra7Address): Integra7Size {
        val ourLength = ((((msb * 0x80u) + mmsb) * 0x80u) + mlsb) * 0x80u + lsb
        val otherLength = ((((other.msb * 0x80u) + other.mmsb) * 0x80u) + other.mlsb) * 0x80u + other.lsb
        return Integra7Size(ourLength - otherLength)
    }

    fun fullByteAddress(): Int =
        (((((msb * 0x80u) + mmsb) * 0x80u) + mlsb) * 0x80u + lsb).toInt()

    override fun compareTo(other: Integra7Address): Int =
        Comparator
            .comparing(Integra7Address::msb)
            .thenComparing(Integra7Address::mmsb)
            .thenComparing(Integra7Address::mlsb)
            .thenComparing(Integra7Address::lsb)
            .compare(this, other)

    fun rangeName(): String =
        when (msb * 0x1000000u + mmsb * 0x10000u + mlsb * 0x100u + lsb) {
            in 0x19000000u..0x1900FFFFu -> "'Part 1 PCM Synth tone'"
            in 0x19010000u..0x1901FFFFu -> "'Part 1 superNATURAL Synth tone'"
            in 0x19020000u..0x1902FFFFu -> "'Part 1 superNATURAL Acoustic tone'"
            in 0x19030000u..0x190FFFFFu -> "'Part 1 superNATURAL Drum kit'"
            in 0x19100000u..0x191FFFFFu -> "'Part 1 PCM Drum kit'"

            in 0x19200000u..0x1920FFFFu -> "'Part 2 PCM Synth tone'"
            in 0x19210000u..0x1921FFFFu -> "'Part 2 superNATURAL Synth tone'"
            in 0x19220000u..0x1922FFFFu -> "'Part 2 superNATURAL Acoustic tone'"
            in 0x19230000u..0x192FFFFFu -> "'Part 2 superNATURAL Drum kit'"
            in 0x19300000u..0x193FFFFFu -> "'Part 2 PCM Drum kit'"

            in 0x19400000u..0x1940FFFFu -> "'Part 3 PCM Synth tone'"
            in 0x19410000u..0x1941FFFFu -> "'Part 3 superNATURAL Synth tone'"
            in 0x19420000u..0x1942FFFFu -> "'Part 3 superNATURAL Acoustic tone'"
            in 0x19430000u..0x194FFFFFu -> "'Part 3 superNATURAL Drum kit'"
            in 0x19500000u..0x195FFFFFu -> "'Part 3 PCM Drum kit'"

            in 0x19600000u..0x1960FFFFu -> "'Part 4 PCM Synth tone'"
            in 0x19610000u..0x1961FFFFu -> "'Part 4 superNATURAL Synth tone'"
            in 0x19620000u..0x1962FFFFu -> "'Part 4 superNATURAL Acoustic tone'"
            in 0x19630000u..0x196FFFFFu -> "'Part 4 superNATURAL Drum kit'"
            in 0x19700000u..0x197FFFFFu -> "'Part 4 PCM Drum kit'"

            in 0x1A000000u..0x1A00FFFFu -> "'Part 5 PCM Synth tone'"
            in 0x1A010000u..0x1A01FFFFu -> "'Part 5 superNATURAL Synth tone'"
            in 0x1A020000u..0x1A02FFFFu -> "'Part 5 superNATURAL Acoustic tone'"
            in 0x1A030000u..0x1A0FFFFFu -> "'Part 5 superNATURAL Drum kit'"
            in 0x1A100000u..0x1A1FFFFFu -> "'Part 5 PCM Drum kit'"

            in 0x1A200000u..0x1A20FFFFu -> "'Part 6 PCM Synth tone'"
            in 0x1A210000u..0x1A21FFFFu -> "'Part 6 superNATURAL Synth tone'"
            in 0x1A220000u..0x1A22FFFFu -> "'Part 6 superNATURAL Acoustic tone'"
            in 0x1A230000u..0x1A2FFFFFu -> "'Part 6 superNATURAL Drum kit'"
            in 0x1A300000u..0x1A3FFFFFu -> "'Part 6 PCM Drum kit'"

            in 0x1A400000u..0x1A40FFFFu -> "'Part 7 PCM Synth tone'"
            in 0x1A410000u..0x1A41FFFFu -> "'Part 7 superNATURAL Synth tone'"
            in 0x1A420000u..0x1A42FFFFu -> "'Part 7 superNATURAL Acoustic tone'"
            in 0x1A430000u..0x1A4FFFFFu -> "'Part 7 superNATURAL Drum kit'"
            in 0x1A500000u..0x1A5FFFFFu -> "'Part 7 PCM Drum kit'"

            in 0x1A600000u..0x1A60FFFFu -> "'Part 8 PCM Synth tone'"
            in 0x1A610000u..0x1A61FFFFu -> "'Part 8 superNATURAL Synth tone'"
            in 0x1A620000u..0x1A62FFFFu -> "'Part 8 superNATURAL Acoustic tone'"
            in 0x1A630000u..0x1A6FFFFFu -> "'Part 8 superNATURAL Drum kit'"
            in 0x1A700000u..0x1A7FFFFFu -> "'Part 8 PCM Drum kit'"

            in 0x1B000000u..0x1B00FFFFu -> "'Part 9 PCM Synth tone'"
            in 0x1B010000u..0x1B01FFFFu -> "'Part 9 superNATURAL Synth tone'"
            in 0x1B020000u..0x1B02FFFFu -> "'Part 9 superNATURAL Acoustic tone'"
            in 0x1B030000u..0x1B0FFFFFu -> "'Part 9 superNATURAL Drum kit'"
            in 0x1B100000u..0x1B1FFFFFu -> "'Part 9 PCM Drum kit'"

            in 0x1B200000u..0x1B20FFFFu -> "'Part 10 PCM Synth tone'"
            in 0x1B210000u..0x1B21FFFFu -> "'Part 10 superNATURAL Synth tone'"
            in 0x1B220000u..0x1B22FFFFu -> "'Part 10 superNATURAL Acoustic tone'"
            in 0x1B230000u..0x1B2FFFFFu -> "'Part 10 superNATURAL Drum kit'"
            in 0x1B300000u..0x1B3FFFFFu -> "'Part 10 PCM Drum kit'"

            in 0x1B400000u..0x1B40FFFFu -> "'Part 11 PCM Synth tone'"
            in 0x1B410000u..0x1B41FFFFu -> "'Part 11 superNATURAL Synth tone'"
            in 0x1B420000u..0x1B42FFFFu -> "'Part 11 superNATURAL Acoustic tone'"
            in 0x1B430000u..0x1B4FFFFFu -> "'Part 11 superNATURAL Drum kit'"
            in 0x1B500000u..0x1B5FFFFFu -> "'Part 11 PCM Drum kit'"

            in 0x1B600000u..0x1B60FFFFu -> "'Part 12 PCM Synth tone'"
            in 0x1B610000u..0x1B61FFFFu -> "'Part 12 superNATURAL Synth tone'"
            in 0x1B620000u..0x1B62FFFFu -> "'Part 12 superNATURAL Acoustic tone'"
            in 0x1B630000u..0x1B6FFFFFu -> "'Part 12 superNATURAL Drum kit'"
            in 0x1B700000u..0x1B7FFFFFu -> "'Part 12 PCM Drum kit'"

            in 0x1C000000u..0x1C00FFFFu -> "'Part 13 PCM Synth tone'"
            in 0x1C010000u..0x1C01FFFFu -> "'Part 13 superNATURAL Synth tone'"
            in 0x1C020000u..0x1C02FFFFu -> "'Part 13 superNATURAL Acoustic tone'"
            in 0x1C030000u..0x1C0FFFFFu -> "'Part 13 superNATURAL Drum kit'"
            in 0x1C100000u..0x1C1FFFFFu -> "'Part 13 PCM Drum kit'"

            in 0x1C200000u..0x1C20FFFFu -> "'Part 14 PCM Synth tone'"
            in 0x1C210000u..0x1C21FFFFu -> "'Part 14 superNATURAL Synth tone'"
            in 0x1C220000u..0x1C22FFFFu -> "'Part 14 superNATURAL Acoustic tone'"
            in 0x1C230000u..0x1C2FFFFFu -> "'Part 14 superNATURAL Drum kit'"
            in 0x1C300000u..0x1C3FFFFFu -> "'Part 14 PCM Drum kit'"

            in 0x1C400000u..0x1C40FFFFu -> "'Part 15 PCM Synth tone'"
            in 0x1C410000u..0x1C41FFFFu -> "'Part 15 superNATURAL Synth tone'"
            in 0x1C420000u..0x1C42FFFFu -> "'Part 15 superNATURAL Acoustic tone'"
            in 0x1C430000u..0x1C4FFFFFu -> "'Part 15 superNATURAL Drum kit'"
            in 0x1C500000u..0x1C5FFFFFu -> "'Part 15 PCM Drum kit'"

            in 0x1C600000u..0x1C60FFFFu -> "'Part 16 PCM Synth tone'"
            in 0x1C610000u..0x1C61FFFFu -> "'Part 16 superNATURAL Synth tone'"
            in 0x1C620000u..0x1C62FFFFu -> "'Part 16 superNATURAL Acoustic tone'"
            in 0x1C630000u..0x1C6FFFFFu -> "'Part 16 superNATURAL Drum kit'"
            in 0x1C700000u..0x1C7FFFFFu -> "'Part 16 PCM Drum kit'"

            else -> "'Unknown range $this'"
        }

    override fun toString(): String =
        String.format("0x%02X%02X%02X%02X", msb.toInt(), mmsb.toInt(), mlsb.toInt(), lsb.toInt())
}

data class Integra7Size(val msb: UByte = 0x00u, val mmsb: UByte = 0x00u, val mlsb: UByte = 0x00u, val lsb: UByte = 0x00u): UByteSerializable {
    companion object {
        val ONE_BYTE = Integra7Size(0x01u)
    }

    constructor(fullByteSize: UInt) : this(
        msb = (fullByteSize / (0x80u * 0x80u * 0x80u)).toUByte(),
        mmsb = ((fullByteSize / (0x80u * 0x80u)) and 0x7Fu).toUByte(),
        mlsb = ((fullByteSize / 0x80u) and 0x7Fu).toUByte(),
        lsb = (fullByteSize and 0x7Fu).toUByte()
    )

    init {
        assert(msb < 0x80u)
        assert(mmsb < 0x80u)
        assert(mlsb < 0x80u)
        assert(lsb < 0x80u)
    }

    override fun bytes(): UByteArray =
        ubyteArrayOf(msb, mmsb, mlsb, lsb)

    operator fun minus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() - other).toUInt())

    operator fun plus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() + other).toUInt())

    fun fullByteSize(): Int =
        (((((msb * 0x80u) + mmsb) * 0x80u) + mlsb) * 0x80u + lsb).toInt()

    override fun toString(): String =
        fullByteSize().toString() + String.format(" Integra: 0x%02x%02x%02x%02x", msb.toInt(), mmsb.toInt(), mlsb.toInt(), lsb.toInt())
}