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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.security.SslManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Represents a connection to an AMQP broker
 */
public class AmqpConnection {

    private static final Logger logger = LoggerFactory.getLogger(AmqpConnection.class);
    private static final List<String> DECLARED_QUEUES = new ArrayList<>();

    private final List<RecoveryListener> recoveryListeners = new ArrayList<>();
    private final List<AmqpReader> readers = new ArrayList<>();
    private final AmqpSettings settings;
    private String exchange;
    private Connection connection;
    private Channel channel;
    private SslManagerInterface sslManager;
    private boolean connected = false;

    /**
     * A connection to an AMQP broker
     *
     * @param settings
     *            connection details {@link AmqpSettings}
     * @throws IOException
     *             when connection fails
     * @throws TimeoutException
     *             when connection fails due time out
     */
    public AmqpConnection(AmqpSettings settings) throws IOException, TimeoutException {
        this.settings = settings;

        if (!settings.isSsl()) {
            logger.info("Starting amqp connection without ssl");
            ConnectionFactory factory = getConnectionFactoryForSsl(settings);

            try {
                connect(settings, factory);
            } catch (Exception e) {
                logger.error("Connection could not be created: {}", e.getMessage());
            }
        }
    }

    private ConnectionFactory getConnectionFactoryForSsl(AmqpSettings settings) {
        ConnectionFactory factory = new ConnectionFactory();
        if (settings.isSsl()) {
            factory.useSslProtocol(sslManager.getSslContext());
            factory.enableHostnameVerification();
        }
        factory.setHost(settings.getHost());
        factory.setPort(settings.getPort());
        factory.setVirtualHost(settings.getVirtualHost());
        factory.setUsername(settings.getUsername());
        factory.setPassword(settings.getPassword());
        factory.setExceptionHandler(new AmqpExceptionHandler());
        factory.setRequestedHeartbeat(settings.getConnectionAliveInterval());
        return factory;
    }

    private void connect(AmqpSettings settings, ConnectionFactory factory) throws IOException {
        establishConnection(factory);

        if (connection == null) {
            logger.warn("Created connection is null, check your config\n{}", settings);
            return;
        }

        connected = true;
        logger.info("Connection established successfully!");

        addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                logger.debug("Connection recovery completed");
                connected = true;
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
                logger.debug("Connection recovery started");
                connected = false;
            }
        });

        channel = connection.createChannel();
        exchange = settings.getExchange();
        channel.exchangeDeclare(exchange, "topic", true);

        if (logger.isTraceEnabled()) {
            logger.trace("Connected to {}:{} on virtualHost {} as user {}", settings.getHost(), settings.getPort(),
                    settings.getVirtualHost(), settings.getUsername());
        }
    }

    private void establishConnection(ConnectionFactory factory) {
        try {
            connection = factory.newConnection();
        } catch (Exception e) {
            logger.error("Error at creation of new connection: {}", e.getMessage());
        }
    }

    private void sslUpdate() {
        logger.warn("SSL configuration changed, reconnecting.");
        disconnect();
        ConnectionFactory factory = getConnectionFactoryForSsl(settings);
        try {
            connect(settings, factory);
            if (connection == null) {
                logger.error("connection after calling ssl update is null");
                return;
            }
            for (RecoveryListener listener : recoveryListeners) {
                ((Recoverable) connection).addRecoveryListener(listener);
                listener.handleRecovery((Recoverable) connection);
            }
            for (AmqpReader reader : readers) {
                reader.resubscribe();
            }
        } catch (IOException e) {
            logger.error("Reconnection failed. Reason: {}", e.getMessage());
        }
        logger.warn("Reconnection completed.");
    }

    /**
     * Close the channel and connection
     */
    public void disconnect() {
        if (channel == null || connection == null) {
            return;
        }
        try {
            channel.close();
            connection.close();
            if (logger.isTraceEnabled()) {
                logger.trace("Successfully disconnected");
            }
        } catch (IOException | TimeoutException | ShutdownSignalException e) {
            logger.error("failed to close connection: {}", e.getMessage());
        }
    }

    /**
     * Declares the passed queue as a durable queue
     *
     * @param queue
     *            the queue that should be declared
     * @throws IOException
     *             if an I/O problem is encountered
     */
    public void declareQueue(String queue) throws IOException {
        if (!DECLARED_QUEUES.contains(queue)) {
            try {
                channel.queueDeclarePassive(queue);
                channel.queueBind(queue, exchange, queue);
                DECLARED_QUEUES.add(queue);
                if (logger.isTraceEnabled()) {
                    logger.trace("Queue {} declared", queue);
                }
            } catch (Exception e) {
                logger.debug("Channel {} not found, start to create it...", queue);
                initDeclare(queue);
            }
        }
    }

    void addRecoveryListener(RecoveryListener listener) {
        recoveryListeners.add(listener);
        if (connection == null) {
            return;
        }
        ((Recoverable) connection).addRecoveryListener(listener);
    }

    void addReader(AmqpReader reader) {
        readers.add(reader);
    }

    private void initDeclare(String queue) throws IOException {
        if (connection == null) {
            logger.error("declaring queue stopped, because connection to broker is null");
            return;
        }
        try {
            channel = connection.createChannel();
        } catch (Exception e) {
            logger.error("Queue {} could not be declared.", queue);
            return;
        }
        channel.exchangeDeclare(exchange, "topic", true);
        channel.queueDeclare(queue, true, false, false, null);
    }

    public String getExchange() {
        return exchange;
    }

    Channel getRabbitMqChannel() {
        return channel;
    }

    AmqpSettings getSettings() {
        return settings;
    }

    public void setSslManager(SslManagerInterface instance) {
        if (!settings.isSsl()) {
            return;
        }
        sslManager = instance;
        sslManager.listenForConfigChange(this::sslUpdate);
        ConnectionFactory factory = getConnectionFactoryForSsl(settings);

        if (sslManager.isLoaded()) {
            try {
                connect(settings, factory);
            } catch (Exception e) {
                logger.error("Connection with SSL couldn't be created");
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
