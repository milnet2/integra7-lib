package de.tobiasblaschke.midipi.server.midi.bearable.javamidi

import de.tobiasblaschke.midipi.server.midi.bearable.*
import de.tobiasblaschke.midipi.server.midi.toHexString
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.midi.*

sealed class MBJavaMidiEndpoint<T: MBJavaMidiEndpoint.MBJavaMidiConnection>(): MBEndpoint<T> {
    protected abstract val connectionFactory: () -> T

    class MBJavaMidiReadableEndpoint(val device: MidiDevice): MBJavaMidiEndpoint<MBJavaMidiConnection.MBJavaMidiReadConnection>() {
        override val connectionFactory = { MBJavaMidiConnection.MBJavaMidiReadConnection(device, false) }
        override val capabilities: Set<MBEndpointCapabilities> = EnumSet.of(MBEndpointCapabilities.READ)
        override val name: String
            get() = device.deviceInfo.name
    }

    class MBJavaMidiWritableEndpoint(val device: MidiDevice): MBJavaMidiEndpoint<MBJavaMidiConnection.MBJavaMidiWriteConnection>() {
        override val connectionFactory = { MBJavaMidiConnection.MBJavaMidiWriteConnection(device, false) }
        override val capabilities: Set<MBEndpointCapabilities> = EnumSet.of(MBEndpointCapabilities.WRITE)
        override val name: String
            get() = device.deviceInfo.name
    }

    class MBJavaMidiReadWriteEndpoint(private val readable: MidiDevice, private val writable: MidiDevice): MBJavaMidiEndpoint<MBJavaMidiConnection.MBJavaMidiReadWriteConnection>() {
        override val connectionFactory = {
            MBJavaMidiConnection.MBJavaMidiReadWriteConnection(
                readable,
                writable,
                false
            )
        }
        override val capabilities: Set<MBEndpointCapabilities> = EnumSet.of(MBEndpointCapabilities.READ, MBEndpointCapabilities.WRITE)
        override val name: String
            get() {
                val rName = readable.deviceInfo.name
                val wName = writable.deviceInfo.name
                return if (rName == wName) {
                    rName
                } else {
                    "$rName / $wName"
                }
            }
    }

    private var connection: T? = null
    override fun <R> withConnection(action: (T) -> R) {
        if (connection == null) {
            connection = connectionFactory()
        }

        connection!!.use { connection ->
            connection.open()
            action(connection)
        }
    }

    override fun toString(): String =
        "MBJavaMidiEndpoint (capabilities=$capabilities, device=$name)"

    sealed class MBJavaMidiConnection: AutoCloseable, MBConnection {
        class MBJavaMidiReadWriteConnection(readable: MidiDevice, writable: MidiDevice, isAutoClose: Boolean) :
            MBJavaMidiConnection(), MBConnectionReadWrite<UByteSerializable> {
            private val readConnection = MBJavaMidiReadConnection(readable, isAutoClose)
            private val writeConnection = MBJavaMidiWriteConnection(writable, isAutoClose)

            override fun send(message: UByteSerializable, timestamp: Long) {
                writeConnection.send(message, timestamp)
            }

            override fun subscribe(listener: (UByteSerializable) -> Unit): MBClosable =
                readConnection.subscribe(listener)

            override fun open() {
                readConnection.open()
                writeConnection.open()
            }

            override fun close() {
                readConnection.close()
                writeConnection.close()
            }
        }

        class MBJavaMidiWriteConnection(private val writable: MidiDevice, isAutoClose: Boolean = false) :
            MBJavaMidiConnection(), MBConnectionWritable, AutoCloseable {
            private val refCount = AtomicInteger(0)
            private var shouldClose = isAutoClose
            private var receiver: Receiver? = null

            override fun send(message: UByteSerializable, timestamp: Long) {
                when (message) {
                    is MidiMessageWrapper -> send(message, timestamp)
                    is MidiMessage -> send(message as MidiMessage, timestamp)
                    else -> send(message.toMidiMessage(), timestamp)
                }
            }

            fun send(message: MidiMessageWrapper, timestamp: Long = -1) {
                send(message.unwrap(), timestamp)
            }

            fun send(message: MidiMessage, timestamp: Long = -1) {
                assert(writable.isOpen)
                assert(receiver != null)
                // TODO: Open and close the receiver?!
                println("Sending MIDI: $message...")
                receiver!!.send(message, timestamp)
            }

            fun UByteSerializable.toMidiMessage(): MidiMessage {
                assert(this.bytes().isNotEmpty())
                return object : MidiMessage(this.bytes().toByteArray()) {
                    override fun clone(): Any =
                        this@toMidiMessage.toMidiMessage()
                }
            }

            override fun open() {
                if (writable.isOpen) {
                    // Not opened by us, so keep it open later
                    shouldClose = false
                } else {
                    try {
                        writable.open()
                    } catch (e: SecurityException) {
                        throw MBException("Unable to open device $writable", e)
                    } catch (e: MidiUnavailableException) {
                        throw MBException("Resource restriction when opening device $writable", e)
                    }
                }
                refCount.incrementAndGet()
                if (receiver == null) {
                    receiver = writable.receiver
                }
            }

            override fun close() {
                val current = refCount.decrementAndGet()
                if (current == 0 && shouldClose) {
                    receiver?.close()
                    writable.close()
                }
            }
        }

        class MBJavaMidiReadConnection(val readable: MidiDevice, val isAutoClose: Boolean = false) :
            MBJavaMidiConnection(), MBConnectionReadable<UByteSerializable>, AutoCloseable {

            private val refCount = AtomicInteger(0)
            private var shouldClose = isAutoClose
            private var transmitter: Transmitter? = null
            private val listeners = mutableListOf<(UByteSerializable) -> Unit>()

            override fun open() {
                if (readable.isOpen) {
                    // Not opened by us, so keep it open later
                    shouldClose = false
                } else {
                    try {
                        readable.open()
                    } catch (e: SecurityException) {
                        throw MBException("Unable to open device $readable", e)
                    } catch (e: MidiUnavailableException) {
                        throw MBException("Resource restriction when opening device $readable", e)
                    }
                }
                refCount.incrementAndGet()
                if (transmitter == null) {
                    subscribeDevice(readable)
                }
            }

            private fun subscribeDevice(readable: MidiDevice) {
                try {
                    transmitter = readable.transmitter
                    transmitter?.receiver = object : Receiver {
                        override fun close() {
                            transmitter?.close()
                            transmitter = null
                        }

                        override fun send(message: MidiMessage?, timeStamp: Long) {
                            if (message == null) {
                                println("Skipping empty MIDI-message")
                            } else {
                                println("Received ${message.message.toUByteArray().toHexString()}")
                                emit(MidiMessageWrapper(message))
                            }
                        }
                    }
                } catch (e: MidiUnavailableException) {
                    transmitter?.close()
                    transmitter = null
                    throw MBException("Unable to obtain transmitter", e)
                }
            }

            override fun subscribe(listener: (UByteSerializable) -> Unit): MBClosable {
                listeners.add(listener)
                return MBClosable { listeners.remove(listener) }
            }

            private fun emit(message: UByteSerializable) {
                listeners.forEach { it(message) }
            }

            override fun close() {
                val current = refCount.decrementAndGet()
                if (current == 0 && shouldClose) {
                    transmitter?.close()
                    readable.close()
                }
            }
        }
    }
}

class MidiMessageWrapper(private val delegate: MidiMessage): UByteSerializable {
    fun unwrap(): MidiMessage =
        delegate

    override fun bytes(): UByteArray =
        delegate.message.toUByteArray()
}