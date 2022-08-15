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

package org.openmuc.framework.datalogger.sql.utils;

import java.sql.JDBCType;
import java.util.Arrays;
import java.util.List;

import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;

public class SqlValues {

    public static final List<String> COLUMNS = Arrays.asList("channelid", "channelAdress", "loggingInterval",
            "loggingTimeOffset", "unit", "valueType", "scalingFactor", "valueOffset", "listening", "loggingEvent",
            "samplingInterval", "samplingTimeOffset", "samplingGroup", "disabled", "description");
    public static final String POSTGRESQL = "postgresql";
    public static final String POSTGRES = "postgres";
    public static final String MYSQL = "mysql";
    public static final String NULL = ") NULL,";
    public static final String AND = "' AND '";
    public static final String VALUE = "value";
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private SqlValues() {
    }

    public static JDBCType getType(ValueType valueType) {
        switch (valueType) {
        case BOOLEAN:
            return JDBCType.BOOLEAN;
        case BYTE_ARRAY:
            return JDBCType.LONGVARBINARY;
        case DOUBLE:
            return JDBCType.FLOAT;
        case FLOAT:
            return JDBCType.DOUBLE;
        case INTEGER:
            return JDBCType.INTEGER;
        case LONG:
            return JDBCType.BIGINT;
        case SHORT:
            return JDBCType.SMALLINT;
        case BYTE:
            return JDBCType.SMALLINT;
        case STRING:
            return JDBCType.VARCHAR;
        default:
            return JDBCType.DOUBLE;
        }
    }

    public static void appendValue(Value value, StringBuilder sb) {

        switch (value.getClass().getSimpleName()) {
        case "BooleanValue":
            sb.append(value.asBoolean());
            break;
        case "ByteValue":
            sb.append(value.asByte());
            break;
        case "ByteArrayValue":
            byteArrayToHexString(sb, value.asByteArray());
            break;
        case "DoubleValue":
            sb.append(value.asDouble());
            break;
        case "FloatValue":
            sb.append(value.asFloat());
            break;
        case "IntValue":
            sb.append(value.asInt());
            break;
        case "LongValue":
            sb.append(value.asLong());
            break;
        case "ShortValue":
            sb.append(value.asShort());
            break;
        case "StringValue":
            sb.append('\'').append(value.asString()).append('\'');
            break;
        default:
            break;
        }
    }

    private static void byteArrayToHexString(StringBuilder sb, byte[] byteArray) {
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        sb.append('\'').append(hexChars).append('\'');
    }

}
