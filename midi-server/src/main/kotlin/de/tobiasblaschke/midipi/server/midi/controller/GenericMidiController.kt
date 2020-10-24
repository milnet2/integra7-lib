package de.tobiasblaschke.midipi.server.midi.controller

import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.CHANNEL_PRESSURE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.CONTROL_CHANGE_AND_CHANNEL_MODE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.NOTE_OFF
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.NOTE_ON
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.PITCH_BEND
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.POLY_PRESSURE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.PROGRAM_CHANGE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_AND_REAL_TIME
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_END_OF_EXCLUSIVE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_EXCLUSIVE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_SONG_POSITION_POINTER
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_SONG_SELECT
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_TIME_CODE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_COMMON_TUNE_REQUEST
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_ACTIVE_SENSING
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_CONTINUE
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_RESET
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_START
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_STOP
import de.tobiasblaschke.midipi.server.midi.controller.MidiMessageMapper.Companion.SYSTEM_REAL_TIME_TIMING_CLOCK
import de.tobiasblaschke.midipi.server.midi.toHexString
import kotlinx.coroutines.channels.Channel
import java.lang.IllegalArgumentException
import javax.sound.midi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow


open class GenericMidiController(
    deviceInfo: MidiDeviceDescriptor.MidiInDeviceInfo,
    private val messageMapper: MidiMessageMapper = GenericMessageMapper()
): MidiController, Receiver {
    private val device: MidiDevice
    private val transmitter: Transmitter
    private val output = Channel<MidiInputEvent>()
    private val sysexResponses = Channel<SystemExclusiveMessage>()

    init {
        try {
            device = deviceInfo.device()
            transmitter = device.transmitter
            transmitter.receiver = this
        } catch (e: MidiUnavailableException) {
            throw RuntimeException("Error fetching device $deviceInfo", e)
        }
    }

    override fun open() {
        if (!device.isOpen) {
            device.open()
        }
    }

    override fun send(message: MidiMessage?, timeStamp: Long) {
        try {
            when(message) {
                null -> println("Unexpected empty message")
                is ShortMessage -> triggerInputEvent(messageMapper.dispatch(message))
                is SysexMessage -> {
                    println("Trying to decode SysEx ${message.data.toUByteArray().toHexString()}")
                    val decoded = messageMapper.dispatch(message)
                    if (decoded != null) {
                        println("Decoded $decoded")
                        triggerInputEvent(decoded)
                    } else {
                        println("Undecodable SysEx message")
                    }
                }
                else -> println("Unsupported message-type ${message.javaClass.simpleName}")
            }
        } catch (ex: Exception) {
            ex.printStackTrace();
        }
    }

    private fun triggerInputEvent(message: MidiInputEvent) {
        GlobalScope.launch {
            output.send(message)
        }
    }

    private fun triggerInputEvent(message: SystemExclusiveMessage) {
        GlobalScope.launch {
            sysexResponses.send(message)
        }
    }

    override fun flow(): Flow<MidiInputEvent> =
        output.consumeAsFlow()

    override fun sysexFlow(): Flow<SystemExclusiveMessage> =
        sysexResponses.consumeAsFlow()

    override fun close() {
        output.close()
        device.close()
    }

    open class GenericMessageMapper: MidiMessageMapper {
        override fun dispatch(message: ShortMessage): MidiInputEvent =
            // https://www.midi.org/specifications-old/item/table-1-summary-of-midi-message
            when(message.command) {
                NOTE_ON -> MidiInputEvent.ChannelEvent.NoteOn(message.channel, message.data1, message.data2)
                NOTE_OFF -> MidiInputEvent.ChannelEvent.NoteOff(message.channel, message.data1, message.data2)
                POLY_PRESSURE -> MidiInputEvent.ChannelEvent.PolyphonicKeyPressure(message.channel, message.data1, message.data2)
                CONTROL_CHANGE_AND_CHANNEL_MODE -> when(message.data1) {
                    120 -> MidiInputEvent.ChannelEvent.ControlChange.AllSoundsOff(message.channel)
                    121 -> MidiInputEvent.ChannelEvent.ControlChange.ResetAllControllers(message.channel, message.data2)
                    122 -> when(message.data2) {
                        0 -> MidiInputEvent.ChannelEvent.ControlChange.LocalControlOff(message.channel)
                        127 -> MidiInputEvent.ChannelEvent.ControlChange.LocalControlOn(message.channel)
                        else -> throw IllegalArgumentException()
                    }
                    123 -> MidiInputEvent.ChannelEvent.ControlChange.AllNotesOff(message.channel)
                    124 -> MidiInputEvent.ChannelEvent.ControlChange.OmniModeOff(message.channel)
                    125 -> MidiInputEvent.ChannelEvent.ControlChange.OmniModeOn(message.channel)
                    126 -> MidiInputEvent.ChannelEvent.ControlChange.MonoModeOn(message.channel, message.data2)
                    127 -> MidiInputEvent.ChannelEvent.ControlChange.PolyModeOn(message.channel)
                    else -> MidiInputEvent.ChannelEvent.ControlChange.ControlChangeController(message.channel, message.data1, message.data2)
                }
                PROGRAM_CHANGE -> MidiInputEvent.ChannelEvent.ProgramChange(message.channel, message.data1)
                CHANNEL_PRESSURE -> MidiInputEvent.ChannelEvent.ChannelPressure(message.channel, message.data1)
                PITCH_BEND -> MidiInputEvent.ChannelEvent.PitchBend(message.channel, message.data1, message.data2)
                SYSTEM_COMMON_AND_REAL_TIME ->
                    when(message.status) {
                        SYSTEM_COMMON_EXCLUSIVE -> MidiInputEvent.SystemCommon.SystemExclusive(message.data1, message.data2)
                        SYSTEM_COMMON_TIME_CODE -> MidiInputEvent.SystemCommon.TimeCode(message.data1, message.data2)
                        SYSTEM_COMMON_SONG_POSITION_POINTER -> MidiInputEvent.SystemCommon.SongPositionPointer(message.data1, message.data2)
                        SYSTEM_COMMON_SONG_SELECT -> MidiInputEvent.SystemCommon.SongSelect(message.data1, message.data2)
                        SYSTEM_COMMON_TUNE_REQUEST -> MidiInputEvent.SystemCommon.TuneRequest
                        SYSTEM_COMMON_END_OF_EXCLUSIVE -> MidiInputEvent.SystemCommon.EndOfExclusive

                        SYSTEM_REAL_TIME_TIMING_CLOCK -> MidiInputEvent.SystemRealTime.TimingClock
                        SYSTEM_REAL_TIME_START -> MidiInputEvent.SystemRealTime.Start
                        SYSTEM_REAL_TIME_CONTINUE -> MidiInputEvent.SystemRealTime.Continue
                        SYSTEM_REAL_TIME_STOP -> MidiInputEvent.SystemRealTime.Stop
                        SYSTEM_REAL_TIME_ACTIVE_SENSING -> MidiInputEvent.SystemRealTime.ActiveSensing
                        SYSTEM_REAL_TIME_RESET -> MidiInputEvent.SystemRealTime.Reset
                        else ->
                            throw IllegalArgumentException("Neither system-common nor real-time")
                    }
                else ->
                    TODO("Not yet implemented")
            }

        override fun dispatch(message: SysexMessage): SystemExclusiveMessage? =
            null
    }
}