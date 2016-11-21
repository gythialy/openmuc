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
package org.openmuc.framework.driver.mbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

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
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MBusConnection implements Connection {
    private final static Logger logger = LoggerFactory.getLogger(MBusConnection.class);

    private final MBusSerialInterface serialInterface;
    private final int mBusAddress;
    private final SecondaryAddress secondaryAddress;

    public MBusConnection(MBusSerialInterface serialInterface, int mBusAddress, SecondaryAddress secondaryAddress) {
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
                serialInterface.getMBusSap().linkReset(mBusAddress);
                VariableDataStructure variableDataStructure = serialInterface.getMBusSap().read(mBusAddress);

                List<DataRecord> dataRecords = variableDataStructure.getDataRecords();

                for (DataRecord dataRecord : dataRecords) {

                    String vib = HexConverter.toShortHexString(dataRecord.getVib());
                    String dib = HexConverter.toShortHexString(dataRecord.getDib());

                    ValueType valueType;
                    Integer valueLength;

                    switch (dataRecord.getDataValueType()) {
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
                    case NONE:
                    case BCD:
                    default:
                        valueType = ValueType.BYTE_ARRAY;
                        valueLength = 250;
                        break;
                    }

                    chanScanInf.add(new ChannelScanInfo(dib + ":" + vib, dataRecord.getDescription().toString(),
                            valueType, valueLength));
                }
            } catch (IOException e) {
                throw new ConnectionException(e);
            } catch (TimeoutException e) {
                return null;
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

            if (secondaryAddress != null) {
                try {
                    serialInterface.getMBusSap().selectComponent(secondaryAddress);
                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                } catch (TimeoutException e) {
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                    }
                    return null;
                }
            }

            VariableDataStructure variableDataStructure = null;
            try {
                variableDataStructure = serialInterface.getMBusSap().read(mBusAddress);
            } catch (IOException e1) {
                serialInterface.close();
                throw new ConnectionException(e1);
            } catch (TimeoutException e1) {
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                }
                return null;
            }

            long timestamp = System.currentTimeMillis();

            List<DataRecord> dataRecords = variableDataStructure.getDataRecords();
            String[] dibvibs = new String[dataRecords.size()];

            int i = 0;
            for (DataRecord dataRecord : dataRecords) {
                dibvibs[i++] = HexConverter.toShortHexString(dataRecord.getDib()) + ':'
                        + HexConverter.toShortHexString(dataRecord.getVib());
            }

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
                        serialInterface.getMBusSap().selectForReadout(mBusAddress, dataRecordsToSelectForReadout);
                    } catch (IOException e) {
                        serialInterface.close();
                        throw new ConnectionException(e);
                    } catch (TimeoutException e) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                        continue;
                    }

                    VariableDataStructure variableDataStructure2 = null;
                    try {
                        variableDataStructure2 = serialInterface.getMBusSap().read(mBusAddress);
                    } catch (IOException e1) {
                        serialInterface.close();
                        throw new ConnectionException(e1);
                    } catch (TimeoutException e1) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
                        continue;
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

            if (selectForReadoutSet) {
                try {
                    serialInterface.getMBusSap().resetReadout(mBusAddress);
                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                } catch (TimeoutException e) {
                    try {
                        serialInterface.getMBusSap().linkReset(mBusAddress);
                    } catch (IOException e1) {
                        serialInterface.close();
                        throw new ConnectionException(e1);
                    } catch (TimeoutException e1) {
                        serialInterface.close();
                        throw new ConnectionException(e1);
                    }
                }
            }

            return null;

        }
    }

    private void setContainersRecord(long timestamp, ChannelRecordContainer container, DataRecord dataRecord) {
        try {
            switch (dataRecord.getDataValueType()) {
            case DATE:
                container.setRecord(
                        new Record(new StringValue(((Date) dataRecord.getDataValue()).toString()), timestamp));
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

}
