/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.driver.canopen;

import org.openmuc.framework.data.*;
import org.openmuc.jcanopen.datatypes.*;

/**
 * @author Frederic Robra
 */
public class Transforms {

    public static Value number2Value(Number number) {
        Value value = null;
        if (number instanceof Short) {
            value = new ShortValue(number.shortValue());
        } else if (number instanceof Integer) {
            value = new IntValue(number.intValue());
        } else if (number instanceof Long) {
            value = new LongValue(number.longValue());
        } else if (number instanceof Float) {
            value = new FloatValue(number.floatValue());
        } else { // if(number instanceof Double) {
            value = new DoubleValue(number.doubleValue());
        }
        return value;
    }

    public static Number value2Number(Value value) {
        Number number = null;
        if (value instanceof ShortValue) {
            number = new Short(value.asShort());
        } else if (value instanceof IntValue) {
            number = new Integer(value.asInt());
        } else if (value instanceof LongValue) {
            number = new Long(value.asLong());
        } else if (value instanceof FloatValue) {
            number = new Float(value.asFloat());
        } else { // if(value instanceof DoubleValue) {
            number = new Double(value.asDouble());
        }
        return number;
    }

    public static int parseHexOrDecValue(String value) {
        if (value.startsWith("0x")) {
            value = value.substring(2);
            return Integer.parseInt(value, 16);
        } else {
            return Integer.parseInt(value);
        }
    }

    public static NumericDataType parseDataType(String dataTypeName) {
        NumericDataType numericDataType = null;
        if (dataTypeName.equals("UNSIGNED8")) {
            numericDataType = new Unsigned8((short) 0);
        } else if (dataTypeName.equals("UNSIGNED16")) {
            numericDataType = new Unsigned16(0);
        } else if (dataTypeName.equals("UNSIGNED24")) {
            numericDataType = new Unsigned24(0);
        } else if (dataTypeName.equals("UNSIGNED32")) {
            numericDataType = new Unsigned32(0);
        } else if (dataTypeName.equals("INTEGER8")) {
            numericDataType = new Integer8((short) 0);
        } else if (dataTypeName.equals("INTEGER16")) {
            numericDataType = new Integer16((short) 0);
        } else if (dataTypeName.equals("INTEGER24")) {
            numericDataType = new Integer24(0);
        } else if (dataTypeName.equals("INTEGER32")) {
            numericDataType = new Integer32(0);
        } else if (dataTypeName.equals("REAL32")) {
            numericDataType = new Real32(0);
        } else if (dataTypeName.equals("REAL64")) {
            numericDataType = new Real64(0);
        }

        return numericDataType;
    }

}
