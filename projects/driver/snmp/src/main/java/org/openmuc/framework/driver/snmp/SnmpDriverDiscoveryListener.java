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
package org.openmuc.framework.driver.snmp;

import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.driver.snmp.implementation.SnmpDiscoveryEvent;
import org.openmuc.framework.driver.snmp.implementation.SnmpDiscoveryListener;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;

/**
 * In scanner we need to notify a listener which is given in arguments and also we have to create another listener in
 * order to listen to SNMP scanner in SnmpDevice. So we notify given listener in callback method of SnmpDevice listener
 */
public class SnmpDriverDiscoveryListener implements SnmpDiscoveryListener {

    private final DriverDeviceScanListener scannerListener;

    public SnmpDriverDiscoveryListener(DriverDeviceScanListener listener) {
        scannerListener = listener;
    }

    @Override
    public void onNewDeviceFound(SnmpDiscoveryEvent e) {
        DeviceScanInfo newDevice = new DeviceScanInfo(e.getDeviceAddress().toString(), null, e.getDescription());
        scannerListener.deviceFound(newDevice);
    }

}
