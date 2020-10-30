package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class SuperNaturalAcousticTone (
    override val common: SupernaturalAcousticToneCommon
): IntegraTone

data class SupernaturalAcousticToneCommon(
    override val name: String,
    override val level: Int,
): IntegraToneCommon