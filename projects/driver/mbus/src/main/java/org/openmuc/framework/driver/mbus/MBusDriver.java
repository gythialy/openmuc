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
package org.openmuc.framework.driver.mbus;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.*;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.jmbus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class MBusDriver implements DriverService {
    private final static Logger logger = LoggerFactory.getLogger(MBusDriver.class);

    private final Map<String, ConnectionHandle> connections = new HashMap<String, ConnectionHandle>();

    private final static DriverInfo info = new DriverInfo("mbus",
                                                          // id
                                                          // description
                                                          "M-Bus (wired) is a protocol to read out meters.",
                                                          // interface address
                                                          "Synopsis: <serial_port>\n(e.g. /dev/ttyS0 (Unix), COM1 (Windows)",
                                                          // device address
                                                          "Synopsis: <mbus_address>\nExample for <mbus_address>: p5 for primary address 5",
                                                          // parameters
                                                          "Synopsis: [<baud_rate>]\nThe default baud rate is 2400",
                                                          // channel address
                                                          "Synopsis: <dib>:<vib>",
                                                          // device scan parameters
                                                          "Synopsis: <serial_port> [baud_rate]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)");

    private boolean interruptScan;

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {

        interruptScan = false;

        String[] args = settings.split("\\s+");
        if (args.length < 1 || args.length > 2) {
            throw new ArgumentSyntaxException(
                    "Less than one or more than two arguments in the settings are not allowed.");
        }

        int baudRate = 2400;
        if (args.length == 2) {
            try {
                baudRate = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("<braud_rate> is not an integer");
            }
        }

        MBusSap mBusSap;
        if (!connections.containsKey(args[0])) {
            mBusSap = new MBusSap(args[0]);
            try {
                mBusSap.open(baudRate);
            }
            catch (IllegalArgumentException e) {
                throw new ArgumentSyntaxException();
            }
            catch (IOException e) {
                throw new ScanException(e);
            }
        } else {
            mBusSap = connections.get(args[0]).getMBusSap();
        }

        mBusSap.setTimeout(1000);

        try {
            for (int i = 0; i <= 250; i++) {

                if (interruptScan) {
                    throw new ScanInterruptedException();
                }

                if (i % 5 == 0) {
                    listener.scanProgressUpdate(i * 100 / 250);
                }
                String primaryAddress = "p" + i;
                logger.debug("scanning for meter with primary address {}", i);
                try {
                    mBusSap.read(primaryAddress);
                }
                catch (TimeoutException e) {
                    continue;
                }
                catch (IOException e) {
                    throw new ScanException(e);
                }
                listener.deviceFound(new DeviceScanInfo(args[0], primaryAddress, "", ""));
                logger.debug("found meter: p{}", i);
            }
        }
        finally {
            mBusSap.close();
        }

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        interruptScan = true;

    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        ConnectionHandle conHandle = (ConnectionHandle) connection.getConnectionHandle();

        List<ChannelScanInfo> chanScanInf = new ArrayList<ChannelScanInfo>();

        try {
            List<VariableDataBlock> vdb = conHandle.getMBusSap().read(conHandle.getDeviceAddres())
                                                   .getVariableDataBlocks();
            for (VariableDataBlock block : vdb) {
                try {
                    block.decode();

                    String vib = bytesToHexString(block.getVIB());
                    String dib = bytesToHexString(block.getDIB());

                    ValueType valueType;
                    Integer valueLength;

                    switch (block.getDataValueType()) {
                    case DATE:
                        valueType = ValueType.BYTE_ARRAY;
                        valueLength = 250;
                        break;
                    case STRING:
                        valueType = ValueType.BYTE_ARRAY;
                        valueLength = 250;
                        break;
                    case LONG:
                        valueType = ValueType.LONG;
                        valueLength = null;
                        break;
                    case DOUBLE:
                        valueType = ValueType.DOUBLE;
                        valueLength = null;
                        break;
                    default:
                        valueType = ValueType.BYTE_ARRAY;
                        valueLength = 250;
                        break;
                    }

                    chanScanInf.add(new ChannelScanInfo(dib + ":" + vib,
                                                        block.getDescription().toString(),
                                                        valueType,
                                                        valueLength));
                }
                catch (DecodingException e) {
                    logger.debug("Skipped invalid or unsupported M-Bus VariableDataBlock:"
                                 + e.getMessage());
                }
            }
        }
        catch (IOException e) {
            throw new ConnectionException(e);
        }
        catch (TimeoutException e) {
            return null;
        }

        return chanScanInf;
    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        ConnectionHandle connectionHandle = connections.get(interfaceAddress);

        if (connectionHandle == null) {

            MBusSap mBusSap = new MBusSap(interfaceAddress);

            int baudrate = 2400;

            if (!settings.isEmpty()) {
                try {
                    baudrate = Integer.parseInt(settings);
                }
                catch (NumberFormatException e) {
                    throw new ArgumentSyntaxException("Settings: baudrate is not a parsable number");
                }
            }

            try {
                mBusSap.open(baudrate);
            }
            catch (IOException e1) {
                throw new ConnectionException("Unable to bind local interface: "
                                              + interfaceAddress);
            }

            try {
                mBusSap.read(deviceAddress);
            }
            catch (Exception e) {
                mBusSap.close();
                throw new ConnectionException(e);
            }

            connectionHandle = new ConnectionHandle(mBusSap, deviceAddress);
            connections.put(interfaceAddress, connectionHandle);

        } else {

            try {
                connectionHandle.getMBusSap().read(deviceAddress);
            }
            catch (Exception e) {
                throw new ConnectionException(e);
            }

            connectionHandle.increaseDeviceCounter();
        }

        return connectionHandle;
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        ConnectionHandle connectionHandle = (ConnectionHandle) connection.getConnectionHandle();
        if (!connectionHandle.isOpen()) {
            return;
        }
        connectionHandle.decreaseDeviceCounter();
        if (connectionHandle.getDeviceCounter() == 0) {
            connectionHandle.getMBusSap().close();
            connections.remove(connection.getInterfaceAddress());
        }
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        ConnectionHandle connectionHandle = (ConnectionHandle) connection.getConnectionHandle();
        if (!connectionHandle.isOpen()) {
            throw new ConnectionException();
        }

        MBusSap mBusSap = connectionHandle.getMBusSap();

        VariableDataResponse response;
        try {
            response = mBusSap.read(connection.getDeviceAddress());
        }
        catch (IOException e1) {
            connectionHandle.close();
            connectionHandle.getMBusSap().close();
            connections.remove(connection.getInterfaceAddress());
            throw new ConnectionException(e1);
        }
        catch (TimeoutException e1) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.TIMEOUT));
            }
            return null;
        }

        long timestamp = System.currentTimeMillis();

        List<VariableDataBlock> vdbs = response.getVariableDataBlocks();
        String[] dibvibs = new String[vdbs.size()];

        int i = 0;
        for (VariableDataBlock vdb : vdbs) {
            dibvibs[i++] = bytesToHexString(vdb.getDIB()) + ':' + bytesToHexString(vdb.getVIB());
        }

        for (ChannelRecordContainer container : containers) {

            i = 0;
            for (VariableDataBlock dataBlock : response.getVariableDataBlocks()) {

                if (dibvibs[i++].equalsIgnoreCase(container.getChannelAddress())) {

                    try {
                        dataBlock.decode();
                    }
                    catch (DecodingException e) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
                        logger.debug("Unable to parse VariableDataBlock received via M-Bus", e);
                        break;
                    }

                    switch (dataBlock.getDataValueType()) {
                    case DATE:
                        container.setRecord(new Record(new StringValue(((Date) dataBlock.getDataValue())
                                                                               .toString()),
                                                       timestamp));
                        break;
                    case STRING:
                        container.setRecord(new Record(new StringValue((String) dataBlock.getDataValue()),
                                                       timestamp));
                        break;
                    case DOUBLE:
                        container.setRecord(new Record(new DoubleValue(dataBlock.getScaledDataValue()),
                                                       timestamp));
                        break;
                    case LONG:
                        if (dataBlock.getMultiplierExponent() == 0) {
                            container.setRecord(new Record(new LongValue((Long) dataBlock.getDataValue()),
                                                           timestamp));
                        } else {
                            container.setRecord(new Record(new DoubleValue(dataBlock.getScaledDataValue()),
                                                           timestamp));
                        }
                        break;
                    case BCD:
                        if (dataBlock.getMultiplierExponent() == 0) {
                            container.setRecord(new Record(new LongValue(((Bcd) dataBlock.getDataValue())
                                                                                 .longValue()),
                                                           timestamp));
                        } else {
                            container.setRecord(new Record(new DoubleValue(((Bcd) dataBlock.getDataValue())
                                                                                   .longValue()
                                                                           * Math.pow(10,
                                                                                      dataBlock.getMultiplierExponent())),
                                                           timestamp));
                        }
                        break;
                    }

                    break;

                }

            }

            if (container.getRecord() == null) {
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
            }

        }

        return null;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%1$02X", b));
        }
        return sb.toString();
    }

}
