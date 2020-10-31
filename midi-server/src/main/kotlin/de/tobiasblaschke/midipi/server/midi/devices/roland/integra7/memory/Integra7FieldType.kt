package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.Integra7MemoryIO
import de.tobiasblaschke.midipi.server.midi.toAsciiString
import de.tobiasblaschke.midipi.server.utils.SparseUByteArray
import de.tobiasblaschke.midipi.server.utils.toUInt7
import de.tobiasblaschke.midipi.server.utils.toUInt7UsingValue
import java.lang.IllegalStateException

abstract class Integra7FieldType<T>: Integra7MemoryIO<T>() {
    @ExperimentalUnsignedTypes
    data class AsciiStringField(override val deviceId: DeviceId, override val address: Integra7Address, val length: Int): Integra7FieldType<String>() {
        override val size = Integra7Size(length.toUInt())

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): String {
            assert(startAddress >= address)

            try {
                return payload[IntRange(startAddress.fullByteAddress(), startAddress.fullByteAddress() + this.length)].toAsciiString().trim()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException(
                    "When reading range $startAddress..${startAddress.offsetBy(this.size)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedValueField(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..127): Integra7FieldType<Int>() {
        override val size = Integra7Size.ONE_BYTE

        init {
            assert(range.first >= 0 && range.last <= 127) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val ret = payload[startAddress.fullByteAddress()].toInt()
                return checkRange(ret, startAddress, payload, range)
            } catch (e: NoSuchElementException) {
                throw IllegalStateException(
                    "When reading range $startAddress..${startAddress.offsetBy(this.size)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedRangeFields(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..127): Integra7FieldType<IntRange>() {
        override val size = Integra7Size(0x02u.toUInt7UsingValue())

        init {
            assert(range.first >= 0 && range.last <= 127) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): IntRange {
            try {
                val min = payload[startAddress.fullByteAddress()].toInt()
                val max = payload[startAddress.fullByteAddress() + 1].toInt()

                return IntRange(
                    checkRange(min, startAddress, payload, range),
                    checkRange(max, startAddress, payload, range))
            } catch (e: NoSuchElementException) {
                throw IllegalStateException(
                    "When reading range $startAddress..${startAddress.offsetBy(this.size)} (${startAddress.fullByteAddress()}, ${startAddress.fullByteAddress() + length}) from ${
                        payload.hexDump(
                            { Integra7Address(it.toUInt7()).toString() },
                            0x10
                        )
                    }", e
                )
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class EnumValueField<T: Enum<T>>(override val deviceId: DeviceId, override val address: Integra7Address, val getter: (Int) -> T): Integra7FieldType<T>() {
        @Deprecated(message = "Use other constructor")
        constructor(deviceId: DeviceId, address: Integra7Address, values: Array<T>)
            :this(deviceId, address, { elem -> values[elem] })
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): T {
            val elem = delegate.interpret(startAddress, length, payload)
            return try {
                getter(elem)
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw NoSuchElementException("When reading address ${startAddress}: No element $elem")
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFF): Integra7FieldType<Int>() {
        override val size = Integra7Size(0x02u.toUInt7UsingValue())

        init {
            assert(range.first >= 0 && range.last <= 0xFF) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val ret = payload[startAddress.fullByteAddress()].toInt() * 0x10 +
                        payload[startAddress.fullByteAddress() + 1].toInt()
                return checkRange(ret, startAddress, payload, range)
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address ${startAddress} (sa=$startAddress, len=$length)", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedLsbMsbBytes(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0x3FFF): Integra7FieldType<Int>() {
        override val size = Integra7Size(0x02u.toUInt7UsingValue())

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val value = payload[startAddress.fullByteAddress()].toInt() +
                        payload[startAddress.fullByteAddress() + 1].toInt() * 0x80
                return checkRange(value, startAddress, payload, range)
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("When reading address ${startAddress} (sa=$startAddress, len=$length)", e)
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class UnsignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = 0..0xFFFF): Integra7FieldType<Int>() {
        override val size = Integra7Size(0x04u.toUInt7UsingValue())

        init {
            assert(range.first >= 0 && range.last <= 0xFFFF) { "Impossible range $range for this datatype" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            try {
                val msb = payload[startAddress.fullByteAddress()].toInt()
                val mmsb = payload[startAddress.fullByteAddress() + 1].toInt()
                val mlsb = payload[startAddress.fullByteAddress() + 2].toInt()
                val lsb = payload[startAddress.fullByteAddress() + 3].toInt()

                val ret = ((((msb * 0x10) + mmsb) * 0x10) + mlsb) * 0x10 + lsb
                return checkRange(ret, startAddress, payload, range)
            } catch (e: NoSuchElementException) {
                throw IllegalStateException(
                    "When reading address $startAddress, len=$size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    @ExperimentalUnsignedTypes
    data class SignedValueField(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = -63..63): Integra7FieldType<Int>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        init {
            assert(range.first >= -64 && range.last <= 63) { "Impossible range $range" }
        }

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val ret = delegate.interpret(startAddress, length, payload) - 64
            return checkRange(ret, startAddress, payload, range)
        }
    }

    data class SignedMsbLsbFourNibbles(override val deviceId: DeviceId, override val address: Integra7Address, val range: IntRange = IntRange(-0x8000, 0x7FFF)): Integra7FieldType<Int>() {
        private val delegate = UnsignedMsbLsbFourNibbles(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Int {
            val ret = delegate.interpret(startAddress, length, payload) - 0x8000
            return checkRange(ret, startAddress, payload, range)
        }
    }

    @ExperimentalUnsignedTypes
    data class BooleanValueField(override val deviceId: DeviceId, override val address: Integra7Address): Integra7FieldType<Boolean>() {
        private val delegate = UnsignedValueField(deviceId, address)
        override val size = delegate.size

        override fun interpret(startAddress: Integra7Address, length: Int, payload: SparseUByteArray): Boolean =
            delegate.interpret(startAddress, length, payload) > 0
    }

    protected fun <V> checkRange(value: V, startAddress: Integra7Address, payload: SparseUByteArray, range: IntRange): V {
        if (!range.contains(value)) {
            throw IllegalStateException(
                "Value $value not in $range when reading address $startAddress, len=$size from ${
                    payload.hexDump({
                        Integra7Address(it.toUInt7()).toString()
                    }, 0x10)
                }"
            )
        } else {
            return value
        }
    }
}