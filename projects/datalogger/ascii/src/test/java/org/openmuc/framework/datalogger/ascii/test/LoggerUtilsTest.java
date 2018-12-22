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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.core.datamanager.LogRecordContainerImpl;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;

public class LoggerUtilsTest {

    LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, true);

    private static int loggingInterval = 1; // ms;
    private static int loggingTimeOffset = 0; // ms;

    private static String ch01 = "Double";
    private static String dummy = "dummy";

    private static HashMap<String, LogChannel> logChannelList = new HashMap<>();
    private static Calendar calendar = new GregorianCalendar();

    @BeforeClass
    public static void setup() {

        int sub = (int) (calendar.getTimeInMillis() % 10l);
        calendar.add(Calendar.MILLISECOND, -loggingInterval * 5 - sub);

        TestUtils.createTestFolder();
        TestUtils.deleteExistingFile(loggingInterval, loggingTimeOffset, calendar);

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "dummy description", dummy, ValueType.DOUBLE,
                loggingInterval, loggingTimeOffset);

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

        LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "dummy description", dummy, ValueType.DOUBLE,
                loggingInterval, loggingTimeOffset);
    }

    // ####################################################################################################################
    // ####################################################################################################################
    // ####################################################################################################################

    private static LogIntervalContainerGroup getGroup(long timeStamp, int i) {

        LogIntervalContainerGroup group = new LogIntervalContainerGroup();

        LogRecordContainer container1 = new LogRecordContainerImpl(ch01,
                new Record(new DoubleValue(i * 7 - 0.555), timeStamp));

        group.add(container1);

        return group;
    }
}
