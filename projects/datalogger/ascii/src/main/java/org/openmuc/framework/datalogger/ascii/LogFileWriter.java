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
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class LogFileWriter {

    private final String characterEncoding = "UTF-8";
    private final String directoryPath;
    private static final Logger logger = LoggerFactory.getLogger(LogFileWriter.class);
    private final LogFileHeader header = new LogFileHeader();
    private File actualFile;

    public LogFileWriter() {
        directoryPath = AsciiLogger.DEFAULT_DIR;
    }

    public LogFileWriter(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    /**
     * Main logger writing controller.
     *
     * @param group
     * @param loggingInterval
     * @param date
     * @param logChannelList
     */
    public void log(LogIntervalContainerGroup group, int loggingInterval, Date date,
                    HashMap<String, LogChannel> logChannelList) {

        PrintStream out = getStream(group, loggingInterval, date, logChannelList);

        if (out == null) {
            return;
        }
        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTime(date);

        StringBuilder sb = new StringBuilder();
        List<LogRecordContainer> containers = group.getList();

        writeTimestamps(sb, calendar);
        // TODO match column with container id, so that they don't get mixed up

        for (int i = 0; i < containers.size(); i++) {
            String value = new String();
            int size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
            boolean left = true;

            if (containers.get(i).getRecord() != null) {
                if (containers.get(i).getRecord().getFlag() == Flag.VALID) {
                    if (containers.get(i).getRecord().getValue() == null) {
                        // write error flag
                        value = LoggerUtils.buildError(Flag.CANNOT_WRITE_NULL_VALUE);
                        size = getDataTypeSize(logChannelList.get(containers.get(i).getChannelId()),
                                               i);
                    } else {
                        ValueType valueType = logChannelList.get(containers.get(i).getChannelId())
                                                            .getValueType();
                        // logger.debug("channel: " + containers.get(i).getChannelId());
                        switch (valueType) {
                        case BOOLEAN:
                            value = String.valueOf(containers.get(i)
                                                             .getRecord()
                                                             .getValue()
                                                             .asShort());
                            break;
                        case LONG:
                            value = String.valueOf(containers.get(i)
                                                             .getRecord()
                                                             .getValue()
                                                             .asLong());
                            size = IESDataFormatUtils.VALUE_SIZE_LONG;
                            break;
                        case INTEGER:
                            value = String.valueOf(containers.get(i)
                                                             .getRecord()
                                                             .getValue()
                                                             .asInt());
                            size = IESDataFormatUtils.VALUE_SIZE_INTEGER;
                            break;
                        case SHORT:
                            value = String.valueOf(containers.get(i)
                                                             .getRecord()
                                                             .getValue()
                                                             .asShort());
                            size = IESDataFormatUtils.VALUE_SIZE_SHORT;
                            break;
                        case DOUBLE:
                        case FLOAT:
                            size = IESDataFormatUtils.VALUE_SIZE_DOUBLE;
                            try {
                                value = IESDataFormatUtils.convertDoubleToStringWithMaxLength(
                                        containers.get(i)
                                                  .getRecord().getValue().asDouble(),
                                        size);
                            }
                            catch (WrongScalingException e) {
                                value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
                                logger.error(e.getMessage() + " ChannelId: " + containers.get(i)
                                                                                         .getChannelId());
                            }
                            break;
                        case BYTE_ARRAY:
                            left = false;
                            size = checkMinimalValueSize(logChannelList.get(containers.get(i)
                                                                                      .getChannelId())
                                                                       .getValueTypeLength());
                            byte[] byteArray = containers.get(i)
                                                         .getRecord()
                                                         .getValue()
                                                         .asByteArray();
                            if (byteArray.length > size) {
                                value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
                                logger.error("The byte array is too big, max Size = "
                                             + size
                                             + ", ChannelId: "
                                             + containers.get(i).getChannelId());
                            } else {
                                value = IESDataFormatUtils.HEXADECIMAL
                                        + LoggerUtils.ByteArrayToHexString(byteArray);
                            }
                            break;
                        case STRING:
                            left = false;
                            size = checkMinimalValueSize(logChannelList.get(containers.get(i)
                                                                                      .getChannelId())
                                                                       .getValueTypeLength());
                            value = containers.get(i).getRecord().getValue().toString();
                            checkStringValue(value);
                            if (value.length() > size) {
                                value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
                                logger.error("The string is too big, max Size = "
                                             + size
                                             + ", ChannelId: "
                                             + containers.get(i).getChannelId());
                            }
                            break;
                        case BYTE:
                            value = String.format("0x%02x",
                                                  containers.get(i)
                                                            .getRecord()
                                                            .getValue()
                                                            .asByte());
                            break;
                        default:
                            throw new RuntimeException("unsupported valueType");
                        }
                    }
                } else {
                    // write errorflag
                    value = LoggerUtils.buildError(containers.get(i).getRecord().getFlag());
                    size = getDataTypeSize(logChannelList.get(containers.get(i).getChannelId()), i);
                }
            } else {
                // got no data
                value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
                size = getDataTypeSize(logChannelList.get(containers.get(i).getChannelId()), i);
            }

            if (left) {
                value = LoggerUtils.addSpacesLeft(value, size);
            } else {
                value = LoggerUtils.addSpacesRight(value, size);
            }
            sb.append(value);

            if (LoggerUtils.hasNext(containers, i)) {
                sb.append(IESDataFormatUtils.SEPARATOR);
            }
        }
        out.println(sb.toString());
        out.flush();
        out.close();
    }

    /**
     * Checkes a string if it is IESData conform, f.e. wrong characters. If not it will drop a error.
     *
     * @param value the string value which should be checked
     */
    private void checkStringValue(String value) {
        try {
            if (value.contains(IESDataFormatUtils.SEPARATOR)) {
                throw new WrongCharacterException(
                        "Wrong character: String contains Seperator character: "
                        + IESDataFormatUtils.SEPARATOR);
            } else if (value.startsWith(IESDataFormatUtils.ERROR)) {
                throw new WrongCharacterException("Wrong character: String begins with: "
                                                  + IESDataFormatUtils.ERROR);
            } else if (value.startsWith(IESDataFormatUtils.HEXADECIMAL)) {
                throw new WrongCharacterException("Wrong character: String begins with: "
                                                  + IESDataFormatUtils.HEXADECIMAL);
            } else if (!value.matches("^[\\x00-\\x7F]*")) {
                throw new WrongCharacterException("Wrong character: Non ASCII character in String.");
            }
        }
        catch (WrongCharacterException e) {
            value = LoggerUtils.buildError(Flag.UNKNOWN_ERROR);
            logger.error(e.getMessage());
        }
    }

    private int checkMinimalValueSize(int size) {
        if (size < IESDataFormatUtils.VALUE_SIZE_MINIMAL) {
            size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;
        }
        return size;
    }

    /**
     * Returns the PrintStream for logging.
     *
     * @param group
     * @param loggingInterval
     * @param date
     * @param logChannelList
     * @return the PrintStream for logging.
     */
    private PrintStream getStream(LogIntervalContainerGroup group, int loggingInterval, Date date,
                                  HashMap<String, LogChannel> logChannelList) {

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTime(date);
        String filename = LoggerUtils.buildFilename(loggingInterval, calendar);

        File file = new File(directoryPath + filename);
        actualFile = file;
        PrintStream out = null;

        // prüfe ob datei existiert
        if (file.exists()) {
            // open existing file
            try {
                out = new PrintStream(new FileOutputStream(file, true), false, characterEncoding);
            }
            catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                file.createNewFile();
                out = new PrintStream(new FileOutputStream(file, true), false, characterEncoding);
                header.writeIESDataFormatHeader(group,
                                                out,
                                                file.getName(),
                                                loggingInterval,
                                                logChannelList);
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return out;
    }

    /**
     * Constructs the timestamp for every log value into a StringBBuilder.
     *
     * @param sb
     * @param calendar
     */
    private void writeTimestamps(StringBuilder sb, Calendar calendar) {

        double unixtimestamp_sec = calendar.getTimeInMillis()
                                   / 1000.0; // double for milliseconds, nanoseconds

        sb.append(String.format(LoggerUtils.DATE_FORMAT, calendar));
        sb.append(IESDataFormatUtils.SEPARATOR);
        sb.append(String.format(LoggerUtils.TIME_FORMAT, calendar));
        sb.append(IESDataFormatUtils.SEPARATOR);
        sb.append(String.format(Locale.ENGLISH, "%10.3f", unixtimestamp_sec));
        sb.append(IESDataFormatUtils.SEPARATOR);
    }

    /**
     * Returns the size of a DataType / ValueType.
     *
     * @param logChannel
     * @param iterator
     * @return size of DataType / ValueType.
     */
    private int getDataTypeSize(LogChannel logChannel, int iterator) {
        int size = IESDataFormatUtils.VALUE_SIZE_MINIMAL;

        if ((logChannel != null) && (logChannel.getValueType().equals(ValueType.BYTE_ARRAY))) {
            // get length from channel for ByteString
            size = logChannel.getValueTypeLength();
        } else if (logChannel != null && !(logChannel.getValueType()
                                                     .equals(ValueType.BYTE_ARRAY))) {
            // get length from channel for simple value types
            size = LoggerUtils.getLengthOfValueType(logChannel.getValueType());
        } else {
            // get length from file
            ValueType vt = LoggerUtils.identifyValueType(iterator
                                                         + IESDataFormatUtils.NUM_OF_TIME_TYPES_IN_HEADER
                                                         + 1,
                                                         actualFile);
            size = LoggerUtils.getLengthOfValueType(vt);
            if (vt.equals(ValueType.BYTE_ARRAY) && size <= IESDataFormatUtils.VALUE_SIZE_MINIMAL) {
                size = LoggerUtils.getValueTypeLengthFromFile(iterator
                                                              + IESDataFormatUtils.NUM_OF_TIME_TYPES_IN_HEADER
                                                              + 1, actualFile);
            }
        }
        return size;
    }

}
