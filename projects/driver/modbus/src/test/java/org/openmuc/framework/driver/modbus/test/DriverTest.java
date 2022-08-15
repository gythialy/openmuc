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
package org.openmuc.framework.driver.modbus.test;

import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.driver.modbus.ModbusDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverTest {

    private static final Logger logger = LoggerFactory.getLogger(DriverTest.class);

    @Test
    public void printDriverInfo() {
        ModbusDriver driver = new ModbusDriver();
        DriverInfo info = driver.getInfo();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Driver Id = " + info.getId() + "\n");
        sb.append("Description = " + info.getDescription() + "\n");
        sb.append("DeviceAddressSyntax = " + info.getDeviceAddressSyntax() + "\n");
        sb.append("SettingsSyntax = " + info.getSettingsSyntax() + "\n");
        sb.append("ChannelAddressSyntax = " + info.getChannelAddressSyntax() + "\n");
        sb.append("DeviceScanSettingsSyntax = " + info.getDeviceScanSettingsSyntax() + "\n");
        logger.info(sb.toString());

    }

}
