package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.toAsciiString
import de.tobiasblaschke.midipi.server.midi.utils.SparseUByteArray
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.math.min


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
                return payload[IntRange(startAddress.fullByteAddress(), startAddress.fullByteAddress() + length)].toAsciiString().trim()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading range $startAddress..${startAddress.offsetBy(length)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from $payload", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedValueField(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Int>() {
        override val size = Integra7Size.ONE_BYTE

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            //assert(this.isCovering(startAddress)) { "Error reading field ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            try {
                return payload[startAddress.fullByteAddress()].toInt()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address ${startAddress.fullByteAddress()} (sa=$startAddress, len=$length)", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class SignedValueField(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<Int>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int =
            delegate.interpret(startAddress, length, payload) - 64
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
    override val size = Integra7Size(0x5Fu, 0x40u, 0x00u, 0x40u)

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Setup {
        assert(startAddress >= address)

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

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Setup {
        assert(startAddress >= address)

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

data class SystemCommonRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<SystemCommonRequestBuilder.SystemCommon>() {
    override val size= Integra7Size(0x2Fu)

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): SystemCommon {
        assert(startAddress >= address)

        val systemControl1Source = SystemControlSourceAddress(deviceId, address.offsetBy(0x20))
        val systemControl2Source = SystemControlSourceAddress(deviceId, address.offsetBy(0x21))
        val systemControl3Source = SystemControlSourceAddress(deviceId, address.offsetBy(0x22))
        val systemControl4Source = SystemControlSourceAddress(deviceId, address.offsetBy(0x23))

        return SystemCommon(
            // TODO masterTune = (startAddress, min(payload.size, 0x0F), payload.copyOfRange(0, min(payload.size, 0x0F)))
            masterKeyShift = payload[0x04].toInt() - 0x40,
            masterLevel = payload[0x05].toInt(),
            scaleTuneSwitch = (payload[0x05] > 0x0u),
            studioSetControlChannel = if (payload[0x11] < 0x0Fu) payload[0x11].toInt() else null,
            systemControl1Source = ControlSource.CC01, // TODO: systemControl1Source.interpret(startAddress.offsetBy(0x20), 1, payload.subRange(0x20, 0x21)),
            systemControl2Source = ControlSource.CC01, // TODO: systemControl2Source.interpret(startAddress.offsetBy(0x21), 1, payload.subRange(0x21, 0x22)),
            systemControl3Source = ControlSource.CC01, // TODO: systemControl3Source.interpret(startAddress.offsetBy(0x22), 1, payload.subRange(0x22, 0x23)),
            systemControl4Source = ControlSource.CC01, // TODO: systemControl4Source.interpret(startAddress.offsetBy(0x23), 1, payload.subRange(0x23, 0x24)),
            controlSource = if (payload[0x24] == 0x00u.toUByte()) ControlSourceType.SYSTEM else ControlSourceType.STUDIO_SET,
            systemClockSource = if (payload[0x25] == 0x00u.toUByte()) ClockSource.MIDI else ClockSource.USB,
            systemTempo = ((payload[0x26] and 0x0Fu) * 0x10u + (payload[0x27] and 0x0Fu)).toInt(),
            tempoAssignSource = if (payload[0x28] == 0x00u.toUByte()) ControlSourceType.SYSTEM else ControlSourceType.STUDIO_SET,
            receiveProgramChange = (payload[0x29] > 0x0u),
            receiveBankSelect =  (payload[0x2A] > 0x0u),
            centerSpeakerSwitch = (payload[0x2B] > 0x0u),
            subWooferSwitch = (payload[0x2C] > 0x0u),
            twoChOutputMode = if (payload[0x2C] > 0x0u) TwoChOutputMode.SPEAKER else TwoChOutputMode.PHONES
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

    private data class SystemControlSourceAddress(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<ControlSource>() {
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
        assert(startAddress >= address)

        return StudioSet(
            common = common.interpret(startAddress, min(payload.size, 0x54), payload)
        )
    }

    data class StudioSet(
        val common: StudioSetCommonAddressRequestBuilder.StudioSetCommon
    )

    data class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommonAddressRequestBuilder.StudioSetCommon>() {
        override val size = Integra7Size(54u)

        val name = StudioSetCommonName(deviceId, address.offsetBy(0x000000))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetCommon {
            assert(startAddress >= address)

            return StudioSetCommon(
                name = name.interpret(startAddress, min(payload.size, 0x0F), payload),
                voiceReserve01 = payload[0x18].toInt(),
                voiceReserve02 = payload[0x19].toInt(),
                voiceReserve03 = payload[0x1A].toInt(),
                voiceReserve04 = payload[0x1B].toInt(),
                voiceReserve05 = payload[0x1C].toInt(),
                voiceReserve06 = payload[0x1D].toInt(),
                voiceReserve07 = payload[0x1E].toInt(),
                voiceReserve08 = payload[0x1F].toInt(),
                voiceReserve09 = payload[0x20].toInt(),
                voiceReserve10 = payload[0x21].toInt(),
                voiceReserve11 = payload[0x22].toInt(),
                voiceReserve12 = payload[0x23].toInt(),
                voiceReserve13 = payload[0x24].toInt(),
                voiceReserve14 = payload[0x25].toInt(),
                voiceReserve15 = payload[0x26].toInt(),
                voiceReserve16 = payload[0x27].toInt(),
                tone1ControlSource = ControlSource.values().first { it.hex == payload[0x39] }!!,
                tone2ControlSource = ControlSource.values().first { it.hex == payload[0x3A] }!!,
                tone3ControlSource = ControlSource.values().first { it.hex == payload[0x3B] }!!,
                tone4ControlSource = ControlSource.values().first { it.hex == payload[0x3C] }!!,
                tempo = ((payload[0x3D] and 0x0Fu) * 0x10u + (payload[0x3E] and 0x0Fu)).toInt(),
                // TODO: soloPart
                reverbSwitch = (payload[0x40] > 0x0u),
                chorusSwitch = (payload[0x41] > 0x0u),
                masterEQSwitch = (payload[0x42] > 0x0u),
                drumCompEQSwitch = (payload[0x43] > 0x0u),
                // TODO: drumCompEQPart
                // Drum Comp/EQ 1 Output Assign
                // Drum Comp/EQ 2 Output Assign
                // Drum Comp/EQ 3 Output Assign
                // Drum Comp/EQ 4 Output Assign
                // Drum Comp/EQ 5 Output Assign
                // Drum Comp/EQ 6 Output Assign
                extPartLevel = payload[0x4C].toInt(),
                extPartChorusSendLevel = payload[0x4D].toInt(),
                extPartReverbSendLevel = payload[0x4E].toInt(),
                extPartReverbMuteSwitch = (payload[0x4F] > 0x0u),
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

    data class StudioSetCommonName(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<String>() {
        override val size = Integra7Size(0x0Fu)

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): String {
            assert(startAddress >= address)

            return payload.toAsciiString(skip = 0, length = size.fullByteSize()).trim()
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

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): PcmSynthTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM synth tone ($address..${
                    address.offsetBy(
                        size
                    )
                }) for part $part, but $startAddress ${startAddress.rangeName()}"
            }

            return PcmSynthTone(
                common = common.interpret(startAddress, 0x50, payload)
            )
        }

        data class PcmSynthTone(
            override val common: PcmSynthToneCommonBuilder.PcmSynthToneCommon
        ): IntegraTone
    }

    data class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommonBuilder.PcmSynthToneCommon>() {
        override val size = Integra7Size(0x50u)

        val name = AsciiStringField(deviceId, address.offsetBy(0x000000), length = 0x0C)
        val level = UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pan = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthToneCommon {
            assert(startAddress >= address)

            return PcmSynthToneCommon(
                name = name.interpret(startAddress, length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                pan = pan.interpret(startAddress.offsetBy(lsb = 0x00Fu), length, payload)
            )
        }

        data class PcmSynthToneCommon(
            override val name: String,
            override val level: Int,
            val pan: Int
        ): IntegraToneCommon
    }

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