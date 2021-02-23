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

package org.openmuc.framework.driver.iec60870;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.iec60870.settings.ChannelAddress;
import org.openmuc.framework.driver.iec60870.settings.DeviceAddress;
import org.openmuc.framework.driver.iec60870.settings.DeviceScanSettings;
import org.openmuc.framework.driver.iec60870.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;

@Component
public final class Iec60870Driver implements DriverService {

    private static final String DRIVER_ID = "iec60870";
    private static final String DESCRIPTION = "This driver can be used to access IEC 60870-104 devices";

    private static final DriverInfo info = new DriverInfo(DRIVER_ID, DESCRIPTION,
            DeviceAddress.syntax(DeviceAddress.class), DeviceSettings.syntax(DeviceSettings.class),
            ChannelAddress.syntax(ChannelAddress.class), DeviceScanSettings.syntax(DeviceScanSettings.class));

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        return new Iec60870Connection(new DeviceAddress(deviceAddress), new DeviceSettings(settings), DRIVER_ID);
    }

}
