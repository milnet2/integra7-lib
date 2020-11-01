package de.tobiasblaschke.midipi.lib.utils

import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalUnsignedTypes::class)
class UNibbleTest {
    @Test
    fun `add should overflow at 0x0F`() {
        assertEquals(0x00.toUNibble(), (0x7F.toUNibble() + 0x01u).toUNibble())
        assertEquals(0x00.toUNibble(), (0x7F.toUNibble() + 0x81u).toUNibble())
        assertEquals(0x10.toUShort7(), (0x7F.toUNibble() + 0x01u))
        assertEquals(0x90.toUShort7(), (0x7F.toUNibble() + 0x81u))
    }

    @Test
    fun `inversion should keep upper bit untouched`() {
        assertEquals(0x0F.toUNibble(), 0x00.toUNibble().inv())
    }

    @Test
    fun `should cut upper bits`() {
        assertEquals(0x00.toUNibble(), 0x10.toUNibble())
    }
}