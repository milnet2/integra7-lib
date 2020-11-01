package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

data class SuperNaturalDrumKit (
    override val common: SupernaturalDrumKitCommon,
    val mfx: PcmSynthToneMfx,
    val commonCompEq: SupernaturalDrumKitCommonCompEq,
    val notes: List<SuperNaturalDrumKitNote>
): IntegraTone {
    override fun toString(): String =
        "SuperNaturalDrumKit(\n" +
                "\tcommon = $common\n" +
                "\tmfx = $mfx\n" +
                "\tcommonCompEq = $commonCompEq\n" +
                "\tnotes = ${notes.joinToString(
                    prefix = "\n\t  ",
                    separator = "\n\t  ",
                    postfix = "\n")}" +
                ")"
}

data class SupernaturalDrumKitCommon(
    override val name: String,
    override val level: Int,
    val ambienceLevel: Int,
    val phraseNo: Int,
    val tfx: Boolean,
): IntegraToneCommon

data class SupernaturalDrumKitCommonCompEq(
    val comp1Switch: Boolean,
    val comp1AttackTime: SupernaturalDrumAttackTime,
    val comp1ReleaseTime: SupernaturalDrumReleaseTime,
    val comp1Threshold: Int,
    val comp1Ratio: SupernaturalDrumRatio,
    val comp1OutputGain: Int,
    val eq1Switch: Boolean,
    val eq1LowFrequency: SupernaturalDrumLowFrequency,
    val eq1LowGain: Int,
    val eq1MidFrequency: SupernaturalDrumMidFrequency,
    val eq1MidGain: Int,
    val eq1MidQ: SupernaturalDrumMidQ,
    val eq1HighFrequency: SupernaturalDrumHighFrequency,
    val eq1HighGain: Int,

    val comp2Switch: Boolean,
    val comp2AttackTime: SupernaturalDrumAttackTime,
    val comp2ReleaseTime: SupernaturalDrumReleaseTime,
    val comp2Threshold: Int,
    val comp2Ratio: SupernaturalDrumRatio,
    val comp2OutputGain: Int,
    val eq2Switch: Boolean,
    val eq2LowFrequency: SupernaturalDrumLowFrequency,
    val eq2LowGain: Int,
    val eq2MidFrequency: SupernaturalDrumMidFrequency,
    val eq2MidGain: Int,
    val eq2MidQ: SupernaturalDrumMidQ,
    val eq2HighFrequency: SupernaturalDrumHighFrequency,
    val eq2HighGain: Int,

    val comp3Switch: Boolean,
    val comp3AttackTime: SupernaturalDrumAttackTime,
    val comp3ReleaseTime: SupernaturalDrumReleaseTime,
    val comp3Threshold: Int,
    val comp3Ratio: SupernaturalDrumRatio,
    val comp3OutputGain: Int,
    val eq3Switch: Boolean,
    val eq3LowFrequency: SupernaturalDrumLowFrequency,
    val eq3LowGain: Int,
    val eq3MidFrequency: SupernaturalDrumMidFrequency,
    val eq3MidGain: Int,
    val eq3MidQ: SupernaturalDrumMidQ,
    val eq3HighFrequency: SupernaturalDrumHighFrequency,
    val eq3HighGain: Int,

    val comp4Switch: Boolean,
    val comp4AttackTime: SupernaturalDrumAttackTime,
    val comp4ReleaseTime: SupernaturalDrumReleaseTime,
    val comp4Threshold: Int,
    val comp4Ratio: SupernaturalDrumRatio,
    val comp4OutputGain: Int,
    val eq4Switch: Boolean,
    val eq4LowFrequency: SupernaturalDrumLowFrequency,
    val eq4LowGain: Int,
    val eq4MidFrequency: SupernaturalDrumMidFrequency,
    val eq4MidGain: Int,
    val eq4MidQ: SupernaturalDrumMidQ,
    val eq4HighFrequency: SupernaturalDrumHighFrequency,
    val eq4HighGain: Int,

    val comp5Switch: Boolean,
    val comp5AttackTime: SupernaturalDrumAttackTime,
    val comp5ReleaseTime: SupernaturalDrumReleaseTime,
    val comp5Threshold: Int,
    val comp5Ratio: SupernaturalDrumRatio,
    val comp5OutputGain: Int,
    val eq5Switch: Boolean,
    val eq5LowFrequency: SupernaturalDrumLowFrequency,
    val eq5LowGain: Int,
    val eq5MidFrequency: SupernaturalDrumMidFrequency,
    val eq5MidGain: Int,
    val eq5MidQ: SupernaturalDrumMidQ,
    val eq5HighFrequency: SupernaturalDrumHighFrequency,
    val eq5HighGain: Int,

    val comp6Switch: Boolean,
    val comp6AttackTime: SupernaturalDrumAttackTime,
    val comp6ReleaseTime: SupernaturalDrumReleaseTime,
    val comp6Threshold: Int,
    val comp6Ratio: SupernaturalDrumRatio,
    val comp6OutputGain: Int,
    val eq6Switch: Boolean,
    val eq6LowFrequency: SupernaturalDrumLowFrequency,
    val eq6LowGain: Int,
    val eq6MidFrequency: SupernaturalDrumMidFrequency,
    val eq6MidGain: Int,
    val eq6MidQ: SupernaturalDrumMidQ,
    val eq6HighFrequency: SupernaturalDrumHighFrequency,
    val eq6HighGain: Int,
) {
    init {
        assert(comp1Threshold in 0..127 ) { "Value not in range $comp1Threshold" }
        assert(comp1OutputGain in 0..24 ) { "Value not in range $comp1OutputGain" }
        assert(eq1LowGain in -15..15 ) { "Value not in range $eq1LowGain" }
        assert(eq1MidGain in -15..15 ) { "Value not in range $eq1MidGain" }
        assert(eq1HighGain in -15..15 ) { "Value not in range $eq1HighGain" }

        assert(comp2Threshold in 0..127 ) { "Value not in range $comp2Threshold" }
        assert(comp2OutputGain in 0..24 ) { "Value not in range $comp2OutputGain" }
        assert(eq2LowGain in -15..15 ) { "Value not in range $eq2LowGain" }
        assert(eq2MidGain in -15..15 ) { "Value not in range $eq2MidGain" }
        assert(eq2HighGain in -15..15 ) { "Value not in range $eq2HighGain" }

        assert(comp3Threshold in 0..127 ) { "Value not in range $comp3Threshold" }
        assert(comp3OutputGain in 0..24 ) { "Value not in range $comp3OutputGain" }
        assert(eq3LowGain in -15..15 ) { "Value not in range $eq3LowGain" }
        assert(eq3MidGain in -15..15 ) { "Value not in range $eq3MidGain" }
        assert(eq3HighGain in -15..15 ) { "Value not in range $eq3HighGain" }

        assert(comp4Threshold in 0..127 ) { "Value not in range $comp4Threshold" }
        assert(comp4OutputGain in 0..24 ) { "Value not in range $comp4OutputGain" }
        assert(eq4LowGain in -15..15 ) { "Value not in range $eq4LowGain" }
        assert(eq4MidGain in -15..15 ) { "Value not in range $eq4MidGain" }
        assert(eq4HighGain in -15..15 ) { "Value not in range $eq4HighGain" }

        assert(comp5Threshold in 0..127 ) { "Value not in range $comp5Threshold" }
        assert(comp5OutputGain in 0..24 ) { "Value not in range $comp5OutputGain" }
        assert(eq5LowGain in -15..15 ) { "Value not in range $eq5LowGain" }
        assert(eq5MidGain in -15..15 ) { "Value not in range $eq5MidGain" }
        assert(eq5HighGain in -15..15 ) { "Value not in range $eq5HighGain" }

        assert(comp6Threshold in 0..127 ) { "Value not in range $comp6Threshold" }
        assert(comp6OutputGain in 0..24 ) { "Value not in range $comp6OutputGain" }
        assert(eq6LowGain in -15..15 ) { "Value not in range $eq6LowGain" }
        assert(eq6MidGain in -15..15 ) { "Value not in range $eq6MidGain" }
        assert(eq6HighGain in -15..15 ) { "Value not in range $eq6HighGain" }
    }
}

