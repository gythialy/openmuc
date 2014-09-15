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

package org.openmuc.framework.driver.dummy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DeviceConnection;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyDriver implements DriverService {

	private final static Logger logger = LoggerFactory.getLogger(DummyDriver.class);

	private final HashMap<Object, DummyListener> listenersByAddress = new HashMap<Object, DummyListener>();

	boolean applianceStateON = false;
	long applianceSwitchedOnTime;
	long applianceSwitchedOffTime;
	long applianceActiveTime;
	long applianceInactiveTime;
	boolean initApplianceState = true;
	double p = Math.random();
	private volatile boolean scanRunning = false;

	protected void activate(ComponentContext context) {
		logger.info("Activating Dummy Driver");
	}

	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating Dummy Driver");
		for (DummyListener listener : listenersByAddress.values()) {
			listener.shutdown();
		}
	}

	@Override
	public DriverInfo getInfo() {
		DriverInfo toReturn = new DriverInfo("dummy", /* id */
		"This is just a dummy driver " + "that returns dummy values " + "and acts as a sink for " + "written values", /* description */
		"/dev/ttyS[0-9]", /* interfaceAddressSyntax */
		"dummy/device/address/[0-9]", /* deviceAddressSyntax */
		"no parameters needed", /* parametersSyntax */
		"dummy/channel/address/[a-z A-Z]", /* channelAddressSyntax */
		"no parameters needed"); /* deviceScanParametersSyntax */
		return toReturn;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		listener.deviceFound(new DeviceScanInfo(null, "dummy/device/address/1", "", "Dummy device 1."));
		try {
			scanRunning = true;
			for (int i = 0; i <= 10; i++) {
				if (!scanRunning) {
					throw new ScanInterruptedException();
				}
				Thread.sleep(500);
				listener.scanProgressUpdate(i * 10);
				if (i == 5) {
					listener.deviceFound(new DeviceScanInfo(null, "dummy/device/address/2", "meaning=101010b",
							"Dummy device 2."));
				}
			}
		} catch (InterruptedException e) {
			throw new ScanInterruptedException(e);
		}
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		logger.debug("interrupting scan");
		scanRunning = false;
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
			throws UnsupportedOperationException, ConnectionException {
		List<ChannelScanInfo> informationList = new ArrayList<ChannelScanInfo>(2);
		informationList.add(new ChannelScanInfo("dummy/channel/address/voltage", "", ValueType.DOUBLE, null));
		informationList.add(new ChannelScanInfo("dummy/channel/address/current", "", ValueType.BYTE_ARRAY, 5));
		return informationList;
	}

	@Override
	public Object connect(String interfaceAddress, String deviceAddress, String settings) throws ConnectionException {
		logger.info("Connecting to interface:" + interfaceAddress + ", device:" + deviceAddress);

		// create you connection object:
		// Object newConnection = new Object();

		return interfaceAddress + "#" + deviceAddress;
	}

	@Override
	public void disconnect(DeviceConnection connection) {
		logger.info("Disconnecting from interface#device:" + (String) connection.getConnectionHandle());
	}

	@Override
	public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
			Object containerListHandle, String samplingGroup) throws UnsupportedOperationException, ConnectionException {

		double value = 0;
		long receiveTime = System.currentTimeMillis();
		for (ChannelRecordContainer channel : containers) {

			if (channel.getChannelAddress().equals("dummy/channel/address/voltage")) {
				// voltage: 227 V - 233 V
				value = 227 + (int) (Math.random() * ((233 - 227) + 1)); /* Generate values between 227 and 233 */
				// voltage: 10 V - 12 V
				// value = Math.random() * (12 - 10) + 10;
				channel.setRecord(new Record(new DoubleValue(value), receiveTime));
			}
			else if (channel.getChannelAddress().equals("dummy/channel/address/current")) {
				// current: 0.3 A - 0,7 A
				value = Math.sin(p) + 2;
				setApplianceState();
				if (applianceStateON) {
					// add 5 A if appliance is on
					value += 3;
				}
				p += 1.0 / 100 % 2 * Math.PI;
			}
			else {
				// a random value between -10 and +10 for any other channel address
				value = Math.random() * (10 - (-10)) + (-10);
			}
			channel.setRecord(new Record(new DoubleValue(value), receiveTime));
		}
		return null;
	}

	@Override
	public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
			RecordsReceivedListener listener) throws UnsupportedOperationException {
		DummyListener listenerThread = listenersByAddress.get(connection);
		if (containers.size() == 0) {
			if (listenerThread != null) {
				listenerThread.shutdown();
			}
		}
		else {
			if (listenerThread == null) {
				listenerThread = new DummyListener(containers, listener);
				listenerThread.start();
				listenersByAddress.put(connection, listenerThread);
			}
			else {
				listenerThread.setNewContainers(containers);
			}
		}
	}

	@Override
	public Object write(DeviceConnection connection, List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		for (ChannelValueContainer valueContainer : containers) {
			valueContainer.setFlag(Flag.VALID);
		}
		return null;
	}

	private void setApplianceState() {

		long currentTime = new Date().getTime();

		if (!applianceStateON) {
			// if appliance off then leave it off for a certain time and then turn it on
			if ((applianceSwitchedOffTime + applianceInactiveTime) < currentTime || initApplianceState) {
				initApplianceState = false;
				applianceActiveTime = (long) (Math.random() * (10000 - 1000) + 1000); // 1s - 10s
				applianceSwitchedOnTime = new Date().getTime();
				applianceStateON = true;
			}
		}
		else {
			// if appliance on then turn it off after a certain time
			if ((applianceSwitchedOnTime + applianceActiveTime) < currentTime) {
				applianceInactiveTime = (long) (Math.random() * (10000 - 1000) + 1000);
				applianceSwitchedOffTime = new Date().getTime();
				applianceStateON = false;
			}
		}
	}
}
