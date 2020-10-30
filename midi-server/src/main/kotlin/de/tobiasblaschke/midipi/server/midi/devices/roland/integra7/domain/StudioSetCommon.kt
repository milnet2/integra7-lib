package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain

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