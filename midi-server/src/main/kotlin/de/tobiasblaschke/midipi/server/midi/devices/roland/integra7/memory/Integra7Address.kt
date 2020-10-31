package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.utils.UByte7
import de.tobiasblaschke.midipi.server.utils.UInt7
import de.tobiasblaschke.midipi.server.utils.toUByte7
import de.tobiasblaschke.midipi.server.utils.toUInt7
import java.lang.IllegalArgumentException

data class Integra7Address(val address: UInt7): UByteSerializable, Comparable<Integra7Address> {
    @Deprecated("Switch to UInt7...")
    constructor(msb: UByte, mmsb: UByte, mlsb: UByte, lsb: UByte):
            this(UInt7(msb.toUByte7(), mmsb.toUByte7(), mlsb.toUByte7(), lsb.toUByte7()))

    @Deprecated("Switch to UInt7")
    constructor(address: Int): this(
        msb = (address / 0x1000000).toUByte(),
        mmsb = ((address / 0x10000).toUInt() and 0xFFu).toUByte(),
        mlsb = ((address / 0x100).toUInt() and 0xFFu).toUByte(),
        lsb = (address.toUInt() and 0xFFu).toUByte())

    @Deprecated("Switch to UInt7")
    val msb = address.msb.toUByte()
    @Deprecated("Switch to UInt7")
    val mmsb = address.mmsb.toUByte()
    @Deprecated("Switch to UInt7")
    val mlsb = address.mlsb.toUByte()
    @Deprecated("Switch to UInt7")
    val lsb = address.lsb.toUByte()

    companion object {
        @Deprecated("Switch to UInt7")
        fun fromFullByteAddress(address: Int): Integra7Address =
            Integra7Address(address.toUInt7())

        private fun literally(repr: UInt): UInt7 =
            UInt7(
                (repr / 0x1000000u).toUByte7(),
                (repr / 0x10000u).toUByte7(),
                (repr / 0x100u).toUByte7(),
                (repr.toUByte7()))
    }

    enum class Integra7Ranges(val range: UIntRange) {
        PART_1_PCM_SYNTH_TONE(literally(0x19000000u).rangeTo(literally(0x1900FFFFu))),
        PART_1_SN_SYNTH_TONE(literally(0x19010000u).rangeTo(literally(0x1901FFFFu))),
        PART_1_SN_ACOUSTIC_TONE(literally(0x19020000u).rangeTo(literally(0x1902FFFFu))),
        PART_1_SN_DRUM_KIT(literally(0x19030000u).rangeTo(literally(0x1903FFFFu))),
        PART_1_PCM_DRUM_KIT(literally(0x19100000u).rangeTo(literally(0x191FFFFFu))),

        PART_2_PCM_SYNTH_TONE(literally(0x19200000u).rangeTo(literally(0x1920FFFFu))),
        PART_2_SN_SYNTH_TONE(literally(0x19210000u).rangeTo(literally(0x1921FFFFu))),
        PART_2_SN_ACOUSTIC_TONE(literally(0x19220000u).rangeTo(literally(0x1922FFFFu))),
        PART_2_SN_DRUM_KIT(literally(0x19230000u).rangeTo(literally(0x1923FFFFu))),
        PART_2_PCM_DRUM_KIT(literally(0x19300000u).rangeTo(literally(0x193FFFFFu))),

        PART_3_PCM_SYNTH_TONE(literally(0x19400000u).rangeTo(literally(0x1920FFFFu))),
        PART_3_SN_SYNTH_TONE(literally(0x19410000u).rangeTo(literally(0x1921FFFFu))),
        PART_3_SN_ACOUSTIC_TONE(literally(0x19420000u).rangeTo(literally(0x1922FFFFu))),
        PART_3_SN_DRUM_KIT(literally(0x19430000u).rangeTo(literally(0x1923FFFFu))),
        PART_3_PCM_DRUM_KIT(literally(0x19500000u).rangeTo(literally(0x195FFFFFu))),

        PART_4_PCM_SYNTH_TONE(literally(0x19600000u).rangeTo(literally(0x1920FFFFu))),
        PART_4_SN_SYNTH_TONE(literally(0x19610000u).rangeTo(literally(0x1921FFFFu))),
        PART_4_SN_ACOUSTIC_TONE(literally(0x19620000u).rangeTo(literally(0x1922FFFFu))),
        PART_4_SN_DRUM_KIT(literally(0x19630000u).rangeTo(literally(0x1923FFFFu))),
        PART_4_PCM_DRUM_KIT(literally(0x19700000u).rangeTo(literally(0x193FFFFFu))),

