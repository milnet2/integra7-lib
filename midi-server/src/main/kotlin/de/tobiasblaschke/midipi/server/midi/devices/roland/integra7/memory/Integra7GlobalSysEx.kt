package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.Integra7MemoryIO
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain.*
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.Integra7FieldType.*
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalArgumentException

sealed class Integra7GlobalSysEx<T>: Integra7MemoryIO<T>() {
    class SetupRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<Setup>() {
        override val size = Integra7Size(38u)

        val soundMode = UByteField(deviceId, address)
        val studioSetBankSelectMsb = UByteField(deviceId, address.offsetBy(lsb = 0x04u)) // #CC 0
        val studioSetBankSelectLsb = UByteField(deviceId, address.offsetBy(lsb = 0x05u)) // #CC 32
        val studioSetPc = UByteField(deviceId, address.offsetBy(lsb = 0x06u))

        override fun deserialize(payload: SparseUByteArray): Setup {
            assert(this.isCovering(payload)) { "Expected Setup-range ($address..${address.offsetBy(size)})" }

            return Setup(
                soundMode = when(val sm = soundMode.deserialize(payload)) {
                    0x01 -> SoundMode.STUDIO
                    0x02 -> SoundMode.GM1
                    0x03 -> SoundMode.GM2
                    0x04 -> SoundMode.GS
                    else -> throw IllegalArgumentException("Unsupported sound-mode $sm")
                },
                studioSetBankSelectMsb = studioSetBankSelectMsb.deserialize(payload),
                studioSetBankSelectLsb = studioSetBankSelectLsb.deserialize(payload),
                studioSetPc = studioSetPc.deserialize(payload))
        }
    }

    class SystemCommonRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<SystemCommon>() {
        override val size= Integra7Size(0x2Fu)

        val masterKeyShift = ByteField(deviceId, address.offsetBy(lsb = 0x04u))
        val masterLevel = UByteField(deviceId, address.offsetBy(lsb = 0x05u))
        val scaleTuneSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x06u))
        val systemControl1Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), ControlSource::fromValue)
        val systemControl2Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x21u), ControlSource::fromValue)
        val systemControl3Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x22u), ControlSource::fromValue)
        val systemControl4Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x23u), ControlSource::fromValue)
        val controlSource =
            EnumField(deviceId, address.offsetBy(lsb = 0x24u), ControlSourceType::values)
        val systemClockSource =
            EnumField(deviceId, address.offsetBy(lsb = 0x25u), ClockSource::values)
        val systemTempo = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x26u))
        val tempoAssignSource =
            EnumField(deviceId, address.offsetBy(lsb = 0x28u), ControlSourceType::values)
        val receiveProgramChange = BooleanField(deviceId, address.offsetBy(lsb = 0x29u))
        val receiveBankSelect = BooleanField(deviceId, address.offsetBy(lsb = 0x2Au))
        val centerSpeakerSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Bu))
        val subWooferSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Cu))
        val twoChOutputMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Du), TwoChOutputMode::values)

        override fun deserialize(payload: SparseUByteArray): SystemCommon {
            assert(this.isCovering(payload)) { "Expected System-common ($address..${address.offsetBy(size)})" }

            return SystemCommon(
                // TODO masterTune = (startAddress, min(payload.size, 0x0F), payload.copyOfRange(0, min(payload.size, 0x0F)))
                masterKeyShift = masterKeyShift.deserialize(payload),
                masterLevel = masterLevel.deserialize(payload),
                scaleTuneSwitch = scaleTuneSwitch.deserialize(payload),
                studioSetControlChannel = null, // TODO: if (payload[0x11] < 0x0Fu) payload[0x11].toInt() else null,
                systemControl1Source = systemControl1Source.deserialize(payload),
                systemControl2Source = systemControl2Source.deserialize(payload),
                systemControl3Source = systemControl3Source.deserialize(payload),
                systemControl4Source = systemControl4Source.deserialize(payload),
                controlSource = controlSource.deserialize(payload),
                systemClockSource = systemClockSource.deserialize(payload),
                systemTempo = systemTempo.deserialize(payload),
                tempoAssignSource = tempoAssignSource.deserialize(payload),
                receiveProgramChange = receiveProgramChange.deserialize(payload),
                receiveBankSelect = receiveBankSelect.deserialize(payload),
                centerSpeakerSwitch = centerSpeakerSwitch.deserialize(payload),
                subWooferSwitch = subWooferSwitch.deserialize(payload),
                twoChOutputMode = twoChOutputMode.deserialize(payload),
            )
        }
    }

