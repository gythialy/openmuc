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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;

public class SqlReaderTest {

    private SqlReader sqlReader;
    private DbAccess dbAccess, dbAccessSpy;
    private DbConnector dbConnectorMock;
    private Connection connection;

    private final String channelId = "testChannel";
    private final ValueType valueType = ValueType.DOUBLE;

    @BeforeEach
    void setup() throws SQLException {
        connection = TestConnectionHelper.getConnection();

        dbConnectorMock = mock(DbConnector.class);
        dbAccess = DbAccess.getTestInstance(dbConnectorMock); // Real DbAccess with mock DbConnector to prevent null
                                                              // pointer exception in queryRecords
        dbAccessSpy = spy(dbAccess); // DbAccess with modified executeQuery

        doAnswer(invocation -> { // pass any executed sql queries to the test connection
            return TestConnectionHelper.executeQuery(connection, invocation.getArgument(0).toString());
        }).when(dbAccessSpy).executeQuery(any());

        sqlReader = new SqlReader(dbAccessSpy);
    }

    @Test
    void readLatestRecordFromDb() throws SQLException {
        writeTestRecords();

        Record record = sqlReader.readLatestRecordFromDb(channelId, valueType);
        assertTrue(record.getValue().asDouble() == 2);

        connection.close();
    }

    void writeTestRecords() throws SQLException {
        TestConnectionHelper.executeSQL(connection,
                String.format("CREATE TABLE %s (time TIMESTAMP NOT NULL, " + "\"VALUE\" DOUBLE)", channelId));
        TestConnectionHelper.executeSQL(connection,
                String.format("INSERT INTO %s (time, \"VALUE\") VALUES ('2020-09-08 14:43:39.0', 1)", channelId));
        TestConnectionHelper.executeSQL(connection,
                String.format("INSERT INTO %s (time, \"VALUE\") VALUES ('2021-09-08 14:43:39.0', 2)", channelId)); // Latest
                                                                                                                   // Date
        TestConnectionHelper.executeSQL(connection,
                String.format("INSERT INTO %s (time, \"VALUE\") VALUES ('2020-09-08 13:43:39.0', 3)", channelId));
    }
}
