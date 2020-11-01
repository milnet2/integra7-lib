package de.tobiasblaschke.midipi.server

import de.tobiasblaschke.midipi.server.midi.bearable.domain.MidiChannel
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiDiscovery
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.IntegraPart
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.RolandIntegra7
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.RolandIntegra7MidiMessage.*
import java.lang.System.exit
import kotlin.system.exitProcess

object MidiServerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val devices = MBJavaMidiDiscovery().scan()
        devices.forEach { println("Device $it") }

        val integra7 = // TODO: Pretty!
            try {
                devices
                    .filterIsInstance<MBJavaMidiEndpoint.MBJavaMidiReadWriteEndpoint>()
                    .first { it.name.contains("INTEGRA7") }!!
            } catch (e: NoSuchElementException) {
                println("No integra7 fround!")
                exitProcess(1)
            }

        println("Integra: $integra7")

        integra7.withConnection { connection ->
            val con = RolandIntegra7(connection)


// -----------------------------------------------------

            println(con.setup)
            println(con.system)
            println(con.studioSet)

// -----------------------------------------------------

            IntegraPart.values()
                .forEach { part ->
                    println("Part $part: " +
                            con.part(part)
                                .sound.tone.tone)
                }

// -----------------------------------------------------

            // slow access (fetches the entire studio-set)
            println("Studio set name:" + con.studioSet.common.name)

            // fast access (fetches only the name)
            println("Studio set name: " + con.request { it.studioSet.common.name }.get())

// -----------------------------------------------------

            // Selects program 20 on channel 4
            con.send(ProgramChange(MidiChannel.CHANNEL_4, 19))

// -----------------------------------------------------

            // Select "Pure ClavCA1" in SN-A
            con.send(BankSelectMsb(MidiChannel.CHANNEL_5, 89))
            con.send(BankSelectLsb(MidiChannel.CHANNEL_5, 64))
            con.send(ProgramChange(MidiChannel.CHANNEL_5, 35))
            // Assume part == channel
            Thread.sleep(100) // Just to be sure it's loaded...
            println( "Expecting 'Pure ClavCA1' == " + con.part(IntegraPart.P5).sound.tone.tone.common.name)

// -----------------------------------------------------

            val identity = con.identity().get()
            println("$identity")
        }
    }
}