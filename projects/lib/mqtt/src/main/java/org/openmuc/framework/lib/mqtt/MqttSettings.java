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

package org.openmuc.framework.lib.mqtt;

public class MqttSettings {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final long maxBufferSize;
    private final long maxFileSize;
    private final int maxFileCount;
    private final int connectionRetryInterval;
    private final int connectionAliveInterval;
    private final String persistenceDirectory;
    private final String lastWillTopic;
    private final byte[] lastWillPayload;
    private final boolean lastWillAlways;
    private final String firstWillTopic;
    private final byte[] firstWillPayload;
    private final int recoveryChunkSize;
    private final int recoveryDelay;
    private final boolean webSocket;

    public MqttSettings(String host, int port, String username, String password, boolean ssl, long maxBufferSize,
            long maxFileSize, int maxFileCount, int connectionRetryInterval, int connectionAliveInterval,
            String persistenceDirectory) {
        this(host, port, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount, connectionRetryInterval,
                connectionAliveInterval, persistenceDirectory, "", "".getBytes(), false, "", "".getBytes(), false);
    }

    public MqttSettings(String host, int port, String username, String password, boolean ssl, long maxBufferSize,
            long maxFileSize, int maxFileCount, int connectionRetryInterval, int connectionAliveInterval,
            String persistenceDirectory, boolean webSocket) {
        this(host, port, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount, connectionRetryInterval,
                connectionAliveInterval, persistenceDirectory, "", "".getBytes(), false, "", "".getBytes(), webSocket);
    }

    public MqttSettings(String host, int port, String username, String password, boolean ssl, long maxBufferSize,
            long maxFileSize, int maxFileCount, int connectionRetryInterval, int connectionAliveInterval,
            String persistenceDirectory, String lastWillTopic, byte[] lastWillPayload, boolean lastWillAlways,
            String firstWillTopic, byte[] firstWillPayload, boolean webSocket) {
        this(host, port, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount, connectionRetryInterval,
                connectionAliveInterval, persistenceDirectory, lastWillTopic, lastWillPayload, lastWillAlways,
                firstWillTopic, firstWillPayload, 0, 0, webSocket);
    }

    public MqttSettings(String host, int port, String username, String password, boolean ssl, long maxBufferSize,
            long maxFileSize, int maxFileCount, int connectionRetryInterval, int connectionAliveInterval,
            String persistenceDirectory, String lastWillTopic, byte[] lastWillPayload, boolean lastWillAlways,
            String firstWillTopic, byte[] firstWillPayload, int recoveryChunkSize, int recoveryDelay,
            boolean webSocket) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.webSocket = webSocket;
        this.maxBufferSize = maxBufferSize;
        this.maxFileSize = maxFileSize;
        this.maxFileCount = maxFileCount;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectionAliveInterval = connectionAliveInterval;
        this.persistenceDirectory = persistenceDirectory;
        this.lastWillTopic = lastWillTopic;
        this.lastWillPayload = lastWillPayload;
        this.lastWillAlways = lastWillAlways;
        this.firstWillTopic = firstWillTopic;
        this.firstWillPayload = firstWillPayload;
        this.recoveryChunkSize = recoveryChunkSize;
        this.recoveryDelay = recoveryDelay;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    /**
     * @return maximum buffer size in Kibibytes
     */
    public long getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * @return maximum file buffer size in Kibibytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    public int getMaxFileCount() {
        return maxFileCount;
    }

    public long getConnectionRetryInterval() {
        return connectionRetryInterval;
    }

    public int getConnectionAliveInterval() {
        return connectionAliveInterval;
    }

    public String getPersistenceDirectory() {
        return persistenceDirectory;
    }

    public String getLastWillTopic() {
        return lastWillTopic;
    }

    public byte[] getLastWillPayload() {
        return lastWillPayload;
    }

    public boolean isLastWillSet() {
        return !lastWillTopic.equals("") && lastWillPayload.length != 0;
    }

    public boolean isLastWillAlways() {
        return lastWillAlways && isLastWillSet();
    }

    public String getFirstWillTopic() {
        return firstWillTopic;
    }

    public byte[] getFirstWillPayload() {
        return firstWillPayload;
    }

    public boolean isFirstWillSet() {
        return !firstWillTopic.equals("") && lastWillPayload.length != 0;
    }

    public boolean isRecoveryLimitSet() {
        return recoveryChunkSize > 0 && recoveryDelay > 0;
    }

    public int getRecoveryChunkSize() {
        return recoveryChunkSize;
    }

    public int getRecoveryDelay() {
        return recoveryDelay;
    }

    public boolean isWebSocket() {
        return webSocket;
    }

    /**
     * Returns a string of all settings, always uses '*****' as password string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host=").append(getHost()).append("\n");
        sb.append("port=").append(getPort()).append("\n");
        sb.append("username=").append(getUsername()).append("\n");
        sb.append("password=").append("*****").append("\n");
        sb.append("ssl=").append(isSsl()).append("\n");
        sb.append("webSocket=").append(isWebSocket());
        sb.append("persistenceDirectory=").append(getPersistenceDirectory()).append("\n");
        sb.append("maxBufferSize=").append(getMaxBufferSize()).append("\n");
        sb.append("maxFileCount=").append(getMaxFileCount()).append("\n");
        sb.append("maxFileSize=").append(getMaxFileSize()).append("\n");
        sb.append("connectionRetryInterval=").append(getConnectionRetryInterval()).append("\n");
        sb.append("connectionAliveInterval=").append(getConnectionAliveInterval()).append("\n");
        sb.append("lastWillTopic=").append(getLastWillTopic()).append("\n");
        sb.append("lastWillPayload=").append(new String(getLastWillPayload())).append("\n");
        sb.append("lastWillAlways=").append(isLastWillAlways()).append("\n");
        sb.append("firstWillTopic=").append(getFirstWillTopic()).append("\n");
        sb.append("firstWillPayload=").append(new String(getFirstWillPayload()));
        sb.append("recoveryChunkSize=").append(getRecoveryChunkSize()).append("\n");
        sb.append("recoveryDelay=").append(getRecoveryDelay()).append("\n");
        return sb.toString();
    }
}
