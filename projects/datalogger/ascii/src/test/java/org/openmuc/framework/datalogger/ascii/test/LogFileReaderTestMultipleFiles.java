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
package org.openmuc.framework.datalogger.ascii.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.core.datamanager.LogRecordContainerImpl;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileReader;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;

public class LogFileReaderTestMultipleFiles {

    // t1 = start timestamp of requestet interval
    // t2 = end timestamp of requestet interval

    private static String fileDate0 = "20770707";
    private static String fileDate1 = "20770708";
    private static String fileDate2 = "20770709";
    private static int loggingInterval = 60000; // ms;
    static int loggingTimeOffset = 0; // ms
    private final static String Channel0Name = "power";
    private final static String EXT = ".dat";
    // private static String[] channelIds = new String[] { Channel0Name };
    private static String dateFormat = "yyyyMMdd HH:mm:ss";
    // private static String ext = ".dat";

    LogChannelTestImpl channelTestImpl = new LogChannelTestImpl(Channel0Name, "Comment", "W", ValueType.DOUBLE,
            loggingInterval, loggingTimeOffset);

    @BeforeClass
    public static void setup() {

        System.out.println("### Setup() LogFileReaderTestMultipleFiles");

        TestSuite.createTestFolder();
        // drei Dateien

        // 1 Kanal im Sekunden-Takt loggen über von 23 Uhr bis 1 Uhr des übernächsten Tages
        // --> Ergebnis müssten drei
        // Dateien sein die vom LogFileWriter erstellt wurden

        String filename0 = TestUtils.TESTFOLDERPATH + fileDate0 + "_" + loggingInterval + EXT;
        String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + EXT;
        String filename2 = TestUtils.TESTFOLDERPATH + fileDate2 + "_" + loggingInterval + EXT;

        File file0 = new File(filename0);
        File file1 = new File(filename1);
        File file2 = new File(filename2);

        if (file0.exists()) {
            System.out.println("Delete File " + filename2);
            file0.delete();
        }
        if (file1.exists()) {
            System.out.println("Delete File " + filename1);
            file1.delete();
        }
        if (file2.exists()) {
            System.out.println("Delete File " + filename2);
            file2.delete();
        }

        HashMap<String, LogChannel> logChannelList = new HashMap<>();

        LogChannelTestImpl ch0 = new LogChannelTestImpl("power", "dummy description", "kW", ValueType.DOUBLE,
                loggingInterval, loggingTimeOffset);

        logChannelList.put(Channel0Name, ch0);

        Calendar calendar = TestUtils.stringToDate(dateFormat, fileDate0 + " 23:00:00");

        int hour = 3600;

        for (int i = 0; i < ((hour * 24 + hour * 2) * (1000d / loggingInterval)); i++) {

            LogRecordContainer container1 = new LogRecordContainerImpl(Channel0Name,
                    new Record(new DoubleValue(1), calendar.getTimeInMillis()));

            LogIntervalContainerGroup group = new LogIntervalContainerGroup();
            group.add(container1);

            LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, false);
            lfw.log(group, loggingInterval, 0, calendar, logChannelList);
            AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, 0, calendar.getTimeInMillis());

            calendar.add(Calendar.MILLISECOND, loggingInterval);
        }
        // }
    }

    @AfterClass
    public static void tearDown() {

        System.out.println("tearing down");
        TestSuite.deleteTestFolder();
    }

    @Test
    public void tc009_t1_t2_within_available_data_with_three_files() {

        System.out.println("### Begin test tc009_t1_t2_within_available_data_with_three_files");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 23:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate2 + " 00:59:" + (60 - loggingInterval / 1000))
                .getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2);

        int hour = 3600;
        long expectedRecords = (hour * 24 + hour * 2) / (loggingInterval / 1000);
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());

        boolean result;

        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected); ");
        assertTrue(result);
    }
}
