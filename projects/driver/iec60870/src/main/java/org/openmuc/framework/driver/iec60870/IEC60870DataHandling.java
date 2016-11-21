package org.openmuc.framework.driver.iec60870;

import java.nio.ByteBuffer;

import javax.naming.ConfigurationException;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.iec60870.settings.ChannelAddress;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.IeBinaryCounterReading;
import org.openmuc.j60870.IeBinaryStateInformation;
import org.openmuc.j60870.IeDoublePointWithQuality;
import org.openmuc.j60870.IeNormalizedValue;
import org.openmuc.j60870.IeProtectionQuality;
import org.openmuc.j60870.IeQuality;
import org.openmuc.j60870.IeScaledValue;
import org.openmuc.j60870.IeShortFloat;
import org.openmuc.j60870.IeSinglePointWithQuality;
import org.openmuc.j60870.InformationElement;
import org.openmuc.j60870.InformationObject;
import org.openmuc.j60870.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IEC60870DataHandling {

    private final static Logger logger = LoggerFactory.getLogger(IEC60870DataHandling.class);

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
                record = IEC60870DataHandling.creatNewRecord(informationElements, aSdu.getTypeIdentification(),
                        channelAddress, timestamp);
            }
            else {
                record = new Record(Flag.UNKNOWN_ERROR);
            }
        }
        return record;
    }

    private static Record creatNewRecord(InformationElement[] informationElements, TypeId typeIdentification,
            ChannelAddress channelAddress, long timestamp) {
        if (!channelAddress.dataType().equals("v")) {
            return getQualityDescriptorAsRecord(channelAddress.dataType(), informationElements, typeIdentification,
                    timestamp);
        }
        else {
            switch (typeIdentification) {
            case M_ME_NA_1:
            case M_ME_TA_1:
            case M_ME_ND_1:
            case M_ME_TD_1:
            case C_SE_NA_1:
            case P_ME_NA_1:
                IeNormalizedValue normalizedValue = (IeNormalizedValue) informationElements[0]; // TODO: is 0 correct?
                return new Record(new DoubleValue(normalizedValue.getValue() / 32768.), timestamp);
            case M_ME_NB_1:
            case M_ME_TB_1:
            case M_ME_TE_1:
            case C_SE_NB_1:
            case P_ME_NB_1:
                IeScaledValue scaledValue = (IeScaledValue) informationElements[0];// TODO: is 0 correct?
                return new Record(new IntValue(scaledValue.getValue()), timestamp); // test this
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
            case M_ST_NA_1: // TODO: test this!!! It's not really a SinglePointInformation
            case M_ST_TA_1: // TODO: test this!!! It's not really a SinglePointInformation
            case M_ST_TB_1: // TODO: test this!!! It's not really a SinglePointInformation
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
            TypeId typeIdentification, long timestamp) {
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

        switch (dataType) {// iv ca cy
        case "iv":
            record = new Record(new BooleanValue(quality.isInvalid()), timestamp);
            break;
        case "ca":
            record = new Record(new BooleanValue(quality.isCounterAdjusted()), timestamp);
            break;
        case "cy":
            record = new Record(new BooleanValue(quality.isCarry()), timestamp);
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
                IeBinaryStateInformation binaryStateInformation = (IeBinaryStateInformation) informationElements[0];
                byteBuffer.putInt(binaryStateInformation.getValue());
            } catch (ConfigurationException e) {
                logger.warn(e.getMessage());
                return new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
            }
        }

        byte[] value = byteBuffer.array();
        Record record = new Record(new ByteArrayValue(value), timestamp);

        return record;
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

    private static int sizeOfType(TypeId typeIdentification) {
        int size = -1; // size in byte;
        switch (typeIdentification) {
        case M_BO_NA_1:
        case M_BO_TA_1:
        case M_BO_TB_1:
            size = 4;
            break;
        default:
            logger.debug(
                    "Not able to set Data Type " + typeIdentification.toString() + "  as multiple IOAs or Indices.");
            break;
        }
        return size;
    }

}
