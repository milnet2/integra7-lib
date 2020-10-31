package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

interface IntegraTone {
    val common: IntegraToneCommon
}

interface IntegraToneCommon {
    val name: String
    val level: Int
}

enum class Priority { LAST, LOUDEST }
enum class MonoPoly { MONO, POLY }
enum class MonoPolyTone { MONO, POLY, TONE }
enum class OffOnTone { OFF, ON, TONE }
enum class PortamentoMode { NORMAL, LEGATO }
enum class PortamentoType { RATE, TIME }
enum class PortamentoStart { PITCH, NOTE }