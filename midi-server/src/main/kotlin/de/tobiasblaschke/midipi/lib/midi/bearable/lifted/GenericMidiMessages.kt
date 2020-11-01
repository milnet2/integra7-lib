package de.tobiasblaschke.midipi.lib.midi.bearable.lifted

import de.tobiasblaschke.midipi.lib.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.lib.midi.bearable.domain.MidiChannel
import de.tobiasblaschke.midipi.lib.midi.bearable.lifted.DeviceId.Companion.ALL_DEVICES
import de.tobiasblaschke.midipi.lib.midi.toHexString
import de.tobiasblaschke.midipi.lib.utils.toUShort7
import java.time.Duration

fun interface MidiMapper<I: UByteSerializable, O: UByteSerializable> {
    fun lift(`in`: I): O
}

interface MBUnidirectionalMidiMessage: UByteSerializable {
    interface NoteOff: MBUnidirectionalMidiMessage { val channel: MidiChannel; val key: Int; val velocity: Int }
    interface NoteOn: MBUnidirectionalMidiMessage { val channel: MidiChannel; val key: Int; val velocity: Int }
    interface PolyphonicKeyPressure: MBUnidirectionalMidiMessage { val channel: MidiChannel; val key: Int; val velocity: Int }
    interface AllSoundsOff: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface ResetAllControllers: MBUnidirectionalMidiMessage { val channel: MidiChannel; val value: Int }
    interface LocalControlOff: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface LocalControlOn: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface AllNotesOff: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface OmniModeOff: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface OmniModeOn: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface MonoModeOn: MBUnidirectionalMidiMessage { val channel: MidiChannel; val channelCount: Int }
    interface PolyModeOn: MBUnidirectionalMidiMessage { val channel: MidiChannel }
    interface ControlChangeController: MBUnidirectionalMidiMessage { val channel: MidiChannel; val controllerNumber: Int; val value: Int }
    interface ProgramChange: MBUnidirectionalMidiMessage { val channel: MidiChannel; val programNumber: Int }
    interface ChannelPressure: MBUnidirectionalMidiMessage { val channel: MidiChannel; val value: Int }
    interface PitchBend: MBUnidirectionalMidiMessage { val channel: MidiChannel; val value: Int }
    interface TimeCode: MBUnidirectionalMidiMessage {val messageType: Int; val value: Int }
    interface SongPositionPointer: MBUnidirectionalMidiMessage {val beats: Int }
    interface SongSelect: MBUnidirectionalMidiMessage {val song: Int }
    interface TuneRequest: MBUnidirectionalMidiMessage
    interface TimingClock: MBUnidirectionalMidiMessage
    interface Start: MBUnidirectionalMidiMessage
    interface Continue: MBUnidirectionalMidiMessage
    interface Stop: MBUnidirectionalMidiMessage
    interface ActiveSensing: MBUnidirectionalMidiMessage
    interface Reset: MBUnidirectionalMidiMessage
}

interface MBRequestResponseMidiMessage: UByteSerializable {
    val completeAfter: Duration?
    fun isExpectingResponse(message: MBResponseMidiMessage): Boolean
    fun isComplete(message: MBResponseMidiMessage): Boolean = true
    fun merge(left: MBResponseMidiMessage, right: MBResponseMidiMessage): MBResponseMidiMessage {
        println("OVERRIDE ME!!!")
        TODO("Not yet implemented")
    }
}

interface MBResponseMidiMessage: UByteSerializable

@Deprecated(message = "Reserved command, don't create :)")
data class Reserved(val bytes: UByteArray) : MBGenericMidiMessage() {
    override fun bytes(): UByteArray = bytes
}

@ExperimentalUnsignedTypes
sealed class MBGenericMidiMessage: MBUnidirectionalMidiMessage {
    sealed class ChannelEvent: MBGenericMidiMessage() {
        abstract val channel: MidiChannel

        data class NoteOff(override val channel: MidiChannel, override val key: Int, override val velocity: Int) : ChannelEvent(), MBUnidirectionalMidiMessage.NoteOff {
            init {
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0x80u.toUByte() or channel.zeroBased.toUByte(), key.toUByte(), velocity.toUByte())
        }

        data class NoteOn(override val channel: MidiChannel, override val key: Int, override val velocity: Int) : ChannelEvent(), MBUnidirectionalMidiMessage.NoteOn {
            init {
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0x90u.toUByte() or channel.zeroBased.toUByte(), key.toUByte(), velocity.toUByte())
        }

        data class PolyphonicKeyPressure(override val channel: MidiChannel, override val key: Int, override val velocity: Int) : ChannelEvent(), MBUnidirectionalMidiMessage.PolyphonicKeyPressure {
            init {
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xA0u.toUByte() or channel.zeroBased.toUByte(), key.toUByte(), velocity.toUByte())
        }

        sealed class ControlChange: ChannelEvent() {
            data class AllSoundsOff(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.AllSoundsOff {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 120u, 0u)
            }

            data class ResetAllControllers(override val channel: MidiChannel, override val value: Int = 0) : ControlChange(), MBUnidirectionalMidiMessage.ResetAllControllers {
                init {
                    assert(value in 0..127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 121u, value.toUByte())
            }

