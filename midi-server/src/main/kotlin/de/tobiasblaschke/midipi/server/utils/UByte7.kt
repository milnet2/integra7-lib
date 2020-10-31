/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package de.tobiasblaschke.midipi.server.utils

import java.lang.IllegalArgumentException
import kotlin.experimental.*

/**
 * A UByte only using the lower 7 bits
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public class UByte7 @PublishedApi internal constructor(@PublishedApi internal val data: Byte): Comparable<UByte7> {
    public constructor(msn: UNibble, lsn: UNibble): this((msn * 0x10u + lsn.toUByte()).toByte())

    companion object {
        public val MIN_VALUE: UByte7 = UByte7(0)
        public val MAX_VALUE: UByte7 = UByte7(0x7F)

        public const val SIZE_BYTES: Int = 1
        public const val SIZE_BITS: Int = 7
    }

    init {
        if(data >= 0x80) throw IllegalArgumentException("UByte7 can only hold values up to 0x7F")
    }

    val upperNibble: UNibble
        get() = (this / 0x10u).toUNibble()
    val lowerNibble: UNibble
        get() = this.toUNibble()

    @Suppress("OVERRIDE_BY_INLINE")
    public override inline operator fun compareTo(other: UByte7): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UByte): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UShort7): Int = this.toInt().compareTo(other.toInt())
    public inline operator fun compareTo(other: UInt): Int = this.toUInt().compareTo(other)
    public inline operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)

    public inline operator fun plus(other: UByte): UShort7 = UShort7(((data + other.toByte()) % 0x8000).toShort())
    public inline operator fun plus(other: UShort): UShort7 = UShort7(((data + other.toShort()) % 0x8000).toShort())
    public inline operator fun plus(other: UNibble): UShort7 = UShort7(((data + other.toByte()) % 0x8000).toShort())
    public inline operator fun plus(other: UByte7): UShort7 = UShort7(((data + other.toByte()) % 0x8000).toShort())
    public inline operator fun plus(other: UShort7): UShort7 = UShort7(((data + other.toShort()) % 0x8000).toShort())

    public inline operator fun times(other: UByte): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UShort): UInt = this.toUInt().times(other)
    public inline operator fun times(other: UInt): UInt = this.toUInt().times(other)
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)
    public inline operator fun times(other: UNibble): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UByte7): UInt = this.toUInt().times(other.toUInt())
    public inline operator fun times(other: UShort7): UInt = this.toUInt().times(other.toUInt())

    public inline operator fun div(other: UNibble): UByte7 = UByte7((data / other.data).toByte())
    public inline operator fun div(other: UByte7): UByte7 = UByte7((data / other.data).toByte())
    public inline operator fun div(other: UShort7): UByte7 = UByte7((data / other.data).toByte())
    public inline operator fun div(other: UInt): UByte7 = UByte7((data / other.toByte()).toByte())
    public inline operator fun div(other: ULong): UByte7 = UByte7((data / other.toByte()).toByte())

    public inline operator fun rem(other: UNibble): UByte7 = UByte7((data % other.data).toByte())
    public inline operator fun rem(other: UByte7): UByte7 = UByte7((data % other.data).toByte())
    public inline operator fun rem(other: UShort7): UByte7 = UByte7((data % other.data).toByte())
    public inline operator fun rem(other: UInt): UByte7 = UByte7((data % other.toByte()).toByte())
    public inline operator fun rem(other: ULong): UByte7 = UByte7((data % other.toByte()).toByte())

    public inline operator fun rangeTo(other: UByte7): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    public inline infix fun and(other: UByte7): UByte7 = UByte7(this.data and other.data)
    public inline infix fun or(other: UByte7): UByte7 = UByte7(this.data or other.data)
    public inline infix fun xor(other: UByte7): UByte7 = UByte7(this.data xor other.data)
    public inline fun inv(): UByte7 = UByte7(data.inv() and 0x7F)

    public inline infix fun and(other: UByte): UByte7 = UByte7(this.data and other.toByte())
    public inline infix fun or(other: UByte): UByte7 = UByte7(this.data or other.toByte())
    public inline infix fun xor(other: UByte): UByte7 = UByte7(this.data xor other.toByte())

    public inline fun toByte(): Byte = data
    public inline fun toShort(): Short = data.toShort() and 0x7F
    public inline fun toInt(): Int = data.toInt() and 0x7F
    public inline fun toLong(): Long = data.toLong() and 0x7F

    public inline fun toUByte(): UByte = data.toUByte()
    public inline fun toUShort(): UShort = data.toUShort()
    public inline fun toUInt(): UInt = data.toUInt()
    public inline fun toULong(): ULong = data.toULong()

    public inline fun toUNibble(): UNibble = UNibble(data and 0x0F)
    public inline fun toUByte7(): UByte7 = this
    public inline fun toUShort7(): UShort7 = UShort7(MIN_VALUE, this)
    public inline fun toUInt7(): UInt7 = UInt7(MIN_VALUE, MIN_VALUE, MIN_VALUE, this)
//    public inline fun toULong7(): ULong = ULong(data.toLong() and 0xFF)

    public inline fun toFloat(): Float = this.toInt().toFloat()
    public inline fun toDouble(): Double = this.toInt().toDouble()

    override fun equals(other: Any?): Boolean = when(other) {
        is UByte, is UShort, is UInt, is ULong ->
            toUInt() == other
        is UNibble -> data == other.data
        is UByte7 -> data == other.data
        is UShort7 -> data.toShort() == other.data
        else -> false
    }

    override fun hashCode(): Int =
        toUInt().hashCode()

    public override fun toString(): String =
        String.format("0x%02X", toInt())
}

public fun Byte.toUByte7(): UByte7 = UByte7(this and 0x7F)
public fun Short.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
public fun Int.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
public fun Long.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)

public fun UByte.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
public fun UShort.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
public fun UInt.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
public fun ULong.toUByte7(): UByte7 = UByte7(this.toByte() and 0x7F)
