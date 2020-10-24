package de.tobiasblaschke.midipi.server.midi.controller.devices.integra7

enum class ReverbType(val byte: UByte) {
    REVERB_1(0x00u),
    REVERB_2(0x01u),
    REVERB_3(0x02u),
    REVERB_4(0x03u),
    REVERB_5(0x04u),
    REVERB_6(0x05u);

    companion object {
        fun read(value: UByte) =
            values()
                .first { it.byte == value }
    }
}