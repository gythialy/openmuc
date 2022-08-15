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

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRESQL;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.VALUE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbAccess {

    private final Logger logger = LoggerFactory.getLogger(DbAccess.class);
    private final String url;
    private final DbConnector dbConnector;

    public DbAccess() {
        dbConnector = new DbConnector();
        PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
        url = propertyHandler.getString(Settings.URL);
        if (url.contains("h2") && url.contains("tcp")) {
            dbConnector.startH2Server();
        }
    }

    private DbAccess(DbConnector connector) { // for testing
        url = "";
        this.dbConnector = connector;
    }

    static protected DbAccess getTestInstance(DbConnector connector) {
        return new DbAccess(connector);
    }

    /**
     * Converts StringBuilder to String
     *
     * @param sb
     *            StringBuilder to convert
     */
    public void executeSQL(StringBuilder sb) {
        String sql = sb.toString();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        if (!dbConnector.isConnected()) {
            dbConnector.getConnectionToDb();
        }
        synchronized (dbConnector) {
            synchronizeStatement(sql);
        }
    }

    private void synchronizeStatement(String sql) {
        try (Statement statement = dbConnector.createStatementWithConnection()) {
            statement.execute(sql);
        } catch (SQLException e) {
            logger.error(MessageFormat.format("Error executing SQL: \n{0}", sql), e.getMessage());
            logger.error(MessageFormat.format("SQLState:     {0}", e.getSQLState()));
            logger.error(MessageFormat.format("VendorError:  {0}", e.getErrorCode()));
        }
    }

    public ResultSet executeQuery(StringBuilder sb) throws SQLException {
        Statement statement = dbConnector.createStatementWithConnection();
        return statement.executeQuery(sb.toString());
    }

    public boolean timeScaleIsActive() {
        StringBuilder sbExtensions = new StringBuilder("SELECT * FROM pg_extension;");

        try (ResultSet resultSet = dbConnector.createStatementWithConnection().executeQuery(sbExtensions.toString())) {
            while (resultSet.next()) {
                return resultSet.getString("extname").contains("timescale");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    /**
     * Queries the database for a columns length and then returns it as a list of ints
     *
     * @param columns
     *            List containing all column names
     * @param table
     *            name of the table
     * @return a list containing each columns length
     */
    public List<Integer> getColumnLength(List<String> columns, String table) {
        ArrayList<Integer> columnsLength = new ArrayList<>();

        if (url.contains(POSTGRESQL)) {
            table = table.toLowerCase();
        }
        for (String column : columns) {
            StringBuilder sbVarcharLength = new StringBuilder();
            sbVarcharLength.append("select character_maximum_length from information_schema.columns")
                    .append(" where table_name = '" + table + "' AND column_name = '" + column.toLowerCase() + "';");

            try {
                if (!dbConnector.isConnected()) {
                    dbConnector.getConnectionToDb();
                }
                ResultSet rsLength = executeQuery(sbVarcharLength);
                rsLength.next();
                columnsLength.add(rsLength.getInt(1));
            } catch (SQLException e) {
                logger.debug(e.getMessage());
                columnsLength.add(0);
            }
        }
        return columnsLength;
    }

    public void closeConnection() {
        dbConnector.closeConnection();
    }

    /**
     * Retrieves data from database and adds it to records
     */

    public List<Record> queryRecords(StringBuilder sb, ValueType valuetype) {
        // retrieve numeric values from database and add them to the records list
        List<Record> records = new ArrayList<>();
        if (!dbConnector.isConnected()) {
            dbConnector.getConnectionToDb();
        }
        try (ResultSet resultSet = executeQuery(sb)) {
            while (resultSet.next()) {
                if (valuetype == ValueType.STRING) {
                    Record rc = new Record(new StringValue(resultSet.getString(VALUE)),
                            resultSet.getTimestamp("time").getTime(), Flag.VALID);
                    records.add(rc);
                }
                else if (valuetype == ValueType.BYTE_ARRAY) {
                    Record rc = new Record(new ByteArrayValue(resultSet.getBytes(VALUE)),
                            resultSet.getTimestamp("time").getTime(), Flag.VALID);
                    records.add(rc);
                }
                else if (valuetype == ValueType.BOOLEAN) {
                    Record rc = new Record(new BooleanValue(resultSet.getBoolean(VALUE)),
                            resultSet.getTimestamp("time").getTime(), Flag.VALID);
                    records.add(rc);
                }
                else {
                    Record rc = new Record(new DoubleValue(resultSet.getDouble(VALUE)),
                            resultSet.getTimestamp("time").getTime(), Flag.VALID);
                    records.add(rc);
                }
            }
        } catch (SQLException e) {
            String sql = sb.toString();
            logger.error(MessageFormat.format("Error executing SQL: \n{0}", sql), e.getMessage());
        }

        return records;
    }
}