enum class SupernaturalDrumAttackTime {
    T_50US, T_60US, T_70US, T_80US, T_90US,
    T_100US, T_200US, T_300US, T_400US, T_500US, T_600US,
    T_700US, T_800US, T_900US, T_1MS, T_2MS, T_3MS,
    T_4MS, T_5MS, T_6MS, T_7MS, T_8MS, T_9MS,
    T_10MS, T_15MS, T_20MS, T_25_MS, T_30MS,
    T_35MS, T_40MS, T_45MS, T_50MS
}

enum class SupernaturalDrumReleaseTime {
    T_50US, T_70US, T_100US, T_500US,
    T_1MS, T_5MS,
    T_10MS, T_17MS, T_25MS, T_50_MS, T_75MS,
    T_100MS,  T_200MS, T_300MS, T_400MS,  T_500MS,
    T_600MS,  T_700MS, T_800MS, T_900MS,
    T_1000MS, T_1200MS, T_1500MS, T_2000MS
}

enum class SupernaturalDrumRatio {
    ONE_TO_ONE, TWO_TO_ONE, THREE_TO_ONE, FOUR_TO_ONE, FIVE_TO_ONE,
    SIX_TO_ONE, SEVEN_TO_ONE, EIGHT_TO_ONE, NINE_TO_ONE, TEN_TO_ONE,
    TWENTY_TO_ONE, THIRTY_TO_ONE, FOURTY_TO_ONE, FIFTY_TO_ONE,
    SIXTY_TO_ONE, SEVENTY_TO_ONE, EIGHTY_TO_ONE, NINETY_TO_ONE,
    HUNDRED_TO_ONE, INFINITY
}

