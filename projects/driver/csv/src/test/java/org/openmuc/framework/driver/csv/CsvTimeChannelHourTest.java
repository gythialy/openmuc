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
package org.openmuc.framework.driver.csv;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.driver.csv.channel.CsvChannelHHMMSS;
import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;

public class CsvTimeChannelHourTest {

    static List<String> data;
    static long[] timestamps;
    static String value;

    @BeforeAll
    public static void initTestClass() {

        // create test data. first data entry corresponds to first timestamps entry
        data = new ArrayList<>();
        data.add("0.0");
        data.add("5.0");
        data.add("10.0");
        data.add("15.0");
        data.add("20.0");

        timestamps = new long[] { 100000, 100005, 100010, 100015, 100020 };

    }

    @Test
    public void testReadNextValueInbetween() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, false, timestamps);

        value = channel.readValue(createTimestamp(100006));
        assertTrue(value.equals("5.0"));

        value = channel.readValue(createTimestamp(100014));
        assertTrue(value.equals("10.0"));
    }

    @Test
    public void testReadNextValueStart() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, false, timestamps);

        value = channel.readValue(createTimestamp(100000));
        assertTrue(value.equals("0.0"));

        value = channel.readValue(createTimestamp(100005));
        assertTrue(value.equals("5.0"));
    }

    @Test
    public void testReadNextValueEndNoRewind() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, false, timestamps);

        value = channel.readValue(createTimestamp(100020));
        assertTrue(value.equals("20.0"));

        value = channel.readValue(createTimestamp(100025));
        assertTrue(value.equals("20.0"));

        // timestamp before the last one, but rewind is false so timestamp is not considered and old value is returned
        try {
            value = channel.readValue(createTimestamp(100000));
        } catch (TimeTravelException e) {
            assertTrue(true);
        }

    }

    @Test
    public void testReadNextValueEndWithRewind() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, true, timestamps);

        value = channel.readValue(createTimestamp(100020));
        assertTrue(value.equals("20.0"));

        value = channel.readValue(createTimestamp(100025));
        assertTrue(value.equals("20.0"));

        value = channel.readValue(createTimestamp(100000));
        assertTrue(value.equals("0.0"));
    }

    @Test
    public void testReadT1BeforeT2Valid() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, false, timestamps);

        try {
            value = channel.readValue(createTimestamp(90000));
            assertTrue(false);
        } catch (NoValueReceivedYetException e) {
            assertTrue(true);
        }

        value = channel.readValue(createTimestamp(100000));
        assertTrue(value.equals("0.0"));

    }

    @Test
    public void testReadT1ValidT2BeforeDisabledRewind() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, false, timestamps);

        value = channel.readValue(createTimestamp(100000));
        assertTrue(value.equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(createTimestamp(90000));
            assertTrue(false);
        } catch (TimeTravelException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testReadT1ValidT2BeforeEnabledRewind() throws CsvException {

        CsvChannelHHMMSS channel = new CsvChannelHHMMSS(data, true, timestamps);

        value = channel.readValue(createTimestamp(100000));
        assertTrue(value.equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(createTimestamp(90000));
            assertTrue(false);
        } catch (TimeTravelException e) {
            assertTrue(true);
        }

    }

    private long createTimestamp(long hhmmss) {
        GregorianCalendar cal = new GregorianCalendar(Locale.GERMANY);

        cal.setTimeInMillis(System.currentTimeMillis());

        // add leading zeros e.g. 90000 (9 o'clock) will be converted to 090000
        String time = String.format("%06d", hhmmss);

        String hourStr = time.substring(0, 2);
        String minuteStr = time.substring(2, 4);
        String secondStr = time.substring(4, 6);

        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourStr));
        cal.set(Calendar.MINUTE, Integer.parseInt(minuteStr));
        cal.set(Calendar.SECOND, Integer.parseInt(secondStr));
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();

    }

}
