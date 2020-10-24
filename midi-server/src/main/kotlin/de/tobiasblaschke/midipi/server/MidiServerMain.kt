package de.tobiasblaschke.midipi.server

import de.tobiasblaschke.midipi.server.midi.MatchedDevice
import de.tobiasblaschke.midipi.server.midi.MidiDiscovery
import de.tobiasblaschke.midipi.server.midi.controller.MidiController

object MidiServerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val d = MidiDiscovery()
        val devices = d.scan()
        devices.forEach { println("Device $it") }

        val controllers = devices
            .filterIsInstance<MatchedDevice.Pair>()
            .map(MidiController::create)


        controllers.forEach {  controller ->
            println("Opening a ${controller.javaClass.simpleName}")
            controller.open()
//            val flow = controller.flow()
//            GlobalScope.launch {
//                flow.collect { event: MidiInputEvent -> println("Received $event") }
//            }
        }

        Thread.sleep(100000)
    }
}