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

    @ExperimentalUnsignedTypes
    class PcmSynth7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<PcmSynthTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x30u.toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = PcmSynthToneCommonBuilder(deviceId, address, part)
        val mfx = MfxSysEx(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part)
        val partialMixTable = PcmSynthTonePartialMixTableBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u), part)
        val partials = IntRange(0, 3)
            .map { PcmSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = it), part) }
        val common2 = PcmSynthToneCommon2Builder(deviceId, address.offsetBy(mlsb = 0x30u, lsb = 0x00u), part)

        override fun deserialize(payload: SparseUByteArray): PcmSynthTone {
            assert(this.isCovering(payload)) { "Not a PCM synth tone ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return PcmSynthTone(
                common = common.deserialize(payload),
                mfx = mfx.deserialize(payload),
                partialMixTable = partialMixTable.deserialize(payload),
                partials = partials.map { it.deserialize(payload) },
                common2 = common2.deserialize(payload), //else null,
            )
        }
    }

    @ExperimentalUnsignedTypes
    class PcmSynthToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneCommon>() {
        override val size = Integra7Size(0x50u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val priority = EnumField(deviceId, address.offsetBy(lsb = 0x10u), Priority::fromValue)
        val coarseTuning = ByteField(deviceId, address.offsetBy(lsb = 0x11u), -48..48)
        val fineTuning = ByteField(deviceId, address.offsetBy(lsb = 0x12u), -50..50)
        val ocataveShift = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val stretchTuneDepth = UByteField(deviceId, address.offsetBy(lsb = 0x14u), 0..3)
        val analogFeel = UByteField(deviceId, address.offsetBy(lsb = 0x15u))
        val monoPoly = EnumField(deviceId, address.offsetBy(lsb = 0x16u), MonoPoly::values)
        val legatoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x17u))
        val legatoRetrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x18u))
        val portamentoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x19u))
        val portamentoMode = EnumField(deviceId, address.offsetBy(lsb = 0x1Au), PortamentoMode::fromValue)
        val portamentoType = EnumField(deviceId, address.offsetBy(lsb = 0x1Bu), PortamentoType::fromValue)
        val portamentoStart = EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), PortamentoStart::fromValue)
        val portamentoTime = UByteField(deviceId, address.offsetBy(lsb = 0x1Du))

        val cutoffOffset = ByteField(deviceId, address.offsetBy(lsb = 0x22u))
        val resonanceOffset = ByteField(deviceId, address.offsetBy(lsb = 0x23u))
        val attackTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x24u))
        val releaseTimeOffset = ByteField(deviceId, address.offsetBy(lsb = 0x25u))
        val velocitySensOffset = ByteField(deviceId, address.offsetBy(lsb = 0x26u))

        val pmtControlSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x28u))
        val pitchBendRangeUp = UByteField(deviceId, address.offsetBy(lsb = 0x29u), 0..48)
        val pitchBendRangeDown = UByteField(deviceId, address.offsetBy(lsb = 0x2Au), 0..48)

        val matrixControl1Source = EnumField(deviceId, address.offsetBy(lsb = 0x2Bu), MatrixControlSource::fromValue)
        val matrixControl1Destination1 = EnumField(deviceId, address.offsetBy(lsb = 0x2Cu), MatrixControlDestination::fromValue)
        val matrixControl1Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val matrixControl1Destination2 = EnumField(deviceId, address.offsetBy(lsb = 0x2Eu), MatrixControlDestination::fromValue)
        val matrixControl1Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x2Fu))
        val matrixControl1Destination3 = EnumField(deviceId, address.offsetBy(lsb = 0x30u), MatrixControlDestination::fromValue)
        val matrixControl1Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x31u))
        val matrixControl1Destination4 = EnumField(deviceId, address.offsetBy(lsb = 0x32u), MatrixControlDestination::fromValue)
        val matrixControl1Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x33u))

        val matrixControl2Source = EnumField(deviceId, address.offsetBy(lsb = 0x34u), MatrixControlSource::fromValue)
        val matrixControl2Destination1 = EnumField(deviceId, address.offsetBy(lsb = 0x35u), MatrixControlDestination::fromValue)
        val matrixControl2Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x36u))
        val matrixControl2Destination2 = EnumField(deviceId, address.offsetBy(lsb = 0x37u), MatrixControlDestination::fromValue)
        val matrixControl2Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x38u))
        val matrixControl2Destination3 = EnumField(deviceId, address.offsetBy(lsb = 0x39u), MatrixControlDestination::fromValue)
        val matrixControl2Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x3Au))
        val matrixControl2Destination4 = EnumField(deviceId, address.offsetBy(lsb = 0x3Bu), MatrixControlDestination::fromValue)
        val matrixControl2Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x3Cu))

        val matrixControl3Source = EnumField(deviceId, address.offsetBy(lsb = 0x3Du), MatrixControlSource::fromValue)
        val matrixControl3Destination1 = EnumField(deviceId, address.offsetBy(lsb = 0x3Eu), MatrixControlDestination::fromValue)
        val matrixControl3Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x3Fu))
        val matrixControl3Destination2 = EnumField(deviceId, address.offsetBy(lsb = 0x40u), MatrixControlDestination::fromValue)
        val matrixControl3Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x41u))
        val matrixControl3Destination3 = EnumField(deviceId, address.offsetBy(lsb = 0x42u), MatrixControlDestination::fromValue)
        val matrixControl3Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x43u))
        val matrixControl3Destination4 = EnumField(deviceId, address.offsetBy(lsb = 0x44u), MatrixControlDestination::fromValue)
        val matrixControl3Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x45u))

        val matrixControl4Source = EnumField(deviceId, address.offsetBy(lsb = 0x46u), MatrixControlSource::fromValue)
        val matrixControl4Destination1 = EnumField(deviceId, address.offsetBy(lsb = 0x47u), MatrixControlDestination::fromValue)
        val matrixControl4Sens1 = ByteField(deviceId, address.offsetBy(lsb = 0x48u))
        val matrixControl4Destination2 = EnumField(deviceId, address.offsetBy(lsb = 0x49u), MatrixControlDestination::fromValue)
        val matrixControl4Sens2 = ByteField(deviceId, address.offsetBy(lsb = 0x4Au))
        val matrixControl4Destination3 = EnumField(deviceId, address.offsetBy(lsb = 0x4Bu), MatrixControlDestination::fromValue)
        val matrixControl4Sens3 = ByteField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val matrixControl4Destination4 = EnumField(deviceId, address.offsetBy(lsb = 0x4Du), MatrixControlDestination::fromValue)
        val matrixControl4Sens4 = ByteField(deviceId, address.offsetBy(lsb = 0x4Eu))

        override fun deserialize(payload: SparseUByteArray): PcmSynthToneCommon {
            assert(this.isCovering(payload)) { "Not PCM synth tone common ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            try {
                return PcmSynthToneCommon(
                    name = name.deserialize(payload),
                    level = level.deserialize(payload),
                    pan = pan.deserialize(payload),
                    priority = priority.deserialize(payload),
                    coarseTuning = coarseTuning.deserialize(payload),
                    fineTuning = fineTuning.deserialize(payload),
                    ocataveShift = ocataveShift.deserialize(payload),
                    stretchTuneDepth = stretchTuneDepth.deserialize(payload),
                    analogFeel = analogFeel.deserialize(payload),
                    monoPoly = monoPoly.deserialize(payload),
                    legatoSwitch = legatoSwitch.deserialize(payload),
                    legatoRetrigger = legatoRetrigger.deserialize(payload),
                    portamentoSwitch = portamentoSwitch.deserialize(payload),
                    portamentoMode = portamentoMode.deserialize(payload),
                    portamentoType = portamentoType.deserialize(payload),
                    portamentoStart = portamentoStart.deserialize(payload),
                    portamentoTime = portamentoTime.deserialize(payload),

                    cutoffOffset = cutoffOffset.deserialize(payload),
                    resonanceOffset = resonanceOffset.deserialize(payload),
                    attackTimeOffset = attackTimeOffset.deserialize(payload),
                    releaseTimeOffset = releaseTimeOffset.deserialize(payload),
                    velocitySensOffset = velocitySensOffset.deserialize(payload),

                    pmtControlSwitch = pmtControlSwitch.deserialize(payload),
                    pitchBendRangeUp = pitchBendRangeUp.deserialize(payload),
                    pitchBendRangeDown = pitchBendRangeDown.deserialize(payload),

                    matrixControl1Source = matrixControl1Source.deserialize(payload),
                    matrixControl1Destination1 = matrixControl1Destination1.deserialize(payload),
                    matrixControl1Sens1 = matrixControl1Sens1.deserialize(payload),
                    matrixControl1Destination2 = matrixControl1Destination2.deserialize(payload),
                    matrixControl1Sens2 = matrixControl1Sens2.deserialize(payload),
                    matrixControl1Destination3 = matrixControl1Destination3.deserialize(payload),
                    matrixControl1Sens3 = matrixControl1Sens3.deserialize(payload),
                    matrixControl1Destination4 = matrixControl1Destination4.deserialize(payload),
                    matrixControl1Sens4 = matrixControl1Sens4.deserialize(payload),

                    matrixControl2Source = matrixControl2Source.deserialize(payload),
                    matrixControl2Destination1 = matrixControl2Destination1.deserialize(payload),
                    matrixControl2Sens1 = matrixControl2Sens1.deserialize(payload),
                    matrixControl2Destination2 = matrixControl2Destination2.deserialize(payload),
                    matrixControl2Sens2 = matrixControl2Sens2.deserialize(payload),
                    matrixControl2Destination3 = matrixControl2Destination3.deserialize(payload),
                    matrixControl2Sens3 = matrixControl2Sens3.deserialize(payload),
                    matrixControl2Destination4 = matrixControl2Destination4.deserialize(payload),
                    matrixControl2Sens4 = matrixControl2Sens4.deserialize(payload),

                    matrixControl3Source = matrixControl3Source.deserialize(payload),
                    matrixControl3Destination1 = matrixControl3Destination1.deserialize(payload),
                    matrixControl3Sens1 = matrixControl3Sens1.deserialize(payload),
                    matrixControl3Destination2 = matrixControl3Destination2.deserialize(payload),
                    matrixControl3Sens2 = matrixControl3Sens2.deserialize(payload),
                    matrixControl3Destination3 = matrixControl3Destination3.deserialize(payload),
                    matrixControl3Sens3 = matrixControl3Sens3.deserialize(payload),
                    matrixControl3Destination4 = matrixControl3Destination4.deserialize(payload),
                    matrixControl3Sens4 = matrixControl3Sens4.deserialize(payload),

                    matrixControl4Source = matrixControl4Source.deserialize(payload),
                    matrixControl4Destination1 = matrixControl4Destination1.deserialize(payload),
                    matrixControl4Sens1 = matrixControl4Sens1.deserialize(payload),
                    matrixControl4Destination2 = matrixControl4Destination2.deserialize(payload),
                    matrixControl4Sens2 = matrixControl4Sens2.deserialize(payload),
                    matrixControl4Destination3 = matrixControl4Destination3.deserialize(payload),
                    matrixControl4Sens3 = matrixControl4Sens3.deserialize(payload),
                    matrixControl4Destination4 = matrixControl4Destination4.deserialize(payload),
                    matrixControl4Sens4 = matrixControl4Sens4.deserialize(payload),
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

    @ExperimentalUnsignedTypes
    class MfxSysEx(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneMfx>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x11u.toUByte7()))

        val mfxType = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val mfxChorusSend = UByteField(deviceId, address.offsetBy(lsb = 0x02u))
        val mfxReverbSend = UByteField(deviceId, address.offsetBy(lsb = 0x03u))

        val mfxControl1Source = EnumField(deviceId, address.offsetBy(lsb = 0x05u), MfxControlSource::fromValue)
        val mfxControl1Sens = ByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val mfxControl2Source = EnumField(deviceId, address.offsetBy(lsb = 0x07u), MfxControlSource::fromValue)
        val mfxControl2Sens = ByteField(deviceId, address.offsetBy(lsb = 0x08u))
        val mfxControl3Source = EnumField(deviceId, address.offsetBy(lsb = 0x09u), MfxControlSource::fromValue)
        val mfxControl3Sens = ByteField(deviceId, address.offsetBy(lsb = 0x0Au))
        val mfxControl4Source = EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), MfxControlSource::fromValue)
        val mfxControl4Sens = ByteField(deviceId, address.offsetBy(lsb = 0x0Cu))

        val mfxControlAssignments = IntRange(0, 3)
            .map { UByteField(deviceId, address.offsetBy(lsb = 0x0Du).offsetBy(0x01.toUInt7(), factor = it)) }

        val mfxParameters = IntRange(0, 30) // TODO: 31
            .map { SignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x11u).offsetBy(0x04.toUInt7(), factor = it)) }

        override fun deserialize(payload: SparseUByteArray): PcmSynthToneMfx {
            assert(this.isCovering(payload)) { "Not a MFX definition ($address..${address.offsetBy(size)} for part $part but ${address.toStringDetailed()}" }

            try {
            return PcmSynthToneMfx(
                mfxType = mfxType.deserialize(payload),
                mfxChorusSend = mfxChorusSend.deserialize(payload),
                mfxReverbSend = mfxReverbSend.deserialize(payload),

                mfxControl1Source = mfxControl1Source.deserialize(payload),
                mfxControl1Sens = mfxControl1Sens.deserialize(payload),
                mfxControl2Source = mfxControl2Source.deserialize(payload),
                mfxControl2Sens = mfxControl2Sens.deserialize(payload),
                mfxControl3Source = mfxControl3Source.deserialize(payload),
                mfxControl3Sens = mfxControl3Sens.deserialize(payload),
                mfxControl4Source = mfxControl4Source.deserialize(payload),
                mfxControl4Sens = mfxControl4Sens.deserialize(payload),

                mfxControlAssignments = mfxControlAssignments
                    .map { it.deserialize(payload) },
                mfxParameters = mfxParameters
                    .map { it.deserialize(payload) },
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

    @ExperimentalUnsignedTypes
    class PcmSynthTonePartialMixTableBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthTonePartialMixTable>() {
        override val size = Integra7Size(0x29u.toUInt7UsingValue())

        val structureType12 = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val booster12 = UByteField(deviceId, address.offsetBy(lsb = 0x01u)) // ENUM
        val structureType34 = UByteField(deviceId, address.offsetBy(lsb = 0x02u))
        val booster34 = UByteField(deviceId, address.offsetBy(lsb = 0x03u)) // ENUM

        val velocityControl =
            EnumField(deviceId, address.offsetBy(lsb = 0x04u), VelocityControl::values)

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

        override fun deserialize(
            payload: SparseUByteArray
        ): PcmSynthTonePartialMixTable {
            assert(this.isCovering(payload)) { "Not a PCM synth tone PMT ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            try {
                return PcmSynthTonePartialMixTable(
                    structureType12 = structureType12.deserialize(payload),
                    booster12 = booster12.deserialize(payload),
                    structureType34 = structureType34.deserialize(payload),
                    booster34 = booster34.deserialize(payload),

                    velocityControl = velocityControl.deserialize(payload),

                    pmt1PartialSwitch = pmt1PartialSwitch.deserialize(payload),
                    pmt1KeyboardRange = pmt1KeyboardRange.deserialize(payload),
                    pmt1KeyboardFadeWidth = pmt1KeyboardFadeWidth.deserialize(payload),
                    pmt1VelocityRange = pmt1VelocityRange.deserialize(payload),
                    pmt1VelocityFade = pmt1VelocityFade.deserialize(payload),

                    pmt2PartialSwitch = pmt2PartialSwitch.deserialize(payload),
                    pmt2KeyboardRange = pmt2KeyboardRange.deserialize(payload),
                    pmt2KeyboardFadeWidth = pmt2KeyboardFadeWidth.deserialize(payload),
                    pmt2VelocityRange = pmt2VelocityRange.deserialize(payload),
                    pmt2VelocityFade = pmt2VelocityFade.deserialize(payload),

                    pmt3PartialSwitch = pmt3PartialSwitch.deserialize(payload),
                    pmt3KeyboardRange = pmt3KeyboardRange.deserialize(payload),
                    pmt3KeyboardFadeWidth = pmt3KeyboardFadeWidth.deserialize(payload),
                    pmt3VelocityRange = pmt3VelocityRange.deserialize(payload),
                    pmt3VelocityFade = pmt3VelocityFade.deserialize(payload),

                    pmt4PartialSwitch = pmt4PartialSwitch.deserialize(payload),
                    pmt4KeyboardRange = pmt4KeyboardRange.deserialize(payload),
                    pmt4KeyboardFadeWidth = pmt4KeyboardFadeWidth.deserialize(payload),
                    pmt4VelocityRange = pmt4VelocityRange.deserialize(payload),
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

    @ExperimentalUnsignedTypes
    class PcmSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthTonePartial>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01.toUByte7(), lsb = 0x1Au.toUByte7()))

        val level = UByteField(deviceId, address.offsetBy(lsb = 0x00u))
        val chorusTune = ByteField(deviceId, address.offsetBy(lsb = 0x01u), -48..48)
        val fineTune = ByteField(deviceId, address.offsetBy(lsb = 0x02u), -50..50)
        val randomPithDepth =
            EnumField(deviceId, address.offsetBy(lsb = 0x03u), RandomPithDepth::values)
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x04u))
        // TODO val panKeyFollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x05u), -100..100)
        val panDepth = UByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val alternatePanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x07u))
        val envMode = EnumField(deviceId, address.offsetBy(lsb = 0x08u), EnvMode::values)
        val delayMode = EnumField(deviceId, address.offsetBy(lsb = 0x09u), DelayMode::values)
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
            EnumField(deviceId, address.offsetBy(lsb = 0x17u), OffOnReverse::values)
        val partialControl1Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x18u), OffOnReverse::values)
        val partialControl1Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x19u), OffOnReverse::values)
        val partialControl1Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Au), OffOnReverse::values)
        val partialControl2Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Bu), OffOnReverse::values)
        val partialControl2Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), OffOnReverse::values)
        val partialControl2Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Du), OffOnReverse::values)
        val partialControl2Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Eu), OffOnReverse::values)
        val partialControl3Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), OffOnReverse::values)
        val partialControl3Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), OffOnReverse::values)
        val partialControl3Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x21u), OffOnReverse::values)
        val partialControl3Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x22u), OffOnReverse::values)
        val partialControl4Switch1 =
            EnumField(deviceId, address.offsetBy(lsb = 0x23u), OffOnReverse::values)
        val partialControl4Switch2 =
            EnumField(deviceId, address.offsetBy(lsb = 0x24u), OffOnReverse::values)
        val partialControl4Switch3 =
            EnumField(deviceId, address.offsetBy(lsb = 0x25u), OffOnReverse::values)
        val partialControl4Switch4 =
            EnumField(deviceId, address.offsetBy(lsb = 0x26u), OffOnReverse::values)

        val waveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x27u), WaveGroupType::values)
        val waveGroupId = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x28u), 0..16384)
        val waveNumberL = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Cu), 0..16384)
        val waveNumberR = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x30u), 0..16384)
        val waveGain = EnumField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain::values)
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
            EnumField(deviceId, address.offsetBy(lsb = 0x48u), TvfFilterType::values)
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
            EnumField(deviceId, address.offsetBy(lsb = 0x60u), BiasDirection::values)
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
            EnumField(deviceId, address.offsetBy(lsb = 0x6Du), LfoWaveForm::values)
        val lfo1Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x6Eu), 0..149)
        val lfo1Offset = EnumField(deviceId, address.offsetBy(lsb = 0x70u), LfoOffset::values)
        val lfo1RateDetune = UByteField(deviceId, address.offsetBy(lsb = 0x71u))
        val lfo1DelayTime = UByteField(deviceId, address.offsetBy(lsb = 0x72u))
        // TODO val lfo1Keyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x73u), -100..100)
        val lfo1FadeMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x74u), LfoFadeMode::values)
        val lfo1FadeTime = UByteField(deviceId, address.offsetBy(lsb = 0x75u))
        val lfo1KeyTrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x76u))
        val lfo1PitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x77u))
        val lfo1TvfDepth = ByteField(deviceId, address.offsetBy(lsb = 0x78u))
        val lfo1TvaDepth = ByteField(deviceId, address.offsetBy(lsb = 0x79u))
        val lfo1PanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x7Au))

        val lfo2WaveForm =
            EnumField(deviceId, address.offsetBy(lsb = 0x7Bu), LfoWaveForm::values)
        val lfo2Rate = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x7Cu), 0..149)
        val lfo2Offset = EnumField(deviceId, address.offsetBy(lsb = 0x7Eu), LfoOffset::values)
        val lfo2RateDetune = UByteField(deviceId, address.offsetBy(lsb = 0x7Fu))
        val lfo2DelayTime = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x00u))
        // TODO val lfo2Keyfollow = SignedValueField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x01u), -100..100)
        val lfo2FadeMode = EnumField(
            deviceId,
            address.offsetBy(mlsb = 0x01u, lsb = 0x02u),
            LfoFadeMode::values
        )
        val lfo2FadeTime = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x03u))
        val lfo2KeyTrigger = BooleanField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x04u))
        val lfo2PitchDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x05u))
        val lfo2TvfDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u))
        val lfo2TvaDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x07u))
        val lfo2PanDepth = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x08u))

        val lfoStepType = UByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x09u), 0..1)
        val lfoStep1 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Au), -36..36)
        val lfoStep2 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Bu), -36..36)
        val lfoStep3 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Cu), -36..36)
        val lfoStep4 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Du), -36..36)
        val lfoStep5 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Eu), -36..36)
        val lfoStep6 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x0Fu), -36..36)
        val lfoStep7 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x10u), -36..36)
        val lfoStep8 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x11u), -36..36)
        val lfoStep9 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x12u), -36..36)
        val lfoStep10 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x13u), -36..36)
        val lfoStep11 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x14u), -36..36)
        val lfoStep12 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x15u), -36..36)
        val lfoStep13 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x16u), -36..36)
        val lfoStep14 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x17u), -36..36)
        val lfoStep15 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x18u), -36..36)
        val lfoStep16 = ByteField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x19u), -36..36)

        override fun deserialize(
            payload: SparseUByteArray
        ): PcmSynthTonePartial {
            assert(this.isCovering(payload)) { "Not a PCM synth tone partial ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            try {
                return PcmSynthTonePartial(
                    level = level.deserialize(payload),
                    chorusTune = chorusTune.deserialize(payload),
                    fineTune = fineTune.deserialize(payload),
                    randomPithDepth = randomPithDepth.deserialize(payload),
                    pan = pan.deserialize(payload),
                    // panKeyFollow = panKeyFollow.interpret(startAddress.offsetBy(lsb = 0x05u), length, payload),
                    panDepth = panDepth.deserialize(payload),
                    alternatePanDepth = alternatePanDepth.deserialize(payload),
                    envMode = envMode.deserialize(payload),
                    delayMode = delayMode.deserialize(payload),
                    delayTime = delayTime.deserialize(payload),

                    outputLevel = outputLevel.deserialize(payload),
                    chorusSendLevel = chorusSendLevel.deserialize(payload),
                    reverbSendLevel = reverbSendLevel.deserialize(payload),

                    receiveBender = receiveBender.deserialize(payload),
                    receiveExpression = receiveExpression.deserialize(payload),
                    receiveHold1 = receiveHold1.deserialize(payload),
                    redamper = redamper.deserialize(payload),

                    partialControl1Switch1 = partialControl1Switch1.deserialize(payload),
                    partialControl1Switch2 = partialControl1Switch2.deserialize(payload),
                    partialControl1Switch3 = partialControl1Switch3.deserialize(payload),
                    partialControl1Switch4 = partialControl1Switch4.deserialize(payload),
                    partialControl2Switch1 = partialControl2Switch1.deserialize(payload),
                    partialControl2Switch2 = partialControl2Switch2.deserialize(payload),
                    partialControl2Switch3 = partialControl2Switch3.deserialize(payload),
                    partialControl2Switch4 = partialControl2Switch4.deserialize(payload),
                    partialControl3Switch1 = partialControl3Switch1.deserialize(payload),
                    partialControl3Switch2 = partialControl3Switch2.deserialize(payload),
                    partialControl3Switch3 = partialControl3Switch3.deserialize(payload),
                    partialControl3Switch4 = partialControl3Switch4.deserialize(payload),
                    partialControl4Switch1 = partialControl4Switch1.deserialize(payload),
                    partialControl4Switch2 = partialControl4Switch2.deserialize(payload),
                    partialControl4Switch3 = partialControl4Switch3.deserialize(payload),
                    partialControl4Switch4 = partialControl4Switch4.deserialize(payload),

                    waveGroupType = waveGroupType.deserialize(payload),
                    waveGroupId = waveGroupId.deserialize(payload),
                    waveNumberL = waveNumberL.deserialize(payload),
                    waveNumberR = waveNumberR.deserialize(payload),
                    waveGain = waveGain.deserialize(payload),
                    waveFXMSwitch = waveFXMSwitch.deserialize(payload),
                    waveFXMColor = waveFXMColor.deserialize(payload),
                    waveFXMDepth = waveFXMDepth.deserialize(payload),
                    waveTempoSync = waveTempoSync.deserialize(payload),
                    // wavePitchKeyfollow = wavePitchKeyfollow.interpret(startAddress.offsetBy(lsb = 0x39u), length, payload),

                    pitchEnvDepth = pitchEnvDepth.deserialize(payload),
                    pitchEnvVelocitySens = pitchEnvVelocitySens.deserialize(payload),
                    pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.deserialize(payload),
                    pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.deserialize(payload),
                    // pitchEnvTimeKeyfollow = pitchEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x34u), length, payload),
                    pitchEnvTime1 = pitchEnvTime1.deserialize(payload),
                    pitchEnvTime2 = pitchEnvTime2.deserialize(payload),
                    pitchEnvTime3 = pitchEnvTime3.deserialize(payload),
                    pitchEnvTime4 = pitchEnvTime4.deserialize(payload),
                    pitchEnvLevel0 = pitchEnvLevel0.deserialize(payload),
                    pitchEnvLevel1 = pitchEnvLevel1.deserialize(payload),
                    pitchEnvLevel2 = pitchEnvLevel2.deserialize(payload),
                    pitchEnvLevel3 = pitchEnvLevel3.deserialize(payload),
                    pitchEnvLevel4 = pitchEnvLevel4.deserialize(payload),

                    tvfFilterType = tvfFilterType.deserialize(payload),
                    tvfCutoffFrequency = tvfCutoffFrequency.deserialize(payload),
                    // tvfCutoffKeyfollow = tvfCutoffKeyfollow.interpret(startAddress.offsetBy(lsb = 0x4Au), length, payload),
                    tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.deserialize(payload),
                    tvfCutoffVelocitySens = tvfCutoffVelocitySens.deserialize(payload),
                    tvfResonance = tvfResonance.deserialize(payload),
                    tvfResonanceVelocitySens = tvfResonanceVelocitySens.deserialize(payload),
                    tvfEnvDepth = tvfEnvDepth.deserialize(payload),
                    tvfEnvVelocityCurve = tvfEnvVelocityCurve.deserialize(payload),
                    tvfEnvVelocitySens = tvfEnvVelocitySens.deserialize(payload),
                    tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.deserialize(payload),
                    tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.deserialize(payload),
                    // tvfEnvTimeKeyfollow = tvfEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x54u), length, payload),
                    tvfEnvTime1 = tvfEnvTime1.deserialize(payload),
                    tvfEnvTime2 = tvfEnvTime2.deserialize(payload),
                    tvfEnvTime3 = tvfEnvTime3.deserialize(payload),
                    tvfEnvTime4 = tvfEnvTime4.deserialize(payload),
                    tvfEnvLevel0 = tvfEnvLevel0.deserialize(payload),
                    tvfEnvLevel1 = tvfEnvLevel1.deserialize(payload),
                    tvfEnvLevel2 = tvfEnvLevel2.deserialize(payload),
                    tvfEnvLevel3 = tvfEnvLevel3.deserialize(payload),
                    tvfEnvLevel4 = tvfEnvLevel4.deserialize(payload),

                    //biasLevel = biasLevel.interpret(startAddress.offsetBy(lsb = 0x5Eu), length, payload),
                    biasPosition = biasPosition.deserialize(payload),
                    biasDirection = biasDirection.deserialize(payload),
                    tvaLevelVelocityCurve = tvaLevelVelocityCurve.deserialize(payload),
                    tvaLevelVelocitySens = tvaLevelVelocitySens.deserialize(payload),
                    tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.deserialize(payload),
                    tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.deserialize(payload),
                    // tvaEnvTimeKeyfollow = tvaEnvTimeKeyfollow.interpret(startAddress.offsetBy(lsb = 0x65u), length, payload),
                    tvaEnvTime1 = tvaEnvTime1.deserialize(payload),
                    tvaEnvTime2 = tvaEnvTime2.deserialize(payload),
                    tvaEnvTime3 = tvaEnvTime3.deserialize(payload),
                    tvaEnvTime4 = tvaEnvTime4.deserialize(payload),
                    tvaEnvLevel1 = tvaEnvLevel1.deserialize(payload),
                    tvaEnvLevel2 = tvaEnvLevel2.deserialize(payload),
                    tvaEnvLevel3 = tvaEnvLevel3.deserialize(payload),

                    lfo1WaveForm = lfo1WaveForm.deserialize(payload),
                    lfo1Rate = lfo1Rate.deserialize(payload),
                    lfo1Offset = lfo1Offset.deserialize(payload),
                    lfo1RateDetune = lfo1RateDetune.deserialize(payload),
                    lfo1DelayTime = lfo1DelayTime.deserialize(payload),
                    // lfo1Keyfollow = lfo1Keyfollow.interpret(startAddress.offsetBy(lsb = 0x73u), length, payload),
                    lfo1FadeMode = lfo1FadeMode.deserialize(payload),
                    lfo1FadeTime = lfo1FadeTime.deserialize(payload),
                    lfo1KeyTrigger = lfo1KeyTrigger.deserialize(payload),
                    lfo1PitchDepth = lfo1PitchDepth.deserialize(payload),
                    lfo1TvfDepth = lfo1TvfDepth.deserialize(payload),
                    lfo1TvaDepth = lfo1TvaDepth.deserialize(payload),
                    lfo1PanDepth = lfo1PanDepth.deserialize(payload),

                    lfo2WaveForm = lfo2WaveForm.deserialize(payload),
                    lfo2Rate = lfo2Rate.deserialize(payload),
                    lfo2Offset = lfo2Offset.deserialize(payload),
                    lfo2RateDetune = lfo2RateDetune.deserialize(payload),
                    lfo2DelayTime = lfo2DelayTime.deserialize(payload),
                    // lfo2Keyfollow = lfo2Keyfollow.interpret(startAddress.offsetBy(mlsb = 0x01u, lsb = 0x01u), length, payload),
                    lfo2FadeMode = lfo2FadeMode.deserialize(payload),
                    lfo2FadeTime = lfo2FadeTime.deserialize(payload),
                    lfo2KeyTrigger = lfo2KeyTrigger.deserialize(payload),
                    lfo2PitchDepth = lfo2PitchDepth.deserialize(payload),
                    lfo2TvfDepth = lfo2TvfDepth.deserialize(payload),
                    lfo2TvaDepth = lfo2TvaDepth.deserialize(payload),
                    lfo2PanDepth = lfo2PanDepth.deserialize(payload),


                    lfoStepType = lfoStepType.deserialize(payload),
                    lfoStep1 = lfoStep1.deserialize(payload),
                    lfoStep2 = lfoStep2.deserialize(payload),
                    lfoStep3 = lfoStep3.deserialize(payload),
                    lfoStep4 = lfoStep4.deserialize(payload),
                    lfoStep5 = lfoStep5.deserialize(payload),
                    lfoStep6 = lfoStep6.deserialize(payload),
                    lfoStep7 = lfoStep7.deserialize(payload),
                    lfoStep8 = lfoStep8.deserialize(payload),
                    lfoStep9 = lfoStep9.deserialize(payload),
                    lfoStep10 = lfoStep10.deserialize(payload),
                    lfoStep11 = lfoStep11.deserialize(payload),
                    lfoStep12 = lfoStep12.deserialize(payload),
                    lfoStep13 = lfoStep13.deserialize(payload),
                    lfoStep14 = lfoStep14.deserialize(payload),
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

    @ExperimentalUnsignedTypes
    class PcmSynthToneCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmSynthToneCommon2>() {
        override val size = Integra7Size(0x3Cu.toUInt7UsingValue())

        val toneCategory = UByteField(deviceId, address.offsetBy(lsb = 0x10u))
        val undocumented = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x11u), 0..255)
        val phraseOctaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x13u), -3..3)
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x33u))
        val phraseNmber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x38u), 0..65535)


        override fun deserialize(
            payload: SparseUByteArray
        ): PcmSynthToneCommon2 {
            assert(this.isCovering(payload)) { "Not a PCM synth tone common2 ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            try {
                return PcmSynthToneCommon2(
                    toneCategory = toneCategory.deserialize(payload),
                    undocumented = undocumented.deserialize(payload),
                    phraseOctaveShift = phraseOctaveShift.deserialize(payload),
                    tfxSwitch = tfxSwitch.deserialize(payload),
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

    @ExperimentalUnsignedTypes
    class SuperNaturalSynth7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalSynthTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x22.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalSynthToneCommonBuilder(deviceId, address, part)
        val mfx = MfxSysEx(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val partial1 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x20u, lsb = 0x00u), part)
        val partial2 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x21u, lsb = 0x00u), part)
        val partial3 = SuperNaturalSynthTonePartialBuilder(deviceId, address.offsetBy(mlsb = 0x22u, lsb = 0x00u), part)

        override fun deserialize(payload: SparseUByteArray): SuperNaturalSynthTone {
            assert(this.isCovering(payload)) { "Not a SN-S part ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SuperNaturalSynthTone(
                common = common.deserialize(payload),
                mfx = mfx.deserialize(payload),
                partial1 = partial1.deserialize(payload),
                partial2 = partial2.deserialize(payload),
                partial3 = partial3.deserialize(payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
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

        val ringSwitch = EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), RingSwitch::values)
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x20u))

        val unisonSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val portamentoMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x31u), PortamentoMode::values)
        val legatoSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x32u))
        val analogFeel = UByteField(deviceId, address.offsetBy(lsb = 0x34u))
        val waveShape = UByteField(deviceId, address.offsetBy(lsb = 0x35u))
        val toneCategory = UByteField(deviceId, address.offsetBy(lsb = 0x36u))
        val phraseNumber =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x37u), 0..65535)
        val phraseOctaveShift = ByteField(deviceId, address.offsetBy(lsb = 0x3Bu), -3..3)
        val unisonSize = EnumField(deviceId, address.offsetBy(lsb = 0x3Cu), UnisonSize::values)

        override fun deserialize(
            payload: SparseUByteArray
        ): SupernaturalSynthToneCommon {
            assert(this.isCovering(payload)) { "Not a SN-S common ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SupernaturalSynthToneCommon(
                name = name.deserialize(payload),
                level = level.deserialize(payload),
                portamentoSwitch = portamentoSwitch.deserialize(payload),
                portamentoTime = portamentoTime.deserialize(payload),
                monoSwitch = monoSwitch.deserialize(payload),
                octaveShift = octaveShift.deserialize(payload),
                pithBendRangeUp = pithBendRangeUp.deserialize(payload),
                pitchBendRangeDown = pitchBendRangeDown.deserialize(payload),

                partial1Switch = partial1Switch.deserialize(payload),
                partial1Select = partial1Select.deserialize(payload),
                partial2Switch = partial2Switch.deserialize(payload),
                partial2Select = partial2Select.deserialize(payload),
                partial3Switch = partial3Switch.deserialize(payload),
                partial3Select = partial3Select.deserialize(payload),

                ringSwitch = ringSwitch.deserialize(payload),
                tfxSwitch = tfxSwitch.deserialize(payload),

                unisonSwitch = unisonSwitch.deserialize(payload),
                portamentoMode = portamentoMode.deserialize(payload),
                legatoSwitch = legatoSwitch.deserialize(payload),
                analogFeel = analogFeel.deserialize(payload),
                waveShape = waveShape.deserialize(payload),
                toneCategory = toneCategory.deserialize(payload),
                phraseNumber = phraseNumber.deserialize(payload),
                phraseOctaveShift = phraseOctaveShift.deserialize(payload),
                unisonSize = unisonSize.deserialize(payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalSynthTonePartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SuperNaturalSynthTonePartial>() {
        override val size = Integra7Size(0x3Du)

        val oscWaveForm =
            EnumField(deviceId, address.offsetBy(lsb = 0x00u), SnSWaveForm::values)
        val oscWaveFormVariation =
            EnumField(deviceId, address.offsetBy(lsb = 0x01u), SnsWaveFormVariation::values)
        val oscPitch = ByteField(deviceId, address.offsetBy(lsb = 0x03u), -24..24)
        val oscDetune = ByteField(deviceId, address.offsetBy(lsb = 0x04u), -50..50)
        val oscPulseWidthModulationDepth = UByteField(deviceId, address.offsetBy(lsb = 0x05u))
        val oscPulseWidth = UByteField(deviceId, address.offsetBy(lsb = 0x06u))
        val oscPitchAttackTime = UByteField(deviceId, address.offsetBy(lsb = 0x07u))
        val oscPitchEnvDecay = UByteField(deviceId, address.offsetBy(lsb = 0x08u))
        val oscPitchEnvDepth = ByteField(deviceId, address.offsetBy(lsb = 0x09u))

        val filterMode =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Au), SnsFilterMode::values)
        val filterSlope =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), SnsFilterSlope::values)
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

        val lfoShape = EnumField(deviceId, address.offsetBy(lsb = 0x1Cu), SnsLfoShape::values)
        val lfoRate = UByteField(deviceId, address.offsetBy(lsb = 0x1Du))
        val lfoTempoSyncSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Eu))
        val lfoTempoSyncNote =
            EnumField(deviceId, address.offsetBy(lsb = 0x1Fu), SnsLfoTempoSyncNote::values)
        val lfoFadeTime = UByteField(deviceId, address.offsetBy(lsb = 0x20u))
        val lfoKeyTrigger = BooleanField(deviceId, address.offsetBy(lsb = 0x21u))
        val lfoPitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x22u))
        val lfoFilterDepth = ByteField(deviceId, address.offsetBy(lsb = 0x23u))
        val lfoAmpDepth = ByteField(deviceId, address.offsetBy(lsb = 0x24u))
        val lfoPanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x25u))

        val modulationShape =
            EnumField(deviceId, address.offsetBy(lsb = 0x26u), SnsLfoShape::values)
        val modulationLfoRate = UByteField(deviceId, address.offsetBy(lsb = 0x27u))
        val modulationLfoTempoSyncSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x28u))
        val modulationLfoTempoSyncNote =
            EnumField(deviceId, address.offsetBy(lsb = 0x29u), SnsLfoTempoSyncNote::values)
        val oscPulseWidthShift = UByteField(deviceId, address.offsetBy(lsb = 0x2Au))
        val modulationLfoPitchDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Cu))
        val modulationLfoFilterDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val modulationLfoAmpDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Eu))
        val modulationLfoPanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x2Fu))

        val cutoffAftertouchSens = ByteField(deviceId, address.offsetBy(lsb = 0x30u))
        val levelAftertouchSens = ByteField(deviceId, address.offsetBy(lsb = 0x31u))

        val waveGain = EnumField(deviceId, address.offsetBy(lsb = 0x34u), WaveGain::values)
        val waveNumber = UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x35u), 0..16384)
        val hpfCutoff = UByteField(deviceId, address.offsetBy(lsb = 0x39u))
        val superSawDetune = UByteField(deviceId, address.offsetBy(lsb = 0x3Au))
        val modulationLfoRateControl = ByteField(deviceId, address.offsetBy(lsb = 0x3Bu))