            data class LocalControlOff(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.LocalControlOff {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 122u, 0u)
            }

            data class LocalControlOn(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.LocalControlOn {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 122u, 127u)
            }

            data class AllNotesOff(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.AllNotesOff {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 123u, 0u)
            }

            data class OmniModeOff(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.OmniModeOff {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 124u, 0u)
            }

            data class OmniModeOn(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.OmniModeOn {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 125u, 0u)
            }

            data class MonoModeOn(override val channel: MidiChannel, override val channelCount: Int) : ControlChange(), MBUnidirectionalMidiMessage.MonoModeOn {
                init {
                    assert(channelCount in 0..127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 126u, channelCount.toUByte())
            }

            data class PolyModeOn(override val channel: MidiChannel) : ControlChange(), MBUnidirectionalMidiMessage.PolyModeOn {
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), 127u, 0u)
            }

            data class ControlChangeController(override val channel: MidiChannel, override val controllerNumber: Int, override val value: Int): ControlChange(), MBUnidirectionalMidiMessage.ControlChangeController {
                init {
                    assert(controllerNumber in 0 .. 119)
                    assert(value in 0 .. 127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() or channel.zeroBased.toUByte(), controllerNumber.toUByte(), value.toUByte())
                override fun toString(): String =
                    String.format("%s c=0x%02X(%d) v=0x%02X(%d)", this.javaClass.simpleName, controllerNumber, controllerNumber, value, value)
            }
        }

        data class ProgramChange(override val channel: MidiChannel, override val programNumber: Int): ChannelEvent(), MBUnidirectionalMidiMessage.ProgramChange {
            init {
                assert(programNumber in 0 .. 127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xC0.toUByte() or channel.zeroBased.toUByte(), programNumber.toUByte())
        }
        data class ChannelPressure(override val channel: MidiChannel, override val value: Int) : ChannelEvent(), MBUnidirectionalMidiMessage.ChannelPressure {
            init {
                assert(value in 0 .. 127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xD0.toUByte() or channel.zeroBased.toUByte(), value.toUByte())
        }
        data class PitchBend(override val channel: MidiChannel, override val value: Int) : ChannelEvent(), MBUnidirectionalMidiMessage.PitchBend {
            init {
                assert(value in -16384/2 .. 16384/2)
            }
            override fun bytes(): UByteArray =
                (value + 2000).toUShort7().toUByteArrayLittleEndian()
        }
    }

    sealed class SystemCommon: MBGenericMidiMessage() {
        sealed class SystemExclusive: SystemCommon() {
//            data class RecordReady(val deviceId: DeviceId = ALL_DEVICES, val length1: UByte, val length2: UByte, val trackBitSet: BitSet): SystemExclusive() { // MMC
//                init {
//                    // TODO: This needs refinement!
//                    assert(deviceId in 0 .. 0xFFFFFF)
//                    assert(length1 in 1 .. 0xF6)
//                    assert(length2 in 0 .. 0xF6)
//                    assert(trackBitSet.size() / 7 < length2.toInt())
//                    assert(trackBitSet.toByteArray().all { it < 0x80 })
//                }
//                override fun bytes(): UByteArray =
//                    ubyteArrayOf(0xF0u, 0x7Fu) + deviceId.bytes() + ubyteArrayOf(0x06u, 0x40u, length1, 0x4Fu, length2) +
//                            trackBitSet.toByteArray().toUByteArray() + ubyteArrayOf(0xF7u)
//            }
//
//            data class Goto(val deviceId: DeviceId = ALL_DEVICES, val position: Duration, val frames: UByte = 0u, val subFrames: UByte = 0u): SystemExclusive() { // MMC
//                init {
//                    assert(deviceId in 0 .. 0xFFFFFF)
//                    assert(position.toHours() < 24)
//                    assert(frames in 0 .. 29)
//                    assert(subFrames in 0 .. 99)
//                }
//                override fun bytes(): UByteArray =
//                    ubyteArrayOf(0xF0u, 0xF7u) + deviceId.bytes() + ubyteArrayOf(0x06u, 0x44u, 0x06u, 0x01u,
//                        position.toHours().toUByte(), (position.toMinutes() % 60).toUByte(), (position.seconds % 60).toUByte(),
//                        frames, subFrames, 0xF7u)
//            }
//
//            data class Shuttle(val deviceId: DeviceId = ALL_DEVICES, val speed: Int): SystemExclusive() {
//                override fun bytes(): UByteArray {
//                    val sm: UByte = speed.absoluteValue
//                    return ubyteArrayOf(0xF0u, 0xF7u) + marshalDeviceId(deviceId) + ubyteArrayOf(0x06u, 0x47u, 0x03u,  0xF7u)
//                }
//            }

            data class UniversalNonRealtime(val deviceId: DeviceId = ALL_DEVICES, val payload: UByteArray): SystemExclusive() {
                init {
                    assert(payload.all { it in 0x00u .. 0x7Fu })
                }
                override fun toString(): String =
                    "UniversalNonRealtime(to=$deviceId payload=${payload.toHexString()})"
                override fun bytes(): UByteArray =
                   ubyteArrayOf(0xF0u, 0x7Eu) + deviceId.bytes() + payload + ubyteArrayOf(0xF7u)
            }


