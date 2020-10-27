package de.tobiasblaschke.midipi.server

import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiDiscovery
import de.tobiasblaschke.midipi.server.midi.bearable.javamidi.MBJavaMidiEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.lifted.MBGenericMidiMessage
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.RolandIntegra7
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.RolandIntegra7MidiMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object MidiServerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val devices = MBJavaMidiDiscovery().scan()
        devices.forEach { println("Device $it") }

        val integra7 = // TODO: Pretty!
            devices
                .filterIsInstance<MBJavaMidiEndpoint.MBJavaMidiReadWriteEndpoint>()
                .first { it.name.contains("INTEGRA7") }!!

        println("Integra: $integra7")

        integra7.withConnection { connection ->
            val con = RolandIntegra7(connection)

            val identity = con.identity().get()
            println("Successful response! $identity")

            con.send(RolandIntegra7MidiMessage.ProgramChange(MBGenericMidiMessage.ChannelEvent.ProgramChange(3, 19)))

            // con.request { it.tone1.pcmSynthTone.common }
            con.request { it.test }
        }

//        val d = MidiDiscovery()
//        val devices = d.scan()
//        devices.forEach { println("Device $it") }
//
//        val controllers = devices
//            .filterIsInstance<MatchedDevice.Pair>()
//            .map(MidiController::create)
//
//        Thread.sleep(1000)
//        println()
//        println()
//        println()
//        println()
//
//        controllers
//            .filter { it !is RolandIntegra7 }
//            .forEach {  controller ->
//            println("Opening a ${controller.javaClass.simpleName}")
//            controller.open()
//            dumpEvents(controller)
//        }
//
//        controllers
//            .firstOrNull { it is RolandIntegra7 }
//            ?.let { dumpEvents(it) }
//
//        Thread.sleep(100000)
    }

//    private fun dumpEvents(controller: MidiController) {
//        val flow = controller.flow()
//        GlobalScope.launch {
//            flow.collect { event: MidiInputEvent -> println("Received $event") }
//        }
//
//        if (controller is RolandIntegra7) {
//            GlobalScope.launch {
//                delay(1000)
//                println("Requesting reverb...")
//                println("Identity is ${controller.identity()}")
//                // println("Reverb is ${controller.reverbType}")
//            }
//        }
//    }
}