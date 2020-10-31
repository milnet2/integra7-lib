package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain.*
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.Integra7Address
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.Integra7FieldType
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.Integra7Size
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalArgumentException
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
        val addrSum = address.address.toUByteArrayLittleEndian()
            .sum().toUByte()
        val payloadSum = payload.reduce { a, b -> ((a + b) and 0xFFu).toUByte() }
        val totalSum = ((addrSum + payloadSum) and 0xFFu).toUByte()
        val reminder = (totalSum % 128u).toUByte()
        return if (reminder < 128u) (128u - reminder).toUByte() else (reminder - 128u).toUByte()
    }

    abstract fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): T

}

// ----------------------------------------------------

class AddressRequestBuilder(private val deviceId: DeviceId) {
//    val undocumented = UndocumentedRequestBuilder(deviceId, Integra7Address(0x0F000402))    // TODO: This is not a typical address-request!!
    val setup = SetupRequestBuilder(deviceId, Integra7Address.Integra7Ranges.SETUP.begin())
    val system = SystemCommonRequestBuilder(deviceId, Integra7Address.Integra7Ranges.SYSTEM.begin())
    val studioSet = StudioSetAddressRequestBuilder(deviceId, Integra7Address.Integra7Ranges.STUDIO_SET.begin())

    val tones: Map<IntegraPart, ToneAddressRequestBuilder> = IntegraPart.values()
        .map { it to ToneAddressRequestBuilder(deviceId, Integra7Address.Integra7Ranges.PART_1_PCM_SYNTH_TONE.begin()
            .offsetBy(UInt7(mmsb = 0x20u.toUByte7()), it.zeroBased), it) }
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

    val soundMode = Integra7FieldType.UnsignedValueField(deviceId, address)
    val studioSetBs = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x04u))
    val studioSetPc = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))

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
            studioSetBs = studioSetBs.interpret(startAddress.offsetBy(lsb =0x04u), length, payload),
            studioSetPc = studioSetPc.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload))
    }
}

data class SystemCommonRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<SystemCommon>() {
    override val size= Integra7Size(0x2Fu)

    val masterKeyShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x04u))
    val masterLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
    val scaleTuneSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x06u))
    val systemControl1Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x20u))
    val systemControl2Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x21u))
    val systemControl3Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x22u))
    val systemControl4Source = SystemControlSourceAddress(deviceId, address.offsetBy(lsb = 0x23u))
    val controlSource =
        Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x24u), ControlSourceType.values())
    val systemClockSource =
        Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x25u), ClockSource.values())
    val systemTempo = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x26u))
    val tempoAssignSource =
        Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x28u), ControlSourceType.values())
    val receiveProgramChange = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x29u))
    val receiveBankSelect = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Au))
    val centerSpeakerSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Bu))
    val subWooferSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Cu))
    val twoChOutputMode =
        Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Du), TwoChOutputMode.values())

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

data class StudioSetAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSet>() {
    override val size = Integra7Size(UInt7(mlsb = 0x57u.toUByte7(), lsb = UByte7.MAX_VALUE))

