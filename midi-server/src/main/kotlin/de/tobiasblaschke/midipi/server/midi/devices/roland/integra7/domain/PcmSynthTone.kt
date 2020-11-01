package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class PcmSynthTone(
    override val common: PcmSynthToneCommon,
    val mfx: PcmSynthToneMfx,
    val partialMixTable: PcmSynthTonePartialMixTable,
    val partial1: PcmSynthTonePartial,
    val partial2: PcmSynthTonePartial,
    val partial3: PcmSynthTonePartial,
    val partial4: PcmSynthTonePartial,
    val common2: PcmSynthToneCommon2,
): IntegraTone {
    override fun toString(): String =
        "PcmSynthTone(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
                "\tpartial1 = $partial1\n" +
                "\tpartial2 = $partial2\n" +
                "\tpartial3 = $partial3\n" +
                "\tpartial4 = $partial4\n" +
                "\tcommon2 = $common2\n)"
}

enum class MatrixControlSource(val hex: UByte) {
    OFF(0x00u),
    CC01(1u),  CC02(2u),  CC03(3u),  CC04(4u),  CC05(5u),
    CC06(6u),  CC07(7u),  CC08(8u),  CC09(9u),  CC10(10u),
    CC11(11u), CC12(12u), CC13(13u), CC14(14u), CC15(15u),
    CC16(16u), CC17(17u), CC18(18u), CC19(19u), CC20(20u),
    CC21(21u), CC22(22u), CC23(23u), CC24(24u), CC25(25u),
    CC26(26u), CC27(27u), CC28(28u), CC29(29u), CC30(30u),
    CC31(31u),                 CC33(33u), CC34(34u), CC35(35u),
    CC36(36u), CC37(37u), CC38(38u), CC39(39u), CC40(40u),
    CC41(41u), CC42(42u), CC43(43u), CC44(44u), CC45(45u),
    CC46(46u), CC47(47u), CC48(48u), CC49(49u), CC50(50u),
    CC51(51u), CC52(52u), CC53(53u), CC54(54u), CC55(55u),
    CC56(56u), CC57(57u), CC58(58u), CC59(59u), CC60(60u),
    CC61(61u), CC62(62u), CC63(63u), CC64(64u), CC65(65u),
    CC66(66u), CC67(67u), CC68(68u), CC69(69u), CC70(70u),
    CC71(71u), CC72(72u), CC73(73u), CC74(74u), CC75(75u),
    CC76(76u), CC77(77u), CC78(78u), CC79(79u), CC80(80u),
    CC81(81u), CC82(82u), CC83(83u), CC84(84u), CC85(85u),
    CC86(86u), CC87(87u), CC88(88u), CC89(89u), CC90(90u),
    CC91(91u), CC92(92u), CC93(93u), CC94(94u), CC95(95u),
    BEND(95u), AFT(97u),
    CTRL1(98u), CTRL2(99u), CTRL3(100u), CTRL4(101u),
    VELOCITY(102u), KEYFOLLOW(103u), TEMPO(104u), LFO1(105u),
    LFO2(106u), PIT_ENV(107u), TVF_ENV(108u), TVA_ENV(109u)
}

enum class MatrixControlDestination(val hex: UByte) {
    OFF(0u), PCH(1u), CUT(2u), RES(3u), LEV(4u), PAN(5u),
    DRY(6u), CHO(7u), REV(8u), PIT_LFO1(9u),
    PIT_LFO2(10u), TVF_LFO1(11u), TVF_LFO2(12u),
    TVA_LFO1(13u), TVA_LFO2(14u), PAN_LFO1(15u),
    PAN_LFO2(16u), LFO1_RATE(17u), LFO2_RATE(18u),
    PIT_ATK(19u), PIT_DCY(20u), PIT_REL(21u),
    TVF_ATK(22u), TVF_DCY(23u), TVF_REL(24u),
    TVA_ATK(25u), TVA_DCY(26u), TVA_REL(27u),
    PMT(28u), FXM(29u)
}

