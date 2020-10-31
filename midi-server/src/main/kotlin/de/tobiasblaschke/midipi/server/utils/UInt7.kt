package de.tobiasblaschke.midipi.server.utils

import java.lang.IllegalArgumentException

/**
 * A 28-bit value, that only uses 7 bits in each of its bytes
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public class UInt7 @PublishedApi internal constructor(@PublishedApi internal val data: Int) : Comparable<UInt7> {
    public constructor(msb: UByte7, mmsb: UByte7, mlsb: UByte7, lsb: UByte7):
            this((((msb * 0x80u + mmsb.toUInt()) * 0x80u + mlsb.toUInt()) * 0x80u + lsb.toUInt()).toInt())

    companion object {
        public val MIN_VALUE: UInt7 = UInt7(0x00000000)
        public val MAX_VALUE: UInt7 = UInt7(0x7FFFFFFF)
        public const val SIZE_BYTES: Int = 4
        public const val SIZE_BITS: Int = 28
    }

    init {
        if(data >= 0x80000000) throw IllegalArgumentException("UInt7 can only hold values up to 0x7FFFFFFF")
    }

    val msb: UByte7
        get() = (this / (0x80u * 0x80u * 0x80u)).toUByte7()
    val mmsb: UByte7
        get() = (this / (0x80u * 0x80u)).toUByte7()
    val mlsb: UByte7
        get() = (this / 0x80u).toUByte7()
    val lsb: UByte7
        get() = this.toUByte7()

    /**
     * A byte-array retaining the 7-bit representation
     */
    inline fun toUByteArrayLittleEndian() =
        ubyteArrayOf(msb.toUByte(), mmsb.toUByte(), mlsb.toUByte(), lsb.toUByte())

    public inline operator fun compareTo(other: UByte): Int = this.toInt().compareTo(other.toInt())
    @Suppress("OVERRIDE_BY_INLINE")
    public override inline operator fun compareTo(other: UInt7): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UInt): Int = this.toUInt().compareTo(other)
    public inline operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)

    public inline operator fun plus(other: UByte): UInt = this.toUInt().plus(other.toUInt())
    public inline operator fun plus(other: UShort): UInt = this.toUInt().plus(other.toUInt())
    public inline operator fun plus(other: UInt): UInt = this.toUInt().plus(other)
    public inline operator fun plus(other: ULong): ULong = this.toULong().plus(other)
    public inline operator fun plus(other: UNibble): UInt = this.toUInt().plus(other.toUInt())
    public inline operator fun plus(other: UByte7): UInt = this.toUInt().plus(other.toUInt())
    public inline operator fun plus(other: UInt7): UInt = this.toUInt().plus(other.toUInt())

    public inline operator fun times(other: UByte): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UShort): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UInt): UInt = this.toUInt().times(other)
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)
    public inline operator fun times(other: UNibble): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UByte7): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UInt7): UInt = this.toUInt().times(other.toUInt())

    public inline operator fun div(other: UNibble): UInt7 = UInt7(data / other.data)
    public inline operator fun div(other: UByte7): UInt7 = UInt7(data / other.data)
    public inline operator fun div(other: UInt7): UInt7 = UInt7(data / other.data)
    public inline operator fun div(other: UInt): UInt7 = UInt7(data / other.toInt())
    public inline operator fun div(other: ULong): UInt7 = UInt7(data / other.toInt())

    public inline operator fun rem(other: UNibble): UInt7 = UInt7(data % other.data)
    public inline operator fun rem(other: UByte7): UInt7 = UInt7(data % other.data)
    public inline operator fun rem(other: UInt7): UInt7 = UInt7(data % other.data)
    public inline operator fun rem(other: UInt): UInt7 = UInt7(data % other.toInt())
    public inline operator fun rem(other: ULong): UInt7 = UInt7(data % other.toInt())

    public inline operator fun rangeTo(other: UInt7): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    public inline infix fun and(other: UInt7): UInt7 = UInt7(this.data and other.data)
    public inline infix fun or(other: UInt7): UInt7 = UInt7(this.data or other.data)
    public inline infix fun xor(other: UInt7): UInt7 = UInt7(this.data xor other.data)
    public inline fun inv(): UInt7 = UInt7(data.inv() and 0x7FFFFFFF)

    public inline fun toByte(): Byte = data.toByte()
    public inline fun toShort(): Short = data.toShort()
    public inline fun toInt(): Int = data
    public inline fun toLong(): Long = data.toLong()

    public inline fun toUByte(): UByte = data.toUByte()
    public inline fun toUShort(): UShort = data.toUShort()
    public inline fun toUInt(): UInt = data.toUInt()
    public inline fun toULong(): ULong = data.toULong()

    public inline fun toUNibble(): UNibble = data.toUNibble()
    public inline fun toUByte7(): UByte7 = data.toUByte7()
    public inline fun toUShort7(): UShort7 = data.toUShort7()
    public inline fun toUInt7(): UInt7 = this
//    public inline fun toULong7(): ULong = data.toULong7()

    public inline fun toFloat(): Float = this.toInt().toFloat()
    public inline fun toDouble(): Double = this.toInt().toDouble()

    override fun equals(other: Any?): Boolean = when(other) {
        is UByte, is UShort, is UInt, is ULong ->
            toUInt() == other
        is UNibble -> data == other.data.toInt()
        is UByte7 -> data == other.data.toInt()
        is UShort7 -> data == other.data.toInt()
        is UInt7 -> data == other.data
        else -> false
    }

    override fun hashCode(): Int =
        toUInt().hashCode()

    public override fun toString(): String =
        String.format("0x%02X%02X%02X%02X (%d)", msb.toInt(), mmsb.toInt(), mlsb.toInt(), lsb.toInt(), data.toUShort().toInt())
}

public inline fun Byte.toUInt7(): UInt7 = this.toUInt().toUInt7()
public inline fun Short.toUInt7(): UInt7 = this.toUInt().toUInt7()
public inline fun Int.toUInt7(): UInt7 = this.toUInt().toUInt7()
public inline fun Long.toUInt7(): UInt7 = this.toUInt().toUInt7()

public inline fun UByte.toUInt7(): UInt7 = this.toUInt().toUInt7()
public inline fun UShort.toUInt7(): UInt7 = this.toUInt().toUInt7()
public inline fun UInt.toUInt7(): UInt7 = UInt7((this % 0x80000000u).toInt())
public inline fun ULong.toUInt7(): UInt7 = this.toUInt().toUInt7()