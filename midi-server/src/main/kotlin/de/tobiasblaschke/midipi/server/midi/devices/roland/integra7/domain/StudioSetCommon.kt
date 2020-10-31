package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain


data class StudioSet(
    val common: StudioSetCommon,
    val commonChorus: StudioSetCommonChorus,
    val commonReverb: StudioSetCommonReverb,
    val motionalSurround: StudioSetMotionalSurround,
    val masterEq: StudioSetMasterEq,
    val midiChannelPhaseLocks: List<Boolean>,
    val parts: List<StudioSetPart>,
    val partEqs: List<StudioSetPartEq>
) {
    override fun toString(): String =
        "StudioSet(\n\tcommon = $common\n" +
                "\tcommonChorus = $commonChorus\n" +
                "\tcommonReverb = $commonReverb\n" +
                "\tmotionalSurround = $motionalSurround\n" +
                "\tmasterEq = $masterEq\n" +
                "\tmidiChannelPhaseLocks = $midiChannelPhaseLocks\n" +
                "\tparts = ${parts.joinToString(
                    prefix = "\n\t  ",
                    separator = "\n\t  ",
                    postfix = "\n")}" +
                "\tpartEqs = ${partEqs.joinToString(
                    prefix = "\n\t  ",
                    separator = "\n\t  ",
                    postfix = "\n")}" +
                ")"
}

data class StudioSetCommon(
    val name: String,
    val voiceReserve01: Int,
    val voiceReserve02: Int,
    val voiceReserve03: Int,
    val voiceReserve04: Int,
    val voiceReserve05: Int,
    val voiceReserve06: Int,
    val voiceReserve07: Int,
    val voiceReserve08: Int,
    val voiceReserve09: Int,
    val voiceReserve10: Int,
    val voiceReserve11: Int,
    val voiceReserve12: Int,
    val voiceReserve13: Int,
    val voiceReserve14: Int,
    val voiceReserve15: Int,
    val voiceReserve16: Int,
    val tone1ControlSource: ControlSource,
    val tone2ControlSource: ControlSource,
    val tone3ControlSource: ControlSource,
    val tone4ControlSource: ControlSource,
    val tempo: Int,
    val reverbSwitch: Boolean,
    val chorusSwitch: Boolean,
    val masterEQSwitch: Boolean,
    val drumCompEQSwitch: Boolean,
    val extPartLevel: Int,
    val extPartChorusSendLevel: Int,
    val extPartReverbSendLevel: Int,
    val extPartReverbMuteSwitch: Boolean,
) {
    init {
        assert(name.length < 0x10) { "Name $name is too long" }
        assert(voiceReserve01 in 0..64) { "Value not in range $voiceReserve01" }
        assert(voiceReserve02 in 0..64) { "Value not in range $voiceReserve02" }
        assert(voiceReserve03 in 0..64) { "Value not in range $voiceReserve03" }
        assert(voiceReserve04 in 0..64) { "Value not in range $voiceReserve04" }
        assert(voiceReserve05 in 0..64) { "Value not in range $voiceReserve05" }
        assert(voiceReserve06 in 0..64) { "Value not in range $voiceReserve06" }
        assert(voiceReserve07 in 0..64) { "Value not in range $voiceReserve07" }
        assert(voiceReserve08 in 0..64) { "Value not in range $voiceReserve08" }
        assert(voiceReserve09 in 0..64) { "Value not in range $voiceReserve09" }
        assert(voiceReserve10 in 0..64) { "Value not in range $voiceReserve10" }
        assert(voiceReserve11 in 0..64) { "Value not in range $voiceReserve11" }
        assert(voiceReserve12 in 0..64) { "Value not in range $voiceReserve12" }
        assert(voiceReserve13 in 0..64) { "Value not in range $voiceReserve13" }
        assert(voiceReserve14 in 0..64) { "Value not in range $voiceReserve14" }
        assert(voiceReserve15 in 0..64) { "Value not in range $voiceReserve15" }
        assert(voiceReserve16 in 0..64) { "Value not in range $voiceReserve16" }
        assert(tempo in 20..250) { "Value not in range $tempo" }
        assert(extPartLevel in 0..127) { "Value not in range $extPartLevel" }
        assert(extPartChorusSendLevel in 0..127) { "Value not in range $extPartChorusSendLevel" }
        assert(extPartReverbSendLevel in 0..127) { "Value not in range $extPartReverbSendLevel" }
    }
}

