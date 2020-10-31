package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class Setup(
    val soundMode: SoundMode,
    val studioSetBankSelectMsb: Int,
    val studioSetBankSelectLsb: Int,
    val studioSetPc: Int,
) {
    init {
        assert(studioSetBankSelectMsb in 0..127) { "Disallowed value $studioSetBankSelectMsb" }
        assert(studioSetBankSelectLsb in 0..127) { "Disallowed value $studioSetBankSelectLsb" }
        assert(studioSetPc in 0..127) { "Disallowed value $studioSetPc" }
    }
}

enum class SoundMode {
    STUDIO, GM1, GM2, GS
}