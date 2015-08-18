/*
 * Copyright 2011-15 Fraunhofer ISE
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

	public void lock();

	public boolean tryLock();

	public void unlock();

	public RootConfig getConfig();

	public RootConfig getConfig(ConfigChangeListener listener);

	public void stopListeningForConfigChange(ConfigChangeListener listener);

	public void setConfig(RootConfig config);

	public void writeConfigToFile() throws ConfigWriteException;

	public void reloadConfigFromFile() throws FileNotFoundException, ParseException;

	public RootConfig getEmptyConfig();

	public List<DeviceScanInfo> scanForDevices(String driverId, String settings) throws DriverNotAvailableException,
			UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException;

	public void scanForDevices(String driverId, String settings, DeviceScanListener scanListener)
			throws DriverNotAvailableException;

	public void interruptDeviceScan(String driverId) throws DriverNotAvailableException, UnsupportedOperationException;

	public List<ChannelScanInfo> scanForChannels(String deviceId, String settings) throws DriverNotAvailableException,
			UnsupportedOperationException, ArgumentSyntaxException, ScanException;

	public DriverInfo getDriverInfo(String driverId) throws DriverNotAvailableException;

	public List<String> getIdsOfRunningDrivers();

	public DeviceState getDeviceState(String deviceId);

}
