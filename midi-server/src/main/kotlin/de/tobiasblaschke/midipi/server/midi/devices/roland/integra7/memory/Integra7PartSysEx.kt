package de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory

import de.tobiasblaschke.midipi.server.midi.bearable.lifted.DeviceId
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.Integra7MemoryIO
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.IntegraPart
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.domain.*
import de.tobiasblaschke.midipi.server.midi.devices.roland.integra7.memory.Integra7FieldType.*
import de.tobiasblaschke.midipi.server.utils.*
import java.lang.IllegalArgumentException

sealed class Integra7PartSysEx<T>: Integra7MemoryIO<T>() {
    abstract val part: IntegraPart

    class PcmSynth7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<PcmSynthTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x30u.toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = PcmSynthToneCommonBuilder(deviceId, address, part)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part)
        val partialMixTable = PcmSynthTonePartialMixTableBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u), part)
        val partial1 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u), part)
        val partial2 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x22u, lsb = 0x00u), part)
        val partial3 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x24u, lsb = 0x00u), part)
        val partial4 = PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x26u, lsb = 0x00u), part)
        val common2 = PcmSynthToneCommon2Builder(deviceId, address.offsetBy(mlsb = 0x30u, lsb = 0x00u), part)

        override fun interpret(startAddress: Integra7Address, payload: SparseUByteArray): PcmSynthTone {
            assert(this.isCovering(startAddress)) { "Not a PCM synth tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmSynthTone(
                common = common.interpret(startAddress, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), payload),
                partialMixTable = partialMixTable.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u), payload),
                partial1 = // if (payload.size >= startAddress.offsetBy(msb = 0x20u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                partial1.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u), payload), // else null,
                partial2 = // if (payload.size >= startAddress.offsetBy(msb = 0x22u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                partial2.interpret(startAddress.offsetBy(mlsb = 0x22u, lsb = 0x00u), payload), // else null,
                partial3 = // if (payload.size >= startAddress.offsetBy(msb = 0x24u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                partial3.interpret(startAddress.offsetBy(mlsb = 0x24u, lsb = 0x00u), payload), // else null,
                partial4 = // if (payload.size >= startAddress.offsetBy(msb = 0x26u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                partial4.interpret(startAddress.offsetBy(mlsb = 0x26u, lsb = 0x00u), payload), // else null,
                common2 = // if (payload.size >= startAddress.offsetBy(msb = 0x30u, lsb = 0x00u).offsetBy(partial1.size).fullByteAddress())
                common2.interpret(startAddress.offsetBy(mlsb = 0x30u, lsb = 0x00u), payload), //else null,
            )
        }
    }

    class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneCommon>() {
        override val size = Integra7Size(0x50u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val priority = EnumField(deviceId, address.offsetBy(lsb = 0x10u), Priority.values())
        val coarseTuning = ByteField(deviceId, address.offsetBy(lsb = 0x11u), -48..48)
        val fineTuning = ByteField(deviceId, address.offsetBy(lsb = 0x12u), -50..50)
        val ocataveShift = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val stretchTuneDepth = UByteField(deviceId, address.offsetBy(lsb = 0x14u), 0..3)
        val analogFeel = UByteField(deviceId, address.offsetBy(lsb = 0x15u))
        val monoPoly = EnumField(deviceId, address.offsetBy(lsb = 0x16u), MonoPoly.values())
        val legatoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x17u))
        val legatoRetrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x18u))
        val portamentoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x19u))
        val portamentoMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Au), PortamentoMode.values())
        val portamentoType =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Bu), PortamentoType.values())
        val portamentoStart =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), PortamentoStart.values())
        val portamentoTime = UByteField(deviceId, address.offsetBy(lsb = 0x1Du))

        val cutoffOffset = ByteField(deviceId, address.offsetBy(lsb = 0x22u))
        val resonanceOffset = ByteField(deviceId, address.offsetBy(lsb = 0x23u))
        val attackTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x24u))
        val releaseTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x25u))
        val velocitySensOffset = ByteField(deviceId, address.offsetBy(lsb = 0x26u))

        val pmtControlSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x28u))
        val pitchBendRangeUp = UByteField(deviceId, address.offsetBy(lsb = 0x29u), 0..48)
        val pitchBendRangeDown = UByteField(deviceId, address.offsetBy(lsb = 0x2Au), 0..48)

        val matrixControl1Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Bu), MatrixControlSource.values())
        val matrixControl1Destination1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Cu), MatrixControlDestination.values())
        val matrixControl1Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val matrixControl1Destination2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Eu), MatrixControlDestination.values())
        val matrixControl1Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x2Fu))
        val matrixControl1Destination3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x30u), MatrixControlDestination.values())
        val matrixControl1Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x31u))
        val matrixControl1Destination4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x32u), MatrixControlDestination.values())
        val matrixControl1Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x33u))

        val matrixControl2Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x34u), MatrixControlSource.values())
        val matrixControl2Destination1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x35u), MatrixControlDestination.values())
        val matrixControl2Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x36u))
        val matrixControl2Destination2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x37u), MatrixControlDestination.values())
        val matrixControl2Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x38u))
        val matrixControl2Destination3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x39u), MatrixControlDestination.values())
        val matrixControl2Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x3Au))
        val matrixControl2Destination4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Bu), MatrixControlDestination.values())
        val matrixControl2Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x3Cu))

        val matrixControl3Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Du), MatrixControlSource.values())
        val matrixControl3Destination1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Eu), MatrixControlDestination.values())
        val matrixControl3Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val matrixControl3Destination2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x40u), MatrixControlDestination.values())
        val matrixControl3Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x41u))
        val matrixControl3Destination3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x42u), MatrixControlDestination.values())
        val matrixControl3Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x43u))
        val matrixControl3Destination4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x44u), MatrixControlDestination.values())
        val matrixControl3Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x45u))

        val matrixControl4Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x46u), MatrixControlSource.values())
        val matrixControl4Destination1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x47u), MatrixControlDestination.values())
        val matrixControl4Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x48u))
        val matrixControl4Destination2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x49u), MatrixControlDestination.values())
        val matrixControl4Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x4Au))
        val matrixControl4Destination3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x4Bu), MatrixControlDestination.values())
        val matrixControl4Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val matrixControl4Destination4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x4Du), MatrixControlDestination.values())
        val matrixControl4Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x4Eu))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmSynthToneCommon {
            assert(startAddress >= address)

            try {
                return PcmSynthToneCommon(
                    name = name.interpret(startAddress, payload),
                    level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                    pan = pan.interpret(startAddress.offsetBy(lsb = 0x00Fu), payload),
                    priority = priority.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                    coarseTuning = coarseTuning.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                    fineTuning = fineTuning.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                    ocataveShift = ocataveShift.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                    stretchTuneDepth = stretchTuneDepth.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                    analogFeel = analogFeel.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                    monoPoly = monoPoly.interpret(startAddress.offsetBy(lsb = 0x16u), payload),
                    legatoSwitch = legatoSwitch.interpret(startAddress.offsetBy(lsb = 0x17u), payload),
                    legatoRetrigger = legatoRetrigger.interpret(startAddress.offsetBy(lsb = 0x18u), payload),
                    portamentoSwitch = portamentoSwitch.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                    portamentoMode = portamentoMode.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                    portamentoType = portamentoType.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload),
                    portamentoStart = portamentoStart.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                    portamentoTime = portamentoTime.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),

                    cutoffOffset = cutoffOffset.interpret(startAddress.offsetBy(lsb = 0x22u), payload),
                    resonanceOffset = resonanceOffset.interpret(startAddress.offsetBy(lsb = 0x23u), payload),
                    attackTimeOffset = attackTimeOffset.interpret(startAddress.offsetBy(lsb = 0x24u), payload),
                    releaseTimeOffset = releaseTimeOffset.interpret(startAddress.offsetBy(lsb = 0x25u), payload),
                    velocitySensOffset = velocitySensOffset.interpret(startAddress.offsetBy(lsb = 0x26u), payload),

                    pmtControlSwitch = pmtControlSwitch.interpret(startAddress.offsetBy(lsb = 0x28u), payload),
                    pitchBendRangeUp = pitchBendRangeUp.interpret(startAddress.offsetBy(lsb = 0x29u), payload),
                    pitchBendRangeDown = pitchBendRangeDown.interpret(startAddress.offsetBy(lsb = 0x2Au), payload),

                    matrixControl1Source = matrixControl1Source.interpret(startAddress.offsetBy(lsb = 0x2Bu), payload),
                    matrixControl1Destination1 = matrixControl1Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x2Cu),
                        payload
                    ),
                    matrixControl1Sens1 = matrixControl1Sens1.interpret(startAddress.offsetBy(lsb = 0x2Du), payload),
                    matrixControl1Destination2 = matrixControl1Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x2Eu),
                        payload
                    ),
                    matrixControl1Sens2 = matrixControl1Sens2.interpret(startAddress.offsetBy(lsb = 0x2Fu), payload),
                    matrixControl1Destination3 = matrixControl1Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x30u),
                        payload
                    ),
                    matrixControl1Sens3 = matrixControl1Sens3.interpret(startAddress.offsetBy(lsb = 0x31u), payload),
                    matrixControl1Destination4 = matrixControl1Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x32u), payload
                    ),
                    matrixControl1Sens4 = matrixControl1Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x33u), payload
                    ),

                    matrixControl2Source = matrixControl2Source.interpret(
                        startAddress.offsetBy(lsb = 0x34u), payload
                    ),
                    matrixControl2Destination1 = matrixControl2Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x35u), payload
                    ),
                    matrixControl2Sens1 = matrixControl2Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x36u), payload
                    ),
                    matrixControl2Destination2 = matrixControl2Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x37u), payload
                    ),
                    matrixControl2Sens2 = matrixControl2Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x38u), payload
                    ),
                    matrixControl2Destination3 = matrixControl2Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x39u), payload
                    ),
                    matrixControl2Sens3 = matrixControl2Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x3Au), payload
                    ),
                    matrixControl2Destination4 = matrixControl2Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x3Bu), payload
                    ),
                    matrixControl2Sens4 = matrixControl2Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x3Cu), payload
                    ),

                    matrixControl3Source = matrixControl3Source.interpret(
                        startAddress.offsetBy(lsb = 0x3Du), payload
                    ),
                    matrixControl3Destination1 = matrixControl3Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x3Eu), payload
                    ),
                    matrixControl3Sens1 = matrixControl3Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x3Fu), payload
                    ),
                    matrixControl3Destination2 = matrixControl3Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x40u), payload
                    ),
                    matrixControl3Sens2 = matrixControl3Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x41u), payload
                    ),
                    matrixControl3Destination3 = matrixControl3Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x42u), payload
                    ),
                    matrixControl3Sens3 = matrixControl3Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x43u), payload
                    ),
                    matrixControl3Destination4 = matrixControl3Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x44u), payload
                    ),
                    matrixControl3Sens4 = matrixControl3Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x45u), payload
                    ),

                    matrixControl4Source = matrixControl4Source.interpret(
                        startAddress.offsetBy(lsb = 0x46u), payload
                    ),
                    matrixControl4Destination1 = matrixControl4Destination1.interpret(
                        startAddress.offsetBy(lsb = 0x47u), payload
                    ),
                    matrixControl4Sens1 = matrixControl4Sens1.interpret(
                        startAddress.offsetBy(lsb = 0x48u), payload
                    ),
                    matrixControl4Destination2 = matrixControl4Destination2.interpret(
                        startAddress.offsetBy(lsb = 0x49u), payload
                    ),
                    matrixControl4Sens2 = matrixControl4Sens2.interpret(
                        startAddress.offsetBy(lsb = 0x4Au), payload
                    ),
                    matrixControl4Destination3 = matrixControl4Destination3.interpret(
                        startAddress.offsetBy(lsb = 0x4Bu), payload
                    ),
                    matrixControl4Sens3 = matrixControl4Sens3.interpret(
                        startAddress.offsetBy(lsb = 0x4Cu), payload
                    ),
                    matrixControl4Destination4 = matrixControl4Destination4.interpret(
                        startAddress.offsetBy(lsb = 0x4Du), payload
                    ),
                    matrixControl4Sens4 = matrixControl4Sens4.interpret(
                        startAddress.offsetBy(lsb = 0x4Eu), payload
                    ),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(
                    "When reading $address size $size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    class PcmSynthToneMfxBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneMfx>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x11u.toUByte7()))

        val mfxType = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val mfxChorusSend = UByteField(deviceId, address.offsetBy(lsb = 0x02u))
        val mfxReverbSend = UByteField(deviceId, address.offsetBy(lsb = 0x03u))

        val mfxControl1Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x05u), MfxControlSource.values())
        val mfxControl1Sens = ByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val mfxControl2Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x07u), MfxControlSource.values())
        val mfxControl2Sens = ByteField(deviceId, address.offsetBy(lsb = 0x08u))
        val mfxControl3Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x09u), MfxControlSource.values())
        val mfxControl3Sens = ByteField(deviceId, address.offsetBy(lsb = 0x0Au))
        val mfxControl4Source =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), MfxControlSource.values())
        val mfxControl4Sens = ByteField(deviceId, address.offsetBy(lsb = 0x0Cu))

        val mfxControlAssign1 = UByteField(deviceId, address.offsetBy(lsb = 0x0Du))
        val mfxControlAssign2 = UByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val mfxControlAssign3 = UByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val mfxControlAssign4 = UByteField(deviceId, address.offsetBy(lsb = 0x10u))

        val mfxParameter1 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x11u))
        val mfxParameter2 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x15u))
        val mfxParameter3 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x19u))
        val mfxParameter4 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x1Du))
        val mfxParameter5 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x21u))
        val mfxParameter6 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x25u))
        val mfxParameter7 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x29u))
        val mfxParameter8 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Du))
        val mfxParameter9 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x31u))
        val mfxParameter10 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u))
        val mfxParameter11 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x39u))
        val mfxParameter12 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x3Du))
        val mfxParameter13 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x41u))
        val mfxParameter14 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x45u))
        val mfxParameter15 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x49u))
        val mfxParameter16 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x4Du))
        val mfxParameter17 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x51u))
        val mfxParameter18 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x55u))
        val mfxParameter19 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x59u))
        val mfxParameter20 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du))
        val mfxParameter21 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u))
        val mfxParameter22 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u))
        val mfxParameter23 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x69u))
        val mfxParameter24 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x6Du))
        val mfxParameter25 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x71u))
        val mfxParameter26 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x75u))
        val mfxParameter27 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x79u))
        val mfxParameter28 = SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Du))
        val mfxParameter29 =
            SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u))
        val mfxParameter30 =
            SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val mfxParameter31 =
            SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u))
        val mfxParameter32 =
            SignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmSynthToneMfx {
            assert(this.isCovering(startAddress)) { "Not a MFX definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()} in  ${payload.hexDump({ Integra7Address(
                it.toUInt7()
            ).toString() }, 0x10)}" }

            try {
            return PcmSynthToneMfx(
                mfxType = mfxType.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                mfxChorusSend = mfxChorusSend.interpret(startAddress.offsetBy(lsb = 0x02u), payload),
                mfxReverbSend = mfxReverbSend.interpret(startAddress.offsetBy(lsb = 0x03u), payload),

                mfxControl1Source = mfxControl1Source.interpret(startAddress.offsetBy(lsb = 0x05u), payload),
                mfxControl1Sens = mfxControl1Sens.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                mfxControl2Source = mfxControl2Source.interpret(startAddress.offsetBy(lsb = 0x07u), payload),
                mfxControl2Sens = mfxControl2Sens.interpret(startAddress.offsetBy(lsb = 0x08u), payload),
                mfxControl3Source = mfxControl3Source.interpret(startAddress.offsetBy(lsb = 0x09u), payload),
                mfxControl3Sens = mfxControl3Sens.interpret(startAddress.offsetBy(lsb = 0x0Au), payload),
                mfxControl4Source = mfxControl4Source.interpret(startAddress.offsetBy(lsb = 0x0Bu), payload),
                mfxControl4Sens = mfxControl4Sens.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),

                mfxControlAssign1 = mfxControlAssign1.interpret(startAddress.offsetBy(lsb = 0x0Du), payload),
                mfxControlAssign2 = mfxControlAssign2.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                mfxControlAssign3 = mfxControlAssign3.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                mfxControlAssign4 = mfxControlAssign4.interpret(startAddress.offsetBy(lsb = 0x10u), payload),

                mfxParameter1 = mfxParameter1.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                mfxParameter2 = mfxParameter2.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                mfxParameter3 = mfxParameter3.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                mfxParameter4 = mfxParameter4.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),
                mfxParameter5 = mfxParameter5.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                mfxParameter6 = mfxParameter6.interpret(startAddress.offsetBy(lsb = 0x25u), payload),
                mfxParameter7 = mfxParameter7.interpret(startAddress.offsetBy(lsb = 0x29u), payload),
                mfxParameter8 = mfxParameter8.interpret(startAddress.offsetBy(lsb = 0x2Du), payload),
                mfxParameter9 = mfxParameter9.interpret(startAddress.offsetBy(lsb = 0x31u), payload),

                mfxParameter10 = mfxParameter10.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                mfxParameter11 = mfxParameter11.interpret(startAddress.offsetBy(lsb = 0x39u), payload),
                mfxParameter12 = mfxParameter12.interpret(startAddress.offsetBy(lsb = 0x3Du), payload),
                mfxParameter13 = mfxParameter13.interpret(startAddress.offsetBy(lsb = 0x41u), payload),
                mfxParameter14 = mfxParameter14.interpret(startAddress.offsetBy(lsb = 0x45u), payload),
                mfxParameter15 = mfxParameter15.interpret(startAddress.offsetBy(lsb = 0x49u), payload),
                mfxParameter16 = mfxParameter16.interpret(startAddress.offsetBy(lsb = 0x4Du), payload),
                mfxParameter17 = mfxParameter17.interpret(startAddress.offsetBy(lsb = 0x51u), payload),
                mfxParameter18 = mfxParameter18.interpret(startAddress.offsetBy(lsb = 0x55u), payload),
                mfxParameter19 = mfxParameter19.interpret(startAddress.offsetBy(lsb = 0x59u), payload),

                mfxParameter20 = mfxParameter20.interpret(startAddress.offsetBy(lsb = 0x5Du), payload),
                mfxParameter21 = mfxParameter21.interpret(startAddress.offsetBy(lsb = 0x61u), payload),
                mfxParameter22 = mfxParameter22.interpret(startAddress.offsetBy(lsb = 0x65u), payload),
                mfxParameter23 = mfxParameter23.interpret(startAddress.offsetBy(lsb = 0x69u), payload),
                mfxParameter24 = mfxParameter24.interpret(startAddress.offsetBy(lsb = 0x6Du), payload),
                mfxParameter25 = mfxParameter25.interpret(startAddress.offsetBy(lsb = 0x71u), payload),
                mfxParameter26 = mfxParameter26.interpret(startAddress.offsetBy(lsb = 0x75u), payload),
                mfxParameter27 = mfxParameter27.interpret(startAddress.offsetBy(lsb = 0x79u), payload),
                mfxParameter28 = mfxParameter28.interpret(startAddress.offsetBy(lsb = 0x7Du), payload),
                mfxParameter29 = mfxParameter29.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x01u), payload),

                mfxParameter30 = mfxParameter30.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x05u), payload),
                mfxParameter31 = mfxParameter31.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u), payload),
                mfxParameter32 = 0 // mfxParameter32.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), length, payload),  // TODO!!
            )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(
                    "When reading $address size $size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    class PcmSynthTonePartialMixTableBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthTonePartialMixTable>() {
        override val size = Integra7Size(0x29u.toUInt7UsingValue())

        val structureType12 = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val booster12 = UByteField(deviceId, address.offsetBy(lsb = 0x01u)) // ENUM
        val structureType34 = UByteField(deviceId, address.offsetBy(lsb = 0x02u))
        val booster34 = UByteField(deviceId, address.offsetBy(lsb = 0x03u)) // ENUM

        val velocityControl =
            EnumField(deviceId, address.offsetBy(lsb = 0x04u), VelocityControl.values())

        val pmt1PartialSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x05u))
        val pmt1KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x06u))
        val pmt1KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x08u))
        val pmt1VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Au))
        val pmt1VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Cu))

        val pmt2PartialSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pmt2KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x0Fu))
        val pmt2KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x11u))
        val pmt2VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x13u))
        val pmt2VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x15u))

        val pmt3PartialSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x17u))
        val pmt3KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x18u))
        val pmt3KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Au))
        val pmt3VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Cu))
        val pmt3VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x1Eu))

        val pmt4PartialSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x20u))
        val pmt4KeyboardRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x21u))
        val pmt4KeyboardFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x23u))
        val pmt4VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x25u))
        val pmt4VelocityFade = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x27u))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmSynthTonePartialMixTable {
            assert(startAddress >= address)

            try {
                return PcmSynthTonePartialMixTable(
                    structureType12 = structureType12.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                    booster12 = booster12.interpret(startAddress.offsetBy(lsb = 0x01u), payload),
                    structureType34 = structureType34.interpret(startAddress.offsetBy(lsb = 0x02u), payload),
                    booster34 = booster34.interpret(startAddress.offsetBy(lsb = 0x03u), payload),

                    velocityControl = velocityControl.interpret(startAddress.offsetBy(lsb = 0x04u), payload),

                    pmt1PartialSwitch = pmt1PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x05u), payload),
                    pmt1KeyboardRange = pmt1KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                    pmt1KeyboardFadeWidth = pmt1KeyboardFadeWidth.interpret(
                        startAddress.offsetBy(lsb = 0x08u),
                        payload
                    ),
                    pmt1VelocityRange = pmt1VelocityRange.interpret(startAddress.offsetBy(lsb = 0x0Au), payload),
                    pmt1VelocityFade = pmt1VelocityFade.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),

                    pmt2PartialSwitch = pmt2PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                    pmt2KeyboardRange = pmt2KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                    pmt2KeyboardFadeWidth = pmt2KeyboardFadeWidth.interpret(
                        startAddress.offsetBy(lsb = 0x11u),
                        payload
                    ),
                    pmt2VelocityRange = pmt2VelocityRange.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                    pmt2VelocityFade = pmt2VelocityFade.interpret(startAddress.offsetBy(lsb = 0x15u), payload),

                    pmt3PartialSwitch = pmt3PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x17u), payload),
                    pmt3KeyboardRange = pmt3KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x18u), payload),
                    pmt3KeyboardFadeWidth = pmt3KeyboardFadeWidth.interpret(
                        startAddress.offsetBy(lsb = 0x1Au),
                        payload
                    ),
                    pmt3VelocityRange = pmt3VelocityRange.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                    pmt3VelocityFade = pmt3VelocityFade.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),

                    pmt4PartialSwitch = pmt4PartialSwitch.interpret(startAddress.offsetBy(lsb = 0x20u), payload),
                    pmt4KeyboardRange = pmt4KeyboardRange.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                    pmt4KeyboardFadeWidth = pmt4KeyboardFadeWidth.interpret(
                        startAddress.offsetBy(lsb = 0x23u),
                        payload
                    ),
                    pmt4VelocityRange = pmt4VelocityRange.interpret(startAddress.offsetBy(lsb = 0x25u), payload),
                    pmt4VelocityFade = 0 // TODO = pmt4VelocityFade.interpret(startAddress.offsetBy(lsb = 0x27u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(
                    "When reading $address size $size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    class PcmSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthTonePartial>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01.toUByte7(), lsb = 0x1Au.toUByte7()))

        val level = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusTune = ByteField(deviceId, address.offsetBy(lsb = 0x01u), -48..48)
        val fineTune = ByteField(deviceId, address.offsetBy(lsb = 0x02u), -50..50)
        val randomPithDepth =
            EnumField(deviceId, address.offsetBy(lsb = 0x03u), RandomPithDepth.values())
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x04u))
        // TODO val panKeyFollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x05u), -100..100)
        val panDepth = UByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val alternatePanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x07u))
        val envMode = EnumField(deviceId, address.offsetBy(lsb = 0x08u), EnvMode.values())
        val delayMode = EnumField(deviceId, address.offsetBy(lsb = 0x09u), DelayMode.values())
        val delayTime = UnsignedMsbLsbNibbles(
            deviceId,
            address.offsetBy(lsb = 0x0Au),
            0..149
        ) // TODO: 0 - 127, MUSICAL-NOTES |

        val outputLevel = ByteField(deviceId, address.offsetBy(lsb = 0x0Cu))
        val chorusSendLevel = ByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val reverbSendLevel = ByteField(deviceId, address.offsetBy(lsb = 0x10u))

        val receiveBender = BooleanField(deviceId, address.offsetBy(lsb = 0x12u))
        val receiveExpression = BooleanField(deviceId, address.offsetBy(lsb = 0x13u))
        val receiveHold1 = BooleanField(deviceId, address.offsetBy(lsb = 0x14u))
        val redamper = BooleanField(deviceId, address.offsetBy(lsb = 0x16u))

        val partialControl1Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x17u), OffOnReverse.values())
        val partialControl1Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x18u), OffOnReverse.values())
        val partialControl1Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x19u), OffOnReverse.values())
        val partialControl1Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Au), OffOnReverse.values())
        val partialControl2Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Bu), OffOnReverse.values())
        val partialControl2Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), OffOnReverse.values())
        val partialControl2Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Du), OffOnReverse.values())
        val partialControl2Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Eu), OffOnReverse.values())
        val partialControl3Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), OffOnReverse.values())
        val partialControl3Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), OffOnReverse.values())
        val partialControl3Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x21u), OffOnReverse.values())
        val partialControl3Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x22u), OffOnReverse.values())
        val partialControl4Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x23u), OffOnReverse.values())
        val partialControl4Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x24u), OffOnReverse.values())
        val partialControl4Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x25u), OffOnReverse.values())
        val partialControl4Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x26u), OffOnReverse.values())

        val waveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x27u), WaveGroupType.values())
        val waveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x28u), 0..16384)
        val waveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Cu), 0..16384)
        val waveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x30u), 0..16384)
        val waveGain = EnumField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveFXMSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x35u))
        val waveFXMColor = UByteField(deviceId, address.offsetBy(lsb = 0x36u), 0..3)
        val waveFXMDepth = UByteField(deviceId, address.offsetBy(lsb = 0x37u), 0..16)
        val waveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x38u))
        // TODO val wavePitchKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x37u), -200..200)

        val pitchEnvDepth = ByteField(deviceId, address.offsetBy(lsb = 0x3Au), -12..12)
        val pitchEnvVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val pitchEnvTime1VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x3Cu))
        val pitchEnvTime4VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x3Du))
        // TODO val pitchEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Eu), -100..100)
        val pitchEnvTime1 = UByteField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val pitchEnvTime2 = UByteField(deviceId, address.offsetBy(lsb = 0x40u))
        val pitchEnvTime3 = UByteField(deviceId, address.offsetBy(lsb = 0x41u))
        val pitchEnvTime4 = UByteField(deviceId, address.offsetBy(lsb = 0x42u))
        val pitchEnvLevel0 = ByteField(deviceId, address.offsetBy(lsb = 0x43u))
        val pitchEnvLevel1 = ByteField(deviceId, address.offsetBy(lsb = 0x44u))
        val pitchEnvLevel2 = ByteField(deviceId, address.offsetBy(lsb = 0x45u))
        val pitchEnvLevel3 = ByteField(deviceId, address.offsetBy(lsb = 0x46u))
        val pitchEnvLevel4 = ByteField(deviceId, address.offsetBy(lsb = 0x47u))

        val tvfFilterType =
            EnumField(deviceId, address.offsetBy(lsb = 0x48u), TvfFilterType.values())
        val tvfCutoffFrequency = UByteField(deviceId, address.offsetBy(lsb = 0x49u))
        // TODO val tvfCutoffKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x4Au), -200..200)
        val tvfCutoffVelocityCurve = UByteField(deviceId, address.offsetBy(lsb = 0x4Bu), 0..7)
        val tvfCutoffVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val tvfResonance = UByteField(deviceId, address.offsetBy(lsb = 0x4Du))
        val tvfResonanceVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x4Eu))
        val tvfEnvDepth = ByteField(deviceId, address.offsetBy(lsb = 0x4Fu))
        val tvfEnvVelocityCurve = UByteField(deviceId, address.offsetBy(lsb = 0x50u), 0..7)
        val tvfEnvVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x51u))
        val tvfEnvTime1VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x52u))
        val tvfEnvTime4VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x53u))
        // TODO val tvfEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x54u), -100..100)
        val tvfEnvTime1 = UByteField(deviceId, address.offsetBy(lsb = 0x55u))
        val tvfEnvTime2 = UByteField(deviceId, address.offsetBy(lsb = 0x56u))
        val tvfEnvTime3 = UByteField(deviceId, address.offsetBy(lsb = 0x57u))
        val tvfEnvTime4 = UByteField(deviceId, address.offsetBy(lsb = 0x58u))
        val tvfEnvLevel0 = UByteField(deviceId, address.offsetBy(lsb = 0x59u))
        val tvfEnvLevel1 = UByteField(deviceId, address.offsetBy(lsb = 0x5Au))
        val tvfEnvLevel2 = UByteField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val tvfEnvLevel3 = UByteField(deviceId, address.offsetBy(lsb = 0x5Cu))
        val tvfEnvLevel4 = UByteField(deviceId, address.offsetBy(lsb = 0x5Du))

        // TODO val biasLevel = SignedValueField(deviceId, address.offsetBy(lsb = 0x5Eu), -100..100)
        val biasPosition = UByteField(deviceId, address.offsetBy(lsb = 0x5Fu))
        val biasDirection =
            EnumField(deviceId, address.offsetBy(lsb = 0x60u), BiasDirection.values())
        val tvaLevelVelocityCurve = UByteField(deviceId, address.offsetBy(lsb = 0x61u), 0..7)
        val tvaLevelVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x62u))
        val tvaEnvTime1VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x63u))
        val tvaEnvTime4VelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x64u))
        // TODO val tvaEnvTimeKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x65u), -100.100)
        val tvaEnvTime1 = UByteField(deviceId, address.offsetBy(lsb = 0x66u))
        val tvaEnvTime2 = UByteField(deviceId, address.offsetBy(lsb = 0x67u))
        val tvaEnvTime3 = UByteField(deviceId, address.offsetBy(lsb = 0x68u))
        val tvaEnvTime4 = UByteField(deviceId, address.offsetBy(lsb = 0x69u))
        val tvaEnvLevel1 = UByteField(deviceId, address.offsetBy(lsb = 0x6Au))
        val tvaEnvLevel2 = UByteField(deviceId, address.offsetBy(lsb = 0x6Bu))
        val tvaEnvLevel3 = UByteField(deviceId, address.offsetBy(lsb = 0x6Cu))

        val lfo1WaveForm =
            EnumField(deviceId, address.offsetBy(lsb = 0x6Du), LfoWaveForm.values())
        val lfo1Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x6Eu), 0..149)
        val lfo1Offset = EnumField(deviceId, address.offsetBy(lsb = 0x70u), LfoOffset.values())
        val lfo1RateDetune = UByteField(deviceId, address.offsetBy(lsb = 0x71u))
        val lfo1DelayTime = UByteField(deviceId, address.offsetBy(lsb = 0x72u))
        // TODO val lfo1Keyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x73u), -100..100)
        val lfo1FadeMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x74u), LfoFadeMode.values())
        val lfo1FadeTime = UByteField(deviceId, address.offsetBy(lsb = 0x75u))
        val lfo1KeyTrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x76u))
        val lfo1PitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x77u))
        val lfo1TvfDepth = ByteField(deviceId, address.offsetBy(lsb = 0x78u))
        val lfo1TvaDepth = ByteField(deviceId, address.offsetBy(lsb = 0x79u))
        val lfo1PanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x7Au))

        val lfo2WaveForm =
            EnumField(deviceId, address.offsetBy(lsb = 0x7Bu), LfoWaveForm.values())
        val lfo2Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x7Cu), 0..149)
        val lfo2Offset = EnumField(deviceId, address.offsetBy(lsb = 0x7Eu), LfoOffset.values())
        val lfo2RateDetune = UByteField(deviceId, address.offsetBy(lsb = 0x7Fu))
        val lfo2DelayTime = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x00u))
        // TODO val lfo2Keyfollow = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u), -100..100)
        val lfo2FadeMode = EnumField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x02u),
            LfoFadeMode.values()
        )
        val lfo2FadeTime = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x03u))
        val lfo2KeyTrigger = BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x04u))
        val lfo2PitchDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val lfo2TvfDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u))
        val lfo2TvaDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val lfo2PanDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u))

        val lfoStepType =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..1)
        val lfoStep1 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au), -36..36)
        val lfoStep2 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -36..36)
        val lfoStep3 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -36..36)
        val lfoStep4 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -36..36)
        val lfoStep5 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), -36..36)
        val lfoStep6 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), -36..36)
        val lfoStep7 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u), -36..36)
        val lfoStep8 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u), -36..36)
        val lfoStep9 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x12u), -36..36)
        val lfoStep10 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u), -36..36)
        val lfoStep11 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x14u), -36..36)
        val lfoStep12 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -36..36)
        val lfoStep13 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u), -36..36)
        val lfoStep14 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u), -36..36)
        val lfoStep15 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u), -36..36)
        val lfoStep16 =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u), -36..36)

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmSynthTonePartial {
            assert(startAddress >= address)

            try {
                return PcmSynthTonePartial(
                    level = level.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                    chorusTune = chorusTune.interpret(startAddress.offsetBy(lsb = 0x01u), payload),
                    fineTune = fineTune.interpret(startAddress.offsetBy(lsb = 0x02u), payload),
                    randomPithDepth = randomPithDepth.interpret(startAddress.offsetBy(lsb = 0x03u), payload),
                    pan = pan.interpret(startAddress.offsetBy(lsb = 0x04u), payload),
                    // panKeyFollow = panKeyFollow.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                    panDepth = panDepth.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                    alternatePanDepth = alternatePanDepth.interpret(startAddress.offsetBy(lsb = 0x07u), payload),
                    envMode = envMode.interpret(startAddress.offsetBy(lsb = 0x08u), payload),
                    delayMode = delayMode.interpret(startAddress.offsetBy(lsb = 0x09u), payload),
                    delayTime = delayTime.interpret(startAddress.offsetBy(lsb = 0x0Au), payload),

                    outputLevel = outputLevel.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
                    chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                    reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x10u), payload),

                    receiveBender = receiveBender.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                    receiveExpression = receiveExpression.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                    receiveHold1 = receiveHold1.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                    redamper = redamper.interpret(startAddress.offsetBy(lsb = 0x16u), payload),

                    partialControl1Switch1 = partialControl1Switch1.interpret(
                        startAddress.offsetBy(lsb = 0x17u),
                        payload
                    ),
                    partialControl1Switch2 = partialControl1Switch2.interpret(
                        startAddress.offsetBy(lsb = 0x18u),
                        payload
                    ),
                    partialControl1Switch3 = partialControl1Switch3.interpret(
                        startAddress.offsetBy(lsb = 0x19u),
                        payload
                    ),
                    partialControl1Switch4 = partialControl1Switch4.interpret(
                        startAddress.offsetBy(lsb = 0x1Au),
                        payload
                    ),
                    partialControl2Switch1 = partialControl2Switch1.interpret(
                        startAddress.offsetBy(lsb = 0x1Bu),
                        payload
                    ),
                    partialControl2Switch2 = partialControl2Switch2.interpret(
                        startAddress.offsetBy(lsb = 0x1Cu),
                        payload
                    ),
                    partialControl2Switch3 = partialControl2Switch3.interpret(
                        startAddress.offsetBy(lsb = 0x1Du),
                        payload
                    ),
                    partialControl2Switch4 = partialControl2Switch4.interpret(
                        startAddress.offsetBy(lsb = 0x1Eu),
                        payload
                    ),
                    partialControl3Switch1 = partialControl3Switch1.interpret(
                        startAddress.offsetBy(lsb = 0x1Fu),
                        payload
                    ),
                    partialControl3Switch2 = partialControl3Switch2.interpret(
                        startAddress.offsetBy(lsb = 0x20u),
                        payload
                    ),
                    partialControl3Switch3 = partialControl3Switch3.interpret(
                        startAddress.offsetBy(lsb = 0x21u),
                        payload
                    ),
                    partialControl3Switch4 = partialControl3Switch4.interpret(
                        startAddress.offsetBy(lsb = 0x22u),
                        payload
                    ),
                    partialControl4Switch1 = partialControl4Switch1.interpret(
                        startAddress.offsetBy(lsb = 0x23u),
                        payload
                    ),
                    partialControl4Switch2 = partialControl4Switch2.interpret(
                        startAddress.offsetBy(lsb = 0x24u),
                        payload
                    ),
                    partialControl4Switch3 = partialControl4Switch3.interpret(
                        startAddress.offsetBy(lsb = 0x25u),
                        payload
                    ),
                    partialControl4Switch4 = partialControl4Switch4.interpret(
                        startAddress.offsetBy(lsb = 0x26u),
                        payload
                    ),

                    waveGroupType = waveGroupType.interpret(startAddress.offsetBy(lsb = 0x27u), payload),
                    waveGroupId = waveGroupId.interpret(startAddress.offsetBy(lsb = 0x28u), payload),
                    waveNumberL = waveNumberL.interpret(startAddress.offsetBy(lsb = 0x2Cu), payload),
                    waveNumberR = waveNumberR.interpret(startAddress.offsetBy(lsb = 0x30u), payload),
                    waveGain = waveGain.interpret(startAddress.offsetBy(lsb = 0x34u), payload),
                    waveFXMSwitch = waveFXMSwitch.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                    waveFXMColor = waveFXMColor.interpret(startAddress.offsetBy(lsb = 0x36u), payload),
                    waveFXMDepth = waveFXMDepth.interpret(startAddress.offsetBy(lsb = 0x37u), payload),
                    waveTempoSync = waveTempoSync.interpret(startAddress.offsetBy(lsb = 0x38u), payload),
                    // wavePitchKeyfollow = wavePitchKeyfollow.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),

                    pitchEnvDepth = pitchEnvDepth.interpret(startAddress.offsetBy(lsb = 0x3Au), payload),
                    pitchEnvVelocitySens = pitchEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x3Bu), payload),
                    pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x3Cu),
                        payload
                    ),
                    pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x3Du),
                        payload
                    ),
                    // pitchEnvTimeKeyfollow = pitchEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                    pitchEnvTime1 = pitchEnvTime1.interpret(startAddress.offsetBy(lsb = 0x3Fu), payload),
                    pitchEnvTime2 = pitchEnvTime2.interpret(startAddress.offsetBy(lsb = 0x40u), payload),
                    pitchEnvTime3 = pitchEnvTime3.interpret(startAddress.offsetBy(lsb = 0x41u), payload),
                    pitchEnvTime4 = pitchEnvTime4.interpret(startAddress.offsetBy(lsb = 0x42u), payload),
                    pitchEnvLevel0 = pitchEnvLevel0.interpret(startAddress.offsetBy(lsb = 0x43u), payload),
                    pitchEnvLevel1 = pitchEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x44u), payload),
                    pitchEnvLevel2 = pitchEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x45u), payload),
                    pitchEnvLevel3 = pitchEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x46u), payload),
                    pitchEnvLevel4 = pitchEnvLevel4.interpret(startAddress.offsetBy(lsb = 0x47u), payload),

                    tvfFilterType = tvfFilterType.interpret(startAddress.offsetBy(lsb = 0x48u), payload),
                    tvfCutoffFrequency = tvfCutoffFrequency.interpret(startAddress.offsetBy(lsb = 0x49u), payload),
                    // tvfCutoffKeyfollow = tvfCutoffKeyfollow.interpret(startAddress.offsetBy(lsb = 0x4Au), length, payload),
                    tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.interpret(
                        startAddress.offsetBy(lsb = 0x4Bu),
                        payload
                    ),
                    tvfCutoffVelocitySens = tvfCutoffVelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x4Cu),
                        payload
                    ),
                    tvfResonance = tvfResonance.interpret(startAddress.offsetBy(lsb = 0x4Du), payload),
                    tvfResonanceVelocitySens = tvfResonanceVelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x4Eu),
                        payload
                    ),
                    tvfEnvDepth = tvfEnvDepth.interpret(startAddress.offsetBy(lsb = 0x4Fu), payload),
                    tvfEnvVelocityCurve = tvfEnvVelocityCurve.interpret(startAddress.offsetBy(lsb = 0x50u), payload),
                    tvfEnvVelocitySens = tvfEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x51u), payload),
                    tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x52u),
                        payload
                    ),
                    tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x53u),
                        payload
                    ),
                    // tvfEnvTimeKeyfollow = tvfEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x54u), length, payload),
                    tvfEnvTime1 = tvfEnvTime1.interpret(startAddress.offsetBy(lsb = 0x55u), payload),
                    tvfEnvTime2 = tvfEnvTime2.interpret(startAddress.offsetBy(lsb = 0x56u), payload),
                    tvfEnvTime3 = tvfEnvTime3.interpret(startAddress.offsetBy(lsb = 0x57u), payload),
                    tvfEnvTime4 = tvfEnvTime4.interpret(startAddress.offsetBy(lsb = 0x58u), payload),
                    tvfEnvLevel0 = tvfEnvLevel0.interpret(startAddress.offsetBy(lsb = 0x59u), payload),
                    tvfEnvLevel1 = tvfEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x5Au), payload),
                    tvfEnvLevel2 = tvfEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x5Bu), payload),
                    tvfEnvLevel3 = tvfEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x5Cu), payload),
                    tvfEnvLevel4 = tvfEnvLevel4.interpret(startAddress.offsetBy(lsb = 0x5Du), payload),

                    //biasLevel = biasLevel.interpret(startAddress.offsetBy(lsb = 0x5Eu), length, payload),
                    biasPosition = biasPosition.interpret(startAddress.offsetBy(lsb = 0x5Fu), payload),
                    biasDirection = biasDirection.interpret(startAddress.offsetBy(lsb = 0x60u), payload),
                    tvaLevelVelocityCurve = tvaLevelVelocityCurve.interpret(
                        startAddress.offsetBy(lsb = 0x61u),
                        payload
                    ),
                    tvaLevelVelocitySens = tvaLevelVelocitySens.interpret(startAddress.offsetBy(lsb = 0x62u), payload),
                    tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x63u),
                        payload
                    ),
                    tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.interpret(
                        startAddress.offsetBy(lsb = 0x64u),
                        payload
                    ),
                    // tvaEnvTimeKeyfollow = tvaEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x65u), length, payload),
                    tvaEnvTime1 = tvaEnvTime1.interpret(startAddress.offsetBy(lsb = 0x66u), payload),
                    tvaEnvTime2 = tvaEnvTime2.interpret(startAddress.offsetBy(lsb = 0x67u), payload),
                    tvaEnvTime3 = tvaEnvTime3.interpret(startAddress.offsetBy(lsb = 0x68u), payload),
                    tvaEnvTime4 = tvaEnvTime4.interpret(startAddress.offsetBy(lsb = 0x69u), payload),
                    tvaEnvLevel1 = tvaEnvLevel1.interpret(startAddress.offsetBy(lsb = 0x6Au), payload),
                    tvaEnvLevel2 = tvaEnvLevel2.interpret(startAddress.offsetBy(lsb = 0x6Bu), payload),
                    tvaEnvLevel3 = tvaEnvLevel3.interpret(startAddress.offsetBy(lsb = 0x6Cu), payload),

                    lfo1WaveForm = lfo1WaveForm.interpret(startAddress.offsetBy(lsb = 0x6Du), payload),
                    lfo1Rate = lfo1Rate.interpret(startAddress.offsetBy(lsb = 0x6Eu), payload),
                    lfo1Offset = lfo1Offset.interpret(startAddress.offsetBy(lsb = 0x70u), payload),
                    lfo1RateDetune = lfo1RateDetune.interpret(startAddress.offsetBy(lsb = 0x71u), payload),
                    lfo1DelayTime = lfo1DelayTime.interpret(startAddress.offsetBy(lsb = 0x72u), payload),
                    // lfo1Keyfollow = lfo1Keyfollow.interpret(startAddress.offsetBy(lsb = 0x73u), length, payload),
                    lfo1FadeMode = lfo1FadeMode.interpret(startAddress.offsetBy(lsb = 0x74u), payload),
                    lfo1FadeTime = lfo1FadeTime.interpret(startAddress.offsetBy(lsb = 0x75u), payload),
                    lfo1KeyTrigger = lfo1KeyTrigger.interpret(startAddress.offsetBy(lsb = 0x76u), payload),
                    lfo1PitchDepth = lfo1PitchDepth.interpret(startAddress.offsetBy(lsb = 0x77u), payload),
                    lfo1TvfDepth = lfo1TvfDepth.interpret(startAddress.offsetBy(lsb = 0x78u), payload),
                    lfo1TvaDepth = lfo1TvaDepth.interpret(startAddress.offsetBy(lsb = 0x79u), payload),
                    lfo1PanDepth = lfo1PanDepth.interpret(startAddress.offsetBy(lsb = 0x7Au), payload),

                    lfo2WaveForm = lfo2WaveForm.interpret(startAddress.offsetBy(lsb = 0x7Bu), payload),
                    lfo2Rate = lfo2Rate.interpret(startAddress.offsetBy(lsb = 0x7Cu), payload),
                    lfo2Offset = lfo2Offset.interpret(startAddress.offsetBy(lsb = 0x7Eu), payload),
                    lfo2RateDetune = lfo2RateDetune.interpret(startAddress.offsetBy(lsb = 0x7Fu), payload),
                    lfo2DelayTime = lfo2DelayTime.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x00u), payload),
                    // lfo2Keyfollow = lfo2Keyfollow.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x01u), length, payload),
                    lfo2FadeMode = lfo2FadeMode.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x02u), payload),
                    lfo2FadeTime = lfo2FadeTime.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x03u), payload),
                    lfo2KeyTrigger = lfo2KeyTrigger.interpret(
                        startAddress.offsetBy(mlsb = 0x01u, lsb = 0x04u),
                        payload
                    ),
                    lfo2PitchDepth = lfo2PitchDepth.interpret(
                        startAddress.offsetBy(mlsb = 0x01u, lsb = 0x05u),
                        payload
                    ),
                    lfo2TvfDepth = lfo2TvfDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x06u), payload),
                    lfo2TvaDepth = lfo2TvaDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x07u), payload),
                    lfo2PanDepth = lfo2PanDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x08u), payload),


                    lfoStepType = lfoStepType.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u), payload),
                    lfoStep1 = lfoStep1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Au), payload),
                    lfoStep2 = lfoStep2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), payload),
                    lfoStep3 = lfoStep3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), payload),
                    lfoStep4 = lfoStep4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), payload),
                    lfoStep5 = lfoStep5.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), payload),
                    lfoStep6 = lfoStep6.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), payload),
                    lfoStep7 = lfoStep7.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x10u), payload),
                    lfoStep8 = lfoStep8.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x11u), payload),
                    lfoStep9 = lfoStep9.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x12u), payload),
                    lfoStep10 = lfoStep10.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x13u), payload),
                    lfoStep11 = lfoStep11.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x14u), payload),
                    lfoStep12 = lfoStep12.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x15u), payload),
                    lfoStep13 = lfoStep13.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x16u), payload),
                    lfoStep14 = lfoStep14.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x17u), payload),
                    lfoStep15 = 0, // TODO: lfoStep15.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x18u), length, payload),
                    lfoStep16 = 0 // TODO: lfoStep16.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x19u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address to ${address.offsetBy(size)} size $size from ${payload.hexDump({ Integra7Address(
                    it.toUInt7()
                ).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(
                    "When reading $address to ${address.offsetBy(size)} size $size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    class PcmSynthToneCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneCommon2>() {
        override val size = Integra7Size(0x3Cu.toUInt7UsingValue())

        val toneCategory = UByteField(deviceId, address.offsetBy(lsb = 0x10u))
        val undocumented = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x11u), 0..255)
        val phraseOctaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x33u))
        val phraseNmber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x38u), 0..65535)


        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmSynthToneCommon2 {
            assert(startAddress >= address)

            try {
                return PcmSynthToneCommon2(
                    toneCategory = toneCategory.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                    undocumented = undocumented.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                    phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                    tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x33u), payload),
                    phraseNmber = 0 // TODO: phraseNmber.interpret(startAddress.offsetBy(lsb = 0x38u), length, payload),
                )
            } catch (e: AssertionError) {
                throw AssertionError("When reading $address size $size from ${payload.hexDump({ Integra7Address(it.toUInt7()).toString() }, 0x10)}", e)
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException(
                    "When reading $address size $size from ${
                        payload.hexDump({
                            Integra7Address(
                                it.toUInt7()
                            ).toString()
                        }, 0x10)
                    }", e
                )
            }
        }
    }

    class SuperNaturalSynth7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalSynthTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x22.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalSynthToneCommonBuilder(deviceId, address, part)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val partial1 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u), part)
        val partial2 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x21u, lsb = 0x00u), part)
        val partial3 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x22u, lsb = 0x00u), part)

        override fun interpret(startAddress: Integra7Address, payload: SparseUByteArray): SuperNaturalSynthTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-S tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalSynthTone(
                common = common.interpret(startAddress, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), payload),
                partial1 = partial1.interpret(startAddress.offsetBy(mlsb = 0x20u, lsb = 0x00u), payload),
                partial2 = partial2.interpret(startAddress.offsetBy(mlsb = 0x21u, lsb = 0x00u), payload),
                partial3 = partial3.interpret(startAddress.offsetBy(mlsb = 0x22u, lsb = 0x00u), payload),
            )
        }
    }

    class SuperNaturalSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalSynthToneCommon>() {
        override val size = Integra7Size(0x40u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu))
        val portamentoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x12u))
        val portamentoTime = UByteField(deviceId, address.offsetBy(lsb = 0x13u))
        val monoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x14u))
        val octaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x15u), -3..3)
        val pithBendRangeUp = UByteField(deviceId, address.offsetBy(lsb = 0x16u), 0..24)
        val pitchBendRangeDown = UByteField(deviceId, address.offsetBy(lsb = 0x17u), 0..24)

        val partial1Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x19u))
        val partial1Select = BooleanField(deviceId, address.offsetBy(lsb = 0x1Au))
        val partial2Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val partial2Select = BooleanField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val partial3Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Du))
        val partial3Select = BooleanField(deviceId, address.offsetBy(lsb = 0x1Eu))

        val ringSwitch = EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), RingSwitch.values())
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x20u))

        val unisonSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val portamentoMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x31u), PortamentoMode.values())
        val legatoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x32u))
        val analogFeel = UByteField(deviceId, address.offsetBy(lsb = 0x34u))
        val waveShape = UByteField(deviceId, address.offsetBy(lsb = 0x35u))
        val toneCategory = UByteField(deviceId, address.offsetBy(lsb = 0x36u))
        val phraseNumber =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x37u), 0..65535)
        val phraseOctaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x3Bu), -3..3)
        val unisonSize = EnumField(deviceId, address.offsetBy(lsb = 0x3Cu), UnisonSize.values())

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SupernaturalSynthToneCommon {
            assert(startAddress >= address)

            return SupernaturalSynthToneCommon(
                name = name.interpret(startAddress, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
                portamentoSwitch = portamentoSwitch.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                portamentoTime = portamentoTime.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                monoSwitch = monoSwitch.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                octaveShift = octaveShift.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                pithBendRangeUp = pithBendRangeUp.interpret(startAddress.offsetBy(lsb = 0x16u), payload),
                pitchBendRangeDown = pitchBendRangeDown.interpret(startAddress.offsetBy(lsb = 0x17u), payload),

                partial1Switch = partial1Switch.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                partial1Select = partial1Select.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                partial2Switch = partial2Switch.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload),
                partial2Select = partial2Select.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                partial3Switch = partial3Switch.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),
                partial3Select = partial3Select.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),

                ringSwitch = ringSwitch.interpret(startAddress.offsetBy(lsb = 0x1Fu), payload),
                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x20u), payload),

                unisonSwitch = unisonSwitch.interpret(startAddress.offsetBy(lsb = 0x2Eu), payload),
                portamentoMode = portamentoMode.interpret(startAddress.offsetBy(lsb = 0x31u), payload),
                legatoSwitch = legatoSwitch.interpret(startAddress.offsetBy(lsb = 0x32u), payload),
                analogFeel = analogFeel.interpret(startAddress.offsetBy(lsb = 0x34u), payload),
                waveShape = waveShape.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                toneCategory = toneCategory.interpret(startAddress.offsetBy(lsb = 0x36u), payload),
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x37u), payload),
                phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x3Bu), payload),
                unisonSize = unisonSize.interpret(startAddress.offsetBy(lsb = 0x3Cu), payload),
            )
        }
    }

    class SuperNaturalSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SuperNaturalSynthTonePartial>() {
        override val size = Integra7Size(0x3Du)

        val oscWaveForm =
            EnumField(deviceId, address.offsetBy(lsb = 0x00u), SnSWaveForm.values())
        val oscWaveFormVariation =
            EnumField(deviceId, address.offsetBy(lsb = 0x01u), SnsWaveFormVariation.values())
        val oscPitch = ByteField(deviceId, address.offsetBy(lsb = 0x03u), -24..24)
        val oscDetune = ByteField(deviceId, address.offsetBy(lsb = 0x04u), -50..50)
        val oscPulseWidthModulationDepth = UByteField(deviceId, address.offsetBy(lsb = 0x05u))
        val oscPulseWidth = UByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val oscPitchAttackTime = UByteField(deviceId, address.offsetBy(lsb = 0x07u))
        val oscPitchEnvDecay = UByteField(deviceId, address.offsetBy(lsb = 0x08u))
        val oscPitchEnvDepth = ByteField(deviceId, address.offsetBy(lsb = 0x09u))

        val filterMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Au), SnsFilterMode.values())
        val filterSlope =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), SnsFilterSlope.values())
        val filterCutoff = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu))
