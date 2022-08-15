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

package org.openmuc.framework.datalogger.mqtt;

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

public class MqttLoggerSettings extends GenericSettings {

    public static final String PORT = "port";
    public static final String HOST = "host";
    public static final String SSL = "ssl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String PARSER = "parser";
    public static final String MULTIPLE = "multiple";
    public static final String MAX_FILE_COUNT = "maxFileCount";
    public static final String MAX_FILE_SIZE = "maxFileSize";
    public static final String MAX_BUFFER_SIZE = "maxBufferSize";
    public static final String CONNECTION_RETRY_INTERVAL = "connectionRetryInterval";
    public static final String CONNECTION_ALIVE_INTERVAL = "connectionAliveInterval";
    public static final String PERSISTENCE_DIRECTORY = "persistenceDirectory";
    public static final String LAST_WILL_TOPIC = "lastWillTopic";
    public static final String LAST_WILL_PAYLOAD = "lastWillPayload";
    public static final String LAST_WILL_ALWAYS = "lastWillAlways";
    public static final String FIRST_WILL_TOPIC = "firstWillTopic";
    public static final String FIRST_WILL_PAYLOAD = "firstWillPayload";
    public static final String RECOVERY_CHUNK_SIZE = "recoveryChunkSize";
    public static final String RECOVERY_DELAY = "recoveryDelay";
    public static final String WEB_SOCKET = "webSocket";

    public MqttLoggerSettings() {
        super();
        // properties for connection
        properties.put(PORT, new ServiceProperty(PORT, "port for MQTT communication", "1883", true));
        properties.put(HOST, new ServiceProperty(HOST, "URL of MQTT broker", "localhost", true));
        properties.put(SSL, new ServiceProperty(SSL, "usage of ssl true/false", "false", true));
        properties.put(USERNAME, new ServiceProperty(USERNAME, "name of your MQTT account", null, false));
        properties.put(PASSWORD, new ServiceProperty(PASSWORD, "password of your MQTT account", null, false));
        properties.put(PARSER,
                new ServiceProperty(PARSER, "identifier of needed parser implementation", "openmuc", true));
        properties.put(WEB_SOCKET, new ServiceProperty(WEB_SOCKET, "usage of WebSocket true/false", "false", true));

        // properties for recovery / file buffering
        properties.put(CONNECTION_RETRY_INTERVAL,
                new ServiceProperty(CONNECTION_RETRY_INTERVAL, "connection retry interval in s", "10", true));
        properties.put(CONNECTION_ALIVE_INTERVAL,
                new ServiceProperty(CONNECTION_ALIVE_INTERVAL, "connection alive interval in s", "10", true));
        properties.put(PERSISTENCE_DIRECTORY, new ServiceProperty(PERSISTENCE_DIRECTORY,
                "directory for file buffered messages", "data/logger/mqtt", false));
        properties.put(MULTIPLE, new ServiceProperty(MULTIPLE,
                "if true compose log records of different channels to one mqtt message", "false", true));
        properties.put(MAX_FILE_COUNT,
                new ServiceProperty(MAX_FILE_COUNT, "file buffering: number of files to be created", "2", true));
        properties.put(MAX_FILE_SIZE,
                new ServiceProperty(MAX_FILE_SIZE, "file buffering: file size in kB", "5000", true));
        properties.put(MAX_BUFFER_SIZE,
                new ServiceProperty(MAX_BUFFER_SIZE, "file buffering: buffer size in kB", "1000", true));
        properties.put(RECOVERY_CHUNK_SIZE, new ServiceProperty(RECOVERY_CHUNK_SIZE,
                "number of messages which will be recovered simultaneously, 0 = disabled", "0", false));
        properties.put(RECOVERY_DELAY, new ServiceProperty(RECOVERY_DELAY,
                "delay between recovery chunk sending in ms, 0 = disabled", "0", false));

        // properties for LAST WILL / FIRST WILL
        properties.put(LAST_WILL_TOPIC,
                new ServiceProperty(LAST_WILL_TOPIC, "topic on which lastWillPayload will be published", "", false));
        properties.put(LAST_WILL_PAYLOAD, new ServiceProperty(LAST_WILL_PAYLOAD,
                "payload which will be published after (unwanted) disconnect", "", false));
        properties.put(LAST_WILL_ALWAYS, new ServiceProperty(LAST_WILL_ALWAYS,
                "send the last will also on planned disconnects", "false", false));
        properties.put(FIRST_WILL_TOPIC,
                new ServiceProperty(FIRST_WILL_TOPIC, "topic on which firstWillPayload will be published", "", false));
        properties.put(FIRST_WILL_PAYLOAD,
                new ServiceProperty(FIRST_WILL_PAYLOAD, "payload which will be published after connect", "", false));

    }

}
