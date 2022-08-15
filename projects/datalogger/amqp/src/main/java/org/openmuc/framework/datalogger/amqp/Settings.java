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

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

public class Settings extends GenericSettings {

    public static final String VIRTUAL_HOST = "virtualHost";
    public static final String SSL = "ssl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String FRAMEWORK = "framework";
    public static final String PARSER = "parser";
    public static final String EXCHANGE = "exchange";
    public static final String PORT = "port";
    public static final String HOST = "host";
    public static final String PERSISTENCE_DIR = "persistenceDirectory";
    public static final String MAX_FILE_COUNT = "maxFileCount";
    public static final String MAX_FILE_SIZE = "maxFileSize";
    public static final String MAX_BUFFER_SIZE = "maxBufferSize";
    public static final String CONNECTION_ALIVE_INTERVAL = "connectionAliveInterval";

    public Settings() {
        super();
        properties.put(PORT, new ServiceProperty(PORT, "port for AMQP communication", "5672", true));
        properties.put(HOST, new ServiceProperty(HOST, "URL of AMQP broker", "localhost", true));
        properties.put(SSL, new ServiceProperty(SSL, "usage of ssl true/false", "false", true));
        properties.put(USERNAME, new ServiceProperty(USERNAME, "name of your AMQP account", "guest", true));
        properties.put(PASSWORD, new ServiceProperty(PASSWORD, "password of your AMQP account", "guest", true));
        properties.put(PARSER,
                new ServiceProperty(PARSER, "identifier of needed parser implementation", "openmuc", true));
        properties.put(VIRTUAL_HOST, new ServiceProperty(VIRTUAL_HOST, "used virtual amqp host", "/", true));
        properties.put(FRAMEWORK, new ServiceProperty(FRAMEWORK, "framework identifier", null, false));
        properties.put(EXCHANGE, new ServiceProperty(EXCHANGE, "used amqp exchange", null, false));
        properties.put(PERSISTENCE_DIR, new ServiceProperty(PERSISTENCE_DIR,
                "persistence directory used for file buffer", "data/amqp/logger", true));
        properties.put(MAX_BUFFER_SIZE,
                new ServiceProperty(MAX_BUFFER_SIZE, "maximum RAM usage of buffer", "1024", true));
        properties.put(MAX_FILE_SIZE,
                new ServiceProperty(MAX_FILE_SIZE, "maximum file size per buffer file", "5120", true));
        properties.put(MAX_FILE_COUNT,
                new ServiceProperty(MAX_FILE_COUNT, "maximum number of files per buffer", "2", true));
        properties.put(CONNECTION_ALIVE_INTERVAL, new ServiceProperty(CONNECTION_ALIVE_INTERVAL,
                "interval in seconds to detect broken connections (heartbeat)", "60", true));

    }

}
