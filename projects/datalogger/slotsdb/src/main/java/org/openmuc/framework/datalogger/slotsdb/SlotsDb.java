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

package org.openmuc.framework.datalogger.slotsdb;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.TypeConversionException;
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
public final class SlotsDb implements DataLoggerService {

    /*
     * File extension for SlotsDB files. Only these Files will be loaded.
     */
    public static final String FILE_EXTENSION = ".slots";
    /*
     * Root folder for SlotsDB files
     */
    public static final String DB_ROOT_FOLDER = System
            .getProperty(SlotsDb.class.getPackage().getName().toLowerCase() + ".dbfolder");
    /*
     * If no other root folder is defined, data will be stored to this folder
     */
    public static final String DEFAULT_DB_ROOT_FOLDER = "data/slotsdb/";
    /*
     * Root Folder for JUnit Testcases
     */
    public static final String DB_TEST_ROOT_FOLDER = "testdata/";
    /*
     * limit open files in Hashmap
     *
     * Default Linux Configuration: (should be below)
     *
     * host:/#> ulimit -aH [...] open files (-n) 1024 [...]
     */
    public static final String MAX_OPEN_FOLDERS = System
            .getProperty(SlotsDb.class.getPackage().getName().toLowerCase() + ".max_open_folders");
    public static final int MAX_OPEN_FOLDERS_DEFAULT = 512;
    /*
     * configures the data flush period. The less you flush, the faster SLOTSDB will be. unset this System Property (or
     * set to 0) to flush data directly to disk.
     */
    public static final String FLUSH_PERIOD = System
            .getProperty(SlotsDb.class.getPackage().getName().toLowerCase() + ".flushperiod");
    /*
     * configures how long data will at least be stored in the SLOTSDB.
     */
    public static final String DATA_LIFETIME_IN_DAYS = System
            .getProperty(SlotsDb.class.getPackage().getName().toLowerCase() + ".limit_days");
    /*
     * configures the maximum Database Size (in MB).
     */
    public static final String MAX_DATABASE_SIZE = System
            .getProperty(SlotsDb.class.getPackage().getName().toLowerCase() + ".limit_size");
    /*
     * Minimum Size for SLOTSDB (in MB).
     */
    public static final int MINIMUM_DATABASE_SIZE = 2;
    /*
     * Initial delay for scheduled tasks (size watcher, data expiration, etc.)
     */
    public static final int INITIAL_DELAY = 10000;
    /*
     * Interval for scanning expired, old data. Set this to 86400000 to scan every 24 hours.
     */
    public static final int DATA_EXPIRATION_CHECK_INTERVAL = 5000;
    private static final Logger logger = LoggerFactory.getLogger(SlotsDb.class);
    private final HashMap<String, Integer> loggingIntervalsById = new HashMap<>();
    private FileObjectProxy fileObjectProxy;;

    @Activate
    protected void activate(ComponentContext context) {
        String rootFolder = SlotsDb.DB_ROOT_FOLDER;
        if (rootFolder == null) {
            rootFolder = SlotsDb.DEFAULT_DB_ROOT_FOLDER;
        }

        fileObjectProxy = new FileObjectProxy(rootFolder);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // TODO
    }

    @Override
    public String getId() {
        return "slotsdb";
    }

    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {
        return fileObjectProxy.read(channelId, startTime, endTime);
    }

    @Override
    public void setChannelsToLog(List<LogChannel> channels) {
        loggingIntervalsById.clear();
        for (LogChannel channel : channels) {
            loggingIntervalsById.put(channel.getId(), channel.getLoggingInterval());
        }
    }

    @Override
    public void log(List<LoggingRecord> containers, long timestamp) {
        for (LoggingRecord container : containers) {
            Double value;
            if (container.getRecord().getValue() == null) {
                value = Double.NaN;
            }
            else {
                try {
                    value = container.getRecord().getValue().asDouble();
                } catch (TypeConversionException e) {
                    value = Double.NaN;
                }
            }

            // Long timestamp = container.getRecord().getTimestamp();
            // if (timestamp == null) {
            // timestamp = 0L;
            // }

            try {
                String channelId = container.getChannelId();
                fileObjectProxy.appendValue(channelId, value, timestamp, container.getRecord().getFlag().getCode(),
                        loggingIntervalsById.get(channelId));
            } catch (IOException e) {
                logger.error("error logging records", e);
            }
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
