package de.tobiasblaschke.midipi.server.midi.bearable.lifted

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId.Companion.ALL_DEVICES
import de.tobiasblaschke.midipi.server.midi.toHexString
import java.lang.IllegalArgumentException

fun interface MidiMapper<I: UByteSerializable, O: UByteSerializable> {
    fun lift(`in`: I): O
}

interface MBMidiMessage: UByteSerializable {
    interface NoteOff: MBMidiMessage { val channel: Int; val key: Int; val velocity: Int }
    interface NoteOn: MBMidiMessage { val channel: Int; val key: Int; val velocity: Int }
    interface PolyphonicKeyPressure: MBMidiMessage { val channel: Int; val key: Int; val velocity: Int }
    interface AllSoundsOff: MBMidiMessage { val channel: Int }
    interface ResetAllControllers: MBMidiMessage { val channel: Int; val value: Int }
    interface LocalControlOff: MBMidiMessage { val channel: Int }
    interface LocalControlOn: MBMidiMessage { val channel: Int }
    interface AllNotesOff: MBMidiMessage { val channel: Int }
    interface OmniModeOff: MBMidiMessage { val channel: Int }
    interface OmniModeOn: MBMidiMessage { val channel: Int }
    interface MonoModeOn: MBMidiMessage { val channel: Int; val channelCount: Int }
    interface PolyModeOn: MBMidiMessage { val channel: Int }
    interface ControlChangeController: MBMidiMessage { val channel: Int; val controllerNumber: Int; val value: Int }
    interface ProgramChange: MBMidiMessage { val channel: Int; val programNumber: Int }
    interface ChannelPressure: MBMidiMessage { val channel: Int; val value: Int }
    interface PitchBend: MBMidiMessage { val channel: Int; val value: Int }
    interface TimeCode: MBMidiMessage {val messageType: Int; val value: Int }
    interface SongPositionPointer: MBMidiMessage {val beats: Int }
    interface SongSelect: MBMidiMessage {val song: Int }
    interface TuneRequest: MBMidiMessage
    interface TimingClock: MBMidiMessage
    interface Start: MBMidiMessage
    interface Continue: MBMidiMessage
    interface Stop: MBMidiMessage
    interface ActiveSensing: MBMidiMessage
    interface Reset: MBMidiMessage
}

@Deprecated(message = "Reserved command, don't create :)")
data class Reserved(val bytes: UByteArray) : MBGenericMidiMessage() {
    override fun bytes(): UByteArray = bytes
}

@ExperimentalUnsignedTypes
open class GenericMidiMapper: MidiMapper<UByteSerializable, MBGenericMidiMessage> {
    override fun lift(message: UByteSerializable): MBGenericMidiMessage =
        lift(message.bytes())

    fun lift(message: UByteArray): MBGenericMidiMessage {
        val baseCommand = message[0] and 0xF0u
        val possiblyChannel = (message[0] and 0x0Fu).toInt()

        val decoded: MBGenericMidiMessage = if (baseCommand < 0xF0u) when (baseCommand.toUInt()) {
            0x80u -> MBGenericMidiMessage.ChannelEvent.NoteOn(possiblyChannel, message[1].toInt(), message[2].toInt())
            0x90u -> MBGenericMidiMessage.ChannelEvent.NoteOff(possiblyChannel, message[1].toInt(), message[2].toInt())
            0xA0u -> MBGenericMidiMessage.ChannelEvent.PolyphonicKeyPressure(
                possiblyChannel,
                message[1].toInt(),
                message[2].toInt()
            )
            0xB0u -> when (message[1].toInt()) {
                120 -> MBGenericMidiMessage.ChannelEvent.ControlChange.AllSoundsOff(possiblyChannel)
                121 -> MBGenericMidiMessage.ChannelEvent.ControlChange.ResetAllControllers(
                    possiblyChannel,
                    message[2].toInt()
                )
                122 -> when (message[2].toInt()) {
                    0 -> MBGenericMidiMessage.ChannelEvent.ControlChange.LocalControlOff(possiblyChannel)
                    127 -> MBGenericMidiMessage.ChannelEvent.ControlChange.LocalControlOn(possiblyChannel)
                    else -> throw IllegalArgumentException()
                }
                123 -> MBGenericMidiMessage.ChannelEvent.ControlChange.AllNotesOff(possiblyChannel)
                124 -> MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOff(possiblyChannel)
                125 -> MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOn(possiblyChannel)
                126 -> MBGenericMidiMessage.ChannelEvent.ControlChange.MonoModeOn(possiblyChannel, message[2].toInt())
                127 -> MBGenericMidiMessage.ChannelEvent.ControlChange.PolyModeOn(possiblyChannel)
                else -> MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(
                    possiblyChannel,
                    message[1].toInt(),
                    message[2].toInt()
                )
            }
            0xC0u -> MBGenericMidiMessage.ChannelEvent.ProgramChange(possiblyChannel, message[1].toInt())
            0xD0u -> MBGenericMidiMessage.ChannelEvent.ChannelPressure(possiblyChannel, message[0].toInt())
            0xE0u -> MBGenericMidiMessage.ChannelEvent.PitchBend(
                possiblyChannel,
                message[0].toInt() + message[1].toInt() * 0x80 - 0x2000)
            else -> throw IllegalArgumentException("Cannot decode $baseCommand")
        } else when (message[0].toUInt()) {
            0xF0u -> liftSysex(message)
            0xF1u -> MBGenericMidiMessage.SystemCommon.TimeCode((message[1] / 0x10u).toInt(), (message[1] and 0x0Fu).toInt())
            0xF2u -> MBGenericMidiMessage.SystemCommon.SongPositionPointer((message[1] + message[2] * 0x80u).toInt())
            0xF3u -> MBGenericMidiMessage.SystemCommon.SongSelect(message[1].toInt())
            0xF4u -> MBGenericMidiMessage.Reserved(message)
            0xF5u -> MBGenericMidiMessage.Reserved(message)
            0xF6u -> MBGenericMidiMessage.SystemCommon.TuneRequest
            0xF7u -> MBGenericMidiMessage.SystemCommon.EndOfExclusive
            0xF8u -> MBGenericMidiMessage.SystemRealTime.TimingClock
            0xFAu -> MBGenericMidiMessage.SystemRealTime.Start
            0xFBu -> MBGenericMidiMessage.SystemRealTime.Continue
            0xFCu -> MBGenericMidiMessage.SystemRealTime.Stop
            0xFDu -> MBGenericMidiMessage.Reserved(message)
            0xFEu -> MBGenericMidiMessage.SystemRealTime.ActiveSensing
            0xFFu -> MBGenericMidiMessage.SystemRealTime.Reset
            else ->
                throw IllegalArgumentException("Neither system-common nor real-time")
        }

        assert(decoded.bytes().contentEquals(message))

        return decoded
    }

