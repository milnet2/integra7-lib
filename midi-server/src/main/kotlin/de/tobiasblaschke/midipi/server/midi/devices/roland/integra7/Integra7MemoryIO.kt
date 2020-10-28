package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.toHexString
import kotlin.math.min


abstract class Integra7MemoryIO {
    internal abstract val deviceId: DeviceId
    internal abstract val address: Integra7Address
    internal abstract val size: UInt

    fun asDataRequest1(): RolandIntegra7MidiMessage {
        return RolandIntegra7MidiMessage.IntegraSysExReadRequest(
            deviceId = deviceId,
            address = address,
            size = size,
            checkSum = checkSum(size.toByteArrayMsbFirst()))
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

    abstract fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): Any
}

fun UInt.toByteArrayMsbFirst(): UByteArray {
    val lsb = this.and(0xFFu).toUByte()
    val mlsb = (this / 0x100u).and(0xFFu).toUByte()
    val mmsb = (this / 0x10000u).and(0xFFu).toUByte()
    val msb = (this / 0x1000000u).toUByte()
    return ubyteArrayOf(msb, mmsb, mlsb, lsb)
}
// ----------------------------------------------------

class AddressRequestBuilder(deviceId: DeviceId) {
    val studioSet = StudioSetAddressRequestBuilder(deviceId, Integra7Address(0x18000000))
    val tone1 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 0))
    val tone2 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 1))
    val tone3 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 2))
    val tone4 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 3))
    val tone5 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 4))
    val tone6 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 5))
    val tone7 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 6))
    val tone8 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 7))
    val tone9 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 8))
    val tone10 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 9))
    val tone11 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 10))
    val tone12 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 11))
    val tone13 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 12))
    val tone14 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 13))
    val tone15 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 14))
    val tone16 = ToneAddressRequestBuilder(deviceId, Integra7Address(0x19000000 + 0x200000 * 15))

    fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): Values {
        //assert(length <= size.toInt())
        assert(payload.size <= length)

        return Values(
            tone1 = tone1.interpret(startAddress, 0x303C, payload.copyOfRange(0, 0x303C))
        )
    }

    data class Values(
        val tone1: ToneAddressRequestBuilder.TemporaryTone
    )
}

/* internal abstract */ data class StudioSetAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x1000000u

    val common = StudioSetCommonAddressRequestBuilder(deviceId, address.offsetBy(0x000000))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): StudioSet {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        return StudioSet(
            common = common.interpret(startAddress, min(payload.size, 0x54), payload.copyOfRange(0, min(payload.size, 0x54)))
        )
    }

    data class StudioSet(
        val common: StudioSetCommonAddressRequestBuilder.StudioSetCommon
    )
}

/* internal abstract */ data class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x000054u

    val name = StudioSetCommonName(deviceId, address.offsetBy(0x000000))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): StudioSetCommon {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        return StudioSetCommon(
            name = name.interpret(startAddress, min(payload.size, 0x0F), payload.copyOfRange(0, min(payload.size, 0x0F)))
        )
    }

    data class StudioSetCommon(
        val name: String
    )
}

/* internal */ data class StudioSetCommonName(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x0Fu

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): String {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        val name = payload.joinToString(
            separator = "",
            transform = { if (it in 0x20u .. 0x7Du) it.toByte().toChar().toString() else "." })
        return name
    }
}

/* internal abstract */ data class ToneAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x200000u

    val pcmSynthTone = PcmSynthToneBuilder(deviceId, address.offsetBy(0x000000))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): TemporaryTone {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        return TemporaryTone(
            pcmSynthTone = pcmSynthTone.interpret(startAddress, min(payload.size, 0x303C), payload.copyOfRange(0, min(payload.size, 0x303C)))
        )
    }

    data class TemporaryTone(
        val pcmSynthTone: PcmSynthToneBuilder.PcmSynthTone
    )
}

/* internal */ data class PcmSynthToneBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x303Cu

    val common = PcmSynthToneCommonBuilder(deviceId, address.offsetBy(0x000000))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): PcmSynthTone {
        assert(startAddress.address >= address.address)
        //assert(length <= size.toInt())
        assert(payload.size <= length)

        return PcmSynthTone(
            common = common.interpret(startAddress, 0x50, payload.copyOfRange(0, 0x50))
        )
    }

    data class PcmSynthTone(
        val common: PcmSynthToneCommonBuilder.PcmSynthToneCommon
    )
}

/* internal */ data class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x50u

    val name = PcmSynthToneCommonName(deviceId, address.offsetBy(0x000000))

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): PcmSynthToneCommon {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        return PcmSynthToneCommon(
            name = name.interpret(startAddress, 0x0C, payload.copyOfRange(0, 0x1C)),
            level = payload[0x0E].toInt(),
            pan = payload[0x0F].toInt() - 64
        )
    }

    data class PcmSynthToneCommon(
        val name: String,
        val level: Int,
        val pan: Int
    )
}

/* internal */ data class PcmSynthToneCommonName(override val deviceId: DeviceId, override val address: Integra7Address): Integra7MemoryIO() {
    override val size: UInt = 0x0Cu

    override fun interpret(startAddress: Integra7Address, length: Int, payload: UByteArray): String {
        assert(startAddress.address >= address.address)
        assert(length <= size.toInt())
        assert(payload.size <= length)

        return payload.joinToString(
            separator = "",
            transform = { if (it in 0x20u .. 0x7Du) it.toByte().toChar().toString() else "." })
    }
}

// ----------------------------------------------------


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

    override fun toString(): String =
        String.format("0x%08X", address)
}