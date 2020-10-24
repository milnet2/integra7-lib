package de.tobiasblaschke.midipi.server.midi.controller.devices.integra7

import de.tobiasblaschke.midipi.server.midi.MidiDeviceDescriptor
import de.tobiasblaschke.midipi.server.midi.MidiDiscovery
import de.tobiasblaschke.midipi.server.midi.controller.GenericMidiController
import de.tobiasblaschke.midipi.server.midi.toHexString
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.sound.midi.MidiMessage
import javax.sound.midi.SysexMessage

class RolandIntegra7(private val readable: MidiDeviceDescriptor.MidiInDeviceInfo, private val writable: MidiDeviceDescriptor.MidiOutDeviceInfo, val deviceId: DeviceId): GenericMidiController(readable, Integra7MessageMapper) {
    companion object {
        const val ROLAND: UByte = 0x41u
        const val INTEGRA7: UShort = 0x6402u

        fun matches(identityRequestResponse: UByteArray): Boolean {
            val identity =
                Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.IdentityReply.read(identityRequestResponse)

            return if (identity == null) {
                false
            } else {
                return identity.manufacturerId == ROLAND &&
                        identity.deviceFamily == INTEGRA7 &&
                        identity.deviceFamilyNumber == 0x0000u.toUShort()
            }
        }
    }

    enum class DeviceId(val value: UByte) {
        BROADCAST(0x7Fu),
        DEV01(0x10u),
        DEV02(0x11u),
        DEV03(0x12u),
        DEV04(0x13u),
        DEV05(0x14u),
        DEV06(0x15u),
        DEV07(0x16u),
        DEV08(0x17u),
        DEV09(0x18u),
        DEV10(0x19u),
        DEV11(0x1Au),
        DEV12(0x1Bu),
        DEV13(0x1Cu),
        DEV14(0x1Du),
        DEV15(0x1Eu),
        DEV16(0x1Fu);

        companion object {
            fun read(value: UByte) =
                DeviceId.values()
                    .first { it.value == value }
        }
    }

    suspend fun identity(): Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.IdentityReply {
        val sysexIdentityRequest = SysexMessage()
        sysexIdentityRequest.setMessage(MidiDiscovery.SysexProbe.SYSEX_IDENTITY_REQUEST_BYTES.toByteArray(), MidiDiscovery.SysexProbe.SYSEX_IDENTITY_REQUEST_BYTES.size)
        send(sysexIdentityRequest)
        val response = flow()
            .filterIsInstance<de.tobiasblaschke.midipi.server.midi.controller.MidiInputEvent.SystemExclusive>()
            .map(Integra7MessageMapper::dispatch)
            .filterIsInstance<Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.IdentityReply>()
            .first()
        return response
    }

//    suspend fun getReverbType(): ReverbType {
//        val request = Integra7SystemExclusiveMessage.IntegraAddressRequests.TemporaryStudioSet.reverbType(0x00u).asSysExGet(deviceId)   // TODO: Bad to pass something here
//        send(request.asSysexMessage())
//        val response = runBlocking {
//            sysexFlow()
//                .filterIsInstance<Integra7SystemExclusiveMessage.SystemExclusiveRealtimeResponse.DataSet1Reply>()
//                .map { it.bytes[0] } // TODO: Bad location for unmarshalling
//                .first()
//        }
//        return ReverbType.read(response)
//    }

    fun setReverbType(reverbType: ReverbType) {
        val request = Integra7SystemExclusiveMessage.IntegraAddressRequests.TemporaryStudioSet.reverbType(reverbType.byte).asSysExSet(deviceId)
        send(request.asSysexMessage())
    }

    private fun send(message: MidiMessage) {
        assert(writable.device().isOpen)
        writable.device().receiver.send(message, -1)
        println(" Sent using $this: ${message.message.toUByteArray().toHexString()}")
    }
}