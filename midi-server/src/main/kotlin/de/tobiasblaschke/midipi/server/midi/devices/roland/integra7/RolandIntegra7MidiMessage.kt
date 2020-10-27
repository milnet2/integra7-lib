package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBGenericMidiMessage
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBMidiMessage
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.ManufacturerId
import de.tobiasblaschke.midipi.server.midi.controller.devices.integra7.lsb
import de.tobiasblaschke.midipi.server.midi.controller.devices.integra7.msb

sealed class RolandIntegra7MidiMessage: MBMidiMessage {
    companion object {
        val ROLAND = ManufacturerId.Short(0x41u)
        val INTEGRA7 = ubyteArrayOf(0x00u, 0x00u, 0x64u)
    }

    // -------------------------------------------------------------
    // Supported generic messages
    // -------------------------------------------------------------

    class NoteOff(delegate: MBMidiMessage.NoteOff): RolandIntegra7MidiMessage(), MBMidiMessage.NoteOff by delegate
    class NoteOn(delegate: MBMidiMessage.NoteOn): RolandIntegra7MidiMessage(), MBMidiMessage.NoteOn by delegate
    class PolyphonicKeyPressure(delegate: MBMidiMessage.PolyphonicKeyPressure): RolandIntegra7MidiMessage(), MBMidiMessage.PolyphonicKeyPressure by delegate
    class AllSoundsOff(delegate: MBMidiMessage.AllSoundsOff): RolandIntegra7MidiMessage(), MBMidiMessage.AllSoundsOff by delegate
    class ResetAllControllers(delegate: MBMidiMessage.ResetAllControllers): RolandIntegra7MidiMessage(), MBMidiMessage.ResetAllControllers by delegate
//  Not supported: class LocalControlOff(delegate: MBMidiMessage.LocalControlOff): RolandIntegra7MidiMessage(), MBMidiMessage.LocalControlOff by delegate
//  Not supported: class LocalControlOn(delegate: MBMidiMessage.LocalControlOn): RolandIntegra7MidiMessage(), MBMidiMessage.LocalControlOn by delegate
    class AllNotesOff(delegate: MBMidiMessage.AllNotesOff): RolandIntegra7MidiMessage(), MBMidiMessage.AllNotesOff by delegate
    class OmniModeOff(delegate: MBMidiMessage.OmniModeOff): RolandIntegra7MidiMessage(), MBMidiMessage.OmniModeOff by delegate
    class OmniModeOn(delegate: MBMidiMessage.OmniModeOn): RolandIntegra7MidiMessage(), MBMidiMessage.OmniModeOn by delegate
    class MonoModeOn(delegate: MBMidiMessage.MonoModeOn): RolandIntegra7MidiMessage(), MBMidiMessage.MonoModeOn by delegate
    class PolyModeOn(delegate: MBMidiMessage.PolyModeOn): RolandIntegra7MidiMessage(), MBMidiMessage.PolyModeOn by delegate
//  Not supported: class ControlChangeController(delegate: MBMidiMessage.ControlChangeController): RolandIntegra7MidiMessage(), MBMidiMessage.ControlChangeController by delegate
    class ProgramChange(delegate: MBMidiMessage.ProgramChange): RolandIntegra7MidiMessage(), MBMidiMessage.ProgramChange by delegate
    class ChannelPressure(delegate: MBMidiMessage.ChannelPressure): RolandIntegra7MidiMessage(), MBMidiMessage.ChannelPressure by delegate
    class PitchBend(delegate: MBMidiMessage.PitchBend): RolandIntegra7MidiMessage(), MBMidiMessage.PitchBend by delegate
//  Not supported: class TimeCode(delegate: MBMidiMessage.TimeCode): RolandIntegra7MidiMessage(), MBMidiMessage.TimeCode by delegate
//  Not supported: class SongPositionPointer(delegate: MBMidiMessage.SongPositionPointer): RolandIntegra7MidiMessage(), MBMidiMessage.SongPositionPointer by delegate
//  Not supported: class SongSelect(delegate: MBMidiMessage.SongSelect): RolandIntegra7MidiMessage(), MBMidiMessage.SongSelect by delegate
//  Not supported: class TuneRequest(delegate: MBMidiMessage.TuneRequest): RolandIntegra7MidiMessage(), MBMidiMessage.TuneRequest by delegate
    class TimingClock(delegate: MBMidiMessage.TimingClock): RolandIntegra7MidiMessage(), MBMidiMessage.TimingClock by delegate
//  Not supported: class Start(delegate: MBMidiMessage.Start): RolandIntegra7MidiMessage(), MBMidiMessage.Start by delegate
//  Not supported: class Continue(delegate: MBMidiMessage.Continue): RolandIntegra7MidiMessage(), MBMidiMessage.Continue by delegate
//  Not supported: class Stop(delegate: MBMidiMessage.Stop): RolandIntegra7MidiMessage(), MBMidiMessage.Stop by delegate
    class ActiveSensing(delegate: MBMidiMessage.ActiveSensing): RolandIntegra7MidiMessage(), MBMidiMessage.ActiveSensing by delegate
//  Not supported: class Reset(delegate: MBMidiMessage.Reset): RolandIntegra7MidiMessage(), MBMidiMessage.Reset by delegate

