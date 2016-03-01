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
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsciiLogger implements DataLoggerService {

	private final static Logger logger = LoggerFactory.getLogger(AsciiLogger.class);

	private final String loggerDirectory;
	private final HashMap<String, LogChannel> logChannelList = new HashMap<String, LogChannel>();
	private static HashMap<String, Long> lastLoggedLineList = new HashMap<String, Long>();
	private boolean isFillUpFiles = false;

	protected void activate(ComponentContext context) {

		logger.info("Activating Ascii Logger");
		setSystemProperties();
	}

	protected void deactivate(ComponentContext context) {

		logger.info("Deactivating Ascii Logger");
	}

	/**
	 * 
	 */
	public AsciiLogger() {

		loggerDirectory = Const.DEFAULT_DIR;
		createDirectory();
	}

	public AsciiLogger(String loggerDirectory) {

		this.loggerDirectory = loggerDirectory;
		createDirectory();
	}

	private void createDirectory() {

		logger.trace("using directory: " + loggerDirectory);
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

	/**
	 * Will called if OpenMUC starts the logger
	 */
	@Override
	public void setChannelsToLog(List<LogChannel> channels) {

		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		logChannelList.clear();

		logger.trace("channels to log:");
		for (LogChannel channel : channels) {

			if (logger.isTraceEnabled()) {
				logger.trace("channel.getId() " + channel.getId());
				logger.trace("channel.getLoggingInterval() " + channel.getLoggingInterval());
			}
			logChannelList.put(channel.getId(), channel);
		}

		if (isFillUpFiles) {
			Map<String, Boolean> areHeaderIdentical = LoggerUtils.areHeadersIdentical(loggerDirectory, channels,
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
					LoggerUtils.fillUpFileWithErrorCode(loggerDirectory, key, calendar);
				}
				else {
					// rename file in old file, because of configuration has changed
					if (logger.isTraceEnabled()) {
						logger.trace("Header not identical. Rename file " + LoggerUtils.buildFilename(key, calendar)
								+ " to old.");
					}
					LoggerUtils.renameFileToOld(loggerDirectory, key, calendar);
				}
			}
		}
		else {
			LoggerUtils.renameAllFilesToOld(loggerDirectory, calendar);
		}

	}

	@Override
	public synchronized void log(List<LogRecordContainer> containers, long timestamp) {

		HashMap<List<Integer>, LogIntervalContainerGroup> logIntervalGroups = new HashMap<List<Integer>, LogIntervalContainerGroup>();

		// add each container to a group with the same logging interval
		for (LogRecordContainer container : containers) {

			int logInterval = -1;
			int logTimeOffset = 0;
			List<Integer> logTimeArray = Arrays.asList(logInterval, logTimeOffset);

			if (logChannelList.containsKey(container.getChannelId())) {
				logInterval = logChannelList.get(container.getChannelId()).getLoggingInterval();
				logTimeOffset = logChannelList.get(container.getChannelId()).getLoggingTimeOffset();
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
			LogFileWriter fileOutHandler = new LogFileWriter(isFillUpFiles);

			calendar.setTimeInMillis(timestamp);

			fileOutHandler.log(group, logTimeArray.get(0), logTimeArray.get(1), calendar, logChannelList);
		}
	}

	@Override
	public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {

		LogChannel logChannel = logChannelList.get(channelId);
		LogFileReader reader = null;

		if (logChannel != null) {
			reader = new LogFileReader(loggerDirectory, logChannel);
			return reader.getValues(startTime, endTime);
		} // TODO: hier einfügen das nach Loggdateien gesucht werden sollen die vorhanden sind aber nicht geloggt
			// werden,
			// z.B für server only ohne Logging. Das suchen sollte nur beim ersten mal passieren (start).
		else {
			throw new IOException("ChannelID (" + channelId + ") not available. It's not a logging Channel.");
		}
	}

	private void setSystemProperties() {

		String fillUpProperty = System.getProperty("org.openmuc.framework.datalogger.ascii.fillUpFiles");

		if (fillUpProperty != null) {
			isFillUpFiles = Boolean.parseBoolean(fillUpProperty);
		}
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
}