data class PcmSynthToneCommon(
    override val name: String,
    override val level: Int,
    val pan: Int,
    val priority: Priority,
    val coarseTuning: Int,
    val fineTuning: Int,
    val ocataveShift: Int,
    val stretchTuneDepth: Int,
    val analogFeel: Int,
    val monoPoly: MonoPoly,
    val legatoSwitch: Boolean,
    val legatoRetrigger: Boolean,
    val portamentoSwitch: Boolean,
    val portamentoMode: PortamentoMode,
    val portamentoType: PortamentoType,
    val portamentoStart: PortamentoStart,
    val portamentoTime: Int,

    val cutoffOffset: Int,
    val resonanceOffset: Int,
    val attackTimeOffset: Int,
    val releaseTimeOffset: Int,
    val velocitySensOffset: Int,

    val pmtControlSwitch: Boolean,
    val pitchBendRangeUp: Int,
    val pitchBendRangeDown: Int,

    val matrixControl1Source: MatrixControlSource,
    val matrixControl1Destination1: MatrixControlDestination,
    val matrixControl1Sens1: Int,
    val matrixControl1Destination2: MatrixControlDestination,
    val matrixControl1Sens2: Int,
    val matrixControl1Destination3: MatrixControlDestination,
    val matrixControl1Sens3: Int,
    val matrixControl1Destination4: MatrixControlDestination,
    val matrixControl1Sens4: Int,

    val matrixControl2Source: MatrixControlSource,
    val matrixControl2Destination1: MatrixControlDestination,
    val matrixControl2Sens1: Int,
    val matrixControl2Destination2: MatrixControlDestination,
    val matrixControl2Sens2: Int,
    val matrixControl2Destination3: MatrixControlDestination,
    val matrixControl2Sens3: Int,
    val matrixControl2Destination4: MatrixControlDestination,
    val matrixControl2Sens4: Int,

    val matrixControl3Source: MatrixControlSource,
    val matrixControl3Destination1: MatrixControlDestination,
    val matrixControl3Sens1: Int,
    val matrixControl3Destination2: MatrixControlDestination,
    val matrixControl3Sens2: Int,
    val matrixControl3Destination3: MatrixControlDestination,
    val matrixControl3Sens3: Int,
    val matrixControl3Destination4: MatrixControlDestination,
    val matrixControl3Sens4: Int,

    val matrixControl4Source: MatrixControlSource,
    val matrixControl4Destination1: MatrixControlDestination,
    val matrixControl4Sens1: Int,
    val matrixControl4Destination2: MatrixControlDestination,
    val matrixControl4Sens2: Int,
    val matrixControl4Destination3: MatrixControlDestination,
    val matrixControl4Sens3: Int,
    val matrixControl4Destination4: MatrixControlDestination,
    val matrixControl4Sens4: Int,
): IntegraToneCommon {
    init {
        assert(name.length <= 0x0C)
        assert(level in 0..127) { "Not in the expected range $level" }
        assert(pan in -64..63) { "Not in the expected range $pan" }
        assert(coarseTuning in -48..48) { "Not in the expected range $coarseTuning" }
        assert(fineTuning in -50..50) { "Not in the expected range $fineTuning" }
        assert(ocataveShift in -3..3) { "Not in the expected range $ocataveShift" }
        assert(stretchTuneDepth in 0..3) { "Not in the expected range $stretchTuneDepth" }
        assert(portamentoTime in 0..127) { "Not in the expected range $portamentoTime" }

        assert(cutoffOffset in -63..63) { "Not in the expected range $cutoffOffset" }
        assert(resonanceOffset in -63..63) { "Not in the expected range $resonanceOffset" }
        assert(attackTimeOffset in -63..63) { "Not in the expected range $attackTimeOffset" }
        assert(releaseTimeOffset in -63..63) { "Not in the expected range $releaseTimeOffset" }
        assert(velocitySensOffset in -63..63) { "Not in the expected range $velocitySensOffset" }

        assert(pitchBendRangeUp in 0..48) { "Not in the expected range $pitchBendRangeUp" }
        assert(pitchBendRangeDown in 0..48) { "Not in the expected range $pitchBendRangeDown" }

        assert(matrixControl1Sens1 in -63..63) { "Not in the expected range $matrixControl1Sens1" }
        assert(matrixControl1Sens2 in -63..63) { "Not in the expected range $matrixControl1Sens2" }
        assert(matrixControl1Sens3 in -63..63) { "Not in the expected range $matrixControl1Sens3" }
        assert(matrixControl1Sens4 in -63..63) { "Not in the expected range $matrixControl1Sens4" }

        assert(matrixControl2Sens1 in -63..63) { "Not in the expected range $matrixControl2Sens1" }
        assert(matrixControl2Sens2 in -63..63) { "Not in the expected range $matrixControl2Sens2" }
        assert(matrixControl2Sens3 in -63..63) { "Not in the expected range $matrixControl2Sens3" }
        assert(matrixControl2Sens4 in -63..63) { "Not in the expected range $matrixControl2Sens4" }

        assert(matrixControl3Sens1 in -63..63) { "Not in the expected range $matrixControl3Sens1" }
        assert(matrixControl3Sens2 in -63..63) { "Not in the expected range $matrixControl3Sens2" }
        assert(matrixControl3Sens3 in -63..63) { "Not in the expected range $matrixControl3Sens3" }
        assert(matrixControl3Sens4 in -63..63) { "Not in the expected range $matrixControl3Sens4" }

        assert(matrixControl4Sens1 in -63..63) { "Not in the expected range $matrixControl4Sens1" }
        assert(matrixControl4Sens2 in -63..63) { "Not in the expected range $matrixControl4Sens2" }
        assert(matrixControl4Sens3 in -63..63) { "Not in the expected range $matrixControl4Sens3" }
        assert(matrixControl4Sens4 in -63..63) { "Not in the expected range $matrixControl4Sens4" }
    }
}

