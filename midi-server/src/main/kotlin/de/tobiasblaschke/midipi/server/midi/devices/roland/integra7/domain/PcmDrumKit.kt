package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.IntegraToneBuilder

data class PcmDrumKit (
    override val common: PcmDrumKitCommon
): IntegraTone

data class PcmDrumKitCommon(
    override val name: String,
    override val level: Int,
): IntegraToneCommon