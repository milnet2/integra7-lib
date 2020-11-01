package de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7.domain

data class SuperNaturalSynthTone (
    override val common: SupernaturalSynthToneCommon,
    val mfx: PcmSynthToneMfx,   // Same as PCM
    val partial1: SuperNaturalSynthTonePartial,
    val partial2: SuperNaturalSynthTonePartial,
    val partial3: SuperNaturalSynthTonePartial,
): IntegraTone {
    override fun toString(): String =
        "SuperNaturalSynthTone(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
                "\tpartial1 = $partial1\n" +
                "\tpartial2 = $partial2\n" +
                "\tpartial3 = $partial3\n" +
                ")"
}

data class SupernaturalSynthToneCommon(
    override val name: String,
    override val level: Int,

    val portamentoSwitch: Boolean,
    val portamentoTime: Int,
    val monoSwitch: Boolean,
    val octaveShift: Int,
    val pithBendRangeUp: Int,
    val pitchBendRangeDown: Int,

    val partial1Switch: Boolean,
    val partial1Select: Boolean,
    val partial2Switch: Boolean,
    val partial2Select: Boolean,
    val partial3Switch: Boolean,
    val partial3Select: Boolean,

    val ringSwitch: RingSwitch,
    val tfxSwitch: Boolean,

    val unisonSwitch: Boolean,
    val portamentoMode: PortamentoMode,
    val legatoSwitch: Boolean,
    val analogFeel: Int,
    val waveShape: Int,
    val toneCategory: Int,
    val phraseNumber: Int,
    val phraseOctaveShift: Int,
    val unisonSize: UnisonSize
): IntegraToneCommon {
    init {
        assert(name.length < 0x1C) { "Name is too long $name" }
        assert(level in 0..127 ) { "Value not in range $level" }

        assert(portamentoTime in  0..127 ) { "Value not in range $portamentoTime" }
        assert(octaveShift in -3..3 ) { "Value not in range $octaveShift" }
        assert(pithBendRangeUp in 0..24 ) { "Value not in range $pithBendRangeUp" }
        assert(pitchBendRangeDown in 0..24 ) { "Value not in range $pitchBendRangeDown" }

        assert(analogFeel in  0..127 ) { "Value not in range $analogFeel" }
        assert(waveShape in  0..127 ) { "Value not in range $waveShape" }
        assert(toneCategory in  0..127 ) { "Value not in range $toneCategory" }
        assert(phraseNumber in 0..65535 ) { "Value not in range $phraseNumber" }
        assert(phraseOctaveShift in -3..3 ) { "Value not in range $phraseOctaveShift" }
    }
}

enum class RingSwitch {
    OFF,
    @Deprecated("Unsupported value")
    NA,
    ON
}

enum class UnisonSize { UNISON_2, UNISON_4, UNISON_6, UNISON_8 }

