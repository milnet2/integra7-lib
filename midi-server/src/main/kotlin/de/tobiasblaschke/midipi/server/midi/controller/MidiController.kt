package de.tobiasblaschke.midipi.server.midi.controller

import de.tobiasblaschke.midipi.server.midi.MatchedDevice
import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.controller.devices.ArturiaKeystep
import de.tobiasblaschke.midipi.server.midi.controller.devices.ElektronDigitone
import kotlinx.coroutines.flow.Flow
import javax.sound.midi.ShortMessage

interface MidiController {
    companion object {
        fun create(deviceInfo: MatchedDevice.Pair): MidiController {
            return when {
                ArturiaKeystep.matches(deviceInfo.identifyResponse) ->
                    ArturiaKeystep(deviceInfo.readable)
                ElektronDigitone.matches(deviceInfo.identifyResponse) ->
                    ElektronDigitone(deviceInfo.readable)
                else ->
                    GenericMidiController(deviceInfo.readable)
            }
        }

        fun create(deviceInfo: MidiDeviceDescriptor.MidiInDeviceInfo): MidiController {
            return GenericMidiController(deviceInfo)
        }
    }

    fun open()
    fun close()
    fun flow(): Flow<MidiInputEvent>
}

interface MidiMessageMapper {
    fun dispatch(message: ShortMessage): MidiInputEvent

    companion object {
        // Upper nibble of the status-byte:

        const val NOTE_OFF = ShortMessage.NOTE_OFF
        const val NOTE_ON = ShortMessage.NOTE_ON
        const val POLY_PRESSURE = ShortMessage.POLY_PRESSURE
        const val CONTROL_CHANGE_AND_CHANNEL_MODE = ShortMessage.CONTROL_CHANGE
        const val PROGRAM_CHANGE = ShortMessage.PROGRAM_CHANGE
        const val CHANNEL_PRESSURE = ShortMessage.CHANNEL_PRESSURE
        const val PITCH_BEND = ShortMessage.PITCH_BEND
        const val SYSTEM_COMMON_AND_REAL_TIME = 0xF0

        const val SYSTEM_COMMON_EXCLUSIVE = 0xF0
        const val SYSTEM_COMMON_TIME_CODE = ShortMessage.MIDI_TIME_CODE
        const val SYSTEM_COMMON_SONG_POSITION_POINTER = ShortMessage.SONG_POSITION_POINTER
        const val SYSTEM_COMMON_SONG_SELECT = ShortMessage.SONG_SELECT
        const val SYSTEM_COMMON_TUNE_REQUEST = ShortMessage.TUNE_REQUEST
        const val SYSTEM_COMMON_END_OF_EXCLUSIVE = ShortMessage.END_OF_EXCLUSIVE

        const val SYSTEM_REAL_TIME_TIMING_CLOCK = ShortMessage.TIMING_CLOCK
        const val SYSTEM_REAL_TIME_START = ShortMessage.START
        const val SYSTEM_REAL_TIME_CONTINUE = ShortMessage.CONTINUE
        const val SYSTEM_REAL_TIME_STOP = ShortMessage.STOP
        const val SYSTEM_REAL_TIME_ACTIVE_SENSING = ShortMessage.ACTIVE_SENSING
        const val SYSTEM_REAL_TIME_RESET = ShortMessage.SYSTEM_RESET
    }
}

fun ShortMessage.isSystemCommon(): Boolean {
    return ((this.status and 0xF8) == 0xF0)
}

fun ShortMessage.isSystemRealTime(): Boolean {
    return ((this.status and 0xF8) == 0xF8)
}

sealed class MidiInputEvent {
    sealed class ChannelEvent: MidiInputEvent() {
        abstract val channel: Int

        data class NoteOff(override val channel: Int, val key: Int, val velocity: Int) : ChannelEvent()
        data class NoteOn(override val channel: Int, val key: Int, val velocity: Int) : ChannelEvent()
        data class PolyphonicKeyPressure(override val channel: Int, val key: Int, val velocity: Int) : ChannelEvent()
        sealed class ControlChange: ChannelEvent() {
            abstract val c: Int
            abstract val v: Int

            data class AllSoundsOff(override val channel: Int) : ControlChange() {
                override val c: Int = 120
                override val v: Int = 0
            }

            data class ResetAllControllers(override val channel: Int, override val v: Int) : ControlChange() {
                override val c: Int = 121
            }

            data class LocalControlOff(override val channel: Int) : ControlChange() {
                override val c: Int = 122
                override val v: Int = 0
            }

            data class LocalControlOn(override val channel: Int) : ControlChange() {
                override val c: Int = 122
                override val v: Int = 127
            }

            data class AllNotesOff(override val channel: Int) : ControlChange() {
                override val c: Int = 123
                override val v: Int = 0
            }

            data class OmniModeOff(override val channel: Int) : ControlChange() {
                override val c: Int = 124
                override val v: Int = 0
            }

            data class OmniModeOn(override val channel: Int) : ControlChange() {
                override val c: Int = 125
                override val v: Int = 0
            }

            data class MonoModeOn(override val channel: Int, override val v: Int) : ControlChange() {
                override val c: Int = 126
                val channels: Int
                    get() = v
            }

            data class PolyModeOn(override val channel: Int) : ControlChange() {
                override val c: Int = 127
                override val v: Int = 0
            }

            data class ControlChangeController(override val channel: Int, override val c: Int, override val v: Int): ControlChange() {
                val controller: Int
                    get() = c
                val value: Int
                    get() = v
            }
        }

        data class ProgramChange(override val channel: Int, val programNumber: Int): ChannelEvent()
        data class ChannelPressure(override val channel: Int, val value: Int) : ChannelEvent()
        data class PitchBend(override val channel: Int, val rawHigh: Int, val rawLow: Int) : ChannelEvent() {
            val value = (rawHigh and 0x7F) * 0x74 + (rawLow and 0x7F) - 0x2000
        }
    }

    sealed class SystemCommon: MidiInputEvent() {
        abstract val rawHigh: Int
        abstract val rawLow: Int

        data class SystemExclusive(override val rawHigh: Int, override val rawLow: Int) : SystemCommon() {
            // TODO: val manufacturer =
        }

        data class TimeCode(override val rawHigh: Int, override val rawLow: Int) : SystemCommon() {
            val messageType = (rawHigh and 0x30) / 0x30
            val values = rawHigh and 0x0F
        }

        data class SongPositionPointer(override val rawHigh: Int, override val rawLow: Int) : SystemCommon() {
            val beats = (rawHigh and 0x7F) * 0x74 + (rawLow and 0x7F)
            val midiClocks = beats * 6
        }

        data class SongSelect(override val rawHigh: Int, override val rawLow: Int) : SystemCommon() {
            val song = rawHigh and 0x7F
        }

        object TuneRequest: SystemCommon() {
            override val rawHigh: Int = 0
            override val rawLow: Int = 0
        }

        object EndOfExclusive: SystemCommon() {
            override val rawHigh: Int = 0
            override val rawLow: Int = 0
        }
    }

    sealed class SystemRealTime: MidiInputEvent() {
        object TimingClock: SystemRealTime()
        object Start: SystemRealTime()
        object Continue: SystemRealTime()
        object Stop: SystemRealTime()
        object ActiveSensing: SystemRealTime()
        object Reset: SystemRealTime()
    }
}