/*
 * Copyright 2011-18 Fraunhofer ISE
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

    private final ConnectionInterface serialInterface;
    private final int mBusAddress;
    private final SecondaryAddress secondaryAddress;
    private final static int delay = 100; // delay in ms // ToDo: make it configurable (some devices need 2 s)

    private boolean resetApplication = false;
    private boolean resetLink = false;

    public DriverConnection(ConnectionInterface serialInterface, int mBusAddress, SecondaryAddress secondaryAddress) {
        this.serialInterface = serialInterface;
        this.secondaryAddress = secondaryAddress;
        this.mBusAddress = mBusAddress;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {

        synchronized (serialInterface) {

            List<ChannelScanInfo> chanScanInf = new ArrayList<>();

            try {
                MBusConnection mBusConnection = serialInterface.getMBusConnection();
                if (secondaryAddress != null) {
                    mBusConnection.selectComponent(secondaryAddress);
                }
                else {
                    mBusConnection.linkReset(mBusAddress);
                }
                sleep(delay);

                VariableDataStructure variableDataStructure = mBusConnection.read(mBusAddress);

                List<DataRecord> dataRecords = variableDataStructure.getDataRecords();

                for (DataRecord dataRecord : dataRecords) {

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

                    chanScanInf.add(new ChannelScanInfo(dib + ":" + vib, getDescription(dataRecord), valueType,
                            valueLength, true, true, "", unit));
                }
            } catch (SerialPortTimeoutException e) {
                throw new ConnectionException("Scan timeout.");
            } catch (IOException e) {
                throw new ConnectionException(e);
            }

            return chanScanInf;

        }
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

        synchronized (serialInterface) {

            if (!serialInterface.isOpen()) {
                return;
            }

            serialInterface.decreaseConnectionCounter();
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        synchronized (serialInterface) {

            if (!serialInterface.isOpen()) {
                throw new ConnectionException();
            }

            MBusConnection mBusConnection = serialInterface.getMBusConnection();
            if (secondaryAddress != null) {
                try {
                    mBusConnection.selectComponent(secondaryAddress);
                } catch (SerialPortTimeoutException e) {
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    }
                    return null;
                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                }
            }

            VariableDataStructure variableDataStructure = null;
            try {

                if (secondaryAddress == null) {
                    if (resetLink) {
                        mBusConnection.linkReset(mBusAddress);
                    }
                    if (resetApplication) {
                        mBusConnection.resetReadout(mBusAddress);
                    }
                }

                variableDataStructure = mBusConnection.read(mBusAddress);
            } catch (SerialPortTimeoutException e1) {
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                }
                return null;
            } catch (IOException e1) {
                serialInterface.close();
                throw new ConnectionException(e1);
            }

            long timestamp = System.currentTimeMillis();

            List<DataRecord> dataRecords = variableDataStructure.getDataRecords();
            String[] dibvibs = new String[dataRecords.size()];

            int i = 0;
            i = setDibVibs(dataRecords, dibvibs, i);

            boolean selectForReadoutSet = setRecords(containers, mBusConnection, timestamp, dataRecords, dibvibs, i);

            if (selectForReadoutSet) {
                try {
                    mBusConnection.resetReadout(mBusAddress);
                } catch (SerialPortTimeoutException e) {
                    try {
                        mBusConnection.linkReset(mBusAddress);
                    } catch (SerialPortTimeoutException e1) {
                        serialInterface.close();
                        throw new ConnectionException(e1);
                    } catch (IOException e1) {
                        serialInterface.close();
                        throw new ConnectionException(e1);
                    }
                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                }
            }
            return null;
        }
    }

    private int setDibVibs(List<DataRecord> dataRecords, String[] dibvibs, int i) {
        for (DataRecord dataRecord : dataRecords) {
            String dibHex = Helper.bytesToHex(dataRecord.getDib());
            String vibHex = Helper.bytesToHex(dataRecord.getVib());
            dibvibs[i++] = MessageFormat.format("{0}:{1}", dibHex, vibHex);
        }
        return i;
    }

    private boolean setRecords(List<ChannelRecordContainer> containers, MBusConnection mBusConnection, long timestamp,
            List<DataRecord> dataRecords, String[] dibvibs, int i) throws ConnectionException {
        boolean selectForReadoutSet = false;

        for (ChannelRecordContainer container : containers) {

            String channelAddress = container.getChannelAddress();

            if (channelAddress.startsWith("X")) {
                String[] dibAndVib = channelAddress.split(":");
                if (dibAndVib.length != 2) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
                }
                List<DataRecord> dataRecordsToSelectForReadout = new ArrayList<>(1);
                // TODO
                // dataRecordsToSelectForReadout
                // .add(new DataRecord(HexConverter.fromShortHexString(dibAndVib[0].substring(1)),
                // HexConverter.fromShortHexString(dibAndVib[1]), new byte[] {}, 0));

                selectForReadoutSet = true;

                try {
                    mBusConnection.selectForReadout(mBusAddress, dataRecordsToSelectForReadout);
                } catch (SerialPortTimeoutException e) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    continue;
                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                }

                VariableDataStructure variableDataStructure2 = null;
                try {
                    variableDataStructure2 = mBusConnection.read(mBusAddress);
                } catch (SerialPortTimeoutException e1) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    continue;
                } catch (IOException e1) {
                    serialInterface.close();
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
                    logger.warn("Received data record with <dib>:<vib> = " + container.getChannelAddress()
                            + " has value type NONE.");
                }
                break;
            }
        } catch (IllegalStateException e) {
            container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION));
            logger.error("Received data record with <dib>:<vib> = " + container.getChannelAddress()
                    + " has wrong value type. ", e);
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
        try {
            Thread.sleep(millisec);
        } catch (InterruptedException e) {
            throw new ConnectionException(e);
        }
    }

}
