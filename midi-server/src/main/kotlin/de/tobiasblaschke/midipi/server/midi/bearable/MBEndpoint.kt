package de.tobiasblaschke.midipi.server.midi.bearable

import java.util.concurrent.atomic.AtomicInteger

enum class MBEndpointCapabilities {
    READ,
    WRITE
}

interface MBEndpoint<out C: MBConnection> {
    val capabilities: Set<MBEndpointCapabilities>
    val name: String
    fun <R> withConnection(action: (C) -> R)
}