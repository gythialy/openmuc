/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.driver.dummy;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyDriver implements DriverService {

	private final static Logger logger = LoggerFactory.getLogger(DummyDriver.class);

	private final static DriverInfo info = new DriverInfo("dummy", // id
			// description
			"This is just a dummy driver that returns dummy values and acts as a sink for written values",
			// device address
			"dummy/device/address/[0-9]",
			// parameters
			"N.A.",
			// channel address
			"dummy/channel/address/[a-z A-Z]",
			// device scan settings
			"N.A.");

	private volatile boolean scanRunning = false;

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		listener.deviceFound(new DeviceScanInfo("dummy/device/address/1", "", "Dummy device 1."));
		try {
			scanRunning = true;
			for (int i = 0; i <= 10; i++) {
				if (!scanRunning) {
					throw new ScanInterruptedException();
				}
				Thread.sleep(500);
				listener.scanProgressUpdate(i * 10);
				if (i == 5) {
					listener.deviceFound(new DeviceScanInfo("dummy/device/address/2", "meaning=101010b",
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
	public Connection connect(String deviceAddress, String settings) throws ConnectionException {

		logger.info("Connecting to device: " + deviceAddress);

		return new DummyConnection(deviceAddress);
	}

}
