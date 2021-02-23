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

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

public class Settings extends GenericSettings {

    public static String URL = "url";
    public static String USER = "user";
    public static String PASSWORD = "password";
    public static String SSL = "ssl";
    public static String SOCKET_TIMEOUT = "socket_timeout";
    public static String TCP_KEEP_ALIVE = "tcp_keep_alive";
    public static String PSQL_PASS = "psql_pass";
    public static String TIMEZONE = "timezone";

    public Settings() {
        super();
        String defaultUrl = "jdbc:h2:retry:file:./data/h2/h2;AUTO_SERVER=TRUE;MODE=MYSQL";
        properties.put(URL, new ServiceProperty(URL, "URL of the used database", defaultUrl, true));
        properties.put(USER, new ServiceProperty(USER, "User of the used database", "openmuc", true));
        properties.put(PASSWORD, new ServiceProperty(PASSWORD, "Password for the database user", "openmuc", true));
        properties.put(SSL, new ServiceProperty(SSL, "SSL needed for the database connection", "false", false));
        properties.put(SOCKET_TIMEOUT,
                new ServiceProperty(SOCKET_TIMEOUT, "seconds after a timeout is thrown", "5", false));
        properties.put(TCP_KEEP_ALIVE, new ServiceProperty(TCP_KEEP_ALIVE, "keep tcp connection alive", "true", false));
        properties.put(PSQL_PASS, new ServiceProperty(PSQL_PASS, "password for postgresql", "postgres", true));
        properties.put(TIMEZONE, new ServiceProperty(TIMEZONE, "local time zone", "Europe/Berlin", false));
    }
}
