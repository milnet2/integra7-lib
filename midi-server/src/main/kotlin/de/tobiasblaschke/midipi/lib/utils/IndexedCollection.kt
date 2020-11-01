package de.tobiasblaschke.midipi.lib.utils

class IndexedCollection<T, C: Collection<out T>>(private val collection: C, private val getter: (C, Int) -> T?, private val offset: Int = 0, override val size: Int = collection.size): Collection<T> by collection {
    operator fun get(int: Int): T =
        getter(collection, int - offset) ?: throw NoSuchElementException("No item at index ${int - offset} in $collection")

    fun subRange(from: Int, to: Int): IndexedCollection<T, C> =
        IndexedCollection(collection, getter, from, from + to + 1)
}