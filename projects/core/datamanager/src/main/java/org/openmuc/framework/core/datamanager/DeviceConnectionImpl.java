/*
 * Copyright 2011-14 Fraunhofer ISE
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

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.driver.spi.DeviceConnection;

public final class DeviceConnectionImpl implements DeviceConnection {

    private final String interfaceAddress;
    private final String deviceAddress;
    private final String settings;
    private final Object connectionHandle;
    protected final Device device;

    public DeviceConnectionImpl(String interfaceAddress, String deviceAddress, String settings,
                                Object connectionHandle, Device device) {
        this.interfaceAddress = interfaceAddress;
        this.deviceAddress = deviceAddress;
        this.settings = settings;
        this.connectionHandle = connectionHandle;
        this.device = device;
    }

    @Override
    public String getInterfaceAddress() {
        return interfaceAddress;
    }

    @Override
    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String getSettings() {
        return settings;
    }

    @Override
    public Object getConnectionHandle() {
        return connectionHandle;
    }

}
