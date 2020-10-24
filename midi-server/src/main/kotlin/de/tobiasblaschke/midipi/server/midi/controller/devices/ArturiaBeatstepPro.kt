package de.tobiasblaschke.midipi.server.midi.controller.devices

import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.controller.GenericMidiController

class ArturiaBeatstepPro(deviceInfo: MidiDeviceDescriptor.MidiInDeviceInfo): GenericMidiController(deviceInfo) {
    companion object {
        val MANUFACTURER_ID = ubyteArrayOf(0x00u, 0x20u, 0x6Bu)
        val IDENTIFY_RESPONSE = ubyteArrayOf(0x7Eu, 0x7Fu, 0x06u, 0x02u, 0x00u, 0x20u, 0x6Bu, 0x02u, 0x00u, 0x07u, 0x00u, 0x06u, 0x01u, 0x00u, 0x02u, 0xF7u)

        fun matches(identityRequestResponse: UByteArray): Boolean {
            return identityRequestResponse.contentEquals(IDENTIFY_RESPONSE)
        }
    }
}