enum class SupernaturalDrumLowFrequency { HZ_200, HZ_400 }
enum class SupernaturalDrumMidFrequency {
    HZ_200, HZ_250, HZ_315, HZ_400, HZ_500, HZ_630,
    HZ_800, HZ_1000, HZ_1250, HZ_1600, HZ_2000,
    HZ_2500, HZ_3150, HZ_4000, HZ_5000, HZ_6300,
    HZ_8000
}
enum class SupernaturalDrumHighFrequency { HZ_2000, HZ_4000, HZ_8000 }
enum class SupernaturalDrumMidQ {
    POINT_FIVE, ONE, TWO, FOUR, EIGHT
}

data class SuperNaturalDrumKitNote(
    val instrumentNumber: Int,
    val level: Int,
    val pan: Int,
    val chorusSendLevel: Int,
    val reverbSendLevel: Int,
    val tune: Int,
    val attack: Int,
    val decay: Int,
    val brilliance: Int,
    val variation: SuperNaturalDrumToneVariation,
    val dynamicRange: Int,
    val stereoWidth: Int,
    val outputAssign: SuperNaturalDrumToneOutput
) {
    init {
        assert(instrumentNumber in 0..512 ) { "Value not in range $instrumentNumber" }
        assert(level in 0..127 ) { "Value not in range $level" }
        assert(pan in -64..63 ) { "Value not in range $pan" }
        assert(chorusSendLevel in 0..127 ) { "Value not in range $chorusSendLevel" }
        assert(reverbSendLevel in 0..127 ) { "Value not in range $reverbSendLevel" }
        assert(tune in -1200..1200 ) { "Value not in range $tune" }
        assert(attack in 0..100 ) { "Value not in range $attack" }
        assert(decay in -63..0 ) { "Value not in range $decay" }
        assert(brilliance in -15..12 ) { "Value not in range $brilliance" }
        assert(dynamicRange in 0..63 ) { "Value not in range $dynamicRange" }
        assert(stereoWidth in 0..127 ) { "Value not in range $stereoWidth" }
    }
}

enum class SuperNaturalDrumToneVariation {
    OFF, FLAM1, FLAM2, FLAM3,
    BUZZ1, BUZZ2, BUZZ3, ROLL
}
enum class SuperNaturalDrumToneOutput {
    PART, COMP_EQ1, COMP_EQ2, COMP_EQ3, COMP_EQ4,
    COMP_EQ5, COMP_EQ6
}