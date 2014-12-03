/*
 * Copyright 2011-14 Fraunhofer ISE
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

import java.nio.ByteBuffer;

public enum ValueType {
    DOUBLE, FLOAT, LONG, INTEGER, SHORT, BYTE, BOOLEAN, BYTE_ARRAY, STRING;

    public static Value newValue(ValueType valueType, byte[] concatenate) {
        if (valueType == DOUBLE)
            return new DoubleValue(ByteBuffer.wrap(concatenate).getDouble());

        if (valueType == FLOAT)
            return new FloatValue(ByteBuffer.wrap(concatenate).getFloat());

        if (valueType == LONG)
            return new LongValue(ByteBuffer.wrap(concatenate).getLong());

        if (valueType == INTEGER)
            return new IntValue(ByteBuffer.wrap(concatenate).getInt());

        if (valueType == SHORT)
            return new ShortValue(ByteBuffer.wrap(concatenate).getShort());

        if (valueType == BYTE)
            return new ByteValue(concatenate[0]);

        if (valueType == BOOLEAN) {
            boolean value = (concatenate == null || concatenate.length == 0) ?
                            false :
                            concatenate[0] != 0x00;
            return new BooleanValue(value);
        }

        if (valueType == BYTE_ARRAY)
            return new ByteArrayValue(concatenate);

        if (valueType == STRING)
            return new StringValue(new String(concatenate));

        return null;
    }

}
