/*
 * Copyright 2011-16 Fraunhofer ISE
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

package org.openmuc.framework.config;

import java.io.FileNotFoundException;
import java.util.List;

import org.openmuc.framework.dataaccess.DeviceState;

public interface ConfigService {

    void lock();

    boolean tryLock();

    void unlock();

    /**
     * Returns a <i>clone</i> of the current configuration file.
     * 
     * @return clone of the configuration file.
     * 
     * @see #setConfig(RootConfig)
     */
    RootConfig getConfig();

    RootConfig getConfig(ConfigChangeListener listener);

    void stopListeningForConfigChange(ConfigChangeListener listener);

    void setConfig(RootConfig config);

    void writeConfigToFile() throws ConfigWriteException;

    void reloadConfigFromFile() throws FileNotFoundException, ParseException;

    RootConfig getEmptyConfig();

    List<DeviceScanInfo> scanForDevices(String driverId, String settings) throws DriverNotAvailableException,
            UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException;

    void scanForDevices(String driverId, String settings, DeviceScanListener scanListener)
            throws DriverNotAvailableException;

    void interruptDeviceScan(String driverId) throws DriverNotAvailableException, UnsupportedOperationException;

    List<ChannelScanInfo> scanForChannels(String deviceId, String settings)
            throws DriverNotAvailableException, UnsupportedOperationException, ArgumentSyntaxException, ScanException;

    DriverInfo getDriverInfo(String driverId) throws DriverNotAvailableException;

    List<String> getIdsOfRunningDrivers();

    DeviceState getDeviceState(String deviceId);

}
