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
package org.openmuc.framework.driver.csv.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.CsvDeviceConnection;
import org.openmuc.framework.driver.csv.test.utils.CsvChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, CsvDeviceConnection.class })
public class SamplingModeHhmmssTest {

    private static final String DEVICE_ADDRESS = System.getProperty("user.dir") + "/src/test/resources/test_data.csv";

    private static final int INDEX_HHMMSS = 0;
    private static final int INDEX_POWER = 1;

    // timestamps of csv file
    private static final long TIMESTAMP_BEFORE_1LINE = 1436306405000L; // not part of csv; 00:00:05
    private static final long TIMESTAMP_1LINE = 1436306410000L; // 00:00:10
    private static final long TIMESTAMP_2LINE = 1436306415000L; // 00:00:15
    private static final long TIMESTAMP_3LINE = 1436306420000L; // 00:00:20
    private static final long TIMESTAMP_AFTER_3LINE = 1436306425000L; // not part of csv; 00:00:25

    private List<ChannelRecordContainer> containers;

    @Before
    public void before() {

        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
        containers = new ArrayList<>();
        containers.add(INDEX_HHMMSS, new CsvChannelRecordContainer("hhmmss"));
        containers.add(INDEX_POWER, new CsvChannelRecordContainer("power_grid"));
    }

    @Test
    public void testNormal() throws Exception {

        String deviceSettings = "samplingmode=hhmmss";
        CsvDeviceConnection connectionSpy = PowerMockito.spy(new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings));
        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_1LINE);
        read(connectionSpy, containers);
        assertEquals(10.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_2LINE);
        read(connectionSpy, containers);
        assertEquals(15.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_3LINE);
        read(connectionSpy, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_AFTER_3LINE);
        read(connectionSpy, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    @Test
    public void testNewDayRewind() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=hhmmss;rewind=true";

        CsvDeviceConnection connectionSpy = PowerMockito.spy(new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings));

        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_3LINE);
        read(connectionSpy, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        // rewind should performed so first line can be read
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_1LINE);
        read(connectionSpy, containers);
        assertEquals(10.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    @Test
    public void testNewDayWithoutRewind() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=hhmmss";
        CsvDeviceConnection connectionSpy = PowerMockito.spy(new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings));
        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_3LINE);
        read(connectionSpy, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        // no rewind, causes timetravel exception resulting in NaN
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_1LINE);
        read(connectionSpy, containers);
        assertEquals(Double.NaN, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    @Test
    public void testBeforeAvailableData() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=hhmmss";
        CsvDeviceConnection connectionSpy = PowerMockito.spy(new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings));
        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_BEFORE_1LINE);
        read(connectionSpy, containers);
        assertEquals(Double.NaN, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    @Test
    public void testAfterAvailableData() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=hhmmss";
        CsvDeviceConnection connectionSpy = PowerMockito.spy(new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings));
        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        // last line of file should be returned
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(TIMESTAMP_AFTER_3LINE);
        read(connectionSpy, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    private void read(CsvDeviceConnection connection, List<ChannelRecordContainer> containers)
            throws UnsupportedOperationException, ConnectionException {
        connection.read(containers, null, null);
        System.out.println(String.format("%10s, %10s", containers.get(INDEX_HHMMSS).getRecord().getValue(),
                containers.get(INDEX_POWER).getRecord().getValue()));
    }

}