enum class MfxControlSource(val hex: UByte) {
    OFF(0x00u),
    CC01(1u),  CC02(2u),  CC03(3u),  CC04(4u),  CC05(5u),
    CC06(6u),  CC07(7u),  CC08(8u),  CC09(9u),  CC10(10u),
    CC11(11u), CC12(12u), CC13(13u), CC14(14u), CC15(15u),
    CC16(16u), CC17(17u), CC18(18u), CC19(19u), CC20(20u),
    CC21(21u), CC22(22u), CC23(23u), CC24(24u), CC25(25u),
    CC26(26u), CC27(27u), CC28(28u), CC29(29u), CC30(30u),
    CC31(31u),                 CC33(33u), CC34(34u), CC35(35u),
    CC36(36u), CC37(37u), CC38(38u), CC39(39u), CC40(40u),
    CC41(41u), CC42(42u), CC43(43u), CC44(44u), CC45(45u),
    CC46(46u), CC47(47u), CC48(48u), CC49(49u), CC50(50u),
    CC51(51u), CC52(52u), CC53(53u), CC54(54u), CC55(55u),
    CC56(56u), CC57(57u), CC58(58u), CC59(59u), CC60(60u),
    CC61(61u), CC62(62u), CC63(63u), CC64(64u), CC65(65u),
    CC66(66u), CC67(67u), CC68(68u), CC69(69u), CC70(70u),
    CC71(71u), CC72(72u), CC73(73u), CC74(74u), CC75(75u),
    CC76(76u), CC77(77u), CC78(78u), CC79(79u), CC80(80u),
    CC81(81u), CC82(82u), CC83(83u), CC84(84u), CC85(85u),
    CC86(86u), CC87(87u), CC88(88u), CC89(89u), CC90(90u),
    CC91(91u), CC92(92u), CC93(93u), CC94(94u), CC95(95u),
    BEND(95u), AFT(97u),
    SYS1(98u), SYS2(99u), SYS3(100u), SYS4(101u)
}

