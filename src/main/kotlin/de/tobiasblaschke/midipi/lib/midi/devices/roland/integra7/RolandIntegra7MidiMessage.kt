package de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.lib.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.lib.midi.bearable.domain.MidiChannel
import de.tobiasblaschke.midipi.lib.midi.bearable.lifted.*
import de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7.memory.Integra7Address
import de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7.memory.Integra7Size
import de.tobiasblaschke.midipi.lib.utils.SparseUByteArray
import de.tobiasblaschke.midipi.lib.utils.toUInt7
import java.lang.Exception
import java.time.Duration

@OptIn(ExperimentalUnsignedTypes::class)
sealed class RolandIntegra7MidiMessage: UByteSerializable {
    companion object {
        val ROLAND = ManufacturerId.Short(0x41u)
        val INTEGRA7 = ubyteArrayOf(0x00u, 0x00u, 0x64u)
    }

    // -------------------------------------------------------------
    // Supported generic messages
    // -------------------------------------------------------------

    class NoteOff(delegate: MBUnidirectionalMidiMessage.NoteOff): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.NoteOff by delegate {
        constructor(channel: MidiChannel, key: Int, velocity: Int): this(MBGenericMidiMessage.ChannelEvent.NoteOff(channel, key, velocity))
    }
    class NoteOn(delegate: MBUnidirectionalMidiMessage.NoteOn): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.NoteOn by delegate {
        constructor(channel: MidiChannel, key: Int, velocity: Int): this(MBGenericMidiMessage.ChannelEvent.NoteOn(channel, key, velocity))
    }
    class PolyphonicKeyPressure(delegate: MBUnidirectionalMidiMessage.PolyphonicKeyPressure): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.PolyphonicKeyPressure by delegate {
        constructor(channel: MidiChannel, key: Int, velocity: Int): this(MBGenericMidiMessage.ChannelEvent.PolyphonicKeyPressure(channel, key, velocity))
    }
    class AllSoundsOff(delegate: MBUnidirectionalMidiMessage.AllSoundsOff): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.AllSoundsOff by delegate {
        constructor(channel: MidiChannel): this(MBGenericMidiMessage.ChannelEvent.ControlChange.AllSoundsOff(channel))
    }
    class ResetAllControllers(delegate: MBUnidirectionalMidiMessage.ResetAllControllers): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.ResetAllControllers by delegate {
        constructor(channel: MidiChannel, value: Int = 0): this(MBGenericMidiMessage.ChannelEvent.ControlChange.ResetAllControllers(channel, value))
    }
//  Not supported: class LocalControlOff(delegate: MBMidiMessage.LocalControlOff): RolandIntegra7MidiMessage(), MBMidiMessage.LocalControlOff by delegate
//  Not supported: class LocalControlOn(delegate: MBMidiMessage.LocalControlOn): RolandIntegra7MidiMessage(), MBMidiMessage.LocalControlOn by delegate
    class AllNotesOff(delegate: MBUnidirectionalMidiMessage.AllNotesOff): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.AllNotesOff by delegate {
        constructor(channel: MidiChannel): this(MBGenericMidiMessage.ChannelEvent.ControlChange.AllNotesOff(channel))
    }
    class OmniModeOff(delegate: MBUnidirectionalMidiMessage.OmniModeOff): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.OmniModeOff by delegate {
        constructor(channel: MidiChannel): this(MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOff(channel))
    }
    class OmniModeOn(delegate: MBUnidirectionalMidiMessage.OmniModeOn): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.OmniModeOn by delegate {
        constructor(channel: MidiChannel): this(MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOn(channel))
    }
    class MonoModeOn(delegate: MBUnidirectionalMidiMessage.MonoModeOn): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.MonoModeOn by delegate {
        constructor(channel: MidiChannel, channelCount: Int): this(MBGenericMidiMessage.ChannelEvent.ControlChange.MonoModeOn(channel, channelCount))
    }
    class PolyModeOn(delegate: MBUnidirectionalMidiMessage.PolyModeOn): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.PolyModeOn by delegate
//  Not supported: class ControlChangeController(delegate: MBMidiMessage.ControlChangeController): RolandIntegra7MidiMessage(), MBMidiMessage.ControlChangeController by delegate
    class ProgramChange(delegate: MBUnidirectionalMidiMessage.ProgramChange): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.ProgramChange by delegate {
        constructor(channel: MidiChannel, programNumber: Int): this(MBGenericMidiMessage.ChannelEvent.ProgramChange(channel, programNumber))
    }
    class ChannelPressure(delegate: MBUnidirectionalMidiMessage.ChannelPressure): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.ChannelPressure by delegate {
        constructor(channel: MidiChannel, value: Int): this(MBGenericMidiMessage.ChannelEvent.ChannelPressure(channel, value))
    }
    class PitchBend(delegate: MBUnidirectionalMidiMessage.PitchBend): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.PitchBend by delegate {
        constructor(channel: MidiChannel, value: Int): this(MBGenericMidiMessage.ChannelEvent.PitchBend(channel, value))
    }
//  Not supported: class TimeCode(delegate: MBMidiMessage.TimeCode): RolandIntegra7MidiMessage(), MBMidiMessage.TimeCode by delegate
//  Not supported: class SongPositionPointer(delegate: MBMidiMessage.SongPositionPointer): RolandIntegra7MidiMessage(), MBMidiMessage.SongPositionPointer by delegate
//  Not supported: class SongSelect(delegate: MBMidiMessage.SongSelect): RolandIntegra7MidiMessage(), MBMidiMessage.SongSelect by delegate
//  Not supported: class TuneRequest(delegate: MBMidiMessage.TuneRequest): RolandIntegra7MidiMessage(), MBMidiMessage.TuneRequest by delegate
    class TimingClock(delegate: MBUnidirectionalMidiMessage.TimingClock): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.TimingClock by delegate {
        constructor(): this(de.tobiasblaschke.midipi.lib.midi.bearable.lifted.MBGenericMidiMessage.SystemRealTime.TimingClock)
    }
//  Not supported: class Start(delegate: MBMidiMessage.Start): RolandIntegra7MidiMessage(), MBMidiMessage.Start by delegate
//  Not supported: class Continue(delegate: MBMidiMessage.Continue): RolandIntegra7MidiMessage(), MBMidiMessage.Continue by delegate
//  Not supported: class Stop(delegate: MBMidiMessage.Stop): RolandIntegra7MidiMessage(), MBMidiMessage.Stop by delegate
    class ActiveSensing(delegate: MBUnidirectionalMidiMessage.ActiveSensing): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage(), MBUnidirectionalMidiMessage.ActiveSensing by delegate {
        constructor(): this(de.tobiasblaschke.midipi.lib.midi.bearable.lifted.MBGenericMidiMessage.SystemRealTime.ActiveSensing)
    }
//  Not supported: class Reset(delegate: MBMidiMessage.Reset): RolandIntegra7MidiMessage(), MBMidiMessage.Reset by delegate