        PART_5_PCM_SYNTH_TONE(literally(0x1A000000u).rangeTo(literally(0x1920FFFFu))),
        PART_5_SN_SYNTH_TONE(literally(0x1A010000u).rangeTo(literally(0x1921FFFFu))),
        PART_5_SN_ACOUSTIC_TONE(literally(0x1A020000u).rangeTo(literally(0x1922FFFFu))),
        PART_5_SN_DRUM_KIT(literally(0x1A030000u).rangeTo(literally(0x1923FFFFu))),
        PART_5_PCM_DRUM_KIT(literally(0x1A100000u).rangeTo(literally(0x193FFFFFu))),

        PART_6_PCM_SYNTH_TONE(literally(0x1A200000u).rangeTo(literally(0x1920FFFFu))),
        PART_6_SN_SYNTH_TONE(literally(0x1A210000u).rangeTo(literally(0x1921FFFFu))),
        PART_6_SN_ACOUSTIC_TONE(literally(0x1A220000u).rangeTo(literally(0x1922FFFFu))),
        PART_6_SN_DRUM_KIT(literally(0x1A230000u).rangeTo(literally(0x1923FFFFu))),
        PART_6_PCM_DRUM_KIT(literally(0x1A300000u).rangeTo(literally(0x193FFFFFu))),

        PART_7_PCM_SYNTH_TONE(literally(0x1A400000u).rangeTo(literally(0x1920FFFFu))),
        PART_7_SN_SYNTH_TONE(literally(0x1A410000u).rangeTo(literally(0x1921FFFFu))),
        PART_7_SN_ACOUSTIC_TONE(literally(0x1A420000u).rangeTo(literally(0x1922FFFFu))),
        PART_7_SN_DRUM_KIT(literally(0x1A430000u).rangeTo(literally(0x1923FFFFu))),
        PART_7_PCM_DRUM_KIT(literally(0x1A500000u).rangeTo(literally(0x193FFFFFu))),

        PART_8_PCM_SYNTH_TONE(literally(0x1A600000u).rangeTo(literally(0x1920FFFFu))),
        PART_8_SN_SYNTH_TONE(literally(0x1A610000u).rangeTo(literally(0x1921FFFFu))),
        PART_8_SN_ACOUSTIC_TONE(literally(0x1A620000u).rangeTo(literally(0x1922FFFFu))),
        PART_8_SN_DRUM_KIT(literally(0x1A630000u).rangeTo(literally(0x1923FFFFu))),
        PART_8_PCM_DRUM_KIT(literally(0x1A700000u).rangeTo(literally(0x193FFFFFu))),

        PART_9_PCM_SYNTH_TONE(literally(0x1B000000u).rangeTo(literally(0x1920FFFFu))),
        PART_9_SN_SYNTH_TONE(literally(0x1B010000u).rangeTo(literally(0x1921FFFFu))),
        PART_9_SN_ACOUSTIC_TONE(literally(0x1B020000u).rangeTo(literally(0x1922FFFFu))),
        PART_9_SN_DRUM_KIT(literally(0x1B030000u).rangeTo(literally(0x1923FFFFu))),
        PART_9_PCM_DRUM_KIT(literally(0x1B100000u).rangeTo(literally(0x193FFFFFu))),

        PART_10_PCM_SYNTH_TONE(literally(0x1B200000u).rangeTo(literally(0x1920FFFFu))),
        PART_10_SN_SYNTH_TONE(literally(0x1B210000u).rangeTo(literally(0x1921FFFFu))),
        PART_10_SN_ACOUSTIC_TONE(literally(0x1B220000u).rangeTo(literally(0x1922FFFFu))),
        PART_10_SN_DRUM_KIT(literally(0x1B230000u).rangeTo(literally(0x1923FFFFu))),
        PART_10_PCM_DRUM_KIT(literally(0x1B300000u).rangeTo(literally(0x193FFFFFu))),

        PART_11_PCM_SYNTH_TONE(literally(0x1B400000u).rangeTo(literally(0x1920FFFFu))),
        PART_11_SN_SYNTH_TONE(literally(0x1B410000u).rangeTo(literally(0x1921FFFFu))),
        PART_11_SN_ACOUSTIC_TONE(literally(0x1B420000u).rangeTo(literally(0x1922FFFFu))),
        PART_11_SN_DRUM_KIT(literally(0x1B430000u).rangeTo(literally(0x1923FFFFu))),
        PART_11_PCM_DRUM_KIT(literally(0x1B500000u).rangeTo(literally(0x193FFFFFu))),

