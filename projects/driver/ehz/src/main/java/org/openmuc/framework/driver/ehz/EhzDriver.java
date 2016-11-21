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

package org.openmuc.framework.driver.ehz;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;

@Component
public class EhzDriver implements DriverService {

    private static Logger logger = LoggerFactory.getLogger(EhzDriver.class);

    private static final String ADDR_IEC = "iec";
    private static final String ADDR_SML = "sml";

    private final static DriverInfo info = new DriverInfo("ehz", // id
            // description
            "Driver for IEC 62056-21 and SML.",
            // device address
            "N.A.",
            // parameters
            "N.A.",
            // channel address
            "N.A.",
            // device scan settings
            "N.A.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            String serialPort = port.getName();
            logger.trace("searching for device at " + serialPort);
            URI deviceAddress = null;
            GeneralConnection connection = null;
            if (deviceAddress == null) {
                try {
                    connection = new IecConnection(serialPort, 2000);
                    if (connection.isWorking()) {
                        logger.info("found iec device at " + serialPort);
                        deviceAddress = new URI(ADDR_IEC + "://" + serialPort);
                    }
                    connection.close();
                } catch (Exception e) {
                    logger.trace(serialPort + " is no iec device");
                }
            }
            if (deviceAddress == null) {
                try {
                    connection = new SmlConnection(serialPort);
                    if (connection.isWorking()) {
                        logger.info("found sml device at " + serialPort);
                        deviceAddress = new URI(ADDR_SML + "://" + serialPort);
                    }
                    connection.close();
                } catch (Exception e) {
                    logger.trace(serialPort + " is no sml device");
                }
            }
            if (deviceAddress != null) {
                listener.deviceFound(new DeviceScanInfo(deviceAddress.toString(), "", ""));
            }
            else {
                logger.info("no ehz device found at " + serialPort);
            }
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        logger.trace("trying to connect to " + deviceAddress);
        try {
            URI device = new URI(deviceAddress);

            if (device.getScheme().equals(ADDR_IEC)) {
                logger.trace("connecting to iec device");
                return new IecConnection(device.getPath(), GeneralConnection.timeout);
            }
            else if (device.getScheme().equals(ADDR_SML)) {
                logger.trace("connecting to sml device");
                return new SmlConnection(device.getPath());
            }
            throw new ConnectionException("unrecognized address scheme " + device.getScheme());
        } catch (URISyntaxException e) {
            throw new ConnectionException(e);
        }
    }

}
