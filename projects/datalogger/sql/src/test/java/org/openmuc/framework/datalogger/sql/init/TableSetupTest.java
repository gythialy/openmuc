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

package org.openmuc.framework.datalogger.sql.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.sql.DbAccess;
import org.openmuc.framework.datalogger.sql.MetaBuilder;
import org.openmuc.framework.datalogger.sql.TableSetup;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;

class TableSetupTest {

    private final static String INSERT_META_ENTRIES_PATTERN = "INSERT INTO openmuc_meta (time,channelid,channelAdress,loggingInterval,loggingTimeOffset,unit,valueType,scalingFactor,valueOffset,listening,loggingEvent,samplingInterval,samplingTimeOffset,SamplingGroup,disabled,description) VALUES";
    private TableSetup tableSetup;
    private MetaBuilder metaBuilder;
    private DbAccess accessMock;

    @BeforeEach
    void setupInitializer() throws SQLException {
        accessMock = mock(DbAccess.class);
        List<LogChannel> channelList = new ArrayList<>();
        channelList.add(getMockedChannel("gridPower"));
        channelList.add(getMockedChannel("pvPower"));
        ResultSet resultMocked = mock(ResultSet.class);
        ResultSetMetaData resultMetaMock = mock(ResultSetMetaData.class);
        when(resultMetaMock.getColumnCount()).thenReturn(0);
        when(resultMocked.getMetaData()).thenReturn(resultMetaMock);
        when(accessMock.executeQuery(any())).thenReturn(resultMocked);
        when(accessMock.getColumnLength(anyList(), anyString())).thenReturn(Collections.nCopies(20, 20));

        PropertyHandler propHandlerMock = mock(PropertyHandler.class);
        when(propHandlerMock.getString(Settings.URL)).thenReturn("jdbc:h2");
        PropertyHandlerProvider.getInstance().setPropertyHandler(propHandlerMock);

        tableSetup = new TableSetup(channelList, accessMock);
        metaBuilder = new MetaBuilder(channelList, accessMock);
    }

    private LogChannel getMockedChannel(String channelId) {
        LogChannel channelMock = mock(LogChannel.class);
        when(channelMock.getId()).thenReturn(channelId);
        when(channelMock.getValueType()).thenReturn(ValueType.DOUBLE);
        when(channelMock.getDescription()).thenReturn("");
        when(channelMock.getChannelAddress()).thenReturn("address");
        when(channelMock.getUnit()).thenReturn("W");
        when(channelMock.getSamplingGroup()).thenReturn("sg");
        when(channelMock.getDescription()).thenReturn("");

        return channelMock;
    }

    @Test
    void initNewMetaTable() {
        metaBuilder.writeMetaTable();
        ArgumentCaptor<StringBuilder> sqlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(accessMock, atLeastOnce()).executeSQL(sqlCaptor.capture());

        String sqlConstraint = sqlCaptor.getValue().toString();
        assertTrue(sqlConstraint.contains(INSERT_META_ENTRIES_PATTERN));
        assertTrue(sqlConstraint.contains("gridPower") && sqlConstraint.contains("pvPower"));
    }

    @Test
    void createOpenmucTables() {
        tableSetup.createOpenmucTables();
        ArgumentCaptor<StringBuilder> sqlCaptor = ArgumentCaptor.forClass(StringBuilder.class);

        verify(accessMock, atLeastOnce()).executeSQL(sqlCaptor.capture());
        List<StringBuilder> returnedBuilder = sqlCaptor.getAllValues();
        List<String> expectedConstrains = AssertData.getOpenmucTableConstraints();

        for (int i = 0; i < expectedConstrains.size(); i++) {
            assertEquals(expectedConstrains.get(i), returnedBuilder.get(i).toString());
        }

    }
}
