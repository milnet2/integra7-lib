package de.tobiasblaschke.midipi.server.midi.controller.devices

import de.tobiasblaschke.midipi.server.midi.controller.devices.integra7.RolandIntegra7
import de.tobiasblaschke.midipi.server.midi.controller.devices.integra7.Integra7SystemExclusiveMessage
import de.tobiasblaschke.midipi.server.midi.toHexString
import org.junit.Assert.*
import org.junit.Test

class RolandIntegra7Test {

   @Test
   fun `should calculate reverb checksum`() {
       val request = Integra7SystemExclusiveMessage.IntegraAddressRequests.TemporaryStudioSet.reverbType(0x02u)

       assertEquals("0x18000600", String.format("0x%08x", request.effectiveAddress.toInt()))
       assertEquals("0x60", String.format("0x%02x", request.checkSum().toInt()))
       assertEquals(
           ubyteArrayOf( 0xF0u, 0x41u, 0x10u, 0x00u, 0x00u, 0x64u, 0x12u, 0x18u, 0x00u, 0x06u, 0x00u, 0x02u, 0x60u, 0xF7u).toHexString(),
           request.asSysExSet(RolandIntegra7.DeviceId.DEV01).bytes.toHexString())
   }
}