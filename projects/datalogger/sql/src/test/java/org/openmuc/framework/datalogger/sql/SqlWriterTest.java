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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

class SqlWriterTest {

    private SqlWriter sqlWriter;
    private DbAccess dbAccessMock;

    @BeforeEach
    void setup() {
        dbAccessMock = mock(DbAccess.class);
        sqlWriter = new SqlWriter(dbAccessMock);
    }

    @Test
    void writeEventBasedContainerToDb() {
        ArgumentCaptor<StringBuilder> argument = ArgumentCaptor.forClass(StringBuilder.class);

        List<LoggingRecord> recordList = buildLoggingRecordList(5);
        sqlWriter.writeEventBasedContainerToDb(recordList);
        verify(dbAccessMock, times(1)).executeSQL(argument.capture());

        System.out.println(argument.getValue().toString());
    }

    private List<LoggingRecord> buildLoggingRecordList(int numOfElements) {
        String channelId = "testChannel";
        Value value = new DoubleValue(5);
        long timestamp = 1599569019000L;
        Record record = new Record(value, timestamp, Flag.VALID);

        return Collections.nCopies(numOfElements, new LoggingRecord(channelId, record));
    }
}