    val common = StudioSetCommonAddressRequestBuilder(deviceId, address)
    val commonChorus = StudioSetCommonChorusBuilder(deviceId, address.offsetBy(mlsb = 0x04u, lsb = 0x00u))
    val commonReverb = StudioSetCommonReverbBuilder(deviceId, address.offsetBy(mlsb = 0x06u, lsb = 0x00u))
    val motionalSourround = StudioSetMotionalSurroundBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u))
    val masterEq = StudioSetMasterEqBuilder(deviceId, address.offsetBy(mlsb = 0x09u, lsb = 0x00u))
    val midiChannelPhaseLocks = IntRange(0, 15)
        .map {
            Integra7FieldType.BooleanValueField(
                deviceId,
                address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)
            )
        }
    val parts = IntRange(0, 15)
        .map { StudioSetPartBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)) }
    val partEqs = IntRange(0, 15)
        .map { StudioSetPartEqBuilder(deviceId, address.offsetBy(mlsb = 0x50u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)) }

    override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSet {
        assert(this.isCovering(startAddress)) { "Expected Studio-Set address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

        return StudioSet(
            common = common.interpret(startAddress, length, payload),
            commonChorus = commonChorus.interpret(startAddress.offsetBy(mlsb = 0x04u, lsb = 0x00u), length, payload),
            commonReverb = commonReverb.interpret(startAddress.offsetBy(mlsb = 0x06u, lsb = 0x00u), length, payload),
            motionalSurround = motionalSourround.interpret(startAddress.offsetBy(mlsb = 0x08u, lsb = 0x00u), length, payload),
            masterEq = masterEq.interpret(startAddress.offsetBy(mlsb = 0x09u, lsb = 0x00u), length, payload),
            midiChannelPhaseLocks = midiChannelPhaseLocks
                .mapIndexed { index, p -> p.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = index), length, payload) },
            parts = parts
                .mapIndexed { index, p -> p.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = index), length, payload) },
            partEqs = partEqs
                .mapIndexed { index, p -> p.interpret(startAddress.offsetBy(mlsb = 0x50u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = index), length, payload) },
        )
    }

    data class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommon>() {
        override val size = Integra7Size(54u)

        val name = Integra7FieldType.AsciiStringField(deviceId, address, 0x0F)
        val voiceReserve01 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u), 0..64)
        val voiceReserve02 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u), 0..64)
        val voiceReserve03 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au), 0..64)
        val voiceReserve04 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..64)
        val voiceReserve05 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Cu), 0..64)
        val voiceReserve06 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du), 0..64)
        val voiceReserve07 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Eu), 0..64)
        val voiceReserve08 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Fu), 0..64)
        val voiceReserve09 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u), 0..64)
        val voiceReserve10 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u), 0..64)
        val voiceReserve11 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x22u), 0..64)
        val voiceReserve12 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x23u), 0..64)
        val voiceReserve13 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x24u), 0..64)
        val voiceReserve14 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x25u), 0..64)
        val voiceReserve15 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x26u), 0..64)
        val voiceReserve16 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u), 0..64)
        val tone1ControlSource =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), ControlSource::fromValue)
        val tone2ControlSource =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Au), ControlSource::fromValue)
        val tone3ControlSource =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Bu), ControlSource::fromValue)
        val tone4ControlSource =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), ControlSource::fromValue)
        val tempo = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x3Du), 20..250)
        val reverbSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x40u))
        val chorusSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val masterEQSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x42u))
        val drumCompEQSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val extPartLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val extPartChorusSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val extPartReverbSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))
        val extPartReverbMuteSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x4Fu))

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

    data class StudioSetCommonChorusBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommonChorus>() {
        override val size = Integra7Size(54u)

        val type = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u), 0..3)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x01u), 0..127)
        val outputSelect =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x03u), ChorusOutputSelect.values())
        val parameters = IntRange(1, 19) // TODO: Access last element 20)
            .map {
                Integra7FieldType.SignedMsbLsbFourNibbles(
                    deviceId,
                    address.offsetBy(lsb = 0x04u, factor = it),
                    -20000..20000
                )
            }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetCommonChorus {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set chorus address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetCommonChorus(
                type = type.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                outputSelect = outputSelect.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                parameters = parameters
                    .mapIndexed { idx, p -> p.interpret(address.offsetBy(lsb = 0x04u).offsetBy(lsb = 0x04u, factor = idx), length, payload) }
            )
        }
    }

    data class StudioSetCommonReverbBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetCommonReverb>() {
        override val size = Integra7Size(63u)

        val type = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u), 0..3)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x01u), 0..127)
        val outputSelect =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x02u), ReverbOutputSelect.values())
        val parameters = IntRange(0, 22) // TODO: Acces last element! 23)
            .map {
                Integra7FieldType.SignedMsbLsbFourNibbles(
                    deviceId,
                    address.offsetBy(lsb = 0x03u).offsetBy(lsb = 0x04u, factor = it),
                    -20000..20000
                )
            }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetCommonReverb {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set reverb address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetCommonReverb(
                type = type.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                outputSelect = outputSelect.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                parameters = parameters
                    .mapIndexed { idx, p -> p.interpret(address.offsetBy(lsb = 0x03u).offsetBy(lsb = 0x04u, factor = idx), length, payload) }
            )
        }
    }

    data class StudioSetMotionalSurroundBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetMotionalSurround>() {
        override val size = Integra7Size(0x10u.toUInt7UsingValue())

        val switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val roomType = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x01u), RoomType.values())
        val ambienceLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u))
        val roomSize = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x03u), RoomSize.values())
        val ambienceTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x04u), 0..100)
        val ambienceDensity = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u), 0..100)
        val ambienceHfDamp = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u), 0..100)
        val extPartLR = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x07u), -64..63)
        val extPartFB = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x08u), -64..63)
        val extPartWidth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x09u), 0..32)
        val extPartAmbienceSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Au))
        val extPartControlChannel = Integra7FieldType.UnsignedValueField(
            deviceId,
            address.offsetBy(lsb = 0x0Bu),
            0..16
        ) // 1..16, OFF --> Why is OFF the last now *grrr*
        val depth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..100)

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetMotionalSurround {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set reverb address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetMotionalSurround(
                switch.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                roomType.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                ambienceLevel.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                roomSize.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),
                ambienceTime.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                ambienceDensity.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                ambienceHfDamp.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                extPartLR.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                extPartFB.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),
                extPartWidth.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),
                extPartAmbienceSendLevel.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),
                extPartControlChannel.interpret(startAddress.offsetBy(lsb = 0x0Bu), length, payload),
                0 // depth.interpret(startAddress.offsetBy(lsb = 0xC0u), length, payload),
            )
        }
    }

    data class StudioSetMasterEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetMasterEq>() {
        override val size = Integra7Size(63u)

        val lowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x00u),
            SupernaturalDrumLowFrequency.values()
        )
        val lowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x01u), 0..30) // -15
        val midFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x02u),
            SupernaturalDrumMidFrequency.values()
        )
        val midGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u), 0..30) // -15
        val midQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumMidQ.values())
        val highFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x05u),
            SupernaturalDrumHighFrequency.values()
        )
        val highGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u), 0..30) // -15

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetMasterEq {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set master-eq address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetMasterEq(
                lowFrequency = lowFrequency.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                lowGain = lowGain.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload) - 15,
                midFrequency = midFrequency.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload),
                midGain = midGain.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload) - 15,
                midQ = midQ.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload),
                highFrequency = highFrequency.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                highGain = highGain.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload) - 15,
            )
        }
    }

    data class StudioSetPartBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetPart>() {
        override val size = Integra7Size(0x4Du)

        val receiveChannel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u), 0..30)
        val receiveSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x01u))

        val toneBankMsb = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u)) // CC#0, CC#32
        val toneBankLsb = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u)) // CC#0, CC#32
        val toneProgramNumber = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x08u))

        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x09u)) // CC#7
        val pan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Au), -64..63) // CC#10
        val coarseTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Bu), -48..48) // RPN#2
        val fineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), -50..50) // RPN#1
        val monoPoly = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Du), MonoPolyTone.values())
        val legatoSwitch =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Eu), OffOnTone.values()) // CC#68
        val pitchBendRange =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu), 0..25) // RPN#0 - 0..24, TONE
        val portamentoSwitch =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x10u), OffOnTone.values()) // CC#65
        val portamentoTime = Integra7FieldType.UnsignedMsbLsbNibbles(
            deviceId,
            address.offsetBy(lsb = 0x11u),
            0..128
        ) // CC#5 0..127, TONE
        val cutoffOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -64..63) // CC#74
        val resonanceOffset =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x14u), -64..63) // CC#71
        val attackTimeOffset =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x15u), -64..63) // CC#73
        val decayTimeOffset =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x16u), -64..63) // CC#75
        val releaseTimeOffset =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x17u), -64..63) // CC#72
        val vibratoRate = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x18u), -64..63) // CC#76
        val vibratoDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x19u), -64..63) // CC#77
        val vibratoDelay = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Au), -64..63) // CC#78
        val octaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), -3..3)
        val velocitySensOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val keyboardRange = Integra7FieldType.UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x1Du))
        val keyboardFadeWidth = Integra7FieldType.UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x1Fu))
        val velocityRange = Integra7FieldType.UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x21u))
        val velocityFadeWidth = Integra7FieldType.UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x23u))
        val muteSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x25u))

        val chorusSend = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u)) // CC#93
        val reverbSend = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x28u)) // CC#91
        val outputAssign =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x29u), PartOutput.values())

        val scaleTuneType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Bu), ScaleTuneType.values())
        val scaleTuneKey = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Cu), NoteKey.values())
        val scaleTuneC = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Du), -64..63)
        val scaleTuneCSharp = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Eu), -64..63)
        val scaleTuneD = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu), -64..63)
        val scaleTuneDSharp = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x30u), -64..63)
        val scaleTuneE = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x31u), -64..63)
        val scaleTuneF = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x32u), -64..63)
        val scaleTuneFSharp = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x33u), -64..63)
        val scaleTuneG = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x34u), -64..63)
        val scaleTuneGSharp = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x35u), -64..63)
        val scaleTuneA = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
        val scaleTuneASharp = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x37u), -64..63)
        val scaleTuneB = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x38u), -64..63)

        val receiveProgramChange = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x39u))
        val receiveBankSelect = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Au))
        val receivePitchBend = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val receivePolyphonicKeyPressure = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Cu))
        val receiveChannelPressure = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Du))
        val receiveModulation = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val receiveVolume = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val receivePan = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x40u))
        val receiveExpression = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val receiveHold1 = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x42u))

        val velocityCurveType = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x43u), 0..4)

        val motionalSurroundLR = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x44u), -64..63)
        val motionalSurroundFB = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x46u), -64..63)
        val motionalSurroundWidth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x48u), 0..32)
        val motionalSurroundAmbienceSend = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x49u))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetPart {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set master-eq address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetPart(
                receiveChannel.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                receiveSwitch.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),

                toneBankMsb.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                toneBankLsb.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload),
                toneProgramNumber.interpret(startAddress.offsetBy(lsb = 0x08u), length, payload),

                level.interpret(startAddress.offsetBy(lsb = 0x09u), length, payload),
                pan.interpret(startAddress.offsetBy(lsb = 0x0Au), length, payload),
                coarseTune.interpret(startAddress.offsetBy(lsb = 0x0Bu), length, payload),
                fineTune.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
                monoPoly.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                legatoSwitch.interpret(startAddress.offsetBy(lsb = 0x0Eu), length, payload),
                pitchBendRange.interpret(startAddress.offsetBy(lsb = 0x0Fu), length, payload),
                portamentoSwitch.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                portamentoTime.interpret(startAddress.offsetBy(lsb = 0x11u), length, payload),
                cutoffOffset.interpret(startAddress.offsetBy(lsb = 0x13u), length, payload),
                resonanceOffset.interpret(startAddress.offsetBy(lsb = 0x14u), length, payload),
                attackTimeOffset.interpret(startAddress.offsetBy(lsb = 0x15u), length, payload),
                decayTimeOffset.interpret(startAddress.offsetBy(lsb = 0x16u), length, payload),
                releaseTimeOffset.interpret(startAddress.offsetBy(lsb = 0x17u), length, payload),
                vibratoRate.interpret(startAddress.offsetBy(lsb = 0x18u), length, payload),
                vibratoDepth.interpret(startAddress.offsetBy(lsb = 0x19u), length, payload),
                vibratoDelay.interpret(startAddress.offsetBy(lsb = 0x1Au), length, payload),
                octaveShift.interpret(startAddress.offsetBy(lsb = 0x1Bu), length, payload),
                velocitySensOffset.interpret(startAddress.offsetBy(lsb = 0x1Cu), length, payload),
                keyboardRange.interpret(startAddress.offsetBy(lsb = 0x1Du), length, payload),
                keyboardFadeWidth.interpret(startAddress.offsetBy(lsb = 0x1Fu), length, payload),
                velocityRange.interpret(startAddress.offsetBy(lsb = 0x21u), length, payload),
                velocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x23u), length, payload),
                muteSwitch.interpret(startAddress.offsetBy(lsb = 0x25u), length, payload),

                chorusSend.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                reverbSend.interpret(startAddress.offsetBy(lsb = 0x28u), length, payload),
                outputAssign.interpret(startAddress.offsetBy(lsb = 0x29u), length, payload),

                scaleTuneType.interpret(startAddress.offsetBy(lsb = 0x2Bu), length, payload),
                scaleTuneKey.interpret(startAddress.offsetBy(lsb = 0x2Cu), length, payload),
                scaleTuneC.interpret(startAddress.offsetBy(lsb = 0x2Du), length, payload),
                scaleTuneCSharp.interpret(startAddress.offsetBy(lsb = 0x2Eu), length, payload),
                scaleTuneD.interpret(startAddress.offsetBy(lsb = 0x2Fu), length, payload),
                scaleTuneDSharp.interpret(startAddress.offsetBy(lsb = 0x30u), length, payload),
                scaleTuneE.interpret(startAddress.offsetBy(lsb = 0x31u), length, payload),
                scaleTuneF.interpret(startAddress.offsetBy(lsb = 0x32u), length, payload),
                scaleTuneFSharp.interpret(startAddress.offsetBy(lsb = 0x33u), length, payload),
                scaleTuneG.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                scaleTuneGSharp.interpret(startAddress.offsetBy(lsb = 0x35u), length, payload),
                scaleTuneA.interpret(startAddress.offsetBy(lsb = 0x36u), length, payload),
                scaleTuneASharp.interpret(startAddress.offsetBy(lsb = 0x37u), length, payload),
                scaleTuneB.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),

                receiveProgramChange.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),
                receiveBankSelect.interpret(startAddress.offsetBy(lsb = 0x3Au), length, payload),
                receivePitchBend.interpret(startAddress.offsetBy(lsb = 0x3Bu), length, payload),
                receivePolyphonicKeyPressure.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
                receiveChannelPressure.interpret(startAddress.offsetBy(lsb = 0x3Du), length, payload),
                receiveModulation.interpret(startAddress.offsetBy(lsb = 0x3Eu), length, payload),
                receiveVolume.interpret(startAddress.offsetBy(lsb = 0x3Fu), length, payload),
                receivePan.interpret(startAddress.offsetBy(lsb = 0x40u), length, payload),
                receiveExpression.interpret(startAddress.offsetBy(lsb = 0x41u), length, payload),
                receiveHold1.interpret(startAddress.offsetBy(lsb = 0x42u), length, payload),

                velocityCurveType.interpret(startAddress.offsetBy(lsb = 0x43u), length, payload) + 1, // TODO

                motionalSurroundLR.interpret(startAddress.offsetBy(lsb = 0x44u), length, payload),
                motionalSurroundFB.interpret(startAddress.offsetBy(lsb = 0x46u), length, payload),
                motionalSurroundWidth.interpret(startAddress.offsetBy(lsb = 0x48u), length, payload),
                motionalSurroundAmbienceSend.interpret(startAddress.offsetBy(lsb = 0x49u), length, payload)
            )
        }
    }

    data class StudioSetPartEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO<StudioSetPartEq>() {
        override val size = Integra7Size(8u)

        val switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val lowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x01u),
            SupernaturalDrumLowFrequency.values()
        )
        val lowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u), 0..30) // -15
        val midFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x03u),
            SupernaturalDrumMidFrequency.values()
        )
        val midGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x04u), 0..30) // -15
        val midQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x05u), SupernaturalDrumMidQ.values())
        val highFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x06u),
            SupernaturalDrumHighFrequency.values()
        )
        val highGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u), 0..30) // -15

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): StudioSetPartEq {
            assert(this.isCovering(startAddress)) { "Expected Studio-Set part-eq address ($address..${address.offsetBy(size)}) but was $startAddress ${startAddress.rangeName()}" }

            return StudioSetPartEq(
                switch = switch.interpret(startAddress.offsetBy(lsb = 0x00u), length, payload),
                lowFrequency = lowFrequency.interpret(startAddress.offsetBy(lsb = 0x01u), length, payload),
                lowGain = lowGain.interpret(startAddress.offsetBy(lsb = 0x02u), length, payload) - 15,
                midFrequency = midFrequency.interpret(startAddress.offsetBy(lsb = 0x03u), length, payload),
                midGain = midGain.interpret(startAddress.offsetBy(lsb = 0x04u), length, payload) - 15,
                midQ = midQ.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                highFrequency = highFrequency.interpret(startAddress.offsetBy(lsb = 0x06u), length, payload),
                highGain = highGain.interpret(startAddress.offsetBy(lsb = 0x07u), length, payload) - 15,
            )
        }
    }
}

