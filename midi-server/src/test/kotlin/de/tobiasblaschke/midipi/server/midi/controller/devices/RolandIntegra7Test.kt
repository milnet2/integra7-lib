package de.tobiasblaschke.midipi.server.midi.controller.devices

import org.junit.Assert.*
import org.junit.Test

class RolandIntegra7Test {

   @Test
   fun `should calculate reverb checksum`() {
       val request = RolandIntegra7.IntegraAddressRequests.TemporaryStudioSet.reverbType(0x02u)

       assertEquals("0x18000600", String.format("0x%08x", request.effectiveAddress.toInt()))
       assertEquals("0x60", String.format("0x%02x", request.checkSum().toInt()))
   }


}