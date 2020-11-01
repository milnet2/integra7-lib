package de.tobiasblaschke.midipi.lib.midi.devices.roland.integra7.domain

data class PcmDrumKit(
    override val common: PcmDrumKitCommon,
    val mfx: PcmSynthToneMfx,
    val keys: List<PcmDrumKitPartial>,
    val common2: PcmDrumKitCommon2
): IntegraTone {
    override fun toString(): String =
        "PcmDrumKit(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
                "\tkeys = ${keys.joinToString(
                    prefix = "\n\t  ",
                    separator = "\n\t  ",
                    postfix = "\n")}" +
                "\tcommon2 = $common2\n)"
}

data class PcmDrumKitCommon(
    override val name: String,
    override val level: Int,
): IntegraToneCommon

enum class PcmDrumKitAssignType { MULTI, SINGLE }
enum class WmtVelocityControl { OFF, ON, RANDOM }

data class PcmDrumKitPartial(
    val name: String,

    val assignType: PcmDrumKitAssignType,
    val muteGroup: Int,

    val level: Int,
    val coarseTune: Int,
    val fineTune: Int,
    val randomPitchDepth: RandomPithDepth,
    val pan: Int,
    val randomPanDepth: Int,
    val alternatePanDepth: Int,
    val envMode: EnvMode,

    val outputLevel: Int,
    val chorusSendLevel: Int,
    val reverbSendLevel: Int,
    val outputAssign: SuperNaturalDrumToneOutput,

    val pitchBendRange: Int,
    val receiveExpression: Boolean,
    val receiveHold1: Boolean,

    val wmtVelocityControl: WmtVelocityControl,

    val wmt1WaveSwitch: Boolean,
    val wmt1WaveGroupType: WaveGroupType,
    val wmt1WaveGroupId: Int,
    val wmt1WaveNumberL: Int,
    val wmt1WaveNumberR: Int,
    val wmt1WaveGain: WaveGain,
    val wmt1WaveFxmSwitch: Boolean,
    val wmt1WaveFxmColor: Int,
    val wmt1WaveFxmDepth: Int,
    val wmt1WaveTempoSync: Boolean,
    val wmt1WaveCoarseTune: Int,
    val wmt1WaveFineTune: Int,
    val wmt1WavePan: Int,
    val wmt1WaveRandomPanSwitch: Boolean,
    val wmt1WaveAlternatePanSwitch: OffOnReverse,
    val wmt1WaveLevel: Int,
    val wmt1VelocityRange: Int,
    val wmt1VelocityFadeWidth: Int,

    val wmt2WaveSwitch: Boolean,
    val wmt2WaveGroupType: WaveGroupType,
    val wmt2WaveGroupId: Int,
    val wmt2WaveNumberL: Int,
    val wmt2WaveNumberR: Int,
    val wmt2WaveGain: WaveGain,
    val wmt2WaveFxmSwitch: Boolean,
    val wmt2WaveFxmColor: Int,
    val wmt2WaveFxmDepth: Int,
    val wmt2WaveTempoSync: Boolean,
    val wmt2WaveCoarseTune: Int,
    val wmt2WaveFineTune: Int,
    val wmt2WavePan: Int,
    val wmt2WaveRandomPanSwitch: Boolean,
    val wmt2WaveAlternatePanSwitch: OffOnReverse,
    val wmt2WaveLevel: Int,
    val wmt2VelocityRange: Int,
    val wmt2VelocityFadeWidth: Int,

    val wmt3WaveSwitch: Boolean,
    val wmt3WaveGroupType: WaveGroupType,
    val wmt3WaveGroupId: Int,
    val wmt3WaveNumberL: Int,
    val wmt3WaveNumberR: Int,
    val wmt3WaveGain: WaveGain,
    val wmt3WaveFxmSwitch: Boolean,
    val wmt3WaveFxmColor: Int,
    val wmt3WaveFxmDepth: Int,
    val wmt3WaveTempoSync: Boolean,
    val wmt3WaveCoarseTune: Int,
    val wmt3WaveFineTune: Int,
    val wmt3WavePan: Int,
    val wmt3WaveRandomPanSwitch: Boolean,
    val wmt3WaveAlternatePanSwitch: OffOnReverse,
    val wmt3WaveLevel: Int,
    val wmt3VelocityRange: Int,
    val wmt3VelocityFadeWidth: Int,

    val wmt4WaveSwitch: Boolean,
    val wmt4WaveGroupType: WaveGroupType,
    val wmt4WaveGroupId: Int,
    val wmt4WaveNumberL: Int,
    val wmt4WaveNumberR: Int,
    val wmt4WaveGain: WaveGain,
    val wmt4WaveFxmSwitch: Boolean,
    val wmt4WaveFxmColor: Int,
    val wmt4WaveFxmDepth: Int,
    val wmt4WaveTempoSync: Boolean,
    val wmt4WaveCoarseTune: Int,
    val wmt4WaveFineTune: Int,
    val wmt4WavePan: Int,
    val wmt4WaveRandomPanSwitch: Boolean,
    val wmt4WaveAlternatePanSwitch: OffOnReverse,
    val wmt4WaveLevel: Int,
    val wmt4VelocityRange: Int,
    val wmt4VelocityFadeWidth: Int,

    val pitchEnvDepth: Int,
    val pitchEnvVelocitySens: Int,
    val pitchEnvTime1VelocitySens: Int,
    val pitchEnvTime4VelocitySens: Int,

    val pitchEnvTime1: Int,
    val pitchEnvTime2: Int,
    val pitchEnvTime3: Int,
    val pitchEnvTime4: Int,

    val pitchEnvLevel0: Int,
    val pitchEnvLevel1: Int,
    val pitchEnvLevel2: Int,
    val pitchEnvLevel3: Int,
    val pitchEnvLevel4: Int,

    val tvfFilterType: TvfFilterType,
    val tvfCutoffFrequency: Int,
    val tvfCutoffVelocityCurve: Int,
    val tvfCutoffVelocitySens: Int,
    val tvfResonance: Int,
    val tvfResonanceVelocitySens: Int,
    val tvfEnvDepth: Int,
    val tvfEnvVelocityCurveType: Int,
    val tvfEnvVelocitySens: Int,
    val tvfEnvTime1VelocitySens: Int,
    val tvfEnvTime4VelocitySens: Int,
    val tvfEnvTime1: Int,
    val tvfEnvTime2: Int,
    val tvfEnvTime3: Int,
    val tvfEnvTime4: Int,
    val tvfEnvLevel0: Int,
    val tvfEnvLevel1: Int,
    val tvfEnvLevel2: Int,
    val tvfEnvLevel3: Int,
    val tvfEnvLevel4: Int,

    val tvaLevelVelocityCurve: Int,
    val tvaLevelVelocitySens: Int,
    val tvaEnvTime1VelocitySens: Int,
    val tvaEnvTime4VelocitySens: Int,
    val tvaEnvTime1: Int,
    val tvaEnvTime2: Int,
    val tvaEnvTime3: Int,
    val tvaEnvTime4: Int,
    val tvaEnvLevel1: Int,
    val tvaEnvLevel2: Int,
    val tvaEnvLevel3: Int,

    val oneShotMode: Boolean,
) {
    init {
        assert(name.length <= 0x0C) { "Name is too long '$name' (${name.length} > ${0x0C})" }

        assert(muteGroup in 0..31 ) { "Value not in range $muteGroup" }

        assert(level in 0..127 ) { "Value not in range $level" }
        assert(coarseTune in 0..127 ) { "Value not in range $coarseTune" }
        assert(fineTune in -50..50 ) { "Value not in range $fineTune" }
        assert(pan in -64..63 ) { "Value not in range $pan" }
        assert(randomPanDepth in 0..63 ) { "Value not in range $randomPanDepth" }
        assert(alternatePanDepth in -63..63 ) { "Value not in range $alternatePanDepth" }

        assert(outputLevel in 0..127 ) { "Value not in range $outputLevel" }
        assert(chorusSendLevel in 0..127 ) { "Value not in range $chorusSendLevel" }
        assert(reverbSendLevel in 0..127 ) { "Value not in range $reverbSendLevel" }

        assert(pitchBendRange in 0..48 ) { "Value not in range $pitchBendRange" }

        assert(wmt1WaveGroupId in 0..16384 ) { "Value not in range $wmt1WaveGroupId" }
        assert(wmt1WaveNumberL in 0..16384 ) { "Value not in range $wmt1WaveNumberL" }
        assert(wmt1WaveNumberR in 0..16384 ) { "Value not in range $wmt1WaveNumberR" }
        assert(wmt1WaveFxmColor in 0..3 ) { "Value not in range $wmt1WaveFxmColor" }
        assert(wmt1WaveFxmDepth in 0..16 ) { "Value not in range $wmt1WaveFxmDepth" }
        assert(wmt1WaveCoarseTune in -48..48 ) { "Value not in range $wmt1WaveCoarseTune" }
        assert(wmt1WaveFineTune in -50..50 ) { "Value not in range $wmt1WaveFineTune" }
        assert(wmt1WavePan in -64..63 ) { "Value not in range $wmt1WavePan" }
        assert(wmt1WaveLevel in 0..127 ) { "Value not in range $wmt1WaveLevel" }
        assert(wmt1VelocityRange in 0..0x4000 ) { "Value not in range $wmt1VelocityRange" }
        assert(wmt1VelocityFadeWidth in 0..0x4000 ) { "Value not in range $wmt1VelocityFadeWidth" }

        assert(wmt2WaveGroupId in 0..16384 ) { "Value not in range $wmt2WaveGroupId" }
        assert(wmt2WaveNumberL in 0..16384 ) { "Value not in range $wmt2WaveNumberL" }
        assert(wmt2WaveNumberR in 0..16384 ) { "Value not in range $wmt2WaveNumberR" }
        assert(wmt2WaveFxmColor in 0..3 ) { "Value not in range $wmt2WaveFxmColor" }
        assert(wmt2WaveFxmDepth in 0..16 ) { "Value not in range $wmt2WaveFxmDepth" }
        assert(wmt2WaveCoarseTune in -48..48 ) { "Value not in range $wmt2WaveCoarseTune" }
        assert(wmt2WaveFineTune in -50..50 ) { "Value not in range $wmt2WaveFineTune" }
        assert(wmt2WavePan in -64..63 ) { "Value not in range $wmt2WavePan" }
        assert(wmt2WaveLevel in 0..127 ) { "Value not in range $wmt2WaveLevel" }
        assert(wmt2VelocityRange in 0..0x4000 ) { "Value not in range $wmt2VelocityRange" }
        assert(wmt2VelocityFadeWidth in 0..0x4000 ) { "Value not in range $wmt2VelocityFadeWidth" }

        assert(wmt3WaveGroupId in 0..16384 ) { "Value not in range $wmt3WaveGroupId" }
        assert(wmt3WaveNumberL in 0..16384 ) { "Value not in range $wmt3WaveNumberL" }
        assert(wmt3WaveNumberR in 0..16384 ) { "Value not in range $wmt3WaveNumberR" }
        assert(wmt3WaveFxmColor in 0..3 ) { "Value not in range $wmt3WaveFxmColor" }
        assert(wmt3WaveFxmDepth in 0..16 ) { "Value not in range $wmt3WaveFxmDepth" }
        assert(wmt3WaveCoarseTune in -48..48 ) { "Value not in range $wmt3WaveCoarseTune" }
        assert(wmt3WaveFineTune in -50..50 ) { "Value not in range $wmt3WaveFineTune" }
        assert(wmt3WavePan in -64..63 ) { "Value not in range $wmt3WavePan" }
        assert(wmt3WaveLevel in 0..127 ) { "Value not in range $wmt3WaveLevel" }
        assert(wmt3VelocityRange in 0..0x4000 ) { "Value not in range $wmt3VelocityRange" }
        assert(wmt3VelocityFadeWidth in 0..0x4000 ) { "Value not in range $wmt3VelocityFadeWidth" }

        assert(wmt4WaveGroupId in 0..16384 ) { "Value not in range $wmt4WaveGroupId" }
        assert(wmt4WaveNumberL in 0..16384 ) { "Value not in range $wmt4WaveNumberL" }
        assert(wmt4WaveNumberR in 0..16384 ) { "Value not in range $wmt4WaveNumberR" }
        assert(wmt4WaveFxmColor in 0..3 ) { "Value not in range $wmt4WaveFxmColor" }
        assert(wmt4WaveFxmDepth in 0..16 ) { "Value not in range $wmt4WaveFxmDepth" }
        assert(wmt4WaveCoarseTune in -48..48 ) { "Value not in range $wmt4WaveCoarseTune" }
        assert(wmt4WaveFineTune in -50..50 ) { "Value not in range $wmt4WaveFineTune" }
        assert(wmt4WavePan in -64..63 ) { "Value not in range $wmt4WavePan" }
        assert(wmt4WaveLevel in 0..127 ) { "Value not in range $wmt4WaveLevel" }
        assert(wmt4VelocityRange in 0..0x4000 ) { "Value not in range $wmt4VelocityRange" }
        assert(wmt4VelocityFadeWidth in 0..0x4000 ) { "Value not in range $wmt4VelocityFadeWidth" }

        assert(pitchEnvDepth in -12..12 ) { "Value not in range $pitchEnvDepth" }
        assert(pitchEnvVelocitySens in -63..63 ) { "Value not in range $pitchEnvVelocitySens" }
        assert(pitchEnvTime1VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime1VelocitySens" }
        assert(pitchEnvTime4VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime4VelocitySens" }

        assert(pitchEnvTime1 in 0..127 ) { "Value not in range $pitchEnvTime1" }
        assert(pitchEnvTime2 in 0..127 ) { "Value not in range $pitchEnvTime2" }
        assert(pitchEnvTime3 in 0..127 ) { "Value not in range $pitchEnvTime3" }
        assert(pitchEnvTime4 in 0..127 ) { "Value not in range $pitchEnvTime4" }

        assert(pitchEnvLevel0 in -63..63 ) { "Value not in range $pitchEnvLevel0" }
        assert(pitchEnvLevel1 in -63..63 ) { "Value not in range $pitchEnvLevel1" }
        assert(pitchEnvLevel2 in -63..63 ) { "Value not in range $pitchEnvLevel2" }
        assert(pitchEnvLevel3 in -63..63 ) { "Value not in range $pitchEnvLevel3" }
        assert(pitchEnvLevel4 in -63..63 ) { "Value not in range $pitchEnvLevel4" }

        assert(tvfCutoffFrequency in 0..127 ) { "Value not in range $tvfCutoffFrequency" }
        assert(tvfCutoffVelocityCurve in 0..7 ) { "Value not in range $tvfCutoffVelocityCurve" }
        assert(tvfCutoffVelocitySens in -63..63 ) { "Value not in range $tvfCutoffVelocitySens" }
        assert(tvfResonance in 0..127 ) { "Value not in range $tvfResonance" }
        assert(tvfResonanceVelocitySens in -63..63 ) { "Value not in range $tvfResonanceVelocitySens" }
        assert(tvfEnvDepth in -63..63 ) { "Value not in range $tvfEnvDepth" }
        assert(tvfEnvVelocityCurveType in 0..7 ) { "Value not in range $tvfEnvVelocityCurveType" }
        assert(tvfEnvVelocitySens in -63..63 ) { "Value not in range $tvfEnvVelocitySens" }
        assert(tvfEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime1VelocitySens" }
        assert(tvfEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime4VelocitySens" }
        assert(tvfEnvTime1 in 0..127 ) { "Value not in range $tvfEnvTime1" }
        assert(tvfEnvTime2 in 0..127 ) { "Value not in range $tvfEnvTime2" }
        assert(tvfEnvTime3 in 0..127 ) { "Value not in range $tvfEnvTime3" }
        assert(tvfEnvTime4 in 0..127 ) { "Value not in range $tvfEnvTime4" }
        assert(tvfEnvLevel0 in 0..127 ) { "Value not in range $tvfEnvLevel0" }
        assert(tvfEnvLevel1 in 0..127 ) { "Value not in range $tvfEnvLevel1" }
        assert(tvfEnvLevel2 in 0..127 ) { "Value not in range $tvfEnvLevel2" }
        assert(tvfEnvLevel3 in 0..127 ) { "Value not in range $tvfEnvLevel3" }
        assert(tvfEnvLevel4 in 0..127 ) { "Value not in range $tvfEnvLevel4" }

        assert(tvaLevelVelocityCurve in 0..7 ) { "Value not in range $tvaLevelVelocityCurve" }
        assert(tvaLevelVelocitySens in -63..63 ) { "Value not in range $tvaLevelVelocitySens" }
        assert(tvaEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime1VelocitySens" }
        assert(tvaEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime4VelocitySens" }
        assert(tvaEnvTime1 in 0..127 ) { "Value not in range $tvaEnvTime1" }
        assert(tvaEnvTime2 in 0..127 ) { "Value not in range $tvaEnvTime2" }
        assert(tvaEnvTime3 in 0..127 ) { "Value not in range $tvaEnvTime3" }
        assert(tvaEnvTime4 in 0..127 ) { "Value not in range $tvaEnvTime4" }
        assert(tvaEnvLevel1 in 0..127 ) { "Value not in range $tvaEnvLevel1" }
        assert(tvaEnvLevel2 in 0..127 ) { "Value not in range $tvaEnvLevel2" }
        assert(tvaEnvLevel3 in 0..127 ) { "Value not in range $tvaEnvLevel3" }
    }
}

data class PcmDrumKitCommon2(
    val phraseNumber: Int,
    val tfxSwitch: Boolean
)