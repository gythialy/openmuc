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

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.AND;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmuc.framework.data.Record;

public class SqlReader {

    private final DbAccess dbAccess;

    public SqlReader(DbAccess dbAccess) {
        this.dbAccess = dbAccess;
    }

    public List<Record> readRecordListFromDb(String channelId, long startTime, long endTime) {
        Timestamp startTimestamp = new Timestamp(startTime);
        Timestamp endTimestamp = new Timestamp(endTime);

        ArrayList<String> tableNameList = new ArrayList<>();
        Collections.addAll(tableNameList, DOUBLE_VALUE, FLOAT_VALUE, INT_VALUE, LONG_VALUE, BYTE_VALUE, SHORT_VALUE);

        StringBuilder sbNumeric = new StringBuilder();
        StringBuilder sbString = new StringBuilder();
        StringBuilder sbByteArray = new StringBuilder();
        StringBuilder sbBoolean = new StringBuilder();

        // All numeric datatypes can be retrieved from the database with one query
        for (String tableName : tableNameList) {
            selectFromTable(channelId, startTimestamp, endTimestamp, tableName, sbNumeric);
            sbNumeric.replace(sbNumeric.length() - 1, sbNumeric.length(), " UNION ALL ");
        }
        sbNumeric.replace(sbNumeric.length() - 11, sbNumeric.length(), ";");
        selectFromTable(channelId, startTimestamp, endTimestamp, STRING_VALUE, sbString);
        selectFromTable(channelId, startTimestamp, endTimestamp, BYTE_ARRAY_VALUE, sbByteArray);
        selectFromTable(channelId, startTimestamp, endTimestamp, BOOLEAN_VALUE, sbBoolean);

        return dbAccess.queryRecords(sbNumeric, sbString, sbByteArray, sbBoolean);
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
    private void selectFromTable(String channelId, Timestamp startTimestamp, Timestamp endTimestamp, String tableName,
            StringBuilder sb) {

        sb.append("SELECT time,value FROM ")
                .append(tableName)
                .append(" WHERE channelId = '")
                .append(channelId)
                .append("' AND time BETWEEN '")
                .append(startTimestamp)
                .append(AND)
                .append(endTimestamp)
                .append("';");
    }
}
