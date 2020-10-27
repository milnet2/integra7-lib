package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MidiMapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RolandIntegra7(
    private val midiDevice: MBJavaMidiEndpoint.MBJavaMidiConnection.MBJavaMidiReadWriteConnection,
    private val midiMapper: MidiMapper<UByteSerializable, RolandIntegra7MidiMessage> = RolandIntegra7MidiMapper()) {


    init {
        midiDevice.subscribe {
            val message = midiMapper.lift(it)
            println("Received $message")
        }
    }

    fun identity(): Future<RolandIntegra7MidiMessage.IdentityReply> {
        val ret = CompletableFuture<RolandIntegra7MidiMessage.IdentityReply>()
        midiDevice.send(RolandIntegra7MidiMessage.IdentityRequest())
        // TODO: Map response
        return ret
    }

}