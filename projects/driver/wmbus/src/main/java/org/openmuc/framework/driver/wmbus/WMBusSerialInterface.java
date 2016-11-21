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
package org.openmuc.framework.driver.wmbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jmbus.Bcd;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.WMBusListener;
import org.openmuc.jmbus.WMBusMessage;
import org.openmuc.jmbus.WMBusMode;
import org.openmuc.jmbus.WMBusSap;
import org.openmuc.jmbus.WMBusSapAmber;
import org.openmuc.jmbus.WMBusSapRadioCrafts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
public class WMBusSerialInterface {
    private final static Logger logger = LoggerFactory.getLogger(WMBusSerialInterface.class);

    private final static Map<String, WMBusSerialInterface> interfaces = new HashMap<>();
    private final HashMap<Integer, WMBusConnection> connectionsBySecondaryAddress = new HashMap<>();
    RecordsReceivedListener listener;

    private final WMBusSap wMBusSap;
    private final String serialPortName;
    private final String transceiverString;
    private final String modeString;

    public class Receiver implements WMBusListener {

        @Override
        public void discardedBytes(byte[] bytes) {
            if (logger.isDebugEnabled()) {
                logger.debug("received bytes that will be discarded: " + HexConverter.toHexString(bytes));
            }
        }

        @Override
        public void newMessage(WMBusMessage message) {

            try {
                message.decode();
            } catch (DecodingException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unable to decode header of received message: " + message, e);
                }
                return;
            }

            synchronized (this) {
                WMBusConnection connection = connectionsBySecondaryAddress
                        .get(message.getSecondaryAddress().getHashCode());

                if (connection == null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("WMBus: connection is null, from device: {} with HashCode: {}",
                                message.getSecondaryAddress().getDeviceId().toString(),
                                message.getSecondaryAddress().getHashCode());
                    }
                    return;
                }

                List<ChannelRecordContainer> channelContainers = connection.getContainersToListenFor();

