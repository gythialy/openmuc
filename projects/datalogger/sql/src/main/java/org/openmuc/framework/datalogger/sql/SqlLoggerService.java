/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.datalogger.sql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlLoggerService implements DataLoggerService, ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(SqlLoggerService.class);
    private final Settings settings;
    private final PropertyHandler propertyHandler;
    private final List<LoggingRecord> eventBuffer;
    private SqlWriter writer;
    private SqlReader reader;
    private DbAccess dbAccess;
    private List<LogChannel> channels;

    /**
     * Starts the h2 server if conditions are met and connects to the database.
     */
    public SqlLoggerService() {
        logger.info("Activating SQL Logger");
        settings = new Settings();
        eventBuffer = new ArrayList<>();
        String pid = SqlLoggerService.class.getName();
        propertyHandler = new PropertyHandler(settings, pid);
        PropertyHandlerProvider.getInstance().setPropertyHandler(propertyHandler);
    }

    private void connect() {
        dbAccess = new DbAccess();
        writer = new SqlWriter(dbAccess);
        reader = new SqlReader(dbAccess);
        writeMetaToDb();
        writer.writeEventBasedContainerToDb(eventBuffer);
        eventBuffer.clear();
    }

    private void writeMetaToDb() {
        MetaBuilder metaBuilder = new MetaBuilder(channels, dbAccess);
        metaBuilder.writeMetaTable();

        TableSetup tableSetup = new TableSetup(channels, dbAccess);
        tableSetup.createOpenmucTables();
    }

    /**
     * Closes the connection, stops the timer by calling its cancel Method and stops the h2 server, if the conditions
     * for each are met, if a connection exists
     */
    public void shutdown() {
        logger.info("Deactivating SQL Logger");
        if (dbAccess != null) {
            dbAccess.closeConnection();
        }
    }

    @Override
    public String getId() {
        return "sqllogger";
    }

    /**
     * Creates the metadata table to create the tables for each data type and to insert info about all the channel into
     * the metadata table
     */
    @Override
    public void setChannelsToLog(List<LogChannel> channels) {
        this.channels = channels;
        if (dbAccess != null) {
            TableSetup tableSetup = new TableSetup(channels, dbAccess);
            tableSetup.createOpenmucTables();
        }
    }

    @Override
    public void log(List<LoggingRecord> containers, long timestamp) {
        if (writer == null) {
            logger.warn("Sql connection not established!");
            return;
        }

        writer.writeRecordContainerToDb(containers, timestamp);
    }

    @Override
    public void logEvent(List<LoggingRecord> containers, long timestamp) {
        if (writer == null) {
            logger.debug("Sql connection not established!");
            eventBuffer.addAll(containers);
            return;
        }

        writer.writeEventBasedContainerToDb(containers);
    }

    @Override
    public boolean logSettingsRequired() {
        return false;
    }

    /**
     * @return the queried data
     */
    @Override
    public List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException {
        List<Record> records = new ArrayList<>();
        for (LogChannel temp : this.channels) {
            if (temp.getId().equals(channelId)) {
                records = reader.readRecordListFromDb(channelId, temp.getValueType(), startTime, endTime);
                break;
            }
        }
        return records;
    }

    /**
     * Returns the Record with the highest timestamp available in all logged data for the channel with the given
     * <code>channelId</code>. If there are multiple Records with the same timestamp, results will not be consistent.
     * 
     * @param channelId
     *            the channel ID.
     * @return the Record with the highest timestamp available in all logged data for the channel with the given
     *         <code>channelId</code>. Null if no Record was found.
     * @throws IOException
     */
    @Override
    public Record getLatestLogRecord(String channelId) throws IOException {
        Record record = null;
        for (LogChannel temp : this.channels) {
            if (temp.getId().equals(channelId)) {
                record = reader.readLatestRecordFromDb(channelId, temp.getValueType());
                break;
            }
        }
        return record;
    }

    @Override
    public void updated(Dictionary<String, ?> propertyDict) {
        DictionaryPreprocessor dict = new DictionaryPreprocessor(propertyDict);
        if (!dict.wasIntermediateOsgiInitCall()) {
            tryProcessConfig(dict);
        }
    }

    private void tryProcessConfig(DictionaryPreprocessor newConfig) {
        try {
            propertyHandler.processConfig(newConfig);
            if (propertyHandler.configChanged()) {
                applyConfigChanges();
            }
            else if (propertyHandler.isDefaultConfig() && writer == null) {
                connect();
            }
        } catch (ServicePropertyException e) {
            logger.error("update properties failed", e);
            shutdown();
        }
    }

    private void applyConfigChanges() {
        logger.info("Configuration changed - new configuration {}", propertyHandler.toString());
        if (writer != null) {
            shutdown();
        }
        connect();
    }
}
