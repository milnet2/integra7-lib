package de.tobiasblaschke.midipi.lib.midi.bearable.driver.javamidi

import de.tobiasblaschke.midipi.lib.midi.bearable.MBDiscovery
import de.tobiasblaschke.midipi.lib.midi.bearable.driver.javamidi.MBJavaMidiEndpoint.MBJavaMidiWritableEndpoint
import javax.sound.midi.*

class MBJavaMidiDiscovery(private val pairMatcher: DevicePairMatcher = OsNameDevicePairMatcher): MBDiscovery {
    companion object {
        fun isAvailable(): Boolean =
            true
    }

    override fun scan(): List<MBJavaMidiEndpoint<out MBJavaMidiEndpoint.MBJavaMidiConnection>> {
        return pairMatcher.match(scanSingleDevices())
    }

    private fun scanSingleDevices(): List<MBJavaMidiEndpoint<out MBJavaMidiEndpoint.MBJavaMidiConnection>> {
        return MidiSystem.getMidiDeviceInfo()
            .mapNotNull(::toEndpoint)
            .toList()
    }

    private fun toEndpoint(info: MidiDevice.Info): MBJavaMidiEndpoint<out MBJavaMidiEndpoint.MBJavaMidiConnection>? {
        var device: MidiDevice? = null
        try {
            device = MidiSystem.getMidiDevice(info)

            return when (device.javaClass.simpleName) {
                // "SoftSynthesizer" -> MidiDeviceDescriptor.SoftwareSynthesizerInfo(device)
                "MidiInDevice" -> MBJavaMidiEndpoint.MBJavaMidiReadableEndpoint(device)
                "MidiOutDevice" -> MBJavaMidiWritableEndpoint(device)
                //"RealTimeSequencer" -> MidiDeviceDescriptor.RealTimeSequencerInfo(device)
                //else -> when (device) {
                //    is Synthesizer -> MidiDeviceDescriptor.SynthesizerInfo(device)
                //    is Sequencer -> MidiDeviceDescriptor.SequencerInfo(device)
                //    else -> MidiDeviceDescriptor.UnknownDeviceInfo(device)
                else -> null
            }
        } catch (e: MidiUnavailableException) {
            return null // MidiDeviceDescriptor.UnknownDeviceInfo(device)
        }
    }
}