// -----------------------------------------------------

    class StudioSetAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSet>() {
        override val size = Integra7Size(UInt7(mlsb = 0x57u.toUByte7(), lsb = UByte7.MAX_VALUE))

        val common = StudioSetCommonAddressRequestBuilder(deviceId, address)
        val commonChorus = StudioSetCommonChorusBuilder(deviceId, address.offsetBy(mlsb = 0x04u, lsb = 0x00u))
        val commonReverb = StudioSetCommonReverbBuilder(deviceId, address.offsetBy(mlsb = 0x06u, lsb = 0x00u))
        val motionalSourround = StudioSetMotionalSurroundBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u))
        val masterEq = StudioSetMasterEqBuilder(deviceId, address.offsetBy(mlsb = 0x09u, lsb = 0x00u))
        val midiChannelPhaseLocks = IntRange(0, 15)
            .map {
                BooleanField(
                    deviceId,
                    address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)
                )
            }
        val parts = IntRange(0, 15)
            .map { StudioSetPartBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)) }
        val partEqs = IntRange(0, 15)
            .map { StudioSetPartEqBuilder(deviceId, address.offsetBy(mlsb = 0x50u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it)) }

        override fun deserialize(payload: SparseUByteArray): StudioSet {
            assert(this.isCovering(payload)) { "Expected Studio-Set address ($address..${address.offsetBy(size)})" }

            return StudioSet(
                common = common.deserialize(payload),
                commonChorus = commonChorus.deserialize(payload),
                commonReverb = commonReverb.deserialize(payload),
                motionalSurround = motionalSourround.deserialize(payload),
                masterEq = masterEq.deserialize(payload),
                midiChannelPhaseLocks = midiChannelPhaseLocks
                    .mapIndexed { index, p -> p.deserialize(
                        payload
                    ) },
                parts = parts
                    .mapIndexed { index, p -> p.deserialize(
                        payload
                    ) },
                partEqs = partEqs
                    .mapIndexed { index, p -> p.deserialize(
                        payload
                    ) },
            )
        }

        class StudioSetCommonAddressRequestBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetCommon>() {
            override val size = Integra7Size(54u)

            val name = AsciiStringField(deviceId, address, 0x0F)
            val voiceReserve01 = UByteField(deviceId, address.offsetBy(lsb = 0x18u), 0..64)
            val voiceReserve02 = UByteField(deviceId, address.offsetBy(lsb = 0x19u), 0..64)
            val voiceReserve03 = UByteField(deviceId, address.offsetBy(lsb = 0x1Au), 0..64)
            val voiceReserve04 = UByteField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..64)
            val voiceReserve05 = UByteField(deviceId, address.offsetBy(lsb = 0x1Cu), 0..64)
            val voiceReserve06 = UByteField(deviceId, address.offsetBy(lsb = 0x1Du), 0..64)
            val voiceReserve07 = UByteField(deviceId, address.offsetBy(lsb = 0x1Eu), 0..64)
            val voiceReserve08 = UByteField(deviceId, address.offsetBy(lsb = 0x1Fu), 0..64)
            val voiceReserve09 = UByteField(deviceId, address.offsetBy(lsb = 0x20u), 0..64)
            val voiceReserve10 = UByteField(deviceId, address.offsetBy(lsb = 0x21u), 0..64)
            val voiceReserve11 = UByteField(deviceId, address.offsetBy(lsb = 0x22u), 0..64)
            val voiceReserve12 = UByteField(deviceId, address.offsetBy(lsb = 0x23u), 0..64)
            val voiceReserve13 = UByteField(deviceId, address.offsetBy(lsb = 0x24u), 0..64)
            val voiceReserve14 = UByteField(deviceId, address.offsetBy(lsb = 0x25u), 0..64)
            val voiceReserve15 = UByteField(deviceId, address.offsetBy(lsb = 0x26u), 0..64)
            val voiceReserve16 = UByteField(deviceId, address.offsetBy(lsb = 0x27u), 0..64)
            val tone1ControlSource =
                EnumField(deviceId, address.offsetBy(lsb = 0x39u), ControlSource::fromValue)
            val tone2ControlSource =
                EnumField(deviceId, address.offsetBy(lsb = 0x3Au), ControlSource::fromValue)
            val tone3ControlSource =
                EnumField(deviceId, address.offsetBy(lsb = 0x3Bu), ControlSource::fromValue)
            val tone4ControlSource =
                EnumField(deviceId, address.offsetBy(lsb = 0x3Cu), ControlSource::fromValue)
            val tempo = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x3Du), 20..250)
            val reverbSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x40u))
            val chorusSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x41u))
            val masterEQSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x42u))
            val drumCompEQSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x43u))
            val extPartLevel = UByteField(deviceId, address.offsetBy(lsb = 0x4Cu))
            val extPartChorusSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x4Du))
            val extPartReverbSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x4Eu))
            val extPartReverbMuteSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x4Fu))

            override fun deserialize(payload: SparseUByteArray): StudioSetCommon {
                assert(this.isCovering(payload)) { "Expected Studio-Set common address ($address..${address.offsetBy(size)})" }

                return StudioSetCommon(
                    name = name.deserialize(payload),
                    voiceReserve01 = voiceReserve01.deserialize(payload),
                    voiceReserve02 = voiceReserve02.deserialize(payload),
                    voiceReserve03 = voiceReserve03.deserialize(payload),
                    voiceReserve04 = voiceReserve04.deserialize(payload),
                    voiceReserve05 = voiceReserve05.deserialize(payload),
                    voiceReserve06 = voiceReserve06.deserialize(payload),
                    voiceReserve07 = voiceReserve07.deserialize(payload),
                    voiceReserve08 = voiceReserve08.deserialize(payload),
                    voiceReserve09 = voiceReserve09.deserialize(payload),
                    voiceReserve10 = voiceReserve10.deserialize(payload),
                    voiceReserve11 = voiceReserve11.deserialize(payload),
                    voiceReserve12 = voiceReserve12.deserialize(payload),
                    voiceReserve13 = voiceReserve13.deserialize(payload),
                    voiceReserve14 = voiceReserve14.deserialize(payload),
                    voiceReserve15 = voiceReserve15.deserialize(payload),
                    voiceReserve16 = voiceReserve16.deserialize(payload),
                    tone1ControlSource = tone1ControlSource.deserialize(payload),
                    tone2ControlSource = tone2ControlSource.deserialize(payload),
                    tone3ControlSource = tone3ControlSource.deserialize(payload),
                    tone4ControlSource = tone4ControlSource.deserialize(payload),
                    tempo = tempo.deserialize(payload),
                    // TODO: soloPart
                    reverbSwitch = reverbSwitch.deserialize(payload),
                    chorusSwitch = chorusSwitch.deserialize(payload),
                    masterEQSwitch = masterEQSwitch.deserialize(payload),
                    drumCompEQSwitch = drumCompEQSwitch.deserialize(payload),
                    // TODO: drumCompEQPart
                    // Drum Comp/EQ 1 Output Assign
                    // Drum Comp/EQ 2 Output Assign
                    // Drum Comp/EQ 3 Output Assign
                    // Drum Comp/EQ 4 Output Assign
                    // Drum Comp/EQ 5 Output Assign
                    // Drum Comp/EQ 6 Output Assign
                    extPartLevel = extPartLevel.deserialize(payload),
                    extPartChorusSendLevel = extPartChorusSendLevel.deserialize(payload),
                    extPartReverbSendLevel = extPartReverbSendLevel.deserialize(payload),
                    extPartReverbMuteSwitch = extPartReverbMuteSwitch.deserialize(payload),
                )
            }
        }

        class StudioSetCommonChorusBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetCommonChorus>() {
            override val size = Integra7Size(54u)

            val type = UByteField(deviceId, address.offsetBy(lsb = 0x00u), 0..3)
            val level = UByteField(deviceId, address.offsetBy(lsb = 0x01u), 0..127)
            val outputSelect =
                EnumField(deviceId, address.offsetBy(lsb = 0x03u), ChorusOutputSelect::values)
            val parameters = IntRange(0, 18) // TODO: Access last element 19)
                .map {
                    SignedMsbLsbFourNibbles(
                        deviceId,
                        address.offsetBy(lsb = 0x04u).offsetBy(lsb = 0x04u, factor = it),
                        -20000..20000
                    )
                }

            override fun deserialize(payload: SparseUByteArray): StudioSetCommonChorus {
                assert(this.isCovering(payload)) { "Expected Studio-Set chorus address ($address..${address.offsetBy(size)})" }

                return StudioSetCommonChorus(
                    type = type.deserialize(payload),
                    level = level.deserialize(payload),
                    outputSelect = outputSelect.deserialize(payload),
                    parameters = parameters
                        .mapIndexed { idx, p -> p.deserialize(
                            payload
                        ) }
                )
            }
        }

        class StudioSetCommonReverbBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetCommonReverb>() {
            override val size = Integra7Size(63u)

            val type = UByteField(deviceId, address.offsetBy(lsb = 0x00u), 0..3)
            val level = UByteField(deviceId, address.offsetBy(lsb = 0x01u), 0..127)
            val outputSelect =
                EnumField(deviceId, address.offsetBy(lsb = 0x02u), ReverbOutputSelect::values)
            val parameters = IntRange(0, 22) // TODO: Acces last element! 23)
                .map {
                    SignedMsbLsbFourNibbles(
                        deviceId,
                        address.offsetBy(lsb = 0x03u).offsetBy(lsb = 0x04u, factor = it),
                        -20000..20000
                    )
                }

            override fun deserialize(payload: SparseUByteArray): StudioSetCommonReverb {
                assert(this.isCovering(payload)) { "Expected Studio-Set reverb address ($address..${address.offsetBy(size)})" }

                return StudioSetCommonReverb(
                    type = type.deserialize(payload),
                    level = level.deserialize(payload),
                    outputSelect = outputSelect.deserialize(payload),
                    parameters = parameters
                        .mapIndexed { idx, p -> p.deserialize(
                            payload
                        ) }
                )
            }
        }

        class StudioSetMotionalSurroundBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetMotionalSurround>() {
            override val size = Integra7Size(0x10u.toUInt7UsingValue())

            val switch = BooleanField(deviceId, address.offsetBy(lsb = 0x00u))
            val roomType = EnumField(deviceId, address.offsetBy(lsb = 0x01u), RoomType::values)
            val ambienceLevel = UByteField(deviceId, address.offsetBy(lsb = 0x02u))
            val roomSize = EnumField(deviceId, address.offsetBy(lsb = 0x03u), RoomSize::values)
            val ambienceTime = UByteField(deviceId, address.offsetBy(lsb = 0x04u), 0..100)
            val ambienceDensity = UByteField(deviceId, address.offsetBy(lsb = 0x05u), 0..100)
            val ambienceHfDamp = UByteField(deviceId, address.offsetBy(lsb = 0x06u), 0..100)
            val extPartLR = ByteField(deviceId, address.offsetBy(lsb = 0x07u), -64..63)
            val extPartFB = ByteField(deviceId, address.offsetBy(lsb = 0x08u), -64..63)
            val extPartWidth = UByteField(deviceId, address.offsetBy(lsb = 0x09u), 0..32)
            val extPartAmbienceSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x0Au))
            val extPartControlChannel = UByteField(
                deviceId,
                address.offsetBy(lsb = 0x0Bu),
                0..16
            ) // 1..16, OFF --> Why is OFF the last now *grrr*
            val depth = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..100)

            override fun deserialize(payload: SparseUByteArray): StudioSetMotionalSurround {
                assert(this.isCovering(payload)) { "Expected Studio-Set reverb address ($address..${address.offsetBy(size)})" }

                return StudioSetMotionalSurround(
                    switch.deserialize(payload),
                    roomType.deserialize(payload),
                    ambienceLevel.deserialize(payload),
                    roomSize.deserialize(payload),
                    ambienceTime.deserialize(payload),
                    ambienceDensity.deserialize(payload),
                    ambienceHfDamp.deserialize(payload),
                    extPartLR.deserialize(payload),
                    extPartFB.deserialize(payload),
                    extPartWidth.deserialize(payload),
                    extPartAmbienceSendLevel.deserialize(payload),
                    extPartControlChannel.deserialize(payload),
                    0 // depth.interpret(startAddress.offsetBy(lsb = 0xC0u), length, payload),
                )
            }
        }

        class StudioSetMasterEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetMasterEq>() {
            override val size = Integra7Size(63u)

            val lowFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x00u),
                SupernaturalDrumLowFrequency::values
            )
            val lowGain = UByteField(deviceId, address.offsetBy(lsb = 0x01u), 0..30) // -15
            val midFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x02u),
                SupernaturalDrumMidFrequency::values
            )
            val midGain = UByteField(deviceId, address.offsetBy(lsb = 0x03u), 0..30) // -15
            val midQ =
                EnumField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumMidQ::values)
            val highFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x05u),
                SupernaturalDrumHighFrequency::values
            )
            val highGain = UByteField(deviceId, address.offsetBy(lsb = 0x06u), 0..30) // -15

            override fun deserialize(payload: SparseUByteArray): StudioSetMasterEq {
                assert(this.isCovering(payload)) { "Expected Studio-Set master-eq address ($address..${address.offsetBy(size)})" }

                return StudioSetMasterEq(
                    lowFrequency = lowFrequency.deserialize(payload),
                    lowGain = lowGain.deserialize(payload) - 15,
                    midFrequency = midFrequency.deserialize(payload),
                    midGain = midGain.deserialize(payload) - 15,
                    midQ = midQ.deserialize(payload),
                    highFrequency = highFrequency.deserialize(payload),
                    highGain = highGain.deserialize(payload) - 15,
                )
            }
        }

        class StudioSetPartBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetPart>() {
            override val size = Integra7Size(0x4Du)

            val receiveChannel = UByteField(deviceId, address.offsetBy(lsb = 0x00u), 0..30)
            val receiveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x01u))

            val toneBankMsb = UByteField(deviceId, address.offsetBy(lsb = 0x06u)) // CC#0, CC#32
            val toneBankLsb = UByteField(deviceId, address.offsetBy(lsb = 0x07u)) // CC#0, CC#32
            val toneProgramNumber = UByteField(deviceId, address.offsetBy(lsb = 0x08u))

            val level = UByteField(deviceId, address.offsetBy(lsb = 0x09u)) // CC#7
            val pan = ByteField(deviceId, address.offsetBy(lsb = 0x0Au), -64..63) // CC#10
            val coarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x0Bu), -48..48) // RPN#2
            val fineTune = ByteField(deviceId, address.offsetBy(lsb = 0x0Cu), -50..50) // RPN#1
            val monoPoly = EnumField(deviceId, address.offsetBy(lsb = 0x0Du), MonoPolyTone::values)
            val legatoSwitch =
                EnumField(deviceId, address.offsetBy(lsb = 0x0Eu), OffOnTone::values) // CC#68
            val pitchBendRange =
                UByteField(deviceId, address.offsetBy(lsb = 0x0Fu), 0..25) // RPN#0 - 0..24, TONE
            val portamentoSwitch =
                EnumField(deviceId, address.offsetBy(lsb = 0x10u), OffOnTone::values) // CC#65
            val portamentoTime = UnsignedMsbLsbNibbles(
                deviceId,
                address.offsetBy(lsb = 0x11u),
                0..128
            ) // CC#5 0..127, TONE
            val cutoffOffset = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -64..63) // CC#74
            val resonanceOffset =
                ByteField(deviceId, address.offsetBy(lsb = 0x14u), -64..63) // CC#71
            val attackTimeOffset =
                ByteField(deviceId, address.offsetBy(lsb = 0x15u), -64..63) // CC#73
            val decayTimeOffset =
                ByteField(deviceId, address.offsetBy(lsb = 0x16u), -64..63) // CC#75
            val releaseTimeOffset =
                ByteField(deviceId, address.offsetBy(lsb = 0x17u), -64..63) // CC#72
            val vibratoRate = ByteField(deviceId, address.offsetBy(lsb = 0x18u), -64..63) // CC#76
            val vibratoDepth = ByteField(deviceId, address.offsetBy(lsb = 0x19u), -64..63) // CC#77
            val vibratoDelay = ByteField(deviceId, address.offsetBy(lsb = 0x1Au), -64..63) // CC#78
            val octaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x1Bu), -3..3)
            val velocitySensOffset = ByteField(deviceId, address.offsetBy(lsb = 0x1Cu))
            val keyboardRange = UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x1Du))
            val keyboardFadeWidth = UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x1Fu))
            val velocityRange = UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x21u))
            val velocityFadeWidth = UnsignedRangeFields(deviceId, address.offsetBy(lsb = 0x23u))
            val muteSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x25u))

            val chorusSend = UByteField(deviceId, address.offsetBy(lsb = 0x27u)) // CC#93
            val reverbSend = UByteField(deviceId, address.offsetBy(lsb = 0x28u)) // CC#91
            val outputAssign =
                EnumField(deviceId, address.offsetBy(lsb = 0x29u), PartOutput::values)

            val scaleTuneType =
                EnumField(deviceId, address.offsetBy(lsb = 0x2Bu), ScaleTuneType::values)
            val scaleTuneKey = EnumField(deviceId, address.offsetBy(lsb = 0x2Cu), NoteKey::values)
            val scaleTuneC = ByteField(deviceId, address.offsetBy(lsb = 0x2Du), -64..63)
            val scaleTuneCSharp = ByteField(deviceId, address.offsetBy(lsb = 0x2Eu), -64..63)
            val scaleTuneD = ByteField(deviceId, address.offsetBy(lsb = 0x2Fu), -64..63)
            val scaleTuneDSharp = ByteField(deviceId, address.offsetBy(lsb = 0x30u), -64..63)
            val scaleTuneE = ByteField(deviceId, address.offsetBy(lsb = 0x31u), -64..63)
            val scaleTuneF = ByteField(deviceId, address.offsetBy(lsb = 0x32u), -64..63)
            val scaleTuneFSharp = ByteField(deviceId, address.offsetBy(lsb = 0x33u), -64..63)
            val scaleTuneG = ByteField(deviceId, address.offsetBy(lsb = 0x34u), -64..63)
            val scaleTuneGSharp = ByteField(deviceId, address.offsetBy(lsb = 0x35u), -64..63)
            val scaleTuneA = ByteField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
            val scaleTuneASharp = ByteField(deviceId, address.offsetBy(lsb = 0x37u), -64..63)
            val scaleTuneB = ByteField(deviceId, address.offsetBy(lsb = 0x38u), -64..63)

            val receiveProgramChange = BooleanField(deviceId, address.offsetBy(lsb = 0x39u))
            val receiveBankSelect = BooleanField(deviceId, address.offsetBy(lsb = 0x3Au))
            val receivePitchBend = BooleanField(deviceId, address.offsetBy(lsb = 0x3Bu))
            val receivePolyphonicKeyPressure = BooleanField(deviceId, address.offsetBy(lsb = 0x3Cu))
            val receiveChannelPressure = BooleanField(deviceId, address.offsetBy(lsb = 0x3Du))
            val receiveModulation = BooleanField(deviceId, address.offsetBy(lsb = 0x3Eu))
            val receiveVolume = BooleanField(deviceId, address.offsetBy(lsb = 0x3Fu))
            val receivePan = BooleanField(deviceId, address.offsetBy(lsb = 0x40u))
            val receiveExpression = BooleanField(deviceId, address.offsetBy(lsb = 0x41u))
            val receiveHold1 = BooleanField(deviceId, address.offsetBy(lsb = 0x42u))

            val velocityCurveType = UByteField(deviceId, address.offsetBy(lsb = 0x43u), 0..4)

            val motionalSurroundLR = ByteField(deviceId, address.offsetBy(lsb = 0x44u), -64..63)
            val motionalSurroundFB = ByteField(deviceId, address.offsetBy(lsb = 0x46u), -64..63)
            val motionalSurroundWidth = UByteField(deviceId, address.offsetBy(lsb = 0x48u), 0..32)
            val motionalSurroundAmbienceSend = UByteField(deviceId, address.offsetBy(lsb = 0x49u))

            override fun deserialize(payload: SparseUByteArray): StudioSetPart {
                assert(this.isCovering(payload)) { "Expected Studio-Set master-eq address ($address..${address.offsetBy(size)})" }

                return StudioSetPart(
                    receiveChannel.deserialize(payload),
                    receiveSwitch.deserialize(payload),

                    toneBankMsb.deserialize(payload),
                    toneBankLsb.deserialize(payload),
                    toneProgramNumber.deserialize(payload),

                    level.deserialize(payload),
                    pan.deserialize(payload),
                    coarseTune.deserialize(payload),
                    fineTune.deserialize(payload),
                    monoPoly.deserialize(payload),
                    legatoSwitch.deserialize(payload),
                    pitchBendRange.deserialize(payload),
                    portamentoSwitch.deserialize(payload),
                    portamentoTime.deserialize(payload),
                    cutoffOffset.deserialize(payload),
                    resonanceOffset.deserialize(payload),
                    attackTimeOffset.deserialize(payload),
                    decayTimeOffset.deserialize(payload),
                    releaseTimeOffset.deserialize(payload),
                    vibratoRate.deserialize(payload),
                    vibratoDepth.deserialize(payload),
                    vibratoDelay.deserialize(payload),
                    octaveShift.deserialize(payload),
                    velocitySensOffset.deserialize(payload),
                    keyboardRange.deserialize(payload),
                    keyboardFadeWidth.deserialize(payload),
                    velocityRange.deserialize(payload),
                    velocityFadeWidth.deserialize(payload),
                    muteSwitch.deserialize(payload),

                    chorusSend.deserialize(payload),
                    reverbSend.deserialize(payload),
                    outputAssign.deserialize(payload),

                    scaleTuneType.deserialize(payload),
                    scaleTuneKey.deserialize(payload),
                    scaleTuneC.deserialize(payload),
                    scaleTuneCSharp.deserialize(payload),
                    scaleTuneD.deserialize(payload),
                    scaleTuneDSharp.deserialize(payload),
                    scaleTuneE.deserialize(payload),
                    scaleTuneF.deserialize(payload),
                    scaleTuneFSharp.deserialize(payload),
                    scaleTuneG.deserialize(payload),
                    scaleTuneGSharp.deserialize(payload),
                    scaleTuneA.deserialize(payload),
                    scaleTuneASharp.deserialize(payload),
                    scaleTuneB.deserialize(payload),

                    receiveProgramChange.deserialize(payload),
                    receiveBankSelect.deserialize(payload),
                    receivePitchBend.deserialize(payload),
                    receivePolyphonicKeyPressure.deserialize(payload),
                    receiveChannelPressure.deserialize(payload),
                    receiveModulation.deserialize(payload),
                    receiveVolume.deserialize(payload),
                    receivePan.deserialize(payload),
                    receiveExpression.deserialize(payload),
                    receiveHold1.deserialize(payload),

                    velocityCurveType.deserialize(payload) + 1, // TODO

                    motionalSurroundLR.deserialize(payload),
                    motionalSurroundFB.deserialize(payload),
                    motionalSurroundWidth.deserialize(payload),
                    motionalSurroundAmbienceSend.deserialize(payload)
                )
            }
        }

        class StudioSetPartEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address): Integra7GlobalSysEx<StudioSetPartEq>() {
            override val size = Integra7Size(8u)

            val switch = BooleanField(deviceId, address.offsetBy(lsb = 0x00u))
            val lowFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x01u),
                SupernaturalDrumLowFrequency::values
            )
            val lowGain = UByteField(deviceId, address.offsetBy(lsb = 0x02u), 0..30) // -15
            val midFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x03u),
                SupernaturalDrumMidFrequency::values
            )
            val midGain = UByteField(deviceId, address.offsetBy(lsb = 0x04u), 0..30) // -15
            val midQ =
                EnumField(deviceId, address.offsetBy(lsb = 0x05u), SupernaturalDrumMidQ::values)
            val highFrequency = EnumField(
                deviceId,
                address.offsetBy(lsb = 0x06u),
                SupernaturalDrumHighFrequency::values
            )
            val highGain = UByteField(deviceId, address.offsetBy(lsb = 0x07u), 0..30) // -15

            override fun deserialize(payload: SparseUByteArray): StudioSetPartEq {
                assert(this.isCovering(payload)) { "Expected Studio-Set part-eq address ($address..${address.offsetBy(size)})" }

                return StudioSetPartEq(
                    switch = switch.deserialize(payload),
                    lowFrequency = lowFrequency.deserialize(payload),
                    lowGain = lowGain.deserialize(payload) - 15,
                    midFrequency = midFrequency.deserialize(payload),
                    midGain = midGain.deserialize(payload) - 15,
                    midQ = midQ.deserialize(payload),
                    highFrequency = highFrequency.deserialize(payload),
                    highGain = highGain.deserialize(payload) - 15,
                )
            }
        }
    }
}