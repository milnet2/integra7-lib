package de.tobiasblaschke.midipi.lib.midi.bearable.lifted

import de.tobiasblaschke.midipi.lib.midi.bearable.MBConnectionReadWrite
import de.tobiasblaschke.midipi.lib.midi.bearable.UByteSerializable
import java.lang.RuntimeException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

class RequestResponseConnection<T: UByteSerializable, L: UByteSerializable>(
    private val connection: MBConnectionReadWrite<T>,
    private val midiMapper: MidiMapper<T, L>): MBConnectionReadWrite<T> by connection {

    private val responseCorrelation: MutableList<Pair<MBRequestResponseMidiMessage, RequestResponseFuture<L>>> = mutableListOf()

    init {
        subscribe { m ->
            val message = midiMapper.lift(m)
            if (message is MBResponseMidiMessage) {
                val matchedPair = responseCorrelation.firstOrNull { it.first.isExpectingResponse(message) }
                if (matchedPair != null) {
                    if (matchedPair.first.isComplete(message)) {
                        matchedPair.second.complete(message)
                    } else {
                        matchedPair.second.completePartially(message)
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

    fun send(message: MBRequestResponseMidiMessage, timestamp: Long = -1): MonadicFuture<L> {
        val ret = RequestResponseFuture<L>(merger = { a, b -> message.merge(a as MBResponseMidiMessage, b as MBResponseMidiMessage) as L }, completedAfterTimeout = message.completeAfter)
        val added = Pair(message, ret)
        this.responseCorrelation.add(added)
        connection.send(message, timestamp)
        return MonadicFuture(ret::get, ret::get).map { this.responseCorrelation.remove(added); it }
    }
}

internal class RequestResponseFuture<T>(
    private val delegate: CompletableFuture<T> = CompletableFuture<T>(),
    private val merger: (T, T) -> T,
    private val completedAfterTimeout: Duration? = null,
    private val lastInput: AtomicReference<Instant> = AtomicReference(Instant.now()),
    private val partialResponses: ArrayBlockingQueue<T> = ArrayBlockingQueue<T>(100)): Future<T> by delegate {

    fun completePartially(value: T) {
        lastInput.set(Instant.now())
        partialResponses.add(value)
    }

    fun complete(value: T) {
        partialResponses.add(value)
        val finalResult = partialResponses.reduce(merger)
        delegate.complete(finalResult)
    }

    fun completeExceptionally(e: Throwable) =
        delegate.completeExceptionally(e)

    override fun get(): T {
        if (completedAfterTimeout == null || delegate.isDone) {
            return delegate.get()
        } else {
            return try {
                get(completedAfterTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                if (Duration.between(lastInput.get(), Instant.now()).toMillis() < completedAfterTimeout.toMillis()) {
                    Thread.sleep(1)
                    this.get()
                } else if (partialResponses.isNotEmpty()) {
                    val finalResult = partialResponses.reduce(merger)
                    delegate.complete(finalResult)
                    finalResult
                } else {
                    throw RuntimeException(e)
                }
            }
        }
    }
}

class MonadicFuture<T>(private val directGetter: () -> T, private val timedGetter: (duration: Long, unit: TimeUnit) -> T): Future<T> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCancelled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDone(): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(): T =
        directGetter()

    override fun get(timeout: Long, unit: TimeUnit): T =
        timedGetter(timeout, unit)

    fun <R> map(mapper: (T) -> R): MonadicFuture<R> =
        MonadicFuture(
            directGetter = { mapper(directGetter()) },
            timedGetter = { d, u -> mapper(timedGetter(d, u)) }
        )

    fun <R> flatMap(mapper: (T) -> Future<R>): MonadicFuture<R> =
        MonadicFuture(
            directGetter = { mapper(directGetter()).get() },
            timedGetter = { d, u -> mapper(timedGetter(d, u)).get(d, u) }   // Close enough...
        )
}