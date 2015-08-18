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
package org.openmuc.framework.datalogger.ascii.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

	private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);

	/**
	 * Returns all filenames of the given timespan defined by the two dates
	 * 
	 * @param loggingInterval
	 * @param startTimestamp
	 * @param endTimestamp
	 * @return a list of files which within the timespan
	 */
	public static List<String> getFilenames(int loggingInterval, int logTimeOffset, long startTimestamp,
			long endTimestamp) {

		Calendar calendarStart = new GregorianCalendar(Locale.getDefault());
		calendarStart.setTimeInMillis(startTimestamp);
		Calendar calendarEnd = new GregorianCalendar(Locale.getDefault());
		calendarEnd.setTimeInMillis(endTimestamp);

		// Rename timespanToFilenames....
		// Filename YYYYMMDD_<LoggingIntervall>.dat
		List<String> filenames = new ArrayList<String>();
		while (calendarStart.before(calendarEnd) || calendarStart.equals(calendarEnd)) {
			String filename = buildFilename(loggingInterval, logTimeOffset, calendarStart);
			filenames.add(filename);

			// set date to 00:00:00 of the next day
			calendarStart.add(Calendar.DAY_OF_MONTH, 1);
			calendarStart.set(Calendar.HOUR_OF_DAY, 0);
			calendarStart.set(Calendar.MINUTE, 0);
			calendarStart.set(Calendar.SECOND, 0);
			calendarStart.set(Calendar.MILLISECOND, 0);
		}
		return filenames;
	}

	/**
	 * Returns the filename, with the help of the timestamp and the interval.
	 * 
	 * @param loggingInterval
	 * @param timestamp
	 * @return a filename from timestamp (date) and interval
	 */
	public static String getFilename(int loggingInterval, int logTimeOffset, long timestamp) {

		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		calendar.setTimeInMillis(timestamp);
		return buildFilename(loggingInterval, logTimeOffset, calendar);
	}

	/**
	 * Builds the Logfile name from logging interval, logging time offset and the date of the calendar
	 * 
	 * @param loggingInterval
	 * @param calendar
	 * @return logfile name
	 */
	public static String buildFilename(int loggingInterval, int logTimeOffset, Calendar calendar) {

		StringBuilder sb = new StringBuilder();
		sb.append(String.format(Const.DATE_FORMAT, calendar));
		sb.append('_');
		sb.append(String.valueOf(loggingInterval));

		if (logTimeOffset != 0) {
			sb.append('_');
			sb.append(logTimeOffset);
		}
		sb.append(Const.EXTENSION);
		return sb.toString();
	}

	/**
	 * Builds the Logfile name from string interval_timeOffset and the date of the calendar
	 * 
	 * @param loggingInterval
	 * @param calendar
	 * @return logfile name
	 */
	public static String buildFilename(String interval_timeOffset, Calendar calendar) {

		StringBuilder sb = new StringBuilder();
		sb.append(String.format(Const.DATE_FORMAT, calendar));
		sb.append('_');
		sb.append(interval_timeOffset);

		sb.append(Const.EXTENSION);
		return sb.toString();
	}

	/**
	 * Checks if it has a next container entry.
	 * 
	 * @param containers
	 * @param i
	 * @return true if it has a next container entry, if not else.
	 */
	public static boolean hasNext(List<LogRecordContainer> containers, int i) {

		boolean result = false;
		if (i <= containers.size() - 2) {
			result = true;
		}
		return result;
	}

	/**
	 * This method rename all *.dat files with the date from today in directoryPath into a *.old0, *.old1, ...
	 * 
	 * @param directoryPath
	 * @param calendar
	 */
	public static void renameAllFilesToOld(String directoryPath, Calendar calendar) {

		String date = String.format(Const.DATE_FORMAT, calendar);

		File dir = new File(directoryPath);
		File[] files = dir.listFiles();

		for (File file : files) {
			String currentName = file.getName();
			if (currentName.startsWith(date) && currentName.endsWith(Const.EXTENSION)) {

				String newName = currentName.substring(0, currentName.length() - Const.EXTENSION.length());
				newName += Const.EXTENSION_OLD;
				int j = 0;

				File fileWithNewName = new File(directoryPath + newName + j);

				while (fileWithNewName.exists()) {
					++j;
					fileWithNewName = new File(directoryPath + newName + j);
				}
				if (!file.renameTo(fileWithNewName)) {
					logger.error("Could not rename file to " + newName);
				}
			}
		}
	}

	/**
	 * This method renames a singel &lt;date&gt;_&lt;loggerIntervall&gt;_&lt;loggerTimeOffset&gt;.dat file into a
	 * *.old0, *.old1, ...
	 * 
	 * @param directoryPath
	 * @param calendar
	 */
	public static void renameFileToOld(String directoryPath, String loggerIntervall_loggerTimeOffset, Calendar calendar) {

		File file = new File(directoryPath + buildFilename(loggerIntervall_loggerTimeOffset, calendar));

		if (file.exists()) {
			String currentName = file.getName();

			String newName = currentName.substring(0, currentName.length() - Const.EXTENSION.length());
			newName += Const.EXTENSION_OLD;
			int j = 0;

			File fileWithNewName = new File(directoryPath + newName + j);

			while (fileWithNewName.exists()) {
				++j;
				fileWithNewName = new File(directoryPath + newName + j);
			}
			if (!file.renameTo(fileWithNewName)) {
				logger.error("Could not rename file to " + newName);
			}
		}
	}

	/**
	 * Returns the calendar from today with the first hour, minute, second and millisecond.
	 * 
	 * @param today
	 * @return the calendar from today with the first hour, minute, second and millisecond
	 */
	public static Calendar getCalendarTodayZero(Calendar today) {

		Calendar calendarZero = new GregorianCalendar(Locale.getDefault());
		calendarZero.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DATE), 0, 0, 0);
		calendarZero.set(Calendar.MILLISECOND, 0);

		return calendarZero;
	}

	/**
	 * This method adds a string value up with blank spaces from left to right.
	 * 
	 * @param value
	 *            String value to fill up
	 * @param size
	 *            maximal allowed size
	 * @return the modified string.
	 */
	public static String addSpacesLeft(String value, int size) {

		StringBuilder sb = new StringBuilder();
		int i = value.length();
		while (i < size) {
			sb.append(' ');
			++i;
		}
		sb.append(value);
		return sb.toString();
	}

	/**
	 * This method adds a string value up with blank spaces from right to left.
	 * 
	 * @param value
	 *            String value to fill up
	 * @param size
	 *            maximal allowed size
	 * @return the modified string.
	 */
	public static String addSpacesRight(String value, int size) {

		StringBuilder sb = new StringBuilder();
		sb.append(value);
		int i = value.length();
		while (i < size) {
			sb.append(' ');
			++i;
		}
		return sb.toString();
	}

	/**
	 * This method adds a string value up with blank spaces from left to right.
	 * 
	 * @param value
	 *            String value to fill up
	 * @param size
	 *            maximal allowed size
	 */
	public static void appendSpaces(StringBuilder sb, int number) {

		for (int i = 0; i < number; ++i) {
			sb.append(' ');
		}
	}

	/**
	 * Construct a error value with the flag.
	 * 
	 * @param flag
	 * @return a string with the flag and the standard error prefix.
	 */
	public static String buildError(Flag flag) {

		return Const.ERROR + flag.getCode();
	}

	/**
	 * Get the column number by name.
	 * 
	 * @param line
	 * @param name
	 * @return the column number as int.
	 */
	public static int getColumnNumberByName(String line, String name) {

		int channelColumn = -1;

		// erste Zeile ohne Kommentar finden dann den Spaltennamen suchen und dessen Possitionsnummer zurückgeben.
		if (!line.startsWith(Const.COMMENT_SIGN)) {
			String columns[] = line.split(Const.SEPARATOR);
			int i = 0;
			while (i < columns.length) {
				if (name.equals(columns[i])) {
					return i;
				}
				i++;
			}
		}
		return channelColumn;
	}

	/**
	 * Get the column number by name, in comments. It searches the line by his self. The BufferdReader has to be on the
	 * begin of the file.
	 * 
	 * @param name
	 *            the name to search
	 * @param br
	 *            the BufferedReader
	 * @return column number as int, -1 if name not found
	 * @throws IOException
	 */
	public static int getCommentColumnNumberByName(String name, BufferedReader br) throws IOException,
			NullPointerException {

		String line = br.readLine();

		while (line != null && line.startsWith(Const.COMMENT_SIGN)) {
			if (line.contains(name)) {
				String columns[] = line.split(Const.SEPARATOR);
				for (int i = 0; i < columns.length; i++) {
					if (name.equals(columns[i])) {
						return i;
					}
				}
			}
			try {
				line = br.readLine();
			} catch (NullPointerException e) {
				return -1;
			}
		}
		return -1;
	}

	/**
	 * 
	 * @param col_no
	 *            the number of the channel
	 * @param column
	 *            the column
	 * @param br
	 *            a BufferedReader
	 * @return the value of a column of a specific col_num
	 * @throws IOException
	 */
	public static String getCommentValue(int col_no, int column, BufferedReader br) throws IOException {

		String line = br.readLine();
		String columnName = String.format("%03d", col_no);

		while (line != null && line.startsWith(Const.COMMENT_SIGN)) {
			if (line.startsWith(Const.COMMENT_SIGN + columnName)) {
				String columns[] = line.split(Const.SEPARATOR);
				return columns[column];
			}
			line = br.readLine();
		}
		return null;
	}

	/**
	 * Identifies the ValueType of a logger value on a specific col_no
	 * 
	 * @param columnNumber
	 *            column number
	 * @param dataFile
	 *            the logger data file
	 * @return the ValueType from col_num x
	 */
	public static ValueType identifyValueType(int columnNumber, File dataFile) {

		String valueType = getValueTypeAsString(columnNumber, dataFile);
		String valueTypeArray[] = valueType.split(Const.VALUETYPE_ENDSIGN);

		return ValueType.valueOf(valueTypeArray[0]);
	}

	public static int getValueTypeLengthFromFile(int columnNumber, File dataFile) {

		String valueType = getValueTypeAsString(columnNumber, dataFile);
		return getByteStringLength(valueType);
	}

	private static String getValueTypeAsString(int columnNumber, File dataFile) {

		BufferedReader br = null;
		String value = "";

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "US-ASCII"));
			int column = LoggerUtils.getCommentColumnNumberByName(Const.COMMENT_NAME, br);

			if (column != -1) {
				value = LoggerUtils.getCommentValue(columnNumber, column, br);
				value = value.split(Const.VALUETYPE_ENDSIGN)[0];
			}
			else {
				throw new NoSuchElementException("No element with name \"" + Const.COMMENT_NAME + "\" found.");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		} finally {
			closeBufferdReader(br);
		}
		return value;
	}

	/**
	 * Returns the predefined size of a ValueType.
	 * 
	 * @param valueType
	 * @return predefined size of a ValueType as int.
	 */
	public static int getLengthOfValueType(ValueType valueType) {

		int size;

		switch (valueType) {
		case DOUBLE:
			size = Const.VALUE_SIZE_DOUBLE;
			break;
		case FLOAT:
			size = Const.VALUE_SIZE_DOUBLE;
			break;
		case INTEGER:
			size = Const.VALUE_SIZE_INTEGER;
			break;
		case LONG:
			size = Const.VALUE_SIZE_LONG;
			break;
		case SHORT:
			size = Const.VALUE_SIZE_SHORT;
			break;
		case BYTE_ARRAY:
			size = Const.VALUE_SIZE_MINIMAL;
			break;
		case STRING:
			size = Const.VALUE_SIZE_MINIMAL;
			break;
		case BOOLEAN:
		case BYTE:
		default:
			size = Const.VALUE_SIZE_MINIMAL;
			break;
		}
		return size;
	}

	/**
	 * Converts a byte array to an hexadecimal string
	 * 
	 * @param byteArray
	 * @return hexadecimal string
	 */
	public static String byteArrayToHexString(byte[] byteArray) {

		char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[byteArray.length * 2];
		for (int j = 0; j < byteArray.length; j++) {
			int v = byteArray[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Constructs the timestamp for every log value into a StringBuilder.
	 * 
	 * @param sb
	 * @param calendar
	 */
	public static void setLoggerTimestamps(StringBuilder sb, Calendar calendar) {

		double unixtimestamp_sec = calendar.getTimeInMillis() / 1000.0; // double for milliseconds, nanoseconds

		sb.append(String.format(Const.DATE_FORMAT, calendar));
		sb.append(Const.SEPARATOR);
		sb.append(String.format(Const.TIME_FORMAT, calendar));
		sb.append(Const.SEPARATOR);
		sb.append(String.format(Locale.ENGLISH, "%10.3f", unixtimestamp_sec));
		sb.append(Const.SEPARATOR);
	}

	/**
	 * Constructs the timestamp for every log value into a StringBuilder.
	 * 
	 * @param sb
	 * @param unixTimeStamp
	 *            unix time stamp in ms
	 */
	public static void setLoggerTimestamps(StringBuilder sb, long unixTimeStamp) {

		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		calendar.setTimeInMillis(unixTimeStamp);
		double unixtimestamp_sec = unixTimeStamp / 1000.0; // double for milliseconds, nanoseconds

		sb.append(String.format(Const.DATE_FORMAT, calendar));
		sb.append(Const.SEPARATOR);
		sb.append(String.format(Const.TIME_FORMAT, calendar));
		sb.append(Const.SEPARATOR);
		sb.append(String.format(Locale.ENGLISH, "%10.3f", unixtimestamp_sec));
		sb.append(Const.SEPARATOR);
	}

	public static String getHeaderFromFile(String filePath, String logIntervall_logTimeOffset) {

		BufferedReader br = LoggerUtils.getBufferedReader(new File(filePath));

		if (br != null) {
			StringBuilder sb = new StringBuilder();

			try {
				String line = br.readLine();

				if (line != null) {
					sb.append(line);
					while (line != null && line.startsWith(Const.COMMENT_SIGN)) {
						sb.append(Const.LINESEPARATOR);
						line = br.readLine();
						sb.append(line);
					}
				}
			} catch (IOException e) {
				logger.error("Problems to handle file: " + filePath, e);
			} finally {

				try {
					br.close();
				} catch (IOException e) {
					logger.error("Can not close file: " + filePath, e);
				}
			}
			return sb.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Returns a RandomAccessFile of the specified file.
	 * 
	 * @param filePath
	 * @param accsesMode
	 * @return the RandomAccessFile of the specified file
	 */
	public static RandomAccessFile getRandomAccessFile(File file, String accsesMode) {

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, accsesMode);
		} catch (FileNotFoundException e) {

			logger.warn("Requested logfile: '" + file.getAbsolutePath() + "' not found.");
			// e.printStackTrace();
		}
		return raf;
	}

	public static BufferedReader getBufferedReader(File file) {

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Const.CHAR_SET));
		} catch (IOException e) {
			logger.error("Can not open file: " + file.getAbsolutePath());
		}
		return reader;
	}

	public static PrintWriter getPrintWriter(File file, boolean append) {

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append), Const.CHAR_SET));
		} catch (IOException e) {
			logger.error("Can not open file: " + file.getAbsolutePath());
		}
		return writer;
	}

	public static void fillUpFileWithErrorCode(String directoryPath, String loggerIntervall_loggerTimeOffset,
			Calendar calendar) {

		String filename = buildFilename(loggerIntervall_loggerTimeOffset, calendar);
		File file = new File(directoryPath + filename);
		RandomAccessFile raf = LoggerUtils.getRandomAccessFile(file, "r");
		PrintWriter out = null;

		String firstLogLine = "";
		String lastLogLine = "";
		long loggingIntervall = 0;

		if (loggerIntervall_loggerTimeOffset.contains("_")) {
			loggingIntervall = Long.parseLong(loggerIntervall_loggerTimeOffset.split("_")[0]);
		}
		else {
			loggingIntervall = Long.parseLong(loggerIntervall_loggerTimeOffset);
		}

		// String restOfLastLine = "";
		long unixTimeStamp = 0;

		if (raf != null) {
			try {
				String line = raf.readLine();
				if (line != null) {

					while (line.startsWith(Const.COMMENT_SIGN)) {
						// do nothing with this data, only for finding the begin of logging
						line = raf.readLine();
					}
					firstLogLine = raf.readLine();
				}

				// read last line backwards and read last line
				byte[] byti = new byte[1];
				long filePosition = file.length() - 2;
				String charString;
				while (lastLogLine.isEmpty() && filePosition > 0) {

					raf.seek(filePosition);
					int readedBytes = raf.read(byti);
					if (readedBytes == 1) {
						charString = new String(byti, Const.CHAR_SET);

						if (charString.equals(Const.LINESEPARATOR_STRING)) {
							lastLogLine = raf.readLine();
						}
						else {
							filePosition -= 1;
						}
					}
					else {
						filePosition = -1; // leave the while loop
					}
				}
				raf.close();

				int firstLogLineLength = firstLogLine.length();

				int lastLogLineLength = lastLogLine.length();

				if (firstLogLineLength != lastLogLineLength) {
					/**
					 * TODO: different size of logging lines, probably the last one is corrupted we have to fill it up
					 * restOfLastLine = completeLastLine(firstLogLine, lastLogLine); raf.writeChars(restOfLastLine);
					 */
					// File is corrupted rename to old
					renameFileToOld(directoryPath, loggerIntervall_loggerTimeOffset, calendar);
				}
				else {

					String lineArray[] = lastLogLine.split(Const.SEPARATOR);

					StringBuilder errorValues = getErrorValues(lineArray);
					unixTimeStamp = ((long) Double.parseDouble(lineArray[2])) * 1000;

					// FileChannel fileChannel = raf.getChannel();
					out = getPrintWriter(file, true);

					long numberOfFillUpLines = getNumberOfFillUpLines(unixTimeStamp, loggingIntervall);

					while (numberOfFillUpLines > 0) {

						unixTimeStamp = fillUp(out, unixTimeStamp, loggingIntervall, lastLogLineLength,
								numberOfFillUpLines, errorValues);
						numberOfFillUpLines = getNumberOfFillUpLines(unixTimeStamp, loggingIntervall);
					}
					out.close();
				}
			} catch (IOException e) {
				logger.error("Could not read file " + file.getAbsolutePath(), e);
				renameFileToOld(directoryPath, loggerIntervall_loggerTimeOffset, calendar);
			} finally {
				try {

					if (raf != null) {
						raf.close();
					}
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
					logger.error("Could not close file " + file.getAbsolutePath());
				}
			}
		}
	}

	private static long fillUp(PrintWriter out, long unixTimeStamp, long loggingIntervall, int lastLogLineLength,
			long numberOfFillUpLines, StringBuilder errorValues) throws IOException {

		StringBuilder line = new StringBuilder();
		for (int i = 0; i < numberOfFillUpLines; i++) {

			line.setLength(0);
			unixTimeStamp += loggingIntervall;
			setLoggerTimestamps(line, unixTimeStamp);
			line.append(errorValues);
			line.append(Const.LINESEPARATOR);

			out.append(line.toString());
		}

		return unixTimeStamp;
	}

	private static long getNumberOfFillUpLines(long lastUnixTimeStamp, long loggingIntervall) {

		long numberOfFillUpLines = 0;
		long currentUnixTimeStamp = System.currentTimeMillis();

		numberOfFillUpLines = (currentUnixTimeStamp - lastUnixTimeStamp) / loggingIntervall;

		return numberOfFillUpLines;
	}

	/**
	 * 
	 * @param errorValues
	 *            has to be empty at begin
	 * @param logLine
	 * @return
	 */
	private static StringBuilder getErrorValues(String lineArray[]) {

		StringBuilder errorValues = new StringBuilder();
		int arrayLength = lineArray.length;
		int errorCodeLength = Const.ERROR.length() + 2;
		int separatorLength = Const.SEPARATOR.length();
		int length = 0;

		for (int i = 3; i < arrayLength; ++i) {

			length = lineArray[i].length();
			length -= errorCodeLength;
			if (i > arrayLength - 1) {
				length -= separatorLength;
			}
			appendSpaces(errorValues, length);
			errorValues.append(Const.ERROR);
			errorValues.append(Flag.DATA_LOGGING_NOT_ACTIVE.getCode());

			if (i < arrayLength - 1) {
				errorValues.append(Const.SEPARATOR);
			}
		}
		return errorValues;
	}

	// private static String completeLastLine(String firstLogLine, String lastLogLine) {
	//
	// // TODO different size of logging lines, probably the last one is corrupted we have to fill it up
	// // TODO: wenn letzte Zeile zu defekt ist also ohne Timestamp, löschen und vorletzte Zeile nehmen und
	// // dessen Zeit.
	// int firstLogLineLength = firstLogLine.length();
	// int lastLogLineLength = lastLogLine.length();
	//
	// return "";
	// }

	/**
	 * Get the length from a type+length tuple. Example: "Byte_String,95"
	 * 
	 * @param string
	 *            has to be a string with ByteType and length.
	 * @param dataFile
	 *            the logger data file
	 * @return the length of a ByteString in int.
	 */
	private static int getByteStringLength(String string) {

		String stringArray[];
		int size;

		stringArray = string.split(Const.VALUETYPE_SIZE_SEPARATOR);
		try {
			size = Integer.parseInt(stringArray[1]);
		} catch (NumberFormatException e) {
			logger.warn("Not able to get ValueType length from String. Set length to minimal lenght "
					+ Const.VALUE_SIZE_MINIMAL + ".");
			size = Const.VALUE_SIZE_MINIMAL;
		}
		return size;
	}

	private static void closeBufferdReader(BufferedReader br) {

		try {
			br.close();
		} catch (Exception e1) {
		}
	}

}
