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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFileReader {

    private static final Logger logger = LoggerFactory.getLogger(LogFileReader.class);

    private final String channelId;
    private final String path;
    private final int loggingInterval;
    private final int logTimeOffset;
    private int unixTimestampColumn;
    private long startTimestamp;
    private long endTimestamp;
    private long firstTimestampFromFile;

    /**
     * LogFileReader Constructor
     * 
     * @param path
     *            the path to the files to read from
     * @param logChannel
     *            the channel to read from
     */
    public LogFileReader(String path, LogChannel logChannel) {

        this.path = path;
        channelId = logChannel.getId();
        this.loggingInterval = logChannel.getLoggingInterval();
        this.logTimeOffset = logChannel.getLoggingTimeOffset();
        firstTimestampFromFile = -1;
    }

    /**
     * Get the values between start time stamp and end time stamp
     * 
     * @param startTimestamp
     *            start time stamp
     * @param endTimestamp
     *            end time stamp
     * @return All records of the given time span
     */
    public List<Record> getValues(long startTimestamp, long endTimestamp) {

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;

        List<Record> allRecords = new ArrayList<>();

        List<String> filenames = LoggerUtils.getFilenames(loggingInterval, logTimeOffset, this.startTimestamp,
                this.endTimestamp);

        for (int i = 0; i < filenames.size(); i++) {
            Boolean nextFile = false;
            if (logger.isTraceEnabled()) {
                logger.trace("using " + filenames.get(i));
            }

            String filepath;
            if (path.endsWith(File.separator)) {
                filepath = path + filenames.get(i);
            }
            else {
                filepath = path + File.separatorChar + filenames.get(i);
            }

            if (i > 0) {
                nextFile = true;
            }
            List<Record> fileRecords = processFile(filepath, nextFile);
            if (fileRecords != null) {
                allRecords.addAll(fileRecords);
                if (logger.isTraceEnabled()) {
                    logger.trace("read records: " + fileRecords.size());
                }
            }
            else {
                // some error occurred while processing the file so no records will be added
            }

        }
        return allRecords;
    }

    /**
     * get a single record of time stamp
     *
     * @param timestamp
     *            time stamp
     * @return Record on success, otherwise null
     */
    public Record getValue(long timestamp) {

        // Returns a record which lays within the interval [timestamp, timestamp + loggingInterval]
        // The interval is necessary for a requested time stamp which lays between the time stamps of two logged values
        // e.g.: t_request = 7, t1_logged = 5, t2_logged = 10, loggingInterval = 5
        // method will return the record of t2_logged because this lays within the interval [7,12]
        // If the t_request matches exactly a logged time stamp, then the according record is returned.

        Record record = null;
        List<Record> records = getValues(timestamp, timestamp);// + loggingInterval);
        if (records.size() == 0) {
            // no record found for requested timestamp

            // TODO statt null flag 3 setzen!
            record = null;
        }
        else if (records.size() == 1) {
            // t_request lays between two logged values
            record = records.get(0);
        }
        else if (records.size() == 2) {
            // t_request matches exactly a logged value
            // so getVaules returns a record for t_request and one for t_request+loggingInterval
            record = records.get(0);
        }

        // nur wert zurückgeben wenn zeitstempel identisch ist
        // sonst

        return record;
    }

    /**
     * Reads the file line by line
     * 
     * @param filepath
     *            file path
     * @param nextFile
     *            if it is the next file and not the first between a time span
     * @return records on success, otherwise null
     */
    private List<Record> processFile(String filepath, Boolean nextFile) {

        List<Record> records = new ArrayList<>();
        String line = null;
        long currentPosition = 0;
        long rowSize;
        long firstTimestamp = 0;
        String firstValueLine = null;
        long currentTimestamp = 0;

        RandomAccessFile raf = LoggerUtils.getRandomAccessFile(new File(filepath), "r");
        if (raf == null) {
            return null;
        }
        try {
            int channelColumn = -1;
            while (channelColumn <= 0) {
                line = raf.readLine();
                channelColumn = LoggerUtils.getColumnNumberByName(line, channelId);
                unixTimestampColumn = LoggerUtils.getColumnNumberByName(line, Const.TIMESTAMP_STRING);
            }

            firstValueLine = raf.readLine();

            rowSize = firstValueLine.length() + 1; // +1 because of "\n"

            // rewind the position to the start of the firstValue line
            currentPosition = raf.getFilePointer() - rowSize;

            firstTimestamp = (long) (Double.valueOf((firstValueLine.split(Const.SEPARATOR))[unixTimestampColumn])
                    * 1000);

            if (nextFile || startTimestamp < firstTimestamp) {
                startTimestamp = firstTimestamp;
            }

            if (startTimestamp >= firstTimestamp) {
                long filepos = getFilePosition(loggingInterval, startTimestamp, firstTimestamp, currentPosition,
                        rowSize);
                raf.seek(filepos);

                currentTimestamp = startTimestamp;

                while ((line = raf.readLine()) != null && currentTimestamp <= endTimestamp) {

                    processLine(line, channelColumn, records);
                    currentTimestamp += loggingInterval;
                }
                raf.close();
            }
            else {
                records = null; // because the column of the channel was not identified
            }
        } catch (IOException e) {
            e.printStackTrace();
            records = null;
        }
        return records;
    }

    /**
     * Process the line: ignore comments, read records
     * 
     * @param line
     *            the line to process
     * @param channelColumn
     *            channel column
     * @param records
     *            list of records
     */
    private void processLine(String line, int channelColumn, List<Record> records) {

        if (!line.startsWith(Const.COMMENT_SIGN)) {
            readRecordFromLine(line, channelColumn, records);
        }
    }

    /**
     * Reads records from a line.
     * 
     * @param line
     *            to read
     * @param column
     *            of the channelId
     * @return Record read from line
     */
    private void readRecordFromLine(String line, int channelColumn, List<Record> records) {

        String columnValue[] = line.split(Const.SEPARATOR);

        try {
            Double timestampS = Double.parseDouble(columnValue[unixTimestampColumn]);

            long timestampMS = ((Double) (timestampS * (1000))).longValue();

            if (isTimestampPartOfRequestedInterval(timestampMS)) {
                Record record = convertLogfileEntryToRecord(columnValue[channelColumn].trim(), timestampMS);
                records.add(record);
            }
            else {
                // for debugging
                // SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                // logger.trace("timestampMS: " + sdf.format(timestampMS) + " " + timestampMS);
            }
        } catch (NumberFormatException e) {
            logger.debug("It's not a timestamp.");
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the time stamp read from file is part of the requested logging interval
     * 
     * @param lineTimestamp
     *            time stamp to check if it is part of the time span
     * @return true if it is a part of the requested interval, if not false.
     */
    private boolean isTimestampPartOfRequestedInterval(long lineTimestamp) {
        boolean result = false;

        // TODO tidy up, move to better place, is asked each line!
        if (firstTimestampFromFile == -1) {
            firstTimestampFromFile = lineTimestamp;
        }

        if (lineTimestamp >= startTimestamp && lineTimestamp <= endTimestamp) {
            result = true;
        }
        return result;
    }

    /**
     * Get the position of the startTimestamp, without Header.
     * 
     * @param loggingInterval
     *            logging interval
     * @param startTimestamp
     *            start time stamp
     * @return the position of the start timestamp as long.
     */
    private long getFilePosition(int loggingInterval, long startTimestamp, long firstTimestampOfFile,
            long firstValuePos, long rowSize) {

        long timeOffsetMs = startTimestamp - firstTimestampOfFile;
        long numberOfLinesToSkip = timeOffsetMs / loggingInterval;

        // if offset isn't a multiple of loggingInterval add an additional line
        if (timeOffsetMs % loggingInterval != 0) {
            ++numberOfLinesToSkip;
        }

        long pos = numberOfLinesToSkip * rowSize + firstValuePos;

        // for debugging
        // logger.trace("pos " + pos);
        // logger.trace("startTimestamp " + startTimestamp);
        // logger.trace("firstTimestamp " + firstTimestampOfFile);
        // logger.trace("loggingInterval " + loggingInterval);
        // logger.trace("rowSize " + rowSize);
        // logger.trace("firstValuePos " + firstValuePos);

        return pos;
    }

    // TODO support ints, booleans, ...
    /**
     * Converts an entry from the logging file into a record
     * 
     * @param strValue
     *            string value
     * @param timestamp
     *            time stamp
     * @return the converted logfile entry.
     */
    private Record convertLogfileEntryToRecord(String strValue, long timestamp) {

        Record record = null;
        if (isNumber(strValue)) {
            record = new Record(new DoubleValue(Double.parseDouble(strValue)), timestamp, Flag.VALID);
        }
        else {
            // fehlerfall, wenn errors "errxx" geloggt wurden
            // record = new Record(null, timestamp, Flag);
            record = getRecordFromNonNumberValue(strValue, timestamp);

        }
        return record;
    }

    /**
     * Returns the record from a non number value read from the logfile. This is the case if the value is an error like
     * "e0" or a normal ByteArrayValue
     * 
     * @param strValue
     *            string value
     * @param timestamp
     *            time stamp
     * @return the value in a record.
     */
    private Record getRecordFromNonNumberValue(String strValue, long timestamp) {

        Record record = null;

        if (strValue.trim().startsWith(Const.ERROR)) {

            int errorSize = Const.ERROR.length();
            int stringLength = strValue.length();
            String errorFlag = strValue.substring(errorSize, errorSize + stringLength - errorSize);
            errorFlag = errorFlag.trim();

            if (isNumber(errorFlag)) {
                record = new Record(null, timestamp, Flag.newFlag(Integer.parseInt(errorFlag)));
            }
            else {
                record = new Record(null, timestamp, Flag.NO_VALUE_RECEIVED_YET);
            }
        }
        else if (strValue.trim().startsWith(Const.HEXADECIMAL)) {
            try {
                record = new Record(new ByteArrayValue(strValue.trim().getBytes(Const.CHAR_SET)), timestamp,
                        Flag.VALID);
            } catch (UnsupportedEncodingException e) {
                record = new Record(Flag.UNKNOWN_ERROR);
                logger.error("Hexadecimal value is non US-ASCII decoded, value is: " + strValue.trim());
            }
        }
        else {
            record = new Record(new StringValue(strValue.trim()), timestamp, Flag.VALID);
        }
        return record;
    }

    /**
     * Checks if the string value is a number
     * 
     * @param strValue
     *            string value
     * @return True on success, otherwise false
     */
    private boolean isNumber(String strValue) {

        boolean isDecimalSeparatorFound = false;

        if (!Character.isDigit(strValue.charAt(0)) && strValue.charAt(0) != Const.MINUS_SIGN
                && strValue.charAt(0) != Const.PLUS_SIGN) {
            return false;
        }

        for (char charactor : strValue.substring(1).toCharArray()) {
            if (!Character.isDigit(charactor)) {
                if (charactor == Const.DECIMAL_SEPARATOR && !isDecimalSeparatorFound) {
                    isDecimalSeparatorFound = true;
                    continue;
                }
                return false;
            }
        }
        return true;
    }
}
