package de.tobiasblaschke.midipi.server.midi.bearable

import java.lang.RuntimeException

class MBException(message: String, cause: Throwable): RuntimeException(message, cause) {
}