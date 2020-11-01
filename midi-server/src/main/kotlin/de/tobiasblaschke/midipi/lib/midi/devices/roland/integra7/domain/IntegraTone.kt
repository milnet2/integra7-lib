package de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7.domain

interface IntegraTone {
    val common: IntegraToneCommon
}

interface IntegraToneCommon {
    val name: String
    val level: Int
}

enum class Priority(val ord: Int) {
    LAST(0),
    LOUDEST(1);

    companion object {
        fun fromValue(value: Int): Priority =
            values().first { it.ord.toInt() == value }
    }
}
enum class MonoPoly(val ord: Int) {
    MONO(0),
    POLY(1);

    companion object {
        fun fromValue(value: Int): MonoPoly =
            values().first { it.ord.toInt() == value }
    }
}
enum class MonoPolyTone(val ord: Int) {
    MONO(0),
    POLY(1),
    TONE(2);

    companion object {
        fun fromValue(value: Int): MonoPolyTone =
            values().first { it.ord.toInt() == value }
    }
}
enum class OffOnTone(val ord: Int) {
    OFF(0),
    ON(1),
    TONE(2);

    companion object {
        fun fromValue(value: Int): OffOnTone =
            values().first { it.ord.toInt() == value }
    }
}
enum class PortamentoMode(val ord: Int) {
    NORMAL(0),
    LEGATO(1);

    companion object {
        fun fromValue(value: Int): PortamentoMode =
            values().first { it.ord.toInt() == value }
    }
}
enum class PortamentoType(val ord: Int) {
    RATE(0),
    TIME(1);

    companion object {
        fun fromValue(value: Int): PortamentoType =
            values().first { it.ord.toInt() == value }
    }
}
enum class PortamentoStart(val ord: Int) {
    PITCH(0),
    NOTE(1);

    companion object {
        fun fromValue(value: Int): PortamentoStart =
            values().first { it.ord.toInt() == value }
    }
}