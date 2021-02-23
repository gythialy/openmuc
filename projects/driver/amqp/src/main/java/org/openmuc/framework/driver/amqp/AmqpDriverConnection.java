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

package org.openmuc.framework.driver.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.framework.lib.amqp.AmqpConnection;
import org.openmuc.framework.lib.amqp.AmqpReader;
import org.openmuc.framework.lib.amqp.AmqpSettings;
import org.openmuc.framework.lib.amqp.AmqpWriter;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.parser.spi.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpDriverConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(AmqpDriverConnection.class);
    private final Setting setting;
    private final AmqpConnection connection;
    private final AmqpWriter writer;
    private final AmqpReader reader;
    private final Map<String, ParserService> parsers = new HashMap<>();
    private final Map<String, Long> lastLoggedRecords = new HashMap<>();
    private List<ChannelRecordContainer> recordContainerList;

    public AmqpDriverConnection(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        recordContainerList = new ArrayList<>();
        setting = new Setting(settings);

        AmqpSettings amqpSettings = new AmqpSettings(deviceAddress, setting.port, setting.vhost, setting.user,
                setting.password, setting.ssl, setting.exchange);

        try {
            connection = new AmqpConnection(amqpSettings);
        } catch (TimeoutException e) {
            throw new ConnectionException("Timeout while connect.", e);
        } catch (IOException e) {
            throw new ConnectionException("Not able to connect to " + deviceAddress + " " + setting.vhost, e);
        }
        writer = new AmqpWriter(connection);
        reader = new AmqpReader(connection);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException {
        for (ChannelRecordContainer container : containers) {

            String queue = container.getChannelAddress();

            byte[] message = reader.read(queue);

            if (message != null) {
                Record record = getRecord(message, container.getChannel().getValueType());
                container.setRecord(record);
            }
            else {
                container.setRecord(new Record(Flag.NO_VALUE_RECEIVED_YET));
            }
        }
        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException {

        for (ChannelRecordContainer container : containers) {
            String queue = container.getChannelAddress();

            reader.listen(Collections.singleton(queue), (String receivedQueue, byte[] message) -> {

                Record record = getRecord(message, container.getChannel().getValueType());

                if (recordsIsOld(container.getChannel().getId(), record)) {
                    return;
                }

                addMessageToContainerList(record, container);
                if (recordContainerList.size() >= setting.bufferSize) {
                    notifyListenerAndPurgeList(listener);
                }
            });

        }
    }

    private boolean recordsIsOld(String channelId, Record record) {
        Long lastTs = lastLoggedRecords.get(channelId);

        if (lastTs == null) {
            lastLoggedRecords.put(channelId, record.getTimestamp());
            return false;
        }

        if (record.getTimestamp() == null || record.getTimestamp() <= lastTs) {
            return true;
        }

        lastLoggedRecords.put(channelId, record.getTimestamp());
        return false;
    }

    private void notifyListenerAndPurgeList(RecordsReceivedListener listener) {
        listener.newRecords(recordContainerList);
        recordContainerList = new ArrayList<>();
    }

    private void addMessageToContainerList(Record record, ChannelRecordContainer container) {
        ChannelRecordContainer copiedContainer = container.copy();
        copiedContainer.setRecord(record);

        recordContainerList.add(copiedContainer);
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException {
        for (ChannelValueContainer container : containers) {
            Record record = new Record(container.getValue(), System.currentTimeMillis());

            // ToDo: cleanup data structure
            Channel channel = ((WriteValueContainer) container).getChannel();
            LoggingRecord logRecordContainer = new LoggingRecord(channel.getId(), record);

            if (parsers.containsKey(setting.parser)) {
                byte[] message = new byte[0];
                try {
                    message = parsers.get(setting.parser).serialize(logRecordContainer);
                } catch (SerializationException e) {
                    logger.error(e.getMessage());
                }
                writer.write(setting.framework + '.' + container.getChannelAddress(), message);
                container.setFlag(Flag.VALID);
            }
            else {
                throw new UnsupportedOperationException("A parser is needed to write messages");
            }
        }
        return null;
    }

    @Override
    public void disconnect() {
        connection.disconnect();
    }

    public void setParser(String parserId, ParserService parser) {
        if (parser == null) {
            parsers.remove(parserId);
            return;
        }
        parsers.put(parserId, parser);
    }

    private Record getRecord(byte[] message, ValueType valueType) {
        Record record;
        if (parsers.containsKey(setting.parser)) {
            record = parsers.get(setting.parser).deserialize(message, valueType);
        }
        else {
            record = new Record(new ByteArrayValue(message), System.currentTimeMillis());
        }

        return record;
    }

    private class Setting {
        private static final String SEPARATOR = ";";
        private static final String SETTING_VALUE_SEPARATOR = "=";

        private int port = 5672;
        private String vhost;
        private String user;
        private String password;
        private String framework;
        private String parser;
        private String exchange;
        private String frameworkChannelSeparator = ".";
        private int bufferSize = 1;
        private boolean ssl = true;

        Setting(String settings) throws ArgumentSyntaxException {
            separate(settings);
        }

        /**
         * This function extracts information out of the settings string
         *
         * @param settings
         *            The settings to separate
         * @throws ArgumentSyntaxException
         *             This is thrown if any setting is invalid
         */
        private void separate(String settings) throws ArgumentSyntaxException {

            if (settings == null || settings.isEmpty()) {
                throw new ArgumentSyntaxException("No settings given");
            }

            String[] settingsSplit = settings.split(SEPARATOR);
            for (String settingSplit : settingsSplit) {
                String[] settingPair = settingSplit.split(SETTING_VALUE_SEPARATOR);

                if (settingPair.length != 2) {
                    throw new ArgumentSyntaxException(
                            "Corrupt setting. Malformed setting found, should be <setting>=<value>");
                }
                String settingP0 = settingPair[0].trim();
                String settingP1 = settingPair[1].trim();

                switch (settingP0) {
                case "port":
                    port = parseInt(settingP1);
                    break;
                case "vhost":
                    vhost = settingP1;
                    break;
                case "user":
                    user = settingP1;
                    break;
                case "password":
                    password = settingP1;
                    break;
                case "framework":
                    framework = settingP1;
                    break;
                case "parser":
                    parser = settingP1.toLowerCase();
                    break;
                case "buffersize":
                    bufferSize = parseInt(settingP1);
                    break;
                case "ssl":
                    ssl = Boolean.parseBoolean(settingP1);
                    break;
                case "separator":
                    frameworkChannelSeparator = settingP1;
                    break;
                case "exchange":
                    exchange = settingP1;
                    break;
                default:
                    throw new ArgumentSyntaxException("Invalid setting given: " + settingP0);
                }
            }
        }

        int parseInt(String value) throws ArgumentSyntaxException {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("Value of port is not a integer");
            }
        }

    }
}
