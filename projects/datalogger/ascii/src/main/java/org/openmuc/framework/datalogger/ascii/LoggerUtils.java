/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.framework.datalogger.ascii;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class LoggerUtils {

    public static final String HEADER_SIGN = "##";
    public static final String COMMENT_SIGN = "#";
    public static final String TIMESTAMP_STRING = "unixtimestamp";
    public static final short INVALID_INDEX = -1;
    public static final String DATE_FORMAT = "%1$tY%1$tm%1$td"; // Date in YYYYMMDD
    public static final String TIME_FORMAT = "%1$tH%1$tM%1$tS"; // Time in HHMMSS

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);

    /**
     * Returns all filenames of the given timespan defined by the two dates
     *
     * @param loggingInterval
     * @param startTimestamp
     * @param endTimestamp
     * @return a list of files which within the timespan
     */
    public static List<String> getFilenames(int loggingInterval,
                                            long startTimestamp,
                                            long endTimestamp) {

        Calendar calendarStart = new GregorianCalendar(Locale.getDefault());
        calendarStart.setTimeInMillis(startTimestamp);
        Calendar calendarEnd = new GregorianCalendar(Locale.getDefault());
        calendarEnd.setTimeInMillis(endTimestamp);

        // Rename timespanToFilenames....
        // Filename YYYYMMDD_<LoggingIntervall>.dat
        List<String> filenames = new ArrayList<String>();
        while (calendarStart.before(calendarEnd) || calendarStart.equals(calendarEnd)) {
            String filename = buildFilename(loggingInterval, calendarStart);
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
    public static String getFilename(int loggingInterval, long timestamp) {
        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTimeInMillis(timestamp);
        return buildFilename(loggingInterval, calendar);
    }

    /**
     * Builds the Logfile name from logging interval and the date of the calendar
     *
     * @param loggingInterval
     * @param calendar
     * @return logfile name
     */
    public static String buildFilename(int loggingInterval, Calendar calendar) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(DATE_FORMAT, calendar));
        sb.append("_");
        sb.append(String.valueOf(loggingInterval));
        sb.append(AsciiLogger.EXTENSION);
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
     * This method rename a *.dat file into a *.old0, *.old1, ...
     *
     * @param directoryPath
     * @param calendar
     */
    public void renameOldFiles(String directoryPath, Calendar calendar) {

        String date = String.format(DATE_FORMAT, calendar);

        File dir = new File(directoryPath);
        File[] files = dir.listFiles();

        for (File file : files) {
            String currentName = file.getName();
            if (currentName.startsWith(date) && currentName.endsWith(AsciiLogger.EXTENSION)) {

                String newName = currentName.substring(0,
                                                       currentName.length()
                                                       - AsciiLogger.EXTENSION.length());
                newName += AsciiLogger.EXTENSION_OLD;
                int j = 0;

                File fileWithNewName = new File(directoryPath + newName + j);

                while (fileWithNewName.exists()) {
                    ++j;
                    fileWithNewName = new File(directoryPath + newName + j);
                }
                file.renameTo(fileWithNewName);
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
        calendarZero.set(today.get(Calendar.YEAR),
                         today.get(Calendar.MONTH),
                         today.get(Calendar.DATE),
                         0,
                         0,
                         0);
        calendarZero.set(Calendar.MILLISECOND, 0);

        return calendarZero;
    }

    /**
     * This method adds a string value up with blank spaces from left to right.
     *
     * @param value String value to fill up
     * @param size  maximal allowed size
     * @return the modified string.
     */
    public static String addSpacesLeft(String value, int size) {
        while (value.length() < size) {
            value = " " + value;
        }
        return value;
    }

    /**
     * This method adds a string value up with blank spaces from right to left.
     *
     * @param value String value to fill up
     * @param size  maximal allowed size
     * @return the modified string.
     */
    public static String addSpacesRight(String value, int size) {
        while (value.length() < size) {
            value = value + " ";
        }
        return value;
    }

    /**
     * Construct a error value with the flag.
     *
     * @param flag
     * @return a string with the flag and the standard error prefix.
     */
    public static String buildError(Flag flag) {
        return IESDataFormatUtils.ERROR + flag.getCode();
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
        if (!line.startsWith(IESDataFormatUtils.COMMENT)) {
            String columns[] = line.split(IESDataFormatUtils.SEPARATOR);
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
     * @param name the name to search
     * @param br   the BufferedReader
     * @return column number as int, -1 if name not found
     * @throws IOException
     */
    public static int getCommentColumnNumberByName(String name, BufferedReader br)
            throws IOException {

        String line = br.readLine();
        while (line.startsWith(IESDataFormatUtils.COMMENT)) {
            if (line.contains(name)) {
                String columns[] = line.split(IESDataFormatUtils.SEPARATOR);
                for (int i = 0; i < columns.length; i++) {
                    if (name.equals(columns[i])) {
                        return i;
                    }
                }
            }
            line = br.readLine();
        }
        return -1;
    }

    /**
     * @param col_no the number of the channel
     * @param column the column
     * @param br     a BufferedReader
     * @return the value of a column of a specific col_num
     * @throws IOException
     */
    public static String getCommentValue(int col_no, int column, BufferedReader br)
            throws IOException {

        String line = br.readLine();
        String columnName = String.format("%03d", col_no);

        while (line.startsWith(IESDataFormatUtils.COMMENT)) {
            if (line.startsWith(IESDataFormatUtils.COMMENT + columnName)) {
                String columns[] = line.split(IESDataFormatUtils.SEPARATOR);
                return columns[column];
            }
            line = br.readLine();
        }
        return null;
    }

    /**
     * Convert a String with a value type into object ValueType. This Strings are allowed: DOUBLE, FLOAT, INTEGER, LONG,
     * SHORT, BOOLEAN, BYTE, BYTE_ARRAY and STRING.
     *
     * @param string
     * @return the matching ValueType
     */
    public static ValueType stringToValueType(String string) {

        String stringArray[] = string.split(IESDataFormatUtils.VALUETYPE_SIZE_SEPARATOR);

        if ("DOUBLE".equals(stringArray[0])) {
            return ValueType.DOUBLE;
        } else if ("FLOAT".equals(stringArray[0])) {
            return ValueType.FLOAT;
        } else if ("INTEGER".equals(stringArray[0])) {
            return ValueType.INTEGER;
        } else if ("LONG".equals(stringArray[0])) {
            return ValueType.LONG;
        } else if ("SHORT".equals(stringArray[0])) {
            return ValueType.SHORT;
        } else if ("BOOLEAN".equals(stringArray[0])) {
            return ValueType.BOOLEAN;
        } else if ("BYTE".equals(stringArray[0])) {
            return ValueType.BYTE;
        } else if ("BYTE_ARRAY".equals(stringArray[0])) {
            return ValueType.BYTE_ARRAY;
        } else if ("STRING".equals(stringArray[0])) {
            return ValueType.STRING;
        } else {
            return null;
        }
    }

    /**
     * Identifies the ValueType of a logger value on a specific col_no
     *
     * @param columnNumber column number
     * @param dataFile     the logger data file
     * @return the ValueType from col_num x
     */
    public static ValueType identifyValueType(int columnNumber, File dataFile) {

        String valueType = getValueTypeAsString(columnNumber, dataFile);
        String valueTypeArray[] = valueType.split(IESDataFormatUtils.VALUETYPE_ENDSIGN);

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
            br = new BufferedReader(new FileReader(dataFile));
            int column = LoggerUtils.getCommentColumnNumberByName(IESDataFormatUtils.COMMENT_NAME,
                                                                  br);

            if (column != -1) {
                value = LoggerUtils.getCommentValue(columnNumber, column, br);
                value = value.split(IESDataFormatUtils.VALUETYPE_ENDSIGN)[0];
            } else {
                throw new NoSuchElementException("No element with name \""
                                                 + IESDataFormatUtils.COMMENT_NAME
                                                 + "\" found.");
            }
            br.close();
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchElementException e) {
            e.printStackTrace();
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
            size = IESDataFormatUtils.VALUE_SIZE_DOUBLE;
            break;
        case FLOAT:
            size = IESDataFormatUtils.VALUE_SIZE_DOUBLE;
            break;
        case INTEGER:
            size = IESDataFormatUtils.VALUE_SIZE_INTEGER;
            break;
        case LONG:
            size = IESDataFormatUtils.VALUE_SIZE_LONG;
            break;
        case SHORT:
            size = IESDataFormatUtils.VALUE_SIZE_SHORT;
            break;
        case BYTE_ARRAY:
            size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
            break;
        case STRING:
            size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
            break;
        case BOOLEAN:
        case BYTE:
        default:
            size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
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
    public static String ByteArrayToHexString(byte[] byteArray) {
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
     * Get the length from a type+length tuple. Example: "Byte_String,95"
     *
     * @param string   has to be a string with ByteType and length.
     * @param dataFile the logger data file
     * @return the length of a ByteString in int.
     */
    private static int getByteStringLength(String string) {
        String stringArray[] = {""};
        int size;

        stringArray = string.split(IESDataFormatUtils.VALUETYPE_SIZE_SEPARATOR);
        try {
            size = Integer.valueOf(stringArray[1]);
        }
        catch (NumberFormatException e) {
            logger.warn(
                    "Not able to get ValueType length from String. Set length to minimal lenght "
                    + IESDataFormatUtils.VALUE_SIZE_MINIMAL + ".");
            size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
        }
        return size;
    }

}
