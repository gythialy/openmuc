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
package org.openmuc.framework.datalogger.ascii;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;

public class LogFileHeader {

    private LogFileHeader() {
    }

    /**
     * Generate the standard IES Data Format Header.
     * 
     * @param group
     *            a group of the LogIntervallContainer
     * @param filename
     *            the name of the file to add the header
     * @param loggingInterval
     *            logging interval in ms
     * @param logChannelList
     *            a list of all channels for this file
     * @return the header as a string
     */
    public static String getIESDataFormatHeaderString(LogIntervalContainerGroup group, String filename,
            int loggingInterval, HashMap<String, LogChannel> logChannelList) {

        StringBuilder sb = new StringBuilder();
        setHeaderTop(sb, loggingInterval, filename);

        // write channel specific header informations
        int colNumber = 4;
        for (LogRecordContainer container : group.getList()) {

            LogChannel logChannel = logChannelList.get(container.getChannelId());
            appendChannelSpecificComment(sb, logChannel, colNumber);
            ++colNumber;
        }
        List<LogRecordContainer> containers = group.getList();
        appendColumnHeaderTimestamp(sb);

        Iterator<LogRecordContainer> iterator = containers.iterator();

        while (iterator.hasNext()) {
            sb.append(iterator.next().getChannelId());
            if (iterator.hasNext()) {
                sb.append(Const.SEPARATOR);
            }
        }

        sb.append(Const.LINESEPARATOR);
        return sb.toString();
    }

    /**
     * Generate the standard IES Data Format Header
     * 
     * @param filename
     *            the name of the file to add the header
     * @param logChannelList
     *            a list of all channels for this file
     * @return the header as a string
     */
    public static String getIESDataFormatHeaderString(String filename, List<LogChannel> logChannelList) {

        StringBuilder sb0 = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        setHeaderTop(sb0, logChannelList.get(0).getLoggingInterval(), filename);

        // write channel specific header informations
        int colNumber = 4;
        Iterator<LogChannel> iterator = logChannelList.listIterator();
        while (iterator.hasNext()) {

            LogChannel logChannel = iterator.next();
            appendChannelSpecificComment(sb0, logChannel, colNumber);

            sb1.append(logChannel.getId());
            if (iterator.hasNext()) {
                sb1.append(Const.SEPARATOR);
            }
            ++colNumber;
        }
        appendColumnHeaderTimestamp(sb0);
        sb0.append(sb1);
        sb0.append(Const.LINESEPARATOR);
        return sb0.toString();
    }

    /**
     * Appends channel specific comments to a StringBuilder
     * 
     * @param sb
     * @param logChannel
     * @param colNumber
     */
    private static void appendChannelSpecificComment(StringBuilder sb, LogChannel logChannel, int colNumber) {

        String unit = logChannel.getUnit();
        if (unit.equals("")) {
            unit = "0";
        }
        ValueType vType = logChannel.getValueType();
        String valueType = vType.toString();
        int valueTypeLength = 0;
        if (vType.equals(ValueType.BYTE_ARRAY) || vType.equals(ValueType.STRING)) {
            valueTypeLength = logChannel.getValueTypeLength();
        }

        String description = logChannel.getDescription();
        if (description.equals("")) {
            description = "-";
        }

        createRow(sb, String.format("%03d", colNumber), logChannel.getId(), "FALSE", "TRUE", unit, "other", valueType,
                valueTypeLength, description);
    }

    /**
     * Append column headers, the timestamps, in a StringBuilder
     * 
     * @param sb
     * @param group
     */
    private static void appendColumnHeaderTimestamp(StringBuilder sb) {

        // write column headers
        sb.append("YYYYMMDD");
        sb.append(Const.SEPARATOR);
        sb.append("hhmmss");
        sb.append(Const.SEPARATOR);
        sb.append("unixtimestamp");
        sb.append(Const.SEPARATOR);
    }

