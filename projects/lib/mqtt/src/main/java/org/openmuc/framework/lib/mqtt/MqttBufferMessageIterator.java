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

import java.util.Iterator;

import org.openmuc.framework.lib.filePersistence.FilePersistence;

public class MqttBufferMessageIterator implements Iterator<MessageTuple> {

    private final FilePersistence filePersistence;
    private final String buffer;

    public MqttBufferMessageIterator(String buffer, FilePersistence filePersistence) {
        this.buffer = buffer;
        this.filePersistence = filePersistence;
    }

    @Override
    public boolean hasNext() {
        return filePersistence.fileExistsFor(buffer);
    }

    @Override
    public MessageTuple next() {
        byte[] message;
        synchronized (filePersistence) {
            message = filePersistence.getMessage(buffer);
        }
        return new MessageTuple(buffer, message);
    }
}
