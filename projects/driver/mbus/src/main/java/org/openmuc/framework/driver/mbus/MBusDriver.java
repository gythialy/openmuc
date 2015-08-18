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
package org.openmuc.framework.driver.mbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jmbus.MBusSap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MBusDriver implements DriverService {
	private final static Logger logger = LoggerFactory.getLogger(MBusDriver.class);

	private final Map<String, MBusSerialInterface> interfaces = new HashMap<String, MBusSerialInterface>();

	private final static DriverInfo info = new DriverInfo("mbus", // id
			// description
			"M-Bus (wired) is a protocol to read out meters.",
			// device address
			"Synopsis: <serial_port>:<mbus_address>\nExample for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)\nExample for <mbus_address>: 5 for primary address 5",
			// settings
			"Synopsis: [<baud_rate>]\nThe default baud rate is 2400",
			// channel address
			"Synopsis: [X]<dib>:<vib>\nThe DIB and VIB fields in hexadecimal form seperated by a collon. If the channel address starts with an X then the specific data record will be selected for readout before reading it.",
			// device scan settings
			"Synopsis: <serial_port> [baud_rate]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)");

	private boolean interruptScan;

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		interruptScan = false;

		String[] args = settings.split("\\s+");
		if (args.length < 1 || args.length > 2) {
			throw new ArgumentSyntaxException(
					"Less than one or more than two arguments in the settings are not allowed.");
		}

		int baudRate = 2400;
		if (args.length == 2) {
			try {
				baudRate = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new ArgumentSyntaxException("<braud_rate> is not an integer");
			}
		}

		MBusSap mBusSap;
		if (!interfaces.containsKey(args[0])) {
			mBusSap = new MBusSap(args[0], baudRate);
			try {
				mBusSap.open();
			} catch (IllegalArgumentException e) {
				throw new ArgumentSyntaxException();
			} catch (IOException e) {
				throw new ScanException(e);
			}
		}
		else {
			mBusSap = interfaces.get(args[0]).getMBusSap();
		}

		mBusSap.setTimeout(1000);

		try {
			for (int i = 0; i <= 250; i++) {

				if (interruptScan) {
					throw new ScanInterruptedException();
				}

				if (i % 5 == 0) {
					listener.scanProgressUpdate(i * 100 / 250);
				}
				logger.debug("scanning for meter with primary address {}", i);
				try {
					mBusSap.read(i);
				} catch (TimeoutException e) {
					continue;
				} catch (IOException e) {
					throw new ScanException(e);
				}
				listener.deviceFound(new DeviceScanInfo(args[0] + ":" + i, "", ""));
				logger.debug("found meter: {}", i);
			}
		} finally {
			mBusSap.close();
		}

	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		interruptScan = true;

	}

	@Override
	public Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException,
			ConnectionException {

		String[] deviceAddressTokens = deviceAddress.trim().split(":");

		if (deviceAddressTokens.length != 2) {
			throw new ArgumentSyntaxException("The device address does not consist of two parameters.");
		}

		String serialPortName = deviceAddressTokens[0];
		Integer mBusAddress = Integer.decode(deviceAddressTokens[1]);
		if (mBusAddress == null) {
			throw new ArgumentSyntaxException("Settings: mBusAddress (" + deviceAddressTokens[1] + ") is not a int");
		}

		MBusSerialInterface serialInterface;

		synchronized (this) {

			synchronized (interfaces) {

				serialInterface = interfaces.get(serialPortName);

				if (serialInterface == null) {

					int baudrate = 2400;

					if (!settings.isEmpty()) {
						try {
							baudrate = Integer.parseInt(settings);
						} catch (NumberFormatException e) {
							throw new ArgumentSyntaxException("Settings: baudrate is not a parsable number");
						}
					}

					MBusSap mBusSap = new MBusSap(serialPortName, baudrate);

					try {
						mBusSap.open();
					} catch (IOException e1) {
						throw new ConnectionException("Unable to bind local interface: " + deviceAddressTokens[0]);
					}

					serialInterface = new MBusSerialInterface(mBusSap, serialPortName, interfaces);

				}
			}

			synchronized (serialInterface) {
				try {
					serialInterface.getMBusSap().read(mBusAddress);
				} catch (IOException e) {
					serialInterface.close();
					throw new ConnectionException(e);
				} catch (TimeoutException e) {
					if (serialInterface.getDeviceCounter() == 0) {
						serialInterface.close();
					}
					throw new ConnectionException(e);
				}

				serialInterface.increaseConnectionCounter();

			}

		}

		return new MBusConnection(serialInterface, mBusAddress);

	}

}