//        val filterCutoffKeyflow = SignedValueField(deviceId, address.offsetBy(lsb = 0x0Du), -100..100)
        val filterEnvVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val filterResonance = UByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val filterEnvAttackTime = UByteField(deviceId, address.offsetBy(lsb = 0x10u))
        val filterEnvDecayTime = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val filterEnvSustainLevel = UByteField(deviceId, address.offsetBy(lsb = 0x12u))
        val filterEnvReleaseTime = UByteField(deviceId, address.offsetBy(lsb = 0x13u))
        val filterEnvDepth = ByteField(deviceId, address.offsetBy(lsb = 0x14u))
        val ampLevel = UByteField(deviceId, address.offsetBy(lsb = 0x15u))
        val ampVelocitySens = ByteField(deviceId, address.offsetBy(lsb = 0x16u))
        val ampEnvAttackTime = UByteField(deviceId, address.offsetBy(lsb = 0x17u))
        val ampEnvDecayTime = UByteField(deviceId, address.offsetBy(lsb = 0x18u))
        val ampEnvSustainLevel = UByteField(deviceId, address.offsetBy(lsb = 0x19u))
        val ampEnvReleaseTime = UByteField(deviceId, address.offsetBy(lsb = 0x1Au))
        val ampPan = ByteField(deviceId, address.offsetBy(lsb = 0x1Bu), -64..63)

        val lfoShape = EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), SnsLfoShape.values())
        val lfoRate = UByteField(deviceId, address.offsetBy(lsb = 0x1Du))
        val lfoTempoSyncSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Eu))
        val lfoTempoSyncNote =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), SnsLfoTempoSyncNote.values())
        val lfoFadeTime = UByteField(deviceId, address.offsetBy(lsb = 0x20u))
        val lfoKeyTrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x21u))
        val lfoPitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x22u))
        val lfoFilterDepth = ByteField(deviceId, address.offsetBy(lsb = 0x23u))
        val lfoAmpDepth = ByteField(deviceId, address.offsetBy(lsb = 0x24u))
        val lfoPanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x25u))

        val modulationShape =
            EnumField(deviceId, address.offsetBy(lsb = 0x26u), SnsLfoShape.values())
        val modulationLfoRate = UByteField(deviceId, address.offsetBy(lsb = 0x27u))
        val modulationLfoTempoSyncSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x28u))
        val modulationLfoTempoSyncNote =
            EnumField(deviceId, address.offsetBy(lsb = 0x29u), SnsLfoTempoSyncNote.values())
        val oscPulseWidthShift = UByteField(deviceId, address.offsetBy(lsb = 0x2Au))
        val modulationLfoPitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Cu))
        val modulationLfoFilterDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val modulationLfoAmpDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val modulationLfoPanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Fu))

        val cutoffAftertouchSens = ByteField(deviceId, address.offsetBy(lsb = 0x30u))
        val levelAftertouchSens = ByteField(deviceId, address.offsetBy(lsb = 0x31u))

        val waveGain = EnumField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain.values())
        val waveNumber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u), 0..16384)
        val hpfCutoff = UByteField(deviceId, address.offsetBy(lsb = 0x39u))
        val superSawDetune = UByteField(deviceId, address.offsetBy(lsb = 0x3Au))
        val modulationLfoRateControl = ByteField(deviceId, address.offsetBy(lsb = 0x3Bu))
