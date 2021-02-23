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
package org.openmuc.framework.datalogger.ascii.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.LogFileReader;

public class LogFileReaderTestBrokenFile {

    private String fileDate;

    String dateFormat = "yyyyMMdd HH:mm:ss";
    private static final int loggingInterval = 1000; // ms
    static int loggingTimeOffset = 0; // ms
    private static final String Channel0Name = "power";

    LogChannelTestImpl channelTestImpl = new LogChannelTestImpl(Channel0Name, "", "Comment", "W", ValueType.DOUBLE, 0.0,
            0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

    @Test
    public void tc200_logfile_does_not_exist() {

        System.out.println("### Begin test tc200_logfile_does_not_exist");

        fileDate = "20131201";

        long t1 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:10").getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        long expectedRecords = 0;
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");

        if (records.size() == expectedRecords) {
            assertTrue(true);
        }
        else {
            assertTrue(false);
        }

    }

    @AfterAll
    public static void tearDown() {

        System.out.println("tearing down");
        TestUtils.deleteTestFolder();
    }

    // @Ignore
    // @Test
    // public void tc201_no_header_in_logfile() {
    //
    // System.out.println("### Begin test tc201_no_header_in_logfile");
    //
    // fileDate = "20131202";
    //
    // String ext = ".dat";
    // long startTimestampFile = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:00").getTime();
    // long endTimestampFile = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:30").getTime();
    // String[] channelIds = new String[] { "power", "energy" };
    //
    // String filename = TestUtils.TESTFOLDER + "/" + fileDate + "_" + loggingInterval + ext;
    // createLogFileWithoutHeader(filename, channelIds, startTimestampFile, endTimestampFile, loggingInterval);
    //
    // long t1 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:00").getTime();
    // long t2 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:10").getTime();
    //
    // LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
    // List<Record> records = fr.getValues(t1, t2);
    //
    // long expectedRecords = 0;
    // System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
    // System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
    //
    // if (records.size() == expectedRecords) {
    // assertTrue(true);
    // }
    // else {
    // assertTrue(false);
    // }
    //
    // }

    // @Ignore
    // @Test
    // public void tc202_channelId_not_in_header() {
    //
    // System.out.println("### Begin test tc201_no_header_in_logfile");
    //
    // fileDate = "20131202";
    //
    // String ext = ".dat";
    // long startTimestampFile = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:00").getTime();
    // long endTimestampFile = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:30").getTime();
    // String[] channelIds = new String[] { "energy" };
    //
    // String filename = TestUtils.TESTFOLDER + "/" + fileDate + "_" + loggingInterval + ext;
    // createLogFile(filename, channelIds, startTimestampFile, endTimestampFile, loggingInterval);
    //
    // long t1 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:00").getTime();
    // long t2 = TestUtils.stringToDate(dateFormat, fileDate + " 12:00:10").getTime();
    //
    // LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
    // List<Record> records = fr.getValues(t1, t2);
    //
    // long expectedRecords = 0;
    // System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());
    // System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected)");
    //
    // if (records.size() == expectedRecords) {
    // assertTrue(true);
    // }
    // else {
    // assertTrue(false);
    // }
    // }

}
