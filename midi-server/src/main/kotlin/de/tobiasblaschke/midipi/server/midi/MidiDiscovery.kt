package de.tobiasblaschke.midipi.server.midi

import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.sound.midi.*


class MidiDiscovery {
    fun scan(): List<MatchedDevice> {
        val primordialList = MidiSystem.getMidiDeviceInfo()
            .mapNotNull(::map)
            .toList()

        val probe = SysexProbe(primordialList)
        return probe.probeAll()
    }

    private fun map(info: MidiDevice.Info): MidiDeviceDescriptor? {
        var device: MidiDevice? = null
        try {
            device = MidiSystem.getMidiDevice(info)

            return when (device.javaClass.simpleName) {
                "SoftSynthesizer" -> MidiDeviceDescriptor.SoftwareSynthesizerInfo(device)
                "MidiInDevice" -> MidiDeviceDescriptor.MidiInDeviceInfo(device)
                "MidiOutDevice" -> MidiDeviceDescriptor.MidiOutDeviceInfo(device)
                "RealTimeSequencer" -> MidiDeviceDescriptor.RealTimeSequencerInfo(device)
                else -> when (device) {
                    is Synthesizer -> MidiDeviceDescriptor.SynthesizerInfo(device)
                    is Sequencer -> MidiDeviceDescriptor.SequencerInfo(device)
                    else -> MidiDeviceDescriptor.UnknownDeviceInfo(device)
                }
            }
        } catch (e: MidiUnavailableException) {
            return null // MidiDeviceDescriptor.UnknownDeviceInfo(device)
        } finally {
            if (device != null && device.isOpen) {
                device.close()
            }
        }
    }

    class SysexProbe(val toProbe: List<MidiDeviceDescriptor>) {
        companion object {
            const val SYSEX_GLOBAL_CHANNEL_BYTE: UByte = 0x7Fu
            val SYSEX_IDENTITY_REQUEST_BYTES = ubyteArrayOf(0xF0u, 0x7Eu, SYSEX_GLOBAL_CHANNEL_BYTE, 0x06u, 0x01u, 0xF7u)
        }

        private val answers = mutableListOf<SysexReceiver>()
        private val devices = mutableListOf<MidiDevice>()
        private val matchedDevices = mutableListOf<MatchedDevice>()

        init {
            toProbe.forEach {
                when(it) {
                    is MidiDeviceDescriptor.SynthesizerInfo -> matchedDevices.add(MatchedDevice.Single(it))
                    is MidiDeviceDescriptor.SoftwareSynthesizerInfo -> matchedDevices.add(MatchedDevice.Single(it))
                    is MidiDeviceDescriptor.MidiInDeviceInfo -> { }
                    is MidiDeviceDescriptor.MidiOutDeviceInfo -> { }
                    is MidiDeviceDescriptor.RealTimeSequencerInfo -> matchedDevices.add(MatchedDevice.Single(it))
                    is MidiDeviceDescriptor.SequencerInfo -> matchedDevices.add(MatchedDevice.Single(it))
                    is MidiDeviceDescriptor.UnknownDeviceInfo -> matchedDevices.add(MatchedDevice.Single(it))
                }
            }
        }

        fun probeAll(): List<MatchedDevice> {
            // Attach to all inputs
            toProbe
                .filterIsInstance<MidiDeviceDescriptor.MidiInDeviceInfo>()
                .forEach(this::attachInDevice)

            Thread.sleep(1000)
            println("Attachment complete")

            // Send on each output
            toProbe
                .filterIsInstance<MidiDeviceDescriptor.MidiOutDeviceInfo>()
                .map(::probe)
                .forEach(matchedDevices::add)
            println("Probing complete")

            // Append Unmatched
            answers
                .filter { !it.response.isDone && !it.consumed }
                .forEach {
                    it.close()
                    it.consumed = true
                    matchedDevices.add(MatchedDevice.Single(it.info))
                }

            devices
                .filter { it.isOpen }
                .forEach {
                    println("  Closing ${it.deviceInfo.description}")
                    it.close()
                }

            return matchedDevices
        }

        private fun probe(info: MidiDeviceDescriptor.MidiOutDeviceInfo): MatchedDevice {
            var input: MidiDevice? = null

            try {
                input = info.device()
                input.open()
                sendRequest(input)

                // Wait for answer
                val output = awaitResponse()

                return if (output != null) {
                    MatchedDevice.Pair(writable = info, readable = output.info, output.response.get())
                } else {
                    MatchedDevice.Single(info)
                }
            } catch (e: MidiUnavailableException) {
                println("  Unavailable: $info")
                return MatchedDevice.Single(info)
            } finally {
                if (input != null && input.isOpen) {
                    println("  Closing $info")
                    input.close()
                }
            }
        }

