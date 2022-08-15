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
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;

/**
 * Gets (reads) messages from an AmqpConnection
 */
public class AmqpReader {
    private final Logger logger = LoggerFactory.getLogger(AmqpReader.class);
    private final AmqpConnection connection;
    private final List<Listener> listeners = new ArrayList<>();

    /**
     * @param connection
     *            an instance of {@link AmqpConnection}
     */
    public AmqpReader(AmqpConnection connection) {
        connection.addReader(this);
        this.connection = connection;
    }

    /**
     * get a message from the specified queue
     *
     * @param queue
     *            the queue from which to pull a message
     * @return byte array containing the received message, null if no message was received
     */
    public byte[] read(String queue) {
        try {
            connection.declareQueue(queue);
        } catch (IOException e) {
            logger.error("Declaring queue failed: {}", e.getMessage());
            return null;
        }

        GetResponse response;
        try {
            response = connection.getRabbitMqChannel().basicGet(queue, true);
        } catch (IOException e) {
            logger.error("Could not receive message: {}", e.getMessage());
            return null;
        }

        if (response == null) {
            // no message received, queue empty
            return null;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("message on queue {} received, payload: {}", queue, new String(response.getBody()));
        }

        return response.getBody();
    }

    /**
     * get messages from specified queues and send them to the specified {@link AmqpMessageListener}
     *
     * @param queues
     *            String collection with queues to receive messages via push
     * @param listener
     *            received messages are sent to this listener
     */
    public void listen(Collection<String> queues, AmqpMessageListener listener) {
        listeners.add(new Listener(queues, listener));
        for (String queue : queues) {
            DeliverCallback deliverCallback = (consumerTag, message) -> {
                listener.newMessage(queue, message.getBody());
                if (logger.isTraceEnabled()) {
                    logger.trace("message on queue {} received, payload: {}", queue, new String(message.getBody()));
                }
            };

            if (connection.isConnected()) {
                try {
                    connection.declareQueue(queue);
                } catch (IOException e) {
                    logger.error("Declaring queue failed: {}", e.getMessage());
                    continue;
                }

                try {
                    connection.getRabbitMqChannel().basicConsume(queue, true, deliverCallback, consumerTag -> {
                    });
                } catch (IOException e) {
                    logger.error("Could not subscribe for messages: {}", e.getMessage());
                }
            }
        }
    }

    void resubscribe() {
        List<Listener> listenersCopy = new ArrayList<>(listeners);
        listeners.clear();
        for (Listener listener : listenersCopy) {
            listen(listener.queues, listener.listener);
        }
    }

    private static class Listener {
        private final Collection<String> queues;
        private final AmqpMessageListener listener;

        private Listener(Collection<String> queues, AmqpMessageListener listener) {
            this.queues = queues;
            this.listener = listener;
        }
    }
}
