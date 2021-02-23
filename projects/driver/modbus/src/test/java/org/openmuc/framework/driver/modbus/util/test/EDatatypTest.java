/*
 * Copyright 2011-2021 Fraunhofer ISE
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
package org.openmuc.framework.driver.modbus.util.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openmuc.framework.driver.modbus.EDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDatatypTest {

    private static final Logger logger = LoggerFactory.getLogger(EDatatypTest.class);

    // INT8, Byte
    private static final int INT8_MIN = -128;
    private static final int INT8_MAX = -127;

    private static final int UINT8_MIN = 0;
    private static final int UINT8_MAX = 255;

    @Test
    public void getSupportedDatatypesTest() {

        logger.info("Supported Datatyps: " + EDatatype.getSupportedDatatypes());
        assertTrue(true);
    }

    @Test
    public void isValidDatatypTest() {

        // valid
        assertTrue(EDatatype.isValid("int32"));
        assertTrue(EDatatype.isValid("INT32"));

        // invalid
        assertFalse(EDatatype.isValid("INT30"));
        assertFalse(EDatatype.isValid("shorts"));
    }

    // @Test
    // public void modbusRegisterToValue() {
    // ModbusDriverUtil util = new ModbusDriverUtil();
    //
    // // One modbus register has the size of two Byte
    //
    // byte[] registers = new byte[] { (byte) 0xFF, (byte) 0xFF };
    //
    // Value value;
    //
    // value = util.getValueFromByteArray(registers, EDatatype.INT8);
    // logger.info(registers.toString() + " : " + value.toString());
    //
    // value = util.getValueFromByteArray(registers, EDatatype.UINT8);
    // logger.info(registers.toString() + " : " + value.toString());
    //
    // }

}
