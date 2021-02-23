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

package org.openmuc.framework.lib.amqp;

/**
 * Settings needed by AmqpConnection
 */
public class AmqpSettings {
    private final String host;
    private final int port;
    private final String virtualHost;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final String exchange;

    /**
     * @param host        the host, i.e. broker.domain.tld
     * @param port        the port, i.e. 5672
     * @param virtualHost the virtualHost to use, i.e. /
     * @param username    the username, i.e. guest
     * @param password    the password, i.e. guest
     * @param ssl         whether connecting with ssl
     * @param exchange    the exchange to use when publishing
     */
    public AmqpSettings(String host, int port, String virtualHost, String username, String password, boolean ssl,
                        String exchange) {
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.exchange = exchange;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getExchange() {
        return exchange;
    }
}
