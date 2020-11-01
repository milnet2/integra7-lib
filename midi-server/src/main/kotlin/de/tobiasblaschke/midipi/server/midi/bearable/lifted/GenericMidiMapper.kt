package de.tobiasblaschke.midipi.server.midi.bearable.lifted

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.domain.MidiChannel
import java.lang.IllegalArgumentException

@ExperimentalUnsignedTypes
open class GenericMidiMapper: MidiMapper<UByteSerializable, MBGenericMidiMessage> {
    override fun lift(message: UByteSerializable): MBGenericMidiMessage =
        lift(message.bytes())

    fun lift(message: UByteArray): MBGenericMidiMessage {
        val baseCommand = message[0] and 0xF0u
        val possiblyChannel = (message[0] and 0x0Fu).toInt()

        val decoded: MBGenericMidiMessage = if (baseCommand < 0xF0u) when (baseCommand.toUInt()) {
            0x80u -> MBGenericMidiMessage.ChannelEvent.NoteOn(MidiChannel.values()[possiblyChannel], message[1].toInt(), message[2].toInt())
            0x90u -> MBGenericMidiMessage.ChannelEvent.NoteOff(MidiChannel.values()[possiblyChannel], message[1].toInt(), message[2].toInt())
            0xA0u -> MBGenericMidiMessage.ChannelEvent.PolyphonicKeyPressure(
                MidiChannel.values()[possiblyChannel],
                message[1].toInt(),
                message[2].toInt()
            )
            0xB0u -> when (message[1].toInt()) {
                120 -> MBGenericMidiMessage.ChannelEvent.ControlChange.AllSoundsOff(MidiChannel.values()[possiblyChannel])
                121 -> MBGenericMidiMessage.ChannelEvent.ControlChange.ResetAllControllers(
                    MidiChannel.values()[possiblyChannel],
                    message[2].toInt()
                )
                122 -> when (message[2].toInt()) {
                    0 -> MBGenericMidiMessage.ChannelEvent.ControlChange.LocalControlOff(MidiChannel.values()[possiblyChannel])
                    127 -> MBGenericMidiMessage.ChannelEvent.ControlChange.LocalControlOn(MidiChannel.values()[possiblyChannel])
                    else -> throw IllegalArgumentException()
                }
                123 -> MBGenericMidiMessage.ChannelEvent.ControlChange.AllNotesOff(MidiChannel.values()[possiblyChannel])
                124 -> MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOff(MidiChannel.values()[possiblyChannel])
                125 -> MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOn(MidiChannel.values()[possiblyChannel])
                126 -> MBGenericMidiMessage.ChannelEvent.ControlChange.MonoModeOn(MidiChannel.values()[possiblyChannel], message[2].toInt())
                127 -> MBGenericMidiMessage.ChannelEvent.ControlChange.PolyModeOn(MidiChannel.values()[possiblyChannel])
                else -> MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(
                    MidiChannel.values()[possiblyChannel],
                    message[1].toInt(),
                    message[2].toInt()
                )
            }
            0xC0u -> MBGenericMidiMessage.ChannelEvent.ProgramChange(MidiChannel.values()[possiblyChannel], message[1].toInt())
            0xD0u -> MBGenericMidiMessage.ChannelEvent.ChannelPressure(MidiChannel.values()[possiblyChannel], message[0].toInt())
            0xE0u -> MBGenericMidiMessage.ChannelEvent.PitchBend(
                MidiChannel.values()[possiblyChannel],
                message[0].toInt() + message[1].toInt() * 0x80 - 0x2000
            )
            else -> throw IllegalArgumentException("Cannot decode $baseCommand")
        } else when (message[0].toUInt()) {
            0xF0u -> liftSysex(message)
            0xF1u -> MBGenericMidiMessage.SystemCommon.TimeCode(
                (message[1] / 0x10u).toInt(),
                (message[1] and 0x0Fu).toInt()
            )
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
        val manufacturer = if (message[1] != 0x00u.toUByte()) ManufacturerId.Short(message[1]) else ManufacturerId.Triplet(
            message[2],
            message[3]
        )

        return when(manufacturer) {
            ManufacturerId.UNIVERSAL_REALTIME ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(
                    DeviceId.Short(message[2]),
                    message.toList().drop(3).dropLast(1).toUByteArray()
                )
            ManufacturerId.UNIVERSAL_NON_REALTIME ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime(
                    DeviceId.Short(message[2]),
                    message.toList().drop(3).dropLast(1).toUByteArray()
                )
            else ->
                MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific(
                    manufacturer,
                    message.toList().drop(1 + offset).dropLast(1).toUByteArray()
                )
        }
    }
}