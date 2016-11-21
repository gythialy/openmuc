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
package org.openmuc.framework.driver.iec61850;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.openiec61850.BasicDataAttribute;
import org.openmuc.openiec61850.BdaBitString;
import org.openmuc.openiec61850.BdaBoolean;
import org.openmuc.openiec61850.BdaEntryTime;
import org.openmuc.openiec61850.BdaFloat32;
import org.openmuc.openiec61850.BdaFloat64;
import org.openmuc.openiec61850.BdaInt16;
import org.openmuc.openiec61850.BdaInt16U;
import org.openmuc.openiec61850.BdaInt32;
import org.openmuc.openiec61850.BdaInt32U;
import org.openmuc.openiec61850.BdaInt64;
import org.openmuc.openiec61850.BdaInt8;
import org.openmuc.openiec61850.BdaInt8U;
import org.openmuc.openiec61850.BdaOctetString;
import org.openmuc.openiec61850.BdaTimestamp;
import org.openmuc.openiec61850.BdaUnicodeString;
import org.openmuc.openiec61850.BdaVisibleString;
import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.Fc;
import org.openmuc.openiec61850.FcModelNode;
import org.openmuc.openiec61850.ModelNode;
import org.openmuc.openiec61850.ServerModel;
import org.openmuc.openiec61850.ServiceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Iec61850Connection implements Connection {

    private final static Logger logger = LoggerFactory.getLogger(Iec61850Connection.class);

    private final static String STRING_SEPARATOR = ",";

    private final ClientAssociation clientAssociation;
    private final ServerModel serverModel;

    public Iec61850Connection(ClientAssociation clientAssociation, ServerModel serverModel) {
        this.clientAssociation = clientAssociation;
        this.serverModel = serverModel;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        List<BasicDataAttribute> bdas = serverModel.getBasicDataAttributes();

        List<ChannelScanInfo> scanInfos = new ArrayList<>(bdas.size());

        for (BasicDataAttribute bda : bdas) {

            String channelAddress = bda.getReference() + ":" + bda.getFc();

            switch (bda.getBasicType()) {

            case CHECK:
            case DOUBLE_BIT_POS:
            case OPTFLDS:
            case QUALITY:
            case REASON_FOR_INCLUSION:
            case TAP_COMMAND:
            case TRIGGER_CONDITIONS:
            case ENTRY_TIME:
            case OCTET_STRING:
            case VISIBLE_STRING:
            case UNICODE_STRING:
                bda.setDefault();
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length));
                break;
            case TIMESTAMP:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.LONG, null));
                break;
            case BOOLEAN:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.BOOLEAN, null));
                break;
            case FLOAT32:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.FLOAT, null));
                break;
            case FLOAT64:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.DOUBLE, null));
                break;
            case INT8:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.BYTE, null));
                break;
            case INT8U:
            case INT16:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.SHORT, null));
                break;
            case INT16U:
            case INT32:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.INTEGER, null));
                break;
            case INT32U:
            case INT64:
                scanInfos.add(new ChannelScanInfo(channelAddress, "", ValueType.LONG, null));
                break;
            default:
                throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
            }

        }

        return scanInfos;

    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer container : containers) {

            if (container.getChannelHandle() == null) {

                String[] args = container.getChannelAddress().split(":", 3);

                if (args.length != 2) {
                    logger.debug("Wrong channel address syntax: {}", container.getChannelAddress());
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                    continue;
                }

                ModelNode modelNode = serverModel.findModelNode(args[0], Fc.fromString(args[1]));

                if (modelNode == null) {
                    logger.debug("No Basic Data Attribute for the channel address {} was found in the server model.",
                            container.getChannelAddress());
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                    continue;
                }

                FcModelNode fcModelNode;
                try {
                    fcModelNode = (FcModelNode) modelNode;
                } catch (ClassCastException e) {
                    logger.debug(
                            "ModelNode with object reference {} was found in the server model but is not a Basic Data Attribute.",
                            container.getChannelAddress());
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                    continue;
                }

                container.setChannelHandle(fcModelNode);

            }
        }

        if (!samplingGroup.isEmpty()) {

            FcModelNode fcModelNode;
            if (containerListHandle != null) {
                fcModelNode = (FcModelNode) containerListHandle;
            }

            else {

                String[] args = samplingGroup.split(":", 3);

                if (args.length != 2) {
                    logger.debug("Wrong sampling group syntax: {}", samplingGroup);
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND));
                    }
                    return null;
                }

                ModelNode modelNode = serverModel.findModelNode(args[0], Fc.fromString(args[1]));

                if (modelNode == null) {
                    logger.debug(
                            "Error reading sampling group: no FCDO/DA or DataSet with object reference {} was not found in the server model.",
                            samplingGroup);
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND));
                    }
                    return null;
                }

                try {
                    fcModelNode = (FcModelNode) modelNode;
                } catch (ClassCastException e) {
                    logger.debug(
                            "Error reading channel: ModelNode with sampling group reference {} was found in the server model but is not a FcModelNode.",
                            samplingGroup);
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND));
                    }
                    return null;
                }

            }

            try {
                clientAssociation.getDataValues(fcModelNode);
            } catch (ServiceError e) {
                logger.debug("Error reading sampling group: service error calling getDataValues on {}: {}",
                        samplingGroup, e);
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_ACCESSIBLE));
                }
                return fcModelNode;
            } catch (IOException e) {
                throw new ConnectionException(e);
            }

            long receiveTime = System.currentTimeMillis();

            for (ChannelRecordContainer container : containers) {
                if (container.getChannelHandle() != null) {
                    setRecord(container, (BasicDataAttribute) container.getChannelHandle(), receiveTime);
                }
                else {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_PART_OF_SAMPLING_GROUP));
                }
            }

            return fcModelNode;

        }
        // sampling group is empty
        else {

            for (ChannelRecordContainer container : containers) {

                if (container.getChannelHandle() == null) {
                    continue;
                }
                FcModelNode fcModelNode = (FcModelNode) container.getChannelHandle();
                try {
                    clientAssociation.getDataValues(fcModelNode);
                } catch (ServiceError e) {
                    logger.debug("Error reading channel: service error calling getDataValues on {}: {}",
                            container.getChannelAddress(), e);
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
                    continue;
                } catch (IOException e) {
                    throw new ConnectionException(e);
                }

                if (fcModelNode instanceof BasicDataAttribute) {
                    long receiveTime = System.currentTimeMillis();
                    setRecord(container, (BasicDataAttribute) fcModelNode, receiveTime);
                }
                else {
                    StringBuilder sb = new StringBuilder("");
                    for (BasicDataAttribute bda : fcModelNode.getBasicDataAttributes()) {
                        sb.append(bda2String(bda) + STRING_SEPARATOR);
                    }
                    sb.delete(sb.length() - 1, sb.length());// remove last separator
                    long receiveTime = System.currentTimeMillis();
                    setRecord(container, sb.toString(), receiveTime);
                }
            }
            return null;
        }

    }

    private String bda2String(BasicDataAttribute bda) {
        String result;
        switch (bda.getBasicType()) {
        case CHECK:
        case DOUBLE_BIT_POS:
        case OPTFLDS:
        case QUALITY:
        case REASON_FOR_INCLUSION:
        case TAP_COMMAND:
        case TRIGGER_CONDITIONS:
        case ENTRY_TIME:
        case OCTET_STRING:
        case VISIBLE_STRING:
        case UNICODE_STRING:
            result = new String(((BdaBitString) bda).getValue());
            break;
        case TIMESTAMP:
            Date date = ((BdaTimestamp) bda).getDate();
            result = date == null ? "<invalid date>" : ("" + date.getTime());
            break;
        case BOOLEAN:
            result = String.valueOf(((BdaBoolean) bda).getValue());
            break;
        case FLOAT32:
            result = String.valueOf(((BdaFloat32) bda).getFloat());
            break;
        case FLOAT64:
            result = String.valueOf(((BdaFloat64) bda).getDouble());
            break;
        case INT8:
            result = String.valueOf(((BdaInt8) bda).getValue());
            break;
        case INT8U:
            result = String.valueOf(((BdaInt8U) bda).getValue());
            break;
        case INT16:
            result = String.valueOf(((BdaInt16) bda).getValue());
            break;
        case INT16U:
            result = String.valueOf(((BdaInt16U) bda).getValue());
            break;
        case INT32:
            result = String.valueOf(((BdaInt32) bda).getValue());
            break;
        case INT32U:
            result = String.valueOf(((BdaInt32U) bda).getValue());
            break;
        case INT64:
            result = String.valueOf(((BdaInt64) bda).getValue());
            break;
        default:
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
        return result;
    }

    private void setRecord(ChannelRecordContainer container, String stringValue, long receiveTime) {
        container.setRecord(new Record(new ByteArrayValue(stringValue.getBytes(), true), receiveTime));
    }

    private void setRecord(ChannelRecordContainer container, BasicDataAttribute bda, long receiveTime) {
        switch (bda.getBasicType()) {
        case CHECK:
        case DOUBLE_BIT_POS:
        case OPTFLDS:
        case QUALITY:
        case REASON_FOR_INCLUSION:
        case TAP_COMMAND:
        case TRIGGER_CONDITIONS:
            container.setRecord(new Record(new ByteArrayValue(((BdaBitString) bda).getValue(), true), receiveTime));
            break;
        case ENTRY_TIME:
            container.setRecord(new Record(new ByteArrayValue(((BdaEntryTime) bda).getValue(), true), receiveTime));
            break;
        case OCTET_STRING:
            container.setRecord(new Record(new ByteArrayValue(((BdaOctetString) bda).getValue(), true), receiveTime));
            break;
        case VISIBLE_STRING:
            container.setRecord(new Record(new StringValue(((BdaVisibleString) bda).getStringValue()), receiveTime));
            break;
        case UNICODE_STRING:
            container.setRecord(new Record(new ByteArrayValue(((BdaUnicodeString) bda).getValue(), true), receiveTime));
            break;
        case TIMESTAMP:
            Date date = ((BdaTimestamp) bda).getDate();
            if (date == null) {
                container.setRecord(new Record(new LongValue(-1l), receiveTime));
            }
            else {
                container.setRecord(new Record(new LongValue(date.getTime()), receiveTime));
            }
            break;
        case BOOLEAN:
            container.setRecord(new Record(new BooleanValue(((BdaBoolean) bda).getValue()), receiveTime));
            break;
        case FLOAT32:
            container.setRecord(new Record(new FloatValue(((BdaFloat32) bda).getFloat()), receiveTime));
            break;
        case FLOAT64:
            container.setRecord(new Record(new DoubleValue(((BdaFloat64) bda).getDouble()), receiveTime));
            break;
        case INT8:
            container.setRecord(new Record(new DoubleValue(((BdaInt8) bda).getValue()), receiveTime));
            break;
        case INT8U:
            container.setRecord(new Record(new DoubleValue(((BdaInt8U) bda).getValue()), receiveTime));
            break;
        case INT16:
            container.setRecord(new Record(new DoubleValue(((BdaInt16) bda).getValue()), receiveTime));
            break;
        case INT16U:
            container.setRecord(new Record(new DoubleValue(((BdaInt16U) bda).getValue()), receiveTime));
            break;
        case INT32:
            container.setRecord(new Record(new DoubleValue(((BdaInt32) bda).getValue()), receiveTime));
            break;
        case INT32U:
            container.setRecord(new Record(new DoubleValue(((BdaInt32U) bda).getValue()), receiveTime));
            break;
        case INT64:
            container.setRecord(new Record(new DoubleValue(((BdaInt64) bda).getValue()), receiveTime));
            break;
        default:
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        List<FcModelNode> modelNodesToBeWritten = new ArrayList<>(containers.size());
        for (ChannelValueContainer container : containers) {

            if (container.getChannelHandle() != null) {
                modelNodesToBeWritten.add((FcModelNode) container.getChannelHandle());
                setFcModelNode(container, (FcModelNode) container.getChannelHandle());
            }
            else {

                String[] args = container.getChannelAddress().split(":", 3);

                if (args.length != 2) {
                    logger.debug("Wrong channel address syntax: {}", container.getChannelAddress());
                    container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
                    continue;
                }

                ModelNode modelNode = serverModel.findModelNode(args[0], Fc.fromString(args[1]));

                if (modelNode == null) {
                    logger.debug("No Basic Data Attribute for the channel address {} was found in the server model.",
                            container.getChannelAddress());
                    container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
                    continue;
                }

                FcModelNode fcModelNode;
                try {
                    fcModelNode = (FcModelNode) modelNode;

                } catch (ClassCastException e) {
                    logger.debug(
                            "ModelNode with object reference {} was found in the server model but is not a Basic Data Attribute.",
                            container.getChannelAddress());
                    container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
                    continue;
                }
                container.setChannelHandle(fcModelNode);
                modelNodesToBeWritten.add(fcModelNode);
                setFcModelNode(container, fcModelNode);

            }
        }

        // TODO
        // first check all datasets if the are some that contain only requested channels
        // then check all remaining model nodes

        List<FcModelNode> fcNodesToBeRequested = new ArrayList<>();

        while (modelNodesToBeWritten.size() > 0) {
            fillRequestedNodes(fcNodesToBeRequested, modelNodesToBeWritten, serverModel);
        }

        for (FcModelNode fcModelNode : fcNodesToBeRequested) {
            try {
                clientAssociation.setDataValues(fcModelNode);
            } catch (ServiceError e) {
                logger.debug("Error writing to channel: service error calling setDataValues on {}: {}",
                        fcModelNode.getReference(), e);
                for (BasicDataAttribute bda : fcModelNode.getBasicDataAttributes()) {
                    for (ChannelValueContainer valueContainer : containers) {
                        if (valueContainer.getChannelHandle() == bda) {
                            valueContainer.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
                        }
                    }
                }
                return null;
            } catch (IOException e) {
                throw new ConnectionException(e);
            }
            for (BasicDataAttribute bda : fcModelNode.getBasicDataAttributes()) {
                for (ChannelValueContainer valueContainer : containers) {
                    if (valueContainer.getChannelHandle() == bda) {
                        valueContainer.setFlag(Flag.VALID);
                    }
                }
            }
        }

        return null;

    }

    void fillRequestedNodes(List<FcModelNode> fcNodesToBeRequested, List<FcModelNode> remainingFcModelNodes,
            ServerModel serverModel) {

        FcModelNode currentFcModelNode = remainingFcModelNodes.get(0);

        if (!checkParent(currentFcModelNode, fcNodesToBeRequested, remainingFcModelNodes, serverModel)) {
            remainingFcModelNodes.remove(currentFcModelNode);
            fcNodesToBeRequested.add(currentFcModelNode);
        }

    }

    boolean checkParent(ModelNode modelNode, List<FcModelNode> fcNodesToBeRequested,
            List<FcModelNode> remainingModelNodes, ServerModel serverModel) {

        if (!(modelNode instanceof FcModelNode)) {
            return false;
        }

        FcModelNode fcModelNode = (FcModelNode) modelNode;

        ModelNode parentNode = serverModel;
        for (int i = 0; i < fcModelNode.getReference().size() - 1; i++) {
            parentNode = parentNode.getChild(fcModelNode.getReference().get(i), fcModelNode.getFc());
        }

        List<BasicDataAttribute> basicDataAttributes = parentNode.getBasicDataAttributes();
        for (BasicDataAttribute bda : basicDataAttributes) {
            if (!remainingModelNodes.contains(bda)) {
                return false;
            }
        }

        if (!checkParent(parentNode, fcNodesToBeRequested, remainingModelNodes, serverModel)) {
            for (BasicDataAttribute bda : basicDataAttributes) {
                remainingModelNodes.remove(bda);
            }
            fcNodesToBeRequested.add((FcModelNode) parentNode);
        }

        return true;
    }

    private void setFcModelNode(ChannelValueContainer container, FcModelNode fcModelNode) {
        if (fcModelNode instanceof BasicDataAttribute) {
            setBda(container, (BasicDataAttribute) fcModelNode);
        }
        else {
            List<BasicDataAttribute> bdas = fcModelNode.getBasicDataAttributes();
            String valueString = container.getValue().toString();
            String[] bdaValues = valueString.split(STRING_SEPARATOR);
            if (bdaValues.length != bdas.size()) {
                throw new IllegalStateException("attempt to write array " + valueString + " into fcModelNode "
                        + fcModelNode.getName() + " failed as the dimensions don't fit.");
            }
            for (int i = 0; i < bdaValues.length; i++) {
                setBda(bdaValues[i], bdas.get(i));
            }
        }
    }

    private void setBda(String bdaValueString, BasicDataAttribute bda) {
        switch (bda.getBasicType()) {
        case CHECK:
        case DOUBLE_BIT_POS:
        case OPTFLDS:
        case QUALITY:
        case REASON_FOR_INCLUSION:
        case TAP_COMMAND:
        case TRIGGER_CONDITIONS:
            ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            break;
        case ENTRY_TIME:
            ((BdaEntryTime) bda).setValue(bdaValueString.getBytes());
            break;
        case OCTET_STRING:
            ((BdaOctetString) bda).setValue(bdaValueString.getBytes());
            break;
        case VISIBLE_STRING:
            ((BdaVisibleString) bda).setValue(bdaValueString);
            break;
        case UNICODE_STRING:
            ((BdaUnicodeString) bda).setValue(bdaValueString.getBytes());
            break;
        case TIMESTAMP:
            ((BdaTimestamp) bda).setDate(new Date(Long.parseLong(bdaValueString)));
            break;
        case BOOLEAN:
            ((BdaBoolean) bda).setValue(Boolean.parseBoolean(bdaValueString));
            break;
        case FLOAT32:
            ((BdaFloat32) bda).setFloat(Float.parseFloat(bdaValueString));
            break;
        case FLOAT64:
            ((BdaFloat64) bda).setDouble(Double.parseDouble(bdaValueString));
            break;
        case INT8:
            ((BdaInt8) bda).setValue(Byte.parseByte(bdaValueString));
            break;
        case INT8U:
            ((BdaInt8U) bda).setValue(Short.parseShort(bdaValueString));
            break;
        case INT16:
            ((BdaInt16) bda).setValue(Short.parseShort(bdaValueString));
            break;
        case INT16U:
            ((BdaInt16U) bda).setValue(Integer.parseInt(bdaValueString));
            break;
        case INT32:
            ((BdaInt32) bda).setValue(Integer.parseInt(bdaValueString));
            break;
        case INT32U:
            ((BdaInt32U) bda).setValue(Long.parseLong(bdaValueString));
            break;
        case INT64:
            ((BdaInt64) bda).setValue(Long.parseLong(bdaValueString));
            break;
        default:
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    private void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
        switch (bda.getBasicType()) {
        case CHECK:
        case DOUBLE_BIT_POS:
        case OPTFLDS:
        case QUALITY:
        case REASON_FOR_INCLUSION:
        case TAP_COMMAND:
        case TRIGGER_CONDITIONS:
            ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            break;
        case ENTRY_TIME:
            ((BdaEntryTime) bda).setValue(container.getValue().asByteArray());
            break;
        case OCTET_STRING:
            ((BdaOctetString) bda).setValue(container.getValue().asByteArray());
            break;
        case VISIBLE_STRING:
            ((BdaVisibleString) bda).setValue(container.getValue().asString());
            break;
        case UNICODE_STRING:
            ((BdaUnicodeString) bda).setValue(container.getValue().asByteArray());
            break;
        case TIMESTAMP:
            ((BdaTimestamp) bda).setDate(new Date(container.getValue().asLong()));
            break;
        case BOOLEAN:
            ((BdaBoolean) bda).setValue(container.getValue().asBoolean());
            break;
        case FLOAT32:
            ((BdaFloat32) bda).setFloat(container.getValue().asFloat());
            break;
        case FLOAT64:
            ((BdaFloat64) bda).setDouble(container.getValue().asDouble());
            break;
        case INT8:
            ((BdaInt8) bda).setValue(container.getValue().asByte());
            break;
        case INT8U:
            ((BdaInt8U) bda).setValue(container.getValue().asShort());
            break;
        case INT16:
            ((BdaInt16) bda).setValue(container.getValue().asShort());
            break;
        case INT16U:
            ((BdaInt16U) bda).setValue(container.getValue().asInt());
            break;
        case INT32:
            ((BdaInt32) bda).setValue(container.getValue().asInt());
            break;
        case INT32U:
            ((BdaInt32U) bda).setValue(container.getValue().asLong());
            break;
        case INT64:
            ((BdaInt64) bda).setValue(container.getValue().asLong());
            break;
        default:
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    @Override
    public void disconnect() {
        clientAssociation.close();
    }

}
