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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

class SqlWriterTest {

    private SqlWriter sqlWriter;
    private DbAccess dbAccessMock;
    private Connection connection;

    @BeforeEach
    void setup() throws SQLException {
        connection = TestConnectionHelper.getConnection();

        dbAccessMock = mock(DbAccess.class);

        doAnswer(invocation -> { // pass any executed sql statements to the test connection
            TestConnectionHelper.executeSQL(connection, invocation.getArgument(0).toString());
            return null;
        }).when(dbAccessMock).executeSQL(any());

        sqlWriter = new SqlWriter(dbAccessMock);
    }

    @Test
    void writeEventBasedContainerToDb() throws SQLException {
        List<LoggingRecord> recordList = buildLoggingRecordList(5);

        TestConnectionHelper.executeSQL(connection, String.format( // create table for the tests to write to
                "CREATE TABLE %s (time TIMESTAMP NOT NULL, " + "flag SMALLINT NOT NULL, \"VALUE\" DOUBLE)",
                recordList.get(0).getChannelId()));

        sqlWriter.writeEventBasedContainerToDb(recordList);

        verify(dbAccessMock, times(5)).executeSQL(any());

        connection.close();
    }

    private List<LoggingRecord> buildLoggingRecordList(int numOfElements) {
        String channelId = "testChannel";
        Value value = new DoubleValue(5);
        long timestamp = 1599569019000L;
        Record record = new Record(value, timestamp, Flag.VALID);

        return Collections.nCopies(numOfElements, new LoggingRecord(channelId, record));
    }
}
