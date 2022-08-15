/*
 * Copyright 2011-2022 Fraunhofer ISE
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

//import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jmbus.SecondaryAddress;
import org.osgi.service.component.annotations.Component;

@Component
public class Driver implements DriverService {

    private static final DriverInfo info = new DriverInfo("wmbus", // id
            // description
            "Wireless M-Bus is a protocol to read out meters and sensors.",
            // device address
            "Synopsis: <serial_port>:<secondary_address> / TCP:<host_address>:<port>:<secondary_address>"
                    + "Example for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)  <secondary_address> as a hex string.  "
                    + "Example for <host_address>:<port> 192.168.8.15:2018",
            // settings
            "Synopsis: <transceiver> <mode> [<key>]\n Transceiver could be 'amber', 'imst' and 'rc'for RadioCraft. Possible modes are T or S. ",
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

        String connectionPort = deviceAddressTokens[0];
        String secondaryAddressAsString = "";
        String host = "";
        int port = 0;
        boolean isTCP = false;
        if (connectionPort.equalsIgnoreCase("TCP")) {
            isTCP = true;
            host = deviceAddressTokens[0];
            try {
                port = Integer.decode(deviceAddressTokens[1]);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("TCP port is not a number.");
            }
            secondaryAddressAsString = deviceAddressTokens[2].toLowerCase();
        }
        else {
            secondaryAddressAsString = deviceAddressTokens[1].toLowerCase();
        }

        SecondaryAddress secondaryAddress = null;
        try {
            secondaryAddress = parseSecondaryAddress(secondaryAddressAsString);
        } catch (DecoderException e) {
            e.printStackTrace();
        }

        String[] settingsTokens = splitSettingsToken(settings);

        String transceiverString = settingsTokens[0];
        String modeString = settingsTokens[1];
        String keyString = null;
        if (settingsTokens.length == 3) {
            keyString = settingsTokens[2];
        }

        WMBusInterface wmBusInterface;

        if (isTCP) {
            synchronized (this) {
                wmBusInterface = WMBusInterface.getTCPInstance(host, port, transceiverString, modeString);
                try {
                    return wmBusInterface.connect(secondaryAddress, keyString);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            synchronized (this) {
                wmBusInterface = WMBusInterface.getSerialInstance(connectionPort, transceiverString, modeString);
                try {
                    return wmBusInterface.connect(secondaryAddress, keyString);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String[] splitSettingsToken(String settings) throws ArgumentSyntaxException {
        String[] settingsTokens = settings.trim().toLowerCase().split(" ");

        if (settingsTokens.length < 2 || settingsTokens.length > 3) {
            throw new ArgumentSyntaxException("The device's settings parameters does not contain 2 or 3 parameters.");
        }
        return settingsTokens;
    }

    private SecondaryAddress parseSecondaryAddress(String secondaryAddressAsString)
            throws ArgumentSyntaxException, DecoderException {
        SecondaryAddress secondaryAddress;
        try {
            byte[] bytes = Hex.decodeHex(secondaryAddressAsString);
            secondaryAddress = SecondaryAddress.newFromWMBusHeader(bytes, 0);
        } catch (NumberFormatException e) {
            throw new ArgumentSyntaxException(
                    "The SecondaryAddress: " + secondaryAddressAsString + " could not be converted to a byte array.");
        }
        return secondaryAddress;
    }

}
