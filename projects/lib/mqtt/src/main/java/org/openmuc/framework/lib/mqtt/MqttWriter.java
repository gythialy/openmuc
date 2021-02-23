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

package org.openmuc.framework.lib.mqtt;

import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MqttWriter {
    private static final Logger logger = LoggerFactory.getLogger(MqttWriter.class);
    private static final Queue<MessageTuple> MESSAGE_BUFFER = new LinkedList<>();

    private final MqttConnection connection;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final MqttBufferHandler buffer;
    private final String pid;
    private boolean connected = false;
    private LocalDateTime timeOfConnectionLoss;

    public MqttWriter(MqttConnection connection, String pid) {
        this.connection = connection;
        addConnectedListener();
        addDisconnectedListener();
        MqttSettings s = connection.getSettings();
        buffer = new MqttBufferHandler(s.getMaxBufferSize(), s.getMaxFileCount(), s.getMaxFileSize(),
                s.getPersistenceDirectory());
        this.pid = pid;
    }

    private void addConnectedListener() {
        connection.addConnectedListener(context -> {

            // FIXME null checks currently workaround for MqttWriterTest, it is not set there
            String serverHost = "UNKNOWN";
            String serverPort = "UNKNOWN";

            if (context.getClientConfig() != null) {
                serverHost = context.getClientConfig().getServerHost();
                serverPort = String.valueOf(context.getClientConfig().getServerPort());
            }

            log("connected to broker {}:{}", serverHost, serverPort);
            connected = true;

            MqttSettings settings = connection.getSettings();
            if (settings.isFirstWillSet()) {
                write(settings.getFirstWillTopic(), settings.getFirstWillPayload());
            }

            emptyBuffer();
            emptyFileBuffer();

        });
    }

    private void emptyFileBuffer() {

        log("Clearing file buffer.");
        String[] buffers = buffer.getBuffers();
        if (buffers.length == 0) {
            log("File buffer already empty.");
        }
        for (String buffer : buffers) {
            Iterator<MessageTuple> iterator = this.buffer.getMessageIterator(buffer);
            while (iterator.hasNext()) {
                MessageTuple messageTuple = iterator.next();
                if (logger.isTraceEnabled()) {
                    trace("Resend from file: {}", new String(messageTuple.message));
                }
                write(messageTuple.topic, messageTuple.message);
            }
        }

        log("Empty file buffer done.");
    }

    private void emptyBuffer() {
        log("Clearing memory (RAM) buffer.");
        if (buffer.isEmpty()) {
            log("Memory buffer already empty.");
        }
        while (!buffer.isEmpty()) {
            MessageTuple messageTuple = buffer.removeNextMessage();
            if (logger.isTraceEnabled()) {
                trace("Resend from memory: {}", new String(messageTuple.message));
            }
            write(messageTuple.topic, messageTuple.message);
        }
        log("Empty memory buffer done.");
    }

    private void addDisconnectedListener() {
        connection.addDisconnectedListener(context -> {
            if (context.getReconnector().isReconnect()) {
                String serverHost = context.getClientConfig().getServerHost();
                String cause = context.getCause().getMessage();

                if (connected) {
                    handleDisconnect(serverHost, cause);
                } else {
                    handleFailedReconnect(serverHost, cause);
                }
            }
        });
    }

    private void handleFailedReconnect(String serverHost, String cause) {
        if (isInitialConnect()) {
            timeOfConnectionLoss = LocalDateTime.now();
        }
        long d = Duration.between(timeOfConnectionLoss, LocalDateTime.now()).getSeconds() * 1000;
        String duration = sdf.format(new Date(d - TimeZone.getDefault().getRawOffset()));
        warn("Reconnect failed: broker '{}'. Cause: '{}'. Connection lost at: {}, duration {}", serverHost, cause,
                dateFormatter.format(timeOfConnectionLoss), duration);
    }

    private boolean isInitialConnect() {
        return timeOfConnectionLoss == null;
    }

    private void handleDisconnect(String serverHost, String cause) {
        timeOfConnectionLoss = LocalDateTime.now();
        connected = false;
        warn("Connection lost: broker '{}'. Cause: '{}'", serverHost, cause);
    }

    /**
     * Publishes a message to the specified topic
     *
     * @param topic   the topic on which to publish the message
     * @param message the message to be published
     */
    public void write(String topic, byte[] message) {
        if (connected) {
            startPublishing(topic, message);
        } else {
            warn("No connection to broker - adding message to buffer");
            buffer.add(topic, message);
        }
    }

    private void startPublishing(String topic, byte[] message) {
        publish(topic, message).whenComplete((publish, exception) -> {
            if (exception != null) {
                warn("Connection issue: {} message could not be sent. Adding message to buffer",
                        exception.getMessage());
                buffer.add(topic, message);
            }
        });
    }

    CompletableFuture<Mqtt3Publish> publish(String topic, byte[] message) {
        return connection.getClient().publishWith().topic(topic).payload(message).send();
    }

    public MqttConnection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        return connection != null;
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
