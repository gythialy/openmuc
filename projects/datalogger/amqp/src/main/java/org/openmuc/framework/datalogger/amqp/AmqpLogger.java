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

package org.openmuc.framework.datalogger.amqp;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import javax.management.openmbean.InvalidKeyException;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.lib.amqp.AmqpConnection;
import org.openmuc.framework.lib.amqp.AmqpSettings;
import org.openmuc.framework.lib.amqp.AmqpWriter;
import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.parser.spi.SerializationException;
import org.openmuc.framework.security.SslManagerInterface;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class AmqpLogger implements DataLoggerService, ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(AmqpLogger.class);
    private final HashMap<String, LogChannel> channelsToLog = new HashMap<>();
    private final HashMap<String, ParserService> parsers = new HashMap<>();
    private final PropertyHandler propertyHandler;
    private final Settings settings;
    private AmqpWriter writer;
    private AmqpConnection connection;
    private SslManagerInterface sslManager;
    private boolean configLoaded = false;
    private final boolean sslLoaded = false;
    private final boolean listening = false;

    public AmqpLogger() {
        String pid = AmqpLogger.class.getName();
        settings = new Settings();
        propertyHandler = new PropertyHandler(settings, pid);
    }

    @Override
    public String getId() {
        return "amqplogger";
    }

    @Override
    public void setChannelsToLog(List<LogChannel> logChannels) {
        channelsToLog.clear();

        for (LogChannel logChannel : logChannels) {
            String channelId = logChannel.getId();
            channelsToLog.put(channelId, logChannel);
        }
    }

    @Override
    public synchronized void log(List<LoggingRecord> containers, long timestamp) {
        if (!isLoggerReady()) {
            logger.warn("Skipped logging values, still loading");
            return;
        }
        if (writer == null) {
            logger.warn("AMQP connection is not established");
            return;
        }

        iterateContainersToLog(containers);

        // ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        // Future future = executor.submit(() -> {
        //
        // });
        //
        // int currentLoggingInterval = channelsToLog.get(containers.get(0).getChannelId()).getLoggingInterval();
        // executor.schedule(() -> {
        // future.cancel(true);
        // logger.warn("Logging execution needs too much time, skipping...");
        // }, currentLoggingInterval, TimeUnit.SECONDS);
    }

    private void iterateContainersToLog(List<LoggingRecord> containers) {
        for (LoggingRecord loggingRecord : containers) {
            String channelId = loggingRecord.getChannelId();
            if (channelsToLog.containsKey(channelId)) {
                executeLog(loggingRecord);
            }
        }
    }

    private void executeLog(LoggingRecord loggingRecord) {
        String channelId = loggingRecord.getChannelId();
        byte[] message;

        if (parsers.containsKey(propertyHandler.getString(Settings.PARSER))) {
            message = parseMessage(loggingRecord);
        }
        else {
            Gson gson = new Gson();
            message = gson.toJson(loggingRecord.getRecord()).getBytes();
        }

        if (message == null) {
            return;
        }

        writer.write(getQueueName(channelId), message);
    }

    private byte[] parseMessage(LoggingRecord loggingRecord) {
        try {
            return parsers.get(propertyHandler.getString(Settings.PARSER)).serialize(loggingRecord);
        } catch (SerializationException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private String getQueueName(String channelId) {
        LogChannel logChannelMeta = channelsToLog.get(channelId);
        String logSettings = logChannelMeta.getLoggingSettings();

        if (logSettings == null || logSettings.isEmpty()) {
            return propertyHandler.getString(Settings.FRAMEWORK) + channelId;
        }
        else {
            return parseDefinedQueue(logSettings);
        }
    }

    private String parseDefinedQueue(String logSettings) {
        String amqpLoggerSegment = Arrays.stream(logSettings.split(";"))
                .filter(seg -> seg.contains("amqplogger"))
                .map(seg -> seg.replace(':', ','))
                .findFirst()
                .orElseThrow(() -> new InvalidKeyException());

        return Arrays.stream(amqpLoggerSegment.split(","))
                .filter(part -> part.contains("queue"))
                .map(queue -> queue.split("=")[1])
                .findFirst()
                .orElseThrow(() -> new InvalidKeyException());
    }

    @Override
    public void logEvent(List<LoggingRecord> loggingRecords, long timestamp) {
        log(loggingRecords, timestamp);
    }

    @Override
    public boolean logSettingsRequired() {
        return true;
    }

    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Record getLatestLogRecord(String channelId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updated(Dictionary<String, ?> propertyDict) throws ConfigurationException {
        DictionaryPreprocessor dict = new DictionaryPreprocessor(propertyDict);
        if (!dict.wasIntermediateOsgiInitCall()) {
            tryProcessConfig(dict);
        }
    }

    private void tryProcessConfig(DictionaryPreprocessor newConfig) {
        try {
            propertyHandler.processConfig(newConfig);
            if (propertyHandler.configChanged()) {
                applyConfigChanges();
            }
        } catch (ServicePropertyException e) {
            logger.error("update properties failed", e);
            shutdown();
        }
    }

    private void applyConfigChanges() {
        configLoaded = true;
        logger.info("Configuration changed - new configuration {}", propertyHandler.toString());
        if (writer != null) {
            shutdown();
        }
        connect();
    }

    private void connect() {
        if (configLoaded) {
            logger.info("Start connection to amqp backend...");
            AmqpSettings amqpSettings = createAmqpSettings();
            try {
                connection = new AmqpConnection(amqpSettings);
                writer = new AmqpWriter(connection, getId());
                connection.setSslManager(sslManager);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                logger.error("Check your configuration!");
            }
        }
    }

    private boolean isLoggerReady() {
        boolean sslNeeded = propertyHandler.getBoolean(Settings.SSL);

        if (sslNeeded) {
            return isLoggerReadyForSsl();
        }

        return configLoaded;
    }

    private boolean isLoggerReadyForSsl() {
        if (sslManager == null) {
            return false;
        }

        return configLoaded && sslManager.isLoaded();
    }

    private AmqpSettings createAmqpSettings() {
        // @formatter:off
        AmqpSettings amqpSettings = new AmqpSettings(
                propertyHandler.getString(Settings.HOST),
                propertyHandler.getInt(Settings.PORT),
                propertyHandler.getString(Settings.VIRTUAL_HOST),
                propertyHandler.getString(Settings.USERNAME),
                propertyHandler.getString(Settings.PASSWORD),
                propertyHandler.getBoolean(Settings.SSL),
                propertyHandler.getString(Settings.EXCHANGE),
                propertyHandler.getString(Settings.PERSISTENCE_DIR),
                propertyHandler.getInt(Settings.MAX_FILE_COUNT),
                propertyHandler.getInt(Settings.MAX_FILE_SIZE),
                propertyHandler.getInt(Settings.MAX_BUFFER_SIZE),
                propertyHandler.getInt(Settings.CONNECTION_ALIVE_INTERVAL));
        // @formatter:on
        return amqpSettings;
    }

    public void addParser(String parserId, ParserService parserService) {
        parsers.put(parserId, parserService);
    }

    public void removeParser(String parserId) {
        parsers.remove(parserId);
    }

    public void shutdown() {
        logger.info("closing AMQP connection");
        if (connection != null) {
            writer.shutdown();
            connection.disconnect();
        }
    }

    public void setSslManager(SslManagerInterface instance) {
        sslManager = instance;
        connect();
    }
}
