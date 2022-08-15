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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openmuc.framework.security.SslManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;

/**
 * Represents a connection to a MQTT broker
 */
public class MqttConnection {
    private static final Logger logger = LoggerFactory.getLogger(MqttConnection.class);
    private final MqttSettings settings;
    private final AtomicBoolean cancelReconnect = new AtomicBoolean(false);

    private final List<MqttClientConnectedListener> connectedListeners = new ArrayList<>();
    private final List<MqttClientDisconnectedListener> disconnectedListeners = new ArrayList<>();

    private boolean sslReady = false;

    private Mqtt3ClientBuilder clientBuilder;
    private Mqtt3AsyncClient client;

    private SslManagerInterface sslManager = null;

    /**
     * A connection to a MQTT broker
     *
     * @param settings
     *            connection details {@link MqttSettings}
     */
    public MqttConnection(MqttSettings settings) {
        this.settings = settings;
        clientBuilder = getClientBuilder();
        client = buildClient();
    }

    public boolean isReady() {
        if (settings.isSsl()) {
            return sslReady;
        }
        return true;
    }

    private void sslUpdate() {
        logger.warn("SSL configuration changed, reconnecting.");
        cancelReconnect.set(true);
        sslReady = true;
        client.disconnect().whenComplete((ack, e) -> {
            clientBuilder.sslConfig(getSslConfig());
            clientBuilder.identifier(UUID.randomUUID().toString());
            connect();
        });
    }

    private Mqtt3Connect getConnect() {
        Mqtt3ConnectBuilder connectBuilder = Mqtt3Connect.builder();
        connectBuilder.keepAlive(settings.getConnectionAliveInterval());
        if (settings.isLastWillSet()) {
            connectBuilder.willPublish()
                    .topic(settings.getLastWillTopic())
                    .payload(settings.getLastWillPayload())
                    .applyWillPublish();
        }
        if (settings.getUsername() != null) {
            connectBuilder.simpleAuth()
                    .username(settings.getUsername())
                    .password(settings.getPassword().getBytes())
                    .applySimpleAuth();
        }
        return connectBuilder.build();
    }

    /**
     * Connect to the MQTT broker
     */
    public void connect() {
        client = buildClient();
        String uuid = client.getConfig().getClientIdentifier().toString();
        LocalDateTime time = LocalDateTime.now();
        client.connect(getConnect()).whenComplete((ack, e) -> {
            if (e != null && uuid.equals(client.getConfig().getClientIdentifier().toString())) {
                logger.error("Error with connection initiated at {}: {}", time, e.getMessage());
            }
        });
    }

    /**
     * Disconnect from the MQTT broker
     */
    public void disconnect() {
        if (settings.isLastWillAlways()) {
            client.publishWith()
                    .topic(settings.getLastWillTopic())
                    .payload(settings.getLastWillPayload())
                    .send()
                    .whenComplete((publish, e) -> {
                        client.disconnect();
                    });
        }
        else {
            client.disconnect();
        }
    }

    void addConnectedListener(MqttClientConnectedListener listener) {
        if (clientBuilder == null) {
            connectedListeners.add(listener);
        }
        else {
            clientBuilder.addConnectedListener(listener);
            if (!connectedListeners.contains(listener)) {
                connectedListeners.add(listener);
            }
        }
    }

    void addDisconnectedListener(MqttClientDisconnectedListener listener) {
        if (clientBuilder == null) {
            disconnectedListeners.add(listener);
        }
        else {
            clientBuilder.addDisconnectedListener(listener);
            if (!disconnectedListeners.contains(listener)) {
                disconnectedListeners.add(listener);
            }
        }
    }

    Mqtt3AsyncClient getClient() {
        return client;
    }

    /**
     * @return the settings {@link MqttSettings} this connection was constructed with
     */
    public MqttSettings getSettings() {
        return settings;
    }

    private Mqtt3ClientBuilder getClientBuilder() {
        Mqtt3ClientBuilder clientBuilder = Mqtt3Client.builder()
                .identifier(UUID.randomUUID().toString())
                .automaticReconnect()
                .initialDelay(settings.getConnectionRetryInterval(), TimeUnit.SECONDS)
                .maxDelay(settings.getConnectionRetryInterval(), TimeUnit.SECONDS)
                .applyAutomaticReconnect()
                .serverHost(settings.getHost())
                .serverPort(settings.getPort());
        if (settings.isSsl() && sslManager != null) {
            clientBuilder.sslConfig(getSslConfig());
        }
        if (settings.isWebSocket()) {
            clientBuilder.webSocketWithDefaultConfig();
        }
        return clientBuilder;
    }

    private MqttClientSslConfig getSslConfig() {
        return MqttClientSslConfig.builder()
                .keyManagerFactory(sslManager.getKeyManagerFactory())
                .trustManagerFactory(sslManager.getTrustManagerFactory())
                .handshakeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    private Mqtt3AsyncClient buildClient() {
        return clientBuilder.buildAsync();
    }

    public void setSslManager(SslManagerInterface instance) {
        if (!settings.isSsl()) {
            return;
        }
        sslManager = instance;
        clientBuilder = getClientBuilder();
        for (MqttClientConnectedListener listener : connectedListeners) {
            addConnectedListener(listener);
        }
        connectedListeners.clear();
        for (MqttClientDisconnectedListener listener : disconnectedListeners) {
            addDisconnectedListener(listener);
        }
        disconnectedListeners.clear();
        sslManager.listenForConfigChange(this::sslUpdate);
        addDisconnectedListener(context -> {
            if (cancelReconnect.getAndSet(false)) {
                context.getReconnector().reconnect(false);
            }
            else if (context.getReconnector().getAttempts() >= 3) {
                logger.debug("Renewing client");
                context.getReconnector().reconnect(false);
                clientBuilder.identifier(UUID.randomUUID().toString());
                connect();
            }
        });
        client = buildClient();
        if (sslManager.isLoaded()) {
            sslReady = true;
        }
    }
}