data class StudioSetCommonChorus(
    val type: Int,
    val level: Int,
    val outputSelect: ChorusOutputSelect,
    val parameters: List<Int>
) {
    init {
        assert(type in 0..3) { "Value not in range $type" }
        assert(level in 0..127) { "Value not in range $level" }
//        assert(parameters.size == 20) { "Expecting exactly 20 parameters" }
        assert(parameters.all { it in -20000..20000 }) { "One of the parameters is not in the expected range $parameters" }
    }
}

enum class ChorusOutputSelect { MAIN, REV, MAIN_REV }

data class StudioSetCommonReverb(
    val type: Int,
    val level: Int,
    val outputSelect: ReverbOutputSelect,
    val parameters: List<Int>
) {
    init {
        assert(type in 0..3) { "Value not in range $type" }
        assert(level in 0..127) { "Value not in range $level" }
//        assert(parameters.size == 24) { "Expecting exactly 24 parameters" }
        assert(parameters.all { it in -2000..2000 }) { "A value is not in the expected range $parameters" }
    }
}

enum class ReverbOutputSelect { A, B, C, D }

data class StudioSetMotionalSurround(
    val switch: Boolean,
    val roomType: RoomType,
    val ambienceLevel: Int,
    val roomSize: RoomSize,
    val ambienceTime: Int,
    val ambienceDensity: Int,
    val ambienceHfDamp: Int,
    val extPartLR: Int,
    val extPartFB: Int,
    val extPartWidth: Int,
    val extPartAmbienceSendLevel: Int,
    val extPartControlChannel: Int,
    val depth: Int
) {
    init {
        assert(ambienceLevel in 0..127 ) { "Value not in range $ambienceLevel" }
        assert(ambienceTime in 0..100 ) { "Value not in range $ambienceTime" }
        assert(ambienceDensity in 0..100 ) { "Value not in range $ambienceDensity" }
        assert(ambienceHfDamp in 0..100 ) { "Value not in range $ambienceHfDamp" }
        assert(extPartLR in -64..63 ) { "Value not in range $extPartLR" }
        assert(extPartFB in -64..63 ) { "Value not in range $extPartFB" }
        assert(extPartWidth in 0..32 ) { "Value not in range $extPartWidth" }
        assert(extPartAmbienceSendLevel in 0..127 ) { "Value not in range $extPartAmbienceSendLevel" }
        assert(extPartControlChannel in 0..16 ) { "Value not in range $extPartControlChannel" }
        assert(depth in 0..100 ) { "Value not in range $depth" }
    }
}

enum class RoomType { ROOM1, ROOM2, HALL1, HALL2 }
enum class RoomSize { SMALL, MEDIUM, LARGE }

data class StudioSetMasterEq(
    val lowFrequency: SupernaturalDrumLowFrequency,
    val lowGain: Int,
    val midFrequency: SupernaturalDrumMidFrequency,
    val midGain: Int,
    val midQ: SupernaturalDrumMidQ,
    val highFrequency: SupernaturalDrumHighFrequency,
    val highGain: Int
) {
    init {
        assert(lowGain in -15..15 ) { "Value not in range $lowGain" }
        assert(midGain in -15..15 ) { "Value not in range $midGain" }
        assert(highGain in -15..15 ) { "Value not in range $highGain" }
    }
}

