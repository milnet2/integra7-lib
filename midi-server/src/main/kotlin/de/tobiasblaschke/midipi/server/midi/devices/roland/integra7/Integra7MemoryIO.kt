package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain.*
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.*
import de.tobiasblaschke.midipi.server.utils.*


abstract class Integra7MemoryIO<T> {
    internal abstract val deviceId: DeviceId
    internal abstract val address: Integra7Address
    internal abstract val size: Integra7Size

    protected fun assertCovered(payload: SparseUByteArray, message: () -> String) {
        assert(isCovering(payload)) {
            message() +
            " ($address..${address.offsetBy(size - 1)}) in range ${address.rangeName()}\n" +
             payload.hexDump(
                 addressTransform = { Integra7Address(it.toUInt7()).toString() },
                 chunkSize = 0x10)
        }
    }

    fun isCovering(payload: SparseUByteArray): Boolean =
        payload.contains(address.fullByteAddress()) &&
        payload.contains(address.offsetBy(size - 1).fullByteAddress())

    fun isCovering(address: Integra7Address) =
        address >= this.address && address <= this.address.offsetBy(size)

    protected fun SparseUByteArray.integraStartAddress(): Integra7Address =
        Integra7Address(this.startAddress.toUInt().toUInt7UsingValue())

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

    abstract fun deserialize(payload: SparseUByteArray): T

}

// ----------------------------------------------------

class AddressRequestBuilder(private val deviceId: DeviceId) {
//    val undocumented = UndocumentedRequestBuilder(deviceId, Integra7Address(0x0F000402))    // TODO: This is not a typical address-request!!
    val setup = Integra7GlobalSysEx.SetupRequestBuilder(deviceId, Integra7Address.Integra7Ranges.SETUP.begin())
    val system = Integra7GlobalSysEx.SystemCommonRequestBuilder(deviceId, Integra7Address.Integra7Ranges.SYSTEM.begin())
    val studioSet =
        Integra7GlobalSysEx.StudioSetAddressRequestBuilder(deviceId, Integra7Address.Integra7Ranges.STUDIO_SET.begin())

    val tones: Map<IntegraPart, ToneAddressRequestBuilder> = IntegraPart.values()
        .map { it to ToneAddressRequestBuilder(deviceId, Integra7Address.Integra7Ranges.PART_1_PCM_SYNTH_TONE.begin()
            .offsetBy(UInt7(mmsb = 0x20u.toUByte7()), it.zeroBased), it) }
        .toMap()

    fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Values {
        //assert(length <= size.toInt())
        assert(payload.size <= length)

        return Values(
            tone = IntegraPart.values()
                .map { it to this.tones.getValue(it).deserialize(payload) }
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



data class ToneAddressRequestBuilder(
    override val deviceId: DeviceId,
    override val address: Integra7Address,
    val part: IntegraPart
): Integra7MemoryIO<ToneAddressRequestBuilder.TemporaryTone>() {
    override val size: Integra7Size = Integra7Size(UInt7(mmsb = 0x20.toUByte7()))

    val pcmSynthTone = Integra7PartSysEx.PcmSynth7PartSysEx(deviceId, address, part)
    val snSynthTone = Integra7PartSysEx.SuperNaturalSynth7PartSysEx(deviceId, address.offsetBy(mmsb = 0x01u.toUByte7()), part)
    val snAcousticTone = Integra7PartSysEx.SuperNaturalAcoustic7PartSysEx(deviceId, address.offsetBy(mmsb = 0x02u.toUByte7()), part)
    val snaDrumKit = Integra7PartSysEx.SuperNaturalDrumKitBuilder(deviceId, address.offsetBy(mmsb = 0x03u.toUByte7()), part)
    val pcmDrumKit = Integra7PartSysEx.PcmDrumKitBuilder(deviceId, address.offsetBy(mmsb = 0x10u.toUByte7()), part)

    override fun deserialize(payload: SparseUByteArray): TemporaryTone {
        return when {
            pcmSynthTone.isCovering(payload) -> TemporaryTone(
                tone = pcmSynthTone.deserialize(payload))
            snSynthTone.isCovering(payload) -> TemporaryTone(
                tone = snSynthTone.deserialize(payload))
            snAcousticTone.isCovering(payload) -> TemporaryTone(
                tone = snAcousticTone.deserialize(payload))
            snaDrumKit.isCovering(payload) -> TemporaryTone(
                tone = snaDrumKit.deserialize(payload))
            pcmDrumKit.isCovering(payload) -> TemporaryTone(
                tone = pcmDrumKit.deserialize(payload))
            else -> throw Integra7FieldType.FieldReadException(
                address, size, payload,
                """
                    |Unsupported part-type for $part should be either of
                    |* ${pcmSynthTone.address} .. ${pcmSynthTone.address + pcmSynthTone.size}  ${pcmSynthTone.address.rangeName()}
                    |* ${snSynthTone.address} .. ${snSynthTone.address + snSynthTone.size}  ${snSynthTone.address.rangeName()}
                    |* ${snAcousticTone.address} .. ${snAcousticTone.address + snAcousticTone.size}  ${snAcousticTone.address.rangeName()}
                    |* ${snaDrumKit.address} .. ${snaDrumKit.address + snaDrumKit.size}  ${snaDrumKit.address.rangeName()}
                    |* ${pcmDrumKit.address} .. ${pcmDrumKit.address + pcmDrumKit.size}  ${pcmDrumKit.address.rangeName()}
                    |
                """.trimMargin())
        }
    }

    data class TemporaryTone(
        val tone: IntegraTone
    )
}

