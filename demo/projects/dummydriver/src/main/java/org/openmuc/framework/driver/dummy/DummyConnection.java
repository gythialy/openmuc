/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.driver.dummy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyConnection implements Connection {

	private final static Logger logger = LoggerFactory.getLogger(DummyConnection.class);

	private final String deviceAddress;

	DummyListener listenerThread;

	boolean applianceStateON = false;
	long applianceSwitchedOnTime;
	long applianceSwitchedOffTime;
	long applianceActiveTime;
	long applianceInactiveTime;
	boolean initApplianceState = true;
	double p = Math.random();

	public DummyConnection(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException,
			ConnectionException {
		List<ChannelScanInfo> informationList = new ArrayList<ChannelScanInfo>(2);
		informationList.add(new ChannelScanInfo("dummy/channel/address/voltage", "", ValueType.DOUBLE, null));
		informationList.add(new ChannelScanInfo("dummy/channel/address/current", "", ValueType.BYTE_ARRAY, 5));
		return informationList;
	}

	@Override
	public void disconnect() {
		logger.info("Disconnecting from device: " + deviceAddress);
		if (listenerThread != null) {
			listenerThread.shutdown();
		}
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {

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
	public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
			throws UnsupportedOperationException {
		if (containers.size() == 0) {
			if (listenerThread != null) {
				listenerThread.shutdown();
			}
		}
		else {
			if (listenerThread == null) {
				listenerThread = new DummyListener(containers, listener);
				listenerThread.start();
			}
			else {
				listenerThread.setNewContainers(containers);
			}
		}
	}

	@Override
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
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
