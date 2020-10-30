package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
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
                throw IllegalStateException("When reading range $startAddress..${startAddress.offsetBy(this.length)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from $payload", e)
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
            val ret = payload[startAddress.fullByteAddress()].toInt()
            if (!range.contains(ret)) {
                throw IllegalStateException("Value $ret not in $range When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}")
            } else {
                return ret
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
            assert(range.first >= 0 && range.endInclusive <= 0xFF) { "Impossible range $range" }
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
                val ret = payload[startAddress.fullByteAddress()].toInt() * 0x1000 +
                        payload[startAddress.fullByteAddress() + 1].toInt() * 0x100 +
                        payload[startAddress.fullByteAddress() + 2].toInt() * 0x10 +
                        payload[startAddress.fullByteAddress() + 3].toInt()
                if (range.contains(ret)) {
                    return ret
                } else {
                    throw IllegalStateException("Valie $ret not in $range, When reading address $startAddress, len=$size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}")
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
            assert(range.first >= -63 && range.endInclusive <= 63) { "Impossible range $range" }
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
    val undocumented = UndocumentedRequestBuilder(deviceId, Integra7Address(0x0F000402))    // TODO: This is not a typical address-request!!
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

data class UndocumentedRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<UndocumentedRequestBuilder.Setup>() {
    // TODO: This field works different here!
    override val size = Integra7Size(0x5Fu, 0x40u, 0x00u, 0x40u)

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Setup {
        //assert(this.isCovering(startAddress)) { "Error reading field ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

        return Setup(
            soundMode = when(payload[0].toUInt()) {
                0x01u -> SoundMode.STUDIO
                0x02u -> SoundMode.GM1
                0x03u -> SoundMode.GM2
                0x04u -> SoundMode.GS
                else -> throw IllegalArgumentException()
            },
            studioSetBsMsb = payload[0x04],
            studioSetBsLsb = payload[0x05],
            studioSetPc = payload[0x06]
        )
    }

    data class Setup(
        val soundMode: SoundMode,
        val studioSetBsMsb: UByte,
        val studioSetBsLsb: UByte,
        val studioSetPc: UByte,
    )

    enum class SoundMode {
        STUDIO, GM1, GM2, GS
    }
}

data class SetupRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<SetupRequestBuilder.Setup>() {
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

    data class Setup(
        val soundMode: SoundMode,
        val studioSetBs: Int,
        val studioSetPc: Int,
    )

    enum class SoundMode {
        STUDIO, GM1, GM2, GS
    }
}

data class SystemCommonRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<SystemCommonRequestBuilder.SystemCommon>() {
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

    data class SystemCommon(
        // val masterTune: String
        val masterKeyShift: Int,
        val masterLevel: Int,
        val scaleTuneSwitch: Boolean,
        val studioSetControlChannel: Int?,
        val systemControl1Source: ControlSource,
        val systemControl2Source: ControlSource,
        val systemControl3Source: ControlSource,
        val systemControl4Source: ControlSource,
        val controlSource: ControlSourceType,
        val systemClockSource: ClockSource,
        val systemTempo: Int,
        val tempoAssignSource: ControlSourceType,
        val receiveProgramChange: Boolean,
        val receiveBankSelect: Boolean,
        val centerSpeakerSwitch: Boolean,
        val subWooferSwitch: Boolean,
        val twoChOutputMode: TwoChOutputMode
    )

    enum class ControlSourceType {
        SYSTEM, STUDIO_SET
    }

    enum class ClockSource {
        MIDI, USB
    }

    enum class TwoChOutputMode {
        SPEAKER, PHONES
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

enum class ControlSource(val hex: UByte) {
    OFF(0x00u),
    CC01(1u),  CC02(2u),  CC03(3u),  CC04(4u),  CC05(5u),
    CC06(6u),  CC07(7u),  CC08(8u),  CC09(9u),  CC10(10u),
    CC11(11u), CC12(12u), CC13(13u), CC14(14u), CC15(15u),
    CC16(16u), CC17(17u), CC18(18u), CC19(19u), CC20(20u),
    CC21(21u), CC22(22u), CC23(23u), CC24(24u), CC25(25u),
    CC26(26u), CC27(27u), CC28(28u), CC29(29u), CC30(30u),
    CC31(31u),                 CC33(33u), CC34(34u), CC35(35u),
    CC36(36u), CC37(37u), CC38(38u), CC39(39u), CC40(40u),
    CC41(41u), CC42(42u), CC43(43u), CC44(44u), CC45(45u),
    CC46(46u), CC47(47u), CC48(48u), CC49(49u), CC50(50u),
    CC51(51u), CC52(52u), CC53(53u), CC54(54u), CC55(55u),
    CC56(56u), CC57(57u), CC58(58u), CC59(59u), CC60(60u),
    CC61(61u), CC62(62u), CC63(63u), CC64(64u), CC65(65u),
    CC66(66u), CC67(67u), CC68(68u), CC69(69u), CC70(70u),
    CC71(71u), CC72(72u), CC73(73u), CC74(74u), CC75(75u),
    CC76(76u), CC77(77u), CC78(78u), CC79(79u), CC80(80u),
    CC81(81u), CC82(82u), CC83(83u), CC84(84u), CC85(85u),
    CC86(86u), CC87(87u), CC88(88u), CC89(89u), CC90(90u),
    CC91(91u), CC92(92u), CC93(93u), CC94(94u), CC95(95u),
    BEND(95u), AFT(97u)
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
        val common: StudioSetCommonAddressRequestBuilder.StudioSetCommon
    )

    data class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommonAddressRequestBuilder.StudioSetCommon>() {
        override val size = Integra7Size(54u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), 0x0F)
        val voiceReserve01 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u))
        val voiceReserve02 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val voiceReserve03 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val voiceReserve04 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val voiceReserve05 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val voiceReserve06 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du))
        val voiceReserve07 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Eu))
        val voiceReserve08 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Fu))
        val voiceReserve09 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val voiceReserve10 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u))
        val voiceReserve11 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val voiceReserve12 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x23u))
        val voiceReserve13 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x24u))
        val voiceReserve14 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x25u))
        val voiceReserve15 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x26u))
        val voiceReserve16 = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u))
        val tone1ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), ControlSource.values())
        val tone2ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Au), ControlSource.values())
        val tone3ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Bu), ControlSource.values())
        val tone4ControlSource = EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), ControlSource.values())
        val tempo = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x3Du))
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

        data class StudioSetCommon(
            val name: String,
            val voiceReserve01: Int,
            val voiceReserve02: Int,
            val voiceReserve03: Int,
            val voiceReserve04: Int,
            val voiceReserve05: Int,
            val voiceReserve06: Int,
            val voiceReserve07: Int,
            val voiceReserve08: Int,
            val voiceReserve09: Int,
            val voiceReserve10: Int,
            val voiceReserve11: Int,
            val voiceReserve12: Int,
            val voiceReserve13: Int,
            val voiceReserve14: Int,
            val voiceReserve15: Int,
            val voiceReserve16: Int,
            val tone1ControlSource: ControlSource,
            val tone2ControlSource: ControlSource,
            val tone3ControlSource: ControlSource,
            val tone4ControlSource: ControlSource,
            val tempo: Int,
            val reverbSwitch: Boolean,
            val chorusSwitch: Boolean,
            val masterEQSwitch: Boolean,
            val drumCompEQSwitch: Boolean,
            val extPartLevel: Int,
            val extPartChorusSendLevel: Int,
            val extPartReverbSendLevel: Int,
            val extPartReverbMuteSwitch: Boolean,
        )
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
    val pcmDrumKit = IntegraToneBuilder.SuperNaturalAcousticToneBuilder(deviceId, address.offsetBy(0x100000), part)

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
        val tone: IntegraToneBuilder.IntegraTone
    )
}