        private fun sendRequest(input: MidiDevice) {
            assert(input.isOpen)
            println("  Sending to ${input.deviceInfo.description}")
            val sysexIdentityRequest = SysexMessage()
            sysexIdentityRequest.setMessage(SYSEX_IDENTITY_REQUEST_BYTES.toByteArray(), SYSEX_IDENTITY_REQUEST_BYTES.size)
            input.receiver.send(sysexIdentityRequest, -1)
        }

        private fun awaitResponse(): SysexReceiver? {

            return try {
                val futures = answers.map { it.response }
                CompletableFuture.anyOf(*futures.toTypedArray())
                    .get(1, TimeUnit.SECONDS)

                val ret = answers
                    .firstOrNull {
                        it.response.isDone
                    }
                if (ret != null) {
                    println("    RESPONSE")
                    answers.remove(ret)
                } else {
                    println("    no response")
                }

                ret
            } catch (e: TimeoutException) {
                println("    no response")
                null
            }
        }

        private fun attachInDevice(info: MidiDeviceDescriptor.MidiInDeviceInfo) {
            var device: MidiDevice? = null
            var receiver: SysexReceiver? = null
            try {
                device = info.device()
                device.open()
                val transmitter = device.transmitter
                receiver = SysexReceiver(info)
                transmitter.receiver = receiver
                println("  Attached to $info")
                answers.add(receiver)
                devices.add(device)
            } catch (e: MidiUnavailableException) {
                println("  Attaching to ${info}: ${e.message}")
                if (device != null && device.isOpen) {
                    println("  Closing device $info")
                    device.close()
                }
                receiver?.close()
            }
        }

        class SysexReceiver(val info: MidiDeviceDescriptor.MidiInDeviceInfo): Receiver {
            val response = CompletableFuture<UByteArray>()
            var consumed = false

            override fun close() {
                println("  Closing receiver $info")
                if (!response.isDone) {
                    response.completeExceptionally(IllegalStateException("closed"))
                }
            }

            override fun send(message: MidiMessage?, timeStamp: Long) {
                when(message) {
                    is SysexMessage -> {
                        val unsigned = message.data.toUByteArray()
                        println("  Received: ${unsigned.toHexString()} on $info")
                        response.complete(unsigned)
                    }
                    else -> {
                        println("  unexpected response on $info!")
                        close()
                    }
                }
            }
        }
    }
}

sealed class MidiDeviceDescriptor(protected val device: MidiDevice) {
    open fun device() = device
    override fun toString(): String =
        this.javaClass.simpleName + " " + device.deviceInfo.toString() + " " + device.deviceInfo.description

    class SynthesizerInfo(device: MidiDevice): MidiDeviceDescriptor(device as Synthesizer) {
        override fun device(): Synthesizer {
            return device as Synthesizer
        }
    }
    class SoftwareSynthesizerInfo(device: MidiDevice): MidiDeviceDescriptor(device as Synthesizer)  {
        override fun device(): Synthesizer {
            return device as Synthesizer
        }
    }
    class MidiInDeviceInfo(device: MidiDevice): MidiDeviceDescriptor(device)
    class MidiOutDeviceInfo(device: MidiDevice): MidiDeviceDescriptor(device)
    class RealTimeSequencerInfo(device: MidiDevice): MidiDeviceDescriptor(device as Sequencer) {
        override fun device(): Sequencer {
            return device as Sequencer
        }
    }
    class SequencerInfo(device: MidiDevice): MidiDeviceDescriptor(device as Sequencer)  {
        override fun device(): Sequencer {
            return device as Sequencer
        }
    }
    class UnknownDeviceInfo(device: MidiDevice): MidiDeviceDescriptor(device)
}

sealed class MatchedDevice {
    data class Single(val device: MidiDeviceDescriptor): MatchedDevice()
    data class Pair(val writable: MidiDeviceDescriptor.MidiOutDeviceInfo, val readable: MidiDeviceDescriptor.MidiInDeviceInfo, val identifyResponse: UByteArray): MatchedDevice() {
        override fun toString(): String =
            "Device-pair(\n  writable = $writable\n  readable = $readable\n  identifyResponse = ${identifyResponse.toHexString()})"
    }
}

fun UByteArray.toHexString() =
    this.joinToString(
            separator = " ",
            transform = { String.format("0x%02X", it.toInt()) }
        )

fun UByteArray.toAsciiString(skip: Int = 0, length: Int = this.size) =
    this.toList()
        .drop(skip)
        .take(length)
        .takeWhile { it != 0x00u.toUByte() }
        .joinToString(
            separator = "",
            transform = { if (it in 0x20u .. 0x7Du) it.toByte().toChar().toString() else "." })