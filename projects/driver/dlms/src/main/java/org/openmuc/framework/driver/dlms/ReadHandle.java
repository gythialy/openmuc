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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.dlms.settings.ChannelAddress;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.datatypes.DataObject.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReadHandle {

    private static final Logger logger = LoggerFactory.getLogger(ReadHandle.class);

    private final DlmsConnection dlmsConnection;

    public ReadHandle(DlmsConnection dlmsConnection) {
        this.dlmsConnection = dlmsConnection;
    }

    public void read(List<ChannelRecordContainer> containers) throws ConnectionException {
        List<ChannelRecordContainer> readList = new ArrayList<>(containers);

        try {
            callGet(dlmsConnection, readList);
        } catch (IOException ex) {
            handleIoException(containers, ex);
        }
    }

    private List<AttributeAddress> createAttributeAddresFor(List<ChannelRecordContainer> readList)
            throws ConnectionException {
        List<AttributeAddress> getParams = new ArrayList<>(readList.size());
        for (ChannelRecordContainer recordContainer : readList) {
            try {
                ChannelAddress channelAddress = new ChannelAddress(recordContainer.getChannelAddress());
                getParams.add(channelAddress.getAttributeAddress());
            } catch (ArgumentSyntaxException e) {
                throw new ConnectionException(e);
            }
        }
        return getParams;
    }

    private void callGet(DlmsConnection dlmsConnection, List<ChannelRecordContainer> readList)
            throws IOException, ConnectionException {

        final long timestamp = System.currentTimeMillis();

        List<AttributeAddress> getParams = createAttributeAddresFor(readList);

        Iterator<ChannelRecordContainer> writeListIter = readList.iterator();
        Iterator<GetResult> resIter = this.dlmsConnection.get(getParams).iterator();

        while (writeListIter.hasNext() && resIter.hasNext()) {
            ChannelRecordContainer channelContainer = writeListIter.next();
            Record record = createRecordFor(timestamp, resIter.next());
            channelContainer.setRecord(record);
        }
    }

    private static void handleIoException(List<ChannelRecordContainer> containers, IOException ex)
            throws ConnectionException {
        logger.error("Failed to read from device.", ex);

        final long timestamp = System.currentTimeMillis();

        for (ChannelRecordContainer c : containers) {
            c.setRecord(new Record(null, timestamp, Flag.COMM_DEVICE_NOT_CONNECTED));
        }

        throw new ConnectionException(ex.getMessage());
    }

    private static Record createRecordFor(final long timestamp, GetResult result) {

        if (result.getResultCode() == AccessResultCode.SUCCESS) {
            Flag resultFlag = Flag.VALID;
            Value resultValue = convertToValue(result.getResultData());
            return new Record(resultValue, timestamp, resultFlag);
        }
        else {
            Flag resultFlag = convertStatusFlag(result.getResultCode());
            return new Record(null, timestamp, resultFlag);
        }
    }

    @SuppressWarnings("unused")
    private static ValueType getType(DataObject dataObject) {
        final ValueType valueType;
        final Type type = dataObject.getType();

        switch (type) {

        case BOOLEAN:
            valueType = ValueType.BOOLEAN;
            break;

        case FLOAT32:
            valueType = ValueType.FLOAT;
            break;

        case FLOAT64:
            valueType = ValueType.DOUBLE;
            break;

        case BCD:
        case INTEGER:
            valueType = ValueType.BYTE;
            break;

        case LONG_INTEGER:
        case UNSIGNED:
            valueType = ValueType.SHORT;
            break;

        case ENUMERATE:
        case DOUBLE_LONG:
        case LONG_UNSIGNED:
            valueType = ValueType.INTEGER;
            break;

        case DOUBLE_LONG_UNSIGNED:
        case LONG64:
        case LONG64_UNSIGNED: // Long is to small for Long unsigned
            valueType = ValueType.LONG;
            break;

        case OCTET_STRING:
            valueType = ValueType.BYTE_ARRAY;
            break;

        case VISIBLE_STRING:
            valueType = ValueType.STRING;
            break;

        case NULL_DATA:
            valueType = null; // TODO
            break;

        case ARRAY:
        case BIT_STRING:
        case COMPACT_ARRAY:
        case DONT_CARE:
        case DATE:
        case DATE_TIME:
        case STRUCTURE:
        case TIME:
        default:
            valueType = null;
            break;
        }

        return valueType;
    }

    private static Flag convertStatusFlag(AccessResultCode accessResultCode) {
        switch (accessResultCode) {
        case HARDWARE_FAULT:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
        case TEMPORARY_FAILURE:
            return Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE;
        case READ_WRITE_DENIED:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
        case OBJECT_UNDEFINED:
            return Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND;
        case OBJECT_UNAVAILABLE:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
        default:
            return Flag.UNKNOWN_ERROR;
        }
    }

    private static Value convertToValue(DataObject data) {
        if (data.isBoolean()) {
            return new BooleanValue((boolean) data.getValue());
        }
        else if (data.isNumber()) {
            Number numberVal = data.getValue();
            return new DoubleValue(numberVal.doubleValue());
        }
        // else if (data.isCosemDateFormat()) {
        // CosemDateFormat cosemDate = data.getValue();
        // return new LongValue(cosemDate.toCalendar().getTimeInMillis());
        // }
        else if (data.isByteArray()) {
            return new ByteArrayValue((byte[]) data.getValue());
        }
        else if (data.isBitString()) {
            return new StringValue(data.getValue().toString());
        }
        else if (data.isComplex()) {
            // better solution?
            return new StringValue(data.toString());
        }
        else {
            return new StringValue(data.toString());
        }
    }

}
