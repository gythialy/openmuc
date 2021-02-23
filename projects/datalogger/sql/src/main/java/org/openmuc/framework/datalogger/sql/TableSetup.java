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

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.MYSQL;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRES;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRESQL;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BOOLEAN_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_ARRAY_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.BYTE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.DOUBLE_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.FLOAT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.INT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.LONG_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.SHORT_VALUE;
import static org.openmuc.framework.datalogger.sql.utils.TabelNames.STRING_VALUE;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableSetup {

    private final Logger logger = LoggerFactory.getLogger(TableSetup.class);
    private final List<LogChannel> channels;
    private final DbAccess dbAccess;
    private final String url;

    public TableSetup(List<LogChannel> channels, DbAccess dbAccess) {
        this.dbAccess = dbAccess;
        this.channels = channels;
        PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
        url = propertyHandler.getString(Settings.URL);
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

    /**
     * Creates and executes the queries to create a table for each data type. Following methods are used to create the
     * queries: {@link #appendTimestamp(StringBuilder)} to append the timestamp column to the query
     * <p>
     * This method further creates linked table using createLinkedTable() and inserts all data present in local db is
     * set.
     */
    public void createOpenmucTables() {

        List<String> tableNameList = new ArrayList<>();
        Collections.addAll(tableNameList, BOOLEAN_VALUE, BYTE_ARRAY_VALUE, FLOAT_VALUE, DOUBLE_VALUE, INT_VALUE,
                LONG_VALUE, BYTE_VALUE, SHORT_VALUE, STRING_VALUE);

        List<JDBCType> typeList = new ArrayList<>();
        Collections.addAll(typeList, JDBCType.BOOLEAN, JDBCType.LONGVARBINARY, JDBCType.FLOAT, JDBCType.DOUBLE,
                JDBCType.INTEGER, JDBCType.BIGINT, JDBCType.SMALLINT, JDBCType.SMALLINT, JDBCType.VARCHAR);

        for (int i = 0; i < tableNameList.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(tableNameList.get(i));

            appendTimestamp(sb);

            sb.append("channelID VARCHAR(40) NOT NULL,")
                    .append("flag ")
                    .append(JDBCType.SMALLINT)
                    .append(" NOT NULL,")
                    .append("value ");

            appendTypeList(typeList, i, sb);

            if (i == 8) {
                sb.append(" (100)");
            }

            appendMySqlIndex(tableNameList, i, sb);
            sb.append(",PRIMARY KEY (channelid, time));");
            dbAccess.executeSQL(sb);
            activatePostgreSqlIndex(tableNameList, i);
            activateTimescaleDbHypertable(tableNameList, i);
        }
        // reduceSizeOfChannelIdCol(tableNameList);
    }

    private void reduceSizeOfChannelIdCol(List<String> tableNameList) {
        // FIXME
        for (LogChannel logChannel : channels) {
            String channelId = logChannel.getId();
            List<String> columns = Arrays.asList("channelid");
            List<Integer> varcharLength = dbAccess.getColumnLength(columns, DOUBLE_VALUE);

            if (varcharLength.get(0) < channelId.length()) {
                for (String table : tableNameList) {
                    increaseDescriptionColumnLength(table, channelId, columns.get(0));
                }
            }
        }
    }

    /**
     * Append MySQL specific query to create a tables' Index
     *
     * @param tableNameList
     *            List containing the names of all data type tables
     * @param i
     *            Index for the tableNameList
     * @param sb
     *            StringBuilder for the query
     */
    private void appendMySqlIndex(List<String> tableNameList, int i, StringBuilder sb) {
        if (!url.contains(POSTGRESQL)) {
            sb.append(",INDEX ").append(tableNameList.get(i)).append("Index(time)");
        }
    }

    /**
     * Sends query to turn this table into a timescale hypertable
     *
     * @param tableNameList
     *            List containing the names of all data type tables
     * @param i
     *            Index for the tableNameList
     */
    private void activateTimescaleDbHypertable(List<String> tableNameList, int i) {
        if (url.contains(POSTGRESQL) && dbAccess.timeScaleIsActive()) {
            try {
                dbAccess.executeQuery(new StringBuilder(
                        "SELECT create_hypertable('" + tableNameList.get(i) + "', 'time', if_not_exists => TRUE);"));
            } catch (SQLException e) {
                logger.error(MessageFormat.format("{0}test", e.getMessage()));
            }
        }
    }

    /**
     * Execute PostgreSQl specific query to create Index if timescale is not activated
     *
     * @param tableNameList
     *            List containing the names of all data type tables
     * @param i
     *            Index for the tableNameList
     */
    private void activatePostgreSqlIndex(List<String> tableNameList, int i) {
        if (url.contains(POSTGRESQL) && !dbAccess.timeScaleIsActive()) {
            StringBuilder sbIndex = new StringBuilder("CREATE INDEX IF NOT EXISTS ");
            sbIndex.append(tableNameList.get(i)).append("Index ON ").append(tableNameList.get(i)).append(" (time);");

            dbAccess.executeSQL(sbIndex);
        }
    }

    /**
     * @param typeList
     *            List containing all JDBC data types
     * @param i
     *            Index for typeList
     * @param sb
     *            StringBuilder containing the query
     */
    private void appendTypeList(List<JDBCType> typeList, int i, StringBuilder sb) {
        if (i == 1) {
            byteArrayDataType(typeList, i, sb);
        }
        else if (i == 3) {
            doubleDataType(typeList, i, sb);
        }
        else {
            sb.append(typeList.get(i));
        }
    }

    /**
     * Appends TIMESTAMPTZ(timezone) if PostgreSQL is used or TIMSTAMP if not
     *
     * @param sb
     *            StingBuilder of the query
     */
    private void appendTimestamp(StringBuilder sb) {
        if (url.contains(POSTGRES)) {
            sb.append("(time TIMESTAMPTZ NOT NULL,\n");
        }
        else {
            sb.append("(time TIMESTAMP NOT NULL,\n");
        }
    }

    /**
     * Appends "DOUBLE PRECISION" to the query if PostgreSQL is used or double if not
     *
     * @param typeList
     *            List containing all JDBC data types
     * @param i
     *            Index for typeList
     * @param sb
     *            StringBuilder containing the query
     */
    private void doubleDataType(List<JDBCType> typeList, int i, StringBuilder sb) {
        if (url.contains(POSTGRESQL)) {
            sb.append("DOUBLE PRECISION");
        }
        else {
            sb.append(typeList.get(i));
        }
    }

    /**
     * Appends the appropriate data type for byte array to the query depending on the used database
     *
     * @param typeList
     *            List containing all JDBC data types
     * @param i
     *            Index for typeList
     * @param sb
     *            StringBuilder containing the query
     */
    private void byteArrayDataType(List<JDBCType> typeList, int i, StringBuilder sb) {
        if (url.contains(POSTGRESQL)) {
            sb.append("BYTEA");
        }
        else if (url.contains(MYSQL)) {
            sb.append("BLOB");
        }
        else {
            sb.append(typeList.get(i));
        }
    }
}
