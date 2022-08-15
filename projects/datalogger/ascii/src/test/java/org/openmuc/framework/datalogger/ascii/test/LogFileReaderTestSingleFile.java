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
package org.openmuc.framework.datalogger.ascii.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileReader;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

public class LogFileReaderTestSingleFile {

    // t1 = start timestamp of requested interval
    // t2 = end timestamp of requested interval

    static String fileDate0 = "20660606";
    static int loggingInterval = 10000; // ms
    static int loggingTimeOffset = 0; // ms
    static String ext = ".dat";
    static long startTimestampFile;
    static long endTimestampFile;
    static String Channel0Name = "power";
    static String[] channelIds = { Channel0Name };
    static String dateFormat = "yyyyMMdd HH:mm:ss";

    LogChannelTestImpl channelTestImpl = new LogChannelTestImpl(Channel0Name, "", "Comment", "W", ValueType.DOUBLE, 0.0,
            0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

    @BeforeAll
    public static void setup() {

        System.out.println("### Setup() LogFileReaderTestSingleFile");

        TestUtils.createTestFolder();

        // File file = new File(TestUtils.TESTFOLDERPATH + fileDate0 + "_" + loggingInterval + ext);

        // if (file.exists()) {
        // Do nothing, file exists.
        // }
        // else {
        // eine Datei
        channelIds = new String[] { "power" };

        // Logs 1 channel in second interval from 1 to 3 o'clock

        HashMap<String, LogChannel> logChannelList = new HashMap<>();

        LogChannelTestImpl ch1 = new LogChannelTestImpl(Channel0Name, "", "dummy description", "kW", ValueType.DOUBLE,
                0.0, 0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

        logChannelList.put(Channel0Name, ch1);

        Calendar calendar = TestUtils.stringToDate(dateFormat, fileDate0 + " 01:00:00");

        for (int i = 0; i < ((60 * 60 * 2) * (1000d / loggingInterval)); i++) {
            LoggingRecord container1 = new LoggingRecord(Channel0Name,
                    new Record(new DoubleValue(i), calendar.getTimeInMillis()));

            LogIntervalContainerGroup group = new LogIntervalContainerGroup();
            group.add(container1);

            LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, false);
            lfw.log(group, loggingInterval, 0, calendar, logChannelList);
            AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, 0, calendar.getTimeInMillis());

            calendar.add(Calendar.MILLISECOND, loggingInterval);
        }
        // }
    }

    @AfterAll
    public static void tearDown() {

        System.out.println("tearing down");
        TestUtils.deleteTestFolder();
    }

    @Test
    public void tc000_t1_t2_within_available_data() {

        System.out.println("### Begin test tc000_t1_t2_within_available_data");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 01:50:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate0 + " 01:51:00").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 7;
        boolean result;
        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
        assertTrue(result);
    }

    @Test
    public void tc001_t1_before_available_data_t2_within() {

        System.out.println("### Begin test tc001_t1_before_available_data_t2_within");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 00:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate0 + " 00:00:10").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 0;
        boolean result;

        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
        assertTrue(result);
    }

    @Test
    public void tc002_t2_after_available_data() {

        System.out.println("### Begin test tc002_t2_after_available_data");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 01:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate0 + " 02:00:00").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 361; //

        boolean result;
        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
        assertTrue(result);
    }

    @Test
    public void tc003_t1_t2_before_available_data() {

        System.out.println("### Begin test tc003_t1_t2_before_available_data");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 00:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate0 + " 00:59:59").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 0;
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());

        boolean result = true;
        int wrong = 0;
        int ok = 0;

        for (Record record : records) {
            if (record.getFlag().equals(Flag.NO_VALUE_RECEIVED_YET)) {
                ++ok;
            }
            else {
                ++wrong;
                result = false;
            }
        }
        System.out.print(" records = " + records.size() + " (" + expectedRecords + " expected); ");
        System.out.println("wrong = " + wrong + ", ok(with Flag 7) = " + ok);
        assertTrue(result);
    }

    @Test
    public void tc004_t1_t2_after_available_data() {

        System.out.println("### Begin test tc004_t1_t2_after_available_data");

        // test 5 - startTimestampRequest & endTimestampRequest after available logged data
        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 03:00:01").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate0 + " 03:59:59").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 0;

        boolean result;
        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
        assertTrue(result);
    }

    @Test
    public void tc005_t1_within_available_data() {

        System.out.println("### Begin test tc005_t1_within_available_data");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 01:11:10").getTimeInMillis();
        boolean result;

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        Record record = fr.getValue(t1).get(channelTestImpl.getId());

        if (record != null) {
            result = true;

        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" record = " + result + "record = ");
        assertTrue(result);
    }

    @Test
    public void tc006_t1_before_available_data() {

        System.out.println("### Begin test tc006_t1_before_available_data");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 00:59:00").getTimeInMillis();
        boolean result;

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        Record record = fr.getValue(t1).get(channelTestImpl.getId());
        System.out.println("record: " + record);
        if (record == null) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" no records = " + result);
        assertTrue(result);
    }

    // @Test
    public void tc007_t1_within_available_data_with_loggingInterval() {

        System.out.println("### Begin test tc007_t1_within_available_data_with_loggingInterval");

        boolean result;
        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 02:59:59").getTimeInMillis();
        // get value looks from 02:59:59 to 3:00:00. before 3:00:00 a value exists
        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);

        Record record = fr.getValue(t1).get(channelTestImpl.getId());

        if (record != null) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" record = " + result);
        assertTrue(result);
    }
}
