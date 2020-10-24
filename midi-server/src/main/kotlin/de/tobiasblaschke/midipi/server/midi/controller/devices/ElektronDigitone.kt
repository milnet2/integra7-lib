package de.tobiasblaschke.midipi.server.midi.controller.devices

import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.controller.GenericMidiController

class ElektronDigitone(deviceInfo: MidiDeviceDescriptor.MidiInDeviceInfo): GenericMidiController(deviceInfo) {
    companion object {
        val MANUFACTURER_ID = ubyteArrayOf(0x00u, 0x20u, 0x3Cu)
        val IDENTIFY_RESPONSE = ubyteArrayOf(0x7Eu, 0x00u, 0x7Cu, 0x00u, 0xF7u)

        fun matches(identityRequestResponse: UByteArray): Boolean {
            return identityRequestResponse.contentEquals(IDENTIFY_RESPONSE)
        }
    }
}