            data class UniversalRealtime(val deviceId: DeviceId = ALL_DEVICES, val payload: UByteArray): SystemExclusive() {
                init {
                    assert(payload.all { it in 0x00u .. 0x7Fu })
                }
                override fun toString(): String =
                    "UniversalNonRealtime(to=$deviceId payload=${payload.toHexString()})"
                override fun bytes(): UByteArray =
                    ubyteArrayOf(0xF0u, 0x7Fu) + deviceId.bytes() + payload + ubyteArrayOf(0xF7u)
            }

            data class ManufacturerSpecific(val manufacturer: ManufacturerId, val payload: UByteArray): SystemExclusive() {
                init {
                    assert(payload.all { it in 0x00u .. 0x7Fu }) { "Payload contains illegal value: ${payload.toHexString()}" }
                }
                override fun toString(): String =
                    "SystemExclusive(to=$manufacturer payload=${payload.toHexString()})"
                override fun bytes(): UByteArray =
                    ubyteArrayOf(0xF0u) + manufacturer.bytes() + payload + ubyteArrayOf(0xF7u)
            }
        }

        data class TimeCode(override val messageType: Int, override val value: Int) : SystemCommon(), MBUnidirectionalMidiMessage.TimeCode {
            init {
                assert(messageType in 0 .. 7)
                assert(value in 0 .. 0x0F)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xF1u, (messageType * 0x10 + value).toUByte())
        }

        data class SongPositionPointer(override val beats: Int) : SystemCommon(), MBUnidirectionalMidiMessage.SongPositionPointer {
            public val midiClocks = beats * 6
            init {
                assert(beats in 0 .. 0x3FFF)
            }
            override fun bytes(): UByteArray =
                beats.toUShort7().toUByteArrayLittleEndian()
        }

        data class SongSelect(override val song: Int) : SystemCommon(), MBUnidirectionalMidiMessage.SongSelect {
            init {
                assert(song in 0 .. 0x7F)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xF3u, song.toUByte())
        }

        object TuneRequest: SystemCommon(), MBUnidirectionalMidiMessage.TuneRequest {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF6u)
        }

        @Deprecated(message = "EOX doesn't make sense on it's own. Should be part of the SysEx-body!")
        object EndOfExclusive: SystemCommon() {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF7u)
        }
    }

    sealed class SystemRealTime: MBGenericMidiMessage() {
        object TimingClock: SystemRealTime(), MBUnidirectionalMidiMessage.TimingClock {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF8u)
        }

        object Start: SystemRealTime(), MBUnidirectionalMidiMessage.Start {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFAu)
        }

        object Continue: SystemRealTime(), MBUnidirectionalMidiMessage.Continue {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFBu)
        }

        object Stop: SystemRealTime(), MBUnidirectionalMidiMessage.Stop {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFCu)
        }

        object ActiveSensing: SystemRealTime(), MBUnidirectionalMidiMessage.ActiveSensing {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFEu)
        }

        object Reset: SystemRealTime(), MBUnidirectionalMidiMessage.Reset {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFEu)
        }
    }

    @Deprecated(message = "Reserved command, don't create :)")
    data class Reserved(val bytes: UByteArray) : MBGenericMidiMessage() {
        override fun bytes(): UByteArray = bytes
    }
}

@ExperimentalUnsignedTypes
sealed class DeviceId: UByteSerializable {
    companion object {
        val ALL_DEVICES = Short(0x7Fu)
    }
    data class Short(val deviceId: UByte): DeviceId() {
        init {
            assert(deviceId in 0x01u .. 0x7Fu)
        }
        override fun bytes() = ubyteArrayOf(deviceId)
    }
}

@ExperimentalUnsignedTypes
sealed class ManufacturerId: UByteSerializable {
    companion object {
        val UNIVERSAL_REALTIME = Short(0x7Fu)
        val UNIVERSAL_NON_REALTIME = Short(0x7Eu)

        val ROLAND = Short(0x41u)
    }
    data class Short(val manufacturer: UByte): ManufacturerId() {
        init {
            assert(manufacturer in 0x01u .. 0x7Fu) { String.format("Manufacturer 0x02X not in the expected range!", manufacturer) }
        }
        override fun bytes() = ubyteArrayOf(manufacturer)

        override fun toString(): String = when(this) {
            UNIVERSAL_NON_REALTIME -> "UNIVERSAL_NON_REALTIME"
            UNIVERSAL_REALTIME -> "UNIVERSAL_REALTIME"
            ROLAND -> "Roland"
            else -> super.toString()
        }
    }
    data class Triplet(val first: UByte, val second: UByte): ManufacturerId() {
        init {
            assert(first in 0x00u .. 0x7Fu)
            assert(second in 0x00u .. 0x7Fu)
        }
        override fun bytes() = ubyteArrayOf(0x00u, first, second)
    }
}