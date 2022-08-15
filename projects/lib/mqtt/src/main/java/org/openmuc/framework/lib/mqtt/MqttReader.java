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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3SubscribeBuilder;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;

public class MqttReader {
    private static final Logger logger = LoggerFactory.getLogger(MqttReader.class);
    private final MqttConnection connection;
    private boolean connected = false;
    private final List<SubscribeListenerTuple> subscribes = new LinkedList<>();
    private final String pid;

    /**
     * Note that the connect method of the connection should be called after the Writer got instantiated.
     *
     * @param connection
     *            the {@link MqttConnection} this Writer should use
     * @param pid
     *            an id which is preceding every log call
     */
    public MqttReader(MqttConnection connection, String pid) {
        this.connection = connection;
        this.pid = pid;
        addConnectedListener(connection);
        addDisconnectedListener(connection);
    }

    private void addDisconnectedListener(MqttConnection connection) {
        connection.addDisconnectedListener(context -> {
            if (context.getReconnector().isReconnect()) {
                if (connected) {
                    warn("Disconnected! {}", context.getCause().getMessage());
                }
                else {
                    warn("Reconnect failed! Reason: {}", context.getCause().getMessage());
                }
                connected = false;
            }
        });
    }

    private void addConnectedListener(MqttConnection connection) {
        connection.addConnectedListener(context -> {
            for (SubscribeListenerTuple tuple : subscribes) {
                subscribe(tuple.subscribe, tuple.listener);
            }
            connected = true;
            log("Connected to {}:{}", context.getClientConfig().getServerHost(),
                    context.getClientConfig().getServerPort());
        });
    }

    /**
     * Listens on all topics and notifies the listener when a new message on one of the topics comes in
     *
     * @param topics
     *            List with topic string to listen on
     * @param listener
     *            listener which gets notified of new messages coming in
     */
    public void listen(List<String> topics, MqttMessageListener listener) {
        Mqtt3Subscribe subscribe = buildSubscribe(topics);

        if (subscribe == null) {
            error("No topic given to listen on");
            return;
        }

        if (connected) {
            subscribe(subscribe, listener);
        }
        subscribes.add(new SubscribeListenerTuple(subscribe, listener));
    }

    private void subscribe(Mqtt3Subscribe subscribe, MqttMessageListener listener) {
        this.connection.getClient().subscribe(subscribe, mqtt3Publish -> {
            listener.newMessage(mqtt3Publish.getTopic().toString(), mqtt3Publish.getPayloadAsBytes());
            if (logger.isTraceEnabled()) {
                trace("Message on topic {} received, payload: {}", mqtt3Publish.getTopic().toString(),
                        new String(mqtt3Publish.getPayloadAsBytes()));
            }
        });
    }

    private Mqtt3Subscribe buildSubscribe(List<String> topics) {
        Mqtt3SubscribeBuilder subscribeBuilder = Mqtt3Subscribe.builder();
        Mqtt3Subscribe subscribe = null;
        for (String topic : topics) {
            Mqtt3Subscription subscription = Mqtt3Subscription.builder().topicFilter(topic).build();
            // last topic, build the subscribe object
            if (topics.get(topics.size() - 1).equals(topic)) {
                subscribe = subscribeBuilder.addSubscription(subscription).build();
                break;
            }
            subscribeBuilder.addSubscription(subscription);
        }
        return subscribe;
    }

    private static class SubscribeListenerTuple {
        private final Mqtt3Subscribe subscribe;
        private final MqttMessageListener listener;

        private SubscribeListenerTuple(Mqtt3Subscribe subscribe, MqttMessageListener listener) {
            this.subscribe = subscribe;
            this.listener = listener;
        }
    }

    private void log(String message, Object... args) {
        message = MessageFormatter.arrayFormat(message, args).getMessage();
        logger.info("[{}] {}", pid, message);
    }

    private void debug(String message, Object... args) {
        message = MessageFormatter.arrayFormat(message, args).getMessage();
        logger.debug("[{}] {}", pid, message);
    }

    private void warn(String message, Object... args) {
        message = MessageFormatter.arrayFormat(message, args).getMessage();
        logger.warn("[{}] {}", pid, message);
    }

    private void error(String message, Object... args) {
        message = MessageFormatter.arrayFormat(message, args).getMessage();
        logger.error("[{}] {}", pid, message);
    }

    private void trace(String message, Object... args) {
        message = MessageFormatter.arrayFormat(message, args).getMessage();
        logger.trace("[{}] {}", pid, message);
    }
}
