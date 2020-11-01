package de.tobiasblaschke.midipi.server.utils

import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalUnsignedTypes::class)
class UShort7Test {
    @Test
    fun `should overflow at 0x8000`() {
        assertEquals(0x0000.toUShort7(), (0x7FFF.toUShort7() + 0x01u).toUShort7())
        assertEquals(UShort7(0x01.toUByte7(), 0x00.toUByte7()), (0x7FFF.toUShort7() + 0x81u).toUShort7())
        assertEquals(0x8000u, (0x7FFF.toUShort7() + 0x01u).toUInt())
        assertEquals(0x8080u, (0x7FFF.toUShort7() + 0x81u).toUInt())
    }

    @Test
    fun `inversion should keep upper bit untouched`() {
        assertEquals(UShort7.MAX_VALUE, 0x00.toUShort7().inv())
    }

    @Test
    fun `should cut upper bits`() {
        assertEquals(0x0000.toUShort7(), 0x8000.toUShort7())
        assertEquals(0x1234.toUShort7(), 0x9234.toUShort7())
    }

    @Test
    fun `should report proper msb and lsb`() {
        assertEquals(0x7F.toUByte7(), UShort7.MAX_VALUE.msb)
        assertEquals(0x7F.toUByte7(), UShort7.MAX_VALUE.lsb)
        assertEquals(0x00.toUByte7(), 0x80.toUShort7().lsb)
        assertEquals(0x01.toUByte7(), 0x80.toUShort7().msb)
    }
}