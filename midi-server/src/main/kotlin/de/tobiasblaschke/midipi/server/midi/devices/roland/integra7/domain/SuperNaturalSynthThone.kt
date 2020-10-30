package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.IntegraToneBuilder

data class SuperNaturalSynthTone (
    override val common: SupernaturalSynthToneCommon
): IntegraTone

data class SupernaturalSynthToneCommon(
    override val name: String,
    override val level: Int,
): IntegraToneCommon