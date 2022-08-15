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

package org.openmuc.framework.driver.mqtt;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.framework.lib.mqtt.MqttConnection;
import org.openmuc.framework.lib.mqtt.MqttReader;
import org.openmuc.framework.lib.mqtt.MqttSettings;
import org.openmuc.framework.lib.mqtt.MqttWriter;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.parser.spi.SerializationException;
import org.openmuc.framework.security.SslManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDriverConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(MqttDriverConnection.class);
    private final MqttConnection mqttConnection;
    private final MqttWriter mqttWriter;
    private final MqttReader mqttReader;
    private final Map<String, ParserService> parsers = new HashMap<>();
    private final Map<String, Long> lastLoggedRecords = new HashMap<>();
    private final List<ChannelRecordContainer> recordContainerList = new ArrayList<>();
    private final Properties settings = new Properties();

    public MqttDriverConnection(String host, String settings) throws ArgumentSyntaxException {
        MqttSettings mqttSettings = getMqttSettings(host, settings);
        mqttConnection = new MqttConnection(mqttSettings);
        String pid = "mqttdriver";
        mqttWriter = new MqttWriter(mqttConnection, pid);
        mqttReader = new MqttReader(mqttConnection, pid);
        if (!mqttSettings.isSsl()) {
            mqttConnection.connect();
        }
    }

    private MqttSettings getMqttSettings(String host, String settings) throws ArgumentSyntaxException {
        settings = settings.replaceAll(";", "\n");
        try {
            this.settings.load(new StringReader(settings));
        } catch (IOException e) {
            throw new ArgumentSyntaxException("Could not read settings string");
        }

        int port = Integer.parseInt(this.settings.getProperty("port"));
        String username = this.settings.getProperty("username");
        String password = this.settings.getProperty("password");
        boolean ssl = Boolean.parseBoolean(this.settings.getProperty("ssl"));
        long maxBufferSize = Long.parseLong(this.settings.getProperty("maxBufferSize", "0"));
        long maxFileSize = Long.parseLong(this.settings.getProperty("maxFileSize", "0"));
        int maxFileCount = Integer.parseInt(this.settings.getProperty("maxFileCount", "1"));
        int connectionRetryInterval = Integer.parseInt(this.settings.getProperty("connectionRetryInterval", "10"));
        int connectionAliveInterval = Integer.parseInt(this.settings.getProperty("connectionAliveInterval", "10"));
        String persistenceDirectory = this.settings.getProperty("persistenceDirectory", "data/driver/mqtt");
        String lastWillTopic = this.settings.getProperty("lastWillTopic", "");
        byte[] lastWillPayload = this.settings.getProperty("lastWillPayload", "").getBytes();
        boolean lastWillAlways = Boolean.parseBoolean(this.settings.getProperty("lastWillAlways", "false"));
        String firstWillTopic = this.settings.getProperty("firstWillTopic", "");
        byte[] firstWillPayload = this.settings.getProperty("firstWillPayload", "").getBytes();
        boolean webSocket = Boolean.parseBoolean(this.settings.getProperty("webSocket", "false"));

        return new MqttSettings(host, port, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount,
                connectionRetryInterval, connectionAliveInterval, persistenceDirectory, lastWillTopic, lastWillPayload,
                lastWillAlways, firstWillTopic, firstWillPayload, webSocket);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        List<String> topics = new ArrayList<>();
        for (ChannelRecordContainer container : containers) {
            topics.add(container.getChannelAddress());
        }

        if (topics.isEmpty()) {
            return;
        }
        mqttReader.listen(topics, (topic, message) -> {
            Channel channel = containers.get(topics.indexOf(topic)).getChannel();
            Record record = getRecord(message, channel.getValueType());

            if (recordIsOld(channel.getId(), record)) {
                return;
            }

            addMessageToContainerList(record, containers.get(topics.indexOf(topic)));
            if (recordContainerList.size() >= Integer.parseInt(settings.getProperty("recordCollectionSize", "1"))) {
                notifyListenerAndPurgeList(listener);
            }
        });
    }

    private void notifyListenerAndPurgeList(RecordsReceivedListener listener) {
        logTraceNewRecord();
        listener.newRecords(recordContainerList);
        recordContainerList.clear();
    }

    private void addMessageToContainerList(Record record, ChannelRecordContainer container) {
        ChannelRecordContainer copiedContainer = container.copy();
        copiedContainer.setRecord(record);

        recordContainerList.add(copiedContainer);
    }

    private boolean recordIsOld(String channelId, Record record) {
        Long lastTimestamp = lastLoggedRecords.get(channelId);

        if (lastTimestamp == null) {
            lastLoggedRecords.put(channelId, record.getTimestamp());
            return false;
        }

        if (record.getTimestamp() == null || record.getTimestamp() <= lastTimestamp) {
            return true;
        }

        lastLoggedRecords.put(channelId, record.getTimestamp());
        return false;
    }

    private Record getRecord(byte[] message, ValueType valueType) {
        Record record;
        if (parsers.containsKey(settings.getProperty("parser"))) {
            record = parsers.get(settings.getProperty("parser")).deserialize(message, valueType);
        }
        else {
            record = new Record(new ByteArrayValue(message), System.currentTimeMillis());
        }

        return record;
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        for (ChannelValueContainer container : containers) {
            Record record = new Record(container.getValue(), System.currentTimeMillis());
            LoggingRecord loggingRecord = new LoggingRecord(container.getChannelAddress(), record);
            if (parsers.containsKey(settings.getProperty("parser"))) {
                byte[] message;
                try {
                    message = parsers.get(settings.getProperty("parser")).serialize(loggingRecord);
                } catch (SerializationException e) {
                    logger.error(e.getMessage());
                    continue;
                }
                mqttWriter.write(container.getChannelAddress(), message);
                container.setFlag(Flag.VALID);
            }
            else {
                logger.error("A parser is needed to write messages and none have been registered.");
                throw new UnsupportedOperationException();
            }
        }
        return null;
    }

    @Override
    public void disconnect() {
        mqttWriter.shutdown();
        mqttConnection.disconnect();
    }

    public void setParser(String parserId, ParserService parser) {
        if (parser == null) {
            parsers.remove(parserId);
            return;
        }
        parsers.put(parserId, parser);
    }

    private void logTraceNewRecord() {
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("new records");
            for (ChannelRecordContainer container : recordContainerList) {
                sb.append("\ntopic: " + sb.append(container.getChannelAddress()) + "\n");
                sb.append("record: " + container.getRecord().toString());
            }
            logger.trace(sb.toString());
        }
    }

    public void setSslManager(SslManagerInterface instance) {
        if (mqttConnection.getSettings().isSsl()) {
            logger.debug("SSLManager registered in driver");
            mqttConnection.setSslManager(instance);
            if (instance.isLoaded()) {
                mqttConnection.connect();
            }
        }
    }
}
