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

import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Sends (writes) messages to an AmqpConnection
 */
public class AmqpWriter {
    private static final Logger logger = LoggerFactory.getLogger(AmqpWriter.class);

    private final Queue<MessageTuple> messageBuffer = new LinkedList<>();
    private final AmqpConnection connection;

    /**
     * @param connection an instance of {@link AmqpConnection}
     */
    public AmqpWriter(AmqpConnection connection) {
        this.connection = connection;

        connection.addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                while (!messageBuffer.isEmpty()) {
                    MessageTuple messageTuple = messageBuffer.remove();
                    if (logger.isTraceEnabled()) {
                        logger.trace("resending buffered message");
                    }
                    write(messageTuple.routingKey, messageTuple.message);
                }
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
            }
        });
    }

    /**
     * Publish a message with routing key, when failing the message is buffered and republished on recovery
     *
     * @param routingKey the routingKey with which to publish the message
     * @param message    byte array containing the message to be published
     */
    public void write(String routingKey, byte[] message) {
        if (!publish(routingKey, message)) {
            messageBuffer.add(new MessageTuple(routingKey, message));
            logger.debug("Added not published message to message buffer. Size: {}", messageBuffer.size());
        }
    }

    private boolean publish(String routingKey, byte[] message) {
        try {
            connection.declareQueue(routingKey);
            connection.getRabbitMqChannel().basicPublish(connection.getExchange(), routingKey, false, null, message);
        } catch (Exception e) {
            logger.error("Could not publish message: {}", e.getMessage());
            return false;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("published with routingKey {}, payload: {}", routingKey, new String(message));
        }
        return true;
    }

    private static class MessageTuple {
        private final String routingKey;
        private final byte[] message;

        MessageTuple(String routingKey, byte[] message) {
            this.routingKey = routingKey;
            this.message = message;
        }
    }
}
