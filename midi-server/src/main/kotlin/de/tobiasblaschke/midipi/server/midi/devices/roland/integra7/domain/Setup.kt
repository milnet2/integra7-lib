package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class Setup(
    val soundMode: SoundMode,
    val studioSetBs: Int,
    val studioSetPc: Int,
)

enum class SoundMode {
    STUDIO, GM1, GM2, GS
}