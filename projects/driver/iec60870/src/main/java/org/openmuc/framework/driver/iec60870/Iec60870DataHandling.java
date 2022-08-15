/*
 * Copyright 2011-2022 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.iec60870;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.TypeConversionException;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.iec60870.settings.ChannelAddress;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ie.IeBinaryCounterReading;
import org.openmuc.j60870.ie.IeBinaryStateInformation;
import org.openmuc.j60870.ie.IeDoubleCommand;
import org.openmuc.j60870.ie.IeDoubleCommand.DoubleCommandState;
import org.openmuc.j60870.ie.IeDoublePointWithQuality;
import org.openmuc.j60870.ie.IeNormalizedValue;
import org.openmuc.j60870.ie.IeProtectionQuality;
import org.openmuc.j60870.ie.IeQualifierOfCounterInterrogation;
import org.openmuc.j60870.ie.IeQualifierOfInterrogation;
import org.openmuc.j60870.ie.IeQualifierOfResetProcessCommand;
import org.openmuc.j60870.ie.IeQualifierOfSetPointCommand;
import org.openmuc.j60870.ie.IeQuality;
import org.openmuc.j60870.ie.IeRegulatingStepCommand;
import org.openmuc.j60870.ie.IeRegulatingStepCommand.StepCommandState;
import org.openmuc.j60870.ie.IeScaledValue;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.IeSingleCommand;
import org.openmuc.j60870.ie.IeSinglePointWithQuality;
import org.openmuc.j60870.ie.IeTestSequenceCounter;
import org.openmuc.j60870.ie.IeTime16;
import org.openmuc.j60870.ie.IeTime56;
import org.openmuc.j60870.ie.InformationElement;
import org.openmuc.j60870.ie.InformationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Iec60870DataHandling {

    private static final String ONLY_BYTE_ARRAY_WITH_LENGTH = "): Only byte array with length ";

    private static final Logger logger = LoggerFactory.getLogger(Iec60870DataHandling.class);

    private static final int INT32_BYTE_LENGTH = 4;

    static void writeSingleCommand(Record record, ChannelAddress channelAddress, Connection clientConnection)
            throws IOException, UnsupportedOperationException, TypeConversionException {

        int commonAddress = channelAddress.commonAddress();
        boolean qualifierSelect = channelAddress.select();
        int informationObjectAddress = channelAddress.ioa();
        ASduType typeId = ASduType.typeFor(channelAddress.typeId());

        Flag flag = record.getFlag();
        Value value = record.getValue();
        IeTime56 timestamp = new IeTime56(record.getTimestamp());

        CauseOfTransmission cot = CauseOfTransmission.ACTIVATION;

        if (flag == Flag.VALID && value != null) {
            switch (typeId) {
            case C_DC_NA_1:
                DoubleCommandState doubleCommandState = value.asBoolean() ? DoubleCommandState.ON
                        : DoubleCommandState.OFF;
                clientConnection.doubleCommand(commonAddress, cot, informationObjectAddress,
                        new IeDoubleCommand(doubleCommandState, 0, false));
                break;
            case C_DC_TA_1:
                doubleCommandState = value.asBoolean() ? DoubleCommandState.ON : DoubleCommandState.OFF;
                clientConnection.doubleCommandWithTimeTag(commonAddress, cot, informationObjectAddress,
                        new IeDoubleCommand(doubleCommandState, 0, false), timestamp);
                break;
            case C_BO_NA_1:
                IeBinaryStateInformation binaryStateInformation = new IeBinaryStateInformation(value.asInt());
                clientConnection.bitStringCommand(commonAddress, cot, informationObjectAddress, binaryStateInformation);
                break;
            case C_BO_TA_1:
                binaryStateInformation = new IeBinaryStateInformation(value.asInt());
                clientConnection.bitStringCommandWithTimeTag(commonAddress, cot, informationObjectAddress,
                        binaryStateInformation, timestamp);
                break;
            case C_CD_NA_1: // Writes only the current time, no values
                IeTime16 time16 = new IeTime16(record.getTimestamp());
                clientConnection.delayAcquisitionCommand(commonAddress, cot, time16);
                break;
            case C_CI_NA_1: // Uses ByteArray Value [request, freeze]
                byte[] baQualifier = value.asByteArray();
                if (baQualifier.length == 2) {
                    IeQualifierOfCounterInterrogation qualifier = new IeQualifierOfCounterInterrogation(baQualifier[0],
                            baQualifier[1]);
                    clientConnection.counterInterrogation(commonAddress, cot, qualifier);
                }
                else {
                    throw new TypeConversionException(typeId + "(" + typeId.getId()
                            + "): Only byte array with length 2 allowed. byte[0]=request, byte[1]=freeze]");
                }
                break;
            case C_CS_NA_1: // Writes only the current time, no values
                clientConnection.synchronizeClocks(commonAddress, new IeTime56(System.currentTimeMillis()));
                break;
            case C_IC_NA_1:
                IeQualifierOfInterrogation ieQualifierOfInterrogation = new IeQualifierOfInterrogation(value.asInt());
                clientConnection.interrogation(commonAddress, cot, ieQualifierOfInterrogation);
                break;
            case C_RC_NA_1:
                IeRegulatingStepCommand regulatingStepCommand = getIeRegulatingStepCommand(typeId, value);
                clientConnection.regulatingStepCommand(commonAddress, cot, informationObjectAddress,
                        regulatingStepCommand);
                break;
            case C_RC_TA_1:
                try {
                    regulatingStepCommand = getIeRegulatingStepCommand(typeId, value);
                    clientConnection.regulatingStepCommandWithTimeTag(commonAddress, cot, informationObjectAddress,
                            regulatingStepCommand, timestamp);
                } catch (Exception e) {
                    logger.error("", e);
                }
                break;
            case C_RD_NA_1:
                clientConnection.readCommand(commonAddress, informationObjectAddress);
                break;
            case C_RP_NA_1:
                clientConnection.resetProcessCommand(commonAddress,
                        new IeQualifierOfResetProcessCommand(value.asInt()));
                break;
            case C_SC_NA_1:
                IeSingleCommand singleCommand = getIeSingeleCommand(typeId, value);
                clientConnection.singleCommand(commonAddress, cot, informationObjectAddress, singleCommand);
                break;
            case C_SC_TA_1:
                singleCommand = getIeSingeleCommand(typeId, value);
                clientConnection.singleCommandWithTimeTag(commonAddress, cot, informationObjectAddress, singleCommand,
                        timestamp);
                break;
            case C_SE_NA_1:
                byte[] values = value.asByteArray();
                int arrayLength = 6;
                int valueLength = 4;
                checkLength(typeId, values, arrayLength,
                        "byte[0-3]=command state, byte[4]=qualifier of command, byte[5]=execute/select");
                IeQualifierOfSetPointCommand ieQualifierOfSetPointCommand = getIeQualifierSetPointCommand(values,
                        arrayLength);
                IeNormalizedValue ieNormalizedValue = new IeNormalizedValue(
                        bytesToSignedInt32(values, valueLength, false));

                clientConnection.setNormalizedValueCommand(commonAddress, cot, informationObjectAddress,
                        ieNormalizedValue, ieQualifierOfSetPointCommand);
                break;
            case C_SE_NB_1:
                values = value.asByteArray();
                arrayLength = 4;
                checkLength(typeId, values, arrayLength,
                        "byte[0-1]=command state, byte[2]=qualifier of command, byte[3]=execute/select");
                ieQualifierOfSetPointCommand = getIeQualifierSetPointCommand(values, arrayLength);
                IeScaledValue scaledValue = new IeScaledValue(bytesToSignedInt32(values, 2, false));
                clientConnection.setScaledValueCommand(commonAddress, cot, informationObjectAddress, scaledValue,
                        ieQualifierOfSetPointCommand);
                break;
            case C_SE_NC_1:
                IeShortFloat shortFloat = new IeShortFloat(value.asFloat());
                IeQualifierOfSetPointCommand qualifier = new IeQualifierOfSetPointCommand(0, qualifierSelect);
                clientConnection.setShortFloatCommand(commonAddress, cot, informationObjectAddress, shortFloat,
                        qualifier);
                break;
            case C_SE_TA_1:
                values = value.asByteArray();
                arrayLength = 6;
                valueLength = 4;
                checkLength(typeId, values, arrayLength,
                        "byte[0-3]=command state, byte[4]=qualifier of command, byte[5]=execute/select");
                ieQualifierOfSetPointCommand = getIeQualifierSetPointCommand(values, arrayLength);
                ieNormalizedValue = new IeNormalizedValue(bytesToSignedInt32(values, valueLength, false));

                clientConnection.setNormalizedValueCommandWithTimeTag(commonAddress, cot, informationObjectAddress,
                        ieNormalizedValue, ieQualifierOfSetPointCommand, timestamp);
                break;
            case C_SE_TB_1:
                values = value.asByteArray();
                arrayLength = 4;
                checkLength(typeId, values, arrayLength,
                        "byte[0-1]=command state, byte[2]=qualifier of command, byte[3]=execute/select");
                ieQualifierOfSetPointCommand = getIeQualifierSetPointCommand(values, arrayLength);
                scaledValue = new IeScaledValue(bytesToSignedInt32(values, 2, false));
                clientConnection.setScaledValueCommandWithTimeTag(commonAddress, cot, informationObjectAddress,
                        scaledValue, ieQualifierOfSetPointCommand, timestamp);
                break;
            case C_SE_TC_1:
                // TODO:
                throw new UnsupportedOperationException(
                        "TypeID " + typeId + "(" + typeId.getId() + ") is not supported, yet.");
            case C_TS_NA_1:
                clientConnection.testCommand(commonAddress);
                break;
            case C_TS_TA_1:
                clientConnection.testCommandWithTimeTag(commonAddress, new IeTestSequenceCounter(value.asInt()),
                        timestamp);
                break;
            case F_AF_NA_1:
            case F_DR_TA_1:
            case F_FR_NA_1:
            case F_LS_NA_1:
            case F_SC_NA_1:
            case F_SC_NB_1:
            case F_SG_NA_1:
            case F_SR_NA_1:
            case M_BO_NA_1:
            case M_BO_TA_1:
            case M_BO_TB_1:
            case M_DP_NA_1:
            case M_DP_TA_1:
            case M_DP_TB_1:
            case M_EI_NA_1:
            case M_EP_TA_1:
            case M_EP_TB_1:
            case M_EP_TC_1:
            case M_EP_TD_1:
            case M_EP_TE_1:
            case M_EP_TF_1:
            case M_IT_NA_1:
            case M_IT_TA_1:
            case M_IT_TB_1:
            case M_ME_NA_1:
            case M_ME_NB_1:
            case M_ME_NC_1:
            case M_ME_ND_1:
            case M_ME_TA_1:
            case M_ME_TB_1:
            case M_ME_TC_1:
            case M_ME_TD_1:
            case M_ME_TE_1:
            case M_ME_TF_1:
            case M_PS_NA_1:
            case M_SP_NA_1:
            case M_SP_TA_1:
            case M_SP_TB_1:
            case M_ST_NA_1:
            case M_ST_TA_1:
            case M_ST_TB_1:
            case P_AC_NA_1:
            case P_ME_NA_1:
            case P_ME_NB_1:
            case P_ME_NC_1:
            case PRIVATE_128:
            case PRIVATE_129:
            case PRIVATE_130:
            case PRIVATE_131:
            case PRIVATE_132:
            case PRIVATE_133:
            case PRIVATE_134:
            case PRIVATE_135:
            case PRIVATE_136:
            case PRIVATE_137:
            case PRIVATE_138:
            case PRIVATE_139:
            case PRIVATE_140:
            case PRIVATE_141:
            case PRIVATE_142:
            case PRIVATE_143:
            case PRIVATE_144:
            case PRIVATE_145:
            case PRIVATE_146:
            case PRIVATE_147:
            case PRIVATE_148:
            case PRIVATE_149:
            case PRIVATE_150:
            case PRIVATE_151:
            case PRIVATE_152:
            case PRIVATE_153:
            case PRIVATE_154:
            case PRIVATE_155:
            case PRIVATE_156:
            case PRIVATE_157:
            case PRIVATE_158:
            case PRIVATE_159:
            case PRIVATE_160:
            case PRIVATE_161:
            case PRIVATE_162:
            case PRIVATE_163:
            case PRIVATE_164:
            case PRIVATE_165:
            case PRIVATE_166:
            case PRIVATE_167:
            case PRIVATE_168:
            case PRIVATE_169:
            case PRIVATE_170:
            case PRIVATE_171:
            case PRIVATE_172:
            case PRIVATE_173:
            case PRIVATE_174:
            case PRIVATE_175:
            case PRIVATE_176:
            case PRIVATE_177:
            case PRIVATE_178:
            case PRIVATE_179:
            case PRIVATE_180:
            case PRIVATE_181:
            case PRIVATE_182:
            case PRIVATE_183:
            case PRIVATE_184:
            case PRIVATE_185:
            case PRIVATE_186:
            case PRIVATE_187:
            case PRIVATE_188:
            case PRIVATE_189:
            case PRIVATE_190:
            case PRIVATE_191:
            case PRIVATE_192:
            case PRIVATE_193:
            case PRIVATE_194:
            case PRIVATE_195:
            case PRIVATE_196:
            case PRIVATE_197:
            case PRIVATE_198:
            case PRIVATE_199:
            case PRIVATE_200:
            case PRIVATE_201:
            case PRIVATE_202:
            case PRIVATE_203:
            case PRIVATE_204:
            case PRIVATE_205:
            case PRIVATE_206:
            case PRIVATE_207:
            case PRIVATE_208:
            case PRIVATE_209:
            case PRIVATE_210:
            case PRIVATE_211:
            case PRIVATE_212:
            case PRIVATE_213:
            case PRIVATE_214:
            case PRIVATE_215:
            case PRIVATE_216:
            case PRIVATE_217:
            case PRIVATE_218:
            case PRIVATE_219:
            case PRIVATE_220:
            case PRIVATE_221:
            case PRIVATE_222:
            case PRIVATE_223:
            case PRIVATE_224:
            case PRIVATE_225:
            case PRIVATE_226:
            case PRIVATE_227:
            case PRIVATE_228:
            case PRIVATE_229:
            case PRIVATE_230:
            case PRIVATE_231:
            case PRIVATE_232:
            case PRIVATE_233:
            case PRIVATE_234:
            case PRIVATE_235:
            case PRIVATE_236:
            case PRIVATE_237:
            case PRIVATE_238:
            case PRIVATE_239:
            case PRIVATE_240:
            case PRIVATE_241:
            case PRIVATE_242:
            case PRIVATE_243:
            case PRIVATE_244:
            case PRIVATE_245:
            case PRIVATE_246:
            case PRIVATE_247:
            case PRIVATE_248:
            case PRIVATE_249:
            case PRIVATE_250:
            case PRIVATE_251:
            case PRIVATE_252:
            case PRIVATE_253:
            case PRIVATE_254:
            case PRIVATE_255:
            default:
                throw new UnsupportedOperationException(
                        "TypeID " + typeId + "(" + typeId.getId() + ") is not supported, yet.");
            }
        }
    }

    private static void checkLength(ASduType typeId, byte[] values, int maxLength, String commands)
            throws TypeConversionException {
        int length = values.length;
        if (length != maxLength) {
            throw new UnsupportedOperationException(
                    typeId + "(" + typeId.getId() + ONLY_BYTE_ARRAY_WITH_LENGTH + maxLength + " allowed. " + commands);
        }
    }

    private static IeQualifierOfSetPointCommand getIeQualifierSetPointCommand(byte[] values, int maxLength) {
        int qualifier = values[maxLength - 2];
        boolean select = values[maxLength - 1] >= 0;
        return new IeQualifierOfSetPointCommand(qualifier, select);
    }

    private static IeSingleCommand getIeSingeleCommand(ASduType typeId, Value value) throws TypeConversionException {
        byte[] values = value.asByteArray();
        boolean commandStateOn;
        boolean select;
        int length = 3;

        if (values.length == length) {
            commandStateOn = values[0] >= 0;
            select = values[1] >= 0;
        }
        else {
            throw new TypeConversionException(typeId + "(" + typeId.getId() + ONLY_BYTE_ARRAY_WITH_LENGTH + length
                    + " allowed. byte[0]=command state on, byte[1]=execute/select, byte[2]=qualifier of command");
        }
        return new IeSingleCommand(commandStateOn, values[2], select);
    }

    private static IeRegulatingStepCommand getIeRegulatingStepCommand(ASduType typeId, Value value)
            throws TypeConversionException {
        byte[] values = value.asByteArray();
        StepCommandState commandState;
        boolean select;
        int length = 3;

        if (values.length == length) {
            commandState = StepCommandState.getInstance(values[0]);
            select = values[1] >= 0;
        }
        else {
            throw new TypeConversionException(typeId + "(" + typeId.getId() + ONLY_BYTE_ARRAY_WITH_LENGTH + length
                    + " allowed. byte[0]=command state, byte[1]=execute/select, byte[2]=qualifier of command ");
        }
        return new IeRegulatingStepCommand(commandState, values[2], select);
    }

    static Record handleInformationObject(ASdu aSdu, long timestamp, ChannelAddress channelAddress,
            InformationObject informationObject) {
        Record record;

        if (channelAddress.multiple() > 1) {
            record = handleMultipleElementObjects(aSdu, timestamp, channelAddress, informationObject);
        }
        else {
            InformationElement[] informationElements;
            try {
                informationElements = handleSingleElementObject(aSdu, timestamp, channelAddress, informationObject);
            } catch (ConfigurationException e) {
                logger.warn(e.getMessage());
                return new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
            }
            if (informationElements != null) {
                record = Iec60870DataHandling.creatNewRecord(informationElements, aSdu.getTypeIdentification(),
                        channelAddress, timestamp);
            }
            else {
                record = new Record(Flag.UNKNOWN_ERROR);
            }
        }
        return record;
    }

    private static Record creatNewRecord(InformationElement[] informationElements, ASduType typeId,
            ChannelAddress channelAddress, long timestamp) {
        if (!channelAddress.dataType().equals("v")) {
            return getQualityDescriptorAsRecord(channelAddress.dataType(), informationElements, typeId, timestamp);
        }
        else {
            switch (typeId) {
            case M_ME_NA_1:
            case M_ME_TA_1:
            case M_ME_ND_1:
            case M_ME_TD_1:
            case C_SE_NA_1:
            case P_ME_NA_1:
                IeNormalizedValue normalizedValue = (IeNormalizedValue) informationElements[0]; // TODO: is 0 correct?
                return new Record(new DoubleValue(normalizedValue.getNormalizedValue()), timestamp);
            case M_ME_NB_1:
            case M_ME_TB_1:
            case M_ME_TE_1:
            case C_SE_NB_1:
            case P_ME_NB_1:
                IeScaledValue scaledValue = (IeScaledValue) informationElements[0];// TODO: is 0 correct?
                return new Record(new IntValue(scaledValue.getUnnormalizedValue()), timestamp); // test this
            case M_ME_NC_1:
            case M_ME_TC_1:
            case M_ME_TF_1:
            case C_SE_NC_1:
            case P_ME_NC_1:
                IeShortFloat shortFloat = (IeShortFloat) informationElements[0];
                return new Record(new DoubleValue(shortFloat.getValue()), timestamp);
            case M_BO_NA_1:
            case M_BO_TA_1:
            case M_BO_TB_1:
                IeBinaryStateInformation binaryStateInformation = (IeBinaryStateInformation) informationElements[0];
                return new Record(
                        new ByteArrayValue(ByteBuffer.allocate(4).putInt(binaryStateInformation.getValue()).array()),
                        timestamp);
            case M_SP_NA_1:
            case M_SP_TA_1:
            case M_PS_NA_1:
            case M_SP_TB_1:
            case M_ST_NA_1:
                // TODO: test this!!! It's not really a SinglePointInformation
            case M_ST_TA_1:
                // TODO: test this!!! It's not really a SinglePointInformation
            case M_ST_TB_1:
                // TODO: test this!!! It's not really a SinglePointInformation
                IeSinglePointWithQuality singlePointWithQuality = (IeSinglePointWithQuality) informationElements[0];
                return new Record(new BooleanValue(singlePointWithQuality.isOn()), timestamp);
            case M_DP_NA_1:
            case M_DP_TA_1:
            case M_DP_TB_1:
                IeDoublePointWithQuality doublePointWithQuality = (IeDoublePointWithQuality) informationElements[0];
                return new Record(new IntValue(doublePointWithQuality.getDoublePointInformation().ordinal()),
                        timestamp); // TODO: check this solution. Is Enum to int correct?
            case M_IT_NA_1:
            case M_IT_TA_1:
            case M_IT_TB_1:
                IeBinaryCounterReading binaryCounterReading = (IeBinaryCounterReading) informationElements[0];
                // TODO: change to String because of more values e.g. getSequenceNumber, isCarry, ... ?
                return new Record(new IntValue(binaryCounterReading.getCounterReading()), timestamp);
            default:
                logger.debug("Not supported Type Identification.");
                return new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION);
            }
        }
    }

    private static Record getQualityDescriptorAsRecord(String dataType, InformationElement[] informationElements,
            ASduType typeIdentification, long timestamp) {
        Record record = null;
        InformationElement informationElement = informationElements[informationElements.length - 1];

        if (typeIdentification.getId() <= 14 || typeIdentification.getId() == 20
                || typeIdentification.getId() >= 30 && typeIdentification.getId() <= 36) {
            record = quality(dataType, timestamp, informationElement);
        }
        else if (typeIdentification.getId() >= 15 && typeIdentification.getId() <= 16
                || typeIdentification.getId() == 37) {
            record = binaryCounterReading(dataType, timestamp, informationElement);
        }
        else if (typeIdentification.getId() >= 17 && typeIdentification.getId() <= 19
                || typeIdentification.getId() >= 38 && typeIdentification.getId() <= 40) {

            record = protectionQuality(dataType, timestamp, informationElement);
        }

        if (record == null) {
            logger.debug("Not supported Quality Descriptor.");
            record = new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION);
        }
        return record;
    }

    private static Record quality(String dataType, long timestamp, InformationElement informationElement) {
        IeQuality quality = (IeQuality) informationElement;
        Record record = null;

        switch (dataType) { // iv nt sb bl
        case "iv":
            record = new Record(new BooleanValue(quality.isInvalid()), timestamp);
            break;
        case "sb":
            record = new Record(new BooleanValue(quality.isSubstituted()), timestamp);
            break;
        case "nt":
            record = new Record(new BooleanValue(quality.isNotTopical()), timestamp);
            break;
        case "bl":
            record = new Record(new BooleanValue(quality.isBlocked()), timestamp);
            break;
        default:
        }
        return record;
    }

    private static Record protectionQuality(String dataType, long timestamp, InformationElement informationElement) {
        IeProtectionQuality quality = (IeProtectionQuality) informationElement;
        Record record = null;

        switch (dataType) { // iv nt sb bl ei
        case "iv":
            record = new Record(new BooleanValue(quality.isInvalid()), timestamp);
            break;
        case "sb":
            record = new Record(new BooleanValue(quality.isSubstituted()), timestamp);
            break;
        case "nt":
            record = new Record(new BooleanValue(quality.isNotTopical()), timestamp);
            break;
        case "bl":
            record = new Record(new BooleanValue(quality.isBlocked()), timestamp);
            break;
        case "ei":
            record = new Record(new BooleanValue(quality.isElapsedTimeInvalid()), timestamp);
            break;
        default:
        }
        return record;
    }

    private static Record binaryCounterReading(String dataType, long timestamp, InformationElement informationElement) {
        IeBinaryCounterReading quality = (IeBinaryCounterReading) informationElement;
        Record record = null;
        Set<org.openmuc.j60870.ie.IeBinaryCounterReading.Flag> flags = quality.getFlags();

        switch (dataType) {// iv ca cy
        case "iv":
            record = new Record(new BooleanValue(flags.contains(IeBinaryCounterReading.Flag.INVALID)), timestamp);
            break;
        case "ca":
            record = new Record(new BooleanValue(flags.contains(IeBinaryCounterReading.Flag.COUNTER_ADJUSTED)),
                    timestamp);
            break;
        case "cy":
            record = new Record(new BooleanValue(flags.contains(IeBinaryCounterReading.Flag.CARRY)), timestamp);
            break;
        default:
        }
        return record;
    }

    private static Record handleMultipleElementObjects(ASdu aSdu, long timestamp, ChannelAddress channelAddress,
            InformationObject informationObject) {
        int singleSize = sizeOfType(aSdu.getTypeIdentification());
        int arrayLength = singleSize * channelAddress.multiple();
        ByteBuffer byteBuffer = ByteBuffer.allocate(arrayLength);

        for (int i = 0; i < channelAddress.multiple(); ++i) {
            InformationElement[] informationElements;
            try {
                informationElements = handleSingleElementObject(aSdu, timestamp, channelAddress, informationObject);
                if (informationElements != null && informationElements.length > 0) {
                    IeBinaryStateInformation binaryStateInformation = (IeBinaryStateInformation) informationElements[0];
                    byteBuffer.putInt(binaryStateInformation.getValue());
                }
                else {
                    logger.warn("Information element of IAO {} {}", channelAddress.ioa(), "is null or empty.");
                    return new Record(Flag.UNKNOWN_ERROR);
                }
            } catch (ConfigurationException e) {
                logger.warn(e.getMessage());
                return new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
            }
        }

        byte[] value = byteBuffer.array();

        return new Record(new ByteArrayValue(value), timestamp);
    }

    private static InformationElement[] handleSingleElementObject(ASdu aSdu, long timestamp,
            ChannelAddress channelAddress, InformationObject informationObject) throws ConfigurationException {
        InformationElement[] informationElements = null;
        if (channelAddress.ioa() == informationObject.getInformationObjectAddress()) {
            if (aSdu.isSequenceOfElements()) {
                informationElements = sequenceOfElements(aSdu, timestamp, channelAddress, informationObject);
            }
            else {
                informationElements = informationObject.getInformationElements()[0];
            }
        }
        return informationElements;
    }

    private static InformationElement[] sequenceOfElements(ASdu aSdu, long timestamp, ChannelAddress channelAddress,
            InformationObject informationObject) throws ConfigurationException {
        InformationElement[] informationElements = null;

        if (channelAddress.index() >= -1) {
            informationElements = informationObject.getInformationElements()[channelAddress.index()];
        }
        else {
            throw new ConfigurationException(
                    "Got ASdu with same TypeId, Common Address and IOA, but it is a Sequence Of Elements. For this index in ChannelAddress is needed.");
        }
        return informationElements;
    }

    private static int sizeOfType(ASduType typeIdentification) {
        int size = -1; // size in byte
        switch (typeIdentification) {
        case M_BO_NA_1:
        case M_BO_TA_1:
        case M_BO_TB_1:
            size = 4;
            break;
        default:
            logger.debug("Not able to set Data Type {}  as multiple IOAs or Indices.", typeIdentification);
            break;
        }
        return size;
    }

    private static int bytesToSignedInt32(byte[] bytes, int length, boolean isLitteleEndian) {
        if (length <= INT32_BYTE_LENGTH) {
            int returnValue = 0;
            int lengthLoop = bytes.length - 1;

            if (isLitteleEndian) {
                reverseByteOrder(bytes);
            }

            for (int i = 0; i <= lengthLoop; ++i) {
                int shift = length - i << 3;
                returnValue |= (long) (bytes[i] & 0xff) << shift;
            }
            return returnValue;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to convert bytes due to wrong number of bytes. Minimum 1 byte, maximum " + INT32_BYTE_LENGTH
                            + " bytes needed for conversion.");
        }
    }

    private static void reverseByteOrder(byte[] bytes) {
        int indexLength = bytes.length - 1;
        int halfLength = bytes.length / 2;
        for (int i = 0; i < halfLength; i++) {
            int index = indexLength - i;
            byte temp = bytes[i];
            bytes[i] = bytes[index];
            bytes[index] = temp;
        }
    }

    private Iec60870DataHandling() {
        // Hide this constructor.
    }

}
