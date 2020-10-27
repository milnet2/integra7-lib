package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MidiMapper
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.RequestResponseConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RolandIntegra7(
    midiDevice: MBJavaMidiEndpoint.MBJavaMidiConnection.MBJavaMidiReadWriteConnection,
    private val midiMapper: MidiMapper<UByteSerializable, RolandIntegra7MidiMessage> = RolandIntegra7MidiMapper()) {

    private val device = RequestResponseConnection(midiDevice, midiMapper)

    init {
        device.subscribe {
            val message = midiMapper.lift(it)
            println("Received $message")
        }
    }

    fun identity(): Future<RolandIntegra7MidiMessage.IdentityReply> {
        return device.send(RolandIntegra7MidiMessage.IdentityRequest())
            .map { it as RolandIntegra7MidiMessage.IdentityReply }
    }


    fun <T, R> CompletableFuture<T>.map(mapper: (T) -> R): CompletableFuture<R> =
        this.thenApply(mapper)
}