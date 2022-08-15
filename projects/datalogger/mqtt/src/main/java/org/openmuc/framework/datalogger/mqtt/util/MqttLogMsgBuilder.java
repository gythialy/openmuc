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

package org.openmuc.framework.datalogger.mqtt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.openmuc.framework.datalogger.mqtt.dto.MqttLogChannel;
import org.openmuc.framework.datalogger.mqtt.dto.MqttLogMsg;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.parser.spi.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttLogMsgBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MqttLogMsgBuilder.class);

    private final HashMap<String, MqttLogChannel> channelsToLog;
    private final ParserService parserService;

    public MqttLogMsgBuilder(HashMap<String, MqttLogChannel> channelsToLog, ParserService parserService) {
        this.channelsToLog = channelsToLog;
        this.parserService = parserService;
    }

    public List<MqttLogMsg> buildLogMsg(List<LoggingRecord> loggingRecordList, boolean isLogMultiple) {
        if (isLogMultiple) {
            return logMultiple(loggingRecordList);
        }
        else {
            return logSingle(loggingRecordList);
        }
    }

    private List<MqttLogMsg> logSingle(List<LoggingRecord> loggingRecords) {

        List<MqttLogMsg> logMessages = new ArrayList<>();

        for (LoggingRecord loggingRecord : loggingRecords) {
            try {
                String topic = channelsToLog.get(loggingRecord.getChannelId()).topic;
                byte[] message = parserService.serialize(loggingRecord);
                logMessages.add(new MqttLogMsg(loggingRecord.getChannelId(), message, topic));
            } catch (SerializationException e) {
                logger.error("failed to parse records {}", e.getMessage());
            }
        }

        return logMessages;
    }

    private List<MqttLogMsg> logMultiple(List<LoggingRecord> loggingRecords) {

        List<MqttLogMsg> logMessages = new ArrayList<>();

        if (hasDifferentTopics()) {

            throw new UnsupportedOperationException(
                    "logMultiple feature is an experimental feature: logMultiple=true is not possible with "
                            + "different topics in logSettings. Set logMultiple=false OR leave it true "
                            + "and assign same topic to all channels.");

            // TODO make improvement: check only for given channels

            // TODO make improvement:
            // CASE A - OK
            // ch1, ch2, ch3 = 5 s - topic1
            // CASE B - NOT SUPPORTED YET
            // ch1, ch2 logInterval = 5 s - topic1
            // ch3, ch3 logInterval = 10 s - topic2
            // ch4 logInterval 20 s - topic 3
            // if isLogMultiple=true, then group channels per topic
            // or default: log warning and use logSingle instead

        }
        else {
            try {
                // since all topics are the same, get the topic of
                String topic = channelsToLog.get(loggingRecords.get(0).getChannelId()).topic;
                byte[] message = parserService.serialize(loggingRecords);
                String channelIds = loggingRecords.stream()
                        .map(record -> record.getChannelId())
                        .collect(Collectors.toList())
                        .toString();
                logMessages.add(new MqttLogMsg(channelIds, message, topic));
            } catch (SerializationException e) {
                logger.error("failed to parse records {}", e.getMessage());
            }

        }

        return logMessages;
    }

    private boolean hasDifferentTopics() {
        long distinct = channelsToLog.values().stream().map(channel -> channel.topic).distinct().count();
        // If the count of this stream is smaller or equal to 1, then all the elements are equal. so > 1 means unequal
        return distinct > 1;
    }

}
