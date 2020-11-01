package de.tobiasblaschke.midipi.lib.midi.bearable.domain

enum class MidiChannel(val zeroBased: Int) {
    CHANNEL_1(0x00),
    CHANNEL_2(0x01),
    CHANNEL_3(0x02),
    CHANNEL_4(0x03),
    CHANNEL_5(0x04),
    CHANNEL_6(0x05),
    CHANNEL_7(0x06),
    CHANNEL_8(0x07),
    CHANNEL_9(0x08),
    CHANNEL_10(0x09),
    CHANNEL_11(0x0A),
    CHANNEL_12(0x0B),
    CHANNEL_13(0x0C),
    CHANNEL_14(0x0D),
    CHANNEL_15(0x0E),
    CHANNEL_16(0x0F),
}