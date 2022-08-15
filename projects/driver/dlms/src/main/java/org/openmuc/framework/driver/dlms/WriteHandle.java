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
package org.openmuc.framework.driver.dlms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.dlms.settings.ChannelAddress;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.CosemDate;
import org.openmuc.jdlms.datatypes.CosemDateTime;
import org.openmuc.jdlms.datatypes.CosemDateTime.ClockStatus;
import org.openmuc.jdlms.datatypes.CosemTime;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.datatypes.DataObject.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WriteHandle {

    private static final Logger logger = LoggerFactory.getLogger(WriteHandle.class);

    private final DlmsConnection dlmsConnection;

    public WriteHandle(DlmsConnection dlmsConnection) {
        this.dlmsConnection = dlmsConnection;
    }

    public void write(List<ChannelValueContainer> containers)
            throws ConnectionException, UnsupportedOperationException {
        multiSet(new ArrayList<>(containers));
    }

    private void multiSet(List<ChannelValueContainer> writeList) throws ConnectionException {
        List<AccessResultCode> resultCodes = callSet(writeList);

        Iterator<AccessResultCode> iterResult = resultCodes.iterator();
        Iterator<ChannelValueContainer> iterWriteList = writeList.iterator();
        while (iterResult.hasNext() && iterWriteList.hasNext()) {
            ChannelValueContainer valueContainer = iterWriteList.next();
            AccessResultCode resCode = iterResult.next();

            Flag flag = convertToFlag(resCode);
            valueContainer.setFlag(flag);
        }
    }

    private List<AccessResultCode> callSet(List<ChannelValueContainer> writeList) throws ConnectionException {

        List<SetParameter> setParams = createSetParamsFor(writeList);

        List<AccessResultCode> resultCodes = null;
        try {
            resultCodes = this.dlmsConnection.set(setParams);
        } catch (IOException ex) {
            handleIoException(writeList, ex);
        }

        if (resultCodes == null) {
            throw new ConnectionException("Did not get any result after xDLMS SET was called.");
        }
        return resultCodes;
    }

    private static List<SetParameter> createSetParamsFor(List<ChannelValueContainer> writeList)
            throws ConnectionException {
        List<SetParameter> setParams = new ArrayList<>(writeList.size());

        for (ChannelValueContainer channelContainer : writeList) {
            try {
                ChannelAddress channelAddress = new ChannelAddress(channelContainer.getChannelAddress());
                Type type = channelAddress.getType();

                if (type == null) {
                    String msg = MessageFormat.format(
                            "Can not set attribute with address {0} where the type is unknown.", channelAddress);
                    throw new ConnectionException(msg);
                }
                DataObject newValue = createDoFor(channelContainer, type);
                AttributeAddress address = channelAddress.getAttributeAddress();
                SetParameter setParameter = new SetParameter(address, newValue);

                setParams.add(setParameter);
            } catch (ArgumentSyntaxException e) {
                throw new ConnectionException(e);
            }
        }
        return setParams;
    }

    private static Flag convertToFlag(AccessResultCode setResult) {

        // should not occur
        if (setResult == null) {
            return Flag.UNKNOWN_ERROR;
        }

        switch (setResult) {
        case HARDWARE_FAULT:
            return Flag.UNKNOWN_ERROR;

        case OBJECT_UNDEFINED:
            return Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND;

        case OBJECT_UNAVAILABLE:
        case READ_WRITE_DENIED:
        case SCOPE_OF_ACCESS_VIOLATED:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;

        case SUCCESS:
            return Flag.VALID;

        case TEMPORARY_FAILURE:
            return Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE;

        case OBJECT_CLASS_INCONSISTENT:
        case TYPE_UNMATCHED:
            return Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;

        case DATA_BLOCK_NUMBER_INVALID:
        case DATA_BLOCK_UNAVAILABLE:
        case LONG_GET_ABORTED:
        case LONG_SET_ABORTED:
        case NO_LONG_GET_IN_PROGRESS:
        case NO_LONG_SET_IN_PROGRESS:
        case OTHER_REASON:
        default:
            return Flag.DRIVER_THREW_UNKNOWN_EXCEPTION;
        }
    }

    private static void handleIoException(List<ChannelValueContainer> containers, IOException ex)
            throws ConnectionException {
        logger.error("Faild to write to device.", ex);
        for (ChannelValueContainer c : containers) {
            c.setFlag(Flag.COMM_DEVICE_NOT_CONNECTED);
        }
        throw new ConnectionException(ex.getMessage());
    }

    private static DataObject createDoFor(ChannelValueContainer channelValueContainer, Type type)
            throws UnsupportedOperationException {
        Flag flag = channelValueContainer.getFlag();

        if (flag != Flag.VALID) {
            return null;
        }

        Value value = channelValueContainer.getValue();
        switch (type) {
        case BCD:
            return DataObject.newBcdData(value.asByte());
        case BOOLEAN:
            return DataObject.newBoolData(value.asBoolean());
        case DOUBLE_LONG:
            return DataObject.newInteger32Data(value.asInt());
        case DOUBLE_LONG_UNSIGNED:
            return DataObject.newUInteger32Data(value.asLong()); // TODO: not safe!
        case ENUMERATE:
            return DataObject.newEnumerateData(value.asInt());
        case FLOAT32:
            return DataObject.newFloat32Data(value.asFloat());
        case FLOAT64:
            return DataObject.newFloat64Data(value.asDouble());
        case INTEGER:
            return DataObject.newInteger8Data(value.asByte());
        case LONG64:
            return DataObject.newInteger64Data(value.asLong());
        case LONG64_UNSIGNED:
            return DataObject.newUInteger64Data(value.asLong()); // TODO: is not unsigned
        case LONG_INTEGER:
            return DataObject.newInteger16Data(value.asShort());
        case LONG_UNSIGNED:
            return DataObject.newUInteger16Data(value.asInt()); // TODO: not safe!
        case NULL_DATA:
            return DataObject.newNullData();
        case OCTET_STRING:
            return DataObject.newOctetStringData(value.asByteArray());
        case UNSIGNED:
            return DataObject.newUInteger8Data(value.asShort()); // TODO: not safe!
        case UTF8_STRING:
            byte[] byteArrayValue = byteArrayValueOf(value);
            return DataObject.newUtf8StringData(byteArrayValue);
        case VISIBLE_STRING:
            byteArrayValue = byteArrayValueOf(value);
            return DataObject.newVisibleStringData(byteArrayValue);
        case DATE:
            Calendar calendar = getCalendar(value.asLong());
            return DataObject.newDateData(new CosemDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)));
        case DATE_TIME:
            calendar = getCalendar(value.asLong());
            return DataObject.newDateTimeData(new CosemDateTime(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.DAY_OF_WEEK), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                    calendar.get(Calendar.MILLISECOND) / 10, 0x8000, ClockStatus.INVALID_CLOCK_STATUS));
        case TIME:
            calendar = getCalendar(value.asLong());
            return DataObject.newTimeData(new CosemTime(calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
        case ARRAY:
        case BIT_STRING:
        case COMPACT_ARRAY:
        case DONT_CARE:
        case STRUCTURE:
        default:
            String message = MessageFormat.format("DateType {0} not supported, yet.", type.toString());
            throw new UnsupportedOperationException(message);

        }

    }

    private static byte[] byteArrayValueOf(Value value) {
        if (value instanceof StringValue) {
            return value.asString().getBytes(StandardCharsets.UTF_8);
        }
        else if (value instanceof ByteArrayValue) {
            return value.asByteArray();
        }
        else {
            return new byte[0];
        }
    }

    private static Calendar getCalendar(long timestampInMilisec) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timestampInMilisec);
        return calendar;
    }

}