    // -------------------------------------------------------------
    // Specific regular midi-messages
    // -------------------------------------------------------------
    data class BankSelectMsb(val channel: Int, val bankNumberMsb: Int): RolandIntegra7MidiMessage() {
        companion object {
            private val ALLOWED_MSB = setOf( 85, 89, 95, 88, 87, 121, 86, 120, 93, 92, 88, 97, 96, 120 )
        }
        init {
            assert(ALLOWED_MSB.contains(bankNumberMsb))
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0, bankNumberMsb)
        override fun bytes() = delegate.bytes()
    }
    data class BankSelectLsb(val channel: Int, val bankNumberLsb: Int): RolandIntegra7MidiMessage() {
        init {
            assert(bankNumberLsb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x20, bankNumberLsb)
        override fun bytes() = delegate.bytes()
    }
    data class Modulation(val channel: Int, val depth: Int): RolandIntegra7MidiMessage() {
        init {
            assert(depth in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x01, depth)
        override fun bytes() = delegate.bytes()
    }
    data class BreathType(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x02, value)
        override fun bytes() = delegate.bytes()
    }
    data class FootType(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x04, value)
        override fun bytes() = delegate.bytes()
    }
    data class PortamentoTime(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x05, value)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class DataEntryLsb(val channel: Int, val rpnNrpn: Int): RolandIntegra7MidiMessage() {
        init {
            assert(rpnNrpn in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x26, rpnNrpn)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class DataEntryMsb(val channel: Int, val rpnNrpn: Int): RolandIntegra7MidiMessage() {
        init {
            assert(rpnNrpn in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x06, rpnNrpn)
        override fun bytes() = delegate.bytes()
    }
    data class Volume(val channel: Int, val volume: Int): RolandIntegra7MidiMessage() {
        init {
            assert(volume in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x07, volume)
        override fun bytes() = delegate.bytes()
    }
    data class Panpot(val channel: Int, val leftRight: Int): RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0A, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Expression(val channel: Int, val expression: Int): RolandIntegra7MidiMessage() {
        init {
            assert(expression in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0B, expression)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl1(val channel: Int, val leftRight: Int): RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0C, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl2(val channel: Int, val backFront: Int): RolandIntegra7MidiMessage() {
        init {
            assert(backFront in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0D, backFront + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundControl3(val channel: Int, val ambience: Int): RolandIntegra7MidiMessage() {
        init {
            assert(ambience in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x0E, ambience)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify1(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x10, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify2(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x11, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify3(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x12, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneModify4(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x13, value)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart1(val channel: Int, val leftRight: Int): RolandIntegra7MidiMessage() {
        init {
            assert(leftRight in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1C, leftRight + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart2(val channel: Int, val backFront: Int): RolandIntegra7MidiMessage() {
        init {
            assert(backFront in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1D, backFront + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class MotionalSurroundExternalPart3(val channel: Int, val ambience: Int): RolandIntegra7MidiMessage() {
        init {
            assert(ambience in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x1E, ambience)
        override fun bytes() = delegate.bytes()
    }
    data class Hold1(val channel: Int, val isOn: Boolean): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x40, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Portamento(val channel: Int, val isOn: Boolean): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x41, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Sostenuto(val channel: Int, val isOn: Boolean): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x42, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Soft(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x43, value)
        override fun bytes() = delegate.bytes()
    }
    data class LegatoFootSwitch(val channel: Int, val isOn: Boolean): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x44, if (isOn) 64 else 0)
        override fun bytes() = delegate.bytes()
    }
    data class Hold2(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x45, value)
        override fun bytes() = delegate.bytes()
    }
    data class Resonance(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x47, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class ReleaseTime(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x48, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class AttackTime(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x49, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Cutoff(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4A, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class Decay(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4B, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoRate(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4C, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoDepth(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4D, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class VibratoDelay(val channel: Int, val offset: Int): RolandIntegra7MidiMessage() {
        init {
            assert(offset in -64 .. 63)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x4E, offset + 0x40)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation1(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x50, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation2(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x51, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation3(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x52, value)
        override fun bytes() = delegate.bytes()
    }
    data class ToneVariation4(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x53, value)
        override fun bytes() = delegate.bytes()
    }
    data class PortamentoControl(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x54, value)
        override fun bytes() = delegate.bytes()
    }
    data class ReverbSend(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x5B, value)
        override fun bytes() = delegate.bytes()
    }
    data class ChorusSend(val channel: Int, val value: Int): RolandIntegra7MidiMessage() {
        init {
            assert(value in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x5D, value)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class RpnMsb(val channel: Int, val msb: Int): RolandIntegra7MidiMessage() {
        init {
            assert(msb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x65, msb)
        override fun bytes() = delegate.bytes()
    }
    @Deprecated(message = "Use via RolandIntegra7RpnMessage")
    data class RpnLsb(val channel: Int, val lsb: Int): RolandIntegra7MidiMessage() {
        init {
            assert(lsb in 0 .. 127)
        }
        private val delegate = MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController(channel, 0x64, lsb)
        override fun bytes() = delegate.bytes()
    }

    // -------------------------------------------------------------
    // Specific sysEx-messages
    // -------------------------------------------------------------
    data class IdentityRequest(val deviceId: DeviceId = DeviceId.ALL_DEVICES): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime(deviceId,
            payload = ubyteArrayOf(0x6u, 0x1u))
        override fun bytes() = delegate.bytes()
    }

    // TODO: Bad location, the rest are requests...
    data class IdentityReply(val deviceId: DeviceId, val manufacturerId: ManufacturerId, val familyCode: UShort, val familyNumberCode: UShort, val softwareRev: UInt): RolandIntegra7MidiMessage() {
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

    data class MasterVolume(val level: Int): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime(
            deviceId = DeviceId.ALL_DEVICES,
            payload = ubyteArrayOf(
                0x4u, 0x1u,
                (level.toUInt() and 0xFFu).toUByte(),((level.toUInt() / 0x100u) and 0xFFu).toUByte()))
        override fun bytes() = delegate.bytes()
    }

    @Deprecated(message = "Not properly implemented")
    data class MasterFineTuning(val cent: Float): RolandIntegra7MidiMessage() {
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

    data class MasterCoarseTuning(val semitones: Int): RolandIntegra7MidiMessage() {
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
     * @param payload starting with COMMAND, excluding EOX
     */
    internal data class IntegraSysExRequest(val deviceId: DeviceId, val payload: UByteArray): RolandIntegra7MidiMessage() {
        private val delegate = MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific(
            manufacturer = ROLAND,
            payload = deviceId.bytes() + INTEGRA7 + payload)
        override fun bytes() = delegate.bytes()
    }
}

sealed class RolandIntegra7RpnMessage {
    abstract val messages: List<RolandIntegra7MidiMessage>
    data class PitchBendSensitivity(val channel: Int, val semitones: Int): RolandIntegra7RpnMessage() {
        init {
            assert(semitones in 0 .. 24)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 0),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, semitones),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    @Deprecated(message = "Not properly implemented")
    data class ChannelFineTuning(val channel: Int, val cent: Int): RolandIntegra7RpnMessage() {
        init {
            TODO("Map the values")
            assert(cent in -4096 * 100 / 8192 ..  +4096 * 100 / 8192)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 1),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, cent),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    data class ChannelCoarseTuning(val channel: Int, val semitones: Int): RolandIntegra7RpnMessage() {
        init {
            assert(semitones in -48 .. 48)
        }
        @Suppress("DEPRECATION")
        override val messages = listOf(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0),
            RolandIntegra7MidiMessage.RpnLsb(channel, 2),
            RolandIntegra7MidiMessage.DataEntryMsb(channel, semitones + 0x40),
            RolandIntegra7MidiMessage.DataEntryLsb(channel, 0)
        ) + RpnNull(channel).messages
    }
    data class RpnNull(val channel: Int): RolandIntegra7RpnMessage() {
        @Suppress("DEPRECATION")
        override val messages = listOf(
            RolandIntegra7MidiMessage.RpnMsb(channel, 0x7F),
            RolandIntegra7MidiMessage.RpnLsb(channel, 0x7F)
        )
    }
}