data class PcmSynthToneMfx(
    val mfxType: Int,
    val mfxChorusSend: Int,
    val mfxReverbSend: Int,

    val mfxControl1Source: MfxControlSource,
    val mfxControl1Sens: Int,
    val mfxControl2Source: MfxControlSource,
    val mfxControl2Sens: Int,
    val mfxControl3Source: MfxControlSource,
    val mfxControl3Sens: Int,
    val mfxControl4Source: MfxControlSource,
    val mfxControl4Sens: Int,

    val mfxControlAssign1: Int,
    val mfxControlAssign2: Int,
    val mfxControlAssign3: Int,
    val mfxControlAssign4: Int,

    val mfxParameter1: Int,
    val mfxParameter2: Int,
    val mfxParameter3: Int,
    val mfxParameter4: Int,
    val mfxParameter5: Int,
    val mfxParameter6: Int,
    val mfxParameter7: Int,
    val mfxParameter8: Int,
    val mfxParameter9: Int,
    val mfxParameter10: Int,
    val mfxParameter11: Int,
    val mfxParameter12: Int,
    val mfxParameter13: Int,
    val mfxParameter14: Int,
    val mfxParameter15: Int,
    val mfxParameter16: Int,
    val mfxParameter17: Int,
    val mfxParameter18: Int,
    val mfxParameter19: Int,
    val mfxParameter20: Int,
    val mfxParameter21: Int,
    val mfxParameter22: Int,
    val mfxParameter23: Int,
    val mfxParameter24: Int,
    val mfxParameter25: Int,
    val mfxParameter26: Int,
    val mfxParameter27: Int,
    val mfxParameter28: Int,
    val mfxParameter29: Int,
    val mfxParameter30: Int,
    val mfxParameter31: Int,
    val mfxParameter32: Int,
) {
    init {
        assert(mfxType in 0..67) { "Value not in range $mfxType" }
        assert(mfxChorusSend in 0..127) { "Value not in range $mfxChorusSend" }
        assert(mfxReverbSend in 0..127) { "Value not in range $mfxReverbSend" }

        assert(mfxControl1Sens in -63..63) { "Value not in range $mfxControl1Sens" }
        assert(mfxControl2Sens in -63..63) { "Value not in range $mfxControl2Sens" }
        assert(mfxControl3Sens in -63..63) { "Value not in range $mfxControl3Sens" }
        assert(mfxControl4Sens in -63..63) { "Value not in range $mfxControl4Sens" }

        assert(mfxControlAssign1 in 0..16) { "Value not in range $mfxControlAssign1" }
        assert(mfxControlAssign2 in 0..16) { "Value not in range $mfxControlAssign2" }
        assert(mfxControlAssign3 in 0..16) { "Value not in range $mfxControlAssign3" }
        assert(mfxControlAssign4 in 0..16) { "Value not in range $mfxControlAssign4" }

        assert(mfxParameter1  in -20000..20000) { "Value not in range $mfxParameter1" }
        assert(mfxParameter2  in -20000..20000) { "Value not in range $mfxParameter2" }
        assert(mfxParameter3  in -20000..20000) { "Value not in range $mfxParameter3" }
        assert(mfxParameter4  in -20000..20000) { "Value not in range $mfxParameter4" }
        assert(mfxParameter5  in -20000..20000) { "Value not in range $mfxParameter5" }
        assert(mfxParameter6  in -20000..20000) { "Value not in range $mfxParameter6" }
        assert(mfxParameter7  in -20000..20000) { "Value not in range $mfxParameter7" }
        assert(mfxParameter8  in -20000..20000) { "Value not in range $mfxParameter8" }
        assert(mfxParameter9  in -20000..20000) { "Value not in range $mfxParameter9" }
        assert(mfxParameter10  in -20000..20000) { "Value not in range $mfxParameter10" }
        assert(mfxParameter11  in -20000..20000) { "Value not in range $mfxParameter11" }
        assert(mfxParameter12  in -20000..20000) { "Value not in range $mfxParameter12" }
        assert(mfxParameter13  in -20000..20000) { "Value not in range $mfxParameter13" }
        assert(mfxParameter14  in -20000..20000) { "Value not in range $mfxParameter14" }
        assert(mfxParameter15  in -20000..20000) { "Value not in range $mfxParameter15" }
        assert(mfxParameter16  in -20000..20000) { "Value not in range $mfxParameter16" }
        assert(mfxParameter17  in -20000..20000) { "Value not in range $mfxParameter17" }
        assert(mfxParameter18  in -20000..20000) { "Value not in range $mfxParameter18" }
        assert(mfxParameter19  in -20000..20000) { "Value not in range $mfxParameter19" }
        assert(mfxParameter20  in -20000..20000) { "Value not in range $mfxParameter20" }
        assert(mfxParameter21  in -20000..20000) { "Value not in range $mfxParameter21" }
        assert(mfxParameter22  in -20000..20000) { "Value not in range $mfxParameter22" }
        assert(mfxParameter23  in -20000..20000) { "Value not in range $mfxParameter23" }
        assert(mfxParameter24  in -20000..20000) { "Value not in range $mfxParameter24" }
        assert(mfxParameter25  in -20000..20000) { "Value not in range $mfxParameter25" }
        assert(mfxParameter26  in -20000..20000) { "Value not in range $mfxParameter26" }
        assert(mfxParameter27  in -20000..20000) { "Value not in range $mfxParameter27" }
        assert(mfxParameter28  in -20000..20000) { "Value not in range $mfxParameter28" }
        assert(mfxParameter29  in -20000..20000) { "Value not in range $mfxParameter29" }
        assert(mfxParameter30  in -20000..20000) { "Value not in range $mfxParameter30" }
        assert(mfxParameter31  in -20000..20000) { "Value not in range $mfxParameter31" }
        assert(mfxParameter32  in -20000..20000) { "Value not in range $mfxParameter32" }
    }
}

enum class VelocityControl{ OFF, ON, RANDOM, CYCLE }

