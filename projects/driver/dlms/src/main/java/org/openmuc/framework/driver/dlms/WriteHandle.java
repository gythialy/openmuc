/*
 * Copyright 2011-16 Fraunhofer ISE
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

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.jdlms.client.AccessResultCode;
import org.openmuc.jdlms.client.Data;
import org.openmuc.jdlms.client.GetRequest;
import org.openmuc.jdlms.client.GetResult;
import org.openmuc.jdlms.client.SetRequest;

public class WriteHandle {
    private final ChannelValueContainer container;
    private GetResult getResult;
    private AccessResultCode setResult;

    private int getIndex = -1;
    private int setIndex = -1;
    private Flag flag;

    public WriteHandle(ChannelValueContainer container) {
        this.container = container;
    }

    public GetRequest createGetRequest() {
        GetRequest channelHandle = null;
        if (container.getChannelHandle() == null) {
            try {
                channelHandle = ChannelAddress.parse(container.getChannelAddress()).createGetRequest();
                container.setChannelHandle(channelHandle);
            } catch (IllegalArgumentException e) {
                flag = Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID;
            }
        }
        else {
            channelHandle = (GetRequest) container.getChannelHandle();
        }
        return channelHandle;
    }

    public void setGetResult(GetResult result) {
        getResult = result;
        if (getResult == null) {
            flag = Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
        }
        else {
            if (!getResult.isSuccess()) {
                flag = Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
            }
        }
    }

    public SetRequest createSetRequest() {
        if (flag != null) {
            return null;
        }
        SetRequest result = ((GetRequest) container.getChannelHandle()).toSetRequest();
        Data originData = getResult.getResultData();
        Data param = result.data();

        switch (originData.getChoiceIndex()) {
        case NULL_DATA:
            param.setNull();
            break;
        case ARRAY:
        case STRUCTURE:
        case COMPACT_ARRAY:
        case DATE_TIME:
        case DATE:
        case TIME:
        case DONT_CARE:
            flag = Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
            result = null;
            break;
        case BOOL:
            param.setbool(container.getValue().asBoolean());
            break;
        case BIT_STRING:
            param.setBitString(container.getValue().asByteArray(), container.getValue().asByteArray().length * 8);
            break;
        case DOUBLE_LONG:
            param.setInteger32(container.getValue().asInt());
            break;
        case DOUBLE_LONG_UNSIGNED:
            param.setUnsigned32(container.getValue().asLong());
            break;
        case OCTET_STRING:
            param.setOctetString(container.getValue().asByteArray());
            break;
        case VISIBLE_STRING:
            param.setVisibleString(container.getValue().asByteArray());
            break;
        case BCD:
            param.setBcd(container.getValue().asByte());
            break;
        case INTEGER:
            param.setInteger8(container.getValue().asByte());
            break;
        case LONG_INTEGER:
            param.setInteger16(container.getValue().asShort());
            break;
        case UNSIGNED:
            param.setUnsigned8(container.getValue().asShort());
            break;
        case LONG_UNSIGNED:
            param.setUnsigned16(container.getValue().asInt());
            break;
        case LONG64:
            param.setInteger64(container.getValue().asLong());
            break;
        case LONG64_UNSIGNED:
            param.setUnsigned64(container.getValue().asLong());
            break;
        case ENUMERATE:
            param.setEnumerate(container.getValue().asByte());
            break;
        case FLOAT32:
            param.setFloat32(container.getValue().asFloat());
            break;
        case FLOAT64:
            param.setFloat64(container.getValue().asDouble());
            break;
        }
        return result;
    }

    public void setSetResult(AccessResultCode result) {
        setResult = result;
    }

    public void writeFlag() {
        if (setResult == null) {
            container.setFlag(flag);
        }
        else {
            if (setResult == AccessResultCode.SUCCESS) {
                container.setFlag(Flag.VALID);
            }
            else if (setResult == AccessResultCode.HARDWARE_FAULT) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
            }
            else if (setResult == AccessResultCode.TEMPORARY_FAILURE) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE);
            }
            else if (setResult == AccessResultCode.READ_WRITE_DENIED) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
            }
            else if (setResult == AccessResultCode.OBJECT_UNDEFINED) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
            }
            else if (setResult == AccessResultCode.OBJECT_UNAVAILABLE) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
            }
            else {
                container.setFlag(Flag.UNKNOWN_ERROR);
            }
        }
    }

    public void setReadIndex(int index) {
        getIndex = index;
    }

    public int getReadIndex() {
        return getIndex;
    }

    public void setWriteIndex(int index) {
        setIndex = index;
    }

    public int getWriteIndex() {
        return setIndex;
    }
}
