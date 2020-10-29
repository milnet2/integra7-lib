package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.*
import de.tobiasblaschke.midipi.server.midi.utils.IndexedCollection
import de.tobiasblaschke.midipi.server.midi.utils.SparseUByteArray
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RolandIntegra7(
    midiDevice: MBJavaMidiEndpoint.MBJavaMidiConnection.MBJavaMidiReadWriteConnection,
    private val midiMapper: MidiMapper<UByteSerializable, RolandIntegra7MidiMessage> = RolandIntegra7MidiMapper()) {

    private val device = RequestResponseConnection(midiDevice, midiMapper)
    private val addressRequestBuilder: MonadicFuture<AddressRequestBuilder> =
        device.send(RolandIntegra7MidiMessage.IdentityRequest())
            .map { it as RolandIntegra7MidiMessage.IdentityReply }
            .map { AddressRequestBuilder(it.deviceId) }

    fun part(p: IntegraPart) =
        Integra7PartFacade(p, this)

    init {
        device.subscribe {
            val message = midiMapper.lift(it)
//            println("Received $message")
        }
    }

    fun <M> send(unidirectional: M, timestamp: Long = -1) where M: MBUnidirectionalMidiMessage, M: RolandIntegra7MidiMessage {
        device.send(unidirectional, timestamp)
    }

    fun send(rpn: RolandIntegra7RpnMessage) {
        rpn.messages.forEach { device.send(it, -1) }
    }

    fun <T> request(req: (AddressRequestBuilder) -> Integra7MemoryIO<T>): MonadicFuture<T> {
        val addressRange = req(addressRequestBuilder.get())
        val sysEx: RolandIntegra7MidiMessage = addressRange.asDataRequest1()
        return device.send(sysEx as MBRequestResponseMidiMessage, -1)
            .map { it as RolandIntegra7MidiMessage.IntegraSysExDataSet1Response }
            .map { addressRange.interpret(it.startAddress, it.payload.size, it.payload) }
    }

    fun identity(): Future<RolandIntegra7MidiMessage.IdentityReply> {
        return device.send(RolandIntegra7MidiMessage.IdentityRequest())
            .map { it as RolandIntegra7MidiMessage.IdentityReply }
    }


    fun <T, R> CompletableFuture<T>.map(mapper: (T) -> R): CompletableFuture<R> =
        this.thenApply(mapper)




    class Integra7PartFacade(private val part: IntegraPart, private val integra: RolandIntegra7) {
        val sound: Integra7ToneFacade
            get() = Integra7ToneFacade(integra.request({ it.tones[part]!! }).get())

        class Integra7ToneFacade(private val tone: ToneAddressRequestBuilder.TemporaryTone) {  // Temporary Tone
            val pcm: PcmSynthToneBuilder.PcmSynthTone
                get() = tone.pcmSynthTone
        }
    }
}

enum class IntegraPart(val zeroBased: Int) {
    P1(0), P2(1), P3(2), P4(3), P5(4),
    P6(5), P7(6), P8(7), P9(8), P10(9),
    P11(10), P12(11), P13(12), P14(13), P15(14),
    P16(15)
}