//        val ampLevelKeyfollow = SignedValueField(deviceId, address.offsetBy(lsb = 0x3Cu), 100..100)

        override fun deserialize(
            payload: SparseUByteArray
        ): SuperNaturalSynthTonePartial {
            assert(this.isCovering(payload)) { "Not a SN-S tone definition ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SuperNaturalSynthTonePartial(
                oscWaveForm = oscWaveForm.deserialize(payload),
                oscWaveFormVariation = oscWaveFormVariation.deserialize(payload),
                oscPitch = oscPitch.deserialize(payload),
                oscDetune = oscDetune.deserialize(payload),
                oscPulseWidthModulationDepth = oscPulseWidthModulationDepth.deserialize(
                    payload
                ),
                oscPulseWidth = oscPulseWidth.deserialize(payload),
                oscPitchAttackTime = oscPitchAttackTime.deserialize(payload),
                oscPitchEnvDecay = oscPitchEnvDecay.deserialize(payload),
                oscPitchEnvDepth = oscPitchEnvDepth.deserialize(payload),

                filterMode = filterMode.deserialize(payload),
                filterSlope = filterSlope.deserialize(payload),
                filterCutoff = filterCutoff.deserialize(payload),
//                filterCutoffKeyflow = filterCutoffKeyflow.interpret(startAddress.offsetBy(lsb = 0x0Du), length, payload),
                filterEnvVelocitySens = filterEnvVelocitySens.deserialize(payload),
                filterResonance = filterResonance.deserialize(payload),
                filterEnvAttackTime = filterEnvAttackTime.deserialize(payload),
                filterEnvDecayTime = filterEnvDecayTime.deserialize(payload),
                filterEnvSustainLevel = filterEnvSustainLevel.deserialize(payload),
                filterEnvReleaseTime = filterEnvReleaseTime.deserialize(payload),
                filterEnvDepth = filterEnvDepth.deserialize(payload),
                ampLevel = ampLevel.deserialize(payload),
                ampVelocitySens = ampVelocitySens.deserialize(payload),
                ampEnvAttackTime = ampEnvAttackTime.deserialize(payload),
                ampEnvDecayTime = ampEnvDecayTime.deserialize(payload),
                ampEnvSustainLevel = ampEnvSustainLevel.deserialize(payload),
                ampEnvReleaseTime = ampEnvReleaseTime.deserialize(payload),
                ampPan = ampPan.deserialize(payload),

                lfoShape = lfoShape.deserialize(payload),
                lfoRate = lfoRate.deserialize(payload),
                lfoTempoSyncSwitch = lfoTempoSyncSwitch.deserialize(payload),
                lfoTempoSyncNote = lfoTempoSyncNote.deserialize(payload),
                lfoFadeTime = lfoFadeTime.deserialize(payload),
                lfoKeyTrigger = lfoKeyTrigger.deserialize(payload),
                lfoPitchDepth = lfoPitchDepth.deserialize(payload),
                lfoFilterDepth = lfoFilterDepth.deserialize(payload),
                lfoAmpDepth = lfoAmpDepth.deserialize(payload),
                lfoPanDepth = lfoPanDepth.deserialize(payload),

                modulationShape = modulationShape.deserialize(payload),
                modulationLfoRate = modulationLfoRate.deserialize(payload),
                modulationLfoTempoSyncSwitch = modulationLfoTempoSyncSwitch.deserialize(payload),
                modulationLfoTempoSyncNote = modulationLfoTempoSyncNote.deserialize(payload),
                oscPulseWidthShift = oscPulseWidthShift.deserialize(payload),
                modulationLfoPitchDepth = modulationLfoPitchDepth.deserialize(payload),
                modulationLfoFilterDepth = modulationLfoFilterDepth.deserialize(payload),
                modulationLfoAmpDepth = modulationLfoAmpDepth.deserialize(payload),
                modulationLfoPanDepth = modulationLfoPanDepth.deserialize(payload),

                cutoffAftertouchSens = cutoffAftertouchSens.deserialize(payload),
                levelAftertouchSens = levelAftertouchSens.deserialize(payload),

                waveGain = waveGain.deserialize(payload),
                waveNumber = waveNumber.deserialize(payload),
                hpfCutoff = hpfCutoff.deserialize(payload),
                superSawDetune = superSawDetune.deserialize(payload),
                modulationLfoRateControl = modulationLfoRateControl.deserialize(payload),
//                ampLevelKeyfollow = ampLevelKeyfollow.interpret(startAddress.offsetBy(lsb = 0x3Cu), length, payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalAcoustic7PartSysEx(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalAcousticTone>() {
        override val size = Integra7Size(UInt7(mlsb = 0x30u.toUByte7(), lsb = 0x3Cu.toUByte7()))

        val common = SuperNaturalAcousticToneCommonBuilder(deviceId, address, part)
        val mfx = MfxSysEx(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM

        override fun deserialize(payload: SparseUByteArray): SuperNaturalAcousticTone {
            assert(this.isCovering(payload)) { "Not a SN-A part ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SuperNaturalAcousticTone(
                common = common.deserialize(payload),
                mfx = mfx.deserialize(payload)
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalAcousticToneCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalAcousticToneCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x10u))

        val monoPoly = EnumField(deviceId, address.offsetBy(lsb = 0x11u), MonoPoly::values)
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

        override fun deserialize(
            payload: SparseUByteArray
        ): SupernaturalAcousticToneCommon {
            assert(this.isCovering(payload)) { "Not SN-A Common ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SupernaturalAcousticToneCommon(
                name = name.deserialize(payload),
                level = level.deserialize(payload),
                monoPoly = monoPoly.deserialize(payload),
                portamentoTimeOffset = portamentoTimeOffset.deserialize(payload),
                cutoffOffset = cutoffOffset.deserialize(payload),
                resonanceOffset = resonanceOffset.deserialize(payload),
                attackTimeOffset = attackTimeOffset.deserialize(payload),
                releaseTimeOffset = releaseTimeOffset.deserialize(payload),
                vibratoRate = vibratoRate.deserialize(payload),
                vibratoDepth = vibratoDepth.deserialize(payload),
                vibratorDelay = vibratorDelay.deserialize(payload),
                octaveShift = octaveShift.deserialize(payload),
                category = category.deserialize(payload),
                phraseNumber = phraseNumber.deserialize(payload),
                phraseOctaveShift = phraseOctaveShift.deserialize(payload),

                tfxSwitch = tfxSwitch.deserialize(payload),

                instrumentVariation = instrumentVariation.deserialize(payload),
                instrumentNumber = instrumentNumber.deserialize(payload),
                modifyParameters = modifyParameters
                    .mapIndexed { idx, fd ->
                        fd.deserialize(
                            payload
                        )
                    }
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<SuperNaturalDrumKit>() {
        override val size = Integra7Size(UInt7(mlsb = 0x4D.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = SuperNaturalDrumKitCommonBuilder(deviceId, address, part)
        val mfx = MfxSysEx(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u), part)
        val notes = IntRange(0, 88-17)
            .map { SuperNaturalDrumKitNoteBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x01u, lsb = 0x00u, factor = it), part) }

        override fun deserialize(payload: SparseUByteArray): SuperNaturalDrumKit {
            assert(this.isCovering(payload)) { "Not a SN-D kit ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SuperNaturalDrumKit(
                common = common.deserialize(payload),
                mfx = mfx.deserialize(payload),
                commonCompEq = commonCompEq.deserialize(payload),
                notes = notes.map { it.deserialize(payload) }
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x10u))
        val ambienceLevel = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val phraseNo = UByteField(deviceId, address.offsetBy(lsb = 0x12u))
        val tfx = BooleanField(deviceId, address.offsetBy(lsb = 0x13u))

        override fun deserialize(
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommon {
            assert(this.isCovering(payload)) { "Not SN-D Common ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SupernaturalDrumKitCommon(
                name = name.deserialize(payload),
                level = level.deserialize(payload),
                ambienceLevel = ambienceLevel.deserialize(payload),
                phraseNo = phraseNo.deserialize(payload),
                tfx = tfx.deserialize(payload)
            )
        }
    }

    @ExperimentalUnsignedTypes
    class SuperNaturalDrumKitCommonCompEqBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<SupernaturalDrumKitCommonCompEq>() {
        override val size = Integra7Size(0x54u)

        val comp1Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x00u))
        val comp1AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x01u),
            SupernaturalDrumAttackTime::values
        )
        val comp1ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x02u),
            SupernaturalDrumReleaseTime::values
        )
        val comp1Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x03u))
        val comp1Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x04u), SupernaturalDrumRatio::values)
        val comp1OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x05u), 0..24)
        val eq1Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x06u))
        val eq1LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x07u),
            SupernaturalDrumLowFrequency::values
        )
        val eq1LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x08u), 0..30) // - 15
        val eq1MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x09u),
            SupernaturalDrumMidFrequency::values
        )
        val eq1MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x0Au), 0..30) // - 15
        val eq1MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Bu), SupernaturalDrumMidQ::values)
        val eq1HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x0Cu),
            SupernaturalDrumHighFrequency::values
        )
        val eq1HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x0Du), 0..30) // - 15

        val comp2Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val comp2AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x0Fu),
            SupernaturalDrumAttackTime::values
        )
        val comp2ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x10u),
            SupernaturalDrumReleaseTime::values
        )
        val comp2Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val comp2Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x12u), SupernaturalDrumRatio::values)
        val comp2OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x13u), 0..24)
        val eq2Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x14u))
        val eq2LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x15u),
            SupernaturalDrumLowFrequency::values
        )
        val eq2LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x16u), 0..30) // - 15
        val eq2MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x17u),
            SupernaturalDrumMidFrequency::values
        )
        val eq2MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x18u), 0..30) // - 15
        val eq2MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x19u), SupernaturalDrumMidQ::values)
        val eq2HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Au),
            SupernaturalDrumHighFrequency::values
        )
        val eq2HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x1Bu), 0..30) // - 15

        val comp3Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x1Cu))
        val comp3AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Du),
            SupernaturalDrumAttackTime::values
        )
        val comp3ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Eu),
            SupernaturalDrumReleaseTime::values
        )
        val comp3Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x1Fu))
        val comp3Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), SupernaturalDrumRatio::values)
        val comp3OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x21u), 0..24)
        val eq3Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x22u))
        val eq3LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x23u),
            SupernaturalDrumLowFrequency::values
        )
        val eq3LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x24u), 0..30) // - 15
        val eq3MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x25u),
            SupernaturalDrumMidFrequency::values
        )
        val eq3MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x26u), 0..30) // - 15
        val eq3MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x27u), SupernaturalDrumMidQ::values)
        val eq3HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x28u),
            SupernaturalDrumHighFrequency::values
        )
        val eq3HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x29u), 0..30) // - 15

        val comp4Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x2Au))
        val comp4AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x2Bu),
            SupernaturalDrumAttackTime::values
        )
        val comp4ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x2Cu),
            SupernaturalDrumReleaseTime::values
        )
        val comp4Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x2Du))
        val comp4Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x2Eu), SupernaturalDrumRatio::values)
        val comp4OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x2Fu), 0..24)
        val eq4Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x30u))
        val eq4LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x31u),
            SupernaturalDrumLowFrequency::values
        )
        val eq4LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x32u), 0..30) // - 15
        val eq4MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x33u),
            SupernaturalDrumMidFrequency::values
        )
        val eq4MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x34u), 0..30) // - 15
        val eq4MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x35u), SupernaturalDrumMidQ::values)
        val eq4HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x36u),
            SupernaturalDrumHighFrequency::values
        )
        val eq4HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x37u), 0..30) // - 15

        val comp5Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x38u))
        val comp5AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x39u),
            SupernaturalDrumAttackTime::values
        )
        val comp5ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x3Au),
            SupernaturalDrumReleaseTime::values
        )
        val comp5Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x3Bu))
        val comp5Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Cu), SupernaturalDrumRatio::values)
        val comp5OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x3Du), 0..24)
        val eq5Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val eq5LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x3Fu),
            SupernaturalDrumLowFrequency::values
        )
        val eq5LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x40u), 0..30) // - 15
        val eq5MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x41u),
            SupernaturalDrumMidFrequency::values
        )
        val eq5MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x42u), 0..30) // - 15
        val eq5MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x43u), SupernaturalDrumMidQ::values)
        val eq5HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x44u),
            SupernaturalDrumHighFrequency::values
        )
        val eq5HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x45u), 0..30) // - 15

        val comp6Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x46u))
        val comp6AttackTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x47u),
            SupernaturalDrumAttackTime::values
        )
        val comp6ReleaseTime = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x48u),
            SupernaturalDrumReleaseTime::values
        )
        val comp6Threshold = UByteField(deviceId, address.offsetBy(lsb = 0x49u))
        val comp6Ratio =
            EnumField(deviceId, address.offsetBy(lsb = 0x4Au), SupernaturalDrumRatio::values)
        val comp6OutputGain = UByteField(deviceId, address.offsetBy(lsb = 0x4Bu), 0..24)
        val eq6Switch = BooleanField(deviceId, address.offsetBy(lsb = 0x4Cu))
        val eq6LowFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x4Du),
            SupernaturalDrumLowFrequency::values
        )
        val eq6LowGain = UByteField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..30) // - 15
        val eq6MidFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x4Fu),
            SupernaturalDrumMidFrequency::values
        )
        val eq6MidGain = UByteField(deviceId, address.offsetBy(lsb = 0x50u), 0..30) // - 15
        val eq6MidQ =
            EnumField(deviceId, address.offsetBy(lsb = 0x51u), SupernaturalDrumMidQ::values)
        val eq6HighFrequency = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x52u),
            SupernaturalDrumHighFrequency::values
        )
        val eq6HighGain = UByteField(deviceId, address.offsetBy(lsb = 0x53u), 0..30) // - 15

        override fun deserialize(
            payload: SparseUByteArray
        ): SupernaturalDrumKitCommonCompEq {
            assert(this.isCovering(payload)) { "Not SN-D comp/eq ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SupernaturalDrumKitCommonCompEq(
                comp1Switch = comp1Switch.deserialize(payload),
                comp1AttackTime = comp1AttackTime.deserialize(payload),
                comp1ReleaseTime = comp1ReleaseTime.deserialize(payload),
                comp1Threshold = comp1Threshold.deserialize(payload),
                comp1Ratio = comp1Ratio.deserialize(payload),
                comp1OutputGain = comp1OutputGain.deserialize(payload),
                eq1Switch = eq1Switch.deserialize(payload),
                eq1LowFrequency = eq1LowFrequency.deserialize(payload),
                eq1LowGain = eq1LowGain.deserialize(payload) - 15,
                eq1MidFrequency = eq1MidFrequency.deserialize(payload),
                eq1MidGain = eq1MidGain.deserialize(payload) - 15,
                eq1MidQ = eq1MidQ.deserialize(payload),
                eq1HighFrequency = eq1HighFrequency.deserialize(payload),
                eq1HighGain = eq1HighGain.deserialize(payload) - 15,

                comp2Switch = comp2Switch.deserialize(payload),
                comp2AttackTime = comp2AttackTime.deserialize(payload),
                comp2ReleaseTime = comp2ReleaseTime.deserialize(payload),
                comp2Threshold = comp2Threshold.deserialize(payload),
                comp2Ratio = comp2Ratio.deserialize(payload),
                comp2OutputGain = comp2OutputGain.deserialize(payload),
                eq2Switch = eq2Switch.deserialize(payload),
                eq2LowFrequency = eq2LowFrequency.deserialize(payload),
                eq2LowGain = eq2LowGain.deserialize(payload) - 15,
                eq2MidFrequency = eq2MidFrequency.deserialize(payload),
                eq2MidGain = eq2MidGain.deserialize(payload) - 15,
                eq2MidQ = eq2MidQ.deserialize(payload),
                eq2HighFrequency = eq2HighFrequency.deserialize(payload),
                eq2HighGain = eq2HighGain.deserialize(payload) - 15,

                comp3Switch = comp3Switch.deserialize(payload),
                comp3AttackTime = comp3AttackTime.deserialize(payload),
                comp3ReleaseTime = comp3ReleaseTime.deserialize(payload),
                comp3Threshold = comp3Threshold.deserialize(payload),
                comp3Ratio = comp3Ratio.deserialize(payload),
                comp3OutputGain = comp3OutputGain.deserialize(payload),
                eq3Switch = eq3Switch.deserialize(payload),
                eq3LowFrequency = eq3LowFrequency.deserialize(payload),
                eq3LowGain = eq3LowGain.deserialize(payload) - 15,
                eq3MidFrequency = eq3MidFrequency.deserialize(payload),
                eq3MidGain = eq3MidGain.deserialize(payload) - 15,
                eq3MidQ = eq3MidQ.deserialize(payload),
                eq3HighFrequency = eq3HighFrequency.deserialize(payload),
                eq3HighGain = eq3HighGain.deserialize(payload) - 15,

                comp4Switch = comp4Switch.deserialize(payload),
                comp4AttackTime = comp4AttackTime.deserialize(payload),
                comp4ReleaseTime = comp4ReleaseTime.deserialize(payload),
                comp4Threshold = comp4Threshold.deserialize(payload),
                comp4Ratio = comp4Ratio.deserialize(payload),
                comp4OutputGain = comp4OutputGain.deserialize(payload),
                eq4Switch = eq4Switch.deserialize(payload),
                eq4LowFrequency = eq4LowFrequency.deserialize(payload),
                eq4LowGain = eq4LowGain.deserialize(payload) - 15,
                eq4MidFrequency = eq4MidFrequency.deserialize(payload),
                eq4MidGain = eq4MidGain.deserialize(payload) - 15,
                eq4MidQ = eq4MidQ.deserialize(payload),
                eq4HighFrequency = eq4HighFrequency.deserialize(payload),
                eq4HighGain = eq4HighGain.deserialize(payload) - 15,

                comp5Switch = comp5Switch.deserialize(payload),
                comp5AttackTime = comp5AttackTime.deserialize(payload),
                comp5ReleaseTime = comp5ReleaseTime.deserialize(payload),
                comp5Threshold = comp5Threshold.deserialize(payload),
                comp5Ratio = comp5Ratio.deserialize(payload),
                comp5OutputGain = comp5OutputGain.deserialize(payload),
                eq5Switch = eq5Switch.deserialize(payload),
                eq5LowFrequency = eq5LowFrequency.deserialize(payload),
                eq5LowGain = eq5LowGain.deserialize(payload) - 15,
                eq5MidFrequency = eq5MidFrequency.deserialize(payload),
                eq5MidGain = eq5MidGain.deserialize(payload) - 15,
                eq5MidQ = eq5MidQ.deserialize(payload),
                eq5HighFrequency = eq5HighFrequency.deserialize(payload),
                eq5HighGain = eq5HighGain.deserialize(payload) - 15,

                comp6Switch = comp6Switch.deserialize(payload),
                comp6AttackTime = comp6AttackTime.deserialize(payload),
                comp6ReleaseTime = comp6ReleaseTime.deserialize(payload),
                comp6Threshold = comp6Threshold.deserialize(payload),
                comp6Ratio = comp6Ratio.deserialize(payload),
                comp6OutputGain = comp6OutputGain.deserialize(payload),
                eq6Switch = eq6Switch.deserialize(payload),
                eq6LowFrequency = eq6LowFrequency.deserialize(payload),
                eq6LowGain = eq6LowGain.deserialize(payload) - 15,
                eq6MidFrequency = eq6MidFrequency.deserialize(payload),
                eq6MidGain = eq6MidGain.deserialize(payload) - 15,
                eq6MidQ = eq6MidQ.deserialize(payload),
                eq6HighFrequency = eq6HighFrequency.deserialize(payload),
                eq6HighGain = 0 // TODO eq6HighGain.interpret(startAddress.offsetBy(lsb = 0x53u), length, payload) - 15,
            )
        }
    }

    @ExperimentalUnsignedTypes
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
            SuperNaturalDrumToneVariation::values
        )
        val dynamicRange = UByteField(deviceId, address.offsetBy(lsb = 0x10u), 0..63)
        val stereoWidth = UByteField(deviceId, address.offsetBy(lsb = 0x11u))
        val outputAssign = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x12u),
            SuperNaturalDrumToneOutput::values
        )

        override fun deserialize(
            payload: SparseUByteArray
        ): SuperNaturalDrumKitNote {
            assert(this.isCovering(payload)) { "Not SN-D note ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return SuperNaturalDrumKitNote(
                instrumentNumber = instrumentNumber.deserialize(payload),
                level = level.deserialize(payload),
                pan = pan.deserialize(payload),
                chorusSendLevel = chorusSendLevel.deserialize(payload),
                reverbSendLevel = reverbSendLevel.deserialize(payload),
                tune = tune.deserialize(payload),
                attack = attack.deserialize(payload),
                decay = decay.deserialize(payload),
                brilliance = brilliance.deserialize(payload),
                variation = variation.deserialize(payload),
                dynamicRange = dynamicRange.deserialize(payload),
                stereoWidth = stereoWidth.deserialize(payload),
                outputAssign = SuperNaturalDrumToneOutput.PART // TODO outputAssign.interpret(startAddress.offsetBy(lsb = 0x12u), length, payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class PcmDrumKitBuilder(
        override val deviceId: DeviceId,
        override val address: Integra7Address,
        override val part: IntegraPart
    ) : Integra7PartSysEx<PcmDrumKit>() {
        override val size =
            Integra7Size(UInt7(mmsb = 0x02u.toUByte7(), mlsb = 0x07Fu.toUByte7(), lsb = 0x7Fu.toUByte7()))

        val common = PcmDrumKitCommonBuilder(deviceId, address, part)
        val mfx = MfxSysEx(deviceId, address.offsetBy(mlsb = 0x02u, lsb = 0x00u), part) // Same as PCM
        val commonCompEq = SuperNaturalDrumKitCommonCompEqBuilder(deviceId, address.offsetBy(mlsb = 0x08u, lsb = 0x00u), part) // Same as SN-D
        val keys = IntRange(0, 78) // key 21 .. 108
            .map { PcmDrumKitPartialBuilder(deviceId, address.offsetBy(mlsb = 0x10u, lsb = 0x00u).offsetBy(mlsb = 0x02u, lsb = 0x00u, factor = it), part)  }
        val common2 = PcmDrumKitCommon2Builder(deviceId, address.offsetBy(mmsb = 0x02u, lsb = 0x00u), part)

        override fun deserialize(payload: SparseUByteArray): PcmDrumKit {
            assert(this.isCovering(payload)) { "Not PCMD ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return PcmDrumKit(
                common = common.deserialize(payload),
                mfx = mfx.deserialize(payload),
                keys = keys
                    .mapIndexed { index, b ->
                        b.deserialize(
                            payload
                        )
                    },
                common2 = common2.deserialize(payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class PcmDrumKitCommonBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitCommon>() {
        override val size = Integra7Size(0x46u)

        val name = AsciiStringField(deviceId, address, length = 0x0C)
        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Cu))

        override fun deserialize(
            payload: SparseUByteArray
        ): PcmDrumKitCommon {
            assert(this.isCovering(payload)) { "Not PCMD Common ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return PcmDrumKitCommon(
                name = name.deserialize(payload),
                level = level.deserialize(payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class PcmDrumKitPartialBuilder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitPartial>() {
        override val size = Integra7Size(UInt7(mlsb = 0x01u.toUByte7(), lsb = 0x43u.toUByte7()))

        val name = AsciiStringField(deviceId, address, length = 0x0C)

        val assignType =
            EnumField(deviceId, address.offsetBy(lsb = 0x0Cu), PcmDrumKitAssignType::values)
        val muteGroup = UByteField(deviceId, address.offsetBy(lsb = 0x0Du), 0..31)

        val level = UByteField(deviceId, address.offsetBy(lsb = 0x0Eu))
        val coarseTune = UByteField(deviceId, address.offsetBy(lsb = 0x0Fu))
        val fineTune = ByteField(deviceId, address.offsetBy(lsb = 0x10u), -50..50)
        val randomPitchDepth =
            EnumField(deviceId, address.offsetBy(lsb = 0x11u), RandomPithDepth::values)
        val pan = ByteField(deviceId, address.offsetBy(lsb = 0x12u), -64..63)
        val randomPanDepth = UByteField(deviceId, address.offsetBy(lsb = 0x13u), 0..63)
        val alternatePanDepth = ByteField(deviceId, address.offsetBy(lsb = 0x14u))
        val envMode = EnumField(deviceId, address.offsetBy(lsb = 0x15u), EnvMode::values)

        val outputLevel = UByteField(deviceId, address.offsetBy(lsb = 0x16u))
        val chorusSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x19u))
        val reverbSendLevel = UByteField(deviceId, address.offsetBy(lsb = 0x1Au))
        val outputAssign = EnumField(
            deviceId,
            address.offsetBy(lsb = 0x1Bu),
            SuperNaturalDrumToneOutput::values
        )

        val pitchBendRange = UByteField(deviceId, address.offsetBy(lsb = 0x1Cu), 0..48)
        val receiveExpression = BooleanField(deviceId, address.offsetBy(lsb = 0x1Du))
        val receiveHold1 = BooleanField(deviceId, address.offsetBy(lsb = 0x1Eu))

        val wmtVelocityControl =
            EnumField(deviceId, address.offsetBy(lsb = 0x20u), WmtVelocityControl::values)

        val wmt1WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x21u))
        val wmt1WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x22u), WaveGroupType::values)
        val wmt1WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x23u), 0..16384)
        val wmt1WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x27u), 0..16384)
        val wmt1WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x2Bu), 0..16384)
        val wmt1WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x2Fu), WaveGain::values)
        val wmt1WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x30u))
        val wmt1WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x31u), 0..3)
        val wmt1WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x32u), 0..16)
        val wmt1WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x33u))
        val wmt1WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x34u), -48..48)
        val wmt1WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x35u), -50..50)
        val wmt1WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x36u), -64..63)
        val wmt1WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x37u))
        val wmt1WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x38u), OffOnReverse::values)
        val wmt1WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x39u))
        val wmt1VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Au))
        val wmt1VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x3Cu))

        val wmt2WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x3Eu))
        val wmt2WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x3Fu), WaveGroupType::values)
        val wmt2WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x40u), 0..16384)
        val wmt2WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x44u), 0..16384)
        val wmt2WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x48u), 0..16384)
        val wmt2WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x4Cu), WaveGain::values)
        val wmt2WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x4Du))
        val wmt2WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x4Eu), 0..3)
        val wmt2WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x4Fu), 0..16)
        val wmt2WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x50u))
        val wmt2WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x51u), -48..48)
        val wmt2WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x52u), -50..50)
        val wmt2WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x53u), -64..63)
        val wmt2WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x54u))
        val wmt2WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x55u), OffOnReverse::values)
        val wmt2WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x56u))
        val wmt2VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x57u))
        val wmt2VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x59u))

        val wmt3WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x5Bu))
        val wmt3WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x5Cu), WaveGroupType::values)
        val wmt3WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x5Du), 0..16384)
        val wmt3WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x61u), 0..16384)
        val wmt3WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x65u), 0..16384)
        val wmt3WaveGain = EnumField(deviceId, address.offsetBy(lsb = 0x69u), WaveGain::values)
        val wmt3WaveFxmSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x6Au))
        val wmt3WaveFxmColor = UByteField(deviceId, address.offsetBy(lsb = 0x6Bu), 0..3)
        val wmt3WaveFxmDepth = UByteField(deviceId, address.offsetBy(lsb = 0x6Cu), 0..16)
        val wmt3WaveTempoSync = BooleanField(deviceId, address.offsetBy(lsb = 0x6Du))
        val wmt3WaveCoarseTune = ByteField(deviceId, address.offsetBy(lsb = 0x6Eu), -48..48)
        val wmt3WaveFineTune = ByteField(deviceId, address.offsetBy(lsb = 0x6Fu), -50..50)
        val wmt3WavePan = ByteField(deviceId, address.offsetBy(lsb = 0x70u), -64..63)
        val wmt3WaveRandomPanSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x71u))
        val wmt3WaveAlternatePanSwitch =
            EnumField(deviceId, address.offsetBy(lsb = 0x72u), OffOnReverse::values)
        val wmt3WaveLevel = UByteField(deviceId, address.offsetBy(lsb = 0x73u))
        val wmt3VelocityRange = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x74u))
        val wmt3VelocityFadeWidth = UnsignedLsbMsbBytes(deviceId, address.offsetBy(lsb = 0x76u))

        val wmt4WaveSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x78u))
        val wmt4WaveGroupType =
            EnumField(deviceId, address.offsetBy(lsb = 0x79u), WaveGroupType::values)
        val wmt4WaveGroupId =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Au), 0..16384)
        val wmt4WaveNumberL =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(lsb = 0x7Eu), 0..16384)
        val wmt4WaveNumberR =
            UnsignedMsbLsbFourNibbles(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x02u), 0..16384)
        val wmt4WaveGain =
            EnumField(deviceId, address.offsetBy(mlsb = 0x01u, lsb = 0x06u), WaveGain::values)
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
            OffOnReverse::values
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
            TvfFilterType::values
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

        override fun deserialize(
            payload: SparseUByteArray
        ): PcmDrumKitPartial {
            assert(this.isCovering(payload)) { "Not PCMS partial ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return PcmDrumKitPartial(
                name = name.deserialize(payload),

                assignType = assignType.deserialize(payload),
                muteGroup = muteGroup.deserialize(payload),

                level = level.deserialize(payload),
                coarseTune = coarseTune.deserialize(payload),
                fineTune = fineTune.deserialize(payload),
                randomPitchDepth = randomPitchDepth.deserialize(payload),
                pan = pan.deserialize(payload),
                randomPanDepth = randomPanDepth.deserialize(payload),
                alternatePanDepth = alternatePanDepth.deserialize(payload),
                envMode = envMode.deserialize(payload),

                outputLevel = outputLevel.deserialize(payload),
                chorusSendLevel = chorusSendLevel.deserialize(payload),
                reverbSendLevel = reverbSendLevel.deserialize(payload),
                outputAssign = outputAssign.deserialize(payload),

                pitchBendRange = pitchBendRange.deserialize(payload),
                receiveExpression = receiveExpression.deserialize(payload),
                receiveHold1 = receiveHold1.deserialize(payload),

                wmtVelocityControl = wmtVelocityControl.deserialize(payload),

                wmt1WaveSwitch = wmt1WaveSwitch.deserialize(payload),
                wmt1WaveGroupType = wmt1WaveGroupType.deserialize(payload),
                wmt1WaveGroupId = wmt1WaveGroupId.deserialize(payload),
                wmt1WaveNumberL = wmt1WaveNumberL.deserialize(payload),
                wmt1WaveNumberR = wmt1WaveNumberR.deserialize(payload),
                wmt1WaveGain = wmt1WaveGain.deserialize(payload),
                wmt1WaveFxmSwitch = wmt1WaveFxmSwitch.deserialize(payload),
                wmt1WaveFxmColor = wmt1WaveFxmColor.deserialize(payload),
                wmt1WaveFxmDepth = wmt1WaveFxmDepth.deserialize(payload),
                wmt1WaveTempoSync = wmt1WaveTempoSync.deserialize(payload),
                wmt1WaveCoarseTune = wmt1WaveCoarseTune.deserialize(payload),
                wmt1WaveFineTune = wmt1WaveFineTune.deserialize(payload),
                wmt1WavePan = wmt1WavePan.deserialize(payload),
                wmt1WaveRandomPanSwitch = wmt1WaveRandomPanSwitch.deserialize(
                    payload
                ),
                wmt1WaveAlternatePanSwitch = wmt1WaveAlternatePanSwitch.deserialize(
                    payload
                ),
                wmt1WaveLevel = wmt1WaveLevel.deserialize(payload),
                wmt1VelocityRange = wmt1VelocityRange.deserialize(payload),
                wmt1VelocityFadeWidth = wmt1VelocityFadeWidth.deserialize(payload),

                wmt2WaveSwitch = wmt2WaveSwitch.deserialize(payload),
                wmt2WaveGroupType = wmt2WaveGroupType.deserialize(payload),
                wmt2WaveGroupId = wmt2WaveGroupId.deserialize(payload),
                wmt2WaveNumberL = wmt2WaveNumberL.deserialize(payload),
                wmt2WaveNumberR = wmt2WaveNumberR.deserialize(payload),
                wmt2WaveGain = wmt2WaveGain.deserialize(payload),
                wmt2WaveFxmSwitch = wmt2WaveFxmSwitch.deserialize(payload),
                wmt2WaveFxmColor = wmt2WaveFxmColor.deserialize(payload),
                wmt2WaveFxmDepth = wmt2WaveFxmDepth.deserialize(payload),
                wmt2WaveTempoSync = wmt2WaveTempoSync.deserialize(payload),
                wmt2WaveCoarseTune = wmt2WaveCoarseTune.deserialize(payload),
                wmt2WaveFineTune = wmt2WaveFineTune.deserialize(payload),
                wmt2WavePan = wmt2WavePan.deserialize(payload),
                wmt2WaveRandomPanSwitch = wmt2WaveRandomPanSwitch.deserialize(
                    payload
                ),
                wmt2WaveAlternatePanSwitch = wmt2WaveAlternatePanSwitch.deserialize(
                    payload
                ),
                wmt2WaveLevel = wmt2WaveLevel.deserialize(payload),
                wmt2VelocityRange = wmt2VelocityRange.deserialize(payload),
                wmt2VelocityFadeWidth = wmt2VelocityFadeWidth.deserialize(payload),

                wmt3WaveSwitch = wmt3WaveSwitch.deserialize(payload),
                wmt3WaveGroupType = wmt3WaveGroupType.deserialize(payload),
                wmt3WaveGroupId = wmt3WaveGroupId.deserialize(payload),
                wmt3WaveNumberL = wmt3WaveNumberL.deserialize(payload),
                wmt3WaveNumberR = wmt3WaveNumberR.deserialize(payload),
                wmt3WaveGain = wmt3WaveGain.deserialize(payload),
                wmt3WaveFxmSwitch = wmt3WaveFxmSwitch.deserialize(payload),
                wmt3WaveFxmColor = wmt3WaveFxmColor.deserialize(payload),
                wmt3WaveFxmDepth = wmt3WaveFxmDepth.deserialize(payload),
                wmt3WaveTempoSync = wmt3WaveTempoSync.deserialize(payload),
                wmt3WaveCoarseTune = wmt3WaveCoarseTune.deserialize(payload),
                wmt3WaveFineTune = wmt3WaveFineTune.deserialize(payload),
                wmt3WavePan = wmt3WavePan.deserialize(payload),
                wmt3WaveRandomPanSwitch = wmt3WaveRandomPanSwitch.deserialize(
                    payload
                ),
                wmt3WaveAlternatePanSwitch = wmt3WaveAlternatePanSwitch.deserialize(
                    payload
                ),
                wmt3WaveLevel = wmt3WaveLevel.deserialize(payload),
                wmt3VelocityRange = wmt3VelocityRange.deserialize(payload),
                wmt3VelocityFadeWidth = wmt3VelocityFadeWidth.deserialize(payload),

                wmt4WaveSwitch = wmt4WaveSwitch.deserialize(payload),
                wmt4WaveGroupType = wmt4WaveGroupType.deserialize(payload),
                wmt4WaveGroupId = wmt4WaveGroupId.deserialize(payload),
                wmt4WaveNumberL = wmt4WaveNumberL.deserialize(payload),
                wmt4WaveNumberR = wmt4WaveNumberR.deserialize(payload),
                wmt4WaveGain = wmt4WaveGain.deserialize(payload),
                wmt4WaveFxmSwitch = wmt4WaveFxmSwitch.deserialize(
                    payload
                ),
                wmt4WaveFxmColor = wmt4WaveFxmColor.deserialize(
                    payload
                ),
                wmt4WaveFxmDepth = wmt4WaveFxmDepth.deserialize(
                    payload
                ),
                wmt4WaveTempoSync = wmt4WaveTempoSync.deserialize(
                    payload
                ),
                wmt4WaveCoarseTune = wmt4WaveCoarseTune.deserialize(
                    payload
                ),
                wmt4WaveFineTune = wmt4WaveFineTune.deserialize(
                    payload
                ),
                wmt4WavePan = wmt4WavePan.deserialize(payload),
                wmt4WaveRandomPanSwitch = wmt4WaveRandomPanSwitch.deserialize(
                    payload
                ),
                wmt4WaveAlternatePanSwitch = wmt4WaveAlternatePanSwitch.deserialize(
                    payload
                ),
                wmt4WaveLevel = wmt4WaveLevel.deserialize(payload),
                wmt4VelocityRange = wmt4VelocityRange.deserialize(
                    payload
                ),
                wmt4VelocityFadeWidth = wmt4VelocityFadeWidth.deserialize(
                    payload
                ),

                pitchEnvDepth = pitchEnvDepth.deserialize(payload),
                pitchEnvVelocitySens = pitchEnvVelocitySens.deserialize(
                    payload
                ),
                pitchEnvTime1VelocitySens = pitchEnvTime1VelocitySens.deserialize(
                    payload
                ),
                pitchEnvTime4VelocitySens = pitchEnvTime4VelocitySens.deserialize(
                    payload
                ),

                pitchEnvTime1 = pitchEnvTime1.deserialize(payload),
                pitchEnvTime2 = pitchEnvTime2.deserialize(payload),
                pitchEnvTime3 = pitchEnvTime3.deserialize(payload),
                pitchEnvTime4 = pitchEnvTime4.deserialize(payload),

                pitchEnvLevel0 = pitchEnvLevel0.deserialize(payload),
                pitchEnvLevel1 = pitchEnvLevel1.deserialize(payload),
                pitchEnvLevel2 = pitchEnvLevel2.deserialize(payload),
                pitchEnvLevel3 = pitchEnvLevel3.deserialize(payload),
                pitchEnvLevel4 = pitchEnvLevel4.deserialize(payload),

                tvfFilterType = tvfFilterType.deserialize(payload),
                tvfCutoffFrequency = tvfCutoffFrequency.deserialize(
                    payload
                ),
                tvfCutoffVelocityCurve = tvfCutoffVelocityCurve.deserialize(
                    payload
                ),
                tvfCutoffVelocitySens = tvfCutoffVelocitySens.deserialize(
                    payload
                ),
                tvfResonance = tvfResonance.deserialize(payload),
                tvfResonanceVelocitySens = tvfResonanceVelocitySens.deserialize(
                    payload
                ),
                tvfEnvDepth = tvfEnvDepth.deserialize(payload),
                tvfEnvVelocityCurveType = tvfEnvVelocityCurveType.deserialize(
                    payload
                ),
                tvfEnvVelocitySens = tvfEnvVelocitySens.deserialize(
                    payload
                ),
                tvfEnvTime1VelocitySens = tvfEnvTime1VelocitySens.deserialize(
                    payload
                ),
                tvfEnvTime4VelocitySens = tvfEnvTime4VelocitySens.deserialize(
                    payload
                ),
                tvfEnvTime1 = tvfEnvTime1.deserialize(payload),
                tvfEnvTime2 = tvfEnvTime2.deserialize(payload),
                tvfEnvTime3 = tvfEnvTime3.deserialize(payload),
                tvfEnvTime4 = tvfEnvTime4.deserialize(payload),
                tvfEnvLevel0 = tvfEnvLevel0.deserialize(payload),
                tvfEnvLevel1 = tvfEnvLevel1.deserialize(payload),
                tvfEnvLevel2 = tvfEnvLevel2.deserialize(payload),
                tvfEnvLevel3 = tvfEnvLevel3.deserialize(payload),
                tvfEnvLevel4 = tvfEnvLevel4.deserialize(payload),

                tvaLevelVelocityCurve = tvaLevelVelocityCurve.deserialize(
                    payload
                ),
                tvaLevelVelocitySens = tvaLevelVelocitySens.deserialize(
                    payload
                ),
                tvaEnvTime1VelocitySens = tvaEnvTime1VelocitySens.deserialize(
                    payload
                ),
                tvaEnvTime4VelocitySens = tvaEnvTime4VelocitySens.deserialize(
                    payload
                ),
                tvaEnvTime1 = tvaEnvTime1.deserialize(payload),
                tvaEnvTime2 = tvaEnvTime2.deserialize(payload),
                tvaEnvTime3 = tvaEnvTime3.deserialize(payload),
                tvaEnvTime4 = tvaEnvTime4.deserialize(payload),
                tvaEnvLevel1 = tvaEnvLevel1.deserialize(payload),
                tvaEnvLevel2 = tvaEnvLevel2.deserialize(payload),
                tvaEnvLevel3 = tvaEnvLevel3.deserialize(payload),

                oneShotMode = oneShotMode.deserialize(payload),
            )
        }
    }

    @ExperimentalUnsignedTypes
    class PcmDrumKitCommon2Builder(override val deviceId: DeviceId, override val address: Integra7Address, override val part: IntegraPart) :
        Integra7PartSysEx<PcmDrumKitCommon2>() {
        override val size = Integra7Size(0x32u)

        val phraseNumber = UnsignedMsbLsbNibbles(deviceId, address.offsetBy(lsb = 0x10u))
        val tfxSwitch = BooleanField(deviceId, address.offsetBy(lsb = 0x31u))

        override fun deserialize(
            payload: SparseUByteArray
        ): PcmDrumKitCommon2 {
            assert(this.isCovering(payload)) { "Not PCMD Common2 ($address..${address.offsetBy(size)}) for part $part but ${address.toStringDetailed()}" }

            return PcmDrumKitCommon2(
                phraseNumber = phraseNumber.deserialize(payload),
                tfxSwitch = tfxSwitch.deserialize(payload),
            )
        }
    }
}