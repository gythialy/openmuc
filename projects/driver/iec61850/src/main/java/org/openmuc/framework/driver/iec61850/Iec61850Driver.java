/*
 * Copyright 2011-14 Fraunhofer ISE
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

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.*;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.openiec61850.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Iec61850Driver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(Iec61850Driver.class);

    private final static String STRING_SEPARATOR = ",";

    private final static DriverInfo info = new DriverInfo("iec61850",
                                                          // id
                                                          // description
                                                          "This driver can be used to access IEC 61850 MMS devices",
                                                          // interface address
                                                          "N.A.",
                                                          // device address
                                                          "Synopsis: <host>[:<port>]\nThe default port is 102.",
                                                          // parameters
                                                          "Synopsis: [-a <authentication_parameter>] [-lt <local_t-selector>] [-rt <remote_t-selector>]",
                                                          // channel address
                                                          "Synopsis: <bda_reference>:<fc>",
                                                          // device scan parameters
                                                          "N.A.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        Iec61850Connection connectionHandle = (Iec61850Connection) connection.getConnectionHandle();
        ServerModel serverModel = connectionHandle.getServerModel();
        List<BasicDataAttribute> bdas = serverModel.getBasicDataAttributes();

        List<ChannelScanInfo> scanInfos = new ArrayList<ChannelScanInfo>(bdas.size());

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
                scanInfos.add(new ChannelScanInfo(channelAddress,
                                                  "",
                                                  ValueType.BYTE_ARRAY,
                                                  ((BdaBitString) bda)
                                                          .getValue().length));
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
                throw new IllegalStateException("unknown BasicType received: "
                                                + bda.getBasicType());
            }

        }

        return scanInfos;

    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        String[] deviceAddresses = deviceAddress.split(":");

        if (deviceAddresses.length < 1 || deviceAddresses.length > 2) {
            throw new ArgumentSyntaxException("Invalid device address syntax.");
        }

        String remoteHost = deviceAddresses[0];
        InetAddress address;
        try {
            address = InetAddress.getByName(remoteHost);
        }
        catch (UnknownHostException e) {
            throw new ConnectionException("Unknown host: " + remoteHost, e);
        }

        int remotePort = 102;
        if (deviceAddresses.length == 2) {
            try {
                remotePort = Integer.parseInt(deviceAddresses[1]);
            }
            catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("The specified port is not an integer");
            }
        }

        ClientSap clientSap = new ClientSap();

        String authentication = null;

        if (!settings.isEmpty()) {
            String[] args = settings.split("\\s+", 0);
            if (args.length > 6) {
                throw new ArgumentSyntaxException(
                        "Less than one or more than four arguments in the settings are not allowed.");
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-a")) {
                    i++;
                    if (i == args.length) {
                        throw new ArgumentSyntaxException(
                                "No authentication parameter was specified after the -a parameter");
                    }
                    authentication = args[i];
                } else if (args[i].equals("-lt")) {

                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        clientSap.setTSelLocal(new byte[0]);
                    } else {
                        i++;
                        byte[] tSelLocal = new byte[args[i].length()];
                        for (int j = 0; j < args[i].length(); j++) {
                            tSelLocal[j] = (byte) args[i].charAt(j);
                        }
                        clientSap.setTSelLocal(tSelLocal);
                    }
                } else if (args[i].equals("-rt")) {

                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        clientSap.setTSelRemote(new byte[0]);
                    } else {
                        i++;
                        byte[] tSelRemote = new byte[args[i].length()];
                        for (int j = 0; j < args[i].length(); j++) {
                            tSelRemote[j] = (byte) args[i].charAt(j);
                        }
                        clientSap.setTSelRemote(tSelRemote);
                    }
                } else {
                    throw new ArgumentSyntaxException("Unexpected argument: " + args[i]);
                }
            }
        }

        ClientAssociation clientAssociation;
        try {
            clientAssociation = clientSap.associate(address, remotePort, authentication, null);
        }
        catch (IOException e) {
            throw new ConnectionException(e);
        }

        ServerModel serverModel;
        try {
            serverModel = clientAssociation.retrieveModel();
        }
        catch (ServiceError e) {
            clientAssociation.close();
            throw new ConnectionException("Service error retrieving server model" + e.getMessage(),
                                          e);
        }
        catch (IOException e) {
            clientAssociation.close();
            throw new ConnectionException("IOException retrieving server model: " + e.getMessage(),
                                          e);
        }

        return new Iec61850Connection(clientAssociation, serverModel);
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        ((Iec61850Connection) connection.getConnectionHandle()).getClientAssociation().close();
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        Iec61850Connection connectionHandle = (Iec61850Connection) connection.getConnectionHandle();
        ServerModel serverModel = connectionHandle.getServerModel();

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
                    logger.debug(
                            "No Basic Data Attribute for the channel address {} was found in the server model.",
                            container.getChannelAddress());
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                    continue;
                }

                FcModelNode fcModelNode;
                try {
                    fcModelNode = (FcModelNode) modelNode;
                }
                catch (ClassCastException e) {
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
            } else {

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
                }
                catch (ClassCastException e) {
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
                connectionHandle.getClientAssociation().getDataValues(fcModelNode);
            }
            catch (ServiceError e) {
                logger.debug(
                        "Error reading sampling group: service error calling getDataValues on {}: {}",
                        samplingGroup,
                        e);
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_ACCESSIBLE));
                }
                return fcModelNode;
            }
            catch (IOException e) {
                throw new ConnectionException(e);
            }

            long receiveTime = System.currentTimeMillis();

            for (ChannelRecordContainer container : containers) {
                if (container.getChannelHandle() != null) {
                    setRecord(container,
                              (BasicDataAttribute) container.getChannelHandle(),
                              receiveTime);
                } else {
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
                    connectionHandle.getClientAssociation().getDataValues(fcModelNode);
                }
                catch (ServiceError e) {
                    logger.debug(
                            "Error reading channel: service error calling getDataValues on {}: {}",
                            container.getChannelAddress(),
                            e);
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
                    continue;
                }
                catch (IOException e) {
                    throw new ConnectionException(e);
                }

                if (fcModelNode instanceof BasicDataAttribute) {
                    long receiveTime = System.currentTimeMillis();
                    setRecord(container, (BasicDataAttribute) fcModelNode, receiveTime);
                } else {
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
            result = date == null ? "<invalid date>" : date.toString();
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
        container.setRecord(new Record(new ByteArrayValue(stringValue.getBytes(), true),
                                       receiveTime));
    }

    private void setRecord(ChannelRecordContainer container,
                           BasicDataAttribute bda,
                           long receiveTime) {
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
            container.setRecord(new Record(new ByteArrayValue(((BdaBitString) bda).getValue(),
                                                              true), receiveTime));
            break;
        case TIMESTAMP:
            Date date = ((BdaTimestamp) bda).getDate();
            if (date == null) {
                container.setRecord(new Record(new LongValue(-1l), receiveTime));
            } else {
                container.setRecord(new Record(new LongValue(date.getTime()), receiveTime));
            }
            break;
        case BOOLEAN:
            container.setRecord(new Record(new BooleanValue(((BdaBoolean) bda).getValue()),
                                           receiveTime));
            break;
        case FLOAT32:
            container.setRecord(new Record(new FloatValue(((BdaFloat32) bda).getFloat()),
                                           receiveTime));
            break;
        case FLOAT64:
            container.setRecord(new Record(new DoubleValue(((BdaFloat64) bda).getDouble()),
                                           receiveTime));
            break;
        case INT8:
            container.setRecord(new Record(new DoubleValue(((BdaInt8) bda).getValue()),
                                           receiveTime));
            break;
        case INT8U:
            container.setRecord(new Record(new DoubleValue(((BdaInt8U) bda).getValue()),
                                           receiveTime));
            break;
        case INT16:
            container.setRecord(new Record(new DoubleValue(((BdaInt16) bda).getValue()),
                                           receiveTime));
            break;
        case INT16U:
            container.setRecord(new Record(new DoubleValue(((BdaInt16U) bda).getValue()),
                                           receiveTime));
            break;
        case INT32:
            container.setRecord(new Record(new DoubleValue(((BdaInt32) bda).getValue()),
                                           receiveTime));
            break;
        case INT32U:
            container.setRecord(new Record(new DoubleValue(((BdaInt32U) bda).getValue()),
                                           receiveTime));
            break;
        case INT64:
            container.setRecord(new Record(new DoubleValue(((BdaInt64) bda).getValue()),
                                           receiveTime));
            break;
        default:
            throw new IllegalStateException("unknown BasicType received: " + bda.getBasicType());
        }
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        Iec61850Connection connectionHandle = (Iec61850Connection) connection.getConnectionHandle();
        ServerModel serverModel = connectionHandle.getServerModel();

        List<FcModelNode> modelNodesToBeWritten = new ArrayList<FcModelNode>(containers.size());
        for (ChannelValueContainer container : containers) {

            if (container.getChannelHandle() != null) {
                modelNodesToBeWritten.add((FcModelNode) container.getChannelHandle());
                setFcModelNode(container, (FcModelNode) container.getChannelHandle());
            } else {

                String[] args = container.getChannelAddress().split(":", 3);

                if (args.length != 2) {
                    logger.debug("Wrong channel address syntax: {}", container.getChannelAddress());
                    container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
                    continue;
                }

                ModelNode modelNode = serverModel.findModelNode(args[0], Fc.fromString(args[1]));

                if (modelNode == null) {
                    logger.debug(
                            "No Basic Data Attribute for the channel address {} was found in the server model.",
                            container.getChannelAddress());
                    container.setFlag(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND);
                    continue;
                }

                FcModelNode fcModelNode;
                try {
                    fcModelNode = (FcModelNode) modelNode;

                }
                catch (ClassCastException e) {
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

        List<FcModelNode> fcNodesToBeRequested = new ArrayList<FcModelNode>();

        while (modelNodesToBeWritten.size() > 0) {
            fillRequestedNodes(fcNodesToBeRequested, modelNodesToBeWritten, serverModel);
        }

        for (FcModelNode fcModelNode : fcNodesToBeRequested) {
            try {
                connectionHandle.getClientAssociation().setDataValues(fcModelNode);
            }
            catch (ServiceError e) {
                logger.debug(
                        "Error writing to channel: service error calling setDataValues on {}: {}",
                        fcModelNode.getReference(),
                        e);
                for (BasicDataAttribute bda : fcModelNode.getBasicDataAttributes()) {
                    for (ChannelValueContainer valueContainer : containers) {
                        if (valueContainer.getChannelHandle() == bda) {
                            valueContainer.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
                        }
                    }
                }
                return null;
            }
            catch (IOException e) {
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

    void fillRequestedNodes(List<FcModelNode> fcNodesToBeRequested,
                            List<FcModelNode> remainingFcModelNodes,
                            ServerModel serverModel) {

        FcModelNode currentFcModelNode = remainingFcModelNodes.get(0);

        if (!checkParent(currentFcModelNode,
                         fcNodesToBeRequested,
                         remainingFcModelNodes,
                         serverModel)) {
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
            parentNode = parentNode.getChild(fcModelNode.getReference().get(i),
                                             fcModelNode.getFc());
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
        } else {
            List<BasicDataAttribute> bdas = fcModelNode.getBasicDataAttributes();
            String valueString = container.getValue().toString();
            String[] bdaValues = valueString.split(STRING_SEPARATOR);
            if (bdaValues.length != bdas.size()) {
                throw new IllegalStateException("attempt to write array "
                                                + valueString
                                                + " into fcModelNode "
                                                + fcModelNode.getName()
                                                + " failed as the dimensions don't fit.");
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
        case ENTRY_TIME:
        case OCTET_STRING:
        case VISIBLE_STRING:
        case UNICODE_STRING:
            ((BdaBitString) bda).setValue(bdaValueString.getBytes());
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
        case ENTRY_TIME:
        case OCTET_STRING:
        case VISIBLE_STRING:
        case UNICODE_STRING:
            ((BdaBitString) bda).setValue(container.getValue().asByteArray());
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

}