sealed class IntegraToneBuilder<T: IntegraToneBuilder.IntegraTone>: Integra7MemoryIO<T>() {
    interface IntegraTone {
        val common: IntegraToneCommon
    }
    interface IntegraToneCommon {
        val name: String
        val level: Int
    }

    data class PcmSynthToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<PcmSynthToneBuilder.PcmSynthTone>() {
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
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM synth tone ($address..${
                    address.offsetBy(
                        size
                    )
                }) for part $part, but $startAddress ${startAddress.rangeName()}"
            }

            return PcmSynthTone(
                common = common.interpret(startAddress, 0x50, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), length, payload),
                partialMixTable = partialMixTable.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u), length, payload),
                partial1 = partial1.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u), length, payload),
                partial2 = partial2.interpret(startAddress.offsetBy(mlsb = 0x22u, lsb = 0x00u), length, payload),
                partial3 = partial3.interpret(startAddress.offsetBy(mlsb = 0x24u, lsb = 0x00u), length, payload),
                partial4 = partial4.interpret(startAddress.offsetBy(mlsb = 0x26u, lsb = 0x00u), length, payload),
                common2 = common2.interpret(startAddress.offsetBy(mlsb = 0x30u, lsb = 0x00u), length, payload),
            )
        }

        data class PcmSynthTone(
            override val common: PcmSynthToneCommonBuilder.PcmSynthToneCommon,
            val mfx: PcmSynthToneMfxBuilder.PcmSynthToneMfx,
            val partialMixTable: PcmSynthTonePartialMixTableBuilder.PcmSynthTonePartialMixTable,
            val partial1: PcmSynthTonePartialBuilder.PcmSynthTonePartial,
            val partial2: PcmSynthTonePartialBuilder.PcmSynthTonePartial,
            val partial3: PcmSynthTonePartialBuilder.PcmSynthTonePartial,
            val partial4: PcmSynthTonePartialBuilder.PcmSynthTonePartial,
            val common2: PcmSynthToneCommon2Builder.PcmSynthToneCommon2,
        ): IntegraTone
    }

    data class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommonBuilder.PcmSynthToneCommon>() {
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

        enum class MatrixControlSource(val hex: UByte) {
            OFF(0x00u),
            CC01(1u),  CC02(2u),  CC03(3u),  CC04(4u),  CC05(5u),
            CC06(6u),  CC07(7u),  CC08(8u),  CC09(9u),  CC10(10u),
            CC11(11u), CC12(12u), CC13(13u), CC14(14u), CC15(15u),
            CC16(16u), CC17(17u), CC18(18u), CC19(19u), CC20(20u),
            CC21(21u), CC22(22u), CC23(23u), CC24(24u), CC25(25u),
            CC26(26u), CC27(27u), CC28(28u), CC29(29u), CC30(30u),
            CC31(31u),                 CC33(33u), CC34(34u), CC35(35u),
            CC36(36u), CC37(37u), CC38(38u), CC39(39u), CC40(40u),
            CC41(41u), CC42(42u), CC43(43u), CC44(44u), CC45(45u),
            CC46(46u), CC47(47u), CC48(48u), CC49(49u), CC50(50u),
            CC51(51u), CC52(52u), CC53(53u), CC54(54u), CC55(55u),
            CC56(56u), CC57(57u), CC58(58u), CC59(59u), CC60(60u),
            CC61(61u), CC62(62u), CC63(63u), CC64(64u), CC65(65u),
            CC66(66u), CC67(67u), CC68(68u), CC69(69u), CC70(70u),
            CC71(71u), CC72(72u), CC73(73u), CC74(74u), CC75(75u),
            CC76(76u), CC77(77u), CC78(78u), CC79(79u), CC80(80u),
            CC81(81u), CC82(82u), CC83(83u), CC84(84u), CC85(85u),
            CC86(86u), CC87(87u), CC88(88u), CC89(89u), CC90(90u),
            CC91(91u), CC92(92u), CC93(93u), CC94(94u), CC95(95u),
            BEND(95u), AFT(97u),
            CTRL1(98u), CTRL2(99u), CTRL3(100u), CTRL4(101u),
            VELOCITY(102u), KEYFOLLOW(103u), TEMPO(104u), LFO1(105u),
            LFO2(106u), PIT_ENV(107u), TVF_ENV(108u), TVA_ENV(109u)
        }

        enum class MatrixControlDestination(val hex: UByte) {
            OFF(0u), PCH(1u), CUT(2u), RES(3u), LEV(4u), PAN(5u),
            DRY(6u), CHO(7u), REV(8u), PIT_LFO1(9u),
            PIT_LFO2(10u), TVF_LFO1(11u), TVF_LFO2(12u),
            TVA_LFO1(13u), TVA_LFO2(14u), PAN_LFO1(15u),
            PAN_LFO2(16u), LFO1_RATE(17u), LFO2_RATE(18u),
            PIT_ATK(19u), PIT_DCY(20u), PIT_REL(21u),
            TVF_ATK(22u), TVF_DCY(23u), TVF_REL(24u),
            TVA_ATK(25u), TVA_DCY(26u), TVA_REL(27u),
            PMT(28u), FXM(29u)
        }

        data class PcmSynthToneCommon(
            override val name: String,
            override val level: Int,
            val pan: Int,
            val priority: Priority,
            val coarseTuning: Int,
            val fineTuning: Int,
            val ocataveShift: Int,
            val stretchTuneDepth: Int,
            val analogFeel: Int,
            val monoPoly: MonoPoly,
            val legatoSwitch: Boolean,
            val legatoRetrigger: Boolean,
            val portamentoSwitch: Boolean,
            val portamentoMode: PortamentoMode,
            val portamentoType: PortamentoType,
            val portamentoStart: PortamentoStart,
            val portamentoTime: Int,

            val cutoffOffset: Int,
            val resonanceOffset: Int,
            val attackTimeOffset: Int,
            val releaseTimeOffset: Int,
            val velocitySensOffset: Int,

            val pmtControlSwitch: Boolean,
            val pitchBendRangeUp: Int,
            val pitchBendRangeDown: Int,

            val matrixControl1Source: MatrixControlSource,
            val matrixControl1Destination1: MatrixControlDestination,
            val matrixControl1Sens1: Int,
            val matrixControl1Destination2: MatrixControlDestination,
            val matrixControl1Sens2: Int,
            val matrixControl1Destination3: MatrixControlDestination,
            val matrixControl1Sens3: Int,
            val matrixControl1Destination4: MatrixControlDestination,
            val matrixControl1Sens4: Int,

            val matrixControl2Source: MatrixControlSource,
            val matrixControl2Destination1: MatrixControlDestination,
            val matrixControl2Sens1: Int,
            val matrixControl2Destination2: MatrixControlDestination,
            val matrixControl2Sens2: Int,
            val matrixControl2Destination3: MatrixControlDestination,
            val matrixControl2Sens3: Int,
            val matrixControl2Destination4: MatrixControlDestination,
            val matrixControl2Sens4: Int,

            val matrixControl3Source: MatrixControlSource,
            val matrixControl3Destination1: MatrixControlDestination,
            val matrixControl3Sens1: Int,
            val matrixControl3Destination2: MatrixControlDestination,
            val matrixControl3Sens2: Int,
            val matrixControl3Destination3: MatrixControlDestination,
            val matrixControl3Sens3: Int,
            val matrixControl3Destination4: MatrixControlDestination,
            val matrixControl3Sens4: Int,

            val matrixControl4Source: MatrixControlSource,
            val matrixControl4Destination1: MatrixControlDestination,
            val matrixControl4Sens1: Int,
            val matrixControl4Destination2: MatrixControlDestination,
            val matrixControl4Sens2: Int,
            val matrixControl4Destination3: MatrixControlDestination,
            val matrixControl4Sens3: Int,
            val matrixControl4Destination4: MatrixControlDestination,
            val matrixControl4Sens4: Int,
        ): IntegraToneCommon {
            init {
                assert(name.length <= 0x0C)
                assert(level in 0..127) { "Not in the expected range $level" }
                assert(pan in -64..63) { "Not in the expected range $pan" }
                assert(coarseTuning in -48..48) { "Not in the expected range $coarseTuning" }
                assert(fineTuning in -50..50) { "Not in the expected range $fineTuning" }
                assert(ocataveShift in -3..3) { "Not in the expected range $ocataveShift" }
                assert(stretchTuneDepth in 0..3) { "Not in the expected range $stretchTuneDepth" }
                assert(portamentoTime in 0..127) { "Not in the expected range $portamentoTime" }

                assert(cutoffOffset in -63..63) { "Not in the expected range $cutoffOffset" }
                assert(resonanceOffset in -63..63) { "Not in the expected range $resonanceOffset" }
                assert(attackTimeOffset in -63..63) { "Not in the expected range $attackTimeOffset" }
                assert(releaseTimeOffset in -63..63) { "Not in the expected range $releaseTimeOffset" }
                assert(velocitySensOffset in -63..63) { "Not in the expected range $velocitySensOffset" }

                assert(pitchBendRangeUp in 0..48) { "Not in the expected range $pitchBendRangeUp" }
                assert(pitchBendRangeDown in 0..48) { "Not in the expected range $pitchBendRangeDown" }

                assert(matrixControl1Sens1 in -63..63) { "Not in the expected range $matrixControl1Sens1" }
                assert(matrixControl1Sens2 in -63..63) { "Not in the expected range $matrixControl1Sens2" }
                assert(matrixControl1Sens3 in -63..63) { "Not in the expected range $matrixControl1Sens3" }
                assert(matrixControl1Sens4 in -63..63) { "Not in the expected range $matrixControl1Sens4" }

                assert(matrixControl2Sens1 in -63..63) { "Not in the expected range $matrixControl2Sens1" }
                assert(matrixControl2Sens2 in -63..63) { "Not in the expected range $matrixControl2Sens2" }
                assert(matrixControl2Sens3 in -63..63) { "Not in the expected range $matrixControl2Sens3" }
                assert(matrixControl2Sens4 in -63..63) { "Not in the expected range $matrixControl2Sens4" }

                assert(matrixControl3Sens1 in -63..63) { "Not in the expected range $matrixControl3Sens1" }
                assert(matrixControl3Sens2 in -63..63) { "Not in the expected range $matrixControl3Sens2" }
                assert(matrixControl3Sens3 in -63..63) { "Not in the expected range $matrixControl3Sens3" }
                assert(matrixControl3Sens4 in -63..63) { "Not in the expected range $matrixControl3Sens4" }

                assert(matrixControl4Sens1 in -63..63) { "Not in the expected range $matrixControl4Sens1" }
                assert(matrixControl4Sens2 in -63..63) { "Not in the expected range $matrixControl4Sens2" }
                assert(matrixControl4Sens3 in -63..63) { "Not in the expected range $matrixControl4Sens3" }
                assert(matrixControl4Sens4 in -63..63) { "Not in the expected range $matrixControl4Sens4" }
            }
        }
    }

    data class PcmSynthToneMfxBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneMfxBuilder.PcmSynthToneMfx>() {
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
            assert(startAddress >= address)

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

        enum class MfxControlSource(val hex: UByte) {
            OFF(0x00u),
            CC01(1u),  CC02(2u),  CC03(3u),  CC04(4u),  CC05(5u),
            CC06(6u),  CC07(7u),  CC08(8u),  CC09(9u),  CC10(10u),
            CC11(11u), CC12(12u), CC13(13u), CC14(14u), CC15(15u),
            CC16(16u), CC17(17u), CC18(18u), CC19(19u), CC20(20u),
            CC21(21u), CC22(22u), CC23(23u), CC24(24u), CC25(25u),
            CC26(26u), CC27(27u), CC28(28u), CC29(29u), CC30(30u),
            CC31(31u),                 CC33(33u), CC34(34u), CC35(35u),
            CC36(36u), CC37(37u), CC38(38u), CC39(39u), CC40(40u),
            CC41(41u), CC42(42u), CC43(43u), CC44(44u), CC45(45u),
            CC46(46u), CC47(47u), CC48(48u), CC49(49u), CC50(50u),
            CC51(51u), CC52(52u), CC53(53u), CC54(54u), CC55(55u),
            CC56(56u), CC57(57u), CC58(58u), CC59(59u), CC60(60u),
            CC61(61u), CC62(62u), CC63(63u), CC64(64u), CC65(65u),
            CC66(66u), CC67(67u), CC68(68u), CC69(69u), CC70(70u),
            CC71(71u), CC72(72u), CC73(73u), CC74(74u), CC75(75u),
            CC76(76u), CC77(77u), CC78(78u), CC79(79u), CC80(80u),
            CC81(81u), CC82(82u), CC83(83u), CC84(84u), CC85(85u),
            CC86(86u), CC87(87u), CC88(88u), CC89(89u), CC90(90u),
            CC91(91u), CC92(92u), CC93(93u), CC94(94u), CC95(95u),
            BEND(95u), AFT(97u),
            SYS1(98u), SYS2(99u), SYS3(100u), SYS4(101u)
        }

        data class PcmSynthToneMfx(
            val mfxType: Int,
            val mfxChorusSend: Int,
            val mfxReverbSend: Int,

            val mfxControl1Source: MfxControlSource,
            val mfxControl1Sens: Int,
            val mfxControl2Source: MfxControlSource,
            val mfxControl2Sens: Int,
            val mfxControl3Source: MfxControlSource,
            val mfxControl3Sens: Int,
            val mfxControl4Source: MfxControlSource,
            val mfxControl4Sens: Int,

            val mfxControlAssign1: Int,
            val mfxControlAssign2: Int,
            val mfxControlAssign3: Int,
            val mfxControlAssign4: Int,

            val mfxParameter1: Int,
            val mfxParameter2: Int,
            val mfxParameter3: Int,
            val mfxParameter4: Int,
            val mfxParameter5: Int,
            val mfxParameter6: Int,
            val mfxParameter7: Int,
            val mfxParameter8: Int,
            val mfxParameter9: Int,
            val mfxParameter10: Int,
            val mfxParameter11: Int,
            val mfxParameter12: Int,
            val mfxParameter13: Int,
            val mfxParameter14: Int,
            val mfxParameter15: Int,
            val mfxParameter16: Int,
            val mfxParameter17: Int,
            val mfxParameter18: Int,
            val mfxParameter19: Int,
            val mfxParameter20: Int,
            val mfxParameter21: Int,
            val mfxParameter22: Int,
            val mfxParameter23: Int,
            val mfxParameter24: Int,
            val mfxParameter25: Int,
            val mfxParameter26: Int,
            val mfxParameter27: Int,
            val mfxParameter28: Int,
            val mfxParameter29: Int,
            val mfxParameter30: Int,
            val mfxParameter31: Int,
            val mfxParameter32: Int,
        ) {
            init {
                assert(mfxType in 0..67) { "Value not in range $mfxType" }
                assert(mfxChorusSend in 0..127) { "Value not in range $mfxChorusSend" }
                assert(mfxReverbSend in 0..127) { "Value not in range $mfxReverbSend" }

                assert(mfxControl1Sens in -63..63) { "Value not in range $mfxControl1Sens" }
                assert(mfxControl2Sens in -63..63) { "Value not in range $mfxControl2Sens" }
                assert(mfxControl3Sens in -63..63) { "Value not in range $mfxControl3Sens" }
                assert(mfxControl4Sens in -63..63) { "Value not in range $mfxControl4Sens" }

                assert(mfxControlAssign1 in 0..16) { "Value not in range $mfxControlAssign1" }
                assert(mfxControlAssign2 in 0..16) { "Value not in range $mfxControlAssign2" }
                assert(mfxControlAssign3 in 0..16) { "Value not in range $mfxControlAssign3" }
                assert(mfxControlAssign4 in 0..16) { "Value not in range $mfxControlAssign4" }

                assert(mfxParameter1  in -20000..20000) { "Value not in range $mfxParameter1" }
                assert(mfxParameter2  in -20000..20000) { "Value not in range $mfxParameter2" }
                assert(mfxParameter3  in -20000..20000) { "Value not in range $mfxParameter3" }
                assert(mfxParameter4  in -20000..20000) { "Value not in range $mfxParameter4" }
                assert(mfxParameter5  in -20000..20000) { "Value not in range $mfxParameter5" }
                assert(mfxParameter6  in -20000..20000) { "Value not in range $mfxParameter6" }
                assert(mfxParameter7  in -20000..20000) { "Value not in range $mfxParameter7" }
                assert(mfxParameter8  in -20000..20000) { "Value not in range $mfxParameter8" }
                assert(mfxParameter9  in -20000..20000) { "Value not in range $mfxParameter9" }
                assert(mfxParameter10  in -20000..20000) { "Value not in range $mfxParameter10" }
                assert(mfxParameter11  in -20000..20000) { "Value not in range $mfxParameter11" }
                assert(mfxParameter12  in -20000..20000) { "Value not in range $mfxParameter12" }
                assert(mfxParameter13  in -20000..20000) { "Value not in range $mfxParameter13" }
                assert(mfxParameter14  in -20000..20000) { "Value not in range $mfxParameter14" }
                assert(mfxParameter15  in -20000..20000) { "Value not in range $mfxParameter15" }
                assert(mfxParameter16  in -20000..20000) { "Value not in range $mfxParameter16" }
                assert(mfxParameter17  in -20000..20000) { "Value not in range $mfxParameter17" }
                assert(mfxParameter18  in -20000..20000) { "Value not in range $mfxParameter18" }
                assert(mfxParameter19  in -20000..20000) { "Value not in range $mfxParameter19" }
                assert(mfxParameter20  in -20000..20000) { "Value not in range $mfxParameter20" }
                assert(mfxParameter21  in -20000..20000) { "Value not in range $mfxParameter21" }
                assert(mfxParameter22  in -20000..20000) { "Value not in range $mfxParameter22" }
                assert(mfxParameter23  in -20000..20000) { "Value not in range $mfxParameter23" }
                assert(mfxParameter24  in -20000..20000) { "Value not in range $mfxParameter24" }
                assert(mfxParameter25  in -20000..20000) { "Value not in range $mfxParameter25" }
                assert(mfxParameter26  in -20000..20000) { "Value not in range $mfxParameter26" }
                assert(mfxParameter27  in -20000..20000) { "Value not in range $mfxParameter27" }
                assert(mfxParameter28  in -20000..20000) { "Value not in range $mfxParameter28" }
                assert(mfxParameter29  in -20000..20000) { "Value not in range $mfxParameter29" }
                assert(mfxParameter30  in -20000..20000) { "Value not in range $mfxParameter30" }
                assert(mfxParameter31  in -20000..20000) { "Value not in range $mfxParameter31" }
                assert(mfxParameter32  in -20000..20000) { "Value not in range $mfxParameter32" }
            }
        }
    }

    data class PcmSynthTonePartialMixTableBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartialMixTableBuilder.PcmSynthTonePartialMixTable>() {
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

        enum class VelocityControl{ OFF, ON, RANDOM, CYCLE }

        data class PcmSynthTonePartialMixTable(
            val structureType12: Int,
            val booster12: Int,
            val structureType34: Int,
            val booster34: Int,

            val velocityControl: VelocityControl,

            val pmt1PartialSwitch: Boolean,
            val pmt1KeyboardRange: Int,
            val pmt1KeyboardFadeWidth: Int,
            val pmt1VelocityRange: Int,
            val pmt1VelocityFade: Int,

            val pmt2PartialSwitch: Boolean,
            val pmt2KeyboardRange: Int,
            val pmt2KeyboardFadeWidth: Int,
            val pmt2VelocityRange: Int,
            val pmt2VelocityFade: Int,

            val pmt3PartialSwitch: Boolean,
            val pmt3KeyboardRange: Int,
            val pmt3KeyboardFadeWidth: Int,
            val pmt3VelocityRange: Int,
            val pmt3VelocityFade: Int,

            val pmt4PartialSwitch: Boolean,
            val pmt4KeyboardRange: Int,
            val pmt4KeyboardFadeWidth: Int,
            val pmt4VelocityRange: Int,
            val pmt4VelocityFade: Int,
        ) {
            init {
                assert(structureType12 in 0..9 ) { "Value not in range $structureType12" }
                assert(booster12 in 0..3 ) { "Value not in range $booster12" }
                assert(structureType34 in 0..9) { "Value not in range $structureType34" }
                assert(booster34 in 0..3) { "Value not in range $booster34" }

                assert(pmt1KeyboardRange in 0..0x4000) { "Value not in range $pmt1KeyboardRange" }
                assert(pmt1KeyboardFadeWidth in 0..0x4000 ) { "Value not in range $pmt1KeyboardFadeWidth" }
                assert(pmt1VelocityRange in 0..0x4000) { "Value not in range $pmt1VelocityRange" }
                assert(pmt1VelocityFade in 0..0x4000) { "Value not in range $pmt1VelocityFade" }

                assert(pmt2KeyboardRange in 0..0x4000) { "Value not in range $pmt2KeyboardRange" }
                assert(pmt2KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt2KeyboardFadeWidth" }
                assert(pmt2VelocityRange in 0..0x4000) { "Value not in range $pmt2VelocityRange" }
                assert(pmt2VelocityFade in 0..0x4000) { "Value not in range $pmt2VelocityFade" }

                assert(pmt3KeyboardRange in 0..0x4000) { "Value not in range $pmt3KeyboardRange" }
                assert(pmt3KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt3KeyboardFadeWidth" }
                assert(pmt3VelocityRange in 0..0x4000) { "Value not in range $pmt3VelocityRange" }
                assert(pmt3VelocityFade in 0..0x4000) { "Value not in range $pmt3VelocityFade" }

                assert(pmt4KeyboardRange in 0..0x4000) { "Value not in range $pmt4KeyboardRange" }
                assert(pmt4KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt4KeyboardFadeWidth" }
                assert(pmt4VelocityRange in 0..0x4000) { "Value not in range $pmt4VelocityRange" }
                assert(pmt4VelocityFade in 0..0x4000) { "Value not in range $pmt4VelocityFade" }
            }
        }
    }

    data class PcmSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartialBuilder.PcmSynthTonePartial>() {
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
        val tvfResonance = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Du))
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
        val biasPosition = SignedValueField(deviceId, address.offsetBy(lsb = 0x5Fu))
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

                    waveGroupType = waveGroupType.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveGroupId = waveGroupId.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveNumberL = waveNumberL.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveNumberR = waveNumberR.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveGain = waveGain.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveFXMSwitch = waveFXMSwitch.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveFXMColor = waveFXMColor.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveFXMDepth = waveFXMDepth.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    waveTempoSync = waveTempoSync.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                    // wavePitchKeyfollow = wavePitchKeyfollow.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),

                    pitchEnvDepth = pitchEnvDepth.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                    pitchEnvVelocitySens = pitchEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                    pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                    pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
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
                    lfoStep15 = lfoStep15.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x18u), length, payload),
                    lfoStep16 = lfoStep16.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x19u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }


        enum class RandomPithDepth {
            PD_0, PD_1, PD_2, PD_3, PD_4, PD_5, PD_6, PD_7, PD_8, PD_9,
            PD_10, PD_20, PD_30, PD_40, PD_50, PD_60, PD_70, PD_80,
            PD_90, PD_100, PD_200, PD_300, PD_400, PD_500,
            PD_600, PD_700, PD_800, PD_900, PD_1000, PD_1100,
            PD_1200 }
        enum class EnvMode { SUSTAIN, NO_SUSTAIN }
        enum class DelayMode { NORMAL, HOLD, KEY_OFF_NORMAL, KEY_OFF_DECAY }
        enum class OffOnReverse { OFF, ON, REVERSE }
        enum class WaveGroupType { INT, SRX }
        enum class WaveGain { MINUS_6_DB, ZERO, PLUS_6_DB, PLUS_12_DB }
        enum class TvfFilterType { OFF, LPF, BPF, HPF, PKG, LPF2, LPF3 }
        enum class BiasDirection { LOWER, UPPER, LOWER_UPPER, ALL }
        enum class LfoWaveForm { SIN, TRI, SAW_UP, SAW_DW, SQR, RND, BEND_UP, BEND_DW, TRP, SAMPLE_AND_HOLD, CHS, VSIN, STEP }
        enum class LfoOffset { MINUS_100, MINUS_50, ZERO, PLUS_50, PLUS_100}
        enum class LfoFadeMode { ON_IN, ON_OUT, OFF_IN, OFF_OUT }

        data class PcmSynthTonePartial(
            val level: Int,
            val chorusTune: Int,
            val fineTune: Int,
            val randomPithDepth: RandomPithDepth,
            val pan: Int,
            // val panKeyFollow: Int,
            val panDepth: Int,
            val alternatePanDepth: Int,
            val envMode: EnvMode,
            val delayMode: DelayMode,
            val delayTime: Int,

            val outputLevel: Int,
            val chorusSendLevel: Int,
            val reverbSendLevel: Int,

            val receiveBender: Boolean,
            val receiveExpression: Boolean,
            val receiveHold1: Boolean,
            val redamper: Boolean,

            val partialControl1Switch1: OffOnReverse,
            val partialControl1Switch2: OffOnReverse,
            val partialControl1Switch3: OffOnReverse,
            val partialControl1Switch4: OffOnReverse,
            val partialControl2Switch1: OffOnReverse,
            val partialControl2Switch2: OffOnReverse,
            val partialControl2Switch3: OffOnReverse,
            val partialControl2Switch4: OffOnReverse,
            val partialControl3Switch1: OffOnReverse,
            val partialControl3Switch2: OffOnReverse,
            val partialControl3Switch3: OffOnReverse,
            val partialControl3Switch4: OffOnReverse,
            val partialControl4Switch1: OffOnReverse,
            val partialControl4Switch2: OffOnReverse,
            val partialControl4Switch3: OffOnReverse,
            val partialControl4Switch4: OffOnReverse,

            val waveGroupType: WaveGroupType,
            val waveGroupId: Int,
            val waveNumberL: Int,
            val waveNumberR: Int,
            val waveGain: WaveGain,
            val waveFXMSwitch: Boolean,
            val waveFXMColor: Int,
            val waveFXMDepth: Int,
            val waveTempoSync: Boolean,
            // val wavePitchKeyfollow: Int,

            val pitchEnvDepth: Int,
            val pitchEnvVelocitySens: Int,
            val pitchEnvTime1VelocitySens: Int,
            val pitchEnvTime4VelocitySens: Int,
            // val pitchEnvTimeKeyfollow: Int,
            val pitchEnvTime1: Int,
            val pitchEnvTime2: Int,
            val pitchEnvTime3: Int,
            val pitchEnvTime4: Int,
            val pitchEnvLevel0: Int,
            val pitchEnvLevel1: Int,
            val pitchEnvLevel2: Int,
            val pitchEnvLevel3: Int,
            val pitchEnvLevel4: Int,

            val tvfFilterType: TvfFilterType,
            val tvfCutoffFrequency: Int,
            // val tvfCutoffKeyfollow: Int,
            val tvfCutoffVelocityCurve: Int,
            val tvfCutoffVelocitySens: Int,
            val tvfResonance: Int,
            val tvfResonanceVelocitySens: Int,
            val tvfEnvDepth: Int,
            val tvfEnvVelocityCurve: Int,
            val tvfEnvVelocitySens: Int,
            val tvfEnvTime1VelocitySens: Int,
            val tvfEnvTime4VelocitySens: Int,
            // val tvfEnvTimeKeyfollow: Int,
            val tvfEnvTime1: Int,
            val tvfEnvTime2: Int,
            val tvfEnvTime3: Int,
            val tvfEnvTime4: Int,
            val tvfEnvLevel0: Int,
            val tvfEnvLevel1: Int,
            val tvfEnvLevel2: Int,
            val tvfEnvLevel3: Int,
            val tvfEnvLevel4: Int,

            //val biasLevel: Int,
            val biasPosition: Int,
            val biasDirection: BiasDirection,
            val tvaLevelVelocityCurve: Int,
            val tvaLevelVelocitySens: Int,
            val tvaEnvTime1VelocitySens: Int,
            val tvaEnvTime4VelocitySens: Int,
            // val tvaEnvTimeKeyfollow: Int,
            val tvaEnvTime1: Int,
            val tvaEnvTime2: Int,
            val tvaEnvTime3: Int,
            val tvaEnvTime4: Int,
            val tvaEnvLevel1: Int,
            val tvaEnvLevel2: Int,
            val tvaEnvLevel3: Int,

            val lfo1WaveForm: LfoWaveForm,
            val lfo1Rate: Int,
            val lfo1Offset: LfoOffset,
            val lfo1RateDetune: Int,
            val lfo1DelayTime: Int,
            // val lfo1Keyfollow: Int,
            val lfo1FadeMode: LfoFadeMode,
            val lfo1FadeTime: Int,
            val lfo1KeyTrigger: Boolean,
            val lfo1PitchDepth: Int,
            val lfo1TvfDepth: Int,
            val lfo1TvaDepth: Int,
            val lfo1PanDepth: Int,

            val lfo2WaveForm: LfoWaveForm,
            val lfo2Rate: Int,
            val lfo2Offset: LfoOffset,
            val lfo2RateDetune: Int,
            val lfo2DelayTime: Int,
            // val lfo2Keyfollow: Int,
            val lfo2FadeMode: LfoFadeMode,
            val lfo2FadeTime: Int,
            val lfo2KeyTrigger: Boolean,
            val lfo2PitchDepth: Int,
            val lfo2TvfDepth: Int,
            val lfo2TvaDepth: Int,
            val lfo2PanDepth: Int,

            val lfoStepType: Int,
            val lfoStep1: Int,
            val lfoStep2: Int,
            val lfoStep3: Int,
            val lfoStep4: Int,
            val lfoStep5: Int,
            val lfoStep6: Int,
            val lfoStep7: Int,
            val lfoStep8: Int,
            val lfoStep9: Int,
            val lfoStep10: Int,
            val lfoStep11: Int,
            val lfoStep12: Int,
            val lfoStep13: Int,
            val lfoStep14: Int,
            val lfoStep15: Int,
            val lfoStep16: Int,
        ) {
            init {
                assert(level in 0..127 ) { "Value not in range $level" }
                assert(chorusTune in -48..48 ) { "Value not in range $chorusTune" }
                assert(fineTune in -50..50 ) { "Value not in range $fineTune" }
                assert(pan in -64..64 ) { "Value not in range $pan" }
                // assert(panKeyFollow in -100..100 ) { "Value not in range $panKeyFollow" }
                assert(panDepth in 0..63 ) { "Value not in range $panDepth" }
                assert(alternatePanDepth in -63..63 ) { "Value not in range $alternatePanDepth" }
                assert(delayTime in 0..149 ) { "Value not in range $delayTime" }

                assert(outputLevel in 0..127 ) { "Value not in range $outputLevel" }
                assert(chorusSendLevel in 0..127 ) { "Value not in range $chorusSendLevel" }
                assert(reverbSendLevel in 0..127 ) { "Value not in range $reverbSendLevel" }

                assert(waveGroupId in 0..16384 ) { "Value not in range $waveGroupId" }
                assert(waveNumberL in 0..16384 ) { "Value not in range $waveNumberL" }
                assert(waveNumberR in 0..16384 ) { "Value not in range $waveNumberR" }
                assert(waveFXMColor in 0..3 ) { "Value not in range $waveFXMColor" }
                assert(waveFXMDepth in 0..16 ) { "Value not in range $waveFXMDepth" }
                // assert(wavePitchKeyfollow in -200..200 ) { "Value not in range $wavePitchKeyfollow" }

                assert(pitchEnvDepth in -12..12 ) { "Value not in range: $pitchEnvDepth" }
                assert(pitchEnvVelocitySens in -63..63 ) { "Value not in range $pitchEnvVelocitySens" }
                assert(pitchEnvTime1VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime1VelocitySens" }
                assert(pitchEnvTime4VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime4VelocitySens" }
                // assert(pitchEnvTimeKeyfollow in -100..100 ) { "Value not in range $pitchEnvTimeKeyfollow" }
                assert(pitchEnvTime1 in 0..127 ) { "Value not in range $pitchEnvTime1" }
                assert(pitchEnvTime2 in 0..127 ) { "Value not in range $pitchEnvTime2" }
                assert(pitchEnvTime3 in 0..127 ) { "Value not in range $pitchEnvTime3" }
                assert(pitchEnvTime4 in 0..127 ) { "Value not in range $pitchEnvTime4" }
                assert(pitchEnvLevel0 in -63..63 ) { "Value not in rangeo $pitchEnvLevel0" }
                assert(pitchEnvLevel1 in -63..63 ) { "Value not in range $pitchEnvLevel1" }
                assert(pitchEnvLevel2 in -63..63 ) { "Value not in range $pitchEnvLevel2" }
                assert(pitchEnvLevel3 in -63..63 ) { "Value not in range $pitchEnvLevel3" }
                assert(pitchEnvLevel4 in -63..63 ) { "Value not in range $pitchEnvLevel4" }

                assert(tvfCutoffFrequency in 0..127 ) { "Value not in range $tvfCutoffFrequency" }
                // assert(tvfCutoffKeyfollow in -200..200 ) { "Value not in range $tvfCutoffKeyfollow" }
                assert(tvfCutoffVelocityCurve in 0..7 ) { "Value not in range $tvfCutoffVelocityCurve" }
                assert(tvfCutoffVelocitySens in -63..63 ) { "Value not in range $tvfCutoffVelocitySens" }
                assert(tvfResonance in 0..127 ) { "Value not in range $tvfResonance" }
                assert(tvfResonanceVelocitySens in -63..63 ) { "Value not in range $tvfResonanceVelocitySens" }
                assert(tvfEnvDepth in -63..63 ) { "Value not in range $tvfEnvDepth" }
                assert(tvfEnvVelocityCurve in 0..7 ) { "Value not in range $tvfEnvVelocityCurve" }
                assert(tvfEnvVelocitySens in -63..63 ) { "Value not in range $tvfEnvVelocitySens" }
                assert(tvfEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime1VelocitySens" }
                assert(tvfEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime4VelocitySens" }
                // assert(tvfEnvTimeKeyfollow in -100..100 ) { "Value not in range $tvfEnvTimeKeyfollow" }
                assert(tvfEnvTime1 in 0..127 ) { "Value not in range $tvfEnvTime1" }
                assert(tvfEnvTime2 in 0..127 ) { "Value not in range $tvfEnvTime2" }
                assert(tvfEnvTime3 in 0..127 ) { "Value not in range $tvfEnvTime3" }
                assert(tvfEnvTime4 in 0..127 ) { "Value not in range $tvfEnvTime4" }
                assert(tvfEnvLevel0 in 0..127 ) { "Value not in range $tvfEnvLevel0" }
                assert(tvfEnvLevel1 in 0..127 ) { "Value not in range $tvfEnvLevel1" }
                assert(tvfEnvLevel2 in 0..127 ) { "Value not in range $tvfEnvLevel2" }
                assert(tvfEnvLevel3 in 0..127 ) { "Value not in range $tvfEnvLevel3" }
                assert(tvfEnvLevel4 in 0..127 ) { "Value not in range $tvfEnvLevel4" }

                // assert(biasLevel in -100..100 ) { "Value not in range $biasLevel" }
                assert(biasPosition in 0..127 ) { "Value not in range $biasPosition" }
                assert(tvaLevelVelocityCurve in 0..7 ) { "Value not in range $tvaLevelVelocityCurve" }
                assert(tvaLevelVelocitySens in -63..63 ) { "Value not in range $tvaLevelVelocitySens" }
                assert(tvaEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime1VelocitySens" }
                assert(tvaEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime4VelocitySens" }
                // assert(tvaEnvTimeKeyfollow in -100..100 ) { "Value not in range $tvaEnvTimeKeyfollow" }
                assert(tvaEnvTime1 in 0..127 ) { "Value not in range $tvaEnvTime1" }
                assert(tvaEnvTime2 in 0..127 ) { "Value not in range $tvaEnvTime2" }
                assert(tvaEnvTime3 in 0..127 ) { "Value not in range $tvaEnvTime3" }
                assert(tvaEnvTime4 in 0..127 ) { "Value not in range $tvaEnvTime4" }
                assert(tvaEnvLevel1 in 0..127 ) { "Value not in range $tvaEnvLevel1" }
                assert(tvaEnvLevel2 in 0..127 ) { "Value not in range $tvaEnvLevel2" }
                assert(tvaEnvLevel3 in 0..127 ) { "Value not in range $tvaEnvLevel3" }

                assert(lfo1Rate in 0..149 ) { "Value not in range $lfo1Rate" }
                assert(lfo1RateDetune in 0..127 ) { "Value not in range $lfo1RateDetune" }
                assert(lfo1DelayTime in 0..127 ) { "Value not in range $lfo1DelayTime" }
                // assert(lfo1Keyfollow in -100..100 ) { "Value not in range $lfo1Keyfollow" }
                assert(lfo1FadeTime in 0..127 ) { "Value not in range $lfo1FadeTime" }
                assert(lfo1PitchDepth in 1..127 ) { "Value not in range $lfo1PitchDepth" }
                assert(lfo1TvfDepth in -63..63 ) { "Value not in range $lfo1TvfDepth" }
                assert(lfo1TvaDepth in -63..63 ) { "Value not in range $lfo1TvaDepth" }
                assert(lfo1PanDepth in -63..63 ) { "Value not in range $lfo1PanDepth" }

                assert(lfo2Rate in 0..149 ) { "Value not in range $lfo2Rate" }
                assert(lfo2RateDetune in 0..127 ) { "Value not in range $lfo2RateDetune" }
                assert(lfo2DelayTime in 0..127 ) { "Value not in range $lfo2DelayTime" }
                // assert(lfo2Keyfollow in -100..100 ) { "Value not in range $lfo2Keyfollow" }
                assert(lfo2FadeTime in 0..127 ) { "Value not in range $lfo2FadeTime" }
                assert(lfo2PitchDepth in 1..127 ) { "Value not in range $lfo2PitchDepth" }
                assert(lfo2TvfDepth in -63..63 ) { "Value not in range $lfo2TvfDepth" }
                assert(lfo2TvaDepth in -63..63 ) { "Value not in range $lfo2TvaDepth" }
                assert(lfo2PanDepth in -63..63 ) { "Value not in range $lfo2PanDepth" }

                assert(lfoStepType in 0..1 ) { "Value not in range $lfoStepType" }
                assert(lfoStep1 in -36..36 ) { "Value not in range $lfoStep1" }
                assert(lfoStep2 in -36..36 ) { "Value not in range $lfoStep2" }
                assert(lfoStep3 in -36..36 ) { "Value not in range $lfoStep3" }
                assert(lfoStep4 in -36..36 ) { "Value not in range $lfoStep4" }
                assert(lfoStep5 in -36..36 ) { "Value not in range $lfoStep5" }
                assert(lfoStep6 in -36..36 ) { "Value not in range $lfoStep6" }
                assert(lfoStep7 in -36..36 ) { "Value not in range $lfoStep7" }
                assert(lfoStep8 in -36..36 ) { "Value not in range $lfoStep8" }
                assert(lfoStep9 in -36..36 ) { "Value not in range $lfoStep9" }
                assert(lfoStep10 in -36..36 ) { "Value not in range $lfoStep10" }
                assert(lfoStep11 in -36..36 ) { "Value not in range $lfoStep11" }
                assert(lfoStep12 in -36..36 ) { "Value not in range $lfoStep12" }
                assert(lfoStep13 in -36..36 ) { "Value not in range $lfoStep13" }
                assert(lfoStep14 in -36..36 ) { "Value not in range $lfoStep14" }
                assert(lfoStep15 in -36..36 ) { "Value not in range $lfoStep15" }
                assert(lfoStep16 in -36..36 ) { "Value not in range $lfoStep16" }
            }
        }
    }

    data class PcmSynthToneCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommon2Builder.PcmSynthToneCommon2>() {
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
                    phraseNmber = phraseNmber.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address.fromFullByteAddress(it).toString() }, 0x10)}", e)
            }
        }

        enum class VelocityControl{ OFF, ON, RANDOM, CYCLE }

        data class PcmSynthToneCommon2(
            val toneCategory: Int,
            val undocumented: Int,
            val phraseOctaveShift: Int,
            val tfxSwitch: Boolean,
            val phraseNmber: Int,
        ) {
            init {
                assert(toneCategory in 0..127 ) { "Value not in range $toneCategory" }
                assert(undocumented in 0..255 ) { "Value not in range $undocumented" }
                assert(phraseOctaveShift in -3..3 ) { "Value not in range $phraseOctaveShift" }
                assert(phraseNmber in 0..65535 ) { "Value not in range $phraseNmber" }
            }
        }
    }


    enum class Priority { LAST, LOUDEST }
    enum class MonoPoly { MONO, POLY }
    enum class PortamentoMode { NORMAL, LEGATO }
    enum class PortamentoType { RATE, TIME }
    enum class PortamentoStart { PITCH, NOTE }

    data class SuperNaturalSynthToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalSynthToneBuilder.SuperNaturalSynthTone>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x22u, 0x7Fu)

        val common = SuperNaturalSynthToneCommonBuilder(deviceId, address.offsetBy(0x000000))
        // mfx
        // partial 1
        // partial 2
        // partial 3

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalSynthTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-S tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalSynthTone(
                common = common.interpret(startAddress, 0x50, payload)
            )
        }

        data class SuperNaturalSynthTone (
            override val common: SuperNaturalSynthToneCommonBuilder.SupernaturalSynthToneCommon
        ): IntegraTone
    }

    data class SuperNaturalSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SuperNaturalSynthToneCommonBuilder.SupernaturalSynthToneCommon>() {
        override val size = Integra7Size(0x40u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalSynthToneCommon {
            assert(startAddress >= address)

            return SupernaturalSynthToneCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
            )
        }

        data class SupernaturalSynthToneCommon(
            override val name: String,
            override val level: Int,
        ): IntegraToneCommon
    }

    data class SuperNaturalAcousticToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalAcousticToneBuilder.SuperNaturalAcousticTone>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x30u, 0x3Cu)

        val common = SuperNaturalAcousticToneCommonBuilder(deviceId, address.offsetBy(0x000000))
        // mfx

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalAcousticTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-A tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalAcousticTone(
                common = common.interpret(startAddress, 0x50, payload)
            )
        }

        data class SuperNaturalAcousticTone (
            override val common: SuperNaturalAcousticToneCommonBuilder.SupernaturalAcousticToneCommon
        ): IntegraTone
    }

    data class SuperNaturalAcousticToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SuperNaturalAcousticToneCommonBuilder.SupernaturalAcousticToneCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalAcousticToneCommon {
            assert(startAddress >= address)

            return SupernaturalAcousticToneCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
            )
        }

        data class SupernaturalAcousticToneCommon(
            override val name: String,
            override val level: Int,
        ): IntegraToneCommon
    }

    data class SuperNaturalDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalDrumKitBuilder.SuperNaturalDrumKit>() {
        override val size = Integra7Size(0x00u, 0x00u, 0x4Du, 0x7Fu)

        val common = SuperNaturalDrumKitCommonBuilder(deviceId, address.offsetBy(0x000000))
        // mfx
        // common-comp
        // note[]

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SuperNaturalDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalDrumKit(
                common = common.interpret(startAddress, 0x50, payload)
            )
        }

        data class SuperNaturalDrumKit (
            override val common: SuperNaturalDrumKitCommonBuilder.SupernaturalDrumKitCommon
        ): IntegraTone
    }

    data class SuperNaturalDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<SuperNaturalDrumKitCommonBuilder.SupernaturalDrumKitCommon>() {
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

        data class SupernaturalDrumKitCommon(
            override val name: String,
            override val level: Int,
            val ambienceLevel: Int,
            val phraseNo: Int,
            val tfx: Boolean,
        ): IntegraToneCommon
    }

    data class PcmDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<PcmDrumKitBuilder.PcmDrumKit>() {
        override val size = Integra7Size(0x00u, 0x02u, 0x7Fu, 0x7Fu)

        val common = PcmDrumKitCommonBuilder(deviceId, address.offsetBy(0x000000))
        // mfx
        // common-comp
        // note[]
        // common2

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): PcmDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM Drum kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmDrumKit(
                common = common.interpret(startAddress, 0x50, payload)
            )
        }

        data class PcmDrumKit (
            override val common: PcmDrumKitCommonBuilder.SupernaturalDrumKitCommon
        ): IntegraTone
    }

    data class PcmDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmDrumKitCommonBuilder.SupernaturalDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommon {
            assert(startAddress >= address)

            return SupernaturalDrumKitCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
            )
        }

        data class SupernaturalDrumKitCommon(
            override val name: String,
            override val level: Int,
        ): IntegraToneCommon
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
            else -> Integra7Address(newMsb, newMmsb and 0x7Fu, newMlsb and 0x7u, newLsb and 0x7u)
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
        fullByteSize().toString()
}