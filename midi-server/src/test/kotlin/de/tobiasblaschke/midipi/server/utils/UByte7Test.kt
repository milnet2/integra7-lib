package de.tobiasblaschke.midipi.server.utils

import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalUnsignedTypes::class)
class UByte7Test {
    @Test
    fun `add should overflow at 0x80`() {
        assertEquals(0x00.toUByte7(), (0x7F.toUByte7() + 0x01u).toUByte7())
        assertEquals(0x00.toUByte7(), (0x7F.toUByte7() + 0x81u).toUByte7())
        assertEquals(0x80.toUShort7(), (0x7F.toUByte7() + 0x01u))
        assertEquals(0x100.toUShort7(), (0x7F.toUByte7() + 0x81u))
    }

    @Test
    fun `inversion should keep upper bit untouched`() {
        assertEquals(0x7F.toUByte7(), 0x00.toUByte7().inv())
    }

    @Test
    fun `should cut upper bits`() {
        assertEquals(0x00.toUByte7(), 0x80.toUByte7())
    }

    @Test
    fun `should properly extract nibbles`() {
        assertEquals(0x07.toUNibble(), 0x7F.toUByte7().upperNibble)
        assertEquals(0x0F.toUNibble(), 0x7F.toUByte7().lowerNibble)
    }
}