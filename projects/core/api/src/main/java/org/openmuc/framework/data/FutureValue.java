/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.data;

import org.openmuc.framework.dataaccess.Channel;

/**
 * Class used to write values in the future.
 * 
 * @see Channel#writeFuture(java.util.List)
 */
public class FutureValue {

    private final Value value;
    private final long writeTime;

    /**
     * Construct a new future value.
     * 
     * @param value
     *            a value.
     * @param writeTime
     *            the write time in the future.
     */
    public FutureValue(Value value, long writeTime) {
        if (writeTime <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Write time must be in the future.");
        }
        this.value = value;
        this.writeTime = writeTime;
    }

    /**
     * The future value.
     * 
     * @return the value.
     */
    public Value getValue() {
        return value;
    }

    /**
     * The write time.
     * 
     * @return the write time.
     */
    public Long getWriteTime() {
        return writeTime;
    }

}
