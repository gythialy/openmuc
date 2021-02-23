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

import com.rabbitmq.client.*;
import org.openmuc.framework.lib.ssl.SslManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

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

    /**
     * A connection to an AMQP broker
     *
     * @param settings connection details {@link AmqpSettings}
     * @throws IOException      when connection fails
     * @throws TimeoutException when connection fails due time out
     */
    public AmqpConnection(AmqpSettings settings) throws IOException, TimeoutException {
        this.settings = settings;
        ConnectionFactory factory;

        /*
         * #88 if (settings.isSsl()) { SslManager.getInstance().listenForConfigChange(this::sslUpdate); }
         */

        factory = getConnectionFactoryForSsl(settings);

        try {
            connect(settings, factory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ConnectionFactory getConnectionFactoryForSsl(AmqpSettings settings) {
        ConnectionFactory factory = new ConnectionFactory();
        if (settings.isSsl()) {
            factory.useSslProtocol(SslManager.getInstance().getSslContext());
            factory.enableHostnameVerification();
        }
        factory.setHost(settings.getHost());
        factory.setPort(settings.getPort());
        factory.setVirtualHost(settings.getVirtualHost());
        factory.setUsername(settings.getUsername());
        factory.setPassword(settings.getPassword());
        return factory;
    }

    private void connect(AmqpSettings settings, ConnectionFactory factory) throws IOException, TimeoutException {

        connection = factory.newConnection();
        addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                logger.debug("Connection recovery completed");
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
                logger.debug("Connection recovery started");
            }
        });

        channel = connection.createChannel();
        exchange = settings.getExchange();
        channel.exchangeDeclare(exchange, "topic", true);

        if (logger.isTraceEnabled()) {
            logger.trace("Connected to {}:{} on virtualHost {} as user {}", settings.getHost(), settings.getPort(),
                    settings.getVirtualHost(), settings.getPort());
        }
    }

    private void sslUpdate() {
        logger.warn("SSL configuration changed, reconnecting.");
        disconnect();
        ConnectionFactory factory = getConnectionFactoryForSsl(settings);
        try {
            connect(settings, factory);
            for (RecoveryListener listener : recoveryListeners) {
                ((Recoverable) connection).addRecoveryListener(listener);
            }
            for (AmqpReader reader : readers) {
                reader.resubscribe();
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Reconnection failed. Reason: {}", e.getMessage());
        }
        logger.warn("Reconnection completed.");
    }

    /**
     * Close the channel and connection
     */
    public void disconnect() {
        if (channel == null || connection == null)
            return;
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
     * @param queue the queue that should be declared
     * @throws IOException if an I/O problem is encountered
     */
    public void declareQueue(String queue) throws IOException {
        if (!DECLARED_QUEUES.contains(queue)) {
            try {
                channel.queueDeclarePassive(queue);
                channel.queueBind(queue, exchange, queue);
                DECLARED_QUEUES.add(queue);
            } catch (Exception e) {
                logger.debug("Channel not found, start to create it...");
                initDeclare(queue);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Queue {} declared", queue);
            }
        }
    }

    void addRecoveryListener(RecoveryListener listener) {
        recoveryListeners.add(listener);
        ((Recoverable) connection).addRecoveryListener(listener);
    }

    void addReader(AmqpReader reader) {
        readers.add(reader);
    }

    private void initDeclare(String queue) throws IOException {
        channel = connection.createChannel();
        channel.exchangeDeclare(exchange, "topic", true);
        channel.queueDeclare(queue, true, false, false, null);
    }

    public String getExchange() {
        return exchange;
    }

    public Channel getRabbitMqChannel() {
        return channel;
    }
}
