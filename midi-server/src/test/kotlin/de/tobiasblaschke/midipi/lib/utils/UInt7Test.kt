package de.tobiasblaschke.midipi.lib.utils

import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalUnsignedTypes::class)
class UInt7Test {
    @Test
    fun `should overflow at 0x8000`() {
        assertEquals(0x0000.toUInt7(), (0x7FFFFFFF.toUInt7() + 0x01u).toUInt7UsingValue())
        assertEquals(UInt7(0x00.toUByte7(), 0x00.toUByte7(), 0x01.toUByte7(), 0x00.toUByte7()), (0x7FFFFFFF.toUInt7() + 0x81u).toUInt7UsingValue())
        assertEquals(0x80000000u, (0x7FFFFFFF.toUInt7() + 0x01u))
        assertEquals(0x80000080u, (0x7FFFFFFF.toUInt7() + 0x81u))
    }

    @Test
    fun `inversion should keep upper bit untouched`() {
        assertEquals(UInt7.MAX_VALUE, 0x00.toUInt7().inv())
    }

    @Test
    fun `should cut upper bits`() {
        assertEquals(0x00000000.toUInt7(), 0x80000000.toUInt7())
    }

    @Test
    fun `should report proper msb and lsb`() {
        assertEquals(0x7F.toUByte7(), UInt7.MAX_VALUE.msb)
        assertEquals(0x7F.toUByte7(), UInt7.MAX_VALUE.mmsb)
        assertEquals(0x7F.toUByte7(), UInt7.MAX_VALUE.mlsb)
        assertEquals(0x7F.toUByte7(), UInt7.MAX_VALUE.lsb)
        assertEquals(0x00.toUByte7(), 0x80.toUInt7().lsb)
        assertEquals(0x01.toUByte7(), 0x80.toUInt7().mlsb)
    }
}