    private fun liftSysex(message: UByteArray): MBGenericMidiMessage.SystemCommon.SystemExclusive {
        val offset: Int = if (message[1] != 0x00u.toUByte()) 1 else 3
        val manufacturer = if (message[1] != 0x00u.toUByte()) ManufacturerId.Short(message[1]) else ManufacturerId.Triplet(message[2], message[3])

        return when(manufacturer) {
            ManufacturerId.UNIVERSAL_REALTIME ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(DeviceId.Short(message[2]),
                    message.toList().drop(3).dropLast(1).toUByteArray())
            ManufacturerId.UNIVERSAL_NON_REALTIME ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime(DeviceId.Short(message[2]),
                    message.toList().drop(3).dropLast(1).toUByteArray())
            else ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific(manufacturer,
                    message.toList().drop(1 + offset).dropLast(1).toUByteArray())
        }
    }
}

@ExperimentalUnsignedTypes
sealed class MBGenericMidiMessage: MBMidiMessage {
    sealed class ChannelEvent: MBGenericMidiMessage() {
        abstract val channel: Int

        data class NoteOff(override val channel: Int, override val key: Int, override val velocity: Int) : ChannelEvent(), MBMidiMessage.NoteOff {
            init {
                assert(channel in 0..15)
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0x80u.toUByte() and channel.toUByte(), key.toUByte(), velocity.toUByte())
        }

        data class NoteOn(override val channel: Int, override val key: Int, override val velocity: Int) : ChannelEvent(), MBMidiMessage.NoteOn {
            init {
                assert(channel in 0..15)
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0x90u.toUByte() and channel.toUByte(), key.toUByte(), velocity.toUByte())
        }

        data class PolyphonicKeyPressure(override val channel: Int, override val key: Int, override val velocity: Int) : ChannelEvent(), MBMidiMessage.PolyphonicKeyPressure {
            init {
                assert(channel in 0..15)
                assert(key in 0..127)
                assert(velocity in 0..127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xA0u.toUByte() and channel.toUByte(), key.toUByte(), velocity.toUByte())
        }

        sealed class ControlChange: ChannelEvent() {
            data class AllSoundsOff(override val channel: Int) : ControlChange(), MBMidiMessage.AllSoundsOff {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 120u, 0u)
            }

            data class ResetAllControllers(override val channel: Int, override val value: Int = 0) : ControlChange(), MBMidiMessage.ResetAllControllers {
                init {
                    assert(channel in 0..15)
                    assert(value in 0..127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 121u, value.toUByte())
            }

            data class LocalControlOff(override val channel: Int) : ControlChange(), MBMidiMessage.LocalControlOff {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 122u, 0u)
            }

            data class LocalControlOn(override val channel: Int) : ControlChange(), MBMidiMessage.LocalControlOn {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 122u, 127u)
            }

            data class AllNotesOff(override val channel: Int) : ControlChange(), MBMidiMessage.AllNotesOff {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 123u, 0u)
            }

