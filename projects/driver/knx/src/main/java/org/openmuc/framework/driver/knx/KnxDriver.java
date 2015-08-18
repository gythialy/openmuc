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
package org.openmuc.framework.driver.knx;

import gnu.io.CommPortIdentifier;

import java.io.IOException;
import java.util.Enumeration;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.log.LogManager;

public class KnxDriver implements DriverService {

	public static final String ADDRESS_SCHEME_KNXIP = "knxip";
	public static final String ADDRESS_SCHEME_RC1180 = "knxrc1180";
	private static Logger logger = LoggerFactory.getLogger(KnxDriver.class);

	final static int timeout = 10000;

	private final static DriverInfo info = new DriverInfo("knx", "Driver to read and write KNX groupaddresses", "?",
			"?", "?", "?");

	protected void activate(ComponentContext context) {
		if (logger.isDebugEnabled()) {
			LogManager.getManager().addWriter("", new KnxLogWriter()); // Add calimero logger
		}
	}

	protected void deactivate(ComponentContext context) {
		logger.debug("Deactivated KNX Driver");
	}

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		String[] args = settings.split("\\s+");

		if (args.length > 0) {
			boolean natAware = false;
			boolean mcastResponse = false;
			if (args.length > 1) {
				logger.debug("Applying settings: " + args[1]);
				natAware = args[1].contains("nat");
				mcastResponse = args[1].contains("mcast");
			}
			KnxIpDiscover discover;
			try {
				discover = new KnxIpDiscover(args[0], natAware, mcastResponse);
				discover.startSearch(0, listener);
			} catch (IOException e) {
				throw new ScanException(e);
			}
		}
		else {
			try {
				Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
				while (ports.hasMoreElements()) {
					CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
					String name = port.getName();
					String description = "settings could be: address=1.1.1;serialNumber=0123456789AB";
					listener.deviceFound(new DeviceScanInfo(ADDRESS_SCHEME_RC1180 + "://" + name, "", description));
				}
			} catch (Exception e) {
				logger.warn("serial communication failed");
			}
		}
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();

	}

	@Override
	public Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException,
			ConnectionException {
		return new KnxConnection(deviceAddress, settings, timeout);
	}

}