data class StudioSetPart(
    val receiveChannel: Int,
    val receiveSwitch: Boolean,

    val toneBankMsb: Int, // CC#0, CC#32
    val toneBankLsb: Int, // CC#0, CC#32
    val toneProgramNumber: Int,

    val level: Int, // CC#7
    val pan: Int, // CC#10
    val coarseTune: Int, // RPN#2
    val fineTune: Int, // RPN#1
    val monoPoly: MonoPolyTone,
    val legatoSwitch: OffOnTone, // CC#68
    val pitchBendRange: Int, // RPN#0 - 0..24, TONE
    val portamentoSwitch: OffOnTone, // CC#65
    val portamentoTime: Int, // CC#5 0..127, TONE
    val cutoffOffset: Int, // CC#74
    val resonanceOffset: Int, // CC#71
    val attackTimeOffset: Int, // CC#73
    val decayTimeOffset: Int, // CC#75
    val releaseTimeOffset: Int, // CC#72
    val vibratoRate: Int, // CC#76
    val vibratoDepth: Int, // CC#77
    val vibratoDelay: Int, // CC#78
    val octaveShift: Int,
    val velocitySensOffset: Int,
    val keyboardRange: IntRange,
    val keyboardFadeWidth: IntRange,
    val velocityRange: IntRange,
    val velocityFadeWidth: IntRange,
    val muteSwitch: Boolean,

    val chorusSend: Int, // CC#93
    val reverbSend: Int, // CC#91
    val outputAssign: PartOutput,

    val scaleTuneType: ScaleTuneType,
    val scaleTuneKey: NoteKey,
    val scaleTuneC: Int,
    val scaleTuneCSharp: Int,
    val scaleTuneD: Int,
    val scaleTuneDSharp: Int,
    val scaleTuneE: Int,
    val scaleTuneF: Int,
    val scaleTuneFSharp: Int,
    val scaleTuneG: Int,
    val scaleTuneGSharp: Int,
    val scaleTuneA: Int,
    val scaleTuneASharp: Int,
    val scaleTuneB: Int,

    val receiveProgramChange: Boolean,
    val receiveBankSelect: Boolean,
    val receivePitchBend: Boolean,
    val receivePolyphonicKeyPressure: Boolean,
    val receiveChannelPressure: Boolean,
    val receiveModulation: Boolean,
    val receiveVolume: Boolean,
    val receivePan: Boolean,
    val receiveExpression: Boolean,
    val receiveHold1: Boolean,

    val velocityCurveType: Int,

    val motionalSurroundLR: Int,
    val motionalSurroundFB: Int,
    val motionalSurroundWidth: Int,
    val motionalSurroundAmbienceSend: Int
) {
    init {
        assert(receiveChannel in 0..15 ) { "Value not in range $receiveChannel" }

        assert(toneBankMsb in 0..0x78 ) { "Value not in range $toneBankMsb" }
        assert(toneBankLsb in 0..0x78 ) { "Value not in range $toneBankLsb" }
        assert(toneProgramNumber in 0..127 ) { "Value not in range $toneProgramNumber" }

        assert(level in 0..127 ) { "Value not in range $level" }
        assert(pan in -64..63 ) { "Value not in range $pan" }
        assert(coarseTune in -48..48 ) { "Value not in range $coarseTune" }
        assert(fineTune in -50..50 ) { "Value not in range $fineTune" }
        assert(pitchBendRange in 0..25 ) { "Value not in range $pitchBendRange" }
        assert(portamentoTime in 0..128 ) { "Value not in range $portamentoTime" }
        assert(cutoffOffset in -64..63 ) { "Value not in range $cutoffOffset" }
        assert(resonanceOffset in -64..63 ) { "Value not in range $resonanceOffset" }
        assert(attackTimeOffset in -64..63 ) { "Value not in range $attackTimeOffset" }
        assert(decayTimeOffset in -64..63 ) { "Value not in range $decayTimeOffset" }
        assert(releaseTimeOffset in -64..63 ) { "Value not in range $releaseTimeOffset" }
        assert(vibratoRate in -64..63 ) { "Value not in range $vibratoRate" }
        assert(vibratoDepth in -64..63 ) { "Value not in range $vibratoDepth" }
        assert(vibratoDelay in -64..63 ) { "Value not in range $vibratoDelay" }
        assert(octaveShift in -3..3 ) { "Value not in range $octaveShift" }
        assert(velocitySensOffset in -63..63 ) { "Value not in range $velocitySensOffset" }
        assert(keyboardRange.first in 0..127 ) { "Value not in range $keyboardRange" }
        assert(keyboardRange.last in 0..127 ) { "Value not in range $keyboardRange" }
        assert(keyboardFadeWidth.first in 0..127 ) { "Value not in range $keyboardFadeWidth" }
        assert(keyboardFadeWidth.last in 0..127 ) { "Value not in range $keyboardFadeWidth" }
        assert(velocityRange.first in 0..127 ) { "Value not in range $velocityRange" }
        assert(velocityRange.last in 0..127 ) { "Value not in range $velocityRange" }
        assert(velocityFadeWidth.first in 0..127 ) { "Value not in range $velocityFadeWidth" }
        assert(velocityFadeWidth.last in 0..127 ) { "Value not in range $velocityFadeWidth" }

        assert(chorusSend in 0..127 ) { "Value not in range $chorusSend" }
        assert(reverbSend in 0..127 ) { "Value not in range $reverbSend" }

        assert(scaleTuneC in -63..63 ) { "Value not in range $scaleTuneC" }
        assert(scaleTuneCSharp in -63..63 ) { "Value not in range $scaleTuneCSharp" }
        assert(scaleTuneD in -63..63 ) { "Value not in range $scaleTuneD" }
        assert(scaleTuneDSharp in -63..63 ) { "Value not in range $scaleTuneDSharp" }
        assert(scaleTuneE in -63..63 ) { "Value not in range $scaleTuneE" }
        assert(scaleTuneF in -63..63 ) { "Value not in range $scaleTuneF" }
        assert(scaleTuneFSharp in -63..63 ) { "Value not in range $scaleTuneFSharp" }
        assert(scaleTuneG in -63..63 ) { "Value not in range $scaleTuneG" }
        assert(scaleTuneGSharp in -63..63 ) { "Value not in range $scaleTuneGSharp" }
        assert(scaleTuneA in -63..63 ) { "Value not in range $scaleTuneA" }
        assert(scaleTuneASharp in -63..63 ) { "Value not in range $scaleTuneASharp" }
        assert(scaleTuneB in -63..63 ) { "Value not in range $scaleTuneB" }

        assert(velocityCurveType in 1..4 ) { "Value not in range $velocityCurveType" }

        assert(motionalSurroundLR in -64..63 ) { "Value not in range $motionalSurroundLR" }
        assert(motionalSurroundFB in -64..63 ) { "Value not in range $motionalSurroundFB" }
        assert(motionalSurroundWidth in 0..32 ) { "Value not in range $motionalSurroundWidth" }
        assert(motionalSurroundAmbienceSend in 0..127 ) { "Value not in range $motionalSurroundAmbienceSend" }
    }
}

