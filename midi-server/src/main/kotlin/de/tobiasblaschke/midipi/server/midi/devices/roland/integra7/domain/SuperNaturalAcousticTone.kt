package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class SuperNaturalAcousticTone (
    override val common: SupernaturalAcousticToneCommon,
    val mfx: PcmSynthToneMfx, // Same as PCM
): IntegraTone {
    override fun toString(): String =
        "SuperNaturalAcousticTone(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
                ")"
}

data class SupernaturalAcousticToneCommon(
    override val name: String,
    override val level: Int,

    val monoPoly: MonoPoly,
    val portamentoTimeOffset: Int,
    val cutoffOffset: Int,
    val resonanceOffset: Int,
    val attackTimeOffset: Int,
    val releaseTimeOffset: Int,
    val vibratoRate: Int,
    val vibratoDepth: Int,
    val vibratorDelay: Int,
    val octaveShift: Int,
    val category: Int,
    val phraseNumber: Int,
    val phraseOctaveShift: Int,

    val tfxSwitch: Boolean,

    val instrumentVariation: Int,
    val instrumentNumber: Int,

    val modifyParameters: List<Int>
): IntegraToneCommon {
    init {
        assert(name.length <= 0x0C) { "Name is too long '$name' (${name.length} > ${0x0C})" }
        assert(level in 0..127 ) { "Value not in range $" }

        assert(portamentoTimeOffset in -64..63 ) { "Value not in range $portamentoTimeOffset" }
        assert(cutoffOffset in -64..63 ) { "Value not in range $cutoffOffset" }
        assert(resonanceOffset in -64..63 ) { "Value not in range $resonanceOffset" }
        assert(attackTimeOffset in -64..63 ) { "Value not in range $attackTimeOffset" }
        assert(releaseTimeOffset in -64..63 ) { "Value not in range $releaseTimeOffset" }
        assert(vibratoRate in -64..63 ) { "Value not in range $vibratoRate" }
        assert(vibratoDepth in -64..63 ) { "Value not in range $vibratoDepth" }
        assert(vibratorDelay in -64..63 ) { "Value not in range $vibratorDelay" }
        assert(octaveShift in -3..3 ) { "Value not in range $octaveShift" }
        assert(category in 0..127 ) { "Value not in range $category" }
        assert(phraseNumber in 0..255 ) { "Value not in range $phraseNumber" }
        assert(phraseOctaveShift in -3..3 ) { "Value not in range $phraseOctaveShift" }

        assert(instrumentVariation in 0..127 ) { "Value not in range $instrumentVariation" }
        assert(instrumentNumber in 0..127 ) { "Value not in range $instrumentNumber" }

        assert(modifyParameters.size == 32) { "Expected exactly 32 modifier values" }
        assert(modifyParameters.all { it in 0..127 })
    }
}