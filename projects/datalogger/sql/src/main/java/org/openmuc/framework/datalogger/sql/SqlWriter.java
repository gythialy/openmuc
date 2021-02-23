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

package org.openmuc.framework.datalogger.sql;

import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BOOLEAN_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_ARRAY_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.DOUBLE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.FLOAT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.INT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.LONG_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.SHORT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.STRING_VALUE;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.datalogger.sql.utils.SqlValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlWriter {

    private static final Logger logger = LoggerFactory.getLogger(SqlWriter.class);
    private final DbAccess dbAccess;
    private final List<StringBuilder> tableList;

    public SqlWriter(DbAccess dbAccess) {
        this.dbAccess = dbAccess;
        tableList = new ArrayList<>();
    }

    public void writeEventBasedContainerToDb(List<LoggingRecord> containers) {
        synchronized (tableList) {
            writeAsTableList(containers);
            tableList.clear();
        }
    }

    private void writeAsTableList(List<LoggingRecord> containers) {
        createTableList();
        addRecordsFromContainersToList(containers);
        for (StringBuilder table : tableList) {
            if (table.toString().contains("),")) {
                table.replace(table.length() - 1, table.length(), ";");
                dbAccess.executeSQL(table);
            }
        }
    }

    private void addRecordsFromContainersToList(List<LoggingRecord> containers) {
        for (LoggingRecord logRecordContainer : containers) {
            if (logRecordContainer.getRecord().getTimestamp() != null) {
                long recordTs = logRecordContainer.getRecord().getTimestamp();
                Timestamp sqlTimestamp = new Timestamp(recordTs);
                addContainerToList(sqlTimestamp, logRecordContainer);
            }
        }
    }

    public void writeRecordContainerToDb(List<LoggingRecord> containers, long timestamp) {
        Timestamp sqlTimestamp = new Timestamp(timestamp);
        createTableList();

        for (LoggingRecord logRecordContainer : containers) {
            addContainerToList(sqlTimestamp, logRecordContainer);
        }

        for (StringBuilder table : tableList) {
            if (table.toString().contains("),")) {
                table.replace(table.length() - 1, table.length(), ";");
                dbAccess.executeSQL(table);
            }
        }
    }

    /**
     * Creates StringBuilders for the generic part of each insert query, puts them in a list and then returns them
     *
     * @return A list of StringBuilders
     */
    private void createTableList() {
        tableList.clear();
        StringBuilder sbBoolean = new StringBuilder("INSERT INTO BooleanValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbByteArray = new StringBuilder("INSERT INTO ByteArrayValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbFloat = new StringBuilder("INSERT INTO FloatValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbDouble = new StringBuilder("INSERT INTO DoubleValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbInt = new StringBuilder("INSERT INTO IntValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbLong = new StringBuilder("INSERT INTO LongValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbByte = new StringBuilder("INSERT INTO ByteValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbShort = new StringBuilder("INSERT INTO ShortValue (time,channelID,flag,value) VALUES ");
        StringBuilder sbString = new StringBuilder("INSERT INTO StringValue (time,channelID,flag,value) VALUES ");

        Collections.addAll(tableList, sbBoolean, sbByteArray, sbFloat, sbDouble, sbInt, sbLong, sbByte, sbShort,
                sbString);
    }

    /**
     * Continues building the insert query and calls {@link #addValue(Record, StringBuilder, Integer)} using the
     * appropriate parameters for the records' value type
     *
     * @param sqlTimestamp
     *            The current timestamp
     * @param logRecordContainer
     *            Container object for the record
     */
    private void addContainerToList(Timestamp sqlTimestamp, LoggingRecord logRecordContainer) {
        String channelId = logRecordContainer.getChannelId();
        Record record = logRecordContainer.getRecord();

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("('")
                .append(sqlTimestamp)
                .append("','")
                .append(channelId)
                .append("',")
                .append(logRecordContainer.getRecord().getFlag().getCode())
                .append(',');

        mapRecordToTable(record, sbQuery);
    }

    private void mapRecordToTable(Record record, StringBuilder sbQuery) {
        if (record.getValue() != null) {
            String valueType = record.getValue().getClass().getSimpleName();
            switch (valueType) {
            case BOOLEAN_VALUE:
                break;
            case BYTE_ARRAY_VALUE:
                addValue(record, sbQuery, 1);
                break;
            case FLOAT_VALUE:
                addValue(record, sbQuery, 2);
                break;
            case DOUBLE_VALUE:
                addValue(record, sbQuery, 3);
                break;
            case INT_VALUE:
                addValue(record, sbQuery, 4);
                break;
            case LONG_VALUE:
                addValue(record, sbQuery, 5);
                break;
            case BYTE_VALUE:
                addValue(record, sbQuery, 6);
                break;
            case SHORT_VALUE:
                addValue(record, sbQuery, 7);
                break;
            case STRING_VALUE:
                addValue(record, sbQuery, 8);
                break;
            default:
                // should not happen
                logger.error(MessageFormat.format("Unknown value type \"{0}\"", valueType));
                addValue(record, sbQuery, 3);
                break;
            }
        }
    }

    /**
     * Appends the channel value to the insert query
     *
     * @param record
     *            Contains the channels' value
     * @param sbQuery
     *            The generic part of the query built
     * @param index
     *            Index to get the appropriate tables' StringBuilder from tableList
     */
    private void addValue(Record record, StringBuilder sbQuery, Integer index) {
        tableList.get(index).append(sbQuery);

        if (record.getValue() != null) {
            SqlValues.appendValue(record.getValue(), tableList.get(index));
        }
        else {
            tableList.get(index).append("NULL");
        }
        tableList.get(index).append("),");
    }

}