//        val ampLevelKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu), 100..100)

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SuperNaturalSynthTonePartial {
            assert(this.isCovering(startAddress)) { "Not a SN-S tone definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalSynthTonePartial(
                oscWaveForm = oscWaveForm.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                oscWaveFormVariation = oscWaveFormVariation.interpret(startAddress.offsetBy(lsb = 0x01u), payload),
                oscPitch = oscPitch.interpret(startAddress.offsetBy(lsb = 0x03u), payload),
                oscDetune = oscDetune.interpret(startAddress.offsetBy(lsb = 0x04u), payload),
                oscPulseWidthModulationDepth = oscPulseWidthModulationDepth.interpret(
                    startAddress.offsetBy(lsb = 0x05u),
                    payload
                ),
                oscPulseWidth = oscPulseWidth.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                oscPitchAttackTime = oscPitchAttackTime.interpret(startAddress.offsetBy(lsb = 0x07u), payload),
                oscPitchEnvDecay = oscPitchEnvDecay.interpret(startAddress.offsetBy(lsb = 0x08u), payload),
                oscPitchEnvDepth = oscPitchEnvDepth.interpret(startAddress.offsetBy(lsb = 0x09u), payload),

                filterMode = filterMode.interpret(startAddress.offsetBy(lsb = 0x0Au), payload),
                filterSlope = filterSlope.interpret(startAddress.offsetBy(lsb = 0x0Bu), payload),
                filterCutoff = filterCutoff.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
//                filterCutoffKeyflow = filterCutoffKeyflow.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                filterEnvVelocitySens = filterEnvVelocitySens.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                filterResonance = filterResonance.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                filterEnvAttackTime = filterEnvAttackTime.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                filterEnvDecayTime = filterEnvDecayTime.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                filterEnvSustainLevel = filterEnvSustainLevel.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                filterEnvReleaseTime = filterEnvReleaseTime.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                filterEnvDepth = filterEnvDepth.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                ampLevel = ampLevel.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                ampVelocitySens = ampVelocitySens.interpret(startAddress.offsetBy(lsb = 0x16u), payload),
                ampEnvAttackTime = ampEnvAttackTime.interpret(startAddress.offsetBy(lsb = 0x17u), payload),
                ampEnvDecayTime = ampEnvDecayTime.interpret(startAddress.offsetBy(lsb = 0x18u), payload),
                ampEnvSustainLevel = ampEnvSustainLevel.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                ampEnvReleaseTime = ampEnvReleaseTime.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                ampPan = ampPan.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload),

                lfoShape = lfoShape.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                lfoRate = lfoRate.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),
                lfoTempoSyncSwitch = lfoTempoSyncSwitch.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),
                lfoTempoSyncNote = lfoTempoSyncNote.interpret(startAddress.offsetBy(lsb = 0x1Fu), payload),
                lfoFadeTime = lfoFadeTime.interpret(startAddress.offsetBy(lsb = 0x20u), payload),
                lfoKeyTrigger = lfoKeyTrigger.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                lfoPitchDepth = lfoPitchDepth.interpret(startAddress.offsetBy(lsb = 0x22u), payload),
                lfoFilterDepth = lfoFilterDepth.interpret(startAddress.offsetBy(lsb = 0x23u), payload),
                lfoAmpDepth = lfoAmpDepth.interpret(startAddress.offsetBy(lsb = 0x24u), payload),
                lfoPanDepth = lfoPanDepth.interpret(startAddress.offsetBy(lsb = 0x25u), payload),

                modulationShape = modulationShape.interpret(startAddress.offsetBy(lsb = 0x26u), payload),
                modulationLfoRate = modulationLfoRate.interpret(startAddress.offsetBy(lsb = 0x27u), payload),
                modulationLfoTempoSyncSwitch = modulationLfoTempoSyncSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x28u),
                    payload
                ),
                modulationLfoTempoSyncNote = modulationLfoTempoSyncNote.interpret(
                    startAddress.offsetBy(lsb = 0x29u),
                    payload
                ),
                oscPulseWidthShift = oscPulseWidthShift.interpret(startAddress.offsetBy(lsb = 0x2Au), payload),
                modulationLfoPitchDepth = modulationLfoPitchDepth.interpret(
                    startAddress.offsetBy(lsb = 0x2Cu),
                    payload
                ),
                modulationLfoFilterDepth = modulationLfoFilterDepth.interpret(
                    startAddress.offsetBy(lsb = 0x2Du),
                    payload
                ),
                modulationLfoAmpDepth = modulationLfoAmpDepth.interpret(startAddress.offsetBy(lsb = 0x2Eu), payload),
                modulationLfoPanDepth = modulationLfoPanDepth.interpret(startAddress.offsetBy(lsb = 0x2Fu), payload),

                cutoffAftertouchSens = cutoffAftertouchSens.interpret(startAddress.offsetBy(lsb = 0x30u), payload),
                levelAftertouchSens = levelAftertouchSens.interpret(startAddress.offsetBy(lsb = 0x31u), payload),

                waveGain = waveGain.interpret(startAddress.offsetBy(lsb = 0x34u), payload),
                waveNumber = waveNumber.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                hpfCutoff = hpfCutoff.interpret(startAddress.offsetBy(lsb = 0x39u), payload),
                superSawDetune = superSawDetune.interpret(startAddress.offsetBy(lsb = 0x3Au), payload),
                modulationLfoRateControl = modulationLfoRateControl.interpret(
                    startAddress.offsetBy(lsb = 0x3Bu),
                    payload
                ),