enum class PartOutput {
    OUTPUT_A, OUTPUT_B, OUTPUT_C, OUTPUT_D, OUTPUT_1,
    OUTPUT_2, OUTPUT_3, OUTPUT_4, OUTPUT_5, OUTPUT_6,
    OUTPUT_7, OUTPUT_8
}

enum class ScaleTuneType {
    CUSTOM, EQUAL, JUST_MAJOR, JUST_MINOR,
    PYTHAGORE, KIRNBERGE, MEANTONE,
    WERCKMEIS, ARABIC
}

enum class NoteKey {
    C, C_SHARP, D, D_SHARP, E, F, F_SHARP, G, G_SHARP,
    A, A_SHARP, B
}

data class StudioSetPartEq(
    val switch: Boolean,
    val lowFrequency: SupernaturalDrumLowFrequency,
    val lowGain: Int,
    val midFrequency: SupernaturalDrumMidFrequency,
    val midGain: Int,
    val midQ: SupernaturalDrumMidQ,
    val highFrequency: SupernaturalDrumHighFrequency,
    val highGain: Int
) {
    init {
        assert(lowGain in -15..15 ) { "Value not in range $lowGain" }
        assert(midGain in -15..15 ) { "Value not in range $midGain" }
        assert(highGain in -15..15 ) { "Value not in range $highGain" }
    }
}