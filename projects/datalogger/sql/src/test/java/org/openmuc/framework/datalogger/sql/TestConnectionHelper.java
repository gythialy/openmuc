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

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.h2.Driver;
import org.h2.util.OsgiDataSourceFactory;

public class TestConnectionHelper {
    public static final String DB_DRIVER = "org.h2.Driver";
    public static final String DB_CONNECTION = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1;" + "MODE=MYSQL";

    /**
     * Creates a in-memory database for testing
     *
     * @return Connection to the database
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("url", DB_CONNECTION);
        properties.setProperty("password", "");
        properties.setProperty("user", "");

        OsgiDataSourceFactory dataSourceFactory = null;
        try {
            dataSourceFactory = new OsgiDataSourceFactory(
                    (Driver) Class.forName(DB_DRIVER).getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                | ClassNotFoundException e) {
            e.printStackTrace();
        }

        assert dataSourceFactory != null;
        DataSource dataSource = dataSourceFactory.createDataSource(properties);

        return dataSource.getConnection();
    }

    /**
     * Executes the sql statement on the connection
     *
     * @param connection
     * @param sql
     * @throws SQLException
     */
    public static void executeSQL(Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

    /**
     * Executes the query on the connection
     *
     * @param connection
     * @param sql
     * @return
     * @throws SQLException
     */
    public static ResultSet executeQuery(Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(sql);
    }
}
