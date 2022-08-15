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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.openmuc.framework.lib.filePersistence.FilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpBufferHandler {

    private static final Logger logger = LoggerFactory.getLogger(AmqpBufferHandler.class);

    private final Queue<AmqpMessageTuple> buffer = new LinkedList<>();
    private final long maxBufferSizeBytes;
    private final int maxFileCount;
    private final FilePersistence filePersistence;

    private long currentBufferSize = 0L;

    public AmqpBufferHandler(long maxBufferSize, int maxFileCount, long maxFileSize, String persistenceDir) {
        maxBufferSizeBytes = maxBufferSize * 1024;
        this.maxFileCount = maxFileCount;
        if (isFileBufferEnabled()) {
            filePersistence = new FilePersistence(persistenceDir, maxFileCount, maxFileSize);
        }
        else {
            filePersistence = null;
        }
    }

    private boolean isFileBufferEnabled() {
        return maxFileCount > 0 && maxBufferSizeBytes > 0;
    }

    public void add(String routingKey, byte[] message) {
        if (isBufferTooFull(message)) {
            handleFull(routingKey, message);
        }
        else {
            synchronized (buffer) {
                buffer.add(new AmqpMessageTuple(routingKey, message));
                currentBufferSize += message.length;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("maxBufferSize = {} B, currentBufferSize = {} B, messageSize = {} B", maxBufferSizeBytes,
                        currentBufferSize, message.length);
            }
        }
    }

    private boolean isBufferTooFull(byte[] message) {
        return currentBufferSize + message.length > maxBufferSizeBytes;
    }

    private void handleFull(String routingKey, byte[] message) {
        if (isFileBufferEnabled()) {
            addToFilePersistence();
            add(routingKey, message);
        }
        else if (message.length <= maxBufferSizeBytes) {
            removeNextMessage();
            add(routingKey, message);
        }
    }

    public AmqpMessageTuple removeNextMessage() {
        AmqpMessageTuple removedMessage;
        synchronized (buffer) {
            removedMessage = buffer.remove();
            currentBufferSize -= removedMessage.getMessage().length;
        }
        return removedMessage;
    }

    private void addToFilePersistence() {
        logger.debug("moving buffered messages from RAM to file");
        while (!isEmpty()) {
            AmqpMessageTuple messageTuple = removeNextMessage();
            writeBufferToFile(messageTuple);
        }
        currentBufferSize = 0;
    }

    private void writeBufferToFile(AmqpMessageTuple messageTuple) {
        try {
            synchronized (filePersistence) {
                filePersistence.writeBufferToFile(messageTuple.getRoutingKey(), messageTuple.getMessage());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public String[] getBuffers() {
        String[] buffers;
        if (isFileBufferEnabled()) {
            buffers = filePersistence.getBuffers();
        }
        else {
            buffers = new String[] {};
        }
        return buffers;
    }

    public Iterator<AmqpMessageTuple> getMessageIterator(String buffer) {
        return new AmqpBufferMessageIterator(buffer, filePersistence);
    }

    public void persist() {
        if (isFileBufferEnabled()) {
            try {
                filePersistence.restructure();
                addToFilePersistence();
            } catch (IOException e) {
                logger.error("Buffer file restructuring error: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
