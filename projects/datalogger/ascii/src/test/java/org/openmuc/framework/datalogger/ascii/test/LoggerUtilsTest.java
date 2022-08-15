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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

public class LoggerUtilsTest {

    private static int loggingInterval = 1; // ms;
    private static int loggingTimeOffset = 0; // ms;
    private static String ch01 = "Double";
    private static String dummy = "dummy";
    private static HashMap<String, LogChannel> logChannelList = new HashMap<>();
    private static Calendar calendar = new GregorianCalendar();
    LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, true);

    @BeforeAll
    public static void setup() {

        int sub = (int) (calendar.getTimeInMillis() % 10l);
        calendar.add(Calendar.MILLISECOND, -loggingInterval * 5 - sub);

        TestUtils.createTestFolder();
        TestUtils.deleteExistingFile(loggingInterval, loggingTimeOffset, calendar);

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "", "dummy description", dummy, ValueType.DOUBLE, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

        logChannelList.put(ch01, ch1);

        long timeStamp = calendar.getTimeInMillis();

        for (int i = 0; i < 5; ++i) {

            LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, false);

            LogIntervalContainerGroup group = getGroup(timeStamp, i);
            lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
            AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, calendar.getTimeInMillis());
            calendar.add(Calendar.MILLISECOND, loggingInterval);
        }
    }

    private static LogIntervalContainerGroup getGroup(long timeStamp, int i) {

        LogIntervalContainerGroup group = new LogIntervalContainerGroup();

        LoggingRecord container1 = new LoggingRecord(ch01, new Record(new DoubleValue(i * 7 - 0.555), timeStamp));

        group.add(container1);

        return group;
    }

    @Test
    public void tc_501_test_getFilenames() {

        List<String> expecteds = new ArrayList<>();
        expecteds.add("20151005_1000_500.dat");
        expecteds.add("20151006_1000_500.dat");
        expecteds.add("20151007_1000_500.dat");
        expecteds.add("20151008_1000_500.dat");

        List<String> actual = LoggerUtils.getFilenames(1000, 500, 1444031465000l, 1444290665000l);

        int i = 0;
        for (String expected : expecteds) {

            assertEquals(actual.get(i++), expected);
        }
    }

    @Test
    public void tc_502_test_getFilename() {

        String expected = "20151005_1000_500.dat";
        String actual = LoggerUtils.getFilename(1000, 500, 1444031465000l);

        assertEquals(actual, expected);
    }

    // ####################################################################################################################
    // ####################################################################################################################
    // ####################################################################################################################

    @Test
    public void tc_503_test_fillUpFileWithErrorCode() {

        long lastTimestamp = AsciiLogger.fillUpFileWithErrorCode(TestUtils.TESTFOLDERPATH,
                Integer.toString(loggingInterval), calendar);

        AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, lastTimestamp);

        LogIntervalContainerGroup group = getGroup(calendar.getTimeInMillis(), 3);
        calendar = new GregorianCalendar();
        int sub = (int) (calendar.getTimeInMillis() % 10l);
        calendar.add(Calendar.MILLISECOND, -sub + 10);

        lfw.log(group, loggingInterval, loggingTimeOffset, calendar, logChannelList);
        AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, loggingTimeOffset, calendar.getTimeInMillis());

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "", "dummy description", dummy, ValueType.DOUBLE, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);
    }

    @Test
    public void tc_504_test_getDateOfFile() {
        try {
            String pattern = "yyyyMMdd";
            Date expectedDate = new SimpleDateFormat(pattern).parse("20151005");
            String fileName = "20151005_1000_500.dat";

            Date actualDate = LoggerUtils.getDateOfFile(fileName);
            long expected = expectedDate.getTime();
            long actual = actualDate.getTime();
            assertEquals(expected, actual);
        } catch (ParseException ex) {
            fail(ex.getMessage() + " \n" + ex.getStackTrace());
        }
    }

    @Test
    public void tc_505_test_findLatestValue() {
        Map<String, List<Record>> recordsMap = new HashMap<>();
        for (int j = 0; j < 5; j++) {
            List<Record> records = new LinkedList<>();
            for (int i = 0; i < 20; i++) {
                long timestamp = i;
                DoubleValue value = new DoubleValue(i + j);
                Record record = new Record(value, timestamp);
                records.add(record);
            }
            recordsMap.put("channel" + j, records);
        }

        Map<String, Record> latestValue = LoggerUtils.findLatestValue(recordsMap);
        for (int j = 0; j < 5; j++) {
            Double actual = latestValue.get("channel" + j).getValue().asDouble();
            Double expected = 19.0 + j;
            assertEquals(expected, actual);
        }
    }
}
