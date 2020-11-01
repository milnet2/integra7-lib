package de.tobiasblaschke.midipi.lib.midi.bearable

interface MBDiscovery {
    fun scan(): List<MBEndpoint<MBConnection>>
}