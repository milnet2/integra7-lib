package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalArgumentException

data class Integra7Address(val address: UInt7): UByteSerializable, Comparable<Integra7Address> {
    companion object {
        private fun literally(repr: UInt): UInt7 =
            repr.toUInt7UsingByteRepresentation()
    }

    enum class Integra7Ranges(val range: UIntRange) {
        SETUP(literally(0x01000000u).rangeTo(literally(0x017F7F7Fu))),
        SYSTEM(literally(0x02000000u).rangeTo(literally(0x027F7F7Fu))),
        STUDIO_SET(literally(0x18000000u).rangeTo(literally(0x187F7F7Fu))),

        PART_1_PCM_SYNTH_TONE(   literally(0x19000000u).rangeTo(literally(0x19007F7Fu))),
        PART_1_SN_SYNTH_TONE(    literally(0x19010000u).rangeTo(literally(0x19017F7Fu))),
        PART_1_SN_ACOUSTIC_TONE( literally(0x19020000u).rangeTo(literally(0x19027F7Fu))),
        PART_1_SN_DRUM_KIT(      literally(0x19030000u).rangeTo(literally(0x19037F7Fu))),
        PART_1_PCM_DRUM_KIT(     literally(0x19100000u).rangeTo(literally(0x191F7F7Fu))),

        PART_2_PCM_SYNTH_TONE(   literally(0x19200000u).rangeTo(literally(0x19207F7Fu))),
        PART_2_SN_SYNTH_TONE(    literally(0x19210000u).rangeTo(literally(0x19217F7Fu))),
        PART_2_SN_ACOUSTIC_TONE( literally(0x19220000u).rangeTo(literally(0x19227F7Fu))),
        PART_2_SN_DRUM_KIT(      literally(0x19230000u).rangeTo(literally(0x19237F7Fu))),
        PART_2_PCM_DRUM_KIT(     literally(0x19300000u).rangeTo(literally(0x193F7F7Fu))),

        PART_3_PCM_SYNTH_TONE(   literally(0x19400000u).rangeTo(literally(0x19407F7Fu))),
        PART_3_SN_SYNTH_TONE(    literally(0x19410000u).rangeTo(literally(0x19417F7Fu))),
        PART_3_SN_ACOUSTIC_TONE( literally(0x19420000u).rangeTo(literally(0x19427F7Fu))),
        PART_3_SN_DRUM_KIT(      literally(0x19430000u).rangeTo(literally(0x19437F7Fu))),
        PART_3_PCM_DRUM_KIT(     literally(0x19500000u).rangeTo(literally(0x195F7F7Fu))),

        PART_4_PCM_SYNTH_TONE(   literally(0x19600000u).rangeTo(literally(0x19607F7Fu))),
        PART_4_SN_SYNTH_TONE(    literally(0x19610000u).rangeTo(literally(0x19617F7Fu))),
        PART_4_SN_ACOUSTIC_TONE( literally(0x19620000u).rangeTo(literally(0x19627F7Fu))),
        PART_4_SN_DRUM_KIT(      literally(0x19630000u).rangeTo(literally(0x19637F7Fu))),
        PART_4_PCM_DRUM_KIT(     literally(0x19700000u).rangeTo(literally(0x197F7F7Fu))),

        PART_5_PCM_SYNTH_TONE(   literally(0x1A000000u).rangeTo(literally(0x1A007F7Fu))),
        PART_5_SN_SYNTH_TONE(    literally(0x1A010000u).rangeTo(literally(0x1A017F7Fu))),
        PART_5_SN_ACOUSTIC_TONE( literally(0x1A020000u).rangeTo(literally(0x1A027F7Fu))),
        PART_5_SN_DRUM_KIT(      literally(0x1A030000u).rangeTo(literally(0x1A037F7Fu))),
        PART_5_PCM_DRUM_KIT(     literally(0x1A100000u).rangeTo(literally(0x1A1F7F7Fu))),

        PART_6_PCM_SYNTH_TONE(   literally(0x1A200000u).rangeTo(literally(0x1A207F7Fu))),
        PART_6_SN_SYNTH_TONE(    literally(0x1A210000u).rangeTo(literally(0x1A217F7Fu))),
        PART_6_SN_ACOUSTIC_TONE( literally(0x1A220000u).rangeTo(literally(0x1A227F7Fu))),
        PART_6_SN_DRUM_KIT(      literally(0x1A230000u).rangeTo(literally(0x1A237F7Fu))),
        PART_6_PCM_DRUM_KIT(     literally(0x1A300000u).rangeTo(literally(0x1A3F7F7Fu))),

