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

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.COLUMNS;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.MYSQL;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.NULL;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRES;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRESQL;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaBuilder {

    private final Logger logger = LoggerFactory.getLogger(MetaBuilder.class);
    private final List<LogChannel> channels;
    private final DbAccess dbAccess;
    private StringBuilder resultComparison;
    private StringBuilder sbMetaInsert;
    private final String url;

    public MetaBuilder(List<LogChannel> channels, DbAccess dbAccess) {
        this.channels = channels;
        this.dbAccess = dbAccess;
        PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
        url = propertyHandler.getString(Settings.URL);
    }

    public void writeMetaTable() {
        StringBuilder metaStructure = createMetaStructure();
        writeMetaStructure(metaStructure);
        StringBuilder metaInserts = createInsertsForMetaTable();

        if (!metaInserts.toString().isEmpty()) {
            dbAccess.executeSQL(new StringBuilder("TRUNCATE TABLE openmuc_meta ;"));
            dbAccess.executeSQL(metaInserts);
        }
    }

    private void writeMetaStructure(StringBuilder metaString) {
        dbAccess.executeSQL(metaString);

        if (url.contains(POSTGRESQL) && !dbAccess.timeScaleIsActive()) {
            StringBuilder sbIndex = new StringBuilder("CREATE INDEX IF NOT EXISTS metaIndex ON openmuc_meta (time);");
            dbAccess.executeSQL(sbIndex);
        }

        if (url.contains(POSTGRESQL) && dbAccess.timeScaleIsActive()) {
            try {
                dbAccess.executeQuery(
                        new StringBuilder("SELECT create_hypertable('openmuc_meta', 'time', if_not_exists => TRUE);"));
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private StringBuilder createMetaStructure() {
        StringBuilder sbMeta = new StringBuilder();
        if (url.contains(POSTGRES)) {
            sbMeta.append("CREATE TABLE IF NOT EXISTS openmuc_meta (time TIMESTAMPTZ NOT NULL,\n");
        }
        else {
            sbMeta.append("CREATE TABLE IF NOT EXISTS openmuc_meta (time TIMESTAMP NOT NULL,\n");
        }

        int channelIdLength = 30;
        int channelAdressLength = 30;
        int unitLength = 15;
        int samplingGroupLength = 30;
        int descripionLength = 30;

        for (LogChannel channel : channels) {
            channelIdLength = updateLengthIfHigher(channel.getId(), channelIdLength);
            channelAdressLength = updateLengthIfHigher(channel.getChannelAddress(), channelAdressLength);
            samplingGroupLength = updateLengthIfHigher(channel.getSamplingGroup(), samplingGroupLength);
            unitLength = updateLengthIfHigher(channel.getUnit(), unitLength);
            descripionLength = updateLengthIfHigher(channel.getDescription(), descripionLength);
        }

        // sbMeta.append("driverID VARCHAR(30) NULL,")
        // .append("deviceID VARCHAR(30) NULL,")
        sbMeta.append("channelID VARCHAR(" + channelIdLength + ") NOT NULL,")
                .append("channelAdress VARCHAR(" + channelAdressLength + NULL)
                .append("loggingInterval VARCHAR(10) NULL,")
                .append("loggingTimeOffset VARCHAR(10) NULL,")
                .append("unit VARCHAR(" + unitLength + NULL)
                .append("valueType VARCHAR(20) NULL,")
                // .append("valueTypeLength VARCHAR(5) NULL,")
                .append("scalingFactor VARCHAR(5) NULL,")
                .append("valueOffset VARCHAR(5) NULL,")
                .append("listening VARCHAR(5) NULL,")
                .append("loggingEvent VARCHAR(5) NULL,")
                .append("samplingInterval VARCHAR(10) NULL,")
                .append("samplingTimeOffset VARCHAR(10) NULL,")
                .append("samplingGroup VARCHAR(" + samplingGroupLength + NULL)
                .append("disabled VARCHAR(5) NULL,")
                .append("description VARCHAR(" + descripionLength + ") NULL");

        if (!url.contains(POSTGRESQL)) {
            sbMeta.append(",INDEX metaIndex(time),PRIMARY KEY (channelid, time));");
        }
        else {
            sbMeta.append(",PRIMARY KEY (channelid, time));");
        }

        return sbMeta;
    }

    /**
     * checks if the attributes length exceeds the standard value
     *
     * @param stringValue
     *            Attribute of the channel
     * @param currentLength
     *            Current or standard column length
     * @return column length to be used
     */
    private int updateLengthIfHigher(String stringValue, int currentLength) {
        if (stringValue != null) {
            int length = stringValue.length();
            if (length > currentLength) {
                currentLength = length;
            }
        }
        return currentLength;
    }

    /**
     * Inserts the needed data into the table openmuc_meta when there are either no prior entries in it or if the
     * metadata has changed since the last entry
     */
    private StringBuilder createInsertsForMetaTable() {
        if (channels.isEmpty()) {
            logger.warn("There are no channels for meta table");
        }

        resultComparison = new StringBuilder();

        sbMetaInsert = new StringBuilder(
                "INSERT INTO openmuc_meta (time,channelid,channelAdress,loggingInterval,loggingTimeOffset,unit,valueType,scalingFactor,valueOffset,listening,loggingEvent,samplingInterval,samplingTimeOffset,SamplingGroup,disabled,description) ");

        StringBuilder sbMetaInsertValues = new StringBuilder("VALUES (");

        for (LogChannel logChannel : channels) {
            sbMetaInsertValues.append(parseChannelToMetaInsert(logChannel));
        }

        sbMetaInsertValues.replace(sbMetaInsertValues.length() - 3, sbMetaInsertValues.length(), ";");
        sbMetaInsert.append(sbMetaInsertValues);

        try {
            if (metaEntriesChanged()) {
                return sbMetaInsert;
            }
        } catch (SQLException e) {
            logger.warn("Exception at reading existing meta entries: {}", e.getMessage());
            return sbMetaInsert;
        }

        return new StringBuilder();
    }

    private ResultSet getExistingEntries() throws SQLException {
        StringBuilder sbMetaSelect = new StringBuilder(
                "SELECT channelid,channelAdress,loggingInterval,loggingTimeOffset,unit,valueType,scalingFactor,valueOffset,"
                        + "listening,samplingInterval,samplingTimeOffset,SamplingGroup,disabled,description FROM"
                        + " openmuc_meta ;");
        // ToDO: needed?
        // WHERE time IN (SELECT * FROM (SELECT time FROM openmuc_meta ORDER BY time DESC LIMIT 1) as time)

        return dbAccess.executeQuery(sbMetaSelect);
    }

    private String parseChannelToMetaInsert(LogChannel logChannel) {
        List<Integer> varcharLength = dbAccess.getColumnLength(COLUMNS, "openmuc_meta");
        Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());
        StringBuilder channelAsString = new StringBuilder();

        String channelAddress = logChannel.getChannelAddress();
        String scalingFactor = getScalingFactor(logChannel);
        String valueOffset = getValueOffset(logChannel);
        String listening = logChannel.isListening().toString();
        String samplingInterval = logChannel.getSamplingInterval().toString();
        String samplingTimeOffset = logChannel.getSamplingTimeOffset().toString();
        String samplingGroup = logChannel.getSamplingGroup();
        String disabled = logChannel.isDisabled().toString();
        String loggingInterval = getLoggingInterval(logChannel);
        String valueTypeLength = getValueTypeLength(logChannel);
        String loggingTimeOffset = logChannel.getLoggingTimeOffset().toString();
        String channelId = logChannel.getId();
        String unit = logChannel.getUnit();
        ValueType vType = logChannel.getValueType();
        String valueType = vType.toString();
        String loggingEvent = String.valueOf(logChannel.isLoggingEvent());
        String description = logChannel.getDescription();

        if (description.equals("")) {
            description = "-";
        }

        // buggy -> needed?
        // List<String> newColumnNames = Arrays.asList(channelId, channelAddress, loggingInterval, loggingTimeOffset,
        // unit,
        // valueType, scalingFactor, valueOffset, listening, loggingEvent, samplingInterval, samplingTimeOffset,
        // samplingGroup, disabled, description);
        //
        // for (int i = 0; i < newColumnNames.size(); ++i) {
        // if (varcharLength.get(i) < newColumnNames.get(i).length()) {
        // increaseDescriptionColumnLength("openmuc_meta", newColumnNames.get(i), COLUMNS.get(i));
        // }
        // }

        resultComparison.append(channelId)
                .append(',')
                .append(channelAddress)
                .append(',')
                .append(loggingInterval)
                .append(',')
                .append(loggingTimeOffset)
                .append(',')
                .append(unit)
                .append(',')
                .append(valueType)
                .append(',')
                // .append(' ')
                // .append(valueTypeLength)
                // .append(',')
                .append(scalingFactor)
                .append(',')
                .append(valueOffset)
                .append(',')
                .append(listening)
                .append(',')
                .append(loggingEvent)
                .append(',')
                .append(samplingInterval)
                .append(',')
                .append(samplingTimeOffset)
                .append(',')
                .append(samplingGroup)
                .append(',')
                .append(disabled)
                .append(',')
                .append(description)
                .append(',');

        channelAsString.append('\'')
                .append(sqlTimestamp)
                .append("',\'")
                .append(channelId)
                .append("','")
                .append(channelAddress)
                .append("','")
                .append(loggingInterval)
                .append("','")
                .append(loggingTimeOffset)
                .append("','")
                .append(unit)
                .append("','")
                .append(valueType)
                .append("','")
                // .append(' ')
                // .append(valueTypeLength)
                // .append("','")
                .append(scalingFactor)
                .append("','")
                .append(valueOffset)
                .append("','")
                .append(listening)
                .append("','")
                .append(loggingEvent)
                .append("','")
                .append(samplingInterval)
                .append("','")
                .append(samplingTimeOffset)
                .append("','")
                .append(samplingGroup)
                .append("','")
                .append(disabled)
                .append("','")
                .append(description)
                .append("'), (");

        return channelAsString.toString();
    }

    private boolean metaEntriesChanged() throws SQLException {
        ResultSet existingEntries = getExistingEntries();
        ResultSetMetaData metaOfExistingEntries = existingEntries.getMetaData();
        int colCount = metaOfExistingEntries.getColumnCount();
        boolean noEntriesExists = true;
        if (colCount <= 0) {
            return true;
        }

        while (existingEntries.next()) {
            noEntriesExists = false;
            StringBuilder entry = new StringBuilder();

            for (int index = 1; index <= colCount; index++) {
                entry.append(existingEntries.getString(index));
                entry.append(",");
            }
            if (entry != null && !resultComparison.toString().contains(entry)) {
                return true;
            }
        }

        return noEntriesExists;
    }

    /**
     * returns the value offset attribute of the channel
     *
     * @param logChannel
     *            channel to be logged
     * @return the value offset attribute of the channel
     */
    private String getValueOffset(LogChannel logChannel) {
        String valueOffset;
        if (logChannel.getValueOffset() == null) {
            valueOffset = "0";
        }
        else {
            valueOffset = logChannel.getValueOffset().toString();
        }
        return valueOffset;
    }

    /**
     * returns the scaling factor attribute of the channel
     *
     * @param logChannel
     *            channel to be logged
     * @return the scaling factor attribute of the channel
     */
    private String getScalingFactor(LogChannel logChannel) {
        String scalingFactor;
        if (logChannel.getScalingFactor() == null) {
            scalingFactor = "0";
        }
        else {
            scalingFactor = logChannel.getScalingFactor().toString();
        }
        return scalingFactor;
    }

    /**
     * returns the logging interval attribute of the channel
     *
     * @param logChannel
     *            channel to be logged
     * @return the logging interval attribute of the channel
     */
    private String getLoggingInterval(LogChannel logChannel) {
        String loggingInterval;
        if (logChannel.getLoggingInterval() == null) {
            loggingInterval = "0";
        }
        else {
            loggingInterval = logChannel.getLoggingInterval().toString();
        }
        return loggingInterval;
    }

    /**
     * returns the valuetype length attribute of the channel
     *
     * @param logChannel
     *            channel to be logged
     * @return the valuetype length attribute of the channel
     */
    private String getValueTypeLength(LogChannel logChannel) {
        String valueTypeLength;
        if (logChannel.getValueTypeLength() == null) {
            valueTypeLength = "0";
        }
        else {
            valueTypeLength = logChannel.getValueTypeLength().toString();
        }
        return valueTypeLength;
    }

    /**
     * Increases the length of a column
     *
     * @param table
     *            Table to be altered
     * @param column
     *            Length to be set for the column
     * @param columnName
     *            Column to be altered
     */
    private void increaseDescriptionColumnLength(String table, String column, String columnName) {
        StringBuilder sbNewVarcharLength = new StringBuilder();

        if (url.contains(MYSQL)) {
            sbNewVarcharLength.append("ALTER TABLE " + table + " MODIFY " + columnName + " VARCHAR (");
        }
        else {
            sbNewVarcharLength.append("ALTER TABLE " + table + " ALTER COLUMN " + columnName + " TYPE VARCHAR (");
        }

        sbNewVarcharLength.append(column.length()).append(");");
        dbAccess.executeSQL(sbNewVarcharLength);
    }
}
