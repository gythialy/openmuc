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
package org.openmuc.framework.datalogger.ascii;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.ascii.utils.Const;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AsciiLogger implements DataLoggerService {

    private static final Logger logger = LoggerFactory.getLogger(AsciiLogger.class);
    private static final String DIRECTORY = System
            .getProperty(AsciiLogger.class.getPackage().getName().toLowerCase() + ".directory");
    private static HashMap<String, Long> lastLoggedLineList = new HashMap<>();
    private final String loggerDirectory;
    private final HashMap<String, LogChannel> logChannelList = new HashMap<>();
    private boolean isFillUpFiles = true;

    public AsciiLogger() {

        if (DIRECTORY == null) {
            loggerDirectory = Const.DEFAULT_DIRECTORY;
        }
        else {
            loggerDirectory = DIRECTORY.trim();
        }
        createDirectory(loggerDirectory);
    }

    public AsciiLogger(String loggerDirectory) {

        this.loggerDirectory = loggerDirectory;
        createDirectory(loggerDirectory);
    }

    public static Long getLastLoggedLineTimeStamp(int loggingInterval, int loggingOffset) {

        return lastLoggedLineList.get(loggingInterval + Const.TIME_SEPERATOR_STRING + loggingOffset);
    }

    public static void setLastLoggedLineTimeStamp(String loggerInterval_loggerTimeOffset, long lastTimestamp) {

        lastLoggedLineList.put(loggerInterval_loggerTimeOffset, lastTimestamp);
    }

    public static void setLastLoggedLineTimeStamp(int loggingInterval, int loggingOffset, long lastTimestamp) {

        lastLoggedLineList.put(loggingInterval + Const.TIME_SEPERATOR_STRING + loggingOffset, lastTimestamp);
    }

    public static long fillUpFileWithErrorCode(String directoryPath, String loggerInterval_loggerTimeOffset,
            Calendar calendar) {

        String filename = LoggerUtils.buildFilename(loggerInterval_loggerTimeOffset, calendar);
        File file = new File(directoryPath + filename);
        RandomAccessFile raf = LoggerUtils.getRandomAccessFile(file, "r");
        PrintWriter out = null;

        String firstLogLine = "";
        String lastLogLine = "";
        long loggingInterval = 0;

        if (loggerInterval_loggerTimeOffset.contains(Const.TIME_SEPERATOR_STRING)) {
            loggingInterval = Long.parseLong(loggerInterval_loggerTimeOffset.split(Const.TIME_SEPERATOR_STRING)[0]);
        }
        else {
            loggingInterval = Long.parseLong(loggerInterval_loggerTimeOffset);
        }

        long lastLogLineTimeStamp = 0;

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
                byte[] readedByte = new byte[1];
                long filePosition = file.length() - 2;
                String charString;
                while (lastLogLine.isEmpty() && filePosition > 0) {

                    raf.seek(filePosition);
                    int readedBytes = raf.read(readedByte);
                    if (readedBytes == 1) {
                        charString = new String(readedByte, Const.CHAR_SET);

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
                    LoggerUtils.renameFileToOld(directoryPath, loggerInterval_loggerTimeOffset, calendar);
                    logger.error("File is coruppted, could not fill up, renamed it. " + file.getAbsolutePath());
                    return 0l;
                }
                else {

                    String lastLogLineArray[] = lastLogLine.split(Const.SEPARATOR);

                    StringBuilder errorValues = LoggerUtils.getErrorValues(lastLogLineArray);
                    lastLogLineTimeStamp = (long) (Double.parseDouble(lastLogLineArray[2]) * 1000.);

                    out = LoggerUtils.getPrintWriter(file, true);

                    long numberOfFillUpLines = LoggerUtils.getNumberOfFillUpLines(lastLogLineTimeStamp,
                            loggingInterval);

                    while (numberOfFillUpLines > 0) {

                        lastLogLineTimeStamp = LoggerUtils.fillUp(out, lastLogLineTimeStamp, loggingInterval,
                                numberOfFillUpLines, errorValues);
                        numberOfFillUpLines = LoggerUtils.getNumberOfFillUpLines(lastLogLineTimeStamp, loggingInterval);
                    }
                    out.close();
                    AsciiLogger.setLastLoggedLineTimeStamp(loggerInterval_loggerTimeOffset, lastLogLineTimeStamp);
                }
            } catch (IOException e) {
                logger.error("Could not read file " + file.getAbsolutePath(), e);
                LoggerUtils.renameFileToOld(directoryPath, loggerInterval_loggerTimeOffset, calendar);
            } finally {
                try {
                    raf.close();
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    logger.error("Could not close file " + file.getAbsolutePath());
                }
            }
        }
        return lastLogLineTimeStamp;
    }

    @Activate
    protected void activate(ComponentContext context) {

        logger.info("Activating Ascii Logger");
        setSystemProperties();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        logger.info("Deactivating Ascii Logger");
    }

    private void createDirectory(String loggerDirectory) {

        logger.trace("using directory: {}", loggerDirectory);
        File asciidata = new File(loggerDirectory);
        if (!asciidata.exists() && !asciidata.mkdirs()) {
            logger.error("Could not create logger directory: " + asciidata.getAbsolutePath());
            // TODO: weitere Behandlung,
        }
    }

    @Override
    public String getId() {
        return "asciilogger";
    }

    /**
     * Will called if OpenMUC starts the logger
     */
    @Override
    public void setChannelsToLog(List<LogChannel> logChannels) {

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        logChannelList.clear();

        logger.trace("channels to log:");
        for (LogChannel logChannel : logChannels) {

            if (logger.isTraceEnabled()) {
                logger.trace("channel.getId() " + logChannel.getId());
                logger.trace("channel.getLoggingInterval() " + logChannel.getLoggingInterval());
            }
            logChannelList.put(logChannel.getId(), logChannel);
        }

        if (isFillUpFiles) {
            Map<String, Boolean> areHeaderIdentical = LoggerUtils.areHeadersIdentical(loggerDirectory, logChannels,
                    calendar);

            for (Entry<String, Boolean> entry : areHeaderIdentical.entrySet()) {
                String key = entry.getKey();
                boolean isHeaderIdentical = entry.getValue();

                if (isHeaderIdentical) {
                    // Fill file up with error flag 32 (DATA_LOGGING_NOT_ACTIVE)
                    if (logger.isTraceEnabled()) {
                        logger.trace(
                                "Fill file " + LoggerUtils.buildFilename(key, calendar) + " up with error flag 32.");
                    }
                    fillUpFileWithErrorCode(loggerDirectory, key, calendar);
                }
                else {
                    // rename file in old file (if file is existing), because of configuration has changed
                    LoggerUtils.renameFileToOld(loggerDirectory, key, calendar);
                }
            }
        }
        else {
            LoggerUtils.renameAllFilesToOld(loggerDirectory, calendar);
        }

    }

    @Override
    public synchronized void log(List<LoggingRecord> loggingRecords, long timestamp) {
        HashMap<List<Integer>, LogIntervalContainerGroup> logIntervalGroups = new HashMap<>();

        // add each container to a group with the same logging interval
        for (LoggingRecord container : loggingRecords) {

            int logInterval = -1;
            int logTimeOffset = 0;
            List<Integer> logTimeArray = Arrays.asList(logInterval, logTimeOffset);
            String channelId = container.getChannelId();

            if (logChannelList.containsKey(channelId)) {
                logInterval = logChannelList.get(channelId).getLoggingInterval();
                logTimeOffset = logChannelList.get(channelId).getLoggingTimeOffset();
                logTimeArray = Arrays.asList(logInterval, logTimeOffset);
            }
            else {
                // TODO there might be a change in the channel config file
            }

            if (logIntervalGroups.containsKey(logTimeArray)) {
                // add the container to an existing group
                LogIntervalContainerGroup group = logIntervalGroups.get(logTimeArray);
                group.add(container);
            }
            else {
                // create a new group and add the container
                LogIntervalContainerGroup group = new LogIntervalContainerGroup();
                group.add(container);
                logIntervalGroups.put(logTimeArray, group);
            }

        }

        // alle gruppen loggen
        Iterator<Entry<List<Integer>, LogIntervalContainerGroup>> it = logIntervalGroups.entrySet().iterator();
        List<Integer> logTimeArray;

        Calendar calendar = new GregorianCalendar(Locale.getDefault());

        while (it.hasNext()) {

            logTimeArray = it.next().getKey();
            LogIntervalContainerGroup group = logIntervalGroups.get(logTimeArray);
            LogFileWriter fileOutHandler = new LogFileWriter(loggerDirectory, isFillUpFiles);

            calendar.setTimeInMillis(timestamp);

            fileOutHandler.log(group, logTimeArray.get(0), logTimeArray.get(1), calendar, logChannelList);
            setLastLoggedLineTimeStamp(logTimeArray.get(0), logTimeArray.get(1), calendar.getTimeInMillis());
        }
    }

    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {

        LogChannel logChannel = logChannelList.get(channelId);
        LogFileReader reader = null;

        if (logChannel != null) {
            reader = new LogFileReader(loggerDirectory, logChannel);
            return reader.getValues(startTime, endTime).get(channelId);
        } // TODO: hier einfuegen das nach Loggdateien gesucht werden sollen die vorhanden sind aber nicht geloggt
          // werden,
          // z.B fuer server only ohne Logging. Das suchen sollte nur beim ersten mal passieren (start).
        else {
            throw new IOException("ChannelID (" + channelId + ") not available. It's not a logging Channel.");
        }
    }

    private void setSystemProperties() {

        // FIXME: better to use a constant here instead of dynamic name in case the package name changes in future than
        // the system.properties entry will be out dated.
        String fillUpPropertyStr = AsciiLogger.class.getPackage().getName().toLowerCase() + ".fillUpFiles";
        String fillUpProperty = System.getProperty(fillUpPropertyStr);

        if (fillUpProperty != null) {
            isFillUpFiles = Boolean.parseBoolean(fillUpProperty);
            logger.debug("Property: {} is set to {}", fillUpPropertyStr, isFillUpFiles);
        }
        else {
            logger.debug("Property: {} not found in system.properties. Using default value: true", fillUpPropertyStr);
            isFillUpFiles = true;
        }
    }

    @Override
    public void logEvent(List<LoggingRecord> containers, long timestamp) {
        logger.warn("Event logging is not implemented, yet.");
    }

    @Override
    public boolean logSettingsRequired() {
        return false;
    }
}