data class ToneAddressRequestBuilder(
    override val deviceId: DeviceId,
    override val address: Integra7Address,
    val part: IntegraPart
): Integra7MemoryIO<ToneAddressRequestBuilder.TemporaryTone>() {
    override val size: Integra7Size = Integra7Size(UInt7(mmsb = 0x20.toUByte7()))

    val pcmSynthTone = IntegraToneBuilder.PcmSynthToneBuilder(deviceId, address, part)
    val snaSynthTone = IntegraToneBuilder.SuperNaturalSynthToneBuilder(deviceId, address.offsetBy(mmsb = 0x01u.toUByte7()), part)
    val snaAcousticTone = IntegraToneBuilder.SuperNaturalAcousticToneBuilder(deviceId, address.offsetBy(mmsb = 0x02u.toUByte7()), part)
    val snaDrumKit = IntegraToneBuilder.SuperNaturalDrumKitBuilder(deviceId, address.offsetBy(mmsb = 0x03u.toUByte7()), part)
    val pcmDrumKit = IntegraToneBuilder.PcmDrumKitBuilder(deviceId, address.offsetBy(mmsb = 0x10u.toUByte7()), part)

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
        override val size = Integra7Size(UInt7(mlsb = 0x30u. toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = PcmSynthToneCommonBuilder(deviceId, address)
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

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val priority = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x10u), Priority.values())
        val coarseTuning = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x11u), -48..48)
        val fineTuning = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -50..50)
        val ocataveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val stretchTuneDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x14u), 0..3)
        val analogFeel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x15u))
        val monoPoly = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x16u), MonoPoly.values())
        val legatoSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val legatoRetrigger = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x18u))
        val portamentoSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val portamentoMode =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Au), PortamentoMode.values())
        val portamentoType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Bu), PortamentoType.values())
        val portamentoStart =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), PortamentoStart.values())
        val portamentoTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du))

        val cutoffOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val resonanceOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x23u))
        val attackTimeOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x24u))
        val releaseTimeOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x25u))
        val velocitySensOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x26u))

        val pmtControlSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x28u))
        val pitchBendRangeUp = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x29u), 0..48)
        val pitchBendRangeDown = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Au), 0..48)

        val matrixControl1Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Bu), MatrixControlSource.values())
        val matrixControl1Destination1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Cu), MatrixControlDestination.values())
        val matrixControl1Sens1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val matrixControl1Destination2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Eu), MatrixControlDestination.values())
        val matrixControl1Sens2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu))
        val matrixControl1Destination3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x30u), MatrixControlDestination.values())
        val matrixControl1Sens3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x31u))
        val matrixControl1Destination4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x32u), MatrixControlDestination.values())
        val matrixControl1Sens4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x33u))

        val matrixControl2Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), MatrixControlSource.values())
        val matrixControl2Destination1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x35u), MatrixControlDestination.values())
        val matrixControl2Sens1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x36u))
        val matrixControl2Destination2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x37u), MatrixControlDestination.values())
        val matrixControl2Sens2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val matrixControl2Destination3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x39u), MatrixControlDestination.values())
        val matrixControl2Sens3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Au))
        val matrixControl2Destination4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Bu), MatrixControlDestination.values())
        val matrixControl2Sens4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu))

        val matrixControl3Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Du), MatrixControlSource.values())
        val matrixControl3Destination1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Eu), MatrixControlDestination.values())
        val matrixControl3Sens1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val matrixControl3Destination2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x40u), MatrixControlDestination.values())
        val matrixControl3Sens2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val matrixControl3Destination3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x42u), MatrixControlDestination.values())
        val matrixControl3Sens3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val matrixControl3Destination4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x44u), MatrixControlDestination.values())
        val matrixControl3Sens4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x45u))

        val matrixControl4Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x46u), MatrixControlSource.values())
        val matrixControl4Destination1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x47u), MatrixControlDestination.values())
        val matrixControl4Sens1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x48u))
        val matrixControl4Destination2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x49u), MatrixControlDestination.values())
        val matrixControl4Sens2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Au))
        val matrixControl4Destination3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x4Bu), MatrixControlDestination.values())
        val matrixControl4Sens3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val matrixControl4Destination4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x4Du), MatrixControlDestination.values())
        val matrixControl4Sens4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))

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
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthToneMfxBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneMfx>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x11u.toUByte7()))

        val mfxType = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val mfxChorusSend = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u))
        val mfxReverbSend = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u))

        val mfxControl1Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x05u), MfxControlSource.values())
        val mfxControl1Sens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val mfxControl2Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x07u), MfxControlSource.values())
        val mfxControl2Sens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x08u))
        val mfxControl3Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x09u), MfxControlSource.values())
        val mfxControl3Sens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Au))
        val mfxControl4Source =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), MfxControlSource.values())
        val mfxControl4Sens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

        val mfxControlAssign1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Du))
        val mfxControlAssign2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val mfxControlAssign3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val mfxControlAssign4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))

        val mfxParameter1 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x11u))
        val mfxParameter2 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x15u))
        val mfxParameter3 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x19u))
        val mfxParameter4 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x1Du))
        val mfxParameter5 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x21u))
        val mfxParameter6 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x25u))
        val mfxParameter7 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x29u))
        val mfxParameter8 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Du))
        val mfxParameter9 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x31u))
        val mfxParameter10 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u))
        val mfxParameter11 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x39u))
        val mfxParameter12 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x3Du))
        val mfxParameter13 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x41u))
        val mfxParameter14 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x45u))
        val mfxParameter15 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x49u))
        val mfxParameter16 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x4Du))
        val mfxParameter17 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x51u))
        val mfxParameter18 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x55u))
        val mfxParameter19 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x59u))
        val mfxParameter20 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du))
        val mfxParameter21 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u))
        val mfxParameter22 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u))
        val mfxParameter23 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x69u))
        val mfxParameter24 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x6Du))
        val mfxParameter25 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x71u))
        val mfxParameter26 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x75u))
        val mfxParameter27 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x79u))
        val mfxParameter28 = Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Du))
        val mfxParameter29 =
            Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u))
        val mfxParameter30 =
            Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val mfxParameter31 =
            Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u))
        val mfxParameter32 =
            Integra7FieldType.SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmSynthToneMfx {
            assert(this.isCovering(startAddress)) { "Not a MFX definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()} in  ${payload.hexDump({ Integra7Address(
                it.toUInt7()).toString() }, 0x10)}" }

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
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthTonePartialMixTableBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartialMixTable>() {
        override val size = Integra7Size(0x29u.toUInt7UsingValue())

        val structureType12 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val booster12 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x01u)) // ENUM
        val structureType34 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x02u))
        val booster34 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u)) // ENUM

        val velocityControl =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x04u), VelocityControl.values())

        val pmt1PartialSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val pmt1KeyboardRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x06u))
        val pmt1KeyboardFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x08u))
        val pmt1VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Au))
        val pmt1VelocityFade = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Cu))

        val pmt2PartialSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pmt2KeyboardRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Fu))
        val pmt2KeyboardFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x11u))
        val pmt2VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x13u))
        val pmt2VelocityFade = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x15u))

        val pmt3PartialSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val pmt3KeyboardRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x18u))
        val pmt3KeyboardFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Au))
        val pmt3VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Cu))
        val pmt3VelocityFade = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Eu))

        val pmt4PartialSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val pmt4KeyboardRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x21u))
        val pmt4KeyboardFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x23u))
        val pmt4VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x25u))
        val pmt4VelocityFade = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x27u))

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
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthTonePartial>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01.toUByte7(), lsb = 0x1Au.toUByte7()))

        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -48..48)
        val fineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -50..50)
        val randomPithDepth =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), RandomPithDepth.values())
        val pan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        // TODO val panKeyFollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x00u), -100..100)
        val panDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val alternatePanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val envMode = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), EnvMode.values())
        val delayMode = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), DelayMode.values())
        val delayTime = Integra7FieldType.UnsignedMsbLsbNibbles(
            deviceId,
            address.offsetBy(lsb = 0x00u),
            0..149
        ) // TODO: 0 - 127, MUSICAL-NOTES |

        val outputLevel = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusSendLevel = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val reverbSendLevel = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x00u))

        val receiveBender = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val receiveExpression = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val receiveHold1 = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val redamper = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x16u))

        val partialControl1Switch1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x17u), OffOnReverse.values())
        val partialControl1Switch2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x18u), OffOnReverse.values())
        val partialControl1Switch3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x19u), OffOnReverse.values())
        val partialControl1Switch4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Au), OffOnReverse.values())
        val partialControl2Switch1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Bu), OffOnReverse.values())
        val partialControl2Switch2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), OffOnReverse.values())
        val partialControl2Switch3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Du), OffOnReverse.values())
        val partialControl2Switch4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Eu), OffOnReverse.values())
        val partialControl3Switch1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), OffOnReverse.values())
        val partialControl3Switch2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), OffOnReverse.values())
        val partialControl3Switch3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x21u), OffOnReverse.values())
        val partialControl3Switch4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x22u), OffOnReverse.values())
        val partialControl4Switch1 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x23u), OffOnReverse.values())
        val partialControl4Switch2 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x24u), OffOnReverse.values())
        val partialControl4Switch3 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x25u), OffOnReverse.values())
        val partialControl4Switch4 =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x26u), OffOnReverse.values())

        val waveGroupType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x27u), WaveGroupType.values())
        val waveGroupId = Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x28u), 0..16384)
        val waveNumberL = Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Cu), 0..16384)
        val waveNumberR = Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x30u), 0..16384)
        val waveGain = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveFXMSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x35u))
        val waveFXMColor = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x36u), 0..3)
        val waveFXMDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x37u), 0..16)
        val waveTempoSync = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        // TODO val wavePitchKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x37u), -200..200)

        val pitchEnvDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Au), -12..12)
        val pitchEnvVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0xBAu))
        val pitchEnvTime1VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu))
        val pitchEnvTime4VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Du))
        // TODO val pitchEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Eu), -100..100)
        val pitchEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val pitchEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u))
        val pitchEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x41u))
        val pitchEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u))
        val pitchEnvLevel0 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x43u))
        val pitchEnvLevel1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x44u))
        val pitchEnvLevel2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x45u))
        val pitchEnvLevel3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x46u))
        val pitchEnvLevel4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x47u))

        val tvfFilterType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x48u), TvfFilterType.values())
        val tvfCutoffFrequency = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x49u))
        // TODO val tvfCutoffKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Au), -200..200)
        val tvfCutoffVelocityCurve = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Bu), 0..7)
        val tvfCutoffVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val tvfResonance = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val tvfResonanceVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu))
        val tvfEnvDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Fu))
        val tvfEnvVelocityCurve = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x50u), 0..7)
        val tvfEnvVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x51u))
        val tvfEnvTime1VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x52u))
        val tvfEnvTime4VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x53u))
        // TODO val tvfEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x54u), -100..100)
        val tvfEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x55u))
        val tvfEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x56u))
        val tvfEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x57u))
        val tvfEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x58u))
        val tvfEnvLevel0 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x59u))
        val tvfEnvLevel1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Au))
        val tvfEnvLevel2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val tvfEnvLevel3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Cu))
        val tvfEnvLevel4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Du))

        // TODO val biasLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x5Eu), -100..100)
        val biasPosition = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x5Fu))
        val biasDirection =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x60u), BiasDirection.values())
        val tvaLevelVelocityCurve = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x61u), 0..7)
        val tvaLevelVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x62u))
        val tvaEnvTime1VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x63u))
        val tvaEnvTime4VelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x64u))
        // TODO val tvaEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x65u), -100.100)
        val tvaEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x66u))
        val tvaEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x67u))
        val tvaEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x68u))
        val tvaEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x69u))
        val tvaEnvLevel1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Au))
        val tvaEnvLevel2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Bu))
        val tvaEnvLevel3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Cu))

        val lfo1WaveForm =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x6Du), LfoWaveForm.values())
        val lfo1Rate = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x6Eu), 0..149)
        val lfo1Offset = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x70u), LfoOffset.values())
        val lfo1RateDetune = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x71u))
        val lfo1DelayTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x72u))
        // TODO val lfo1Keyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x73u), -100..100)
        val lfo1FadeMode =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x74u), LfoFadeMode.values())
        val lfo1FadeTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x75u))
        val lfo1KeyTrigger = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x76u))
        val lfo1PitchDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x77u))
        val lfo1TvfDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x78u))
        val lfo1TvaDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x79u))
        val lfo1PanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x7Au))

        val lfo2WaveForm =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x7Bu), LfoWaveForm.values())
        val lfo2Rate = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x7Cu), 0..149)
        val lfo2Offset = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x7Eu), LfoOffset.values())
        val lfo2RateDetune = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x7Fu))
        val lfo2DelayTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x00u))
        // TODO val lfo2Keyfollow = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u), -100..100)
        val lfo2FadeMode = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x02u),
            LfoFadeMode.values()
        )
        val lfo2FadeTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x03u))
        val lfo2KeyTrigger = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x04u))
        val lfo2PitchDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val lfo2TvfDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u))
        val lfo2TvaDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val lfo2PanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u))

        val lfoStepType =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..1)
        val lfoStep1 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au), -36..36)
        val lfoStep2 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -36..36)
        val lfoStep3 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -36..36)
        val lfoStep4 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -36..36)
        val lfoStep5 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), -36..36)
        val lfoStep6 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), -36..36)
        val lfoStep7 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u), -36..36)
        val lfoStep8 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u), -36..36)
        val lfoStep9 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x12u), -36..36)
        val lfoStep10 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u), -36..36)
        val lfoStep11 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x14u), -36..36)
        val lfoStep12 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -36..36)
        val lfoStep13 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u), -36..36)
        val lfoStep14 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u), -36..36)
        val lfoStep15 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u), -36..36)
        val lfoStep16 =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u), -36..36)

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
                throw AssertionError("When reading $address to ${address.offsetBy(size)} size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address to ${address.offsetBy(size)} size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            }
        }
    }

    data class PcmSynthToneCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmSynthToneCommon2>() {
        override val size = Integra7Size(0x3Cu.toUInt7UsingValue())

        val toneCategory = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val undocumented = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x11u), 0..255)
        val phraseOctaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val tfxSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x33u))
        val phraseNmber = Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x38u), 0..65535)


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
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("When reading $address size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()).toString() }, 0x10)}", e)
            }
        }
    }

    data class SuperNaturalSynthToneBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        val part: IntegraPart
    ) : IntegraToneBuilder<SuperNaturalSynthTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x22.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalSynthToneCommonBuilder(deviceId, address)
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

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))
        val portamentoSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val portamentoTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val monoSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val octaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x15u), -3..3)
        val pithBendRangeUp = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u), 0..24)
        val pitchBendRangeDown = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x17u), 0..24)

        val partial1Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val partial1Select = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val partial2Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val partial2Select = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val partial3Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Du))
        val partial3Select = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Eu))

        val ringSwitch = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), RingSwitch.values())
        val tfxSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x20u))

        val unisonSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val portamentoMode =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x31u), PortamentoMode.values())
        val legatoSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x32u))
        val analogFeel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x34u))
        val waveShape = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x35u))
        val toneCategory = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x36u))
        val phraseNumber =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x37u), 0..65535)
        val phraseOctaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu), -3..3)
        val unisonSize = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), UnisonSize.values())

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

        val oscWaveForm =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x00u), SnSWaveForm.values())
        val oscWaveFormVariation =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x01u), SnsWaveFormVariation.values())
        val oscPitch = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x03u), -24..24)
        val oscDetune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x4Cu), -50..50)
        val oscPulseWidthModulationDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val oscPulseWidth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val oscPitchAttackTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u))
        val oscPitchEnvDecay = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x08u))
        val oscPitchEnvDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x09u))

        val filterMode =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Au), SnsFilterMode.values())
        val filterSlope =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), SnsFilterSlope.values())
        val filterCutoff = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))
