package de.tobiasblaschke.midipi.server.midi.utils

import java.util.*
import kotlin.Comparator
import kotlin.NoSuchElementException

class SparseUByteArray(): Collection<UByte> {
    private val values = TreeMap<IntRange, UByteArray>(Comparator.comparing(IntRange::first))
    override val size: Int
        get() = if (values.isEmpty()) 0 else values.lastEntry().key.last + 1

    constructor(startAddress: Int = 0, initial: UByteArray): this() {
        this.values[IntRange(startAddress, startAddress + initial.size - 1)] = initial // TODO: Clone initial?
    }

    constructor(startAddress: Int = 0, initial: Collection<UByte>): this() {
        this.values[IntRange(startAddress, startAddress + initial.size - 1)] = initial.toUByteArray()
    }

    operator fun contains(key: Int): Boolean =
        findInternal(key) != null

    operator fun get(key: Int): UByte {
        val entry = findInternal(key)
        return if (entry == null) {
            throw NoSuchElementException("When accessing $key")
        } else {
            entry.value[key - entry.key.first]
        }
    }

    operator fun get(rng: IntRange): UByteArray {
        val relevantExistingEntries = this.values.entries
            .filter { it.key.first >= rng.first && it.key.last <= rng.last }
        return if (relevantExistingEntries.isEmpty()) {
            throw NoSuchElementException("When accessing $rng")
        } else if (relevantExistingEntries.size == 1) {
            val entry = relevantExistingEntries[0]
            return entry.value.copyOfRange(rng.first - entry.key.first, rng.last - entry.key.last)
        } else {
            TODO()
        }
    }

    fun put(key: Int, value: UByte) =
        putAll(key, ubyteArrayOf(value))


    fun putAll(addendum: SparseUByteArray) {
        addendum.values.entries
            .forEach { this.putAll(it.key.first, it.value) }
    }

    fun putAll(startAddress: Int, values: UByteArray) {
        val endAddress = startAddress + values.size - 1
        val relevantExistingEntries = this.values.entries
            .filter { it.key.first >= startAddress && it.key.last <= endAddress }

        if (relevantExistingEntries.isEmpty()) {
            this.values[IntRange(startAddress, endAddress)] = values
        } else {
            TODO()
        }
    }

    fun append(value: UByte) =
        put(this.size, value)

    fun appendAll(values: UByteArray) =
        putAll(this.size, values)

    private fun findInternal(key: Int): MutableMap.MutableEntry<IntRange, UByteArray>? {
        // TODO: Use binary search
        return values.entries
            .find { it.key.contains(key) }
    }

    fun toUByteArray(): UByteArray {
        val ret = UByteArray(size, { 0x00u.toUByte() })
        this.values.entries
            .forEach { it.value.copyInto(ret, startIndex = it.key.first) }
        return ret
    }

    override fun contains(element: UByte): Boolean =
        any { it == element }

    override fun containsAll(elements: Collection<UByte>): Boolean =
        elements.all { this.contains(it) }

    override fun isEmpty(): Boolean =
        values.isEmpty()

    override fun iterator(): Iterator<UByte> {
        return object: Iterator<UByte> {
            private val rangeIterator = values.iterator()
            private var currentRange: Iterator<UByte>? = null

            override fun hasNext(): Boolean =
                rangeIterator.hasNext() || (currentRange?.hasNext() ?: false)

            override fun next(): UByte =
                if (currentRange != null && currentRange!!.hasNext()) {
                    currentRange!!.next()
                } else if (rangeIterator.hasNext()) {
                    this.currentRange = rangeIterator.next().value.iterator()
                    next()
                } else {
                    throw NoSuchElementException("Did outrun elements")
                }
            }
    }

    fun all(predicate: (UByte) -> Boolean): Boolean =
        this.values.values.all { it.all(predicate) }

    fun any(predicate: (UByte) -> Boolean): Boolean =
        this.values.values.any { it.any(predicate) }

    override fun toString(): String =
        this.values.toString()
}