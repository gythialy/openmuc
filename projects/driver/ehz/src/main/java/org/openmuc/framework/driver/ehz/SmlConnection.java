/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.driver.ehz;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
import org.openmuc.jsml.structures.ASNObject;
import org.openmuc.jsml.structures.EMessageBody;
import org.openmuc.jsml.structures.Integer16;
import org.openmuc.jsml.structures.Integer32;
import org.openmuc.jsml.structures.Integer64;
import org.openmuc.jsml.structures.Integer8;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlMessage;
import org.openmuc.jsml.structures.Unsigned16;
import org.openmuc.jsml.structures.Unsigned32;
import org.openmuc.jsml.structures.Unsigned64;
import org.openmuc.jsml.structures.Unsigned8;
import org.openmuc.jsml.structures.responses.SmlGetListRes;
import org.openmuc.jsml.transport.SerialReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmlConnection extends GeneralConnection {

    private static Logger logger = LoggerFactory.getLogger(GeneralConnection.class);

    private final SerialReceiver receiver;
    private final SerialPort serialPort;

    // TODO serverId is never used..
    private String serverId;

    private final ExecutorService threadExecutor;

    private ListenerTask listenerTask;

    public SmlConnection(String serialPortName) throws ConnectionException {
        try {
            this.serialPort = setupSerialPort(serialPortName);
            this.receiver = new SerialReceiver(serialPort);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }

        this.threadExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void disconnect() {
        if (this.listenerTask != null) {
            this.listenerTask.stopListening();
        }
        this.threadExecutor.shutdown();

        try {
            if (receiver != null) {
                receiver.close();
            }
            if (!serialPort.isClosed()) {
                serialPort.close();
            }
        } catch (IOException e) {
            logger.warn("Error, while closing serial port.", e);
        }
    }

    @Override
    public void startListening(final List<ChannelRecordContainer> containers, final RecordsReceivedListener listener)
            throws ConnectionException {
        logger.trace("start listening");

        this.listenerTask = new ListenerTask(containers, listener);
        this.threadExecutor.execute(listenerTask);
    }

    private class ListenerTask implements Runnable {

        private final List<ChannelRecordContainer> containers;
        private final RecordsReceivedListener listener;
        private boolean stopListening;

        public ListenerTask(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) {
            this.containers = containers;
            this.listener = listener;
            this.stopListening = false;

        }

        @Override
        public void run() {

            while (!this.stopListening) {
                try {
                    long timestamp = System.currentTimeMillis();
                    SmlListEntry[] smlListEntries = retrieveSmlListEntries();

                    addEntriesToContainers(containers, timestamp, smlListEntries);
                    listener.newRecords(containers);
                } catch (InterruptedIOException e) {
                } catch (IOException e) {
                    listener.connectionInterrupted("ehz", SmlConnection.this);
                }
            }

        }

        public void stopListening() {
            this.stopListening = true;
        }

    }

    @Override
    public void read(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException {
        logger.trace("reading channels");
        final long timestamp = System.currentTimeMillis();
        SmlListEntry[] list;
        try {
            list = retrieveSmlListEntries();
        } catch (IOException e) {
            logger.error("read failed", e);
            disconnect();
            throw new ConnectionException(e);
        }

        addEntriesToContainers(containers, timestamp, list);
    }

    private static void addEntriesToContainers(List<ChannelRecordContainer> containers, final long timestamp,
            SmlListEntry[] smlEntries) {
        Map<String, Value> values = new LinkedHashMap<>();
        for (SmlListEntry entry : smlEntries) {
            String address = convertBytesToHexString(entry.getObjName().getValue());
            ValueContainer valueContainer = extractValueOf(entry);
            values.put(address, valueContainer.value);

            logger.trace("{} = {}", address, valueContainer.value);
        }

        GeneralConnection.handleChannelRecordContainer(containers, values, timestamp);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(int timeout) {
        List<ChannelScanInfo> channelInfos = new LinkedList<>();

        logger.debug("scanning channels");
        try {
            SmlListEntry[] list = retrieveSmlListEntries();
            for (SmlListEntry entry : list) {
                ChannelScanInfo channelInfo = convertEntryToScanInfo(entry);
                channelInfos.add(channelInfo);
            }
        } catch (IOException e) {
            logger.error("scan for channels failed", e);
        }
        return channelInfos;
    }

    private static ChannelScanInfo convertEntryToScanInfo(SmlListEntry entry) {
        String channelAddress = convertBytesToHexString(entry.getObjName().getValue());
        ValueContainer valueContainer = extractValueOf(entry);
        Value value = valueContainer.value;
        String description = MessageFormat.format("Current value: {0} {1}", value, entry.getUnit());
        ValueType valueType = valueContainer.valueType;
        Integer valueTypeLength = null;

        if (value != null) {
            if (valueType == ValueType.STRING) {
                String stringValue = value.asString();
                valueTypeLength = stringValue.length();
            }
            else if (valueType == ValueType.BYTE_ARRAY) {
                byte[] byteValue = value.asByteArray();
                valueTypeLength = byteValue.length;
            }
        }

        boolean readable = true;
        boolean writable = false;
        return new ChannelScanInfo(channelAddress, description, valueType, valueTypeLength, readable, writable);
    }

    @Override
    public boolean works() {
        try {
            retrieveSmlListEntries();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private synchronized SmlListEntry[] retrieveSmlListEntries() throws IOException {
        SmlFile smlFile = receiver.getSMLFile();

        List<SmlMessage> messages = smlFile.getMessages();

        for (SmlMessage message : messages) {
            EMessageBody tag = message.getMessageBody().getTag();

            if (tag != EMessageBody.GET_LIST_RESPONSE) {
                continue;
            }

            SmlGetListRes getListResult = (SmlGetListRes) message.getMessageBody().getChoice();

            if (serverId == null) {
                serverId = convertBytesToHexString(getListResult.getServerId().getValue());
            }

            return getListResult.getValList().getValListEntry();
        }

        return null;
    }

    private static String convertBytesToHexString(byte[] data) {
        return bytesToHex(data);
    }

    static final String HEXES = "0123456789ABCDEF";

    private static String bytesToHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    private static ValueContainer extractValueOf(SmlListEntry entry) {
        double value = 0;
        ValueType valueType = ValueType.DOUBLE;

        ASNObject obj = entry.getValue().getChoice();
        if (obj.getClass().equals(Integer64.class)) {
            Integer64 val = (Integer64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer32.class)) {
            Integer32 val = (Integer32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer16.class)) {
            Integer16 val = (Integer16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer8.class)) {
            Integer8 val = (Integer8) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned64.class)) {
            Unsigned64 val = (Unsigned64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned32.class)) {
            Unsigned32 val = (Unsigned32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned16.class)) {
            Unsigned16 val = (Unsigned16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned8.class)) {
            Unsigned8 val = (Unsigned8) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(OctetString.class)) {
            OctetString val = (OctetString) obj;
            return new ValueContainer(new StringValue(new String(val.getValue())), ValueType.STRING);
        }
        else {
            return new ValueContainer(new DoubleValue(Double.NaN), valueType);
        }

        byte scaler = entry.getScaler().getVal();
        double scaledValue = value * Math.pow(10, scaler);

        return new ValueContainer(new DoubleValue(scaledValue), valueType);
    }

    private static SerialPort setupSerialPort(String serialPortName) throws IOException {
        SerialPortBuilder serialPortBuilder = SerialPortBuilder.newBuilder(serialPortName);
        serialPortBuilder.setBaudRate(9600)
                .setDataBits(DataBits.DATABITS_8)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.NONE)
                .setFlowControl(FlowControl.RTS_CTS);

        return serialPortBuilder.build();
    }

    private static class ValueContainer {
        private final Value value;
        private final ValueType valueType;

        public ValueContainer(Value value, ValueType valueType) {
            this.value = value;
            this.valueType = valueType;
        }
    }

}
