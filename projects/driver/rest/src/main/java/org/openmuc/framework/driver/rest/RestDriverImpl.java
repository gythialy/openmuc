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
package org.openmuc.framework.driver.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;

import de.hsoffenburg.ssdp.service.SSDPServiceDescription;
import de.hsoffenburg.ssdp.service.SSDPServiceHandlerInterface;
import de.hsoffenburg.ssdp.service.listener.SSDPDiscoveryResponseListener;

public class RestDriverImpl implements DriverService {

	// private final static Logger logger = LoggerFactory.getLogger(RestDriverImpl.class);

	private final List<DeviceScanInfo> deviceList;
	private SSDPDiscoveryResponseListener callback;
	private final String searchTarget = "openmuc:rest";
	private final Map<String, SSDPServiceDescription> providers;

	private SSDPServiceHandlerInterface ssdpHandler;

	private final static int timeout = 10000;

	private final static DriverInfo info = new DriverInfo("rest", // id
			// description
			"Driver to connect this OpenMUC instance with another, remote OpenMUC instance with rest.",
			// device address
			"uuid:ABCDEFG:openmuc:restws:1",
			// settings
			"username:password",
			// channel address
			"/rest/channels/channelid",
			// device scan settings
			"N.A.");

	public RestDriverImpl() {

		deviceList = new ArrayList<DeviceScanInfo>();
		providers = new HashMap<String, SSDPServiceDescription>();
	}

	private void initialize() {

		callback = new SSDPDiscoveryResponseListener() {

			@Override
			public synchronized void serviceProvidedBy(SSDPServiceDescription provider) {

				DeviceScanInfo device = new DeviceScanInfo(provider.getUsn(), "", "OpenMUC at: "
						+ provider.getLocation());

				for (DeviceScanInfo knownDevice : deviceList) {
					if (knownDevice.getDeviceAddress().equals(provider.getUsn())) {
						/* skip already registered device. */
						return;
					}
				}
				deviceList.add(device);
				providers.put(provider.getUsn(), provider);
			}

			@Override
			public String getSearchTarget() {

				return searchTarget;
			}

			@Override
			public boolean discoverServicesOnLocalhost() {

				return false;
			}
		};

		ssdpHandler.discoverService(callback);
	}

	@Override
	public Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException,
			ConnectionException {

		String FIX_IP = "IP_";
		RestConnection connection;

		if (settings == null || settings.isEmpty() || settings.trim().isEmpty() || !settings.contains(":")) {
			throw new ArgumentSyntaxException("Invalid User Credentials provided in settings: " + settings
					+ ". Expected Format: username:password");
		}
		if (deviceAddress.startsWith(FIX_IP)) {
			deviceAddress = deviceAddress.replaceFirst(FIX_IP, "");
			connection = new RestConnection(deviceAddress, settings, timeout);
			connection.connect();
			return connection;
		}
		else if (providers.get(deviceAddress) != null) {
			connection = new RestConnection(providers.get(deviceAddress).getLocation(), settings, timeout);
			connection.connect();
			return connection;
		}
		else {
			throw new ConnectionException("Device with USN: " + deviceAddress + " not detected (yet) on the network.");
		}

	}

	@Override
	public DriverInfo getInfo() {

		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException {

		ssdpHandler.discoverService(callback);
		try {
			for (int i = 0; i < 10; i++) {
				Thread.sleep(1000);
				listener.scanProgressUpdate(i * 10);
				for (DeviceScanInfo info : deviceList) {
					listener.deviceFound(info);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {

		/* not supported */
	}

	public void setSSDPHandler(SSDPServiceHandlerInterface ssdpHandler) {

		this.ssdpHandler = ssdpHandler;
		initialize();
	}

	public void unsetSSDPHandler(SSDPServiceHandlerInterface ssdpHandler) {

		this.ssdpHandler = null;
	}
}
