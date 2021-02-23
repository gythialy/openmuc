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

package org.openmuc.framework.datalogger.mqtt;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.mqtt.dto.MqttLogChannel;
import org.openmuc.framework.datalogger.mqtt.dto.MqttLogMsg;
import org.openmuc.framework.datalogger.mqtt.util.MqttLogMsgBuilder;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.lib.mqtt.MqttConnection;
import org.openmuc.framework.lib.mqtt.MqttSettings;
import org.openmuc.framework.lib.mqtt.MqttWriter;
import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.openmuc.framework.parser.spi.ParserService;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttLogger implements DataLoggerService, ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(MqttLogger.class);
    private final HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
    private final HashMap<String, ParserService> availableParsers = new HashMap<>();
    private final PropertyHandler propertyHandler;
    private String parser;
    private boolean isLogMultiple;
    private MqttWriter mqttWriter;

    private static final String LOGGER_ID = "mqttlogger";

    public MqttLogger() {
        String pid = MqttLogger.class.getName();
        MqttLoggerSettings settings = new MqttLoggerSettings();
        propertyHandler = new PropertyHandler(settings, pid);
    }

    @Override
    public String getId() {
        return LOGGER_ID;
    }

    @Override
    public void setChannelsToLog(List<LogChannel> logChannels) {
        // FIXME Datamanger should only pass logChannels which should be logged by MQTT Logger
        // right now all channels are passed to the data logger and dataloger has to
        // decide/parse which channels it hast to log
        channelsToLog.clear();
        for (LogChannel logChannel : logChannels) {
            if (logChannel.getLoggingSettings().contains(LOGGER_ID)) {
                MqttLogChannel mqttLogChannel = new MqttLogChannel(logChannel);
                channelsToLog.put(logChannel.getId(), mqttLogChannel);
            }
        }
        printChannelsConsideredByMqttLogger(logChannels);
    }

    /**
     * mainly for debugging purposes
     */
    private void printChannelsConsideredByMqttLogger(List<LogChannel> logChannels) {
        StringBuilder mqttLogChannelsSb = new StringBuilder();
        mqttLogChannelsSb.append("channels configured for mqttlogging:\n");
        channelsToLog.keySet().stream().forEach(channelId -> mqttLogChannelsSb.append(channelId).append("\n"));

        StringBuilder nonMqttLogChannelsSb = new StringBuilder();
        nonMqttLogChannelsSb.append("channels not configured for mqttlogger:\n");
        for (LogChannel logChannel : logChannels) {
            if (!logChannel.getLoggingSettings().contains(LOGGER_ID)) {
                nonMqttLogChannelsSb.append(logChannel.getId()).append("\n");
            }
        }

        logger.debug(mqttLogChannelsSb.toString());
        logger.debug(nonMqttLogChannelsSb.toString());
    }

    @Override
    public void logEvent(List<LoggingRecord> containers, long timestamp) {
        log(containers, timestamp);
    }

    @Override
    public boolean logSettingsRequired() {
        return true;
    }

    @Override
    public void log(List<LoggingRecord> loggingRecordList, long timestamp) {

        if (!isMqttWriterAvailable() || !isParserAvailable()) {
            logger.error("skipped logging values. isMqttWriterAvailable = {}, isParserAvailable = {}",
                    isMqttWriterAvailable(), isParserAvailable());
            return;
        }

        // logger.info("============================");
        // loggingRecordList.stream().map(LoggingRecord::getChannelId).forEach(id -> logger.info(id));

        // FIXME refactor OpenMUC core - actually the datamanager should only call logger.log()
        // with channels configured for this logger. If this is the case the containsKey check could be ignored
        // The filter serves as WORKAROUND to process only channels which were configured for mqtt logger
        List<LoggingRecord> logRecordsForMqttLogger = loggingRecordList.stream()
                .filter(record -> channelsToLog.containsKey(record.getChannelId()))
                .collect(Collectors.toList());

        // channelsToLog.values().stream().map(channel -> channel.topic).distinct().count();

        // Concept of the MqttLogMsgBuilder:
        // 1. cleaner code
        // 2. better testability: MqttLogMsgBuilder can be easily created in a test and the output of
        // MqttLogMsgBuilder.build() can be verified. It takes the input from logger.log() method, processes it
        // and creates ready to use messages for the mqttWriter
        MqttLogMsgBuilder logMsgBuilder = new MqttLogMsgBuilder(channelsToLog, availableParsers.get(parser));
        List<MqttLogMsg> logMessages = logMsgBuilder.buildLogMsg(logRecordsForMqttLogger, isLogMultiple);
        for (MqttLogMsg msg : logMessages) {
            logTraceMqttMessage(msg);
            mqttWriter.write(msg.topic, msg.message);
        }

        // FIXME joern: is a retrying a useful solution here?
        // catch (IOException e) {
        // logger.error("Couldn't write message to file buffer, retrying");
        // parse(records);
        // }
    }

    private void logTraceMqttMessage(MqttLogMsg msg) {
        if (logger.isTraceEnabled()) {
            logger.trace("{}\n{}: {}", msg.channelId, msg.topic, new String(msg.message));
        }
    }

    private boolean isParserAvailable() {
        if (availableParsers.containsKey(parser)) {
            return true;
        }
        logger.warn("Parser with parserId {} is not available.", parser);
        return false;
    }

    private boolean isMqttWriterAvailable() {
        // FIXME "writer" could be null if datamanager calls log() before mqttlogger has read its configuration.
        // write can be also null if configurations changes during runtime causing a disconnect)
        if (mqttWriter == null) {
            logger.warn("MqttLogger not connected to a broker yet. (MqttWriter is null)");
            return false;
        }
        return true;
    }

    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Connect to MQTT broker
     */
    private void connect() {
        logger.info("Connecting to MQTT Broker");
        MqttSettings settings = createMqttSettings();
        MqttConnection connection = new MqttConnection(settings);
        mqttWriter = new MqttWriter(connection, LOGGER_ID);
        mqttWriter.getConnection().connect();
    }

    private MqttSettings createMqttSettings() {
        // @formatter:off
        MqttSettings settings = new MqttSettings(
                propertyHandler.getString(MqttLoggerSettings.HOST),
                propertyHandler.getInt(MqttLoggerSettings.PORT),
                propertyHandler.getString(MqttLoggerSettings.USERNAME),
                propertyHandler.getString(MqttLoggerSettings.PASSWORD),
                propertyHandler.getBoolean(MqttLoggerSettings.SSL),
                propertyHandler.getInt(MqttLoggerSettings.MAX_BUFFER_SIZE),
                propertyHandler.getInt(MqttLoggerSettings.MAX_FILE_SIZE),
                propertyHandler.getInt(MqttLoggerSettings.MAX_FILE_COUNT),
                propertyHandler.getInt(MqttLoggerSettings.CONNECTION_RETRY_INTERVAL),
                propertyHandler.getInt(MqttLoggerSettings.CONNECTION_ALIVE_INTERVAL),
                propertyHandler.getString(MqttLoggerSettings.PERSISTENCE_DIRECTORY),
                propertyHandler.getString(MqttLoggerSettings.LAST_WILL_TOPIC),
                propertyHandler.getString(MqttLoggerSettings.LAST_WILL_PAYLOAD).getBytes(),
                propertyHandler.getBoolean(MqttLoggerSettings.LAST_WILL_ALWAYS),
                propertyHandler.getString(MqttLoggerSettings.FIRST_WILL_TOPIC),
                propertyHandler.getString(MqttLoggerSettings.FIRST_WILL_PAYLOAD).getBytes());
        // @formatter:on

        if (logger.isTraceEnabled()) {
            logger.trace("MqttSettings for MqttConnection \n" + settings.toString());
        }
        return settings;
    }

    @Override
    public void updated(Dictionary<String, ?> propertyDict) {
        DictionaryPreprocessor dict = new DictionaryPreprocessor(propertyDict);
        if (!dict.wasIntermediateOsgiInitCall()) {
            tryProcessConfig(dict);
        }
    }

    private void tryProcessConfig(DictionaryPreprocessor newConfig) {
        // FIXME clean code
        try {
            propertyHandler.processConfig(newConfig);

            // FIXME consider all cases (running connection, default properties, new properties, closed connection)
            if (!propertyHandler.configChanged() && propertyHandler.isDefaultConfig()) {
                // tells us:
                // 1. if we get till here then updated(dict) was processed without errors and
                // 2. the values from cfg file are identical to the default values
                logger.info("new properties: changed={}, isDefault={}", propertyHandler.configChanged(),
                        propertyHandler.isDefaultConfig());
                applyConfigChanges();
            }

            if (propertyHandler.configChanged()) {
                logger.info("properties changed: {}", propertyHandler.toString());
                applyConfigChanges();
            }
            else {
                // FIXME there should be a more elegant way rather den null check.
                // Also the initial object MqttWriter object should be initialised somewhere
                if (mqttWriter == null) {
                    // if mqttWriter is null and propertyHandler.configChanged returns false then:
                    // we got a valid config since
                    // - it passed wasIntermediateOsgiInitCall() check and
                    // - propertyHandler.processConfig() check
                    // so we can connect... seems not be very intuitive... refactor
                    connect();
                }
            }
        } catch (ServicePropertyException e) {
            logger.error("update properties failed", e);
            shutdown();
        }
    }

    private void applyConfigChanges() {
        logger.info("Configuration changed - new configuration {}", propertyHandler.toString());
        parser = propertyHandler.getString(MqttLoggerSettings.PARSER);
        isLogMultiple = propertyHandler.getBoolean(MqttLoggerSettings.MULTIPLE);
        if (mqttWriter != null) {
            // FIXME could be improved by checking if MqttSettings have changed.
            // If not then there is no need for reconnect.
            shutdown();
        }
        connect();
    }

    public void shutdown() {
        logger.info("closing MQTT connection");
        if (mqttWriter != null) {
            if (mqttWriter.isConnected()) {
                mqttWriter.getConnection().disconnect();
            }
            mqttWriter = null;
        }
    }

    public void addParser(String parserId, ParserService parserService) {
        logger.info("put parserID {} to PARSERS", parserId);
        availableParsers.put(parserId, parserService);
    }

    public void removeParser(String parserId) {
        availableParsers.remove(parserId);
    }

}
