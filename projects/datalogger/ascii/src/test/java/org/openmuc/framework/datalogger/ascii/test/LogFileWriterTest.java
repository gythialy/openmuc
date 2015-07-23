/*
 * Copyright 2011-15 Fraunhofer ISE
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.core.datamanager.LogRecordContainerImpl;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;

public class LogFileWriterTest {

	// t1 = start timestamp of requestet interval
	// t2 = end timestamp of requestet interval

	static int loggingInterval = 10000; // ms;
	static int loggingTimeOffset = 0; // ms;
	static String ext = ".dat";

	static String dateFormat = "yyyyMMdd HH:mm:s";
	static String fileDate1 = "20880808";
	static String fileDate2 = "20880809";

	static String ch01 = "FLOAT";
	static String ch02 = "DOUBLE";
	static String ch03 = "BOOLEAN";
	static String ch04 = "SHORT";
	static String ch05 = "INTEGER";
	static String ch06 = "LONG";
	static String ch07 = "BYTE";
	static String ch08 = "STRING";
	static String ch09 = "BYTE_ARRAY";
	static String dummy = "dummy";
	static String[] channelIds = new String[] { ch01, ch02, ch03, ch04, ch05, ch06, ch07, ch08, ch09 };
	static String time = " 23:55:00";
	static String testStringValueIncorrectASCII = "qwertzuiopü+asdfghjklöä#<yxcvbnm,.-^1234567890ß °!§$%&/()=?QWERTZUIOPÜ*ASDFGHJKLÖÄ'>YXCVBNM;:_"; // 94
																																					// Zeichen
	static String testStringValueCorrect = "qwertzuiop+asdfghjkl#<yxcvbnm,.-^1234567890 !$%&/()=?QWERTZUIOP*ASDFGHJKL'>YXCVBNM;:_";
	static String testStringValueIncorrect = "qwertzuiop+asdfghjkl#<yxcvbnm,.-^1234567890 " + Const.SEPARATOR
			+ "!$%&/()=?QWERTZUIOP*SDFGHJKL'>YXCVBNM;:_";
	static byte[] testByteArray = { 1, 2, 3, 4, -5, -9, 0 };

	static int valueLength = 100;
	static int valueLengthByteArray = testByteArray.length;

	@BeforeClass
	public static void setup() {

		TestSuite.createTestFolder();

		// 2 Kanäle im Stunden-Takt loggen über von 12 Uhr bis 12 Uhr des nächsten Tages
		// --> Ergebnis müssten zwei Dateien sein die vom LogFileWriter erstellt wurden

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

		HashMap<String, LogChannel> logChannelList = new HashMap<String, LogChannel>();

		LogChannelTestImpl ch1 = new LogChannelTestImpl(ch01, "dummy description", dummy, ValueType.FLOAT,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch2 = new LogChannelTestImpl(ch02, "dummy description", dummy, ValueType.DOUBLE,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch3 = new LogChannelTestImpl(ch03, "dummy description", dummy, ValueType.BOOLEAN,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch4 = new LogChannelTestImpl(ch04, "dummy description", dummy, ValueType.SHORT,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch5 = new LogChannelTestImpl(ch05, "dummy description", dummy, ValueType.INTEGER,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch6 = new LogChannelTestImpl(ch06, "dummy description", dummy, ValueType.LONG,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch7 = new LogChannelTestImpl(ch07, "dummy description", dummy, ValueType.BYTE,
				loggingInterval, loggingTimeOffset);
		LogChannelTestImpl ch8 = new LogChannelTestImpl(ch08, "dummy description", dummy, ValueType.STRING,
				loggingInterval, loggingTimeOffset, valueLength);
		LogChannelTestImpl ch9 = new LogChannelTestImpl(ch09, "dummy description", dummy, ValueType.BYTE_ARRAY,
				loggingInterval, loggingTimeOffset, valueLengthByteArray);

		logChannelList.put(ch01, ch1);
		logChannelList.put(ch02, ch2);
		logChannelList.put(ch03, ch3);
		logChannelList.put(ch04, ch4);
		logChannelList.put(ch05, ch5);
		logChannelList.put(ch06, ch6);
		logChannelList.put(ch07, ch7);
		logChannelList.put(ch08, ch8);
		logChannelList.put(ch09, ch9);

		Date date = TestUtils.stringToDate(dateFormat, fileDate1 + time);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		long time = date.getTime();
		boolean boolValue;
		byte byteValue = 0;

		String testString;

		// writes 24 records for 2 channels from 12 o'clock till 12 o'clock of the other day
		for (long i = 0; i < ((60 * 10) * (1000d / loggingInterval)); i++) {

			if ((i % 2) > 0) {
				boolValue = true;
				testString = testStringValueCorrect;
			}
			else {
				boolValue = false;
				testString = testStringValueIncorrect;
			}
			// System.out.println("TEST = " + (i+0.555F));
			LogRecordContainer container1 = new LogRecordContainerImpl(ch01, new Record(
					new FloatValue(i * -7 - 0.555F), time));
			LogRecordContainer container2 = new LogRecordContainerImpl(ch02, new Record(
					new DoubleValue(i * +7 - 0.555), time));
			LogRecordContainer container3 = new LogRecordContainerImpl(ch03, new Record(new BooleanValue(boolValue),
					time));
			LogRecordContainer container4 = new LogRecordContainerImpl(ch04,
					new Record(new ShortValue((short) i), time));
			LogRecordContainer container5 = new LogRecordContainerImpl(ch05, new Record(new IntValue((int) i), time));
			LogRecordContainer container6 = new LogRecordContainerImpl(ch06, new Record(new LongValue(i * 1000000),
					time));
			LogRecordContainer container7 = new LogRecordContainerImpl(ch07, new Record(new ByteValue(byteValue), time));
			LogRecordContainer container8 = new LogRecordContainerImpl(ch08, new Record(new StringValue(testString),
					time));
			LogRecordContainer container9 = new LogRecordContainerImpl(ch09, new Record(new ByteArrayValue(
					testByteArray), time));

			LogIntervalContainerGroup group = new LogIntervalContainerGroup();
			group.add(container1);
			group.add(container2);
			group.add(container3);
			group.add(container4);
			group.add(container5);
			group.add(container6);
			group.add(container7);
			group.add(container8);
			group.add(container9);

			LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH);
			lfw.log(group, loggingInterval, loggingTimeOffset, date, logChannelList);

			calendar.add(Calendar.MILLISECOND, loggingInterval);
			date = calendar.getTime();

			++byteValue;
		}
	}

	@AfterClass
	public static void tearDown() {
		System.out.println("tearing down");
		TestSuite.deleteTestFolder();
	}

	@Test
	public void tc300_check_if_new_file_is_created_on_day_change() {

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
		System.out.println(file1.getAbsolutePath());
		System.out.println(file2.getAbsolutePath() + "\nTwo files created = " + assertT);

		assertTrue(assertT);
	}
}
