package de.tobiasblaschke.midipi.server.utils

import java.lang.IllegalArgumentException
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * A short value, that only uses 7 bits in each of its bytes
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@OptIn(ExperimentalUnsignedTypes::class)
public class UShort7 @PublishedApi internal constructor(@PublishedApi internal val data: Short) : Comparable<UShort7> {
    public constructor(msb: UByte7, lsb: UByte7): this((msb * 0x80u + lsb.toUByte()).toShort())

    companion object {
        public val MIN_VALUE: UShort7 = UShort7(0x0000.toShort())
        public val MAX_VALUE: UShort7 = UShort7(0x7FFF.toShort())
        public const val SIZE_BYTES: Int = 2
        public const val SIZE_BITS: Int = 14
    }

    init {
        if(data >= 0x8000) throw IllegalArgumentException("UShort7 can only hold values up to 0x7FFF")
    }

    val msb: UByte7
        get() = (this / 0x80u).toUByte7()
    val lsb: UByte7
        get() = this.toUByte7()

    /**
     * A byte-array retaining the 7-bit representation
     */
    inline fun toUByteArrayLittleEndian() =
        ubyteArrayOf(msb.toUByte(), lsb.toUByte())

    public inline operator fun compareTo(other: UByte): Int = this.toInt().compareTo(other.toInt())
    @Suppress("OVERRIDE_BY_INLINE")
    public override inline operator fun compareTo(other: UShort7): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UInt): Int = this.toUInt().compareTo(other)
    public inline operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)

    public inline operator fun plus(other: UByte): UInt7 = UInt7(((data + other.toByte()) % 0x80000000).toInt())
    public inline operator fun plus(other: UShort): UInt7 = UInt7(((data + other.toInt()) % 0x80000000).toInt())
    public inline operator fun plus(other: UInt): UInt7 = UInt7(((data + other.toInt()) % 0x80000000).toInt())
    public inline operator fun plus(other: ULong): ULong = ((data + other.toByte()) % 0x8000).toULong()
    public inline operator fun plus(other: UNibble): UInt7 = UInt7(((data + other.toInt()) % 0x80000000).toInt())
    public inline operator fun plus(other: UByte7): UInt7 = UInt7(((data + other.toInt()) % 0x80000000).toInt())
    public inline operator fun plus(other: UShort7): UInt7 = UInt7(((data + other.toInt()) % 0x80000000).toInt())

    public inline operator fun times(other: UByte): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UShort): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UInt): UInt = this.toUInt().times(other)
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)
    public inline operator fun times(other: UNibble): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UByte7): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UShort7): UInt = this.toUInt().times(other.toUInt())

    public inline operator fun div(other: UNibble): UShort7 = UShort7((data / other.data).toShort())
    public inline operator fun div(other: UByte7): UShort7 = UShort7((data / other.data).toShort())
    public inline operator fun div(other: UShort7): UShort7 = UShort7((data / other.data).toShort())
    public inline operator fun div(other: UInt): UShort7 = UShort7((data / other.toShort()).toShort())
    public inline operator fun div(other: ULong): UShort7 = UShort7((data / other.toShort()).toShort())

    public inline operator fun rem(other: UNibble): UShort7 = UShort7((data % other.data).toShort())
    public inline operator fun rem(other: UByte7): UShort7 = UShort7((data % other.data).toShort())
    public inline operator fun rem(other: UShort7): UShort7 = UShort7((data % other.data).toShort())
    public inline operator fun rem(other: UInt): UShort7 = UShort7((data % other.toShort()).toShort())
    public inline operator fun rem(other: ULong): UShort7 = UShort7((data % other.toShort()).toShort())

    public inline operator fun rangeTo(other: UShort7): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    public inline infix fun and(other: UShort7): UShort7 = UShort7(this.data and other.data)
    public inline infix fun or(other: UShort7): UShort7 = UShort7(this.data or other.data)
    public inline infix fun xor(other: UShort7): UShort7 = UShort7(this.data xor other.data)
    public inline fun inv(): UShort7 = UShort7(data.inv() and 0x7FFF)

    public inline fun toByte(): Byte = data.toByte()
    public inline fun toShort(): Short = data
    public inline fun toInt(): Int = data.toInt()
    public inline fun toLong(): Long = data.toLong()

    public inline fun toUByte(): UByte = data.toUByte()
    public inline fun toUShort(): UShort = data.toUShort()
    public inline fun toUInt(): UInt = data.toUInt()
    public inline fun toULong(): ULong = data.toULong()

    public inline fun toUNibble(): UNibble = data.toUNibble()
    public inline fun toUByte7(): UByte7 = data.toUByte7()
    public inline fun toUShort7(): UShort7 = this
    public inline fun toUInt7(): UInt7 = data.toUInt7()
//    public inline fun toULong7(): ULong = data.toULong7()

    public inline fun toFloat(): Float = this.toInt().toFloat()
    public inline fun toDouble(): Double = this.toInt().toDouble()

    override fun equals(other: Any?): Boolean = when(other) {
        is UByte, is UShort, is UInt, is ULong ->
            toUInt() == other
        is UNibble -> data == other.data.toShort()
        is UByte7 -> data == other.data.toShort()
        is UShort7 -> data == other.data
        else -> false
    }

    override fun hashCode(): Int =
        toUInt().hashCode()

    public override fun toString(): String =
        String.format("0x%02X%02X (%d)", msb.toInt(), lsb.toInt(), data.toUShort().toInt())
}

public inline fun Byte.toUShort7(): UShort7 = this.toUInt().toUShort7()
public inline fun Short.toUShort7(): UShort7 = this.toUInt().toUShort7()
public inline fun Int.toUShort7(): UShort7 = this.toUInt().toUShort7()
public inline fun Long.toUShort7(): UShort7 = this.toUInt().toUShort7()

public inline fun UByte.toUShort7(): UShort7 = this.toUInt().toUShort7()
public inline fun UShort.toUShort7(): UShort7 = this.toUInt().toUShort7()
public inline fun UInt.toUShort7(): UShort7 = UShort7((this % 0x8000u).toShort())
public inline fun ULong.toUShort7(): UShort7 = this.toUInt().toUShort7()