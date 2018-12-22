/*
 * Copyright 2011-18 Fraunhofer ISE
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

package org.openmuc.framework.driver.ehz;

import java.net.URI;
import java.net.URISyntaxException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EhzDriver implements DriverService {

    public static final String ID = "ehz";

    private static Logger logger = LoggerFactory.getLogger(EhzDriver.class);

    private static final String ADDR_IEC = "iec";
    private static final String ADDR_SML = "sml";

    private boolean interruptScan = false;

    private static final DriverInfo info = new DriverInfo(ID,
            // description
            "Driver for IEC 62056-21 and SML.",
            // device address
            "iec://<serial_device> or sml://<serial_device> e.g.: sml:///dev/ttyUSB0 or sml://COM3",
            // parameters
            "N.A.",
            // channel address
            "e.g.: 0100010800FF",
            // device scan settings
            "N.A.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        String[] serialPortNames = SerialPortBuilder.getSerialPortNames();

        double i = 0;
        int progress = 0;
        int numberOfPorts = serialPortNames.length;
        interruptScan = false;

        listener.scanProgressUpdate(progress);

        for (String spName : serialPortNames) {
            logger.trace("Searching for device at {}", spName);
            URI deviceAddress;
            try {
                deviceAddress = checkForIEC(spName);
            } catch (ConnectionException | URISyntaxException e) {
                logger.trace("{} is no IEC 62056-21 device", spName);
                continue;
            }
            addDevice(listener, spName, deviceAddress);

            if (interruptScan) {
                return;
            }

            if (deviceAddress == null) {
                updateProgress(listener, i + 0.5, numberOfPorts);
                deviceAddress = checkForSML(spName, deviceAddress);
            }
            addDevice(listener, spName, deviceAddress);

            if (interruptScan) {
                return;
            }
            updateProgress(listener, ++i, numberOfPorts);
        }
    }

    private void updateProgress(DriverDeviceScanListener listener, double i, int numberOfPorts) {
        int progress = (int) (i * 100) / numberOfPorts;
        listener.scanProgressUpdate(progress);
    }

    private void addDevice(DriverDeviceScanListener listener, String spName, URI deviceAddress) {
        if (deviceAddress != null) {
            listener.deviceFound(new DeviceScanInfo(deviceAddress.toString(), "", ""));
        }
        else {
            logger.info("No ehz device found at {}", spName);
        }
    }

    private URI checkForSML(String spName, URI deviceAddress) {
        GeneralConnection connection;
        try {
            connection = new SmlConnection(spName);
            if (connection.works()) {
                logger.info("Found sml device at {}", spName);
                deviceAddress = new URI(ADDR_SML + "://" + spName);
            }
            connection.disconnect();
        } catch (ConnectionException | URISyntaxException e) {
            logger.trace("{} is no SML device", spName);
        }
        return deviceAddress;
    }

    private URI checkForIEC(String spName) throws ConnectionException, URISyntaxException {
        URI deviceAddress = null;
        GeneralConnection connection = new IecConnection(spName, 2000);
        if (connection.works()) {
            logger.info("Found iec device at {}", spName);
            deviceAddress = new URI(ADDR_IEC + "://" + spName);
        }
        connection.disconnect();

        return deviceAddress;
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        interruptScan = true;
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        logger.trace("Trying to connect to {}", deviceAddress);
        try {
            URI device = new URI(deviceAddress);

            if (device.getScheme().equals(ADDR_IEC)) {
                logger.trace("Connecting to iec device");
                return new IecConnection(device.getPath(), GeneralConnection.TIMEOUT);
            }
            else if (device.getScheme().equals(ADDR_SML)) {
                logger.trace("Connecting to sml device");
                return new SmlConnection(device.getPath());
            }
            throw new ConnectionException("Unrecognized address scheme " + device.getScheme());
        } catch (URISyntaxException e) {
            throw new ConnectionException(e);
        }
    }

}