//                ampLevelKeyfollow = ampLevelKeyfollow.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
            )
        }
    }

    class SuperNaturalAcoustic7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalAcousticTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x30u.toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = SuperNaturalAcousticToneCommonBuilder(deviceId, address, part)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM

        override fun interpret(startAddress: Integra7Address, payload: SparseUByteArray): SuperNaturalAcousticTone {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-A tone ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalAcousticTone(
                common = common.interpret(startAddress, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), payload)
            )
        }
    }

    class SuperNaturalAcousticToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalAcousticToneCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x10u))

        val monoPoly = EnumField(deviceId, address.offsetBy(lsb = 0x11u), MonoPoly.values())
        val portamentoTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val cutoffOffset = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -64..63)
        val resonanceOffset = ByteField(deviceId, address.offsetBy(lsb = 0x14u), -64..63)
        val attackTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x15u), -64..63)
        val releaseTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x16u), -64..63)
        val vibratoRate = ByteField(deviceId, address.offsetBy(lsb = 0x17u), -64..63)
        val vibratoDepth = ByteField(deviceId, address.offsetBy(lsb = 0x18u), -64..63)
        val vibratorDelay = ByteField(deviceId, address.offsetBy(lsb = 0x19u), -64..63)
        val octaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x1Au), -3..3)
        val category = UByteField(deviceId, address.offsetBy(lsb = 0x1Bu))
        val phraseNumber = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x1Cu), 0..255)
        val phraseOctaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x1Eu), -3..3)

        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Fu))

        val instrumentVariation = UByteField(deviceId, address.offsetBy(lsb = 0x20u))
        val instrumentNumber = UByteField(deviceId, address.offsetBy(lsb = 0x21u))

        val modifyParameters = IntRange(0, 31).map {
            UByteField(
                deviceId,
                address.offsetBy(lsb = 0x22u).offsetBy(lsb = 0x01u, factor = it)
            )
        }

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SupernaturalAcousticToneCommon {
            assert(startAddress >= address)

            return SupernaturalAcousticToneCommon(
                name = name.interpret(startAddress, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                monoPoly = monoPoly.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                portamentoTimeOffset = portamentoTimeOffset.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                cutoffOffset = cutoffOffset.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                resonanceOffset = resonanceOffset.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                attackTimeOffset = attackTimeOffset.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                releaseTimeOffset = releaseTimeOffset.interpret(startAddress.offsetBy(lsb = 0x16u), payload),
                vibratoRate = vibratoRate.interpret(startAddress.offsetBy(lsb = 0x17u), payload),
                vibratoDepth = vibratoDepth.interpret(startAddress.offsetBy(lsb = 0x18u), payload),
                vibratorDelay = vibratorDelay.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                octaveShift = octaveShift.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                category = category.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload),
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                phraseOctaveShift = phraseOctaveShift.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),

                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x1Fu), payload),

                instrumentVariation = instrumentVariation.interpret(startAddress.offsetBy(lsb = 0x20u), payload),
                instrumentNumber = instrumentNumber.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                modifyParameters = modifyParameters
                    .mapIndexed { idx, fd ->
                        fd.interpret(
                            startAddress.offsetBy(lsb = 0x22u).offsetBy(lsb = 0x01u, factor = idx),
                            payload
                        )
                    }
            )
        }
    }

    class SuperNaturalDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalDrumKit>() {
        override val size = Integra7Size(UInt7(mlsb = 0x4D.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalDrumKitCommonBuilder(deviceId, address, part)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u), part)
        val note27 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u), part)
        val note28 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 1), part)
        val note29 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 2), part)

        val note30 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 3), part)
        val note31 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 4), part)
        val note32 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 5), part)
        val note33 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 6), part)
        val note34 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 7), part)
        val note35 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 8), part)
        val note36 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 9), part)
        val note37 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 10), part)
        val note38 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 11), part)
        val note39 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 12), part)

        val note40 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 13), part)
        val note41 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 14), part)
        val note42 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 15), part)
        val note43 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 16), part)
        val note44 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 17), part)
        val note45 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 18), part)
        val note46 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 19), part)
        val note47 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 20), part)
        val note48 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 21), part)
        val note49 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 22), part)

        val note50 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 23), part)
        val note51 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 24), part)
        val note52 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 25), part)
        val note53 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 26), part)
        val note54 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 27), part)
        val note55 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 28), part)
        val note56 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 29), part)
        val note57 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 30), part)
        val note58 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 31), part)
        val note59 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 32), part)

        val note60 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 33), part)
        val note61 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 34), part)
        val note62 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 35), part)
        val note63 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 36), part)
        val note64 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 37), part)
        val note65 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 38), part)
        val note66 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 39), part)
        val note67 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 40), part)
        val note68 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 41), part)
        val note69 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 42), part)

        val note70 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 43), part)
        val note71 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 44), part)
        val note72 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 45), part)
        val note73 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 46), part)
        val note74 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 47), part)
        val note75 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 48), part)
        val note76 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 49), part)
        val note77 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 50), part)
        val note78 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 51), part)
        val note79 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 52), part)

        val note80 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 53), part)
        val note81 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 54), part)
        val note82 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 55), part)
        val note83 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 56), part)
        val note84 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 57), part)
        val note85 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 58), part)
        val note86 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 59), part)
        val note87 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 60), part)
        val note88 = SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 61), part)

        override fun interpret(startAddress: Integra7Address, payload: SparseUByteArray): SuperNaturalDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalDrumKit(
                common = common.interpret(startAddress, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), payload),
                commonCompEq = commonCompEq.interpret(startAddress.offsetBy(mlsb = 0x08u, lsb = 0x00u), payload),
                notes = listOf(
                    note27.interpret(startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u), payload),
                    note28.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 1),
                        payload
                    ),
                    note29.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 2),
                        payload
                    ),

                    note30.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 3),
                        payload
                    ),
                    note31.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 4),
                        payload
                    ),
                    note32.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 5),
                        payload
                    ),
                    note33.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 6),
                        payload
                    ),
                    note34.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 7),
                        payload
                    ),
                    note35.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 8),
                        payload
                    ),
                    note36.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 9),
                        payload
                    ),
                    note37.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 10),
                        payload
                    ),
                    note38.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 11),
                        payload
                    ),
                    note39.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 12),
                        payload
                    ),

                    note40.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 13),
                        payload
                    ),
                    note41.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 14),
                        payload
                    ),
                    note42.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 15),
                        payload
                    ),
                    note43.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 16),
                        payload
                    ),
                    note44.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 17),
                        payload
                    ),
                    note45.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 18),
                        payload
                    ),
                    note46.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 19),
                        payload
                    ),
                    note47.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 20),
                        payload
                    ),
                    note48.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 21),
                        payload
                    ),
                    note49.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 22),
                        payload
                    ),

                    note50.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 23),
                        payload
                    ),
                    note51.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 24),
                        payload
                    ),
                    note52.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 25),
                        payload
                    ),
                    note53.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 26),
                        payload
                    ),
                    note54.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 27),
                        payload
                    ),
                    note55.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 28),
                        payload
                    ),
                    note56.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 29),
                        payload
                    ),
                    note57.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 30),
                        payload
                    ),
                    note58.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 31),
                        payload
                    ),
                    note59.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 32),
                        payload
                    ),

                    note60.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 33),
                        payload
                    ),
                    note61.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 34),
                        payload
                    ),
                    note62.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 35),
                        payload
                    ),
                    note63.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 36),
                        payload
                    ),
                    note64.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 37),
                        payload
                    ),
                    note65.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 38),
                        payload
                    ),
                    note66.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 39),
                        payload
                    ),
                    note67.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 40),
                        payload
                    ),
                    note68.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 41),
                        payload
                    ),
                    note69.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 42),
                        payload
                    ),

                    note70.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 43),
                        payload
                    ),
                    note71.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 44),
                        payload
                    ),
                    note72.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 45),
                        payload
                    ),
                    note73.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 46),
                        payload
                    ),
                    note74.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 47),
                        payload
                    ),
                    note75.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 48),
                        payload
                    ),
                    note76.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 49),
                        payload
                    ),
                    note77.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 50),
                        payload
                    ),
                    note78.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 51),
                        payload
                    ),
                    note79.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 52),
                        payload
                    ),

                    note80.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 53),
                        payload
                    ),
                    note81.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 54),
                        payload
                    ),
                    note82.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 55),
                        payload
                    ),
                    note83.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 56),
                        payload
                    ),
                    note84.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 57),
                        payload
                    ),
                    note85.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 58),
                        payload
                    ),
                    note86.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 59),
                        payload
                    ),
                    note87.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 60),
                        payload
                    ),
                    note88.interpret(
                        startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                            .offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = 61),
                        payload
                    )
                )
            )
        }
    }

    class SuperNaturalDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x10u))
        val ambienceLevel = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val phraseNo = UByteField(deviceId, address.offsetBy(lsb = 0x12u))
        val tfx = BooleanField(deviceId, address.offsetBy(lsb = 0x13u))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommon {
            assert(startAddress >= address)

            return SupernaturalDrumKitCommon(
                name = name.interpret(startAddress, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                ambienceLevel = ambienceLevel.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                phraseNo = phraseNo.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                tfx = tfx.interpret(startAddress.offsetBy(lsb = 0x13u), payload)
            )
        }
    }

    class SuperNaturalDrumKitCommonCompEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalDrumKitCommonCompEq>() {
        override val size = Integra7Size(0x54u)

        val comp1Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x00u))
        val comp1AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x01u),
            SupernaturalDrumAttackTime.values()
        )
        val comp1ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x02u),
            SupernaturalDrumReleaseTime.values()
        )
        val comp1Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x03u))
        val comp1Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumRatio.values())
        val comp1OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x05u), 0..24)
        val eq1Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x06u))
        val eq1LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x07u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq1LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x08u), 0..30) // - 15
        val eq1MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x09u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq1MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x0Au), 0..30) // - 15
        val eq1MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), SupernaturalDrumMidQ.values())
        val eq1HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x0Cu),
            SupernaturalDrumHighFrequency.values()
        )
        val eq1HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x0Du), 0..30) // - 15

        val comp2Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val comp2AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x0Fu),
            SupernaturalDrumAttackTime.values()
        )
        val comp2ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x10u),
            SupernaturalDrumReleaseTime.values()
        )
        val comp2Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val comp2Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x12u), SupernaturalDrumRatio.values())
        val comp2OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x13u), 0..24)
        val eq2Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x14u))
        val eq2LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x15u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq2LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x16u), 0..30) // - 15
        val eq2MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x17u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq2MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x18u), 0..30) // - 15
        val eq2MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x19u), SupernaturalDrumMidQ.values())
        val eq2HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Au),
            SupernaturalDrumHighFrequency.values()
        )
        val eq2HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..30) // - 15

        val comp3Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val comp3AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Du),
            SupernaturalDrumAttackTime.values()
        )
        val comp3ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Eu),
            SupernaturalDrumReleaseTime.values()
        )
        val comp3Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x1Fu))
        val comp3Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), SupernaturalDrumRatio.values())
        val comp3OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x21u), 0..24)
        val eq3Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x22u))
        val eq3LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x23u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq3LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x24u), 0..30) // - 15
        val eq3MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x25u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq3MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x26u), 0..30) // - 15
        val eq3MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x27u), SupernaturalDrumMidQ.values())
        val eq3HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x28u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq3HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x29u), 0..30) // - 15

        val comp4Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Au))
        val comp4AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x2Bu),
            SupernaturalDrumAttackTime.values()
        )
        val comp4ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x2Cu),
            SupernaturalDrumReleaseTime.values()
        )
        val comp4Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val comp4Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Eu), SupernaturalDrumRatio.values())
        val comp4OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x2Fu), 0..24)
        val eq4Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x30u))
        val eq4LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x31u),
            SupernaturalDrumLowFrequency.values()
        )
        val eq4LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x32u), 0..30) // - 15
        val eq4MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x33u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq4MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x34u), 0..30) // - 15
        val eq4MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x35u), SupernaturalDrumMidQ.values())
        val eq4HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x36u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq4HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x37u), 0..30) // - 15

        val comp5Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp5AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x39u),
            SupernaturalDrumAttackTime.values()
        )
        val comp5ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x3Au),
            SupernaturalDrumReleaseTime.values()
        )
        val comp5Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp5Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio.values())
        val comp5OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq5Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq5LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x3Fu),
            SupernaturalDrumLowFrequency.values()
        )
        val eq5LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq5MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x41u),
            SupernaturalDrumMidFrequency.values()
        )
        val eq5MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq5MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ.values())
        val eq5HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x44u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq5HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

        val comp6Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x46u))
        val comp6AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x47u),
            SupernaturalDrumAttackTime.values()
        )
        val comp6ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x48u),
            SupernaturalDrumReleaseTime.values()
        )
        val comp6Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x49u))
        val comp6Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x4Au), SupernaturalDrumRatio.values())
        val comp6OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x4Bu), 0..24)
        val eq6Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val eq6LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x4Du),
            SupernaturalDrumLowFrequency.values()
        )
        val eq6LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..30) // - 15
        val eq6MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x4Fu),
            SupernaturalDrumMidFrequency.values()
        )
        val eq6MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x50u), 0..30) // - 15
        val eq6MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x51u), SupernaturalDrumMidQ.values())
        val eq6HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x52u),
            SupernaturalDrumHighFrequency.values()
        )
        val eq6HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x53u), 0..30) // - 15

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommonCompEq {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit comp/eq-definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SupernaturalDrumKitCommonCompEq(
                comp1Switch = comp1Switch.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                comp1AttackTime = comp1AttackTime.interpret(startAddress.offsetBy(lsb = 0x01u), payload),
                comp1ReleaseTime = comp1ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x02u), payload),
                comp1Threshold = comp1Threshold.interpret(startAddress.offsetBy(lsb = 0x03u), payload),
                comp1Ratio = comp1Ratio.interpret(startAddress.offsetBy(lsb = 0x04u), payload),
                comp1OutputGain = comp1OutputGain.interpret(startAddress.offsetBy(lsb = 0x05u), payload),
                eq1Switch = eq1Switch.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                eq1LowFrequency = eq1LowFrequency.interpret(startAddress.offsetBy(lsb = 0x07u), payload),
                eq1LowGain = eq1LowGain.interpret(startAddress.offsetBy(lsb = 0x08u), payload) - 15,
                eq1MidFrequency = eq1MidFrequency.interpret(startAddress.offsetBy(lsb = 0x09u), payload),
                eq1MidGain = eq1MidGain.interpret(startAddress.offsetBy(lsb = 0x0Au), payload) - 15,
                eq1MidQ = eq1MidQ.interpret(startAddress.offsetBy(lsb = 0x0Bu), payload),
                eq1HighFrequency = eq1HighFrequency.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
                eq1HighGain = eq1HighGain.interpret(startAddress.offsetBy(lsb = 0x0Du), payload) - 15,

                comp2Switch = comp2Switch.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                comp2AttackTime = comp2AttackTime.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                comp2ReleaseTime = comp2ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                comp2Threshold = comp2Threshold.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                comp2Ratio = comp2Ratio.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                comp2OutputGain = comp2OutputGain.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                eq2Switch = eq2Switch.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                eq2LowFrequency = eq2LowFrequency.interpret(startAddress.offsetBy(lsb = 0x15u), payload),
                eq2LowGain = eq2LowGain.interpret(startAddress.offsetBy(lsb = 0x16u), payload) - 15,
                eq2MidFrequency = eq2MidFrequency.interpret(startAddress.offsetBy(lsb = 0x17u), payload),
                eq2MidGain = eq2MidGain.interpret(startAddress.offsetBy(lsb = 0x18u), payload) - 15,
                eq2MidQ = eq2MidQ.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                eq2HighFrequency = eq2HighFrequency.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                eq2HighGain = eq2HighGain.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload) - 15,

                comp3Switch = comp3Switch.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                comp3AttackTime = comp3AttackTime.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),
                comp3ReleaseTime = comp3ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),
                comp3Threshold = comp3Threshold.interpret(startAddress.offsetBy(lsb = 0x1Fu), payload),
                comp3Ratio = comp3Ratio.interpret(startAddress.offsetBy(lsb = 0x20u), payload),
                comp3OutputGain = comp3OutputGain.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                eq3Switch = eq3Switch.interpret(startAddress.offsetBy(lsb = 0x22u), payload),
                eq3LowFrequency = eq3LowFrequency.interpret(startAddress.offsetBy(lsb = 0x23u), payload),
                eq3LowGain = eq3LowGain.interpret(startAddress.offsetBy(lsb = 0x24u), payload) - 15,
                eq3MidFrequency = eq3MidFrequency.interpret(startAddress.offsetBy(lsb = 0x25u), payload),
                eq3MidGain = eq3MidGain.interpret(startAddress.offsetBy(lsb = 0x26u), payload) - 15,
                eq3MidQ = eq3MidQ.interpret(startAddress.offsetBy(lsb = 0x27u), payload),
                eq3HighFrequency = eq3HighFrequency.interpret(startAddress.offsetBy(lsb = 0x28u), payload),
                eq3HighGain = eq3HighGain.interpret(startAddress.offsetBy(lsb = 0x29u), payload) - 15,

                comp4Switch = comp4Switch.interpret(startAddress.offsetBy(lsb = 0x2Au), payload),
                comp4AttackTime = comp4AttackTime.interpret(startAddress.offsetBy(lsb = 0x2Bu), payload),
                comp4ReleaseTime = comp4ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x2Cu), payload),
                comp4Threshold = comp4Threshold.interpret(startAddress.offsetBy(lsb = 0x2Du), payload),
                comp4Ratio = comp4Ratio.interpret(startAddress.offsetBy(lsb = 0x2Eu), payload),
                comp4OutputGain = comp4OutputGain.interpret(startAddress.offsetBy(lsb = 0x2Fu), payload),
                eq4Switch = eq4Switch.interpret(startAddress.offsetBy(lsb = 0x30u), payload),
                eq4LowFrequency = eq4LowFrequency.interpret(startAddress.offsetBy(lsb = 0x31u), payload),
                eq4LowGain = eq4LowGain.interpret(startAddress.offsetBy(lsb = 0x32u), payload) - 15,
                eq4MidFrequency = eq4MidFrequency.interpret(startAddress.offsetBy(lsb = 0x33u), payload),
                eq4MidGain = eq4MidGain.interpret(startAddress.offsetBy(lsb = 0x34u), payload) - 15,
                eq4MidQ = eq4MidQ.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                eq4HighFrequency = eq4HighFrequency.interpret(startAddress.offsetBy(lsb = 0x36u), payload),
                eq4HighGain = eq4HighGain.interpret(startAddress.offsetBy(lsb = 0x37u), payload) - 15,

                comp5Switch = comp5Switch.interpret(startAddress.offsetBy(lsb = 0x38u), payload),
                comp5AttackTime = comp5AttackTime.interpret(startAddress.offsetBy(lsb = 0x39u), payload),
                comp5ReleaseTime = comp5ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x3Au), payload),
                comp5Threshold = comp5Threshold.interpret(startAddress.offsetBy(lsb = 0x3Bu), payload),
                comp5Ratio = comp5Ratio.interpret(startAddress.offsetBy(lsb = 0x3Cu), payload),
                comp5OutputGain = comp5OutputGain.interpret(startAddress.offsetBy(lsb = 0x3Du), payload),
                eq5Switch = eq5Switch.interpret(startAddress.offsetBy(lsb = 0x3Eu), payload),
                eq5LowFrequency = eq5LowFrequency.interpret(startAddress.offsetBy(lsb = 0x3Fu), payload),
                eq5LowGain = eq5LowGain.interpret(startAddress.offsetBy(lsb = 0x40u), payload) - 15,
                eq5MidFrequency = eq5MidFrequency.interpret(startAddress.offsetBy(lsb = 0x41u), payload),
                eq5MidGain = eq5MidGain.interpret(startAddress.offsetBy(lsb = 0x42u), payload) - 15,
                eq5MidQ = eq5MidQ.interpret(startAddress.offsetBy(lsb = 0x43u), payload),
                eq5HighFrequency = eq5HighFrequency.interpret(startAddress.offsetBy(lsb = 0x44u), payload),
                eq5HighGain = eq5HighGain.interpret(startAddress.offsetBy(lsb = 0x45u), payload) - 15,

                comp6Switch = comp6Switch.interpret(startAddress.offsetBy(lsb = 0x46u), payload),
                comp6AttackTime = comp6AttackTime.interpret(startAddress.offsetBy(lsb = 0x47u), payload),
                comp6ReleaseTime = comp6ReleaseTime.interpret(startAddress.offsetBy(lsb = 0x48u), payload),
                comp6Threshold = comp6Threshold.interpret(startAddress.offsetBy(lsb = 0x49u), payload),
                comp6Ratio = comp6Ratio.interpret(startAddress.offsetBy(lsb = 0x4Au), payload),
                comp6OutputGain = comp6OutputGain.interpret(startAddress.offsetBy(lsb = 0x4Bu), payload),
                eq6Switch = eq6Switch.interpret(startAddress.offsetBy(lsb = 0x4Cu), payload),
                eq6LowFrequency = eq6LowFrequency.interpret(startAddress.offsetBy(lsb = 0x4Du), payload),
                eq6LowGain = eq6LowGain.interpret(startAddress.offsetBy(lsb = 0x4Eu), payload) - 15,
                eq6MidFrequency = eq6MidFrequency.interpret(startAddress.offsetBy(lsb = 0x4Fu), payload),
                eq6MidGain = eq6MidGain.interpret(startAddress.offsetBy(lsb = 0x50u), payload) - 15,
                eq6MidQ = eq6MidQ.interpret(startAddress.offsetBy(lsb = 0x51u), payload),
                eq6HighFrequency = eq6HighFrequency.interpret(startAddress.offsetBy(lsb = 0x52u), payload),
                eq6HighGain = 0 // TODO eq6HighGain.interpret(startAddress.offsetBy(lsb = 0x53u), length, payload) - 15,
            )
        }
    }

    class SuperNaturalDrumKitNoteBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SuperNaturalDrumKitNote>() {
        override val size = Integra7Size(0x13u)

        val instrumentNumber =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x00u), 0..512)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x04u))
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x05u))
        val chorusSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val reverbSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x07u))
        val tune = UnsignedMsbLsbFourNibbles(
            deviceId,
            address.offsetBy(lsb = 0x08u),
            8..248
        ) // TODO: convert!
        val attack = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu), 0..100)
        val decay = ByteField(deviceId, address.offsetBy(lsb = 0x0Du), -63..0)
        val brilliance = ByteField(deviceId, address.offsetBy(lsb = 0x0Eu), -15..12)
        val variation = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x0Fu),
            SuperNaturalDrumToneVariation.values()
        )
        val dynamicRange = UByteField(deviceId, address.offsetBy(lsb = 0x10u), 0..63)
        val stereoWidth = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val outputAssign = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x12u),
            SuperNaturalDrumToneOutput.values()
        )

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): SuperNaturalDrumKitNote {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a SN-D kit note-definition ($address..${address.offsetBy(size)}), but $startAddress ${startAddress.rangeName()}" }

            return SuperNaturalDrumKitNote(
                instrumentNumber = instrumentNumber.interpret(startAddress.offsetBy(lsb = 0x00u), payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x04u), payload),
                pan = pan.interpret(startAddress.offsetBy(lsb = 0x05u), payload),
                chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x06u), payload),
                reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x07u), payload),
                tune = tune.interpret(startAddress.offsetBy(lsb = 0x08u), payload),
                attack = attack.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
                decay = decay.interpret(startAddress.offsetBy(lsb = 0x0Du), payload),
                brilliance = brilliance.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                variation = variation.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                dynamicRange = dynamicRange.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                stereoWidth = stereoWidth.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                outputAssign = SuperNaturalDrumToneOutput.PART // TODO outputAssign.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
            )
        }
    }

    class PcmDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<PcmDrumKit>() {
        override val size =
            Integra7Size(UInt7(mmsb = 0x02u.toUByte7(), mlsb = 0x07Fu.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = PcmDrumKitCommonBuilder(deviceId, address, part)
        val mfx = PcmSynthToneMfxBuilder(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u), part) // Same as SN-D
        val keys = IntRange(0, 78) // key 21 .. 108
            .map { PcmDrumKitPartialBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = it), part)  }
        val common2 = PcmDrumKitCommon2Builder(deviceId, address.offsetBy(mmsb = 0x02u, lsb = 0x00u), part)

        override fun interpret(startAddress: Integra7Address, payload: SparseUByteArray): PcmDrumKit {
            assert(startAddress >= address && startAddress <= address.offsetBy(size)) {
                "Not a PCM Drum kit ($address..${address.offsetBy(size)}) for part $part, but $startAddress ${startAddress.rangeName()}" }

            return PcmDrumKit(
                common = common.interpret(startAddress, payload),
                mfx = mfx.interpret(startAddress.offsetBy(mlsb = 0x02u, lsb = 0x00u), payload),
                keys = keys
                    .mapIndexed { index, b ->
                        b.interpret(
                            startAddress.offsetBy(mlsb = 0x10u, lsb = 0x00u)
                                .offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = index),
                            payload
                        )
                    },
                common2 = common2.interpret(startAddress.offsetBy(mmsb = 0x02u, lsb = 0x00u), payload),
            )
        }
    }

    class PcmDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmDrumKitCommon {
            assert(startAddress >= address)

            return PcmDrumKitCommon(
                name = name.interpret(startAddress, payload),
                level = level.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
            )
        }
    }

    class PcmDrumKitPartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitPartial>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x43u.toUByte7()))

        val name = AsciiStringField(deviceId, address, length = 0x0C)

        val assignType =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Cu), PcmDrumKitAssignType.values())
        val muteGroup = UByteField(deviceId, address.offsetBy(lsb = 0x0Du), 0..31)

        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val coarseTune = UByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val fineTune = ByteField(deviceId, address.offsetBy(lsb = 0x10u), -50..50)
        val randomPitchDepth =
            EnumField(deviceId, address.offsetBy(lsb = 0x11u), RandomPithDepth.values())
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val randomPanDepth = UByteField(deviceId, address.offsetBy(lsb = 0x13u), 0..63)
        val alternatePanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x14u))
        val envMode = EnumField(deviceId, address.offsetBy(lsb = 0x15u), EnvMode.values())

        val outputLevel = UByteField(deviceId, address.offsetBy(lsb = 0x16u))
        val chorusSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x19u))
        val reverbSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x1Au))
        val outputAssign = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Bu),
            SuperNaturalDrumToneOutput.values()
        )

        val pitchBendRange = UByteField(deviceId, address.offsetBy(lsb = 0x1Cu), 0..48)
        val receiveExpression = BooleanField(deviceId, address.offsetBy(lsb = 0x1Du))
        val receiveHold1 = BooleanField(deviceId, address.offsetBy(lsb = 0x1Eu))

        val wmtVelocityControl =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), WmtVelocityControl.values())

        val wmt1WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x21u))
        val wmt1WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x22u), WaveGroupType.values())
        val wmt1WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x23u), 0..16384)
        val wmt1WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x27u), 0..16384)
        val wmt1WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Bu), 0..16384)
        val wmt1WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x2Fu), WaveGain.values())
        val wmt1WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x30u))
        val wmt1WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x31u), 0..3)
        val wmt1WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x32u), 0..16)
        val wmt1WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x33u))
        val wmt1WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x34u), -48..48)
        val wmt1WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x35u), -50..50)
        val wmt1WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
        val wmt1WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x37u))
        val wmt1WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x38u), OffOnReverse.values())
        val wmt1WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x39u))
        val wmt1VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Au))
        val wmt1VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Cu))

        val wmt2WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val wmt2WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Fu), WaveGroupType.values())
        val wmt2WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x40u), 0..16384)
        val wmt2WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x44u), 0..16384)
        val wmt2WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x48u), 0..16384)
        val wmt2WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x4Cu), WaveGain.values())
        val wmt2WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x4Du))
        val wmt2WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..3)
        val wmt2WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x4Fu), 0..16)
        val wmt2WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x50u))
        val wmt2WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x51u), -48..48)
        val wmt2WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x52u), -50..50)
        val wmt2WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x53u), -64..63)
        val wmt2WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x54u))
        val wmt2WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x55u), OffOnReverse.values())
        val wmt2WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x56u))
        val wmt2VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x57u))
        val wmt2VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x59u))

        val wmt3WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val wmt3WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x5Cu), WaveGroupType.values())
        val wmt3WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du), 0..16384)
        val wmt3WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u), 0..16384)
        val wmt3WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u), 0..16384)
        val wmt3WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x69u), WaveGain.values())
        val wmt3WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x6Au))
        val wmt3WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x6Bu), 0..3)
        val wmt3WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x6Cu), 0..16)
        val wmt3WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x6Du))
        val wmt3WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x6Eu), -48..48)
        val wmt3WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x6Fu), -50..50)
        val wmt3WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x70u), -64..63)
        val wmt3WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x71u))
        val wmt3WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x72u), OffOnReverse.values())
        val wmt3WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x73u))
        val wmt3VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x74u))
        val wmt3VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x76u))

        val wmt4WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x78u))
        val wmt4WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x79u), WaveGroupType.values())
        val wmt4WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Au), 0..16384)
        val wmt4WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Eu), 0..16384)
        val wmt4WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x02u), 0..16384)
        val wmt4WaveGain =
            EnumField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u), WaveGain.values())
        val wmt4WaveFxmSwitch =
            BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val wmt4WaveFxmColor =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u), 0..3)
        val wmt4WaveFxmDepth =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..16)
        val wmt4WaveTempoSync =
            BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au))
        val wmt4WaveCoarseTune =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -48..48)
        val wmt4WaveFineTune =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -50..50)
        val wmt4WavePan =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -64..63)
        val wmt4WaveRandomPanSwitch =
            BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu))
        val wmt4WaveAlternatePanSwitch = EnumField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu),
            OffOnReverse.values()
        )
        val wmt4WaveLevel = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u))
        val wmt4VelocityRange =
            UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u))
        val wmt4VelocityFadeWidth =
            UnsignedLsbMsbBytes(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u))

        val pitchEnvDepth =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -12..12)
        val pitchEnvVelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u))
        val pitchEnvTime1VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u))
        val pitchEnvTime4VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u))

        val pitchEnvTime1 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u))
        val pitchEnvTime2 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Au))
        val pitchEnvTime3 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Bu))
        val pitchEnvTime4 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Cu))

        val pitchEnvLevel0 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Du))
        val pitchEnvLevel1 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Eu))
        val pitchEnvLevel2 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x1Fu))
        val pitchEnvLevel3 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x20u))
        val pitchEnvLevel4 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x21u))

        val tvfFilterType = EnumField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x22u),
            TvfFilterType.values()
        )
        val tvfCutoffFrequency =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x23u))
        val tvfCutoffVelocityCurve =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x24u), 0..7)
        val tvfCutoffVelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x25u))
        val tvfResonance = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x26u))
        val tvfResonanceVelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x27u))
        val tvfEnvDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x28u))
        val tvfEnvVelocityCurveType =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x29u), 0..7)
        val tvfEnvVelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Au))
        val tvfEnvTime1VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Bu))
        val tvfEnvTime4VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Cu))
        val tvfEnvTime1 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Du))
        val tvfEnvTime2 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Eu))
        val tvfEnvTime3 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x2Fu))
        val tvfEnvTime4 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x30u))
        val tvfEnvLevel0 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x31u))
        val tvfEnvLevel1 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x32u))
        val tvfEnvLevel2 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x33u))
        val tvfEnvLevel3 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x34u))
        val tvfEnvLevel4 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x35u))

        val tvaLevelVelocityCurve =
            UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x36u), 0..7)
        val tvaLevelVelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x37u))
        val tvaEnvTime1VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x38u))
        val tvaEnvTime4VelocitySens =
            ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x39u))
        val tvaEnvTime1 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Au))
        val tvaEnvTime2 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Bu))
        val tvaEnvTime3 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Cu))
        val tvaEnvTime4 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Du))
        val tvaEnvLevel1 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Eu))
        val tvaEnvLevel2 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x3Fu))
        val tvaEnvLevel3 = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x40u))

        val oneShotMode = BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x41u))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmDrumKitPartial {
            assert(startAddress >= address)

            return PcmDrumKitPartial(
                name = name.interpret(startAddress.offsetBy(lsb = 0x00u), payload),

                assignType = assignType.interpret(startAddress.offsetBy(lsb = 0x0Cu), payload),
                muteGroup = muteGroup.interpret(startAddress.offsetBy(lsb = 0x0Du), payload),

                level = level.interpret(startAddress.offsetBy(lsb = 0x0Eu), payload),
                coarseTune = coarseTune.interpret(startAddress.offsetBy(lsb = 0x0Fu), payload),
                fineTune = fineTune.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                randomPitchDepth = randomPitchDepth.interpret(startAddress.offsetBy(lsb = 0x11u), payload),
                pan = pan.interpret(startAddress.offsetBy(lsb = 0x12u), payload),
                randomPanDepth = randomPanDepth.interpret(startAddress.offsetBy(lsb = 0x13u), payload),
                alternatePanDepth = alternatePanDepth.interpret(startAddress.offsetBy(lsb = 0x14u), payload),
                envMode = envMode.interpret(startAddress.offsetBy(lsb = 0x15u), payload),

                outputLevel = outputLevel.interpret(startAddress.offsetBy(lsb = 0x16u), payload),
                chorusSendLevel = chorusSendLevel.interpret(startAddress.offsetBy(lsb = 0x19u), payload),
                reverbSendLevel = reverbSendLevel.interpret(startAddress.offsetBy(lsb = 0x1Au), payload),
                outputAssign = outputAssign.interpret(startAddress.offsetBy(lsb = 0x1Bu), payload),

                pitchBendRange = pitchBendRange.interpret(startAddress.offsetBy(lsb = 0x1Cu), payload),
                receiveExpression = receiveExpression.interpret(startAddress.offsetBy(lsb = 0x1Du), payload),
                receiveHold1 = receiveHold1.interpret(startAddress.offsetBy(lsb = 0x1Eu), payload),

                wmtVelocityControl = wmtVelocityControl.interpret(startAddress.offsetBy(lsb = 0x20u), payload),

                wmt1WaveSwitch = wmt1WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x21u), payload),
                wmt1WaveGroupType = wmt1WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x22u), payload),
                wmt1WaveGroupId = wmt1WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x23u), payload),
                wmt1WaveNumberL = wmt1WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x27u), payload),
                wmt1WaveNumberR = wmt1WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x2Bu), payload),
                wmt1WaveGain = wmt1WaveGain.interpret(startAddress.offsetBy(lsb = 0x2Fu), payload),
                wmt1WaveFxmSwitch = wmt1WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x30u), payload),
                wmt1WaveFxmColor = wmt1WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x31u), payload),
                wmt1WaveFxmDepth = wmt1WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x32u), payload),
                wmt1WaveTempoSync = wmt1WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x33u), payload),
                wmt1WaveCoarseTune = wmt1WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x34u), payload),
                wmt1WaveFineTune = wmt1WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x35u), payload),
                wmt1WavePan = wmt1WavePan.interpret(startAddress.offsetBy(lsb = 0x36u), payload),
                wmt1WaveRandomPanSwitch = wmt1WaveRandomPanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x37u),
                    payload
                ),
                wmt1WaveAlternatePanSwitch = wmt1WaveAlternatePanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x38u),
                    payload
                ),
                wmt1WaveLevel = wmt1WaveLevel.interpret(startAddress.offsetBy(lsb = 0x39u), payload),
                wmt1VelocityRange = wmt1VelocityRange.interpret(startAddress.offsetBy(lsb = 0x3Au), payload),
                wmt1VelocityFadeWidth = wmt1VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x3Cu), payload),

                wmt2WaveSwitch = wmt2WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x3Eu), payload),
                wmt2WaveGroupType = wmt2WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x3Fu), payload),
                wmt2WaveGroupId = wmt2WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x40u), payload),
                wmt2WaveNumberL = wmt2WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x44u), payload),
                wmt2WaveNumberR = wmt2WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x48u), payload),
                wmt2WaveGain = wmt2WaveGain.interpret(startAddress.offsetBy(lsb = 0x4Cu), payload),
                wmt2WaveFxmSwitch = wmt2WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x4Du), payload),
                wmt2WaveFxmColor = wmt2WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x4Eu), payload),
                wmt2WaveFxmDepth = wmt2WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x4Fu), payload),
                wmt2WaveTempoSync = wmt2WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x50u), payload),
                wmt2WaveCoarseTune = wmt2WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x51u), payload),
                wmt2WaveFineTune = wmt2WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x52u), payload),
                wmt2WavePan = wmt2WavePan.interpret(startAddress.offsetBy(lsb = 0x53u), payload),
                wmt2WaveRandomPanSwitch = wmt2WaveRandomPanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x54u),
                    payload
                ),
                wmt2WaveAlternatePanSwitch = wmt2WaveAlternatePanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x55u),
                    payload
                ),
                wmt2WaveLevel = wmt2WaveLevel.interpret(startAddress.offsetBy(lsb = 0x56u), payload),
                wmt2VelocityRange = wmt2VelocityRange.interpret(startAddress.offsetBy(lsb = 0x57u), payload),
                wmt2VelocityFadeWidth = wmt2VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x59u), payload),

                wmt3WaveSwitch = wmt3WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x5Bu), payload),
                wmt3WaveGroupType = wmt3WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x5Cu), payload),
                wmt3WaveGroupId = wmt3WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x5Du), payload),
                wmt3WaveNumberL = wmt3WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x61u), payload),
                wmt3WaveNumberR = wmt3WaveNumberR.interpret(startAddress.offsetBy(lsb = 0x65u), payload),
                wmt3WaveGain = wmt3WaveGain.interpret(startAddress.offsetBy(lsb = 0x69u), payload),
                wmt3WaveFxmSwitch = wmt3WaveFxmSwitch.interpret(startAddress.offsetBy(lsb = 0x6Au), payload),
                wmt3WaveFxmColor = wmt3WaveFxmColor.interpret(startAddress.offsetBy(lsb = 0x6Bu), payload),
                wmt3WaveFxmDepth = wmt3WaveFxmDepth.interpret(startAddress.offsetBy(lsb = 0x6Cu), payload),
                wmt3WaveTempoSync = wmt3WaveTempoSync.interpret(startAddress.offsetBy(lsb = 0x6Du), payload),
                wmt3WaveCoarseTune = wmt3WaveCoarseTune.interpret(startAddress.offsetBy(lsb = 0x6Eu), payload),
                wmt3WaveFineTune = wmt3WaveFineTune.interpret(startAddress.offsetBy(lsb = 0x6Fu), payload),
                wmt3WavePan = wmt3WavePan.interpret(startAddress.offsetBy(lsb = 0x70u), payload),
                wmt3WaveRandomPanSwitch = wmt3WaveRandomPanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x71u),
                    payload
                ),
                wmt3WaveAlternatePanSwitch = wmt3WaveAlternatePanSwitch.interpret(
                    startAddress.offsetBy(lsb = 0x72u),
                    payload
                ),
                wmt3WaveLevel = wmt3WaveLevel.interpret(startAddress.offsetBy(lsb = 0x73u), payload),
                wmt3VelocityRange = wmt3VelocityRange.interpret(startAddress.offsetBy(lsb = 0x74u), payload),
                wmt3VelocityFadeWidth = wmt3VelocityFadeWidth.interpret(startAddress.offsetBy(lsb = 0x76u), payload),

                wmt4WaveSwitch = wmt4WaveSwitch.interpret(startAddress.offsetBy(lsb = 0x78u), payload),
                wmt4WaveGroupType = wmt4WaveGroupType.interpret(startAddress.offsetBy(lsb = 0x79u), payload),
                wmt4WaveGroupId = wmt4WaveGroupId.interpret(startAddress.offsetBy(lsb = 0x7Au), payload),
                wmt4WaveNumberL = wmt4WaveNumberL.interpret(startAddress.offsetBy(lsb = 0x7Eu), payload),
                wmt4WaveNumberR = wmt4WaveNumberR.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x02u), payload),
                wmt4WaveGain = wmt4WaveGain.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x06u), payload),
                wmt4WaveFxmSwitch = wmt4WaveFxmSwitch.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x07u),
                    payload
                ),
                wmt4WaveFxmColor = wmt4WaveFxmColor.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x08u),
                    payload
                ),
                wmt4WaveFxmDepth = wmt4WaveFxmDepth.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x09u),
                    payload
                ),
                wmt4WaveTempoSync = wmt4WaveTempoSync.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Au),
                    payload
                ),
                wmt4WaveCoarseTune = wmt4WaveCoarseTune.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Bu),
                    payload
                ),
                wmt4WaveFineTune = wmt4WaveFineTune.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Cu),
                    payload
                ),
                wmt4WavePan = wmt4WavePan.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Du), payload),
                wmt4WaveRandomPanSwitch = wmt4WaveRandomPanSwitch.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Eu),
                    payload
                ),
                wmt4WaveAlternatePanSwitch = wmt4WaveAlternatePanSwitch.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x0Fu),
                    payload
                ),
                wmt4WaveLevel = wmt4WaveLevel.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x10u), payload),
                wmt4VelocityRange = wmt4VelocityRange.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x11u),
                    payload
                ),
                wmt4VelocityFadeWidth = wmt4VelocityFadeWidth.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x13u),
                    payload
                ),

                pitchEnvDepth = pitchEnvDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x15u), payload),
                pitchEnvVelocitySens = pitchEnvVelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x16u),
                    payload
                ),
                pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x17u),
                    payload
                ),
                pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x18u),
                    payload
                ),

                pitchEnvTime1 = pitchEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x19u), payload),
                pitchEnvTime2 = pitchEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Au), payload),
                pitchEnvTime3 = pitchEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Bu), payload),
                pitchEnvTime4 = pitchEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Cu), payload),

                pitchEnvLevel0 = pitchEnvLevel0.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Du), payload),
                pitchEnvLevel1 = pitchEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Eu), payload),
                pitchEnvLevel2 = pitchEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x1Fu), payload),
                pitchEnvLevel3 = pitchEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x20u), payload),
                pitchEnvLevel4 = pitchEnvLevel4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x21u), payload),

                tvfFilterType = tvfFilterType.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x22u), payload),
                tvfCutoffFrequency = tvfCutoffFrequency.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x23u),
                    payload
                ),
                tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x24u),
                    payload
                ),
                tvfCutoffVelocitySens = tvfCutoffVelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x25u),
                    payload
                ),
                tvfResonance = tvfResonance.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x26u), payload),
                tvfResonanceVelocitySens = tvfResonanceVelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x27u),
                    payload
                ),
                tvfEnvDepth = tvfEnvDepth.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x28u), payload),
                tvfEnvVelocityCurveType = tvfEnvVelocityCurveType.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x29u),
                    payload
                ),
                tvfEnvVelocitySens = tvfEnvVelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Au),
                    payload
                ),
                tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Bu),
                    payload
                ),
                tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Cu),
                    payload
                ),
                tvfEnvTime1 = tvfEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Du), payload),
                tvfEnvTime2 = tvfEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Eu), payload),
                tvfEnvTime3 = tvfEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x2Fu), payload),
                tvfEnvTime4 = tvfEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x30u), payload),
                tvfEnvLevel0 = tvfEnvLevel0.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x31u), payload),
                tvfEnvLevel1 = tvfEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x32u), payload),
                tvfEnvLevel2 = tvfEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x33u), payload),
                tvfEnvLevel3 = tvfEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x34u), payload),
                tvfEnvLevel4 = tvfEnvLevel4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x35u), payload),

                tvaLevelVelocityCurve = tvaLevelVelocityCurve.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x36u),
                    payload
                ),
                tvaLevelVelocitySens = tvaLevelVelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x37u),
                    payload
                ),
                tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x38u),
                    payload
                ),
                tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.interpret(
                    startAddress.offsetBy(mlsb = 0x01u, lsb = 0x39u),
                    payload
                ),
                tvaEnvTime1 = tvaEnvTime1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Au), payload),
                tvaEnvTime2 = tvaEnvTime2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Bu), payload),
                tvaEnvTime3 = tvaEnvTime3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Cu), payload),
                tvaEnvTime4 = tvaEnvTime4.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Du), payload),
                tvaEnvLevel1 = tvaEnvLevel1.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Eu), payload),
                tvaEnvLevel2 = tvaEnvLevel2.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x3Fu), payload),
                tvaEnvLevel3 = tvaEnvLevel3.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x40u), payload),

                oneShotMode = oneShotMode.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x41u), payload),
            )
        }
    }

    class PcmDrumKitCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitCommon2>() {
        override val size = Integra7Size(0x32u)

        val phraseNumber = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x10u))
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x31u))

        override fun interpret(
            startAddress: Integra7Address,
            payload: SparseUByteArray
        ): PcmDrumKitCommon2 {
            assert(startAddress >= address)

            return PcmDrumKitCommon2(
                phraseNumber = phraseNumber.interpret(startAddress.offsetBy(lsb = 0x10u), payload),
                tfxSwitch = tfxSwitch.interpret(startAddress.offsetBy(lsb = 0x31u), payload),
            )
        }
    }
}