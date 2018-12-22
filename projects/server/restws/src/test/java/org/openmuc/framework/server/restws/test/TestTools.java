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
package org.openmuc.framework.server.restws.test;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
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
            Assert.assertEquals(Test_method + ": Expected boolean is not equal the actual", Constants.BOOLEAN_VALUE,
                    value.asBoolean());
            break;
        case BYTE:
            Assert.assertEquals(Test_method + ": Expected byte is not equal the actual", Constants.BYTE_VALUE,
                    value.asByte());
            break;
        case BYTE_ARRAY:
            if (!Arrays.equals(Constants.BYTE_ARRAY_VALUE, value.asByteArray())) {
                assertTrue(Test_method + ": Expected byte[] is not equal the actual", false);
            }
            break;
        case DOUBLE:
            Assert.assertEquals(Test_method + ": Expected double is not equal the actual", Constants.DOUBLE_VALUE,
                    value.asDouble(), 0.00001);
            break;
        case FLOAT:
            Assert.assertEquals(Test_method + ": Expected double is not equal the actual", Constants.FLOAT_VALUE,
                    value.asFloat(), 0.00001);
            break;
        case INTEGER:
            Assert.assertEquals(Test_method + ": Expected int is not equal the actual", Constants.INTEGER_VALUE,
                    value.asInt());
            break;
        case LONG:
            Assert.assertEquals(Test_method + ": Expected long is not equal the actual", Constants.LONG_VALUE,
                    value.asLong());
            break;
        case SHORT:
            Assert.assertEquals(Test_method + ": Expected short is not equal the actual", Constants.SHORT_VALUE,
                    value.asShort());
            break;
        case STRING:
            Assert.assertEquals(Test_method + ": Expected String is not equal the actual", Constants.STRING_VALUE,
                    value.asString());
            break;
        default:
            // should never happen
        }
    }
}
