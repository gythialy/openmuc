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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.openmuc.framework.lib.filePersistence.FilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffer handler with RAM buffer and managed {@link FilePersistence}
 */
public class MqttBufferHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttBufferHandler.class);

    private final Queue<MessageTuple> buffer = new LinkedList<>();
    private final long maxBufferSizeBytes;
    private long currentBufferSize = 0L;
    private final int maxFileCount;
    private final FilePersistence filePersistence;

    /**
     * Initializes buffers with specified properties.
     *
     * <br>
     * <br>
     * <table border="1" style="text-align: center">
     * <caption>Behaviour summary</caption>
     * <tr>
     * <th>maxBufferSizeKb</th>
     * <th>maxFileCount</th>
     * <th>maxFileSizeKb</th>
     * <th>RAM buffer</th>
     * <th>File buffer</th>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>0</td>
     * <td>0</td>
     * <td>Disabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>0</td>
     * <td>&#62;0</td>
     * <td>Disabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>&#62;0</td>
     * <td>0</td>
     * <td>Disabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>&#62;0</td>
     * <td>&#62;0</td>
     * <td>Disabled</td>
     * <td>Enabled</td>
     * </tr>
     * <tr>
     * <td>&#62;0</td>
     * <td>0</td>
     * <td>0</td>
     * <td>Enabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>&#62;0</td>
     * <td>0</td>
     * <td>&#62;0</td>
     * <td>Enabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>&#62;0</td>
     * <td>&#62;0</td>
     * <td>0</td>
     * <td>Enabled</td>
     * <td>Disabled</td>
     * </tr>
     * <tr>
     * <td>&#62;0</td>
     * <td>&#62;0</td>
     * <td>&#62;0</td>
     * <td>Enabled</td>
     * <td>Enabled</td>
     * </tr>
     * </table>
     *
     * @param maxBufferSizeKb
     *            maximum RAM buffer size in KiB
     * @param maxFileCount
     *            maximum file count used per buffer by {@link FilePersistence}
     * @param maxFileSizeKb
     *            maximum file size used per file by {@link FilePersistence}
     * @param persistenceDirectory
     *            directory in which {@link FilePersistence} stores buffers
     */
    public MqttBufferHandler(long maxBufferSizeKb, int maxFileCount, long maxFileSizeKb, String persistenceDirectory) {
        maxBufferSizeBytes = maxBufferSizeKb * 1024;
        this.maxFileCount = maxFileCount;

        if (isFileBufferEnabled()) {
            filePersistence = new FilePersistence(persistenceDirectory, maxFileCount, maxFileSizeKb);
        }
        else {
            filePersistence = null;
        }
    }

    private boolean isFileBufferEnabled() {
        return maxFileCount > 0 && maxBufferSizeBytes > 0;
    }

    public void add(String topic, byte[] message) {

        if (isBufferTooFull(message)) {
            handleFull(topic, message);
        }
        else {
            synchronized (buffer) {
                buffer.add(new MessageTuple(topic, message));
                currentBufferSize += message.length;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("maxBufferSize = {}, currentBufferSize = {}, messageSize = {}", maxBufferSizeBytes,
                        currentBufferSize, message.length);
            }
        }

    }

    private boolean isBufferTooFull(byte[] message) {
        return currentBufferSize + message.length > maxBufferSizeBytes;
    }

    private void handleFull(String topic, byte[] message) {
        if (isFileBufferEnabled()) {
            addToFilePersistence();
            add(topic, message);
        }
        else if (message.length <= maxBufferSizeBytes) {
            removeNextMessage();
            add(topic, message);
        }
    }

    private void addToFilePersistence() {
        logger.debug("move buffered messages from RAM to file");
        while (!buffer.isEmpty()) {
            MessageTuple messageTuple = removeNextMessage();
            writeBufferToFile(messageTuple);
        }
        currentBufferSize = 0;
    }

    private void writeBufferToFile(MessageTuple messageTuple) {
        try {
            synchronized (filePersistence) {
                filePersistence.writeBufferToFile(messageTuple.topic, messageTuple.message);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public MessageTuple removeNextMessage() {
        MessageTuple removedMessage;
        synchronized (buffer) {
            removedMessage = buffer.remove();
            currentBufferSize -= removedMessage.message.length;
        }
        return removedMessage;
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

    public Iterator<MessageTuple> getMessageIterator(String buffer) {
        return new MqttBufferMessageIterator(buffer, filePersistence);
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
