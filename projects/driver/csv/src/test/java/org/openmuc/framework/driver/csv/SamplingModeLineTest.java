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
package org.openmuc.framework.driver.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.utils.CsvChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;

public class SamplingModeLineTest {

    private static final String DEVICE_ADDRESS = System.getProperty("user.dir") + "/src/test/resources/test_data.csv";

    private static final int INDEX_HHMMSS = 0;
    private static final int INDEX_POWER = 1;

    private List<ChannelRecordContainer> containers;

    @BeforeEach
    public void setup() {

        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
        containers = new ArrayList<>();
        containers.add(INDEX_HHMMSS, new CsvChannelRecordContainer("hhmmss"));
        containers.add(INDEX_POWER, new CsvChannelRecordContainer("power_grid"));
    }

    /**
     * Reads 3 lines of the csv file. Test checks if the correct value of hhmmss is returned
     *
     * @throws ConnectionException
     * @throws ArgumentSyntaxException
     */
    @Test
    public void testLineModeWithoutRewind() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=line";
        CsvDeviceConnection connection = new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings);
        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        read(connection, containers);
        assertEquals(10.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        read(connection, containers);
        assertEquals(15.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        read(connection, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        // no rewind last line is returned
        read(connection, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    @Test
    public void testLineModeWitRewind() throws ConnectionException, ArgumentSyntaxException {

        String deviceSettings = "samplingmode=line;rewind=true";
        CsvDeviceConnection connection = new CsvDeviceConnection(DEVICE_ADDRESS, deviceSettings);

        System.out.println(String.format("%10s, %10s", "hhmmss", "power_grid"));

        read(connection, containers);
        assertEquals(10.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        read(connection, containers);
        assertEquals(15.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        read(connection, containers);
        assertEquals(20.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

        // with rewind first line is returned
        read(connection, containers);
        assertEquals(10.0, containers.get(INDEX_HHMMSS).getRecord().getValue().asDouble());

    }

    private void read(CsvDeviceConnection connection, List<ChannelRecordContainer> containers)
            throws UnsupportedOperationException, ConnectionException {
        connection.read(containers, null, null);
        System.out.println(String.format("%10s, %10s", containers.get(INDEX_HHMMSS).getRecord().getValue(),
                containers.get(INDEX_POWER).getRecord().getValue()));
    }

}
