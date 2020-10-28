package de.tobiasblaschke.midipi.server.midi.bearable.lifted

import de.tobiasblaschke.midipi.server.midi.bearable.MBConnectionReadWrite
import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import java.util.concurrent.CompletableFuture

class RequestResponseConnection<T: UByteSerializable, L: UByteSerializable>(
    private val connection: MBConnectionReadWrite<T>,
    private val midiMapper: MidiMapper<T, L>): MBConnectionReadWrite<T> by connection {

    private val responseCorrelation: MutableList<Pair<MBRequestResponseMidiMessage, CompletableFuture<L>>> = mutableListOf()

    init {
        subscribe { m ->
            val message = midiMapper.lift(m)
            if (message is MBResponseMidiMessage) {
                val matchedPair = responseCorrelation.firstOrNull { it.first.isExpectingResponse(message) }
                if (matchedPair != null) {
                    if (matchedPair.first.isComplete(message)) {
                        println("Message complete")
                        responseCorrelation.remove(matchedPair)
                        matchedPair.second.complete(message)
                    } else {
                        println("Waiting for more data")
                        // TODO: Possible race!
                        val originalFuture = matchedPair.second
                        val nextChunk = CompletableFuture<L>()
                        nextChunk.whenComplete { response, throwable ->
                                if (throwable != null) {
                                    println("COMBINING!!!!!! Error: $throwable")
                                    originalFuture.completeExceptionally(throwable)
                                } else {
                                    val ret = matchedPair.first.merge(message, response as MBResponseMidiMessage) as L
                                    println("COMBINING!!!!!! Response: $ret")
                                    originalFuture.complete(ret)
                                }
                            }

                        responseCorrelation.remove(matchedPair)
                        responseCorrelation.add(Pair(matchedPair.first, nextChunk ))
                    }
                }
            }
        }
    }

    @Deprecated("Better use the two other send-methods")
    override fun send(message: UByteSerializable, timestamp: Long) {
        assert(message is MBUnidirectionalMidiMessage)
        send(message as MBUnidirectionalMidiMessage)
    }

    fun send(message: MBUnidirectionalMidiMessage, timestamp: Long = -1) {
        connection.send(message, timestamp)
    }

    fun send(message: MBRequestResponseMidiMessage, timestamp: Long = -1): CompletableFuture<L> {
        val ret = CompletableFuture<L>()
        this.responseCorrelation.add(Pair(message, ret))
        connection.send(message, timestamp)
        return ret
    }
}