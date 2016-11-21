/*
 * Copyright 2011-16 Fraunhofer ISE
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

/**
 * A Record may represent a reading or a database entry. Record is immutable. It contains a value, a timestamp, and a
 * flag.
 */
public class Record {

    private final Long timestamp;
    private final Flag flag;
    private final Value value;

    public Record(Value value, Long timestamp, Flag flag) {
        this.value = value;
        this.timestamp = timestamp;
        if (value == null && flag.equals(Flag.VALID)) {
            throw new IllegalStateException("If a record's flag is set valid the value may not be NULL.");
        }
        this.flag = flag;
    }

    /**
     * Creates a valid record.
     * 
     * @param value
     *            the value of the record
     * @param timestamp
     *            the timestamp of the record
     */
    public Record(Value value, Long timestamp) {
        this(value, timestamp, Flag.VALID);
    }

    /**
     * Creates an invalid record with the given flag. The flag may not indicate valid.
     * 
     * @param flag
     *            the flag of the invalid record.
     */
    public Record(Flag flag) {
        this(null, null, flag);
        if (flag == Flag.VALID) {
            throw new IllegalArgumentException("flag must indicate an error");
        }
    }

    public Value getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Flag getFlag() {
        return flag;
    }

    @Override
    public String toString() {
        return "value: " + value + "; timestamp: " + timestamp + "; flag: " + flag;
    }

}
