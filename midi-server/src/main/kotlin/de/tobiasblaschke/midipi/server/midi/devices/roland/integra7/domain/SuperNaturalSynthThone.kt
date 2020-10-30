package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.IntegraToneBuilder

data class SuperNaturalSynthTone (
    override val common: SupernaturalSynthToneCommon,
    val mfx: PcmSynthToneMfx,   // Same as PCM
//    val partial1: PcmSynthTonePartial?,
//    val partial2: PcmSynthTonePartial?,
//    val partial3: PcmSynthTonePartial?,
): IntegraTone {
    override fun toString(): String =
        "SuperNaturalSynthTone(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
//                "\tpartial1 = $partial1\n" +
//                "\tpartial2 = $partial2\n" +
//                "\tpartial3 = $partial3\n" +
                ")"
}

data class SupernaturalSynthToneCommon(
    override val name: String,
    override val level: Int,
): IntegraToneCommon