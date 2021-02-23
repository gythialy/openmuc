/*
 * Copyright 2011-2021 Fraunhofer ISE
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beanit.iec61850bean.BasicDataAttribute;
import com.beanit.iec61850bean.BdaBitString;
import com.beanit.iec61850bean.BdaBoolean;
import com.beanit.iec61850bean.BdaCheck;
import com.beanit.iec61850bean.BdaDoubleBitPos;
import com.beanit.iec61850bean.BdaEntryTime;
import com.beanit.iec61850bean.BdaFloat32;
import com.beanit.iec61850bean.BdaFloat64;
import com.beanit.iec61850bean.BdaInt128;
import com.beanit.iec61850bean.BdaInt16;
import com.beanit.iec61850bean.BdaInt16U;
import com.beanit.iec61850bean.BdaInt32;
import com.beanit.iec61850bean.BdaInt32U;
import com.beanit.iec61850bean.BdaInt64;
import com.beanit.iec61850bean.BdaInt8;
import com.beanit.iec61850bean.BdaInt8U;
import com.beanit.iec61850bean.BdaOctetString;
import com.beanit.iec61850bean.BdaOptFlds;
import com.beanit.iec61850bean.BdaQuality;
import com.beanit.iec61850bean.BdaReasonForInclusion;
import com.beanit.iec61850bean.BdaTapCommand;
import com.beanit.iec61850bean.BdaTimestamp;
import com.beanit.iec61850bean.BdaTriggerConditions;
import com.beanit.iec61850bean.BdaUnicodeString;
import com.beanit.iec61850bean.BdaVisibleString;
import com.beanit.iec61850bean.ClientAssociation;
import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.FcModelNode;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServiceError;

public final class Iec61850Connection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(Iec61850Connection.class);

    private static final String STRING_SEPARATOR = ",";

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
            scanInfos.add(createScanInfo(bda));
        }
        return scanInfos;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        // Check if record container objects exist -> check if basic data attribute exists in server model for channel
        // adress
        // -> model node exists but is no BDA
        for (ChannelRecordContainer container : containers) {

            setChannelHandleWithFcModelNode(container);
        }

        if (!samplingGroup.isEmpty()) {

            return setRecordContainerWithSamplingGroup(containers, containerListHandle, samplingGroup);

        }
        // sampling group is empty
        else {

            for (ChannelRecordContainer container : containers) {

                setRecordContainer(container);
            }
            return null;
        }

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
                if (fcModelNode.getFc().toString().equals("CO")) {
                    logger.info("writing CO model node");
                    fcModelNode = (FcModelNode) fcModelNode.getParent().getParent();
                    clientAssociation.operate(fcModelNode);
                }
                else {
                    clientAssociation.setDataValues(fcModelNode);
                }
            } catch (ServiceError e) {
                logger.error("Error writing to channel: service error calling setDataValues on {}: {}",
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

    private FcModelNode setRecordContainer(ChannelRecordContainer container) throws ConnectionException {
        if (container.getChannelHandle() == null) {
            return null;
        }
        FcModelNode fcModelNode = (FcModelNode) container.getChannelHandle();
        try {
            clientAssociation.getDataValues(fcModelNode);
        } catch (ServiceError e) {
            logger.debug("Error reading channel: service error calling getDataValues on {}: {}",
                    container.getChannelAddress(), e);
            container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
            return fcModelNode;
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
        return null;
    }

    private Object setRecordContainerWithSamplingGroup(List<ChannelRecordContainer> containers,
            Object containerListHandle, String samplingGroup) throws ConnectionException {
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
            logger.debug("Error reading sampling group: service error calling getDataValues on {}: {}", samplingGroup,
                    e);
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

    private void setChannelHandleWithFcModelNode(ChannelRecordContainer container) {
        if (container.getChannelHandle() == null) {

            String[] args = container.getChannelAddress().split(":", 3);

            if (args.length != 2) {
                logger.debug("Wrong channel address syntax: {}", container.getChannelAddress());
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                return;
            }

            ModelNode modelNode = serverModel.findModelNode(args[0], Fc.fromString(args[1]));

            if (modelNode == null) {
                logger.debug("No Basic Data Attribute for the channel address {} was found in the server model.",
                        container.getChannelAddress());
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                return;
            }

            FcModelNode fcModelNode;
            try {
                fcModelNode = (FcModelNode) modelNode;
            } catch (ClassCastException e) {
                logger.debug(
                        "ModelNode with object reference {} was found in the server model but is not a Basic Data Attribute.",
                        container.getChannelAddress());
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                return;
            }
            container.setChannelHandle(fcModelNode);

        }
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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

    public enum BdaTypes {
        BOOLEAN {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BOOLEAN, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaBoolean) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new BooleanValue(((BdaBoolean) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBoolean) bda).setValue(Boolean.parseBoolean(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBoolean) bda).setValue(container.getValue().asBoolean());
            }
        },
        INT8 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaInt8) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt8) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt8) bda).setValue(Byte.parseByte(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt8) bda).setValue(container.getValue().asByte());
            }
        },
        INT16 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.SHORT, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return "" + ((BdaInt16) bda).getValue();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt16) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt16) bda).setValue(Short.parseShort(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt16) bda).setValue(container.getValue().asShort());
            }
        },
        INT32 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.INTEGER, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaInt32) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt32) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt32) bda).setValue(Integer.parseInt(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt32) bda).setValue(container.getValue().asInt());
            }
        },
        INT64 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.LONG, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return "" + ((BdaInt64) bda).getValue();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt64) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt64) bda).setValue(Long.parseLong(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt64) bda).setValue(container.getValue().asLong());
            }
        },
        INT128 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return null;
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaInt128) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt128) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt128) bda).setValue(Long.parseLong(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt128) bda).setValue(container.getValue().asLong());
            }
        },
        INT8U {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.SHORT, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return "" + ((BdaInt8U) bda).getValue();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt8U) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt8U) bda).setValue(Short.parseShort(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt8U) bda).setValue(container.getValue().asShort());
            }
        },
        INT16U {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.INTEGER, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return "" + ((BdaInt16U) bda).getValue();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt16U) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt16U) bda).setValue(Integer.parseInt(bdaValueString));
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt16U) bda).setValue(container.getValue().asInt());
            }
        },
        INT32U {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.LONG, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaInt32U) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaInt32U) bda).getValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaInt32U) bda).setValue(Long.parseLong(bdaValueString));

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaInt32U) bda).setValue(container.getValue().asLong());
            }
        },
        FLOAT32 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.FLOAT, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaFloat32) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new FloatValue(((BdaFloat32) bda).getFloat()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaFloat32) bda).setFloat(Float.parseFloat(bdaValueString));
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaFloat32) bda).setFloat(container.getValue().asFloat());
            }
        },
        FLOAT64 {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.DOUBLE, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaFloat64) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new DoubleValue(((BdaFloat64) bda).getDouble()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaFloat64) bda).setDouble(Double.parseDouble(bdaValueString));
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaFloat64) bda).setDouble(container.getValue().asDouble());
            }
        },
        OCTET_STRING {

            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaOctetString) bda).getMaxLength());
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return Arrays.toString(((BdaOctetString) bda).getValue());
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaOctetString) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaOctetString) bda).setValue(bdaValueString.getBytes());

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaOctetString) bda).setValue(container.getValue().asByteArray());
            }
        },
        VISIBLE_STRING {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaVisibleString) bda).getMaxLength());
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaVisibleString) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new StringValue(((BdaVisibleString) bda).getStringValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaVisibleString) bda).setValue(bdaValueString);

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaVisibleString) bda).setValue(container.getValue().asString());
            }
        },
        UNICODE_STRING {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                // TODO Auto- method stub
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaUnicodeString) bda).getMaxLength());
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                byte[] byteValue = ((BdaUnicodeString) bda).getValue();
                if (byteValue == null) {
                    return "null";
                }
                else {
                    return new String(byteValue);
                }
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaUnicodeString) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaUnicodeString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaUnicodeString) bda).setValue(container.getValue().asByteArray());
            }
        },
        TIMESTAMP {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.LONG, null);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                Instant date = ((BdaTimestamp) bda).getInstant();
                return date == null ? "<invalid date>" : ("" + date.toEpochMilli());
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                Instant date = ((BdaTimestamp) bda).getInstant();

                if (date == null) {
                    return new Record(new LongValue(-1l), receiveTime);
                }
                else {
                    return new Record(new LongValue(date.toEpochMilli()), receiveTime);
                }
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaTimestamp) bda).setInstant(Instant.ofEpochMilli(Long.parseLong(bdaValueString)));
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaTimestamp) bda).setInstant(Instant.ofEpochMilli(container.getValue().asLong()));
            }
        },
        ENTRY_TIME {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaEntryTime) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaEntryTime) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaEntryTime) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaEntryTime) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaEntryTime) bda).setValue(container.getValue().asByteArray());
            }
        },
        CHECK {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaCheck) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaCheck) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());

            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        QUALITY {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaQuality) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaQuality) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        DOUBLE_BIT_POS {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaDoubleBitPos) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaDoubleBitPos) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        TAP_COMMAND {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return "" + ((BdaTapCommand) bda).getTapCommand().getIntValue();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new IntValue(((BdaTapCommand) bda).getTapCommand().getIntValue()), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        TRIGGER_CONDITIONS {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaTriggerConditions) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaBitString) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        OPTFLDS {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaOptFlds) bda).toString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaOptFlds) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        },
        REASON_FOR_INCLUSION {
            @Override
            public ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda) {
                return new ChannelScanInfo(channelAddress, "", ValueType.BYTE_ARRAY,
                        ((BdaBitString) bda).getValue().length);
            }

            @Override
            public String bda2String(BasicDataAttribute bda) {
                return ((BdaReasonForInclusion) bda).getValueString();
            }

            @Override
            public Record setRecord(BasicDataAttribute bda, long receiveTime) {
                return new Record(new ByteArrayValue(((BdaReasonForInclusion) bda).getValue(), true), receiveTime);
            }

            @Override
            public void setBda(String bdaValueString, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(bdaValueString.getBytes());
            }

            @Override
            public void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
                ((BdaBitString) bda).setValue(container.getValue().asByteArray());
            }
        };
        public abstract ChannelScanInfo getScanInfo(String channelAddress, BasicDataAttribute bda);

        public abstract String bda2String(BasicDataAttribute bda);

        public abstract Record setRecord(BasicDataAttribute bda, long receiveTime);

        public abstract void setBda(String bdaValueString, BasicDataAttribute bda);

        public abstract void setBda(ChannelValueContainer container, BasicDataAttribute bda);

    }

    public ChannelScanInfo createScanInfo(BasicDataAttribute bda) {
        try {
            return BdaTypes.valueOf(bda.getBasicType().toString())
                    .getScanInfo(bda.getReference() + ":" + bda.getFc(), bda);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());

        }
    }

    public String bda2String(BasicDataAttribute bda) {
        try {
            return BdaTypes.valueOf(bda.getBasicType().toString()).bda2String(bda);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    private void setRecord(ChannelRecordContainer container, BasicDataAttribute bda, long receiveTime) {
        try {
            container.setRecord(BdaTypes.valueOf(bda.getBasicType().toString()).setRecord(bda, receiveTime));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    private void setRecord(ChannelRecordContainer container, String stringValue, long receiveTime) {
        container.setRecord(new Record(new ByteArrayValue(stringValue.getBytes(), true), receiveTime));
    }

    private void setBda(String bdaValueString, BasicDataAttribute bda) {
        try {
            BdaTypes.valueOf(bda.getBasicType().toString()).setBda(bdaValueString, bda);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    private void setBda(ChannelValueContainer container, BasicDataAttribute bda) {
        try {
            BdaTypes.valueOf(bda.getBasicType().toString()).setBda(container, bda);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    @Override
    public void disconnect() {
        clientAssociation.disconnect();
    }

}
