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
package org.openmuc.framework.datalogger.ascii.test;

import java.util.Iterator;
import java.util.TreeMap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.openmuc.framework.datalogger.ascii.exceptions.WrongScalingException;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.ascii.utils.IESDataFormatUtils;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;

public class MiscTests {

    public static final byte[] BYTE_ARRAY = { (byte) 0x00, (byte) 0x01, (byte) 0x0A, (byte) 0xAA, (byte) 0xBB,
            (byte) 0xF7, (byte) 0xFF, (byte) 0xCA, (byte) 0xD5, 0x5E };
    public static final String STRING_BYTE_ARRAY = "0x00010AAABBF7FFCAD55E";

    StringBuilder sb = new StringBuilder();

    @AfterClass
    public static void tearDown() {

        System.out.println("tearing down");
        TestUtils.deleteTestFolder();
    }

    @Test
    public void testDoubleFormattingOk() {

        System.out.println("### Begin test testDoubleFormattingOk");

        TreeMap<Double, String> testData = new TreeMap<>();

        testData.put(-0.0, "+0.000"); // should be +
        testData.put(0.0, "+0.000");

        testData.put(1.0, "+1.000");
        testData.put(-1.0, "-1.000");

        testData.put(10.0, "+10.000");
        testData.put(-10.0, "-10.000");

        testData.put(10.123, "+10.123");
        testData.put(-10.123, "-10.123");

        testData.put(9999.999, "+9999.999");
        testData.put(-9999.999, "-9999.999");

        // decimal digits = 3
        testData.put(1000.123, "+1000.123");
        testData.put(-1000.123, "-1000.123");

        // decimal digits = 2
        testData.put(10000.123, "+10000.12");
        testData.put(-10000.123, "-10000.12");

        // decimal digits = 1
        testData.put(100000.123, "+100000.1");
        testData.put(-100000.123, "-100000.1");

        // decimal digits = 0
        testData.put(1000000.123, "+1000000");
        testData.put(-1000000.123, "-1000000");

        // max number 8 digits
        testData.put(99999999.0, "+99999999");
        testData.put(-99999999.0, "-99999999");

        String expectedResult;
        double input;

        Iterator<Double> i = testData.keySet().iterator();

        while (i.hasNext()) {

            input = i.next();
            expectedResult = testData.get(input);

            try {
                sb.setLength(0);

                IESDataFormatUtils.convertDoubleToStringWithMaxLength(sb, input, Const.VALUE_SIZE_DOUBLE);

                System.out.println(input + " --> " + sb.toString() + " " + expectedResult);

                Assert.assertEquals(expectedResult, sb.toString());

            } catch (WrongScalingException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testWrongScalingException() {

        System.out.println("### Begin test testWrongScalingException");

        double input = 100000000.0;

        try {
            sb.setLength(0);
            IESDataFormatUtils.convertDoubleToStringWithMaxLength(sb, input, Const.VALUE_SIZE_DOUBLE);
            Assert.assertTrue("Expected WrongScalingException", false);
        } catch (WrongScalingException e) {
            Assert.assertTrue(true);
        }

        input = -100000000.0;

        try {
            sb.setLength(0);
            IESDataFormatUtils.convertDoubleToStringWithMaxLength(sb, input, Const.VALUE_SIZE_DOUBLE);
            Assert.assertTrue("Expected WrongScalingException", false);
        } catch (WrongScalingException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testByteArrayConversion() {
        sb.setLength(0);
        sb.append(Const.HEXADECIMAL);
        LoggerUtils.byteArrayToHexString(sb, BYTE_ARRAY);

        Assert.assertEquals(sb.toString(), STRING_BYTE_ARRAY);
    }

}
