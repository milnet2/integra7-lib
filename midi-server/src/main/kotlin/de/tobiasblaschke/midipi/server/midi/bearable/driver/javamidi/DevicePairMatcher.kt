package de.tobiasblaschke.midipi.server.midi.bearable.javamidi

import de.tobiasblaschke.midipi.server.midi.bearable.MBConnection
import de.tobiasblaschke.midipi.server.midi.bearable.MBEndpoint
import de.tobiasblaschke.midipi.server.midi.bearable.MBEndpointCapabilities
import java.util.*

interface DevicePairMatcher {
    fun <T: MBEndpoint<MBConnection>> match(given: List<T>): List<T>
}

object NoOpDevicePairMatcher: DevicePairMatcher {
    override fun <T : MBEndpoint<MBConnection>> match(given: List<T>): List<T> =
        given
}

object OsNameDevicePairMatcher: DevicePairMatcher {
    override fun <T : MBEndpoint<MBConnection>> match(given: List<T>): List<T> {
        val grouped = given.groupBy { it.capabilities }

        val readable = grouped[EnumSet.of(MBEndpointCapabilities.READ)]
            ?.map { it.name to it }
            ?.toMap()
            ?.toMutableMap()
            ?: mutableMapOf()

        val writable = grouped[EnumSet.of(MBEndpointCapabilities.WRITE)]
            ?: emptyList()

        val ret = mutableListOf<T>()
        ret.addAll(grouped[EnumSet.of(MBEndpointCapabilities.READ, MBEndpointCapabilities.WRITE)] ?: emptyList())
        for (device in writable) {
            if (readable.containsKey(device.name)) {
                // TODO: Get rid of this casting mess!
                val reader = readable.remove(device.name)!!
                val new = MBJavaMidiEndpoint.MBJavaMidiReadWriteEndpoint(
                    (reader as MBJavaMidiEndpoint.MBJavaMidiReadableEndpoint).device,
                    (device as MBJavaMidiEndpoint.MBJavaMidiWritableEndpoint).device)
                ret.add(new as T)
            } else {
                ret.add(device)
            }
        }
        ret.addAll(readable.values)
        return ret
    }

}