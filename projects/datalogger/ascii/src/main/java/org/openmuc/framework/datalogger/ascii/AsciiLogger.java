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
package org.openmuc.framework.datalogger.ascii;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class AsciiLogger implements DataLoggerService {

    private final static Logger logger = LoggerFactory.getLogger(AsciiLogger.class);

    public final static String DEFAULT_DIR = System.getProperty("user.dir") + "/asciidata/";
    public final static String EXTENSION = ".dat";
    public final static String EXTENSION_OLD = ".old";

    private final String loggerDirectory;
    private final HashMap<String, LogChannel> logChannelList = new HashMap<String, LogChannel>();

    protected void activate(ComponentContext context) {
        logger.info("Activating Ascii Logger");
    }

    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating Ascii Logger");
    }

    /**
     *
     */
    public AsciiLogger() {
        loggerDirectory = AsciiLogger.DEFAULT_DIR;
        createDirectory();
    }

    public AsciiLogger(String loggerDirectory) {
        this.loggerDirectory = loggerDirectory;
        createDirectory();
    }

    private void createDirectory() {
        logger.debug("using directory: " + loggerDirectory);
        File asciidata = new File(loggerDirectory);
        if (!asciidata.exists()) {
            if (!asciidata.mkdir()) {
                logger.error("Could not create logger directory: " + asciidata.getAbsolutePath());
                // TODO: weitere Behandlung,
            }
        }
    }

    @Override
    public String getId() {
        return "asciilogger";
    }

    public boolean text(String test) {
        return true;
    }

    @Override
    public void setChannelsToLog(List<LogChannel> channels) {

        logChannelList.clear();

        logger.debug("channels to log:");
        for (LogChannel channel : channels) {

            logger.debug("channel.getId() " + channel.getId());
            logger.debug("channel.getLoggingInterval() " + channel.getLoggingInterval());
            logChannelList.put(channel.getId(), channel);
        }

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        LoggerUtils loggerUtils = new LoggerUtils();
        loggerUtils.renameOldFiles(loggerDirectory, calendar);

    }// @Override
    // public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {

    // int loggingInterval = this.logChannelList.get(id).getLoggingInterval();
    // LogFileReader reader = new LogFileReader(loggerDirectory, id, loggingInterval);
    // return reader.getValues(startTime, endTime);
    // }
    //
    // @Override
    // public List<Record> getValues(String id, long startTimestamp) throws IOException {
    // return getValues(id, startTimestamp, System.currentTimeMillis());
    // }
    //
    // @Override
    // public List<Record> getValues(String id, long startTimestamp, long endTimestamp) throws IOException {
    // int loggingInterval = this.logChannelList.get(id).getLoggingInterval();
    // LogFileReader reader = new LogFileReader(loggerDirectory, id, loggingInterval);
    // return reader.getValues(startTimestamp, endTimestamp);
    // }
    //
    // @Override
    // public Record getValue(String id, long timestamp) throws IOException {
    // int loggingInterval = this.logChannelList.get(id).getLoggingInterval();
    // LogFileReader reader = new LogFileReader(loggerDirectory, id, loggingInterval);
    // reader.getValue(timestamp);
    // return null;
    // }

    @Override
    public void log(List<LogRecordContainer> containers, long timestamp) {

        HashMap<Integer, LogIntervalContainerGroup> logIntervalGroups = new HashMap<Integer, LogIntervalContainerGroup>();

        // add each container to a group with the same logging interval
        for (LogRecordContainer container : containers) {

            int logInterval = -1;

            if (logChannelList.containsKey(container.getChannelId())) {
                logInterval = logChannelList.get(container.getChannelId()).getLoggingInterval();
            } else {
                // TODO there might be a change in the channel config file
            }

            if (logIntervalGroups.containsKey(logInterval)) {
                // add the container to an existing group
                LogIntervalContainerGroup group = logIntervalGroups.get(logInterval);
                group.add(container);
            } else {
                // create a new group and add the container
                LogIntervalContainerGroup group = new LogIntervalContainerGroup();
                group.add(container);
                logIntervalGroups.put(logInterval, group);
            }

        }

        // alle gruppen loggen
        Iterator<Entry<Integer, LogIntervalContainerGroup>> it = logIntervalGroups.entrySet().iterator();
        Integer logInterval;
        while (it.hasNext()) {
            logInterval = it.next().getKey();
            LogIntervalContainerGroup group = logIntervalGroups.get(logInterval);
            LogFileWriter fileOutHandler = new LogFileWriter();
            fileOutHandler.log(group, logInterval, new Date(timestamp), logChannelList);
        }
    }

    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {

        LogChannel logChannel = logChannelList.get(channelId);
        LogFileReader reader = null;
        if (logChannel != null) {
            int loggingInterval = logChannel.getLoggingInterval();
            reader = new LogFileReader(loggerDirectory, channelId, loggingInterval);
            return reader.getValues(startTime, endTime);
        } else {
            throw new IOException("ChannelID (" + channelId + ") not available.");
        }
    }

}
