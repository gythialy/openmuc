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
package org.openmuc.framework.server.restws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.openmuc.framework.data.TypeConversionException;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;

public class TestTools {

    public static boolean testValue(String Test_method, ValueType valueType, Value value) {
        boolean result = true;

        if (value == null) {
            result = false;
            System.out.println(Test_method + ": result is \"" + result + "\"; error: Value is null.");
        }
        try {
            checkValueConversion(valueType, value);
        } catch (TypeConversionException e) {
            result = false;
            System.out.println(
                    Test_method + " result is \"" + result + "\"; error: ValueType is wrong;\n errormsg: " + e);
        }
        checkValueValue(Test_method, valueType, value);

        return result;
    }

    public static void checkValueConversion(ValueType valueType, Value value) throws TypeConversionException {

        switch (valueType) {
        case BOOLEAN:
            value.asBoolean();
            break;
        case BYTE:
            value.asByte();
            break;
        case BYTE_ARRAY:
            value.asByteArray();
            break;
        case DOUBLE:
            value.asDouble();
            break;
        case FLOAT:
            value.asFloat();
            break;
        case INTEGER:
            value.asInt();
            break;
        case LONG:
            value.asLong();
            break;
        case SHORT:
            value.asShort();
            break;
        case STRING:
            value.asString();
            break;
        default:
            // should never happen
            throw new TypeConversionException("Unknown ValueType");
        }
    }

    public static void checkValueValue(String Test_method, ValueType valueType, Value value) {

        switch (valueType) {
        case BOOLEAN:
            assertEquals(Constants.BOOLEAN_VALUE, value.asBoolean(),
                    Test_method + ": Expected boolean is not equal the actual");
            break;
        case BYTE:
            assertEquals(Constants.BYTE_VALUE, value.asByte(), Test_method + ": Expected byte is not equal the actual");
            break;
        case BYTE_ARRAY:
            if (!Arrays.equals(Constants.BYTE_ARRAY_VALUE, value.asByteArray())) {
                assertTrue(false, Test_method + ": Expected byte[] is not equal the actual");
            }
            break;
        case DOUBLE:
            assertEquals(Constants.DOUBLE_VALUE, value.asDouble(), 0.00001,
                    Test_method + ": Expected double is not equal the actual");
            break;
        case FLOAT:
            assertEquals(Constants.FLOAT_VALUE, value.asFloat(), 0.00001,
                    Test_method + ": Expected double is not equal the actual");
            break;
        case INTEGER:
            assertEquals(Constants.INTEGER_VALUE, value.asInt(),
                    Test_method + ": Expected int is not equal the actual");
            break;
        case LONG:
            assertEquals(Constants.LONG_VALUE, value.asLong(), Test_method + ": Expected long is not equal the actual");
            break;
        case SHORT:
            assertEquals(Constants.SHORT_VALUE, value.asShort(),
                    Test_method + ": Expected short is not equal the actual");
            break;
        case STRING:
            assertEquals(Constants.STRING_VALUE, value.asString(),
                    Test_method + ": Expected String is not equal the actual");
            break;
        default:
            // should never happen
        }
    }
}
