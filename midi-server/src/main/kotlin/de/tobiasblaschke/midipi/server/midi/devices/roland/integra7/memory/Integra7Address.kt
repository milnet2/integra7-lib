package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalArgumentException

@OptIn(ExperimentalUnsignedTypes::class)
data class Integra7Address(val address: UInt7): UByteSerializable, Comparable<Integra7Address> {
    enum class Integra7Ranges(val range: UIntRange) {
        SETUP(0x01000000u.toUInt7UsingByteRepresentation().rangeTo(0x017F7F7Fu.toUInt7UsingByteRepresentation())),
        SYSTEM(0x02000000u.toUInt7UsingByteRepresentation().rangeTo(0x027F7F7Fu.toUInt7UsingByteRepresentation())),
        STUDIO_SET(0x18000000u.toUInt7UsingByteRepresentation().rangeTo(0x187F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_1_PCM_SYNTH_TONE(   0x19000000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19007F7Fu.toUInt7UsingByteRepresentation())),
        PART_1_SN_SYNTH_TONE(    0x19010000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19017F7Fu.toUInt7UsingByteRepresentation())),
        PART_1_SN_ACOUSTIC_TONE( 0x19020000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19027F7Fu.toUInt7UsingByteRepresentation())),
        PART_1_SN_DRUM_KIT(      0x19030000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19037F7Fu.toUInt7UsingByteRepresentation())),
        PART_1_PCM_DRUM_KIT(     0x19100000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x191F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_2_PCM_SYNTH_TONE(   0x19200000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19207F7Fu.toUInt7UsingByteRepresentation())),
        PART_2_SN_SYNTH_TONE(    0x19210000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19217F7Fu.toUInt7UsingByteRepresentation())),
        PART_2_SN_ACOUSTIC_TONE( 0x19220000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19227F7Fu.toUInt7UsingByteRepresentation())),
        PART_2_SN_DRUM_KIT(      0x19230000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19237F7Fu.toUInt7UsingByteRepresentation())),
        PART_2_PCM_DRUM_KIT(     0x19300000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x193F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_3_PCM_SYNTH_TONE(   0x19400000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19407F7Fu.toUInt7UsingByteRepresentation())),
        PART_3_SN_SYNTH_TONE(    0x19410000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19417F7Fu.toUInt7UsingByteRepresentation())),
        PART_3_SN_ACOUSTIC_TONE( 0x19420000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19427F7Fu.toUInt7UsingByteRepresentation())),
        PART_3_SN_DRUM_KIT(      0x19430000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19437F7Fu.toUInt7UsingByteRepresentation())),
        PART_3_PCM_DRUM_KIT(     0x19500000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x195F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_4_PCM_SYNTH_TONE(   0x19600000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19607F7Fu.toUInt7UsingByteRepresentation())),
        PART_4_SN_SYNTH_TONE(    0x19610000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19617F7Fu.toUInt7UsingByteRepresentation())),
        PART_4_SN_ACOUSTIC_TONE( 0x19620000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19627F7Fu.toUInt7UsingByteRepresentation())),
        PART_4_SN_DRUM_KIT(      0x19630000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x19637F7Fu.toUInt7UsingByteRepresentation())),
        PART_4_PCM_DRUM_KIT(     0x19700000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x197F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_5_PCM_SYNTH_TONE(   0x1A000000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A007F7Fu.toUInt7UsingByteRepresentation())),
        PART_5_SN_SYNTH_TONE(    0x1A010000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A017F7Fu.toUInt7UsingByteRepresentation())),
        PART_5_SN_ACOUSTIC_TONE( 0x1A020000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A027F7Fu.toUInt7UsingByteRepresentation())),
        PART_5_SN_DRUM_KIT(      0x1A030000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A037F7Fu.toUInt7UsingByteRepresentation())),
        PART_5_PCM_DRUM_KIT(     0x1A100000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A1F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_6_PCM_SYNTH_TONE(   0x1A200000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A207F7Fu.toUInt7UsingByteRepresentation())),
        PART_6_SN_SYNTH_TONE(    0x1A210000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A217F7Fu.toUInt7UsingByteRepresentation())),
        PART_6_SN_ACOUSTIC_TONE( 0x1A220000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A227F7Fu.toUInt7UsingByteRepresentation())),
        PART_6_SN_DRUM_KIT(      0x1A230000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A237F7Fu.toUInt7UsingByteRepresentation())),
        PART_6_PCM_DRUM_KIT(     0x1A300000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A3F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_7_PCM_SYNTH_TONE(   0x1A400000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A407F7Fu.toUInt7UsingByteRepresentation())),
        PART_7_SN_SYNTH_TONE(    0x1A410000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A417F7Fu.toUInt7UsingByteRepresentation())),
        PART_7_SN_ACOUSTIC_TONE( 0x1A420000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A427F7Fu.toUInt7UsingByteRepresentation())),
        PART_7_SN_DRUM_KIT(      0x1A430000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A437F7Fu.toUInt7UsingByteRepresentation())),
        PART_7_PCM_DRUM_KIT(     0x1A500000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A5F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_8_PCM_SYNTH_TONE(   0x1A600000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A607F7Fu.toUInt7UsingByteRepresentation())),
        PART_8_SN_SYNTH_TONE(    0x1A610000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A617F7Fu.toUInt7UsingByteRepresentation())),
        PART_8_SN_ACOUSTIC_TONE( 0x1A620000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A627F7Fu.toUInt7UsingByteRepresentation())),
        PART_8_SN_DRUM_KIT(      0x1A630000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A637F7Fu.toUInt7UsingByteRepresentation())),
        PART_8_PCM_DRUM_KIT(     0x1A700000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1A7F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_9_PCM_SYNTH_TONE(   0x1B000000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B007F7Fu.toUInt7UsingByteRepresentation())),
        PART_9_SN_SYNTH_TONE(    0x1B010000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B017F7Fu.toUInt7UsingByteRepresentation())),
        PART_9_SN_ACOUSTIC_TONE( 0x1B020000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B027F7Fu.toUInt7UsingByteRepresentation())),
        PART_9_SN_DRUM_KIT(      0x1B030000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B037F7Fu.toUInt7UsingByteRepresentation())),
        PART_9_PCM_DRUM_KIT(     0x1B100000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B1F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_10_PCM_SYNTH_TONE(  0x1B200000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B207F7Fu.toUInt7UsingByteRepresentation())),
        PART_10_SN_SYNTH_TONE(   0x1B210000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B217F7Fu.toUInt7UsingByteRepresentation())),
        PART_10_SN_ACOUSTIC_TONE(
            0x1B220000u.toUInt7UsingByteRepresentation().rangeTo(0x1B227F7Fu.toUInt7UsingByteRepresentation())),
        PART_10_SN_DRUM_KIT(     0x1B230000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B237F7Fu.toUInt7UsingByteRepresentation())),
        PART_10_PCM_DRUM_KIT(    0x1B300000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B3F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_11_PCM_SYNTH_TONE(  0x1B400000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B407F7Fu.toUInt7UsingByteRepresentation())),
        PART_11_SN_SYNTH_TONE(   0x1B410000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B417F7Fu.toUInt7UsingByteRepresentation())),
        PART_11_SN_ACOUSTIC_TONE(
            0x1B420000u.toUInt7UsingByteRepresentation().rangeTo(0x1B427F7Fu.toUInt7UsingByteRepresentation())),
        PART_11_SN_DRUM_KIT(     0x1B430000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B437F7Fu.toUInt7UsingByteRepresentation())),
        PART_11_PCM_DRUM_KIT(    0x1B500000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B5F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_12_PCM_SYNTH_TONE(  0x1B600000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B607F7Fu.toUInt7UsingByteRepresentation())),
        PART_12_SN_SYNTH_TONE(   0x1B610000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B617F7Fu.toUInt7UsingByteRepresentation())),
        PART_12_SN_ACOUSTIC_TONE(
            0x1B620000u.toUInt7UsingByteRepresentation().rangeTo(0x1B627F7Fu.toUInt7UsingByteRepresentation())),
        PART_12_SN_DRUM_KIT(     0x1B630000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B637F7Fu.toUInt7UsingByteRepresentation())),
        PART_12_PCM_DRUM_KIT(    0x1B700000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1B7F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_13_PCM_SYNTH_TONE(  0x1C000000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C007F7Fu.toUInt7UsingByteRepresentation())),
        PART_13_SN_SYNTH_TONE(   0x1C010000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C017F7Fu.toUInt7UsingByteRepresentation())),
        PART_13_SN_ACOUSTIC_TONE(
            0x1C020000u.toUInt7UsingByteRepresentation().rangeTo(0x1C027F7Fu.toUInt7UsingByteRepresentation())),
        PART_13_SN_DRUM_KIT(     0x1C030000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C037F7Fu.toUInt7UsingByteRepresentation())),
        PART_13_PCM_DRUM_KIT(    0x1C100000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C1F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_14_PCM_SYNTH_TONE(  0x1C200000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C207F7Fu.toUInt7UsingByteRepresentation())),
        PART_14_SN_SYNTH_TONE(   0x1C210000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C217F7Fu.toUInt7UsingByteRepresentation())),
        PART_14_SN_ACOUSTIC_TONE(
            0x1C220000u.toUInt7UsingByteRepresentation().rangeTo(0x1C227F7Fu.toUInt7UsingByteRepresentation())),
        PART_14_SN_DRUM_KIT(     0x1C230000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C237F7Fu.toUInt7UsingByteRepresentation())),
        PART_14_PCM_DRUM_KIT(    0x1C300000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C3F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_15_PCM_SYNTH_TONE(  0x1C400000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C407F7Fu.toUInt7UsingByteRepresentation())),
        PART_15_SN_SYNTH_TONE(   0x1C410000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C417F7Fu.toUInt7UsingByteRepresentation())),
        PART_15_SN_ACOUSTIC_TONE(
            0x1C420000u.toUInt7UsingByteRepresentation().rangeTo(0x1C427F7Fu.toUInt7UsingByteRepresentation())),
        PART_15_SN_DRUM_KIT(     0x1C430000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C437F7Fu.toUInt7UsingByteRepresentation())),
        PART_15_PCM_DRUM_KIT(    0x1C500000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C5F7F7Fu.toUInt7UsingByteRepresentation())),

        PART_16_PCM_SYNTH_TONE(  0x1C600000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C607F7Fu.toUInt7UsingByteRepresentation())),
        PART_16_SN_SYNTH_TONE(   0x1C610000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C617F7Fu.toUInt7UsingByteRepresentation())),
        PART_16_SN_ACOUSTIC_TONE(
            0x1C620000u.toUInt7UsingByteRepresentation().rangeTo(0x1C627F7Fu.toUInt7UsingByteRepresentation())),
        PART_16_SN_DRUM_KIT(     0x1C630000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C637F7Fu.toUInt7UsingByteRepresentation())),
        PART_16_PCM_DRUM_KIT(    0x1C700000u.toUInt7UsingByteRepresentation()
            .rangeTo(0x1C7F7F7Fu.toUInt7UsingByteRepresentation()));

        fun begin(): Integra7Address =
            Integra7Address(range.first.toUInt7UsingValue())

        fun end(): Integra7Address =
            Integra7Address(range.last.toUInt7UsingValue())

        fun size(): Integra7Size =
            Integra7Size((range.first - range.last + 1u).toUInt7UsingValue())
    }

    override fun bytes(): UByteArray =
        address.toUByteArrayLittleEndian()

    fun successor(): Integra7Address =
        Integra7Address((address + 1u).toUInt7UsingValue())

    fun offsetBy(msb: UByte = 0x00u, mmsb: UByte = 0x00u, mlsb: UByte = 0x00u, lsb: UByte, factor: Int = 1): Integra7Address =
        offsetBy(msb.toUByte7(), mmsb.toUByte7(), mlsb.toUByte7(), lsb.toUByte7(), factor)

    fun offsetBy(msb: UByte7 = UByte7.MIN_VALUE, mmsb: UByte7 = UByte7.MIN_VALUE, mlsb: UByte7 = UByte7.MIN_VALUE, lsb: UByte7 = UByte7.MIN_VALUE, factor: Int = 1): Integra7Address =
        offsetBy(UInt7(msb, mmsb, mlsb, lsb), factor)

    fun offsetBy(addendum: UInt7, factor: Int = 1): Integra7Address =
        offsetBy(Integra7Size(addendum), factor)

    fun offsetBy(addendum: Integra7Size, factor: Int = 1): Integra7Address {
        return when {
            factor < 0 -> throw IllegalArgumentException()
            factor == 0 -> this
            factor == 1 -> Integra7Address((address + addendum.size).toUInt7UsingValue())
            else -> Integra7Address((address + addendum.size).toUInt7UsingValue()).offsetBy(addendum, factor - 1)
        }
    }

    /*
     * Returns the size of the address-range
     */
    operator fun minus(other: Integra7Address): Integra7Size =
        Integra7Size((address.toUInt() - other.address.toUInt()).toUInt7UsingValue())

    operator fun plus(other: Integra7Size): Integra7Address =
        Integra7Address((this.address + other.size).toUInt7UsingValue())

    fun fullByteAddress(): Int =
        address.toInt()

    override fun compareTo(other: Integra7Address): Int =
        address.compareTo(other.address)

    fun rangeName(): String = Integra7Ranges.values()
        .firstOrNull { it.range.contains(address.toUInt()) }
        ?.let { it.toString() }
        ?: "Unknown range $this"

    fun toStringDetailed(): String =
        "${toString()} in range ${rangeName()}"

    override fun toString(): String =
        address.toString()
}

data class Integra7Size(val size: UInt7): UByteSerializable {
    constructor(msb: UByte = 0x00u, mmsb: UByte = 0x00u, mlsb: UByte = 0x00u, lsb: UByte):
            this(UInt7(msb.toUByte7(), mmsb.toUByte7(), mlsb.toUByte7(), lsb.toUByte7()))

    @Deprecated("Switch to UInt7...")
    constructor(fullByteSize: UInt) : this(fullByteSize.toUInt7UsingValue())

    companion object {
        val ONE_BYTE = Integra7Size(0x01u.toUInt7UsingValue())
        val TWO_BYTES = Integra7Size(0x02u.toUInt7UsingValue())
        val FOUR_BYTES = Integra7Size(0x04u.toUInt7UsingValue())
    }

    override fun bytes(): UByteArray =
        size.toUByteArrayLittleEndian()

    operator fun minus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() - other).toUInt())

    operator fun plus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() + other).toUInt())

    operator fun plus(other: Integra7Size): Integra7Size =
        Integra7Size(this.size + other.size)

    fun fullByteSize(): Int =
        size.toInt()

    override fun toString(): String =
        fullByteSize().toString() + " Integra: " + size
}