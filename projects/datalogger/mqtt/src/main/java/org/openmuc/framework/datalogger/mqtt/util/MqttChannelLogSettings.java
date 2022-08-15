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

import java.util.Arrays;

import javax.management.openmbean.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO parsing settings should be part of core.datalogger.spi Format and parsing of datalogger
// logSettings should be equal for all loggers
public class MqttChannelLogSettings {

    private static final String LOGGER_SEPARATOR = ";";
    private static final String ELEMENT_SEPARATOR = ",";

    private static final Logger logger = LoggerFactory.getLogger(MqttChannelLogSettings.class);

    public static String getTopic(String logSettings) {
        if (logSettings == null || logSettings.isEmpty()) {
            throw new UnsupportedOperationException("TODO implement default Topic?");
        }
        else {
            return parseTopic(logSettings);
        }
    }

    // Example logSettings
    // 1 <logSettings">amqplogger:queue=my/queue,setting=true,test=123;mqttlogger:topic=/my/topic/</logSettings>
    // 2 amqplogger:queue=my/queue,setting=true,test=123; mqttlogger:topic=/my/topic/
    // 3 mqttlogger:topic=/my/topic/
    // 4 topic=/my/topic/

    private static String parseTopic(String logSettings) {
        String mqttLoggerSegment = Arrays.stream(logSettings.split(LOGGER_SEPARATOR))
                .filter(seg -> seg.contains("mqttlogger"))
                .findFirst()
                .orElseThrow(() -> new InvalidKeyException("logSettings: mqttlogger id is missing"));

        return Arrays.stream(mqttLoggerSegment.split(ELEMENT_SEPARATOR))
                .filter(part -> part.contains("topic"))
                .map(queue -> queue.split("=")[1])
                .findFirst()
                .orElseThrow(() -> new InvalidKeyException("logSettings: topic is missing"));
    }
}
