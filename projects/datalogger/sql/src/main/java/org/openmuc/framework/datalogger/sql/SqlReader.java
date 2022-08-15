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

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.AND;

import java.sql.Timestamp;
import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;

public class SqlReader {

    private final DbAccess dbAccess;

    public SqlReader(DbAccess dbAccess) {
        this.dbAccess = dbAccess;
    }

    public List<Record> readRecordListFromDb(String channelId, ValueType valuetype, long startTime, long endTime) {
        Timestamp startTimestamp = new Timestamp(startTime);
        Timestamp endTimestamp = new Timestamp(endTime);
        StringBuilder sbTable = new StringBuilder();
        selectFromTable(channelId, startTimestamp, endTimestamp, sbTable);
        return dbAccess.queryRecords(sbTable, valuetype);

    }

    /**
     * Get the latest Record by retrieving records in descending order - ordered by time - and limiting to 1 result
     * 
     * @param channelId
     *            ID of the channel
     * @param valuetype
     *            {@link ValueType}
     * @return latest Record with the highest timestamp
     */
    public Record readLatestRecordFromDb(String channelId, ValueType valuetype) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT time,\"VALUE\" FROM ").append(channelId).append(" ORDER BY time DESC LIMIT 1;");
        List<Record> records = dbAccess.queryRecords(sb, valuetype);
        if (records.size() == 1) {
            return records.get(0);
        }
        return null;
    }

    /**
     * Builds Select query using the parameters
     *
     * @param channelId
     *            Name of the channel
     * @param startTimestamp
     *            Start of the timeframe to retrieve data from
     * @param endTimestamp
     *            End of the timeframe to retrieve data from
     * @param tableName
     *            Name of the table
     * @param sb
     *            StringBuilder for the Query
     */
    private void selectFromTable(String channelId, Timestamp startTimestamp, Timestamp endTimestamp, StringBuilder sb) {

        // sb.append("SELECT time,value FROM ")
        // .append(tableName)
        // .append(" WHERE channelId = '")
        // .append(channelId)
        // .append("' AND time BETWEEN '")
        // .append(startTimestamp)
        // .append(AND)
        // .append(endTimestamp)
        // .append("';");

        sb.append("SELECT time,\"VALUE\" FROM ")
                .append(channelId)
                .append(" WHERE time BETWEEN '")
                .append(startTimestamp)
                .append(AND)
                .append(endTimestamp)
                .append("';");
    }
}
