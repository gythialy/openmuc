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
package org.openmuc.framework.driver.modbus;

/**
 * Modbus defines four different address areas called primary tables.
 */
public enum EPrimaryTable {

    COILS, //
    DISCRETE_INPUTS, //
    INPUT_REGISTERS, //
    HOLDING_REGISTERS;

    public static EPrimaryTable getEnumfromString(String enumAsString) {
        EPrimaryTable returnValue = null;
        if (enumAsString != null) {
            for (EPrimaryTable value : EPrimaryTable.values()) {
                if (enumAsString.toUpperCase().equals(value.toString())) {
                    returnValue = EPrimaryTable.valueOf(enumAsString.toUpperCase());
                    break;
                }
            }
        }
        if (returnValue == null) {
            throw new RuntimeException(enumAsString
                    + " is not supported. Use one of the following supported primary tables: " + getSupportedValues());
        }
        return returnValue;
    }

    /**
     * @return all supported values as a comma separated string
     */
    public static String getSupportedValues() {
        String supported = "";
        for (EPrimaryTable value : EPrimaryTable.values()) {
            supported += value.toString() + ", ";
        }
        return supported;
    }

    public static boolean isValidValue(String enumAsString) {
        boolean returnValue = false;
        for (EPrimaryTable type : EPrimaryTable.values()) {
            if (type.toString().toLowerCase().equals(enumAsString.toLowerCase())) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

}
