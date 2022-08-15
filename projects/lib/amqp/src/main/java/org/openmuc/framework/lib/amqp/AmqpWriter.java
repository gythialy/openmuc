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

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;

/**
 * Sends (writes) messages to an AmqpConnection
 */
public class AmqpWriter {
    private static final Logger logger = LoggerFactory.getLogger(AmqpWriter.class);

    private final AmqpBufferHandler bufferHandler;
    private final AmqpConnection connection;
    private final String pid;

    /**
     * @param connection
     *            an instance of {@link AmqpConnection}
     * @param pid
     *            pid for log messages
     */
    public AmqpWriter(AmqpConnection connection, String pid) {
        this.connection = connection;
        this.pid = pid;

        AmqpSettings s = connection.getSettings();
        bufferHandler = new AmqpBufferHandler(s.getMaxBufferSize(), s.getMaxFileCount(), s.getMaxFileSize(),
                s.getPersistenceDirectory());

        connection.addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                emptyFileBuffer();
                emptyRAMBuffer();
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
            }
        });

        if (connection.isConnected()) {
            emptyFileBuffer();
            emptyRAMBuffer();
        }
    }

    private void emptyFileBuffer() {
        String[] buffers = bufferHandler.getBuffers();
        logger.debug("[{}] Clearing file buffer.", pid);
        if (buffers.length == 0) {
            logger.debug("[{}] File buffer already empty.", pid);
        }
        for (String buffer : buffers) {
            Iterator<AmqpMessageTuple> iterator = bufferHandler.getMessageIterator(buffer);
            while (iterator.hasNext()) {
                AmqpMessageTuple messageTuple = iterator.next();
                if (logger.isTraceEnabled()) {
                    logger.trace("[{}] Resend from file: {}", pid, new String(messageTuple.getMessage()));
                }
                write(messageTuple.getRoutingKey(), messageTuple.getMessage());
            }
        }
        logger.debug("[{}] File buffer cleared.", pid);
    }

    private void emptyRAMBuffer() {
        logger.debug("[{}] Clearing RAM buffer.", pid);
        if (bufferHandler.isEmpty()) {
            logger.debug("[{}] RAM buffer already empty.", pid);
        }
        while (!bufferHandler.isEmpty()) {
            AmqpMessageTuple messageTuple = bufferHandler.removeNextMessage();
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] Resend from memory: {}", pid, new String(messageTuple.getMessage()));
            }
            write(messageTuple.getRoutingKey(), messageTuple.getMessage());
        }
        logger.debug("[{}] RAM buffer cleared.", pid);
    }

    /**
     * Publish a message with routing key, when failing the message is buffered and republished on recovery
     *
     * @param routingKey
     *            the routingKey with which to publish the message
     * @param message
     *            byte array containing the message to be published
     */
    public void write(String routingKey, byte[] message) {
        if (!publish(routingKey, message)) {
            bufferHandler.add(routingKey, message);
        }
    }

    private boolean publish(String routingKey, byte[] message) {
        try {
            connection.declareQueue(routingKey);
            connection.getRabbitMqChannel().basicPublish(connection.getExchange(), routingKey, false, null, message);
        } catch (Exception e) {
            logger.error("[{}] Could not publish message: {}", pid, e.getMessage());
            return false;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("[{}] published with routingKey {}, payload: {}", pid, routingKey, new String(message));
        }
        return true;
    }

    public void shutdown() {
        logger.debug("[{}] Saving buffers.", pid);
        bufferHandler.persist();
    }
}
