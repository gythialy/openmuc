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

import java.nio.charset.Charset;

public class StringValue implements Value {

    private final String value;

    private static final Charset charset = Charset.forName("US-ASCII");

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public double asDouble() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public float asFloat() {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public long asLong() {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public int asInt() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public short asShort() {
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public byte asByte() {
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException();
        }
    }

    @Override
    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    @Override
    public byte[] asByteArray() {
        return value.getBytes(charset);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String asString() {
        return toString();
    }

}
