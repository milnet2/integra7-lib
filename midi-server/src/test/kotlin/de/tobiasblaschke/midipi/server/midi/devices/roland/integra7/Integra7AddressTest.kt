package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7

import org.junit.Assert.*
import org.junit.Test

class Integra7AddressTest {

    @Test
    fun `should calculate proper offsets no carry`() {
        val given = Integra7Address(0x12345678)
        val actual = given.offsetBy(Integra7Size(lsb = 0x02u))
        val expected = Integra7Address(0x1234567A)
        assertEquals(expected, actual)
    }

    @Test
    fun `should calculate proper offsets with factor`() {
        val given = Integra7Address(0x1B230000)
        val actual = given.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 2)
        val expected = Integra7Address(0x1B231200)

        assertEquals(Integra7Address(0x1B231000), given.offsetBy(mlsb = 0x10u, lsb = 0x00u))
        assertEquals(expected, actual)
    }


}