package de.tobiasblaschke.midipi.server.midi.bearable

interface MBDiscovery {
    fun scan(): List<MBEndpoint<MBConnection>>
}