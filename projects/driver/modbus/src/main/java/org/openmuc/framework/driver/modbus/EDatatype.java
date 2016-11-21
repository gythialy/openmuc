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

public enum EDatatype {

    BOOLEAN("boolean", 1), // 1 Bit
    SHORT("short", 1), // 1 Register
    INT("int", 2), // 2 Registers
    FLOAT("float", 2), // 2 Registers
    DOUBLE("double", 4), // 4 Registers
    LONG("long", 4), // 4 Registers
    BYTEARRAY("bytearray", 0), // registerCount is calculated dynamically, the 0 will be overwritten
    BYTE_HIGH("byte_high", 1),
    BYTE_LOW("byte_low", 1);

    private final String datatype;
    private final int registerCount;

    EDatatype(String datatypeAsString, int registerSize) {
        datatype = datatypeAsString;
        registerCount = registerSize;
    }

    public int getRegisterCount() {
        return registerCount;
    }

    @Override
    public String toString() {
        return datatype;
    }

    public static EDatatype getEnumFromString(String enumAsString) {
        EDatatype returnValue = null;

        if (enumAsString != null) {

            for (EDatatype type : EDatatype.values()) {
                if (enumAsString.equals(type.datatype)) {
                    returnValue = EDatatype.valueOf(enumAsString.toUpperCase());
                    break;
                }
                else if (enumAsString.toUpperCase().matches("BYTEARRAY\\[\\d+\\]")) {
                    // Special check for BYTEARRAY[n] datatyp
                    returnValue = EDatatype.BYTEARRAY;
                    break;
                }
            }
        }

        if (returnValue == null) {
            throw new RuntimeException(enumAsString
                    + " is not supported. Use one of the following supported datatypes: " + getSupportedDatatypes());
        }

        return returnValue;

    }

    /**
     * @return all supported datatypes
     */
    public static String getSupportedDatatypes() {

        String supported = "";

        for (EDatatype type : EDatatype.values()) {
            supported += type.toString() + ", ";
        }

        return supported;
    }

    /**
     * Checks if the datatype is valid
     * 
     * @param enumAsString
     *            Enum as String
     * @return true if valid, otherwise false
     */
    public static boolean isValidDatatype(String enumAsString) {
        boolean returnValue = false;

        for (EDatatype type : EDatatype.values()) {

            if (type.toString().toLowerCase().equals(enumAsString.toLowerCase())) {
                returnValue = true;
                break;
            }
            else if (enumAsString.toUpperCase().matches("BYTEARRAY\\[\\d+\\]")) {
                // Special check for BYTEARRAY[n] datatyp
                returnValue = true;
                break;
            }
        }

        return returnValue;
    }
}
