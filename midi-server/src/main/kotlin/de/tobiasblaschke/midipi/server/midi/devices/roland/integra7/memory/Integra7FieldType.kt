package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.Integra7MemoryIO
import de.tobiasblaschke.midipi.server.midi.toAsciiString
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalStateException
import java.lang.RuntimeException

abstract class Integra7FieldType<T>: Integra7MemoryIO<T>() {
    @ExperimentalUnsignedTypes
    data class AsciiStringField(override val deviceId: DeviceId, override val address: Integra7Address, val length: Int): Integra7FieldType<String>() {
        override val size = Integra7Size(length.toUInt7())

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): String {
            assert(startAddress >= address)

            try {
                return payload[IntRange(startAddress.fullByteAddress(), startAddress.fullByteAddress() + this.length)].toAsciiString().trim()
            } catch (e: NoSuchElementException) {
                throw FieldReadException(startAddress, size, payload, "", e)
            }
        }
    }

    /**
     * Read one bytes: 0bbbbbbb
     */
    @ExperimentalUnsignedTypes
    data class UnsignedValueField(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..127): Integra7FieldType<Int>() {
        override val size = Integra7Size.ONE_BYTE

        init {
            assert(range.first >= 0 && range.last <= 127) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val ret = readByte(startAddress, payload).toInt()
            return checkRange(ret, startAddress, payload, range)
        }
    }

    /**
     * Read two bytes: 0bbbbbbb, 0bbbbbbb and use the first as minimum
     */
    @ExperimentalUnsignedTypes
    data class UnsignedRangeFields(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..127): Integra7FieldType<IntRange>() {
        override val size = Integra7Size.TWO_BYTES

        init {
            assert(range.first >= 0 && range.last <= 127) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): IntRange {
            val min = readByte(startAddress, payload).toInt()
            val max = readByte(startAddress, payload).toInt()

            return IntRange(
                checkRange(min, startAddress, payload, range),
                checkRange(max, startAddress, payload, range))
        }
    }

    /**
     * Read one bytes: 0bbbbbbb and look it up in an enum
     */
    @ExperimentalUnsignedTypes
    data class EnumValueField<T: Enum<T>>(override val deviceId: DeviceId, override val address: Integra7Address, val getter: (Int) -> T): Integra7FieldType<T>() {
        @Deprecated(message = "Use other constructor")
        constructor(deviceId: DeviceId, address: Integra7Address, values: Array<T>)
            :this(deviceId, address, { elem -> values[elem] })
        override val size = Integra7Size.ONE_BYTE

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): T {
            val elem = readByte(startAddress, payload).toInt()
            return try {
                getter(elem)
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw NoSuchElementException("When reading address ${startAddress}: No element $elem")
            }
        }
    }

    /**
     * Read two bytes: 0000mmmm, 0000llll
     */
    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFF): Integra7FieldType<Int>() {
        override val size = Integra7Size.TWO_BYTES

        init {
            assert(range.first >= 0 && range.last <= 0xFF) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val msn = readByte(startAddress, payload)
            val lsn = readByte(startAddress.successor(), payload)
            val value = msn.toInt() * 0x10 + lsn.toInt()
            return checkRange(value, startAddress, payload, range)
        }
    }

    /**
     * Read two bytes: 0mmmmmmm, 0lllllll
     */
    @ExperimentalUnsignedTypes
    data class UnsignedLsbMsbBytes(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0x3FFF): Integra7FieldType<Int>() {
        override val size = Integra7Size.TWO_BYTES

        init {
            assert(range.first >= 0 && range.last < 0x4000) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val msb = readByte(startAddress, payload)
            val lsb = readByte(startAddress.successor(), payload)
            val value = msb.toInt() * 0x80 + lsb.toInt()
            return checkRange(value, startAddress, payload, range)
        }
    }

    /**
     * Read two bytes: 0000mmmm, 0000aaaa, 0000bbbb, 0000llll
     */
    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFFFF): Integra7FieldType<Int>() {
        override val size = Integra7Size.FOUR_BYTES

        init {
            assert(range.first >= 0 && range.last <= 0xFFFF) { "Impossible range $range for this datatype" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val msb = readByte(startAddress, payload).toInt()
            val mmsb = readByte(startAddress.successor(), payload).toInt()
            val mlsb = readByte(startAddress.successor().successor(), payload).toInt()
            val lsb = readByte(startAddress.successor().successor().successor(), payload).toInt()

            val ret = ((((msb * 0x10) + mmsb) * 0x10) + mlsb) * 0x10 + lsb
            return checkRange(ret, startAddress, payload, range)
        }
    }

    /**
     * Read one bytes: 0bbbbbbb
     */
    @ExperimentalUnsignedTypes
    data class SignedValueField(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = -63..63): Integra7FieldType<Int>() {
        override val size = Integra7Size.ONE_BYTE

        init {
            assert(range.first >= -64 && range.last <= 63) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val ret = readByte(startAddress, payload).toInt() - 64
            return checkRange(ret, startAddress, payload, range)
        }
    }

    /**
     * Read two bytes: 0000mmmm, 0000aaaa, 0000bbbb, 0000llll
     */
    data class SignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = IntRange(-0x8000, 0x7FFF)): Integra7FieldType<Int>() {
        override val size = Integra7Size.FOUR_BYTES

        init {
            assert(range.first >= -0x8000 && range.last <= 0x7FFF) { "Impossible range $range for this datatype" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val msb = readByte(startAddress, payload).toInt()
            val mmsb = readByte(startAddress.successor(), payload).toInt()
            val mlsb = readByte(startAddress.successor().successor(), payload).toInt()
            val lsb = readByte(startAddress.successor().successor().successor(), payload).toInt()

            val ret = ((((msb * 0x10) + mmsb) * 0x10) + mlsb) * 0x10 + lsb - 0x8000
            return checkRange(ret, startAddress, payload, range)
        }
    }

    /**
     * Read one byte: 0000000b
     */
    @ExperimentalUnsignedTypes
    data class BooleanValueField(override val deviceId: DeviceId, override val address: Integra7Address): Integra7FieldType<Boolean>() {
        override val size = Integra7Size.ONE_BYTE

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Boolean =
            when(readByte(startAddress, payload).toInt()) {
                0 -> false
                1 -> true
                else -> throw FieldReadException(startAddress, size, payload, "Will only encode 0,1 to boolean")
            }
    }

    // -------------------------------------------------------------

    protected fun <V> checkRange(value: V, startAddress: Integra7Address, payload: SparseUByteArray, range: IntRange): V {
        if (!range.contains(value)) {
            throw FieldReadException(startAddress, size, payload, "Value $value not in $range")
        } else {
            return value
        }
    }

    protected fun readByte(startAddress: Integra7Address, payload: SparseUByteArray): UByte7 {
        try {
            return payload[startAddress.fullByteAddress()].toUByte7()
        } catch (e: NoSuchElementException) {
            throw FieldReadException(startAddress, size, payload, "", e)
        }
    }

    class FieldReadException(val startAddress: Integra7Address, val size: Integra7Size, payload: SparseUByteArray, message: String, cause: Throwable? = null):
        RuntimeException(
            "When reading range $startAddress..${startAddress.offsetBy(size)}: $message from ${payload.hexDump()}", cause)

    protected fun SparseUByteArray.hexDump() =
        this.hexDump(
            addressTransform = { Integra7Address(it.toUInt7()).toString() },
            chunkSize = 0x10)
}