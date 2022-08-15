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

package org.openmuc.framework.core.datamanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingController {

    private static final Logger logger = LoggerFactory.getLogger(LoggingController.class);
    private final Deque<DataLoggerService> activeDataLoggers;
    private Map<String, List<LoggingRecord>> logContainerMap;

    public LoggingController(Deque<DataLoggerService> activeDataLoggers) {
        this.activeDataLoggers = activeDataLoggers;
    }

    public boolean channelsHaveToBeLogged(Action currentAction) {
        return currentAction.loggingCollections != null && !currentAction.loggingCollections.isEmpty();
    }

    public List<Optional<ChannelCollection>> triggerLogging(Action currentAction) {
        initLoggingRecordMap();
        List<Optional<ChannelCollection>> filledChannels = new ArrayList<>();

        for (ChannelCollection loggingCollection : currentAction.loggingCollections) {
            List<ChannelImpl> toRemove = new LinkedList<>();

            for (ChannelImpl channel : loggingCollection.channels) {
                if (channel.getChannelState() == ChannelState.DELETED) {
                    toRemove.add(channel);
                }
                else if (!channel.config.isDisabled()) {
                    fillLoggingRecordMapWithChannel(channel);
                }
            }

            for (ChannelImpl channel : toRemove) {
                loggingCollection.channels.remove(channel);
            }

            if (loggingCollection.channels != null && !loggingCollection.channels.isEmpty()) {
                filledChannels.add(Optional.of(loggingCollection));
            }
        }
        deliverLogsToLogServices(currentAction.startTime);

        return filledChannels;
    }

    public void deliverLogsToEventBasedLogServices(List<ChannelRecordContainerImpl> channelRecordContainerList) {
        initLoggingRecordMap();
        channelRecordContainerList.stream()
                .forEach(channelRecord -> fillLoggingRecordMapWithChannel(channelRecord.getChannel()));

        for (DataLoggerService dataLogger : activeDataLoggers) {
            List<LoggingRecord> logContainers = logContainerMap.get(dataLogger.getId());

            if (!logContainers.isEmpty()) {
                dataLogger.logEvent(logContainers, System.currentTimeMillis());
            }
        }
    }

    private void initLoggingRecordMap() {
        logContainerMap = new HashMap<>();
        for (DataLoggerService dataLogger : activeDataLoggers) {
            logContainerMap.put(dataLogger.getId(), new ArrayList<>());
        }
    }

    private void fillLoggingRecordMapWithChannel(ChannelImpl channel) {
        String logSettings = channel.getLoggingSettings();

        if (logSettings != null && !logSettings.isEmpty()) {
            extendMapForDefinedLoggerFromSettings(channel, logSettings);
        }
        else {
            addRecordToAllLoggerWhichNotRequiresSettings(channel);
        }
    }

    private void addRecordToAllLoggerWhichNotRequiresSettings(Channel channel) {
        Record latestRecord = channel.getLatestRecord();
        logContainerMap.forEach((k, v) -> {
            if (loggerWithIdNotRequiresSettings(k)) {
                v.add(new LoggingRecord(channel.getId(), latestRecord));
            }
        });
    }

    private boolean loggerWithIdNotRequiresSettings(String loggerId) {
        return activeDataLoggers.stream()
                .filter((DataLoggerService::logSettingsRequired))
                .map(logger -> logger.getId())
                .noneMatch(filteredId -> filteredId.equals(loggerId));
    }

    private void extendMapForDefinedLoggerFromSettings(ChannelImpl channel, String logSettings) {
        List<String> definedLoggerInChannel = parseDefinedLogger(logSettings);

        for (String definedLogger : definedLoggerInChannel) {
            if (logContainerMap.get(definedLogger) != null) {
                Record latestRecord = channel.getLatestRecord();
                logContainerMap.get(definedLogger).add(new LoggingRecord(channel.getId(), latestRecord));
            }
            else {
                logger.warn("DataLoggerService with Id {} not found for channel {}", definedLogger,
                        channel.config.getId());
                logger.warn("Correct configuration in channel.xml?");
            }
        }
    }

    private List<String> parseDefinedLogger(String logSettings) {
        String[] loggerSegments = logSettings.split(";");
        List<String> definedLogger = Arrays.stream(loggerSegments)
                .map(seg -> seg.split(":")[0])
                .collect(Collectors.toList());

        return definedLogger;
    }

    private void deliverLogsToLogServices(long startTime) {
        for (DataLoggerService dataLogger : activeDataLoggers) {
            List<LoggingRecord> logContainers = logContainerMap.get(dataLogger.getId());
            dataLogger.log(logContainers, startTime);
        }
    }
}
