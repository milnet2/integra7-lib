# A library for talking to the Roland INTEGRA-7

Uses MIDI for exchanging messages with the Roland INTEGRA-7 sound module.

* Abstracts from the MIDI-implementation used (only `javax.sound.midi` is implemented for now)
* Covers all of the document [INTEGRA-7 MIDI Implementation](https://www.roland.com/us/support/by_product/integra-7/owners_manuals/ac9d1b3b-a17a-417a-acc9-a67f1b757087/) (almost for now)
* Supports multiple INTEGRA-7 at the same time
* May access random addresses without dumping everything (see example below)
* In theory, it's extensible to also support other devices than the INTEGRA-7

## Open tasks

* The library is in very early stages. The API is sure to change!
* SysEx implementation is *read-only* for now (shouldn't be to hard to make read-write, though)
* Support for un-documented SysEx-commands (as found by Wonderer [here](https://forums.rolandclan.com/viewtopic.php?f=54&t=55157&sid=cf16f62b3c5824b84ab4ad104d2a819c&start=15))
* Additional facade covering the [INTEGRA-7 Parameter Guide](https://www.roland.com/us/support/by_product/integra-7/owners_manuals/699b4cf7-caf9-4d94-a284-47b026e102f9/)
    * Especially find a way of better abstracting from signed values as some range `-63..63` and some `-64..63`

## Examples

See [ExampleMain.kt](src/main/kotlin/de/tobiasblaschke/midipi/lib/ExampleMain.kt) for working examples.

### Discover an INTEGRA-7

Uses the Java-internal midi-libraries for searching for an INTEGRA-7. 

When using this interface, there's not a lot to hold on to, but the OS-supplied device name. 
Another option would be to send SysEx-identity requests to each device discovered.

```kotlin
val integra7 = MBJavaMidiDiscovery().scan()
            .filterIsInstance<MBJavaMidiEndpoint.MBJavaMidiReadWriteEndpoint>()
            .first { it.name.contains("INTEGRA7") }
```

### Read single fields via SysEx

SysEx-requests return regular a Java `Future<>` for resolving the response.

It is possible to fetch entire structures as well as single fields. 
The corresponding names of the request-builders and the returned structures line up.

```kotlin
integra7.withConnection { connection ->
    val con = RolandIntegra7(connection)

   // slow access (fetches the entire studio-set)
   println("Studio set name: " + con.request { it.studioSet }.get().common.name)

   // fast access (fetches only the name)
   println("Studio set name: " + con.request { it.studioSet.common.name }.get())
}
```

### Load a sound

Will use CC and PC messages to load a sound. Then read the name of the loaded sound via SysEx.

```kotlin
integra7.withConnection { connection ->
    val con = RolandIntegra7(connection)

    // Load SN-A "Pure ClavCA1" on channel 5
    con.send(BankSelectMsb(MidiChannel.CHANNEL_5, 89))
    con.send(BankSelectLsb(MidiChannel.CHANNEL_5, 64))
    con.send(ProgramChange(MidiChannel.CHANNEL_5, 35))

    // Assume part == channel
    println( "Expecting 'Pure ClavCA1' == " + con.part(IntegraPart.P5).sound.tone.tone.common.name)
}
```

### Send an RPN-Message

RPN-Messages may be sent like normal MIDI-messages even though they result in multiple actual messages:

```kotlin
integra7.withConnection { connection ->
    val con = RolandIntegra7(connection)

    con.send(RolandIntegra7RpnMessage.PitchBendSensitivity(MidiChannel.CHANNEL_1, semitones = 4))
}
```

## Requirements

* JDK 8 or higher
* [Kotlin stdlib](https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib) 1.4.10