//        val filterCutoffKeyflow = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), -100..100)
        val filterEnvVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val filterResonance = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val filterEnvAttackTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val filterEnvDecayTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val filterEnvSustainLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val filterEnvReleaseTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u))
        val filterEnvDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val ampLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x15u))
        val ampVelocitySens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x16u))
        val ampEnvAttackTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x17u))
        val ampEnvDecayTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u))
        val ampEnvSustainLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val ampEnvReleaseTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val ampPan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), -64..63)

        val lfoShape = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Cu), SnsLfoShape.values())
        val lfoRate = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Du))
        val lfoTempoSyncSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Eu))
        val lfoTempoSyncNote =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x1Fu), SnsLfoTempoSyncNote.values())
        val lfoFadeTime = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val lfoKeyTrigger = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x21u))
        val lfoPitchDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val lfoFilterDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x23u))
        val lfoAmpDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x24u))
        val lfoPanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x25u))

        val modulationShape =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x26u), SnsLfoShape.values())
        val modulationLfoRate = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x27u))
        val modulationLfoTempoSyncSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x28u))
        val modulationLfoTempoSyncNote =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x29u), SnsLfoTempoSyncNote.values())
        val oscPulseWidthShift = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Au))
        val modulationLfoPitchDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Cu))
        val modulationLfoFilterDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val modulationLfoAmpDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val modulationLfoPanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu))

        val cutoffAftertouchSens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val levelAftertouchSens = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x31u))

        val waveGain = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveNumber = Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u), 0..16384)
        val hpfCutoff = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x39u))
        val superSawDetune = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Au))
        val modulationLfoRateControl = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
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
        override val size = Integra7Size(UInt7(mlsb = 0x30u.toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = SuperNaturalAcousticToneCommonBuilder(deviceId, address)
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

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))

        val monoPoly = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x11u), MonoPoly.values())
        val portamentoTimeOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val cutoffOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x13u), -64..63)
        val resonanceOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x14u), -64..63)
        val attackTimeOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x15u), -64..63)
        val releaseTimeOffset = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x16u), -64..63)
        val vibratoRate = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x17u), -64..63)
        val vibratoDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x18u), -64..63)
        val vibratorDelay = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x19u), -64..63)
        val octaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Au), -3..3)
        val category = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val phraseNumber = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x1Cu), 0..255)
        val phraseOctaveShift = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x1Eu), -3..3)

        val tfxSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Fu))

        val instrumentVariation = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x20u))
        val instrumentNumber = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u))

        val modifyParameters = IntRange(0, 31).map {
            Integra7FieldType.UnsignedValueField(
                deviceId,
                address.offsetBy(lsb = 0x22u).offsetBy(lsb = 0x01u, factor = it)
            )
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
        override val size = Integra7Size(UInt7(mlsb = 0x4D.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalDrumKitCommonBuilder(deviceId, address)
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

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u))
        val ambienceLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val phraseNo = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x12u))
        val tfx = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x13u))

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

        val comp1Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x00u))
        val comp1AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x01u),
            SupernaturalDrumAttackTime.values()
        )
        val comp1ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x02u),
            SupernaturalDrumReleaseTime.values()
        )
        val comp1Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x03u))
        val comp1Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumRatio.values())
        val comp1OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x05u), 0..24)
        val eq1Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val eq1LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x07u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq1LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x08u), 0..30) // - 15
        val eq1MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x09u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq1MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Au), 0..30) // - 15
        val eq1MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Bu), SupernaturalDrumMidQ.values())
        val eq1HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x0Cu),
            SupernaturalDrumHighFrequency.values()
        )
        val eq1HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), 0..30) // - 15

        val comp2Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val comp2AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x0Fu),
            SupernaturalDrumAttackTime.values()
        )
        val comp2ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x10u),
            SupernaturalDrumReleaseTime.values()
        )
        val comp2Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val comp2Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x12u), SupernaturalDrumRatio.values())
        val comp2OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u), 0..24)
        val eq2Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val eq2LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x15u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq2LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u), 0..30) // - 15
        val eq2MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x17u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq2MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x18u), 0..30) // - 15
        val eq2MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x19u), SupernaturalDrumMidQ.values())
        val eq2HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x1Au),
            SupernaturalDrumHighFrequency.values()
        )
        val eq2HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..30) // - 15

        val comp3Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val comp3AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x1Du),
            SupernaturalDrumAttackTime.values()
        )
        val comp3ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x1Eu),
            SupernaturalDrumReleaseTime.values()
        )
        val comp3Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Fu))
        val comp3Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), SupernaturalDrumRatio.values())
        val comp3OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x21u), 0..24)
        val eq3Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x22u))
        val eq3LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x23u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq3LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x24u), 0..30) // - 15
        val eq3MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x25u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq3MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x26u), 0..30) // - 15
        val eq3MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x27u), SupernaturalDrumMidQ.values())
        val eq3HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x28u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq3HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x29u), 0..30) // - 15

        val comp4Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x2Au))
        val comp4AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x2Bu),
            SupernaturalDrumAttackTime.values()
        )
        val comp4ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x2Cu),
            SupernaturalDrumReleaseTime.values()
        )
        val comp4Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Du))
        val comp4Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Eu), SupernaturalDrumRatio.values())
        val comp4OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x2Fu), 0..24)
        val eq4Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val eq4LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x31u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq4LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x32u), 0..30) // - 15
        val eq4MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x33u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq4MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x34u), 0..30) // - 15
        val eq4MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x35u), SupernaturalDrumMidQ.values())
        val eq4HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x36u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq4HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x37u), 0..30) // - 15

        val comp5Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp5AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x39u),
            SupernaturalDrumAttackTime.values()
        )
        val comp5ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x3Au),
            SupernaturalDrumReleaseTime.values()
        )
        val comp5Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp5Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio.values())
        val comp5OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq5Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq5LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x3Fu),
            SupernaturalDrumLowFrequency.values()
        )
        val eq5LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq5MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x41u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq5MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq5MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ.values())
        val eq5HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x44u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq5HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

        val comp6Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp6AttackTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x39u),
            SupernaturalDrumAttackTime.values()
        )
        val comp6ReleaseTime = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x3Au),
            SupernaturalDrumReleaseTime.values()
        )
        val comp6Threshold = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp6Ratio =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio.values())
        val comp6OutputGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq6Switch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq6LowFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x3Fu),
            SupernaturalDrumLowFrequency.values()
        )
        val eq6LowGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq6MidFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x41u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq6MidGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq6MidQ =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ.values())
        val eq6HighFrequency = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x44u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq6HighGain = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

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

        val instrumentNumber =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x00u), 0..512)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x04u))
        val pan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x05u))
        val chorusSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x06u))
        val reverbSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x07u))
        val tune = Integra7FieldType.UnsignedMsbLsbFourNibbles(
            deviceId,
            address.offsetBy(lsb = 0x08u),
            8..248
        ) // TODO: convert!
        val attack = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..100)
        val decay = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), -63..0)
        val brilliance = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu), -15..12)
        val variation = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x0Fu),
            SuperNaturalDrumToneVariation.values()
        )
        val dynamicRange = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x10u), 0..63)
        val stereoWidth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x11u))
        val outputAssign = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x12u),
            SuperNaturalDrumToneOutput.values()
        )

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
        override val size = Integra7Size(UInt7(mmsb = 0x02u.toUByte7(), mlsb = 0x07Fu.toUByte7() , lsb = 0x7Fu.toUByte7()))

        val common = PcmDrumKitCommonBuilder(deviceId, address)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u)) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u)) // Same as SN-D
        val keys = IntRange(0, 78) // key 21 .. 108
            .map { PcmDrumKitPartialBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = it))  }
        val common2 = PcmDrumKitCommon2Builder(deviceId, address.offsetBy(mmsb = 0x02u, lsb = 0x00u))

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): PcmDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM Drum kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmDrumKit(
                common = common.interpret(startAddress, length, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb=0x00u), length, payload),
                keys = keys
                    .mapIndexed { index, b -> b.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = index), length, payload) },
                common2 = common2.interpret(startAddress.offsetBy(mmsb = 0x02u, lsb=0x00u), length, payload),
            )
        }
    }

    data class PcmDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)
        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu))

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
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x43u.toUByte7()))

        val name = Integra7FieldType.AsciiStringField(deviceId, address, length = 0x0C)

        val assignType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Cu), PcmDrumKitAssignType.values())
        val muteGroup = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0D0u), 0..31)

        val level = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val coarseTune = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val fineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x10u), -50..50)
        val randomPitchDepth =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x11u), RandomPithDepth.values())
        val pan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val randomPanDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x13u), 0..63)
        val alternatePanDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x14u))
        val envMode = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x15u), EnvMode.values())

        val outputLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x16u))
        val chorusSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x19u))
        val reverbSendLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x1Au))
        val outputAssign = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(lsb = 0x1Bu),
            SuperNaturalDrumToneOutput.values()
        )

        val pitchBendRange = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..48)
        val receiveExpression = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Du))
        val receiveHold1 = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x0Eu))

        val wmtVelocityControl =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x20u), WmtVelocityControl.values())

        val wmt1WaveSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x21u))
        val wmt1WaveGroupType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x22u), WaveGroupType.values())
        val wmt1WaveGroupId =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x23u), 0..16384)
        val wmt1WaveNumberL =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x27u), 0..16384)
        val wmt1WaveNumberR =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Bu), 0..16384)
        val wmt1WaveGain = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x2Fu), WaveGain.values())
        val wmt1WaveFxmSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x30u))
        val wmt1WaveFxmColor = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x31u), 0..3)
        val wmt1WaveFxmDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x32u), 0..16)
        val wmt1WaveTempoSync = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x33u))
        val wmt1WaveCoarseTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x34u), -48..48)
        val wmt1WaveFineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x35u), -50..50)
        val wmt1WavePan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
        val wmt1WaveRandomPanSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x37u))
        val wmt1WaveAlternatePanSwitch =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x38u), OffOnReverse.values())
        val wmt1WaveLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x39u))
        val wmt1VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Au))
        val wmt1VelocityFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Cu))

        val wmt2WaveSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val wmt2WaveGroupType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x3Fu), WaveGroupType.values())
        val wmt2WaveGroupId =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x40u), 0..16384)
        val wmt2WaveNumberL =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x44u), 0..16384)
        val wmt2WaveNumberR =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x48u), 0..16384)
        val wmt2WaveGain = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x4Cu), WaveGain.values())
        val wmt2WaveFxmSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x4Du))
        val wmt2WaveFxmColor = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..3)
        val wmt2WaveFxmDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x4Fu), 0..16)
        val wmt2WaveTempoSync = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x50u))
        val wmt2WaveCoarseTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x51u), -48..48)
        val wmt2WaveFineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x52u), -50..50)
        val wmt2WavePan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x53u), -64..63)
        val wmt2WaveRandomPanSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x54u))
        val wmt2WaveAlternatePanSwitch =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x55u), OffOnReverse.values())
        val wmt2WaveLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x56u))
        val wmt2VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x57u))
        val wmt2VelocityFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x59u))

        val wmt3WaveSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val wmt3WaveGroupType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x0Cu), WaveGroupType.values())
        val wmt3WaveGroupId =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du), 0..16384)
        val wmt3WaveNumberL =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u), 0..16384)
        val wmt3WaveNumberR =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u), 0..16384)
        val wmt3WaveGain = Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x69u), WaveGain.values())
        val wmt3WaveFxmSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x6Au))
        val wmt3WaveFxmColor = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Bu), 0..3)
        val wmt3WaveFxmDepth = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x6Cu), 0..16)
        val wmt3WaveTempoSync = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x6Du))
        val wmt3WaveCoarseTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x6Eu), -48..48)
        val wmt3WaveFineTune = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x6Fu), -50..50)
        val wmt3WavePan = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(lsb = 0x70u), -64..63)
        val wmt3WaveRandomPanSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x71u))
        val wmt3WaveAlternatePanSwitch =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x72u), OffOnReverse.values())
        val wmt3WaveLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(lsb = 0x73u))
        val wmt3VelocityRange = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x74u))
        val wmt3VelocityFadeWidth = Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x76u))

        val wmt4WaveSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x78u))
        val wmt4WaveGroupType =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(lsb = 0x79u), WaveGroupType.values())
        val wmt4WaveGroupId =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Au), 0..16384)
        val wmt4WaveNumberL =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Eu), 0..16384)
        val wmt4WaveNumberR =
            Integra7FieldType.UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x02u), 0..16384)
        val wmt4WaveGain =
            Integra7FieldType.EnumValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u), WaveGain.values())
        val wmt4WaveFxmSwitch =
            Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val wmt4WaveFxmColor =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u), 0..3)
        val wmt4WaveFxmDepth =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..16)
        val wmt4WaveTempoSync =
            Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au))
        val wmt4WaveCoarseTune =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -48..48)
        val wmt4WaveFineTune =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -50..50)
        val wmt4WavePan =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -64..63)
        val wmt4WaveRandomPanSwitch =
            Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu))
        val wmt4WaveAlternatePanSwitch = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu),
            OffOnReverse.values()
        )
        val wmt4WaveLevel = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u))
        val wmt4VelocityRange =
            Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u))
        val wmt4VelocityFadeWidth =
            Integra7FieldType.UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u))

        val pitchEnvDepth =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -12..12)
        val pitchEnvVelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u))
        val pitchEnvTime1VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u))
        val pitchEnvTime4VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u))

        val pitchEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u))
        val pitchEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Au))
        val pitchEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Bu))
        val pitchEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Cu))

        val pitchEnvLevel0 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Du))
        val pitchEnvLevel1 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Eu))
        val pitchEnvLevel2 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Fu))
        val pitchEnvLevel3 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x20u))
        val pitchEnvLevel4 = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x21u))

        val tvfFilterType = Integra7FieldType.EnumValueField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x22u),
            TvfFilterType.values()
        )
        val tvfCutoffFrequency =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x23u))
        val tvfCutoffVelocityCurve =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x24u), 0..7)
        val tvfCutoffVelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x25u))
        val tvfResonance = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x26u))
        val tvfResonanceVelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x27u))
        val tvfEnvDepth = Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x28u))
        val tvfEnvVelocityCurveType =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x29u), 0..7)
        val tvfEnvVelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Au))
        val tvfEnvTime1VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Bu))
        val tvfEnvTime4VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Cu))
        val tvfEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Du))
        val tvfEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Eu))
        val tvfEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Fu))
        val tvfEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x30u))
        val tvfEnvLevel0 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x31u))
        val tvfEnvLevel1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x32u))
        val tvfEnvLevel2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x33u))
        val tvfEnvLevel3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x34u))
        val tvfEnvLevel4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x35u))

        val tvaLevelVelocityCurve =
            Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x36u), 0..7)
        val tvaLevelVelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x37u))
        val tvaEnvTime1VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x38u))
        val tvaEnvTime4VelocitySens =
            Integra7FieldType.SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x39u))
        val tvaEnvTime1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Au))
        val tvaEnvTime2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Bu))
        val tvaEnvTime3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Cu))
        val tvaEnvTime4 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Du))
        val tvaEnvLevel1 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Eu))
        val tvaEnvLevel2 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Fu))
        val tvaEnvLevel3 = Integra7FieldType.UnsignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x40u))

        val oneShotMode = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x41u))

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

    data class PcmDrumKitCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address) :
        Integra7MemoryIO<PcmDrumKitCommon2>() {
        override val size = Integra7Size(0x32u)

        val phraseNumber = Integra7FieldType.UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x10u))
        val tfxSwitch = Integra7FieldType.BooleanValueField(deviceId, address.offsetBy(lsb = 0x31u))

        override fun interpret(
            startAddress: Integra7Address,
            length: Int,
            payload: SparseUByteArray
        ): PcmDrumKitCommon2 {
            assert(startAddress >= address)

            return PcmDrumKitCommon2(
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x10u), length, payload),
                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x0Cu), length, payload),
            )
        }
    }
}

