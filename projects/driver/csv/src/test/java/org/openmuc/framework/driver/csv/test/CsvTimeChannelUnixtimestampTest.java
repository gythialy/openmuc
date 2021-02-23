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
package org.openmuc.framework.driver.csv.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.driver.csv.channel.CsvChannelUnixtimestamp;
import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;

public class CsvTimeChannelUnixtimestampTest {

    static List<String> data;
    static long[] timestamps;
    static String value;
    private static final long OFFSET = 1436306400000l;

    @BeforeClass
    public static void initTestClass() {
        data = new ArrayList<>();
        data.add("0.0");
        data.add("5.0");
        data.add("10.0");
        data.add("15.0");
        data.add("20.0");

        timestamps = new long[] { //
                OFFSET /* ........= 20150708 000000 */, //
                1436306405000l /* = 20150708 000005 */, //
                1436306410000l /* = 20150708 000010 */, //
                1436306415000l /* = 20150708 000015 */, //
                1436306420000l /* = 20150708 000020 */ };
    }

    @Test
    public void testRead() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));

        // interval size is 5 seconds, driver returns new value once the new interval is reached
        value = channel.readValue(OFFSET + 4999);
        Assert.assertTrue(value.equals("0.0"));

        value = channel.readValue(OFFSET + 5000);
        Assert.assertTrue(value.equals("5.0"));

    }

    @Test
    public void testReadNextValueInbetween() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET + 6000l);
        Assert.assertTrue(value.equals("5.0"));

        value = channel.readValue(OFFSET + 14000l);
        Assert.assertTrue(value.equals("10.0"));
    }

    @Test
    public void testReadNextValueStart() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));

        value = channel.readValue(OFFSET + 5000l);
        Assert.assertTrue(value.equals("5.0"));
    }

    @Test
    public void testReadNextValueEndNoRewind() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET + 20000);
        Assert.assertTrue(value.equals("20.0"));

        value = channel.readValue(OFFSET + 25000);
        Assert.assertTrue(value.equals("20.0"));

        // timestamp before the last one, but rewind is false so timestamp is not considered and old value is returned
        try {
            value = channel.readValue(OFFSET);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

    @Test
    public void testReadNextValueEndWithRewind() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, true, timestamps);

        value = channel.readValue(OFFSET + 20000);
        Assert.assertTrue(value.equals("20.0"));

        value = channel.readValue(OFFSET + 25000);
        Assert.assertTrue(value.equals("20.0"));

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));
    }

    @Test
    public void testReadT1BeforeT2Valid() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        try {
            value = channel.readValue(OFFSET - 5000l);
            Assert.assertTrue(false);
        } catch (NoValueReceivedYetException e) {
            Assert.assertTrue(true);
        }

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));

    }

    @Test
    public void testReadT1ValidT2BeforeDisabledRewind() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));

        // sampling jumped back before first timestamp of file
        try {
            value = channel.readValue(OFFSET - 5000);
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testReadT1ValidT2BeforeEnabledRewind() throws CsvException {

        CsvChannelUnixtimestamp channel = new CsvChannelUnixtimestamp(data, true, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(value.equals("0.0"));

        // sampling jumped back before first timestamp of file
        try {
            value = channel.readValue(OFFSET - 5000);
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

}
