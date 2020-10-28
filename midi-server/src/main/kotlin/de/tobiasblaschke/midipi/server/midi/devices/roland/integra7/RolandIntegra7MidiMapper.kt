package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import de.tobiasblaschke.midipi.server.midi.bearable.UByteSerializable
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.*
import de.tobiasblaschke.midipi.server.midi.toHexString
import java.lang.IllegalArgumentException

class RolandIntegra7MidiMapper: MidiMapper<UByteSerializable, RolandIntegra7MidiMessage> {
    private val delegate = GenericMidiMapper()

    override fun lift(`in`: UByteSerializable): RolandIntegra7MidiMessage {
        val generic = delegate.lift(`in`)

        return when(generic) {
            is MBGenericMidiMessage.ChannelEvent.NoteOff ->
                RolandIntegra7MidiMessage.NoteOff(generic)
            is MBGenericMidiMessage.ChannelEvent.NoteOn ->
                RolandIntegra7MidiMessage.NoteOn(generic)
            is MBGenericMidiMessage.ChannelEvent.PolyphonicKeyPressure ->
                RolandIntegra7MidiMessage.PolyphonicKeyPressure(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.AllSoundsOff ->
                RolandIntegra7MidiMessage.AllSoundsOff(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.ResetAllControllers ->
                RolandIntegra7MidiMessage.ResetAllControllers(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.AllNotesOff ->
                RolandIntegra7MidiMessage.AllNotesOff(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOff ->
                RolandIntegra7MidiMessage.OmniModeOff(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.OmniModeOn ->
                RolandIntegra7MidiMessage.OmniModeOn(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.MonoModeOn ->
                RolandIntegra7MidiMessage.MonoModeOn(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.PolyModeOn ->
                RolandIntegra7MidiMessage.PolyModeOn(generic)
            is MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController ->
                lift(generic)
            is MBGenericMidiMessage.ChannelEvent.ProgramChange ->
                RolandIntegra7MidiMessage.ProgramChange(generic)
            is MBGenericMidiMessage.ChannelEvent.ChannelPressure ->
                RolandIntegra7MidiMessage.ChannelPressure(generic)
            is MBGenericMidiMessage.ChannelEvent.PitchBend ->
                RolandIntegra7MidiMessage.PitchBend(generic)
            is MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime ->
                lift(generic)
            is MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime ->
                lift(generic)
            is MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific ->
                lift(generic)
            MBGenericMidiMessage.SystemRealTime.TimingClock -> RolandIntegra7MidiMessage.TimingClock(generic as MBUnidirectionalMidiMessage.TimingClock)
            MBGenericMidiMessage.SystemRealTime.ActiveSensing -> RolandIntegra7MidiMessage.ActiveSensing(generic as MBUnidirectionalMidiMessage.ActiveSensing)
            else -> throw IllegalArgumentException("Unsupported MIDI-message $generic")
        }
    }

    private fun lift(message: MBGenericMidiMessage.ChannelEvent.ControlChange.ControlChangeController): RolandIntegra7MidiMessage {
        return when(message.controllerNumber) {
            0x00 -> RolandIntegra7MidiMessage.BankSelectMsb(message.channel, message.value)
            0x20 -> RolandIntegra7MidiMessage.BankSelectLsb(message.channel, message.value)
            0x01 -> RolandIntegra7MidiMessage.Modulation(message.channel, message.value)
            0x02 -> RolandIntegra7MidiMessage.BreathType(message.channel, message.value)
            0x04 -> RolandIntegra7MidiMessage.FootType(message.channel, message.value)
            0x05 -> RolandIntegra7MidiMessage.PortamentoTime(message.channel, message.value)
            0x06 -> RolandIntegra7MidiMessage.DataEntryMsb(message.channel, message.value)
            0x26 -> RolandIntegra7MidiMessage.DataEntryLsb(message.channel, message.value)
            0x07 -> RolandIntegra7MidiMessage.Volume(message.channel, message.value)
            0x0A -> RolandIntegra7MidiMessage.Panpot(message.channel, message.value - 0x40)
            0x0B -> RolandIntegra7MidiMessage.Expression(message.channel, message.value)
            0x0C -> RolandIntegra7MidiMessage.MotionalSurroundControl1(message.channel, message.value - 0x40)
            0x0D -> RolandIntegra7MidiMessage.MotionalSurroundControl2(message.channel, message.value - 0x40)
            0x0E -> RolandIntegra7MidiMessage.MotionalSurroundControl3(message.channel, message.value)
            0x10 -> RolandIntegra7MidiMessage.ToneModify1(message.channel, message.value)
            0x11 -> RolandIntegra7MidiMessage.ToneModify2(message.channel, message.value)
            0x12 -> RolandIntegra7MidiMessage.ToneModify3(message.channel, message.value)
            0x13 -> RolandIntegra7MidiMessage.ToneModify4(message.channel, message.value)
            0x1C -> RolandIntegra7MidiMessage.MotionalSurroundExternalPart1(message.channel, message.value - 0x40)
            0x1D -> RolandIntegra7MidiMessage.MotionalSurroundExternalPart2(message.channel, message.value - 0x40)
            0x1E -> RolandIntegra7MidiMessage.MotionalSurroundExternalPart3(message.channel, message.value)
            0x40 -> RolandIntegra7MidiMessage.Hold1(message.channel, message.value >= 64)
            0x41 -> RolandIntegra7MidiMessage.Portamento(message.channel, message.value >= 64)
            0x42 -> RolandIntegra7MidiMessage.Sostenuto(message.channel, message.value >= 64)
            0x43 -> RolandIntegra7MidiMessage.Soft(message.channel, message.value)
            0x44 -> RolandIntegra7MidiMessage.LegatoFootSwitch(message.channel, message.value >= 64)
            0x45 -> RolandIntegra7MidiMessage.Hold2(message.channel, message.value)
            0x47 -> RolandIntegra7MidiMessage.Resonance(message.channel, message.value - 0x40)
            0x48 -> RolandIntegra7MidiMessage.ReleaseTime(message.channel, message.value - 0x40)
            0x49 -> RolandIntegra7MidiMessage.AttackTime(message.channel, message.value - 0x40)
            0x4A -> RolandIntegra7MidiMessage.Cutoff(message.channel, message.value - 0x40)
            0x4B -> RolandIntegra7MidiMessage.Decay(message.channel, message.value - 0x40)
            0x4C -> RolandIntegra7MidiMessage.VibratoRate(message.channel, message.value - 0x40)
            0x4D -> RolandIntegra7MidiMessage.VibratoDepth(message.channel, message.value - 0x40)
            0x4E -> RolandIntegra7MidiMessage.VibratoDelay(message.channel, message.value - 0x40)
            0x50 -> RolandIntegra7MidiMessage.ToneVariation1(message.channel, message.value)
            0x51 -> RolandIntegra7MidiMessage.ToneVariation2(message.channel, message.value)
            0x52 -> RolandIntegra7MidiMessage.ToneVariation3(message.channel, message.value)
            0x53 -> RolandIntegra7MidiMessage.ToneVariation4(message.channel, message.value)
            0x54 -> RolandIntegra7MidiMessage.PortamentoControl(message.channel, message.value)
            0x5B -> RolandIntegra7MidiMessage.ReverbSend(message.channel, message.value)
            0x5D -> RolandIntegra7MidiMessage.ChorusSend(message.channel, message.value)
            0x65 -> RolandIntegra7MidiMessage.RpnMsb(message.channel, message.value)
            0x64 -> RolandIntegra7MidiMessage.RpnLsb(message.channel, message.value)
            else -> throw IllegalArgumentException("Unresolvable $message")
        }
    }

    private fun lift(message: MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalNonRealtime): RolandIntegra7MidiMessage {
        return when(message.payload[0] * 0x100u + message.payload[1]) {
            0x0601u -> RolandIntegra7MidiMessage.IdentityRequest(message.deviceId)
            0x0602u ->
                if (message.payload[2] == 0x00u.toUByte()) RolandIntegra7MidiMessage.IdentityReply(
                    deviceId = message.deviceId,
                    manufacturerId = ManufacturerId.Triplet(message.payload[3], message.payload[4]),
                    familyCode = (message.payload[5] + message.payload[6] * 0x100u).toUShort(),
                    familyNumberCode = (message.payload[7] + message.payload[8] * 0x100u).toUShort(),
                    softwareRev =  (message.payload[9] + message.payload[10] * 0x100u + message.payload[11] * 0x10000u + message.payload[12] * 0x1000000u))
                else RolandIntegra7MidiMessage.IdentityReply(
                    deviceId = message.deviceId,
                    manufacturerId = ManufacturerId.Short(message.payload[2]),
                    familyCode = (message.payload[3] + message.payload[4] * 0x100u).toUShort(),
                    familyNumberCode = (message.payload[5] + message.payload[6] * 0x100u).toUShort(),
                    softwareRev =  (message.payload[7] + message.payload[8] * 0x100u + message.payload[9] * 0x10000u + message.payload[10] * 0x1000000u))
            else -> throw IllegalArgumentException("Unsupported Message $message")
        }
    }

    private fun lift(message: MBGenericMidiMessage.SystemCommon.SystemExclusive.UniversalRealtime): RolandIntegra7MidiMessage {
        return when(message.payload[0] * 0x100u + message.payload[1]) {
            0x0401u -> RolandIntegra7MidiMessage.MasterVolume((message.payload[2] + message.payload[3] * 0x100u).toInt())
            0x0403u -> RolandIntegra7MidiMessage.MasterFineTuning((message.payload[2] + message.payload[3] * 0x100u - 0x4000u).toFloat())
            0x0404u -> RolandIntegra7MidiMessage.MasterCoarseTuning((message.payload[2] + message.payload[3] * 0x100u).toInt() - 0x40)
            0x0404u -> RolandIntegra7MidiMessage.MasterCoarseTuning((message.payload[2] + message.payload[3] * 0x100u).toInt() - 0x40)
            else -> throw IllegalArgumentException("Unsupported Message $message")
        }
    }

    private fun lift(message: MBGenericMidiMessage.SystemCommon.SystemExclusive.ManufacturerSpecific): RolandIntegra7MidiMessage {
        return if (message.manufacturer == ManufacturerId.ROLAND) {
            val deviceId = DeviceId.Short(message.payload[0])
            val modelId = message.payload[1] * 0x10000u + message.payload[2] * 0x100u + message.payload[3]
            assert(modelId == 0x000064u)
            val command = message.payload[4]

            return when(command.toUInt()) {
                0x12u -> RolandIntegra7MidiMessage.IntegraSysExDataSet1Response(
                    deviceId = deviceId,
                    startAddress = Integra7Address((message.payload[5] * 0x1000000u + message.payload[6] * 0x10000u + message.payload[7] * 0x100u + message.payload[8]).toInt()),
                    payload = message.payload.toList().drop(7).dropLast(2).toUByteArray(),
                    checkSum = message.payload[message.payload.size - 2])
                else -> throw IllegalArgumentException("Unsupported command $command in ${message.payload.toHexString()}")
            }
        } else {
            throw IllegalArgumentException("Manufacturer is not Roland")
        }
    }
}