package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.SystemCommonRequestBuilder

data class SystemCommon(
    // val masterTune: String
    val masterKeyShift: Int,
    val masterLevel: Int,
    val scaleTuneSwitch: Boolean,
    val studioSetControlChannel: Int?,
    val systemControl1Source: ControlSource,
    val systemControl2Source: ControlSource,
    val systemControl3Source: ControlSource,
    val systemControl4Source: ControlSource,
    val controlSource: ControlSourceType,
    val systemClockSource: ClockSource,
    val systemTempo: Int,
    val tempoAssignSource: ControlSourceType,
    val receiveProgramChange: Boolean,
    val receiveBankSelect: Boolean,
    val centerSpeakerSwitch: Boolean,
    val subWooferSwitch: Boolean,
    val twoChOutputMode: TwoChOutputMode
)

enum class ControlSourceType {
    SYSTEM, STUDIO_SET
}

enum class ClockSource {
    MIDI, USB
}

enum class TwoChOutputMode {
    SPEAKER, PHONES
}

enum class ControlSource(val hex: UByte) {
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
    BEND(95u), AFT(97u)
}