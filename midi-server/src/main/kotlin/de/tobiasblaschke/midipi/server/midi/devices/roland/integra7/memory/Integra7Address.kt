package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import java.lang.IllegalArgumentException

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

data class Integra7Size(val msb: UByte = 0x00u, val mmsb: UByte = 0x00u, val mlsb: UByte = 0x00u, val lsb: UByte = 0x00u):
    UByteSerializable {
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