        PART_7_PCM_SYNTH_TONE(   literally(0x1A400000u).rangeTo(literally(0x1A407F7Fu))),
        PART_7_SN_SYNTH_TONE(    literally(0x1A410000u).rangeTo(literally(0x1A417F7Fu))),
        PART_7_SN_ACOUSTIC_TONE( literally(0x1A420000u).rangeTo(literally(0x1A427F7Fu))),
        PART_7_SN_DRUM_KIT(      literally(0x1A430000u).rangeTo(literally(0x1A437F7Fu))),
        PART_7_PCM_DRUM_KIT(     literally(0x1A500000u).rangeTo(literally(0x1A5F7F7Fu))),

        PART_8_PCM_SYNTH_TONE(   literally(0x1A600000u).rangeTo(literally(0x1A607F7Fu))),
        PART_8_SN_SYNTH_TONE(    literally(0x1A610000u).rangeTo(literally(0x1A617F7Fu))),
        PART_8_SN_ACOUSTIC_TONE( literally(0x1A620000u).rangeTo(literally(0x1A627F7Fu))),
        PART_8_SN_DRUM_KIT(      literally(0x1A630000u).rangeTo(literally(0x1A637F7Fu))),
        PART_8_PCM_DRUM_KIT(     literally(0x1A700000u).rangeTo(literally(0x1A7F7F7Fu))),

        PART_9_PCM_SYNTH_TONE(   literally(0x1B000000u).rangeTo(literally(0x1B007F7Fu))),
        PART_9_SN_SYNTH_TONE(    literally(0x1B010000u).rangeTo(literally(0x1B017F7Fu))),
        PART_9_SN_ACOUSTIC_TONE( literally(0x1B020000u).rangeTo(literally(0x1B027F7Fu))),
        PART_9_SN_DRUM_KIT(      literally(0x1B030000u).rangeTo(literally(0x1B037F7Fu))),
        PART_9_PCM_DRUM_KIT(     literally(0x1B100000u).rangeTo(literally(0x1B1F7F7Fu))),

        PART_10_PCM_SYNTH_TONE(  literally(0x1B200000u).rangeTo(literally(0x1B207F7Fu))),
        PART_10_SN_SYNTH_TONE(   literally(0x1B210000u).rangeTo(literally(0x1B217F7Fu))),
        PART_10_SN_ACOUSTIC_TONE(literally(0x1B220000u).rangeTo(literally(0x1B227F7Fu))),
        PART_10_SN_DRUM_KIT(     literally(0x1B230000u).rangeTo(literally(0x1B237F7Fu))),
        PART_10_PCM_DRUM_KIT(    literally(0x1B300000u).rangeTo(literally(0x1B3F7F7Fu))),

        PART_11_PCM_SYNTH_TONE(  literally(0x1B400000u).rangeTo(literally(0x1B407F7Fu))),
        PART_11_SN_SYNTH_TONE(   literally(0x1B410000u).rangeTo(literally(0x1B417F7Fu))),
        PART_11_SN_ACOUSTIC_TONE(literally(0x1B420000u).rangeTo(literally(0x1B427F7Fu))),
        PART_11_SN_DRUM_KIT(     literally(0x1B430000u).rangeTo(literally(0x1B437F7Fu))),
        PART_11_PCM_DRUM_KIT(    literally(0x1B500000u).rangeTo(literally(0x1B5F7F7Fu))),

        PART_12_PCM_SYNTH_TONE(  literally(0x1B600000u).rangeTo(literally(0x1B607F7Fu))),
        PART_12_SN_SYNTH_TONE(   literally(0x1B610000u).rangeTo(literally(0x1B617F7Fu))),
        PART_12_SN_ACOUSTIC_TONE(literally(0x1B620000u).rangeTo(literally(0x1B627F7Fu))),
        PART_12_SN_DRUM_KIT(     literally(0x1B630000u).rangeTo(literally(0x1B637F7Fu))),
        PART_12_PCM_DRUM_KIT(    literally(0x1B700000u).rangeTo(literally(0x1B7F7F7Fu))),

        PART_13_PCM_SYNTH_TONE(  literally(0x1C000000u).rangeTo(literally(0x1C007F7Fu))),
        PART_13_SN_SYNTH_TONE(   literally(0x1C010000u).rangeTo(literally(0x1C017F7Fu))),
        PART_13_SN_ACOUSTIC_TONE(literally(0x1C020000u).rangeTo(literally(0x1C027F7Fu))),
        PART_13_SN_DRUM_KIT(     literally(0x1C030000u).rangeTo(literally(0x1C037F7Fu))),
        PART_13_PCM_DRUM_KIT(    literally(0x1C100000u).rangeTo(literally(0x1C1F7F7Fu))),

        PART_14_PCM_SYNTH_TONE(  literally(0x1C200000u).rangeTo(literally(0x1C207F7Fu))),
        PART_14_SN_SYNTH_TONE(   literally(0x1C210000u).rangeTo(literally(0x1C217F7Fu))),
        PART_14_SN_ACOUSTIC_TONE(literally(0x1C220000u).rangeTo(literally(0x1C227F7Fu))),
        PART_14_SN_DRUM_KIT(     literally(0x1C230000u).rangeTo(literally(0x1C237F7Fu))),
        PART_14_PCM_DRUM_KIT(    literally(0x1C300000u).rangeTo(literally(0x1C3F7F7Fu))),

