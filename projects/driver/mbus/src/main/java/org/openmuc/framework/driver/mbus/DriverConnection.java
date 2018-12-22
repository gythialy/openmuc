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

import javax.xml.bind.DatatypeConverter;

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
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverConnection implements Connection {
    private static final Logger logger = LoggerFactory.getLogger(DriverConnection.class);

    private final SerialInterface serialInterface;
    private final int mBusAddress;
    private final SecondaryAddress secondaryAddress;

    private boolean resetApplication = false;
    private boolean resetLink = false;

    public DriverConnection(SerialInterface serialInterface, int mBusAddress, SecondaryAddress secondaryAddress) {
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
                mBusConnection.linkReset(mBusAddress);
                VariableDataStructure variableDataStructure = mBusConnection.read(mBusAddress);

                List<DataRecord> dataRecords = variableDataStructure.getDataRecords();

                for (DataRecord dataRecord : dataRecords) {

                    String vib = DatatypeConverter.printHexBinary(dataRecord.getVib());
                    String dib = DatatypeConverter.printHexBinary(dataRecord.getDib());

                    ValueType valueType;
                    Integer valueLength;

                    switch (dataRecord.getDataValueType()) {

                    case STRING:
                        valueType = ValueType.STRING;
                        valueLength = 25;
                        break;
                    case LONG:
                        valueType = ValueType.LONG;
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

                    chanScanInf.add(new ChannelScanInfo(dib + ":" + vib, dataRecord.getDescription().toString(),
                            valueType, valueLength));
                }
            } catch (SerialPortTimeoutException e) {
                return null;
            } catch (IOException e) {
                throw new ConnectionException(e);
            }

            return chanScanInf;

        }
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

                if (resetLink) {
                    mBusConnection.linkReset(mBusAddress);
                }
                if (resetApplication) {
                    mBusConnection.resetReadout(mBusAddress);
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
            String dibHex = DatatypeConverter.printHexBinary(dataRecord.getDib());
            String vibHex = DatatypeConverter.printHexBinary(dataRecord.getVib());
            dibvibs[i++] = MessageFormat.format("{0}:{1}", dibHex, vibHex);
        }
        return i;
    }

    private boolean setRecords(List<ChannelRecordContainer> containers, MBusConnection mBusConnection, long timestamp,
            List<DataRecord> dataRecords, String[] dibvibs, int i) throws ConnectionException {
        boolean selectForReadoutSet = false;

        for (ChannelRecordContainer container : containers) {

            if (container.getChannelAddress().startsWith("X")) {
                String[] dibAndVib = container.getChannelAddress().split(":");
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

            i = 0;
            for (DataRecord dataRecord : dataRecords) {
                if (dibvibs[i++].equalsIgnoreCase(container.getChannelAddress())) {
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

}
