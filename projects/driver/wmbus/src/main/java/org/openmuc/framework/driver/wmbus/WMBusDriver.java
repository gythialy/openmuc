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
package org.openmuc.framework.driver.wmbus;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WMBusDriver implements DriverService {
    private final static Logger logger = LoggerFactory.getLogger(WMBusDriver.class);

    private final static DriverInfo info = new DriverInfo("wmbus", // id
            // description
            "Wireless M-Bus is a protocol to read out meters and sensors.",
            // device address
            "Synopsis: <serial_port>:<secondary_address>\nExample for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)\n<mbus_id> as a hex string",
            // settings
            "Synopsis: <transceiver> <mode> [<key>]\n",
            // channel address
            "Synopsis: <dib>:<vib>",
            // device scan settings
            "N.A.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        String[] deviceAddressTokens = deviceAddress.trim().split(":");

        if (deviceAddressTokens.length != 2) {
            throw new ArgumentSyntaxException("The device address does not consist of two parameters.");
        }

        String serialPortName = deviceAddressTokens[0];
        String secondaryAddressAsString = deviceAddressTokens[1].toLowerCase();
        SecondaryAddress secondaryAddress;
        try {
            secondaryAddress = SecondaryAddress
                    .getFromWMBusLinkLayerHeader(HexConverter.fromShortHexString(secondaryAddressAsString), 0);
        } catch (NumberFormatException e) {
            throw new ArgumentSyntaxException(
                    "The SecondaryAddress: " + secondaryAddressAsString + " could not be converted to a byte array.");
        }

        String[] settingsTokens = settings.trim().toLowerCase().split(" ");

        if (settingsTokens.length < 2 || settingsTokens.length > 3) {
            throw new ArgumentSyntaxException("The device's settings parameters does not contain 2 or 3 parameters.");
        }

        String transceiverString = settingsTokens[0];
        String modeString = settingsTokens[1];
        String keyString = null;
        if (settingsTokens.length == 3) {
            keyString = settingsTokens[2];
        }

        WMBusSerialInterface serialInterface;

        synchronized (this) {
            serialInterface = WMBusSerialInterface.getInstance(serialPortName, transceiverString, modeString);
            return serialInterface.connect(secondaryAddress, keyString);
        }

    }

}