        PART_15_PCM_SYNTH_TONE(  literally(0x1C400000u).rangeTo(literally(0x1C407F7Fu))),
        PART_15_SN_SYNTH_TONE(   literally(0x1C410000u).rangeTo(literally(0x1C417F7Fu))),
        PART_15_SN_ACOUSTIC_TONE(literally(0x1C420000u).rangeTo(literally(0x1C427F7Fu))),
        PART_15_SN_DRUM_KIT(     literally(0x1C430000u).rangeTo(literally(0x1C437F7Fu))),
        PART_15_PCM_DRUM_KIT(    literally(0x1C500000u).rangeTo(literally(0x1C5F7F7Fu))),

        PART_16_PCM_SYNTH_TONE(  literally(0x1C600000u).rangeTo(literally(0x1C607F7Fu))),
        PART_16_SN_SYNTH_TONE(   literally(0x1C610000u).rangeTo(literally(0x1C617F7Fu))),
        PART_16_SN_ACOUSTIC_TONE(literally(0x1C620000u).rangeTo(literally(0x1C627F7Fu))),
        PART_16_SN_DRUM_KIT(     literally(0x1C630000u).rangeTo(literally(0x1C637F7Fu))),
        PART_16_PCM_DRUM_KIT(    literally(0x1C700000u).rangeTo(literally(0x1C7F7F7Fu)));

        fun begin(): Integra7Address =
            Integra7Address(range.first.toUInt7UsingValue())

        fun end(): Integra7Address =
            Integra7Address(range.last.toUInt7UsingValue())
    }

    override fun bytes(): UByteArray =
        address.toUByteArrayLittleEndian()

    fun offsetBy(offset: Integra7Size, factor: Int = 1): Integra7Address =
        offsetBy(offset.size, factor)

    @Deprecated("Switch to UInt7")
    fun offsetBy(offset: Int, factor: Int = 1): Integra7Address =
        offsetBy(literally(offset.toUInt()), factor)

    @Deprecated("Switch to UInt7")
    fun offsetBy(msb: UByte = 0x00u, mmsb: UByte = 0x00u, mlsb: UByte = 0x00u, lsb: UByte, factor: Int = 1): Integra7Address =
        offsetBy(msb.toUByte7(), mmsb.toUByte7(), mlsb.toUByte7(), lsb.toUByte7())

    @Deprecated("Switch to UInt7")
    fun offsetBy(msb: UByte7 = UByte7.MIN_VALUE, mmsb: UByte7 = UByte7.MIN_VALUE, mlsb: UByte7 = UByte7.MIN_VALUE, lsb: UByte7 = UByte7.MIN_VALUE, factor: Int = 1): Integra7Address =
        offsetBy(UInt7(msb, mmsb, mlsb, lsb), factor)

    fun offsetBy(addendum: UInt7, factor: Int = 1): Integra7Address {
        return when {
            factor < 0 -> throw IllegalArgumentException()
            factor == 0 -> this
            factor == 1 -> Integra7Address((address + addendum).toUInt7UsingValue())
            else -> Integra7Address((address + addendum).toUInt7UsingValue()).offsetBy(addendum, factor - 1)
        }
    }

    /*
     * Returns the size of the address-range
     */
    operator fun minus(other: Integra7Address): Integra7Size =
        Integra7Size((address.toUInt() - other.address.toUInt()).toUInt7UsingValue())

    fun fullByteAddress(): Int =
        address.toInt()

    override fun compareTo(other: Integra7Address): Int =
        address.compareTo(other.address)

    fun rangeName(): String = Integra7Ranges.values()
        .firstOrNull { it.range.contains(address.toUInt()) }
        ?.let { it.toString() }
        ?: "Unknown range $this"

    override fun toString(): String =
        address.toString()
}

data class Integra7Size(val size: UInt7): UByteSerializable {
    @Deprecated("Switch to UInt7...")
    constructor(msb: UByte = 0x00u, mmsb: UByte = 0x00u, mlsb: UByte = 0x00u, lsb: UByte):
            this(UInt7(msb.toUByte7(), mmsb.toUByte7(), mlsb.toUByte7(), lsb.toUByte7()))

    @Deprecated("Switch to UInt7...")
    constructor(fullByteSize: UInt) : this(fullByteSize.toUInt7UsingValue())

    companion object {
        val ONE_BYTE = Integra7Size(0x01u.toUInt7UsingValue())
    }

    override fun bytes(): UByteArray =
        size.toUByteArrayLittleEndian()

    operator fun minus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() - other).toUInt())

    operator fun plus(other: Int): Integra7Size =
        Integra7Size((fullByteSize() + other).toUInt())

    fun fullByteSize(): Int =
        size.toInt()

    override fun toString(): String =
        fullByteSize().toString() + " Integra: " + size
}