package de.tobiasblaschke.midipi.server

import de.tobiasblaschke.midipi.server.midi.MatchedDevice
import de.tobiasblaschke.midipi.server.midi.MidiDiscovery
import de.tobiasblaschke.midipi.server.midi.controller.MidiController
import de.tobiasblaschke.midipi.server.midi.controller.MidiInputEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
            dumpEvents(controller)
        }

        Thread.sleep(100000)
    }

    private fun dumpEvents(controller: MidiController) {
        val flow = controller.flow()
        GlobalScope.launch {
            flow.collect { event: MidiInputEvent -> println("Received $event") }
        }
    }
}