                if (channelContainers.size() == 0) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("WMBus: channelContainers.size == 0, from device: "
                                + message.getSecondaryAddress().getDeviceId().toString());
                    }
                    return;
                }

                VariableDataStructure variableDataStructure = message.getVariableDataResponse();

                try {
                    variableDataStructure.decode();
                } catch (DecodingException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Unable to decode header of variable data response or received message: {}",
                                message, e);
                    }
                    return;
                }

                List<DataRecord> dataRecords = message.getVariableDataResponse().getDataRecords();
                String[] dibvibs = new String[dataRecords.size()];

                int i = 0;
                for (DataRecord dataRecord : dataRecords) {
                    dibvibs[i++] = HexConverter.toShortHexString(dataRecord.getDib()) + ':'
                            + HexConverter.toShortHexString(dataRecord.getVib());
                }

                List<ChannelRecordContainer> containersReceived = new ArrayList<>();

                long timestamp = System.currentTimeMillis();

                for (ChannelRecordContainer container : channelContainers) {
                    i = 0;
                    for (DataRecord dataRecord : dataRecords) {

                        if (dibvibs[i++].equalsIgnoreCase(container.getChannelAddress())) {

                            Value value = null;
                            switch (dataRecord.getDataValueType()) {
                            case DATE:
                                value = new StringValue(((Date) dataRecord.getDataValue()).toString());
                                container.setRecord(new Record(value, timestamp));
                                break;
                            case STRING:
                                value = new StringValue((String) dataRecord.getDataValue());
                                container.setRecord(new Record(value, timestamp));
                                break;
                            case DOUBLE:
                                value = new DoubleValue(dataRecord.getScaledDataValue());
                                container.setRecord(new Record(value, timestamp));
                                break;
                            case LONG:
                                if (dataRecord.getMultiplierExponent() == 0) {
                                    value = new LongValue((Long) dataRecord.getDataValue());
                                    container.setRecord(new Record(value, timestamp));
                                }
                                else {
                                    value = new DoubleValue(dataRecord.getScaledDataValue());
                                    container.setRecord(new Record(value, timestamp));
                                }
                                break;
                            case BCD:
                                if (dataRecord.getMultiplierExponent() == 0) {
                                    value = new LongValue(((Bcd) dataRecord.getDataValue()).longValue());
                                    container.setRecord(new Record(value, timestamp));
                                }
                                else {
                                    value = new DoubleValue(((Bcd) dataRecord.getDataValue()).longValue()
                                            * Math.pow(10, dataRecord.getMultiplierExponent()));
                                    container.setRecord(new Record(value, timestamp));
                                }
                                break;
                            case NONE:
                                if (logger.isWarnEnabled()) {
                                    logger.warn("Received data record with <dib>:<vib> = " + dibvibs[i]
                                            + " has value type NONE.");
                                }
                                continue;
                            }
                            if (logger.isTraceEnabled()) {
                                logger.trace("WMBus: Value from channel {}",
                                        container.getChannel().getId() + " is:" + value.toString());
                            }
                            containersReceived.add(container);

                            break;

                        }

                    }

                }

                listener.newRecords(containersReceived);

            }

        }

        @Override
        public void stoppedListening(IOException e) {
            WMBusSerialInterface.this.stoppedListening();
        }

    }

    public static WMBusSerialInterface getInstance(String serialPortName, String transceiverString, String modeString)
            throws ConnectionException, ArgumentSyntaxException {
        WMBusSerialInterface serialInterface;

        synchronized (interfaces) {
            serialInterface = interfaces.get(serialPortName);

            if (serialInterface != null) {
                if (!serialInterface.modeString.equals(modeString)
                        || !serialInterface.transceiverString.equals(transceiverString)) {
                    throw new ConnectionException(
                            "Connections serial interface is already in use with a different transceiver or mode");
                }
            }
            else {
                serialInterface = new WMBusSerialInterface(serialPortName, transceiverString, modeString);
                interfaces.put(serialPortName, serialInterface);
            }
        }

        return serialInterface;

    }

    private WMBusSerialInterface(String serialPortName, String transceiverString, String modeString)
            throws ArgumentSyntaxException, ConnectionException {

        this.serialPortName = serialPortName;
        this.transceiverString = transceiverString;
        this.modeString = modeString;

        WMBusMode mode = null;

        if (modeString.equalsIgnoreCase("s")) {
            mode = WMBusMode.S;
        }
        else if (modeString.equalsIgnoreCase("t")) {
            mode = WMBusMode.T;
        }
        else {
            throw new ArgumentSyntaxException(
                    "The wireless M-Bus mode is not correctly specified in the device's parameters string. Should be S or T but is: "
                            + modeString);
        }

        if (transceiverString.equals("amber")) {
            wMBusSap = new WMBusSapAmber(serialPortName, mode, new Receiver());
        }
        else if (transceiverString.equals("rc")) {
            wMBusSap = new WMBusSapRadioCrafts(serialPortName, mode, new Receiver());
        }
        else {
            throw new ArgumentSyntaxException(
                    "The type of transceiver is not correctly specified in the device's parameters string. Should be amber or rc but is: "
                            + transceiverString);
        }

        try {
            wMBusSap.open();
        } catch (IOException e) {
            throw new ConnectionException("Failed to open serial interface", e);
        }
    }

    public void connectionClosedIndication(SecondaryAddress secondaryAddress) {
        connectionsBySecondaryAddress.remove(secondaryAddress.getHashCode());
        if (connectionsBySecondaryAddress.size() == 0) {
            close();
        }
    }

    public void close() {
        synchronized (interfaces) {
            wMBusSap.close();
            interfaces.remove(serialPortName);
        }
    }

    public Connection connect(SecondaryAddress secondaryAddress, String keyString) throws ArgumentSyntaxException {
        WMBusConnection connection = new WMBusConnection(wMBusSap, secondaryAddress, keyString, this);
        if (logger.isTraceEnabled()) {
            logger.trace("WMBus: connect device with ID " + secondaryAddress.getDeviceId().toString() + " and HashCode "
                    + secondaryAddress.getHashCode());
        }
        connectionsBySecondaryAddress.put(secondaryAddress.getHashCode(), connection);
        return connection;
    }

    public void stoppedListening() {
        synchronized (interfaces) {
            interfaces.remove(serialPortName);
        }
        synchronized (this) {
            for (WMBusConnection connection : connectionsBySecondaryAddress.values()) {
                listener.connectionInterrupted("wmbus", connection);
            }
            connectionsBySecondaryAddress.clear();
        }
    }
}
