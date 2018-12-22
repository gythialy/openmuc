package org.openmuc.framework.driver.modbus.util.test;

import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertTrue(true);
    }

    @Test
    public void isValidDatatypTest() {

        // valid
        Assert.assertTrue(EDatatype.isValid("int32"));
        Assert.assertTrue(EDatatype.isValid("INT32"));

        // invalid
        Assert.assertFalse(EDatatype.isValid("INT30"));
        Assert.assertFalse(EDatatype.isValid("shorts"));
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
