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

import java.sql.Timestamp;
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
    private final List<StringBuilder> tableListChannel;

    public SqlWriter(DbAccess dbAccess) {
        this.dbAccess = dbAccess;
        tableListChannel = new ArrayList<>();

    }

    public void writeEventBasedContainerToDb(List<LoggingRecord> containers) {
        synchronized (tableListChannel) {
            writeAsTableList(containers);
            tableListChannel.clear();
        }
    }

    private void writeAsTableList(List<LoggingRecord> containers) {
        // createTableList();
        addRecordsFromContainersToList(containers);

        for (StringBuilder table : tableListChannel) {
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
        // createTableList();

        for (LoggingRecord logRecordContainer : containers) {
            addContainerToList(sqlTimestamp, logRecordContainer);
        }

        for (StringBuilder table : tableListChannel) {
            if (table.toString().contains("),")) {
                table.replace(table.length() - 1, table.length(), ";");
                dbAccess.executeSQL(table);
            }
        }

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

        if (record.getValue() != null) {
            StringBuilder sbChannel = new StringBuilder("INSERT INTO " + channelId + " (time,flag,\"VALUE\") VALUES ");
            StringBuilder sbQuery2 = new StringBuilder();
            sbQuery2.append("('")
                    .append(sqlTimestamp)
                    .append("',")
                    .append(logRecordContainer.getRecord().getFlag().getCode())
                    .append(',');

            sbChannel.append(sbQuery2);
            if (record.getValue() != null) {
                SqlValues.appendValue(record.getValue(), sbChannel);
            }
            else {
                sbChannel.append("NULL");
            }
            sbChannel.append("),");
            Collections.addAll(tableListChannel, sbChannel);
        }
    }

}
