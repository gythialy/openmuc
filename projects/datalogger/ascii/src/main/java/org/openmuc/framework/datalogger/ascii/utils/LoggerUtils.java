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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.LogFileHeader;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private LoggerUtils() {
    }

    /**
     * Returns all filenames of the given time span defined by the two dates
     *
     * @param loggingInterval
     *            logging interval
     * @param logTimeOffset
     *            logging time offset
     * @param startTimestamp
     *            start time stamp
     * @param endTimestamp
     *            end time stamp
     * @return a list of strings with all files names
     */
    public static List<String> getFilenames(int loggingInterval, int logTimeOffset, long startTimestamp,
            long endTimestamp) {

        Calendar calendarStart = new GregorianCalendar(Locale.getDefault());
        calendarStart.setTimeInMillis(startTimestamp);
        Calendar calendarEnd = new GregorianCalendar(Locale.getDefault());
        calendarEnd.setTimeInMillis(endTimestamp);

        // Rename timespanToFilenames....
        // Filename YYYYMMDD_<LoggingInterval>.dat
        List<String> filenames = new ArrayList<>();
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
     *            logging interval
     * @param logTimeOffset
     *            logging time offset
     * @param timestamp
     *            timestamp
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
     *            logging interval
     * @param logTimeOffset
     *            logging time offset
     * @param calendar
     *            Calendar for the time of the file name
     * @return logging file name
     */
    public static String buildFilename(int loggingInterval, int logTimeOffset, Calendar calendar) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Const.DATE_FORMAT, calendar));
        sb.append(Const.TIME_SEPERATOR);
        sb.append(String.valueOf(loggingInterval));

        if (logTimeOffset != 0) {
            sb.append(Const.TIME_SEPERATOR);
            sb.append(logTimeOffset);
        }
        sb.append(Const.EXTENSION);
        return sb.toString();
    }

    /**
     * Builds the Logfile name from string interval_timeOffset and the date of the calendar
     *
     * @param intervalTimeOffset
     *            the IntervallTimeOffset
     * @param calendar
     *            Calendar for the time of the file name
     * @return logfile name
     */
    public static String buildFilename(String intervalTimeOffset, Calendar calendar) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Const.DATE_FORMAT, calendar));
        sb.append(Const.TIME_SEPERATOR);
        sb.append(intervalTimeOffset);

        sb.append(Const.EXTENSION);
        return sb.toString();
    }

    /**
     * Checks if it has a next container entry.
     *
     * @param containers
     *            a list with LogRecordContainer
     * @param i
     *            the current possition of the list
     * @return true if it has a next container entry, if not else.
     */
    public static boolean hasNext(List<LoggingRecord> containers, int i) {

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
     *            directory path
     * @param calendar
     *            Calendar for the time of the file name
     */
    public static void renameAllFilesToOld(String directoryPath, Calendar calendar) {

        String date = String.format(Const.DATE_FORMAT, calendar);

        File dir = new File(directoryPath);
        File[] files = dir.listFiles();

        if (files != null && files.length > 0) {

            for (File file : files) {
                String currentName = file.getName();
                if (currentName.startsWith(date) && currentName.endsWith(Const.EXTENSION)) {

                    String newName = new StringBuilder()
                            .append(currentName.substring(0, currentName.length() - Const.EXTENSION.length()))
                            .append(Const.EXTENSION_OLD)
                            .toString();
                    int j = 0;

                    File fileWithNewName = new File(directoryPath + newName + j);

                    while (fileWithNewName.exists()) {
                        ++j;
                        fileWithNewName = new File(directoryPath + newName + j);
                    }
                    if (!file.renameTo(fileWithNewName)) {
                        logger.error("Could not rename file to ", newName);
                    }
                }
            }
        }
        else {
            logger.error("No file found in " + directoryPath);
        }
    }

    /**
     * This method renames a singel &lt;date&gt;_&lt;loggerInterval&gt;_&lt;loggerTimeOffset&gt;.dat file into a *.old0,
     * *.old1, ...
     *
     * @param directoryPath
     *            directory path
     * @param loggerIntervalLoggerTimeOffset
     *            logger interval with logger time offset as String separated with underline
     * @param calendar
     *            calendar of the day
     */
    public static void renameFileToOld(String directoryPath, String loggerIntervalLoggerTimeOffset, Calendar calendar) {

        File file = new File(directoryPath + buildFilename(loggerIntervalLoggerTimeOffset, calendar));

        if (file.exists()) {
            String currentName = file.getName();

            if (logger.isTraceEnabled()) {
                logger.trace(MessageFormat.format("Header not identical. Rename file {0} to old.", currentName));
            }

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
     *            the current calendar
     * @return the calendar from today with the first hour, minute, second and millisecond
     */
    public static Calendar getCalendarTodayZero(Calendar today) {

        Calendar calendarZero = new GregorianCalendar(Locale.getDefault());
        calendarZero.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DATE), 0, 0, 0);
        calendarZero.set(Calendar.MILLISECOND, 0);

        return calendarZero;
    }

    /**
     * This method adds a blank spaces to a StringBuilder object.
     *
     * @param length
     *            length of the value to add the spaces
     * @param size
     *            maximal allowed size
     * @param sb
     *            StringBuilder object to add the spaces
     */
    public static void addSpaces(int length, int size, StringBuilder sb) {

        int i = length;
        while (i < size) {
            sb.append(' ');
            ++i;
        }
    }

    /**
     * This method adds a string value up with blank spaces from left to right.
     *
     * @param sb
     *            StringBuilder in wich the spaces will appended
     * @param number
     *            the number of spaces
     */
    public static void appendSpaces(StringBuilder sb, int number) {

        for (int i = 0; i < number; ++i) {
            sb.append(' ');
        }
    }

    /**
     * Construct a error value with standard error prefix and the flag as number.
     *
     * @param flag
     *            the wished error flag
     * @param sbValue
     *            string buffer to add the error flag
     */
    public static void buildError(StringBuilder sbValue, Flag flag) {
        sbValue.setLength(0);
        sbValue.append(Const.ERROR).append(flag.getCode());
    }

    /**
     * Get the column number by name.
     *
     * @param line
     *            the line to search
     * @param name
     *            the name to search in line
     * @return the column number as int.
     */
    public static int getColumnNumberByName(String line, String name) {

        int channelColumn = -1;

        // erst Zeile ohne Kommentar finden, dann den Spaltennamen suchen und dessen Possitionsnummer zurueckgeben.
        if (!line.startsWith(Const.COMMENT_SIGN)) {
            String[] columns = line.split(Const.SEPARATOR);
            for (int i = 0; i < columns.length; i++) {
                if (name.equals(columns[i])) {
                    return i;
                }
            }
        }

        return channelColumn;
    }

    /**
     * Get the columns number by names.
     *
     * @param line
     *            the line to search
     * @param names
     *            the name to search in line
     * @return the column numbers mapped with the name.
     */
    public static Map<String, Integer> getColumnNumbersByNames(String line, String[] names) {

        if (line.startsWith(Const.COMMENT_SIGN)) {
            return null;
        }

        Map<String, Integer> channelColumnsMap = new HashMap<>();
        String[] columns = line.split(Const.SEPARATOR);

        for (int i = 0; i < columns.length; ++i) {
            for (String name : names) {
                if (columns[i].equals(name)) {
                    channelColumnsMap.put(name, i);
                }
            }
        }
        return channelColumnsMap;
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
     *             throws IOException If an I/O error occurs
     */
    public static int getCommentColumnNumberByName(String name, BufferedReader br) throws IOException {

        String line = br.readLine();

        while (line != null && line.startsWith(Const.COMMENT_SIGN)) {
            if (line.contains(name)) {
                String[] columns = line.split(Const.SEPARATOR);
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
     * Get the value which is coded in the comment
     *
     * @param colNumber
     *            the number of the channel
     * @param column
     *            the column
     * @param br
     *            a BufferedReader
     * @return the value of a column of a specific col_num
     * @throws IOException
     *             If an I/O error occurs
     */
    public static String getCommentValue(int colNumber, int column, BufferedReader br) throws IOException {

        final String columnName = String.format("%03d", colNumber);

        String line = br.readLine();
        for (; line != null && line.startsWith(Const.COMMENT_SIGN); line = br.readLine()) {
            if (!line.startsWith(Const.COMMENT_SIGN + columnName)) {
                continue;
            }

            return line.split(Const.SEPARATOR)[column];
        }
        return "";
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

        String valueTypeWithSize = getValueTypeAsString(columnNumber, dataFile);
        String[] valueTypeWithSizeArray = valueTypeWithSize.split(Const.VALUETYPE_ENDSIGN);
        String valueType = valueTypeWithSizeArray[0].split(Const.VALUETYPE_SIZE_SEPARATOR)[0];
        return ValueType.valueOf(valueType);
    }

    public static int getValueTypeLengthFromFile(int columnNumber, File dataFile) {

        String valueType = getValueTypeAsString(columnNumber, dataFile);
        return getByteStringLength(valueType);
    }

    private static String getValueTypeAsString(int columnNumber, File dataFile) {

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(dataFile), Const.CHAR_SET));) {

            int column = LoggerUtils.getCommentColumnNumberByName(Const.COMMENT_NAME, br);

            if (column == -1) {
                String msg = MessageFormat.format("No element with name \"{0}\" found.", Const.COMMENT_NAME);
                throw new NoSuchElementException(msg);
            }

            return LoggerUtils.getCommentValue(columnNumber, column, br).split(Const.VALUETYPE_ENDSIGN)[0];

        } catch (IOException e) {
            logger.error("Failed to get Value type as string.", e);
        }
        return "";
    }

    /**
     * Returns the predefined size of a ValueType.
     *
     * @param valueType
     *            the type to get the predefined size
     * @return predefined size of a ValueType as int.
     */
    public static int getLengthOfValueType(ValueType valueType) {

        switch (valueType) {
        case DOUBLE:
            return Const.VALUE_SIZE_DOUBLE;
        case FLOAT:
            return Const.VALUE_SIZE_DOUBLE;
        case INTEGER:
            return Const.VALUE_SIZE_INTEGER;
        case LONG:
            return Const.VALUE_SIZE_LONG;
        case SHORT:
            return Const.VALUE_SIZE_SHORT;
        case BYTE_ARRAY:
            return Const.VALUE_SIZE_MINIMAL;
        case STRING:
            return Const.VALUE_SIZE_MINIMAL;
        case BOOLEAN:
        case BYTE:
        default:
            return Const.VALUE_SIZE_MINIMAL;
        }
    }

    /**
     * Converts a byte array to an hexadecimal string
     *
     * @param sb
     *            to add hex string
     * @param byteArray
     *            the byte array to convert
     */
    public static void byteArrayToHexString(StringBuilder sb, byte[] byteArray) {
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        sb.append(hexChars);
    }

    /**
     * Constructs the timestamp for every log value into a StringBuilder.
     *
     * @param sb
     *            the StringBuilder to add the logger timestamp
     * @param calendar
     *            Calendar with the wished time
     */
    public static void setLoggerTimestamps(StringBuilder sb, Calendar calendar) {

        double unixtimestampSeconds = calendar.getTimeInMillis() / 1000.0; // double for milliseconds, nanoseconds

        sb.append(String.format(Const.DATE_FORMAT, calendar));
        sb.append(Const.SEPARATOR);
        sb.append(String.format(Const.TIME_FORMAT, calendar));
        sb.append(Const.SEPARATOR);
        sb.append(String.format(Locale.ENGLISH, "%10.3f", unixtimestampSeconds));
        sb.append(Const.SEPARATOR);
    }

    /**
     * Constructs the timestamp for every log value into a StringBuilder.
     *
     * @param sb
     *            the StringBuilder to add the logger timestamp
     * @param unixTimeStamp
     *            unix time stamp in ms
     */
    public static void setLoggerTimestamps(StringBuilder sb, long unixTimeStamp) {

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTimeInMillis(unixTimeStamp);
        double unixtimestampSeconds = unixTimeStamp / 1000.0; // double for milliseconds, nanoseconds

        sb.append(String.format(Const.DATE_FORMAT, calendar));
        sb.append(Const.SEPARATOR);
        sb.append(String.format(Const.TIME_FORMAT, calendar));
        sb.append(Const.SEPARATOR);
        sb.append(String.format(Locale.ENGLISH, "%10.3f", unixtimestampSeconds));
        sb.append(Const.SEPARATOR);
    }

    public static String getHeaderFromFile(String filePath) {

        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), Const.CHAR_SET));
        } catch (IOException e1) {
            return "";
        }

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
                logger.error("Cannot close file: " + filePath, e);
            }
        }
        return sb.toString();
    }

    /**
     * Returns a RandomAccessFile of the specified file.
     *
     * @param file
     *            file get the RandomAccessFile
     * @param accesMode
     *            access mode
     * @return the RandomAccessFile of the specified file, {@code null} if an error occured.
     */
    public static RandomAccessFile getRandomAccessFile(File file, String accesMode) {
        try {
            return new RandomAccessFile(file, accesMode);
        } catch (FileNotFoundException e) {
            logger.warn("Requested logfile: '{}' not found.", file.getAbsolutePath());
        }

        return null;
    }

    public static PrintWriter getPrintWriter(File file, boolean append) throws IOException {

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append), Const.CHAR_SET));
        } catch (IOException e) {
            logger.error("Cannot open file: " + file.getAbsolutePath());
            throw new IOException(e);
        }
        return writer;
    }

    public static Map<String, Boolean> areHeadersIdentical(String loggerDirectory, List<LogChannel> channels,
            Calendar calendar) {

        Map<String, Boolean> areHeadersIdentical = new TreeMap<>();
        Map<String, List<LogChannel>> logChannelMap = new TreeMap<>();

        String key = "";

        for (LogChannel logChannel : channels) {

            if (logChannel.getLoggingTimeOffset() != 0) {
                key = logChannel.getLoggingInterval() + Const.TIME_SEPERATOR_STRING + logChannel.getLoggingTimeOffset();
            }
            else {
                key = logChannel.getLoggingInterval().toString();
            }

            if (!logChannelMap.containsKey(key)) {
                List<LogChannel> logChannelList = new ArrayList<>();
                logChannelList.add(logChannel);
                logChannelMap.put(key, logChannelList);
            }
            else {
                logChannelMap.get(key).add(logChannel);
            }
        }

        List<LogChannel> logChannels;

        for (Entry<String, List<LogChannel>> entry : logChannelMap.entrySet()) {

            key = entry.getKey();
            logChannels = entry.getValue();
            String fileName = LoggerUtils.buildFilename(key, calendar);

            String headerGenerated = LogFileHeader.getIESDataFormatHeaderString(fileName, logChannels);
            String oldHeader = LoggerUtils.getHeaderFromFile(loggerDirectory + fileName) + Const.LINESEPARATOR;
            boolean isHeaderIdentical = headerGenerated.equals(oldHeader);
            areHeadersIdentical.put(key, isHeaderIdentical);
        }

        return areHeadersIdentical;
    }

    /**
     * * fills a AsciiLogg file up.
     *
     * @param out
     *            the output stream to write on
     * @param unixTimeStamp
     *            unix time stamp
     * @param loggingInterval
     *            logging interval
     * @param numberOfFillUpLines
     *            the number to fill up lines
     * @param errorValues
     *            the error value set in the line
     * @return returns the unix time stamp of the last filled up line
     */
    public static long fillUp(PrintWriter out, long unixTimeStamp, long loggingInterval, long numberOfFillUpLines,
            StringBuilder errorValues) {

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < numberOfFillUpLines; ++i) {

            line.setLength(0);
            unixTimeStamp += loggingInterval;
            setLoggerTimestamps(line, unixTimeStamp);
            line.append(errorValues);
            line.append(Const.LINESEPARATOR);

            out.append(line);
        }

        return unixTimeStamp;
    }

    public static long getNumberOfFillUpLines(long lastUnixTimeStamp, long loggingInterval) {

        long numberOfFillUpLines = 0;
        long currentUnixTimeStamp = System.currentTimeMillis();

        numberOfFillUpLines = (currentUnixTimeStamp - lastUnixTimeStamp) / loggingInterval;

        return numberOfFillUpLines;
    }

    /**
     * Returns the error value as a StringBuilder.
     *
     * @param lineArray
     *            a ascii line as a array with error code
     * @return StringBuilder with appended error
     */
    public static StringBuilder getErrorValues(String[] lineArray) {

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

    /**
     * Get the length from a type+length tuple. Example: "Byte_String,95"
     *
     * @param string
     *            has to be a string with ByteType and length.
     * @param dataFile
     *            the logger data file
     * @return the length of a ByteString.
     */
    private static int getByteStringLength(String string) {

        String[] stringArray = string.split(Const.VALUETYPE_SIZE_SEPARATOR);
        try {
            return Integer.parseInt(stringArray[1]);
        } catch (NumberFormatException e) {
            logger.warn("Not able to get ValueType length from String. Set length to minimal lenght "
                    + Const.VALUE_SIZE_MINIMAL + ".");
        }
        return Const.VALUE_SIZE_MINIMAL;
    }

}
