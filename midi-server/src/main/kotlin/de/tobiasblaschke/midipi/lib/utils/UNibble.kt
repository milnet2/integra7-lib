package de.tobiasblaschke.midipi.lib.utils

import java.lang.IllegalArgumentException
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@OptIn(ExperimentalUnsignedTypes::class)
public class UNibble @PublishedApi internal constructor(@PublishedApi internal val data: Byte): Comparable<UNibble> {
    companion object {
        public val MIN_VALUE: UNibble = UNibble(0)
        public val MAX_VALUE: UNibble = UNibble(0x0F)

        public const val SIZE_BYTES: Int = 1
        public const val SIZE_BITS: Int = 4
    }

    init {
        if(data >= 0x80) throw IllegalArgumentException("UNibble can only hold values up to 0x7F")
    }

    @Suppress("OVERRIDE_BY_INLINE")
    public override inline operator fun compareTo(other: UNibble): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UByte): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UShort): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UInt): Int = this.toUInt().compareTo(other)
    public inline operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)
    public inline operator fun compareTo(other: UByte7): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UShort7): Int = this.toInt().compareTo(other.toInt())

    public inline operator fun plus(other: UNibble): UByte7 = UByte7(((data + other.toByte()) % 0x80).toByte())
    public inline operator fun plus(other: UByte): UShort7 = UShort7(((data + other.toByte()) % 0x4000).toShort())
    public inline operator fun plus(other: UShort): UShort7 = UShort7(((data + other.toShort()) % 0x4000).toShort())

    public inline operator fun times(other: UNibble): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UByte): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UShort): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UInt): UInt = this.toUInt().times(other)
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)
    public inline operator fun times(other: UByte7): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UShort7): UInt = this.toUInt().times(other.toUInt())

    public inline operator fun div(other: UNibble): UNibble = UNibble((data / other.data).toByte())
    public inline operator fun div(other: UByte7): UNibble = UNibble((data / other.data).toByte())
    public inline operator fun div(other: UShort7): UNibble = UNibble((data / other.data).toByte())
    public inline operator fun div(other: UInt): UNibble = UNibble((data / other.toByte()).toByte())
    public inline operator fun div(other: ULong): UNibble = UNibble((data / other.toByte()).toByte())

    public inline operator fun rem(other: UNibble): UNibble = UNibble((data % other.data).toByte())
    public inline operator fun rem(other: UByte7): UNibble = UNibble((data % other.data).toByte())
    public inline operator fun rem(other: UShort7): UNibble = UNibble((data % other.data).toByte())
    public inline operator fun rem(other: UInt): UNibble = UNibble((data % other.toByte()).toByte())
    public inline operator fun rem(other: ULong): UNibble = UNibble((data % other.toByte()).toByte())

    public inline operator fun rangeTo(other: UNibble): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    public inline infix fun and(other: UNibble): UNibble = UNibble(this.data and other.data)
    public inline infix fun or(other: UNibble): UNibble = UNibble(this.data or other.data)
    public inline infix fun xor(other: UNibble): UNibble = UNibble(this.data xor other.data)
    public inline fun inv(): UNibble = UNibble(data.inv() and 0x0F)

    public inline infix fun and(other: UByte): UNibble = UNibble(this.data and other.toByte())
    public inline infix fun or(other: UByte): UNibble = UNibble(this.data or other.toByte())
    public inline infix fun xor(other: UByte): UNibble = UNibble(this.data xor other.toByte())

    public inline fun toByte(): Byte = data
    public inline fun toShort(): Short = data.toShort() and 0x7F
    public inline fun toInt(): Int = data.toInt() and 0x7F
    public inline fun toLong(): Long = data.toLong() and 0x7F

    public inline fun toUByte(): UByte = data.toUByte()
    public inline fun toUShort(): UShort = data.toUShort()
    public inline fun toUInt(): UInt = data.toUInt()
    public inline fun toULong(): ULong = data.toULong()

    public inline fun toUNibble(): UNibble = this
    public inline fun toUByte7(): UByte7 = UByte7(this.data)
    public inline fun toUShort7(): UShort7 = UShort7(UByte7.MIN_VALUE, this.toUByte7())
    //    public inline fun toUInt7(): UInt = UInt(data.toInt() and 0xFF)
//    public inline fun toULong7(): ULong = ULong(data.toLong() and 0xFF)

    public inline fun toFloat(): Float = this.toInt().toFloat()
    public inline fun toDouble(): Double = this.toInt().toDouble()

    override fun equals(other: Any?): Boolean = when(other) {
        is UByte, is UShort, is UInt, is ULong ->
            toUInt() == other
        is UNibble -> data == other.data
        is UByte7 -> data == other.data
        is UShort7 -> data == (other.data.toByte() % 0x80).toByte()
        else -> false
    }

    override fun hashCode(): Int =
        toUInt().hashCode()

    public override fun toString(): String =
        String.format("0x%X", toInt())
}

public fun Byte.toUNibble(): UNibble = UNibble(this and 0x0F)
public fun Short.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
public fun Int.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
public fun Long.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)

public fun UByte.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
public fun UInt.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
public fun UShort.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
public fun ULong.toUNibble(): UNibble = UNibble(this.toByte() and 0x0F)
