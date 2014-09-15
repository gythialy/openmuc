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
package org.openmuc.driver.dlms;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.*;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.jdlms.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DlmsDriver implements DriverService {
    private final static Logger logger = LoggerFactory.getLogger(DlmsDriver.class);

    private final IClientConnectionFactory connectionFactory = new OsgiClientConnectionFactory();
    private final AddressParser addressParser = new AddressParser();

    private final static DriverInfo info = new DriverInfo("dlms",
                                                          "This is a driver to communicate with smart meter over the IEC 62056 DLMS/COSEM protocol",
                                                          "?",
                                                          "",
                                                          "?",
                                                          "?",
                                                          "?");

    private final static int timeout = 10000;

    public DlmsDriver() {
        logger.debug("DLMS Driver instantiated. Expecting rxtxserial.so in: " + System.getProperty(
                "java.library.path")
                     + " for serial (HDLC) connections.");
    }

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
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ConnectionException {

        SettingsHelper settingsHelper = new SettingsHelper(settings);

        IClientConnection connection;
        try {
            ClientConnectionSettings<?> params = addressParser.parse(interfaceAddress,
                                                                     deviceAddress,
                                                                     settingsHelper);
            connection = connectionFactory.createClientConnection(params);
        }
        catch (UnknownHostException uhEx) {
            throw new ConnectionException("Device " + deviceAddress + " not found");
        }
        catch (IOException ioEx) {
            throw new ConnectionException("Cannot create connection object. Reason: " + ioEx);
        }

        logger.debug("Connecting to interface :" + interfaceAddress + ", device:" + deviceAddress);
        try {
            connection.connect(timeout, settingsHelper.getPassword());
        }
        catch (IOException ex) {
            throw new ConnectionException(ex.getMessage());
        }
        logger.debug("Connected to device: " + deviceAddress);

        ConnectionHandle handle = new ConnectionHandle(connection, settingsHelper);

        return handle;
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        logger.debug("Disconnecting from device: " + connection.getDeviceAddress());
        ConnectionHandle handle = extractHandle(connection);
        handle.getConnection().disconnect(handle.getSettings().sendDisconnect());
        logger.debug("Disconnected from device: " + connection.getDeviceAddress());
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        ConnectionHandle handle = extractHandle(connection);
        IClientConnection clientConnection = handle.getConnection();

        if (!clientConnection.isConnected()) {
            throw new ConnectionException();
        }

        List<ChannelRecordContainer> writeList = new ArrayList<ChannelRecordContainer>(containers);
        Iterator<ChannelRecordContainer> iter = writeList.iterator();

        long timestamp = System.currentTimeMillis();
        while (iter.hasNext()) {
            ChannelRecordContainer c = iter.next();
            if (c.getChannelHandle() == null) {
                try {
                    GetRequest channelHandle = ChannelAddress.parse(c.getChannelAddress())
                                                             .createGetRequest();
                    c.setChannelHandle(channelHandle);
                }
                catch (IllegalArgumentException e) {
                    c.setRecord(new Record(null,
                                           timestamp,
                                           Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
                    iter.remove();
                }
            }
        }

        GetRequest[] getParams = new GetRequest[writeList.size()];
        int index = 0;
        for (ChannelRecordContainer c : writeList) {
            getParams[index++] = (GetRequest) c.getChannelHandle();
        }

        try {
            List<GetResult> results = null;
            if (handle.getSettings().forceSingle()) {
                synchronized (handle.getConnection()) {
                    results = new ArrayList<GetResult>(getParams.length);
                    for (GetRequest param : getParams) {
                        results.addAll(clientConnection.get(timeout, param));
                    }
                }
            } else {
                results = clientConnection.get(timeout, getParams);
            }
            timestamp = System.currentTimeMillis();

            index = 0;

            for (GetResult result : results) {
                Value resultValue = null;
                Flag resultFlag = Flag.VALID;
                if (result.isSuccess()) {
                    Data data = result.getResultData();
                    if (data.isBoolean()) {
                        resultValue = new BooleanValue(data.getBoolean());
                    } else if (data.isNumber()) {
                        resultValue = new DoubleValue(data.getNumber().doubleValue());
                    } else if (data.isCalendar()) {
                        resultValue = new LongValue(data.getCalendar().getTimeInMillis());
                    } else if (data.isByteArray()) {
                        resultValue = new ByteArrayValue(data.getByteArray());
                    }
                } else {
                    AccessResultCode code = result.getResultCode();
                    if (code == AccessResultCode.HARDWARE_FAULT) {
                        resultFlag = Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
                    } else if (code == AccessResultCode.TEMPORARY_FAILURE) {
                        resultFlag = Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE;
                    } else if (code == AccessResultCode.READ_WRITE_DENIED) {
                        resultFlag = Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
                    } else if (code == AccessResultCode.OBJECT_UNDEFINED) {
                        resultFlag = Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND;
                    } else if (code == AccessResultCode.OBJECT_UNAVAILABLE) {
                        resultFlag = Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
                    } else {
                        resultFlag = Flag.UNKNOWN_ERROR;
                    }
                }
                writeList.get(index++).setRecord(new Record(resultValue, timestamp, resultFlag));
            }
        }
        catch (IOException ex) {
            logger.error("Cannot read from device. Reason: " + ex);
            timestamp = System.currentTimeMillis();
            for (ChannelRecordContainer c : containers) {
                c.setRecord(new Record(null, timestamp, Flag.COMM_DEVICE_NOT_CONNECTED));
            }
            throw new ConnectionException(ex.getMessage());
        }

        return null;
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        // TODO Test with multiple channels simultaneously
        ConnectionHandle connectionHandle = extractHandle(connection);
        IClientConnection clientConnection = connectionHandle.getConnection();

        if (!clientConnection.isConnected()) {
            throw new ConnectionException();
        }

        int size = 0;
        List<WriteHandle> writeHandles = new ArrayList<WriteHandle>(containers.size());
        for (ChannelValueContainer container : containers) {
            WriteHandle handle = new WriteHandle(container);
            if (handle.createGetRequest() != null) {
                size++;
            }
            writeHandles.add(new WriteHandle(container));
        }

        GetRequest[] getParams = new GetRequest[size];
        int index = 0;
        for (WriteHandle handle : writeHandles) {
            GetRequest getRequest = handle.createGetRequest();
            if (getRequest != null) {
                getParams[index] = getRequest;
                handle.setReadIndex(index);
                index++;
            }
        }

        try {
            List<GetResult> getResults = null;
            if (connectionHandle.getSettings().forceSingle()) {
                synchronized (connectionHandle.getConnection()) {
                    getResults = new ArrayList<GetResult>(getParams.length);
                    for (GetRequest param : getParams) {
                        getResults.addAll(clientConnection.get(timeout, param));
                    }
                }
            } else {
                getResults = clientConnection.get(timeout, getParams);
            }

            size = 0;
            for (WriteHandle handle : writeHandles) {
                if (handle.getReadIndex() != -1) {
                    GetResult getResult = getResults.get(handle.getReadIndex());
                    handle.setGetResult(getResult);
                    if (handle.createSetRequest() != null) {
                        size++;
                    }
                }
            }

            index = 0;
            SetRequest[] setParams = new SetRequest[size];
            for (WriteHandle handle : writeHandles) {
                SetRequest setRequest = handle.createSetRequest();
                if (setRequest != null) {
                    setParams[index] = setRequest;
                    handle.setWriteIndex(index);
                    index++;
                }
            }

            List<AccessResultCode> setResults = null;
            if (connectionHandle.getSettings().forceSingle()) {
                synchronized (connectionHandle.getConnection()) {
                    setResults = new ArrayList<AccessResultCode>(setParams.length);
                    for (SetRequest param : setParams) {
                        setResults.addAll(clientConnection.set(timeout, param));
                    }
                }
            } else {
                setResults = clientConnection.set(timeout, setParams);
            }

            for (WriteHandle handle : writeHandles) {
                if (handle.getWriteIndex() != -1) {
                    handle.setSetResult(setResults.get(handle.getWriteIndex()));
                }
                handle.writeFlag();
            }

        }
        catch (IOException ex) {
            logger.error("Cannot write to device. Reason: " + ex);
            for (ChannelValueContainer c : containers) {
                c.setFlag(Flag.COMM_DEVICE_NOT_CONNECTED);
            }
        }

        return null;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        ConnectionHandle handle = extractHandle(connection);
        IClientConnection clientConnection = handle.getConnection();

        GetRequest scanChannels = new GetRequest(15, new ObisCode(0, 0, 40, 0, 0, 255), 2);
        GetResult getResult = null;
        try {
            if (handle.getSettings().forceSingle()) {
                synchronized (handle.getConnection()) {
                    getResult = clientConnection.get(20000, scanChannels).get(0);
                }
            } else {
                getResult = clientConnection.get(20000, scanChannels).get(0);
            }
        }
        catch (IOException ex) {
            logger.debug("Cannot scan device for channels. Reason: " + ex);
            throw new ConnectionException(ex);
        }

        if (!getResult.isSuccess()) {
            throw new ConnectionException("Device sent error code " + getResult.getResultCode()
                                                                               .name());
        }

        List<ChannelScanInfo> result = new LinkedList<ChannelScanInfo>();
        Data root = getResult.getResultData();
        List<Data> objectArray = root.getComplex();
        for (Data objectDef : objectArray) {
            List<Data> defItems = objectDef.getComplex();
            int classId = defItems.get(0).getNumber().intValue();
            byte[] logicalName = defItems.get(2).getByteArray();
            List<Data> attributes = defItems.get(3).getComplex().get(0).getComplex();
            for (Data attributeAccess : attributes) {
                int attributeId = attributeAccess.getComplex().get(0).getNumber().intValue();
                int accessRights = attributeAccess.getComplex().get(1).getNumber().intValue();
                boolean readable = accessRights % 2 == 1;
                boolean writable = accessRights >= 2;
                ChannelAddress channelAddress = new ChannelAddress(classId,
                                                                   logicalName,
                                                                   attributeId);
                result.add(new ChannelScanInfo(channelAddress.toString(),
                                               "",
                                               ValueType.DOUBLE,
                                               0,
                                               readable,
                                               writable));
            }
        }

        return result;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    private ConnectionHandle extractHandle(DeviceConnection deviceConnection) {
        return (ConnectionHandle) deviceConnection.getConnectionHandle();
    }
}
