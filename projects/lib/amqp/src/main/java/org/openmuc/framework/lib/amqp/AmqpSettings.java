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
    private final String persistenceDirectory;
    private final int maxFileCount;
    private final long maxFileSize;
    private final long maxBufferSize;
    private final int connectionAliveInterval;

    /**
     * @param host
     *            the host, i.e. broker.domain.tld
     * @param port
     *            the port, i.e. 5672
     * @param virtualHost
     *            the virtualHost to use, i.e. /
     * @param username
     *            the username, i.e. guest
     * @param password
     *            the password, i.e. guest
     * @param ssl
     *            whether connecting with ssl
     * @param exchange
     *            the exchange to publish to
     * @param persistenceDirectory
     *            directory being used by FilePersistence
     * @param maxFileCount
     *            maximum file count per buffer created by FilePersistence
     * @param maxFileSize
     *            maximum file size per FilePersistence buffer file
     * @param maxBufferSize
     *            maximum RAM buffer size
     * @param connectionAliveInterval
     *            checks every given seconds if connection is alive
     */
    public AmqpSettings(String host, int port, String virtualHost, String username, String password, boolean ssl,
            String exchange, String persistenceDirectory, int maxFileCount, long maxFileSize, long maxBufferSize,
            int connectionAliveInterval) {
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.exchange = exchange;
        this.persistenceDirectory = persistenceDirectory;
        this.maxFileCount = maxFileCount;
        this.maxFileSize = maxFileSize;
        this.maxBufferSize = maxBufferSize;
        this.connectionAliveInterval = connectionAliveInterval;
    }

    public AmqpSettings(String host, int port, String virtualHost, String username, String password, boolean ssl,
            String exchange) {
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.exchange = exchange;
        this.persistenceDirectory = "";
        this.maxFileCount = 0;
        this.maxFileSize = 0;
        this.maxBufferSize = 0;
        this.connectionAliveInterval = 0;
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

    public String getPersistenceDirectory() {
        return persistenceDirectory;
    }

    public int getMaxFileCount() {
        return maxFileCount;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public long getMaxBufferSize() {
        return maxBufferSize;
    }

    public int getConnectionAliveInterval() {
        return connectionAliveInterval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host = " + host + "\n");
        sb.append("port = " + port + "\n");
        sb.append("vHost = " + virtualHost + "\n");
        sb.append("username = " + username + "\n");
        sb.append("passwort = " + password + "\n");
        sb.append("ssl = " + ssl + "\n");
        sb.append("exchange = " + exchange + "\n");
        sb.append("persistenceDirectory = " + persistenceDirectory + "\n");
        sb.append("maxFileCount = " + maxFileCount + "\n");
        sb.append("maxFileSize = " + maxFileSize + "\n");
        sb.append("maxBufferSize = " + maxBufferSize + "\n");
        sb.append("connectionAliveInterval = " + connectionAliveInterval + "\n");

        return sb.toString();
    }
}
