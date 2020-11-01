package de.tobiasblaschke.midipi.lib.midi.bearable

import java.lang.RuntimeException

class MBException(message: String, cause: Throwable): RuntimeException(message, cause) {
}