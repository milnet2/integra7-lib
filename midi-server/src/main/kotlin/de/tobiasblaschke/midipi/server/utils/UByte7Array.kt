package de.tobiasblaschke.midipi.server.utils

public class UByte7Array constructor(@PublishedApi internal val storage: ByteArray) : Collection<UByte7> {
    public constructor(size: Int) : this(ByteArray(size))

    public operator fun get(index: Int): UByte7 = storage[index].toUByte7()
    public operator fun set(index: Int, value: UByte7) {
        storage[index] = value.toByte()
    }

    public override val size: Int get() = storage.size
    public override operator fun iterator(): UByte7Iterator = Iterator(storage)

    private class Iterator(private val array: ByteArray) : UByte7Iterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun nextUByte7() = if (index < array.size) array[index++].toUByte7() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: UByte7): Boolean {
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is UByte7) return false

        return storage.contains(element.toByte())
    }

    override fun containsAll(elements: Collection<UByte7>): Boolean {
        return (elements as Collection<*>).all { it is UByte7 && storage.contains(it.toByte()) }
    }

    override fun isEmpty(): Boolean = this.storage.size == 0
}

public abstract class UByte7Iterator : Iterator<UByte7> {
    override final fun next() = nextUByte7()

    public abstract fun nextUByte7(): UByte7
}

public inline fun UByte7Array(size: Int, init: (Int) -> UByte7): UByte7Array {
    return UByte7Array(ByteArray(size) { index -> init(index).toByte() })
}