    // -------------------------------------------------------------
    // Specific regular midi-messages
    // -------------------------------------------------------------
    data class BankSelectMsb(val channel: MidiChannel, val bankNumberMsb: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        companion object {
            private val ALLOWED_MSB = setOf( 85, 89, 95, 88, 87, 121, 86, 120, 93, 92, 88, 97, 96, 120 )
        }
        init {
            assert(ALLOWED_MSB.contains(bankNumberMsb))
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0, bankNumberMsb)
        override fun bytes() = delegate.bytes()
    }
    data class BankSelectLsb(val channel: MidiChannel, val bankNumberLsb: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(bankNumberLsb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x20, bankNumberLsb)
        override fun bytes() = delegate.bytes()
    }
    data class Modulation(val channel: MidiChannel, val depth: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(depth in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x01, depth)
        override fun bytes() = delegate.bytes()
    }
    data class BreathType(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x02, value)
        override fun bytes() = delegate.bytes()
    }
    data class FootType(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x04, value)
        override fun bytes() = delegate.bytes()
    }
    data class PortamentoTime(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x05, value)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class DataEntryLsb(val channel: MidiChannel, val rpnNrpn: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(rpnNrpn in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x26, rpnNrpn)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class DataEntryMsb(val channel: MidiChannel, val rpnNrpn: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(rpnNrpn in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x06, rpnNrpn)
        override fun bytes() = delegate.bytes()
    }
    data class Volume(val channel: MidiChannel, val volume: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(volume in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x07, volume)
        override fun bytes() = delegate.bytes()
    }
    data class Panpot(val channel: MidiChannel, val leftRight: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0A, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Expression(val channel: MidiChannel, val expression: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(expression in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0B, expression)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl1(val channel: MidiChannel, val leftRight: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0C, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl2(val channel: MidiChannel, val backFront: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(backFront in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0D, backFront + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl3(val channel: MidiChannel, val ambience: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(ambience in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0E, ambience)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify1(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x10, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify2(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x11, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify3(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x12, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify4(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x13, value)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart1(val channel: MidiChannel, val leftRight: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1C, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart2(val channel: MidiChannel, val backFront: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(backFront in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1D, backFront + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart3(val channel: MidiChannel, val ambience: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(ambience in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1E, ambience)
        override fun bytes() = delegate.bytes()
    }
    data class Hold1(val channel: MidiChannel, val isOn: Boolean): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x40, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Portamento(val channel: MidiChannel, val isOn: Boolean): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x41, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Sostenuto(val channel: MidiChannel, val isOn: Boolean): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x42, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Soft(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x43, value)
        override fun bytes() = delegate.bytes()
    }
    data class LegatoFootSwitch(val channel: MidiChannel, val isOn: Boolean): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x44, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Hold2(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x45, value)
        override fun bytes() = delegate.bytes()
    }
    data class Resonance(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x47, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class ReleaseTime(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x48, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class AttackTime(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x49, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Cutoff(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4A, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Decay(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4B, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoRate(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4C, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoDepth(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4D, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoDelay(val channel: MidiChannel, val offset: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4E, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation1(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x50, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation2(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x51, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation3(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x52, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation4(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x53, value)
        override fun bytes() = delegate.bytes()
    }
    data class PortamentoControl(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x54, value)
        override fun bytes() = delegate.bytes()
    }
    data class ReverbSend(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x5B, value)
        override fun bytes() = delegate.bytes()
    }
    data class ChorusSend(val channel: MidiChannel, val value: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x5D, value)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class RpnMsb(val channel: MidiChannel, val msb: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(msb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x65, msb)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class RpnLsb(val channel: MidiChannel, val lsb: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(lsb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x64, lsb)
        override fun bytes() = delegate.bytes()
    }

    // -------------------------------------------------------------
    // Specific sysEx-messages
    // -------------------------------------------------------------
    data class IdentityRequest(val deviceId: DeviceId = DeviceId.ALL_DEVICES): MBRequestResponseMidiMessage, RolandIntegra7MidiMessage() {
        override val completeAfter: Duration? = null
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime(deviceId,
            payload = ubyteArrayOf(0x6u, 0x1u))
        override fun bytes() = delegate.bytes()
        override fun isExpectingResponse(message: MBResponseMidiMessage): Boolean =
            message is IdentityReply
    }

    // TODO: Bad location, the rest are requests...
    data class IdentityReply(val deviceId: DeviceId, val manufacturerId: ManufacturerId, val familyCode: UShort, val familyNumberCode: UShort, val softwareRev: UInt): MBResponseMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime(deviceId,
            payload =
                ubyteArrayOf(0x6u, 0x2u) +
                manufacturerId.bytes() +
                familyCode.msb() + familyCode.lsb() +
                familyNumberCode.msb() + familyNumberCode.lsb() +
                softwareRev.toByteArrayMsbFirst())
        override fun bytes() = delegate.bytes()
        fun isIntegra7(): Boolean =
            (manufacturerId == ManufacturerId.Short(0x41u)) &&
            (familyCode == 0x6402u.toUShort()) &&
            (familyNumberCode == 0x0000u.toUShort()) &&
            (softwareRev == 0x00000000u)
        private fun UInt.toByteArrayMsbFirst(): UByteArray {
            val lsb = this.and(0xFFu).toUByte()
            val mlsb = (this / 0x100u).and(0xFFu).toUByte()
            val mmsb = (this / 0x10000u).and(0xFFu).toUByte()
            val msb = (this / 0x1000000u).toUByte()
            return ubyteArrayOf(msb, mmsb, mlsb, lsb)
        }
    }

    data class MasterVolume(val level: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(
            deviceId = DeviceId.ALL_DEVICES,
            payload = ubyteArrayOf(
                0x4u, 0x1u,
                (level.toUInt() and 0xFFu).toUByte(),((level.toUInt() / 0x100u) and 0xFFu).toUByte()))
        override fun bytes() = delegate.bytes()
    }

    @Deprecated(message = "Not properly implemented")
    data class MasterFineTuning(val cent: Float): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            TODO("Not properly implements")
            assert((cent >= -100) && (cent <= 99.9))
        }
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(
            deviceId = DeviceId.ALL_DEVICES,
            payload = ubyteArrayOf(
                0x4u, 0x3u,
                (cent.toUInt() and 0xFFu).toUByte(),((cent.toUInt() / 0x100u) and 0xFFu).toUByte()))
        override fun bytes() = delegate.bytes()
    }

    data class MasterCoarseTuning(val semitones: Int): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        init {
            assert(semitones in -24 .. 24)
        }
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(
            deviceId = DeviceId.ALL_DEVICES,
            payload = ubyteArrayOf(
                0x4u, 0x4u,
                0x00u,((semitones + 0x40).toUInt() and 0xFFu).toUByte()))
        override fun bytes() = delegate.bytes()
    }

    /**
     * @param checkSum starting with COMMAND, excluding EOX
     */
    internal data class IntegraSysExReadRequest(val deviceId: DeviceId, val address: Integra7Address, val size: Integra7Size, val checkSum: UByte): MBRequestResponseMidiMessage, RolandIntegra7MidiMessage() {
        override val completeAfter = Duration.ofMillis(100)
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific(
            manufacturer = ROLAND,
            payload = deviceId.bytes() + INTEGRA7 + 0x11u + address.bytes() + size.bytes() + checkSum)
        override fun bytes() = delegate.bytes()
        override fun isExpectingResponse(message: MBResponseMidiMessage): Boolean {
            // There may only be one message out in the wild at a time, so it's sufficient to test the type
            return message is IntegraSysExDataSet1Response
        }

        override fun isComplete(message: MBResponseMidiMessage): Boolean {
            val response = message as IntegraSysExDataSet1Response
            val expectedEndAddress = address.offsetBy(size - 1)
            val actualEndAddress = response.startAddress.offsetBy(response.payload.size.toUInt7())
            val complete = expectedEndAddress == actualEndAddress
//            println("  Got from start ${response.startAddress} to $actualEndAddress (size=${response.payload.size} Bytes) expecting a total of ${size.fullByteSize()} Bytes => complete = $complete")
            return complete
        }

        override fun merge(left: MBResponseMidiMessage, right: MBResponseMidiMessage): MBResponseMidiMessage {
            try {
                val l = left as IntegraSysExDataSet1Response
                val r = right as IntegraSysExDataSet1Response

                l.payload.putAll(r.payload)

                return IntegraSysExDataSet1Response(deviceId, l.startAddress, l.payload, 0x00u)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * @param payload starting with COMMAND, excluding EOX
     */
    internal data class IntegraSysExWriteRequest(val deviceId: DeviceId, val payload: UByteArray): MBUnidirectionalMidiMessage, RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific(
            manufacturer = ROLAND,
            payload = deviceId.bytes() + INTEGRA7 + payload)
        override fun bytes() = delegate.bytes()
    }

    /**
     * @param payload starting with COMMAND, excluding EOX
     */
    data class IntegraSysExDataSet1Response(val deviceId: DeviceId, val startAddress: Integra7Address, val payload: SparseUByteArray, val checkSum: UByte): MBResponseMidiMessage, RolandIntegra7MidiMessage() {
        init {
//            println("  ## Received $startAddress to ${Integra7Address.fromFullByteAddress(payload.size)}")
            assert(payload.all { it in 0x00u .. 0x7Fu }) { "Payload contains illegal value" }
        }
        override fun toString(): String =
            "IntegraSysExDataSet1Response()"

        override fun bytes(): UByteArray =
            ubyteArrayOf(0xF0u) + ROLAND.bytes() + payload.toUByteArray() + ubyteArrayOf(0xF7u)
    }
}

sealed class RolandIntegra7RpnMessage {
    abstract val messages: List<MBUnidirectionalMidiMessage>
    data class PitchBendSensitivity(val channel: MidiChannel, val semitones: Int): RolandIntegra7RpnMessage() {
        init {
            assert(semitones in 0 .. 24)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf<MBUnidirectionalMidiMessage>(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 0),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, semitones),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    @Deprecated(message = "Not properly implemented")
    data class ChannelFineTuning(val channel: MidiChannel, val cent: Int): RolandIntegra7RpnMessage() {
        init {
            TODO("Map the values")
            assert(cent in -4096 * 100 / 8192 ..  +4096 * 100 / 8192)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf<MBUnidirectionalMidiMessage>(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 1),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, cent),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    data class ChannelCoarseTuning(val channel: MidiChannel, val semitones: Int): RolandIntegra7RpnMessage() {
        init {
            assert(semitones in -48 .. 48)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf<MBUnidirectionalMidiMessage>(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 2),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, semitones + 0x40),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    data class RpnNull(val channel: MidiChannel): RolandIntegra7RpnMessage() {
        @Suppress("DEPRECATION")
        override val messages = listOf<MBUnidirectionalMidiMessage>(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0x7F),
            RolandIntegra7MidiMessage.RpnLsb(channel, 0x7F)
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun UShort.msb(): UByte =
    (this / 0x100u).toUByte()

@OptIn(ExperimentalUnsignedTypes::class)
fun UShort.lsb(): UByte =
    (this and 0xFFu).toUByte()