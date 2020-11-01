package de.tobiasblaschke.midipi.lib.midi.bearable

import java.io.Closeable

interface UByteSerializable {
    fun bytes(): UByteArray
}

fun interface MBClosable: Closeable {
    override fun close()
}

interface MBConnection: AutoCloseable {
    fun open() // TODO: Get rid of open here - it's a detail of the Java-implementation
}

interface MBConnectionReadable<T: UByteSerializable>: MBConnection {
    fun subscribe(listener: (T) -> Unit): MBClosable
}
interface MBConnectionWritable: MBConnection {
    fun send(message: UByteSerializable, timestamp: Long = -1)
}
interface MBConnectionReadWrite<T: UByteSerializable>: MBConnectionReadable<T>, MBConnectionWritable

abstract class AbstractMBReadConnection: MBConnectionReadable<UByteSerializable> {
    private val listeners = mutableListOf<(UByteSerializable) -> Unit>()

    override fun subscribe(listener: (UByteSerializable) -> Unit): MBClosable {
        listeners.add(listener)
        return MBClosable { listeners.remove { it == listener } }
    }

    protected fun emit(message: UByteSerializable) {
        listeners.forEach { it(message) }
    }
}