    /**
     * Sets the top of the header.
     * 
     * @param sb
     * @param loggingInterval
     * @param filename
     */
    private static void setHeaderTop(StringBuilder sb, int loggingInterval, String filename) {

        String timestep_sec = String.valueOf(loggingInterval / (double) 1000);
        String seperator = Const.SEPARATOR;

        // write general header informations
        appendStrings(sb, "#ies_format_version: ", String.valueOf(Const.ISEFORMATVERSION), Const.LINESEPARATOR_STRING);
        appendStrings(sb, "#file: ", filename, Const.LINESEPARATOR_STRING);
        appendStrings(sb, "#file_info: ", Const.FILEINFO, Const.LINESEPARATOR_STRING);
        appendStrings(sb, "#timezone: ", getDiffLocalUTC(), Const.LINESEPARATOR_STRING);
        appendStrings(sb, "#timestep_sec: ", timestep_sec, Const.LINESEPARATOR_STRING);
        appendStrings(sb, "#", "col_no", seperator, "col_name", seperator, "confidential", seperator, "measured",
                seperator, "unit", seperator, "category", seperator, Const.COMMENT_NAME, Const.LINESEPARATOR_STRING);
        createRow(sb, "001", "YYYYMMDD", "FALSE", "FALSE", "0", "time", "INTEGER", 8, "Date [human readable]");
        createRow(sb, "002", "hhmmss", "FALSE", "FALSE", "0", "time", "SHORT", 6, "Time [human readable]");
        createRow(sb, "003", "unixtimestamp", "FALSE", "FALSE", "s", "time", "DOUBLE", 14,
                "lapsed seconds from 01-01-1970");
    }

    /**
     * Construct a header row with predefined separators and comment signs.
     * 
     * @param col_no
     *            column number example: #001
     * @param col_name
     *            column name example: YYYYMMDD
     * @param confidential
     *            false or true
     * @param measured
     *            false or true
     * @param unit
     *            example: kWh
     * @param category
     *            example: time
     * @param valueType
     *            example: DOUBLE
     * @param valueTypeLength
     *            example: 8
     * @param comment
     *            a comment
     */
    private static void createRow(StringBuilder sb, String col_no, String col_name, String confidential,
            String measured, String unit, String category, String valueType, int valueTypeLength, String comment) {

        String seperator = Const.SEPARATOR;
        String com_sign = Const.COMMENT_SIGN;
        String vtEndSign = Const.VALUETYPE_ENDSIGN;
        String vtSizeSep = Const.VALUETYPE_SIZE_SEPARATOR;
        String valueTypeLengthString = "";
        if (valueTypeLength != 0) {
            valueTypeLengthString += valueTypeLength;
        }
        appendStrings(sb, com_sign, col_no, seperator, col_name, seperator, confidential, seperator, measured,
                seperator, unit, seperator, category, seperator, valueType, vtSizeSep, valueTypeLengthString, vtEndSign,
                comment, Const.LINESEPARATOR_STRING);
    }

    /**
     * appendStrings appends a any String to a StringBuilder
     * 
     * @param sb
     *            StringBuilder to append a String
     * @param s
     *            the String to append
     */
    private static void appendStrings(StringBuilder sb, String... s) {

        for (String element : s) {
            sb.append(element);
        }
    }

    /**
     * Calculates the difference between the configured local time and the Coordinated Universal Time (UTC) without
     * daylight saving time and returns it as a string.
     * 
     * @return the difference between local time and UTC as string.
     */
    private static String getDiffLocalUTC() {

        String ret;
        long time = 0;

        Calendar calendar = new GregorianCalendar(Locale.getDefault());

        time = calendar.getTimeZone().getRawOffset();
        time /= 1000 * 60 * 60;

        if (time >= 0) {
            ret = ("+ " + time);
        }
        else {
            ret = ("- " + time);
        }

        return ret;
    }

}
