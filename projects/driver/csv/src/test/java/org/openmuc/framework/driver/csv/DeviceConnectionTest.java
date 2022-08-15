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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.spi.ConnectionException;

public class DeviceConnectionTest {

    private static final String DIR = System.getProperty("user.dir") + "/src/test/resources/";

    @Test
    public void testCsvWithHhmmss() throws ConnectionException, ArgumentSyntaxException {
        String deviceAddress = DIR + "test_data.csv";
        String deviceSettings = "samplingmode=hhmmss";
        new CsvDeviceConnection(deviceAddress, deviceSettings);
    }

    // expect exception since csv file has no hhmmss column
    @Test
    public void testCvsWithoutHhmmss() throws ConnectionException, ArgumentSyntaxException {
        String deviceAddress = DIR + "test_data_no_hhmmss.csv";
        String deviceSettings = "samplingmode=hhmmss";
        Assertions.assertThrows(ArgumentSyntaxException.class,
                () -> new CsvDeviceConnection(deviceAddress, deviceSettings));
    }

    @Test
    public void testCvsWithUnixtimestamp() throws ConnectionException, ArgumentSyntaxException {
        String deviceAddress = DIR + "test_data.csv";
        String deviceSettings = "samplingmode=unixtimestamp";
        new CsvDeviceConnection(deviceAddress, deviceSettings);
    }

    // expect exception since csv file has no unixtimestamp column
    @Test
    public void testCvsWithoutUnixtimestamp() throws ConnectionException, ArgumentSyntaxException {
        String deviceAddress = DIR + "test_data_no_unixtimestamp.csv";
        String deviceSettings = "samplingmode=unixtimestamp";
        Assertions.assertThrows(ArgumentSyntaxException.class,
                () -> new CsvDeviceConnection(deviceAddress, deviceSettings));
    }

}
