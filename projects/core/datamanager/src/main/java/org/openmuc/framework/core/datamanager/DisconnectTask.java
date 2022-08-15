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

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.driver.spi.DriverService;

public final class DisconnectTask extends DeviceTask {

    public DisconnectTask(DriverService driver, Device device, DataManager dataManager) {
        this.driver = driver;
        this.device = device;
        this.dataManager = dataManager;
    }

    @Override
    public void run() {

        device.connection.disconnect();

        synchronized (dataManager.disconnectedDevices) {
            dataManager.disconnectedDevices.add(device);
        }
        dataManager.interrupt();

    }

    @Override
    public DeviceTaskType getType() {
        return DeviceTaskType.DISCONNECT;
    }

}
