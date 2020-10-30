package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class SuperNaturalDrumKit (
    override val common: SupernaturalDrumKitCommon
): IntegraTone

data class SupernaturalDrumKitCommon(
    override val name: String,
    override val level: Int,
    val ambienceLevel: Int,
    val phraseNo: Int,
    val tfx: Boolean,
): IntegraToneCommon