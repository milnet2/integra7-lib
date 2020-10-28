package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBRequestResponseMidiMessage
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBUnidirectionalMidiMessage
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MidiMapper
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.RequestResponseConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RolandIntegra7(
    midiDevice: MBJavaMidiEndpoint.MBJavaMidiConnection.MBJavaMidiReadWriteConnection,
    private val midiMapper: MidiMapper<UByteSerializable, RolandIntegra7MidiMessage> = RolandIntegra7MidiMapper()) {

    private val device = RequestResponseConnection(midiDevice, midiMapper)
    private val addressRequestBuilder: CompletableFuture<AddressRequestBuilder> =
        device.send(RolandIntegra7MidiMessage.IdentityRequest())
            .map { it as RolandIntegra7MidiMessage.IdentityReply }
            .map { AddressRequestBuilder(it.deviceId) }


    init {
        device.subscribe {
            val message = midiMapper.lift(it)
            println("Received $message")
        }
    }

    fun <M> send(unidirectional: M, timestamp: Long = -1) where M: MBUnidirectionalMidiMessage, M: RolandIntegra7MidiMessage {
        device.send(unidirectional, timestamp)
    }

    fun send(rpn: RolandIntegra7RpnMessage) {
        rpn.messages.forEach { device.send(it, -1) }
    }

    fun request(req: (AddressRequestBuilder) -> Integra7MemoryIO): CompletableFuture<RolandIntegra7MidiMessage.IntegraSysExDataSet1Response> {
        val sysEx: RolandIntegra7MidiMessage = req(addressRequestBuilder.get()).asDataRequest1()
        return device.send(sysEx as MBRequestResponseMidiMessage, -1)
            .map { it as RolandIntegra7MidiMessage.IntegraSysExDataSet1Response }
    }

    fun identity(): Future<RolandIntegra7MidiMessage.IdentityReply> {
        return device.send(RolandIntegra7MidiMessage.IdentityRequest())
            .map { it as RolandIntegra7MidiMessage.IdentityReply }
    }


    fun <T, R> CompletableFuture<T>.map(mapper: (T) -> R): CompletableFuture<R> =
        this.thenApply(mapper)
}