        PART_12_PCM_SYNTH_TONE(literally(0x1B600000u).rangeTo(literally(0x1920FFFFu))),
        PART_12_SN_SYNTH_TONE(literally(0x1B610000u).rangeTo(literally(0x1921FFFFu))),
        PART_12_SN_ACOUSTIC_TONE(literally(0x1B620000u).rangeTo(literally(0x1922FFFFu))),
        PART_12_SN_DRUM_KIT(literally(0x1B630000u).rangeTo(literally(0x1923FFFFu))),
        PART_12_PCM_DRUM_KIT(literally(0x1B700000u).rangeTo(literally(0x193FFFFFu))),

        PART_13_PCM_SYNTH_TONE(literally(0x1C000000u).rangeTo(literally(0x1920FFFFu))),
        PART_13_SN_SYNTH_TONE(literally(0x1C010000u).rangeTo(literally(0x1921FFFFu))),
        PART_13_SN_ACOUSTIC_TONE(literally(0x1C020000u).rangeTo(literally(0x1922FFFFu))),
        PART_13_SN_DRUM_KIT(literally(0x1C030000u).rangeTo(literally(0x1923FFFFu))),
        PART_13_PCM_DRUM_KIT(literally(0x1C100000u).rangeTo(literally(0x193FFFFFu))),

        PART_14_PCM_SYNTH_TONE(literally(0x1C200000u).rangeTo(literally(0x1920FFFFu))),
        PART_14_SN_SYNTH_TONE(literally(0x1C210000u).rangeTo(literally(0x1921FFFFu))),
        PART_14_SN_ACOUSTIC_TONE(literally(0x1C220000u).rangeTo(literally(0x1922FFFFu))),
        PART_14_SN_DRUM_KIT(literally(0x1C230000u).rangeTo(literally(0x1923FFFFu))),
        PART_14_PCM_DRUM_KIT(literally(0x1C300000u).rangeTo(literally(0x193FFFFFu))),

        PART_15_PCM_SYNTH_TONE(literally(0x1C400000u).rangeTo(literally(0x1920FFFFu))),
        PART_15_SN_SYNTH_TONE(literally(0x1C410000u).rangeTo(literally(0x1921FFFFu))),
        PART_15_SN_ACOUSTIC_TONE(literally(0x1C420000u).rangeTo(literally(0x1922FFFFu))),
        PART_15_SN_DRUM_KIT(literally(0x1C430000u).rangeTo(literally(0x1923FFFFu))),
        PART_15_PCM_DRUM_KIT(literally(0x1C500000u).rangeTo(literally(0x193FFFFFu))),

        PART_16_PCM_SYNTH_TONE(literally(0x1C600000u).rangeTo(literally(0x1920FFFFu))),
        PART_16_SN_SYNTH_TONE(literally(0x1C610000u).rangeTo(literally(0x1921FFFFu))),
        PART_16_SN_ACOUSTIC_TONE(literally(0x1C620000u).rangeTo(literally(0x1922FFFFu))),
        PART_16_SN_DRUM_KIT(literally(0x1C630000u).rangeTo(literally(0x1923FFFFu))),
        PART_16_PCM_DRUM_KIT(literally(0x1C700000u).rangeTo(literally(0x193FFFFFu)))
    }

    override fun bytes(): UByteArray =
        address.toUByteArrayLittleEndian()

    fun offsetBy(offset: Integra7Size, factor: Int = 1): Integra7Address =
        offsetBy(offset.size, factor)

    @Deprecated("Switch to UInt7")
    fun offsetBy(offset: Int, factor: Int = 1): Integra7Address =
        offsetBy(
            msb = (offset / 0x1000000).toUByte(),
            mmsb = ((offset / 0x10000).toUInt() and 0xFFu).toUByte(),
            mlsb = ((offset / 0x100).toUInt() and 0xFFu).toUByte(),
            lsb = (offset.toUInt() and 0xFFu).toUByte(),
            factor
        )

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
            else -> Integra7Address(((address + addendum) * factor.toUInt()).toUInt7())
        }
    }

    /*
     * Returns the size of the address-range
     */
    operator fun minus(other: Integra7Address): Integra7Size =
        Integra7Size((address.toUInt() - other.address.toUInt()).toUInt7())

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
    constructor(fullByteSize: UInt) : this(fullByteSize.toUInt7())

    companion object {
        val ONE_BYTE = Integra7Size(0x01u)
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