data class SuperNaturalSynthTonePartial(
    val oscWaveForm: SnSWaveForm,
    val oscWaveFormVariation: SnsWaveFormVariation,
    val oscPitch: Int,
    val oscDetune: Int,
    val oscPulseWidthModulationDepth: Int,
    val oscPulseWidth: Int,
    val oscPitchAttackTime: Int,
    val oscPitchEnvDecay: Int,
    val oscPitchEnvDepth: Int,

    val filterMode: SnsFilterMode,
    val filterSlope: SnsFilterSlope,
    val filterCutoff: Int,
    //val filterCutoffKeyflow: Int,
    val filterEnvVelocitySens: Int,
    val filterResonance: Int,
    val filterEnvAttackTime: Int,
    val filterEnvDecayTime: Int,
    val filterEnvSustainLevel: Int,
    val filterEnvReleaseTime: Int,
    val filterEnvDepth: Int,
    val ampLevel: Int,
    val ampVelocitySens: Int,
    val ampEnvAttackTime: Int,
    val ampEnvDecayTime: Int,
    val ampEnvSustainLevel: Int,
    val ampEnvReleaseTime: Int,
    val ampPan: Int,

    val lfoShape: SnsLfoShape,
    val lfoRate: Int,
    val lfoTempoSyncSwitch: Boolean,
    val lfoTempoSyncNote: SnsLfoTempoSyncNote,
    val lfoFadeTime: Int,
    val lfoKeyTrigger: Boolean,
    val lfoPitchDepth: Int,
    val lfoFilterDepth: Int,
    val lfoAmpDepth: Int,
    val lfoPanDepth: Int,

    val modulationShape: SnsLfoShape,
    val modulationLfoRate: Int,
    val modulationLfoTempoSyncSwitch: Boolean,
    val modulationLfoTempoSyncNote: SnsLfoTempoSyncNote,
    val oscPulseWidthShift: Int,
    val modulationLfoPitchDepth: Int,
    val modulationLfoFilterDepth: Int,
    val modulationLfoAmpDepth: Int,
    val modulationLfoPanDepth: Int,

    val cutoffAftertouchSens: Int,
    val levelAftertouchSens: Int,

    val waveGain: WaveGain,
    val waveNumber: Int,
    val hpfCutoff: Int,
    val superSawDetune: Int,
    val modulationLfoRateControl: Int,
    //val ampLevelKeyfollow: Int
) {
    init {
        assert(oscPitch in -24..24 ) { "Value not in range $oscPitch" }
        assert(oscDetune in -50..50 ) { "Value not in range $oscDetune" }
        assert(oscPulseWidthModulationDepth in 0..127 ) { "Value not in range $oscPulseWidthModulationDepth" }
        assert(oscPulseWidth in 0..127 ) { "Value not in range $oscPulseWidth" }
        assert(oscPitchAttackTime in 0..127 ) { "Value not in range $oscPitchAttackTime" }
        assert(oscPitchEnvDecay in 0..127 ) { "Value not in range $oscPitchEnvDecay" }
        assert(oscPitchEnvDepth in -63..63 ) { "Value not in range $oscPitchEnvDepth" }

        assert(filterCutoff in 0..127 ) { "Value not in range $filterCutoff" }
//        assert(filterCutoffKeyflow in -100..100 ) { "Value not in range $filterCutoffKeyflow" }
        assert(filterEnvVelocitySens in -63..63 ) { "Value not in range $filterEnvVelocitySens" }
        assert(filterResonance in 0..127 ) { "Value not in range $filterResonance" }
        assert(filterEnvAttackTime in 0..127 ) { "Value not in range $filterEnvAttackTime" }
        assert(filterEnvDecayTime in 0..127 ) { "Value not in range $filterEnvDecayTime" }
        assert(filterEnvSustainLevel in 0..127 ) { "Value not in range $filterEnvSustainLevel" }
        assert(filterEnvReleaseTime in 0..127 ) { "Value not in range $filterEnvReleaseTime" }
        assert(filterEnvDepth in -63..63 ) { "Value not in range $filterEnvDepth" }
        assert(ampLevel in 0..127 ) { "Value not in range $ampLevel" }
        assert(ampVelocitySens in -63..63 ) { "Value not in range $ampVelocitySens" }
        assert(ampEnvAttackTime in 0..127 ) { "Value not in range $ampEnvAttackTime" }
        assert(ampEnvDecayTime in 0..127 ) { "Value not in range $ampEnvDecayTime" }
        assert(ampEnvSustainLevel in 0..127 ) { "Value not in range $ampEnvSustainLevel" }
        assert(ampEnvReleaseTime in 0..127 ) { "Value not in range $ampEnvReleaseTime" }
        assert(ampPan in -64..63 ) { "Value not in range $ampPan" }

        assert(lfoRate in 0..127 ) { "Value not in range $lfoRate" }
        assert(lfoFadeTime in 0..127 ) { "Value not in range $lfoFadeTime" }
        assert(lfoPitchDepth in -63..63 ) { "Value not in range $lfoPitchDepth" }
        assert(lfoFilterDepth in -63..63 ) { "Value not in range $lfoFilterDepth" }
        assert(lfoAmpDepth in -63..63 ) { "Value not in range $lfoAmpDepth" }
        assert(lfoPanDepth in -63..63 ) { "Value not in range $lfoPanDepth" }

        assert(modulationLfoRate in 0..127 ) { "Value not in range $modulationLfoRate" }
        assert(oscPulseWidthShift in 0..127 ) { "Value not in range $oscPulseWidthShift" }
        assert(modulationLfoPitchDepth in -63..63 ) { "Value not in range $modulationLfoPitchDepth" }
        assert(modulationLfoFilterDepth in -63..63 ) { "Value not in range $modulationLfoFilterDepth" }
        assert(modulationLfoAmpDepth in -63..63 ) { "Value not in range $modulationLfoAmpDepth" }
        assert(modulationLfoPanDepth in -63..63 ) { "Value not in range $modulationLfoPanDepth" }

        assert(cutoffAftertouchSens in -63..63 ) { "Value not in range $cutoffAftertouchSens" }
        assert(levelAftertouchSens in -63..63 ) { "Value not in range $levelAftertouchSens" }

        assert(waveNumber in 0..16384 ) { "Value not in range $waveNumber" }
        assert(hpfCutoff in 0..127 ) { "Value not in range $hpfCutoff" }
        assert(superSawDetune in 0..127 ) { "Value not in range $superSawDetune" }
        assert(modulationLfoRateControl in -63..63 ) { "Value not in range $modulationLfoRateControl" }
//        assert(ampLevelKeyfollow in -100..100 ) { "Value not in range $ampLevelKeyfollow" }
    }
}

enum class SnSWaveForm { SAW, SQR, PW_SQR, TRI, SINE, NOISE, SUPER_SAW, PCM }
enum class SnsWaveFormVariation { A, B, C}
enum class SnsFilterMode { BYPASS, LPF, HPF, BPF, PKG, LPF2, LPF3, LPF4 }
enum class SnsFilterSlope { MINUS_12_DB, MINUS_24_DB }
enum class SnsLfoShape { TRIANGLE, SINUS, SAW, SQUARE, SIGNAL_HOLD, RANDOM }
enum class SnsLfoTempoSyncNote {
    SIXTEEN, TWELVE, EIGHT, FOUR, TWO, ONE, THREE_QUARTER, HALVE,
    THREE_EIGHTS, THIRD, QUARTER, THREE_SIXTEENTH, SIXTH, EIGHTS,
    THREE_THIRTYTWOTH, TWELFTH, SIXTEENTH, TWENTYFOURTH, THIRTYTWOTH }