data class PcmSynthTonePartialMixTable(
    val structureType12: Int,
    val booster12: Int,
    val structureType34: Int,
    val booster34: Int,

    val velocityControl: VelocityControl,

    val pmt1PartialSwitch: Boolean,
    val pmt1KeyboardRange: Int,
    val pmt1KeyboardFadeWidth: Int,
    val pmt1VelocityRange: Int,
    val pmt1VelocityFade: Int,

    val pmt2PartialSwitch: Boolean,
    val pmt2KeyboardRange: Int,
    val pmt2KeyboardFadeWidth: Int,
    val pmt2VelocityRange: Int,
    val pmt2VelocityFade: Int,

    val pmt3PartialSwitch: Boolean,
    val pmt3KeyboardRange: Int,
    val pmt3KeyboardFadeWidth: Int,
    val pmt3VelocityRange: Int,
    val pmt3VelocityFade: Int,

    val pmt4PartialSwitch: Boolean,
    val pmt4KeyboardRange: Int,
    val pmt4KeyboardFadeWidth: Int,
    val pmt4VelocityRange: Int,
    val pmt4VelocityFade: Int,
) {
    init {
        assert(structureType12 in 0..9 ) { "Value not in range $structureType12" }
        assert(booster12 in 0..3 ) { "Value not in range $booster12" }
        assert(structureType34 in 0..9) { "Value not in range $structureType34" }
        assert(booster34 in 0..3) { "Value not in range $booster34" }

        assert(pmt1KeyboardRange in 0..0x4000) { "Value not in range $pmt1KeyboardRange" }
        assert(pmt1KeyboardFadeWidth in 0..0x4000 ) { "Value not in range $pmt1KeyboardFadeWidth" }
        assert(pmt1VelocityRange in 0..0x4000) { "Value not in range $pmt1VelocityRange" }
        assert(pmt1VelocityFade in 0..0x4000) { "Value not in range $pmt1VelocityFade" }

        assert(pmt2KeyboardRange in 0..0x4000) { "Value not in range $pmt2KeyboardRange" }
        assert(pmt2KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt2KeyboardFadeWidth" }
        assert(pmt2VelocityRange in 0..0x4000) { "Value not in range $pmt2VelocityRange" }
        assert(pmt2VelocityFade in 0..0x4000) { "Value not in range $pmt2VelocityFade" }

        assert(pmt3KeyboardRange in 0..0x4000) { "Value not in range $pmt3KeyboardRange" }
        assert(pmt3KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt3KeyboardFadeWidth" }
        assert(pmt3VelocityRange in 0..0x4000) { "Value not in range $pmt3VelocityRange" }
        assert(pmt3VelocityFade in 0..0x4000) { "Value not in range $pmt3VelocityFade" }

        assert(pmt4KeyboardRange in 0..0x4000) { "Value not in range $pmt4KeyboardRange" }
        assert(pmt4KeyboardFadeWidth in 0..0x4000) { "Value not in range $pmt4KeyboardFadeWidth" }
        assert(pmt4VelocityRange in 0..0x4000) { "Value not in range $pmt4VelocityRange" }
        assert(pmt4VelocityFade in 0..0x4000) { "Value not in range $pmt4VelocityFade" }
    }
}


enum class RandomPithDepth {
    PD_0, PD_1, PD_2, PD_3, PD_4, PD_5, PD_6, PD_7, PD_8, PD_9,
    PD_10, PD_20, PD_30, PD_40, PD_50, PD_60, PD_70, PD_80,
    PD_90, PD_100, PD_200, PD_300, PD_400, PD_500,
    PD_600, PD_700, PD_800, PD_900, PD_1000, PD_1100,
    PD_1200 }
enum class EnvMode { SUSTAIN, NO_SUSTAIN }
enum class DelayMode { NORMAL, HOLD, KEY_OFF_NORMAL, KEY_OFF_DECAY }
enum class OffOnReverse { OFF, ON, REVERSE }
enum class WaveGroupType { INT, SRX }
enum class WaveGain { MINUS_6_DB, ZERO, PLUS_6_DB, PLUS_12_DB }
enum class TvfFilterType { OFF, LPF, BPF, HPF, PKG, LPF2, LPF3 }
enum class BiasDirection { LOWER, UPPER, LOWER_UPPER, ALL }
enum class LfoWaveForm { SIN, TRI, SAW_UP, SAW_DW, SQR, RND, BEND_UP, BEND_DW, TRP, SAMPLE_AND_HOLD, CHS, VSIN, STEP }
enum class LfoOffset { MINUS_100, MINUS_50, ZERO, PLUS_50, PLUS_100}
enum class LfoFadeMode { ON_IN, ON_OUT, OFF_IN, OFF_OUT }

data class PcmSynthTonePartial(
    val level: Int,
    val chorusTune: Int,
    val fineTune: Int,
    val randomPithDepth: RandomPithDepth,
    val pan: Int,
    // val panKeyFollow: Int,
    val panDepth: Int,
    val alternatePanDepth: Int,
    val envMode: EnvMode,
    val delayMode: DelayMode,
    val delayTime: Int,

    val outputLevel: Int,
    val chorusSendLevel: Int,
    val reverbSendLevel: Int,

    val receiveBender: Boolean,
    val receiveExpression: Boolean,
    val receiveHold1: Boolean,
    val redamper: Boolean,

    val partialControl1Switch1: OffOnReverse,
    val partialControl1Switch2: OffOnReverse,
    val partialControl1Switch3: OffOnReverse,
    val partialControl1Switch4: OffOnReverse,
    val partialControl2Switch1: OffOnReverse,
    val partialControl2Switch2: OffOnReverse,
    val partialControl2Switch3: OffOnReverse,
    val partialControl2Switch4: OffOnReverse,
    val partialControl3Switch1: OffOnReverse,
    val partialControl3Switch2: OffOnReverse,
    val partialControl3Switch3: OffOnReverse,
    val partialControl3Switch4: OffOnReverse,
    val partialControl4Switch1: OffOnReverse,
    val partialControl4Switch2: OffOnReverse,
    val partialControl4Switch3: OffOnReverse,
    val partialControl4Switch4: OffOnReverse,

    val waveGroupType: WaveGroupType,
    val waveGroupId: Int,
    val waveNumberL: Int,
    val waveNumberR: Int,
    val waveGain: WaveGain,
    val waveFXMSwitch: Boolean,
    val waveFXMColor: Int,
    val waveFXMDepth: Int,
    val waveTempoSync: Boolean,
    // val wavePitchKeyfollow: Int,

    val pitchEnvDepth: Int,
    val pitchEnvVelocitySens: Int,
    val pitchEnvTime1VelocitySens: Int,
    val pitchEnvTime4VelocitySens: Int,
    // val pitchEnvTimeKeyfollow: Int,
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
    // val tvfCutoffKeyfollow: Int,
    val tvfCutoffVelocityCurve: Int,
    val tvfCutoffVelocitySens: Int,
    val tvfResonance: Int,
    val tvfResonanceVelocitySens: Int,
    val tvfEnvDepth: Int,
    val tvfEnvVelocityCurve: Int,
    val tvfEnvVelocitySens: Int,
    val tvfEnvTime1VelocitySens: Int,
    val tvfEnvTime4VelocitySens: Int,
    // val tvfEnvTimeKeyfollow: Int,
    val tvfEnvTime1: Int,
    val tvfEnvTime2: Int,
    val tvfEnvTime3: Int,
    val tvfEnvTime4: Int,
    val tvfEnvLevel0: Int,
    val tvfEnvLevel1: Int,
    val tvfEnvLevel2: Int,
    val tvfEnvLevel3: Int,
    val tvfEnvLevel4: Int,

    //val biasLevel: Int,
    val biasPosition: Int,
    val biasDirection: BiasDirection,
    val tvaLevelVelocityCurve: Int,
    val tvaLevelVelocitySens: Int,
    val tvaEnvTime1VelocitySens: Int,
    val tvaEnvTime4VelocitySens: Int,
    // val tvaEnvTimeKeyfollow: Int,
    val tvaEnvTime1: Int,
    val tvaEnvTime2: Int,
    val tvaEnvTime3: Int,
    val tvaEnvTime4: Int,
    val tvaEnvLevel1: Int,
    val tvaEnvLevel2: Int,
    val tvaEnvLevel3: Int,

    val lfo1WaveForm: LfoWaveForm,
    val lfo1Rate: Int,
    val lfo1Offset: LfoOffset,
    val lfo1RateDetune: Int,
    val lfo1DelayTime: Int,
    // val lfo1Keyfollow: Int,
    val lfo1FadeMode: LfoFadeMode,
    val lfo1FadeTime: Int,
    val lfo1KeyTrigger: Boolean,
    val lfo1PitchDepth: Int,
    val lfo1TvfDepth: Int,
    val lfo1TvaDepth: Int,
    val lfo1PanDepth: Int,

    val lfo2WaveForm: LfoWaveForm,
    val lfo2Rate: Int,
    val lfo2Offset: LfoOffset,
    val lfo2RateDetune: Int,
    val lfo2DelayTime: Int,
    // val lfo2Keyfollow: Int,
    val lfo2FadeMode: LfoFadeMode,
    val lfo2FadeTime: Int,
    val lfo2KeyTrigger: Boolean,
    val lfo2PitchDepth: Int,
    val lfo2TvfDepth: Int,
    val lfo2TvaDepth: Int,
    val lfo2PanDepth: Int,

    val lfoStepType: Int,
    val lfoStep1: Int,
    val lfoStep2: Int,
    val lfoStep3: Int,
    val lfoStep4: Int,
    val lfoStep5: Int,
    val lfoStep6: Int,
    val lfoStep7: Int,
    val lfoStep8: Int,
    val lfoStep9: Int,
    val lfoStep10: Int,
    val lfoStep11: Int,
    val lfoStep12: Int,
    val lfoStep13: Int,
    val lfoStep14: Int,
    val lfoStep15: Int,
    val lfoStep16: Int,
) {
    init {
        assert(level in 0..127 ) { "Value not in range $level" }
        assert(chorusTune in -48..48 ) { "Value not in range $chorusTune" }
        assert(fineTune in -50..50 ) { "Value not in range $fineTune" }
        assert(pan in -64..64 ) { "Value not in range $pan" }
        // assert(panKeyFollow in -100..100 ) { "Value not in range $panKeyFollow" }
        assert(panDepth in 0..63 ) { "Value not in range $panDepth" }
        assert(alternatePanDepth in -63..63 ) { "Value not in range $alternatePanDepth" }
        assert(delayTime in 0..149 ) { "Value not in range $delayTime" }

        assert(outputLevel in 0..127 ) { "Value not in range $outputLevel" }
        assert(chorusSendLevel in 0..127 ) { "Value not in range $chorusSendLevel" }
        assert(reverbSendLevel in 0..127 ) { "Value not in range $reverbSendLevel" }

        assert(waveGroupId in 0..16384 ) { "Value not in range $waveGroupId" }
        assert(waveNumberL in 0..16384 ) { "Value not in range $waveNumberL" }
        assert(waveNumberR in 0..16384 ) { "Value not in range $waveNumberR" }
        assert(waveFXMColor in 0..3 ) { "Value not in range $waveFXMColor" }
        assert(waveFXMDepth in 0..16 ) { "Value not in range $waveFXMDepth" }
        // assert(wavePitchKeyfollow in -200..200 ) { "Value not in range $wavePitchKeyfollow" }

        assert(pitchEnvDepth in -12..12 ) { "Value not in range: $pitchEnvDepth" }
        assert(pitchEnvVelocitySens in -63..63 ) { "Value not in range $pitchEnvVelocitySens" }
        assert(pitchEnvTime1VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime1VelocitySens" }
        assert(pitchEnvTime4VelocitySens in -63..63 ) { "Value not in range $pitchEnvTime4VelocitySens" }
        // assert(pitchEnvTimeKeyfollow in -100..100 ) { "Value not in range $pitchEnvTimeKeyfollow" }
        assert(pitchEnvTime1 in 0..127 ) { "Value not in range $pitchEnvTime1" }
        assert(pitchEnvTime2 in 0..127 ) { "Value not in range $pitchEnvTime2" }
        assert(pitchEnvTime3 in 0..127 ) { "Value not in range $pitchEnvTime3" }
        assert(pitchEnvTime4 in 0..127 ) { "Value not in range $pitchEnvTime4" }
        assert(pitchEnvLevel0 in -63..63 ) { "Value not in rangeo $pitchEnvLevel0" }
        assert(pitchEnvLevel1 in -63..63 ) { "Value not in range $pitchEnvLevel1" }
        assert(pitchEnvLevel2 in -63..63 ) { "Value not in range $pitchEnvLevel2" }
        assert(pitchEnvLevel3 in -63..63 ) { "Value not in range $pitchEnvLevel3" }
        assert(pitchEnvLevel4 in -63..63 ) { "Value not in range $pitchEnvLevel4" }

        assert(tvfCutoffFrequency in 0..127 ) { "Value not in range $tvfCutoffFrequency" }
        // assert(tvfCutoffKeyfollow in -200..200 ) { "Value not in range $tvfCutoffKeyfollow" }
        assert(tvfCutoffVelocityCurve in 0..7 ) { "Value not in range $tvfCutoffVelocityCurve" }
        assert(tvfCutoffVelocitySens in -63..63 ) { "Value not in range $tvfCutoffVelocitySens" }
        assert(tvfResonance in 0..127 ) { "Value not in range $tvfResonance" }
        assert(tvfResonanceVelocitySens in -63..63 ) { "Value not in range $tvfResonanceVelocitySens" }
        assert(tvfEnvDepth in -63..63 ) { "Value not in range $tvfEnvDepth" }
        assert(tvfEnvVelocityCurve in 0..7 ) { "Value not in range $tvfEnvVelocityCurve" }
        assert(tvfEnvVelocitySens in -63..63 ) { "Value not in range $tvfEnvVelocitySens" }
        assert(tvfEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime1VelocitySens" }
        assert(tvfEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvfEnvTime4VelocitySens" }
        // assert(tvfEnvTimeKeyfollow in -100..100 ) { "Value not in range $tvfEnvTimeKeyfollow" }
        assert(tvfEnvTime1 in 0..127 ) { "Value not in range $tvfEnvTime1" }
        assert(tvfEnvTime2 in 0..127 ) { "Value not in range $tvfEnvTime2" }
        assert(tvfEnvTime3 in 0..127 ) { "Value not in range $tvfEnvTime3" }
        assert(tvfEnvTime4 in 0..127 ) { "Value not in range $tvfEnvTime4" }
        assert(tvfEnvLevel0 in 0..127 ) { "Value not in range $tvfEnvLevel0" }
        assert(tvfEnvLevel1 in 0..127 ) { "Value not in range $tvfEnvLevel1" }
        assert(tvfEnvLevel2 in 0..127 ) { "Value not in range $tvfEnvLevel2" }
        assert(tvfEnvLevel3 in 0..127 ) { "Value not in range $tvfEnvLevel3" }
        assert(tvfEnvLevel4 in 0..127 ) { "Value not in range $tvfEnvLevel4" }

        // assert(biasLevel in -100..100 ) { "Value not in range $biasLevel" }
        assert(biasPosition in 0..127 ) { "Value not in range $biasPosition" }
        assert(tvaLevelVelocityCurve in 0..7 ) { "Value not in range $tvaLevelVelocityCurve" }
        assert(tvaLevelVelocitySens in -63..63 ) { "Value not in range $tvaLevelVelocitySens" }
        assert(tvaEnvTime1VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime1VelocitySens" }
        assert(tvaEnvTime4VelocitySens in -63..63 ) { "Value not in range $tvaEnvTime4VelocitySens" }
        // assert(tvaEnvTimeKeyfollow in -100..100 ) { "Value not in range $tvaEnvTimeKeyfollow" }
        assert(tvaEnvTime1 in 0..127 ) { "Value not in range $tvaEnvTime1" }
        assert(tvaEnvTime2 in 0..127 ) { "Value not in range $tvaEnvTime2" }
        assert(tvaEnvTime3 in 0..127 ) { "Value not in range $tvaEnvTime3" }
        assert(tvaEnvTime4 in 0..127 ) { "Value not in range $tvaEnvTime4" }
        assert(tvaEnvLevel1 in 0..127 ) { "Value not in range $tvaEnvLevel1" }
        assert(tvaEnvLevel2 in 0..127 ) { "Value not in range $tvaEnvLevel2" }
        assert(tvaEnvLevel3 in 0..127 ) { "Value not in range $tvaEnvLevel3" }

        assert(lfo1Rate in 0..149 ) { "Value not in range $lfo1Rate" }
        assert(lfo1RateDetune in 0..127 ) { "Value not in range $lfo1RateDetune" }
        assert(lfo1DelayTime in 0..127 ) { "Value not in range $lfo1DelayTime" }
        // assert(lfo1Keyfollow in -100..100 ) { "Value not in range $lfo1Keyfollow" }
        assert(lfo1FadeTime in 0..127 ) { "Value not in range $lfo1FadeTime" }
        assert(lfo1PitchDepth in -63..63 ) { "Value not in range $lfo1PitchDepth" }
        assert(lfo1TvfDepth in -63..63 ) { "Value not in range $lfo1TvfDepth" }
        assert(lfo1TvaDepth in -63..63 ) { "Value not in range $lfo1TvaDepth" }
        assert(lfo1PanDepth in -63..63 ) { "Value not in range $lfo1PanDepth" }

        assert(lfo2Rate in 0..149 ) { "Value not in range $lfo2Rate" }
        assert(lfo2RateDetune in 0..127 ) { "Value not in range $lfo2RateDetune" }
        assert(lfo2DelayTime in 0..127 ) { "Value not in range $lfo2DelayTime" }
        // assert(lfo2Keyfollow in -100..100 ) { "Value not in range $lfo2Keyfollow" }
        assert(lfo2FadeTime in 0..127 ) { "Value not in range $lfo2FadeTime" }
        assert(lfo2PitchDepth in -63..63 ) { "Value not in range $lfo2PitchDepth" }
        assert(lfo2TvfDepth in -63..63 ) { "Value not in range $lfo2TvfDepth" }
        assert(lfo2TvaDepth in -63..63 ) { "Value not in range $lfo2TvaDepth" }
        assert(lfo2PanDepth in -63..63 ) { "Value not in range $lfo2PanDepth" }

        assert(lfoStepType in 0..1 ) { "Value not in range $lfoStepType" }
        assert(lfoStep1 in -36..36 ) { "Value not in range $lfoStep1" }
        assert(lfoStep2 in -36..36 ) { "Value not in range $lfoStep2" }
        assert(lfoStep3 in -36..36 ) { "Value not in range $lfoStep3" }
        assert(lfoStep4 in -36..36 ) { "Value not in range $lfoStep4" }
        assert(lfoStep5 in -36..36 ) { "Value not in range $lfoStep5" }
        assert(lfoStep6 in -36..36 ) { "Value not in range $lfoStep6" }
        assert(lfoStep7 in -36..36 ) { "Value not in range $lfoStep7" }
        assert(lfoStep8 in -36..36 ) { "Value not in range $lfoStep8" }
        assert(lfoStep9 in -36..36 ) { "Value not in range $lfoStep9" }
        assert(lfoStep10 in -36..36 ) { "Value not in range $lfoStep10" }
        assert(lfoStep11 in -36..36 ) { "Value not in range $lfoStep11" }
        assert(lfoStep12 in -36..36 ) { "Value not in range $lfoStep12" }
        assert(lfoStep13 in -36..36 ) { "Value not in range $lfoStep13" }
        assert(lfoStep14 in -36..36 ) { "Value not in range $lfoStep14" }
        assert(lfoStep15 in -36..36 ) { "Value not in range $lfoStep15" }
        assert(lfoStep16 in -36..36 ) { "Value not in range $lfoStep16" }
    }
}

data class PcmSynthToneCommon2(
    val toneCategory: Int,
    val undocumented: Int,
    val phraseOctaveShift: Int,
    val tfxSwitch: Boolean,
    val phraseNmber: Int,
) {
    init {
        assert(toneCategory in 0..127 ) { "Value not in range $toneCategory" }
        assert(undocumented in 0..255 ) { "Value not in range $undocumented" }
        assert(phraseOctaveShift in -3..3 ) { "Value not in range $phraseOctaveShift" }
        assert(phraseNmber in 0..65535 ) { "Value not in range $phraseNmber" }
    }
}