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

package org.openmuc.framework.datalogger.sql.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.openmuc.framework.datalogger.sql.SqlLoggerService;

public class SqlProperties {

    public static final List<String> COLUMNS = Arrays.asList("channelid", "channelAdress", "loggingInterval",
            "loggingTimeOffset", "unit", "valueType", "scalingFactor", "valueOffset", "listening", "samplingInterval",
            "samplingTimeOffset", "samplingGroup", "disabled", "description");

    public static final String POSTGRESQL = "postgresql";
    public static final String POSTGRES = "postgres";
    public static final String MYSQL = "mysql";
    public static final String NULL = ") NULL,";

    public static final String AND = "' AND '";
    public static final String VALUE = "value";

    private static final String PACKAGE = SqlLoggerService.class.getPackage().getName().toLowerCase();
    private static final String DEFAULT_URL = "jdbc:h2";
    private static final String DEFAULT_USER = "openmuc";
    private static final String DEFAULT_PASS = "openmuc";
    private static final String DEFAULT_SSL = "true";
    private static final String DEFAULT_SOCKET_TIMEOUT = "5";
    private static final String DEFAULT_TCP_KEEP_ALIVE = "true";
    private static final String DEFAULT_PSQL_PASS = "postgres";
    private static final String DEFAULT_TIME_ZONE = "Europe/Berlin";
    public static String url = System.getProperty(PACKAGE + ".url", DEFAULT_URL);
    public static String user = System.getProperty(PACKAGE + ".user", DEFAULT_USER);
    public static String password = System.getProperty(PACKAGE + ".password", DEFAULT_PASS);
    public static String ssl = System.getProperty(PACKAGE + ".ssl", DEFAULT_SSL);
    public static String socketTimeout = System.getProperty(PACKAGE + ".socketTimeout", DEFAULT_SOCKET_TIMEOUT);
    public static String tcpKeepAlive = System.getProperty(PACKAGE + ".tcpKeepAlive", DEFAULT_TCP_KEEP_ALIVE);
    public static String psqlPass = System.getProperty(PACKAGE + ".psqlPass", DEFAULT_PSQL_PASS);
    public static String timeZone = System.getProperty(PACKAGE + ".timeZone", DEFAULT_TIME_ZONE);

    private SqlProperties() throws IOException {
        Properties properties = getProperties();
        setSqlProperties(properties);
    }

    private void setSqlProperties(Properties properties) {
        url = properties.getProperty(PACKAGE + ".url", DEFAULT_URL);
        user = properties.getProperty(PACKAGE + ".user", DEFAULT_USER);
        password = properties.getProperty(PACKAGE + ".password", DEFAULT_PASS);
        ssl = properties.getProperty(PACKAGE + ".ssl", DEFAULT_SSL);
        socketTimeout = properties.getProperty(PACKAGE + ".socketTimeout", DEFAULT_SOCKET_TIMEOUT);
        tcpKeepAlive = properties.getProperty(PACKAGE + ".tcpKeepAlive", DEFAULT_TCP_KEEP_ALIVE);
        psqlPass = properties.getProperty(PACKAGE + ".psqlPass", DEFAULT_PSQL_PASS);
        timeZone = System.getProperty(PACKAGE + ".timeZone", DEFAULT_TIME_ZONE);
    }

    private Properties getProperties() throws IOException {
        String propertyFile = System.getProperties().containsKey("logger.sql.conf.file")
                ? System.getProperty("logger.sql.conf.file")
                : "conf/logger.sql.conf";

        FileReader reader = new FileReader(propertyFile);
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }
}