            data class OmniModeOff(override val channel: Int) : ControlChange(), MBMidiMessage.OmniModeOff {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 124u, 0u)
            }

            data class OmniModeOn(override val channel: Int) : ControlChange(), MBMidiMessage.OmniModeOn {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 125u, 0u)
            }

            data class MonoModeOn(override val channel: Int, override val channelCount: Int) : ControlChange(), MBMidiMessage.MonoModeOn {
                init {
                    assert(channel in 0..15)
                    assert(channelCount in 0..127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 126u, channelCount.toUByte())
            }

            data class PolyModeOn(override val channel: Int) : ControlChange(), MBMidiMessage.PolyModeOn {
                init {
                    assert(channel in 0..15)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), 127u, 0u)
            }

            data class ControlChangeController(override val channel: Int, override val controllerNumber: Int, override val value: Int): ControlChange(), MBMidiMessage.ControlChangeController {
                init {
                    assert(channel in 0..15)
                    assert(controllerNumber in 0 .. 119)
                    assert(value in 0 .. 127)
                }
                override fun bytes(): UByteArray = ubyteArrayOf(
                    0xB0u.toUByte() and channel.toUByte(), controllerNumber.toUByte(), value.toUByte())
                override fun toString(): String =
                    String.format("%s c=0x%02X(%d) v=0x%02X(%d)", this.javaClass.simpleName, controllerNumber, controllerNumber, value, value)
            }
        }

        data class ProgramChange(override val channel: Int, override val programNumber: Int): ChannelEvent(), MBMidiMessage.ProgramChange {
            init {
                assert(channel in 0..15)
                assert(programNumber in 0 .. 127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xC0.toUByte() and channel.toUByte(), programNumber.toUByte())
        }
        data class ChannelPressure(override val channel: Int, override val value: Int) : ChannelEvent(), MBMidiMessage.ChannelPressure {
            init {
                assert(channel in 0..15)
                assert(value in 0 .. 127)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xD0.toUByte() and channel.toUByte(), value.toUByte())
        }
        data class PitchBend(override val channel: Int, override val value: Int) : ChannelEvent(), MBMidiMessage.PitchBend {
            init {
                assert(channel in 0..15)
                assert(value in -16384/2 .. 16384/2)
            }
            override fun bytes(): UByteArray {
                val low: UByte = ((value + 0x2000).toUInt() and 0x7Fu).toUByte()
                val high: UByte = ((value + 0x2000) / 0x80).toUByte()
                return ubyteArrayOf(
                    0xE0.toUByte() and channel.toUByte(), low, high)
            }
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
                    assert(payload.all { it in 0x00u .. 0x7Fu })
                }
                override fun toString(): String =
                    "SystemExclusive(to=$manufacturer payload=${payload.toHexString()})"
                override fun bytes(): UByteArray =
                    ubyteArrayOf(0xF0u) + manufacturer.bytes() + payload + ubyteArrayOf(0xF7u)
            }
        }

        data class TimeCode(override val messageType: Int, override val value: Int) : SystemCommon(), MBMidiMessage.TimeCode {
            init {
                assert(messageType in 0 .. 7)
                assert(value in 0 .. 0x0F)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xF1u, (messageType * 0x10 + value).toUByte())
        }

        data class SongPositionPointer(override val beats: Int) : SystemCommon(), MBMidiMessage.SongPositionPointer {
            public val midiClocks = beats * 6
            init {
                assert(beats in 0 .. 0x3FFF)
            }
            override fun bytes(): UByteArray {
                val low = (beats and 0x7F).toUByte()
                val high = (beats / 0x80).toUByte()
                return ubyteArrayOf(0xF2u, low, high)
            }
        }

        data class SongSelect(override val song: Int) : SystemCommon(), MBMidiMessage.SongSelect {
            init {
                assert(song in 0 .. 0x7F)
            }
            override fun bytes(): UByteArray = ubyteArrayOf(
                0xF3u, song.toUByte())
        }

        object TuneRequest: SystemCommon(), MBMidiMessage.TuneRequest {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF6u)
        }

        @Deprecated(message = "EOX doesn't make sense on it's own. Should be part of the SysEx-body!")
        object EndOfExclusive: SystemCommon() {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF7u)
        }
    }

    sealed class SystemRealTime: MBGenericMidiMessage() {
        object TimingClock: SystemRealTime(), MBMidiMessage.TimingClock {
            override fun bytes(): UByteArray = ubyteArrayOf(0xF8u)
        }

        object Start: SystemRealTime(), MBMidiMessage.Start {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFAu)
        }

        object Continue: SystemRealTime(), MBMidiMessage.Continue {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFBu)
        }

        object Stop: SystemRealTime(), MBMidiMessage.Stop {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFCu)
        }

        object ActiveSensing: SystemRealTime(), MBMidiMessage.ActiveSensing {
            override fun bytes(): UByteArray = ubyteArrayOf(0xFEu)
        }

        object Reset: SystemRealTime(), MBMidiMessage.Reset {
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
            assert(deviceId in 1 .. 0x7F)
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
            assert(manufacturer in 1 .. 0x7F)
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
            assert(first in 0x00 .. 0x80)
            assert(second in 0x00 .. 0x80)
        }
        override fun bytes() = ubyteArrayOf(0x00u, first, second)
    }
}