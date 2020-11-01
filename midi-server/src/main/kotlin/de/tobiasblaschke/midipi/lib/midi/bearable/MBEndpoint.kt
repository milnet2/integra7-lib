package de.tobiasblaschke.midipi.lib.midi.bearable

enum class MBEndpointCapabilities {
    READ,
    WRITE
}

interface MBEndpoint<out C: MBConnection> {
    val capabilities: Set<MBEndpointCapabilities>
    val name: String
    fun <R> withConnection(action: (C) -> R)
}