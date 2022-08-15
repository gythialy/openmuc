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

package org.openmuc.framework.data;

public class BooleanValue implements Value {

    private final boolean value;

    public BooleanValue(boolean value) {
        this.value = value;
    }

    public BooleanValue(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    @Override
    public double asDouble() {
        return asByte();
    }

    @Override
    public float asFloat() {
        return asByte();
    }

    @Override
    public long asLong() {
        return asByte();
    }

    @Override
    public int asInt() {
        return asByte();
    }

    @Override
    public short asShort() {
        return asByte();
    }

    @Override
    public byte asByte() {
        if (value) {
            return 1;
        }
        else {
            return 0;
        }
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public byte[] asByteArray() {
        return new byte[] { asByte() };
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public String asString() {
        return toString();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.BOOLEAN;
    }
}
