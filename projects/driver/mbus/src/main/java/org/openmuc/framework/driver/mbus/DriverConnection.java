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
package org.openmuc.framework.driver.mbus;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jmbus.Bcd;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DataRecord.DataValueType;
import org.openmuc.jmbus.DataRecord.Description;
import org.openmuc.jmbus.DataRecord.FunctionField;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverConnection implements Connection {
    private static final Logger logger = LoggerFactory.getLogger(DriverConnection.class);

    private final ConnectionInterface connectionInterface;
    private final int mBusAddress;
    private final SecondaryAddress secondaryAddress;
    private final int delay;

    private boolean resetApplication = false;
    private boolean resetLink = false;

    public DriverConnection(ConnectionInterface connectionInterface, int mBusAddress, SecondaryAddress secondaryAddress,
            int delay) {
        this.connectionInterface = connectionInterface;
        this.secondaryAddress = secondaryAddress;
        this.mBusAddress = mBusAddress;
        this.delay = delay;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        int scanDelay = 50 + this.delay;

        synchronized (connectionInterface) {

            List<ChannelScanInfo> channelScanInfo = new ArrayList<>();
            try {
                MBusConnection mBusConnection = connectionInterface.getMBusConnection();

                if (secondaryAddress != null) {
                    mBusConnection.selectComponent(secondaryAddress);
                }
                else {
                    mBusConnection.linkReset(mBusAddress);
                    sleep(delay);
                    mBusConnection.resetReadout(mBusAddress);
                }

                VariableDataStructure variableDataStructure;
                do {
                    sleep(scanDelay);
                    variableDataStructure = mBusConnection.read(mBusAddress);
                    List<DataRecord> dataRecords = variableDataStructure.getDataRecords();
                    for (DataRecord dataRecord : dataRecords) {
                        fillDataRecordInChannelScanInfo(channelScanInfo, dataRecord);
                    }
                } while (variableDataStructure.moreRecordsFollow());
            } catch (IOException e) {
                throw new ConnectionException(e);
            }
            return channelScanInfo;
        }
    }

    private void fillDataRecordInChannelScanInfo(List<ChannelScanInfo> channelScanInfo, DataRecord dataRecord) {
        String vib = Helper.bytesToHex(dataRecord.getVib());
        String dib = Helper.bytesToHex(dataRecord.getDib());

        ValueType valueType;
        Integer valueLength;

        switch (dataRecord.getDataValueType()) {

        case STRING:
            valueType = ValueType.STRING;
            valueLength = 25;
            break;
        case LONG:
            if (dataRecord.getMultiplierExponent() == 0) {
                valueType = ValueType.LONG;
            }
            else {
                valueType = ValueType.DOUBLE;
            }
            valueLength = null;
            break;
        case DOUBLE:
        case DATE:
            valueType = ValueType.DOUBLE;
            valueLength = null;
            break;
        case BCD:
            if (dataRecord.getMultiplierExponent() == 0) {
                valueType = ValueType.DOUBLE;
            }
            else {
                valueType = ValueType.LONG;
            }
            valueLength = null;
            break;
        case NONE:
        default:
            valueType = ValueType.BYTE_ARRAY;
            valueLength = 100;
            break;
        }

        String unit = "";
        if (dataRecord.getUnit() != null) {
            unit = dataRecord.getUnit().getUnit();
        }

        channelScanInfo.add(new ChannelScanInfo(dib + ':' + vib, getDescription(dataRecord), valueType, valueLength,
                true, true, "", unit));
    }

    private String getDescription(DataRecord dataRecord) {
        DataValueType dataValueType = dataRecord.getDataValueType();
        Double scaledDataValue = dataRecord.getScaledDataValue();

        Description description = dataRecord.getDescription();
        FunctionField functionField = dataRecord.getFunctionField();
        int tariff = dataRecord.getTariff();

        short subunit = dataRecord.getSubunit();
        String userDefinedDescription = dataRecord.getUserDefinedDescription();
        long storageNumber = dataRecord.getStorageNumber();
        int multiplierExponent = dataRecord.getMultiplierExponent();
        Object dataValue = dataRecord.getDataValue();

        StringBuilder builder = new StringBuilder().append("Descr:").append(description);

        if (description == Description.USER_DEFINED) {
            builder.append(':').append(userDefinedDescription);
        }
        builder.append(";Function:").append(functionField);

        if (storageNumber > 0) {
            builder.append(";Storage:").append(storageNumber);
        }

        if (tariff > 0) {
            builder.append(";Tariff:").append(tariff);
        }

        if (subunit > 0) {
            builder.append(";Subunit:").append(subunit);
        }

        final String valuePlacHolder = ";Value:";
        final String scaledValueString = ";ScaledValue:";

        switch (dataValueType) {
        case DATE:
        case STRING:
            builder.append(valuePlacHolder).append((dataValue).toString());
            break;
        case DOUBLE:
            builder.append(scaledValueString).append(scaledDataValue);
            break;
        case LONG:
            if (multiplierExponent == 0) {
                builder.append(valuePlacHolder).append(dataValue);
            }
            else {
                builder.append(scaledValueString).append(scaledDataValue);
            }
            break;
        case BCD:
            if (multiplierExponent == 0) {
                builder.append(valuePlacHolder).append((dataValue).toString());
            }
            else {
                builder.append(scaledValueString).append(scaledDataValue);
            }
            break;
        case NONE:
            builder.append(";value:NONE");
            break;
        }

        return builder.toString();
    }

    @Override
    public void disconnect() {

        synchronized (connectionInterface) {

            if (!connectionInterface.isOpen()) {
                return;
            }

            connectionInterface.decreaseConnectionCounter();
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws ConnectionException {
        synchronized (connectionInterface) {
            List<DataRecord> dataRecords = new ArrayList<>();

            if (!connectionInterface.isOpen()) {
                throw new ConnectionException(
                        "Connection " + connectionInterface.getInterfaceAddress() + " is closed.");
            }

            MBusConnection mBusConnection = connectionInterface.getMBusConnection();
            if (secondaryAddress != null) {
                try {
                    mBusConnection.selectComponent(secondaryAddress);
                    sleep(delay);
                } catch (IOException e) {
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_UNSPECIFIED));
                    }
                    connectionInterface.close();
                    logger.error(e.getMessage());
                    throw new ConnectionException(e);
                }
            }

            try {
                if (secondaryAddress == null) {
                    if (resetLink) {
                        mBusConnection.linkReset(mBusAddress);
                        sleep(delay);
                    }
                    if (resetApplication) {
                        mBusConnection.resetReadout(mBusAddress);
                        sleep(delay);
                    }
                }
                VariableDataStructure variableDataStructure = null;
                do {
                    variableDataStructure = mBusConnection.read(mBusAddress);
                    sleep(delay);
                    dataRecords.addAll(variableDataStructure.getDataRecords());
                } while (variableDataStructure.moreRecordsFollow());

            } catch (IOException e) {
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_UNSPECIFIED));
                }
                connectionInterface.close();
                logger.error(e.getMessage());
                throw new ConnectionException(e);
            }

            long timestamp = System.currentTimeMillis();

            String[] dibvibs = new String[dataRecords.size()];

            setDibVibs(dataRecords, dibvibs);

            boolean selectForReadoutSet = setRecords(containers, mBusConnection, timestamp, dataRecords, dibvibs);

            if (selectForReadoutSet) {
                try {
                    mBusConnection.resetReadout(mBusAddress);
                    sleep(delay);
                } catch (IOException e) {
                    try {
                        mBusConnection.linkReset(mBusAddress);
                        sleep(delay);
                    } catch (IOException e1) {
                        for (ChannelRecordContainer container : containers) {
                            container.setRecord(new Record(Flag.CONNECTION_EXCEPTION));
                        }
                        connectionInterface.close();
                        logger.error("{}\n{}", e.getMessage(), e1.getMessage());
                        throw new ConnectionException(e);
                    }
                }
            }
            return null;
        }

    }

    private void setDibVibs(List<DataRecord> dataRecords, String[] dibvibs) {
        int i = 0;
        for (DataRecord dataRecord : dataRecords) {
            String dibHex = Helper.bytesToHex(dataRecord.getDib());
            String vibHex = Helper.bytesToHex(dataRecord.getVib());
            dibvibs[i++] = MessageFormat.format("{0}:{1}", dibHex, vibHex);
        }
    }

    private boolean setRecords(List<ChannelRecordContainer> containers, MBusConnection mBusConnection, long timestamp,
            List<DataRecord> dataRecords, String[] dibvibs) throws ConnectionException {
        boolean selectForReadoutSet = false;

        for (ChannelRecordContainer container : containers) {

            String channelAddress = container.getChannelAddress();

            if (channelAddress.startsWith("X")) {
                String[] dibAndVib = channelAddress.split(":");
                if (dibAndVib.length != 2) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
                }
                List<DataRecord> dataRecordsToSelectForReadout = new ArrayList<>(1);

                selectForReadoutSet = true;

                try {
                    mBusConnection.selectForReadout(mBusAddress, dataRecordsToSelectForReadout);
                    sleep(delay);
                } catch (SerialPortTimeoutException e) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    continue;
                } catch (IOException e) {
                    connectionInterface.close();
                    throw new ConnectionException(e);
                }

                VariableDataStructure variableDataStructure2 = null;
                try {
                    variableDataStructure2 = mBusConnection.read(mBusAddress);
                } catch (SerialPortTimeoutException e1) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    continue;
                } catch (IOException e1) {
                    connectionInterface.close();
                    throw new ConnectionException(e1);
                }

                DataRecord dataRecord = variableDataStructure2.getDataRecords().get(0);

                setContainersRecord(timestamp, container, dataRecord);

                continue;
            }

            int j = 0;
            for (DataRecord dataRecord : dataRecords) {
                if (dibvibs[j++].equalsIgnoreCase(channelAddress)) {
                    setContainersRecord(timestamp, container, dataRecord);
                    break;
                }
            }

            if (container.getRecord() == null) {
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
            }

        }
        return selectForReadoutSet;
    }

    private void setContainersRecord(long timestamp, ChannelRecordContainer container, DataRecord dataRecord) {
        try {
            switch (dataRecord.getDataValueType()) {
            case DATE:
                container.setRecord(
                        new Record(new DoubleValue(((Date) dataRecord.getDataValue()).getTime()), timestamp));
                break;
            case STRING:
                container.setRecord(new Record(new StringValue((String) dataRecord.getDataValue()), timestamp));
                break;
            case DOUBLE:
                container.setRecord(new Record(new DoubleValue(dataRecord.getScaledDataValue()), timestamp));
                break;
            case LONG:
                if (dataRecord.getMultiplierExponent() == 0) {
                    container.setRecord(new Record(new LongValue((Long) dataRecord.getDataValue()), timestamp));
                }
                else {
                    container.setRecord(new Record(new DoubleValue(dataRecord.getScaledDataValue()), timestamp));
                }
                break;
            case BCD:
                if (dataRecord.getMultiplierExponent() == 0) {
                    container.setRecord(
                            new Record(new LongValue(((Bcd) dataRecord.getDataValue()).longValue()), timestamp));
                }
                else {
                    container.setRecord(new Record(new DoubleValue(((Bcd) dataRecord.getDataValue()).longValue()
                            * Math.pow(10, dataRecord.getMultiplierExponent())), timestamp));
                }
                break;
            case NONE:
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION));
                if (logger.isWarnEnabled()) {
                    logger.warn("Received data record with <dib>:<vib> = {}  has value type NONE.",
                            container.getChannelAddress());
                }
                break;
            }
        } catch (IllegalStateException e) {
            container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION));
            logger.error("Received data record with <dib>:<vib> = {} has wrong value type. ErrorMsg: {}",
                    container.getChannelAddress(), e.getMessage());
        }
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    void setResetApplication(boolean resetApplication) {
        this.resetApplication = resetApplication;

    }

    void setResetLink(boolean resetLink) {
        this.resetLink = resetLink;
    }

    private void sleep(long millisec) throws ConnectionException {
        if (millisec > 0) {
            try {
                Thread.sleep(millisec);
            } catch (InterruptedException e) {
                throw new ConnectionException(e);
            }
        }
    }

}
