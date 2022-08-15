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

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileReader;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

public class LogFileWriterTest {

    // t1 = start timestamp of requestet interval
    // t2 = end timestamp of requestet interval

    private static int loggingInterval = 10000; // ms;
    private static int loggingTimeOffset = 0; // ms;
    private static String ext = ".dat";
    private static String dateFormat = "yyyyMMdd HH:mm:s";
    private static String fileDate1 = "20880808";
    private static String fileDate2 = "20880809";
    private static String ch01 = "FLOAT";
    private static String ch02 = "DOUBLE";
    private static String ch03 = "BOOLEAN";
    private static String ch04 = "SHORT";
    private static String ch05 = "INTEGER";
    private static String ch06 = "LONG";
    private static String ch07 = "BYTE";
    private static String ch08 = "STRING";
    private static String ch09 = "BYTE_ARRAY";
    private static String dummy = "dummy";
    // private static String[] channelIds = new String[] { ch01, ch02, ch03, ch04, ch05, ch06, ch07, ch08, ch09 };
    private static String time = " 23:55:00";
    private static String testStringValueCorrect = "qwertzuiop+asdfghjkl#<yxcvbnm,.-^1234567890 !$%&/()=?QWERTZUIOP*ASDFGHJKL'>YXCVBNM;:_";
    private static String testStringValueIncorrect = "qwertzuiop+asdfghjkl#<yxcvbnm,.-^1234567890 " + Const.SEPARATOR
            + "!$%&/()=?QWERTZUIOP*SDFGHJKL'>YXCVBNM;:_";
    private static byte[] testByteArray = { 1, 2, 3, 4, -5, -9, 0 };
    private static int valueLength = 100;
    private static int valueLengthByteArray = testByteArray.length;
    private static HashMap<String, LogChannel> logChannelList = new HashMap<>();
    private static Calendar calendar = TestUtils.stringToDate(dateFormat, fileDate1 + time);
    LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, true);

    @BeforeAll
    public static void setup() {

        System.out.println("### Setup() LogFileWriterTest");

        TestUtils.createTestFolder();

        // 2 Kanaele im Stunden-Takt loggen von 12 Uhr bis 12 Uhr in den naechsten Tage hinein
        // --> Ergebnis muessten zwei Dateien sein die vom LogFileWriter erstellt wurden

        String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + ext;
        String filename2 = TestUtils.TESTFOLDERPATH + fileDate2 + "_" + loggingInterval + ext;

        File file1 = new File(filename1);
        File file2 = new File(filename2);

        if (file1.exists()) {
            System.out.println("Delete File " + filename1);
            file1.delete();
        }
        if (file2.exists()) {
            System.out.println("Delete File " + filename2);
            file2.delete();
        }

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "", "dummy description", dummy, ValueType.FLOAT, 0.0, 0.0,
                false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch2 = new LogChannelTestImpl(ch02, "", "dummy description", dummy, ValueType.DOUBLE, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch3 = new LogChannelTestImpl(ch03, "", "dummy description", dummy, ValueType.BOOLEAN, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch4 = new LogChannelTestImpl(ch04, "", "dummy description", dummy, ValueType.SHORT, 0.0, 0.0,
                false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch5 = new LogChannelTestImpl(ch05, "", "dummy description", dummy, ValueType.INTEGER, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch6 = new LogChannelTestImpl(ch06, "", "dummy description", dummy, ValueType.LONG, 0.0, 0.0,
                false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch7 = new LogChannelTestImpl(ch07, "", "dummy description", dummy, ValueType.BYTE, 0.0, 0.0,
                false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogChannelTestImpl ch8 = new LogChannelTestImpl(ch08, "", "dummy description", dummy, ValueType.STRING, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, valueLength, false);
        LogChannelTestImpl ch9 = new LogChannelTestImpl(ch09, "", "dummy description", dummy, ValueType.BYTE_ARRAY, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, valueLengthByteArray, false);

        logChannelList.put(ch01, ch1);
        logChannelList.put(ch02, ch2);
        logChannelList.put(ch03, ch3);
        logChannelList.put(ch04, ch4);
        logChannelList.put(ch05, ch5);
        logChannelList.put(ch06, ch6);
        logChannelList.put(ch07, ch7);
        logChannelList.put(ch08, ch8);
        logChannelList.put(ch09, ch9);

        long timeStamp = calendar.getTimeInMillis();
        boolean boolValue;
        byte byteValue = 0;

        String testString;

        // writes 24 records for 2 channels from 12 o'clock till 12 o'clock of the other day
        AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, 0); // Set to 0, for deleting
        // timestamp of previous test

        for (int i = 0; i < ((60 * 10) * (1000d / loggingInterval)); ++i) {

            if ((i % 2) > 0) {
                boolValue = true;
                testString = testStringValueCorrect;
            }
            else {
                boolValue = false;
                testString = testStringValueIncorrect;
            }

            LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, false);

            LogIntervalContainerGroup group = getGroup(timeStamp, i, boolValue, byteValue, testString);
            lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
            AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, calendar.getTimeInMillis());
            calendar.add(Calendar.MILLISECOND, loggingInterval);

            ++byteValue;
        }
    }

    @AfterAll
    public static void tearDown() {

        System.out.println("tearing down");
        TestUtils.deleteTestFolder();
    }

    private static LogIntervalContainerGroup getGroup(long timeStamp, int i, boolean boolValue, byte byteValue,
            String testString) {

        LogIntervalContainerGroup group = new LogIntervalContainerGroup();

        LoggingRecord container1 = new LoggingRecord(ch01, new Record(new FloatValue(i * -7 - 0.555F), timeStamp));
        LoggingRecord container2 = new LoggingRecord(ch02, new Record(new DoubleValue(i * +7 - 0.555), timeStamp));
        LoggingRecord container3 = new LoggingRecord(ch03, new Record(new BooleanValue(boolValue), timeStamp));
        LoggingRecord container4 = new LoggingRecord(ch04, new Record(new ShortValue((short) i), timeStamp));
        LoggingRecord container5 = new LoggingRecord(ch05, new Record(new IntValue(i), timeStamp));
        LoggingRecord container6 = new LoggingRecord(ch06, new Record(new LongValue(i * 1000000), timeStamp));
        LoggingRecord container7 = new LoggingRecord(ch07, new Record(new ByteValue(byteValue), timeStamp));
        LoggingRecord container8 = new LoggingRecord(ch08, new Record(new StringValue(testString), timeStamp));
        LoggingRecord container9 = new LoggingRecord(ch09, new Record(new ByteArrayValue(testByteArray), timeStamp));

        group.add(container1);
        group.add(container2);
        group.add(container3);
        group.add(container4);
        group.add(container5);
        group.add(container6);
        group.add(container7);
        group.add(container8);
        group.add(container9);

        return group;
    }

    @Test
    public void tc300_check_if_new_file_is_created_on_day_change() {

        System.out.println("### Begin test tc300_check_if_new_file_is_created_on_day_change");

        String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + ext;
        String filename2 = TestUtils.TESTFOLDERPATH + fileDate2 + "_" + loggingInterval + ext;

        File file1 = new File(filename1);
        File file2 = new File(filename2);

        Boolean assertT;
        if (file1.exists() && file2.exists()) {
            assertT = true;
        }
        else {
            assertT = false;
        }
        System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" " + file1.getAbsolutePath());
        System.out.println(" " + file2.getAbsolutePath());
        System.out.println(" Two files created = " + assertT);

        assertTrue(assertT);
    }

    // @Test
    // public void tc302_check_file_fill_up_at_logging_at_day_change() {
    // // TODO:
    // second_setup();
    // System.out.println("### Begin test tc301_check_file_fill_up_at_logging_at_day_change");
    //
    // int valuesToWrite = 5;
    //
    // calendar.add(Calendar.MILLISECOND, loggingInterval * valuesToWrite);
    //
    // LogIntervalContainerGroup group = getSecondGroup(calendar.getTimeInMillis(), 4);
    // lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
    //
    // LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "dummy description", dummy, ValueType.FLOAT,
    // loggingInterval, loggingTimeOffset);
    // LogFileReader lfr = new LogFileReader(TestUtils.TESTFOLDERPATH, ch1);
    //
    // List<Record> recordList = lfr.getValues(calendar.getTimeInMillis() - loggingInterval * 5,
    // calendar.getTimeInMillis());
    // int receivedRecords = recordList.size();
    //
    // int numErrorFlags = 0;
    // for (Record record : recordList) {
    // if (record.getFlag().equals(Flag.DATA_LOGGING_NOT_ACTIVE)) {
    // ++numErrorFlags;
    // }
    // }
    //
    // Boolean assertT;
    // if (receivedRecords == valuesToWrite && numErrorFlags == valuesToWrite - 1) {
    // assertT = true;
    // }
    // else {
    // assertT = false;
    // }
    // System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName());
    // System.out.println(" records = " + receivedRecords + " (" + valuesToWrite + " expected)");
    // System.out
    // .println(" records with error flag 32 = " + numErrorFlags + " (" + (valuesToWrite - 1) + " expected)");
    //
    // assertTrue(assertT);
    // }

    // private void second_setup() {
    //
    // System.out.println("### second_setup() LogFileWriterTest");
    //
    // TestSuite.createTestFolder();
    //
    // // 2 Kanaele im Stunden-Takt loggen von 12 Uhr bis 12 Uhr in den naechsten Tage hinein
    // // --> Ergebnis muessten zwei Dateien sein die vom LogFileWriter erstellt wurden
    //
    // String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + ext;
    // String filename2 = TestUtils.TESTFOLDERPATH + fileDate2 + "_" + loggingInterval + ext;
    //
    // File file1 = new File(filename1);
    // File file2 = new File(filename2);
    //
    // if (file1.exists()) {
    // System.out.println("Delete File " + filename1);
    // file1.delete();
    // }
    // if (file2.exists()) {
    // System.out.println("Delete File " + filename2);
    // file2.delete();
    // }
    //
    // LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "dummy description", dummy, ValueType.FLOAT,
    // loggingInterval, loggingTimeOffset);
    //
    // logChannelList.put(ch01, ch1);
    //
    // long timeStamp = calendar.getTimeInMillis();
    //
    // // writes 24 records for 2 channels from 12 o'clock till 12 o'clock of the other day
    // AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, 0); // Set to 0, for deleting
    // // timestamp of previous test
    // for (int i = 0; i < ((60 * 10) * (1000d / loggingInterval)); ++i) {
    //
    // LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, true);
    //
    // LogIntervalContainerGroup group = getSecondGroup(timeStamp, i);
    // lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
    // calendar.add(Calendar.MILLISECOND, loggingInterval);
    // }
    // }

    @Test
    public void tc301_check_file_fill_up_at_logging() {

        System.out.println("### Begin test tc301_check_file_fill_up_at_logging");

        int valuesToWrite = 5;

        calendar.add(Calendar.MILLISECOND, loggingInterval * valuesToWrite - 10);

        LogIntervalContainerGroup group = getGroup(calendar.getTimeInMillis(), 3, true, (byte) 0x11, "nope");
        lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
        AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, calendar.getTimeInMillis());

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "", "dummy description", dummy, ValueType.FLOAT, 0.0, 0.0,
                false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
        LogFileReader lfr = new LogFileReader(TestUtils.TESTFOLDERPATH, ch1);

        List<Record> recordList = lfr
                .getValues(calendar.getTimeInMillis() - loggingInterval * 5, calendar.getTimeInMillis())
                .get(ch01);
        int receivedRecords = recordList.size();

        int numErrorFlags = 0;
        for (Record record : recordList) {
            if (record.getFlag().equals(Flag.DATA_LOGGING_NOT_ACTIVE)) {
                ++numErrorFlags;
            }
        }

        Boolean assertT;
        if (receivedRecords == valuesToWrite && numErrorFlags == valuesToWrite - 1) {
            assertT = true;
        }
        else {
            assertT = false;
        }
        System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + receivedRecords + " (" + valuesToWrite + " expected)");
        System.out
                .println(" records with error flag 32 = " + numErrorFlags + " (" + (valuesToWrite - 1) + " expected)");

        assertTrue(assertT);
    }

}
