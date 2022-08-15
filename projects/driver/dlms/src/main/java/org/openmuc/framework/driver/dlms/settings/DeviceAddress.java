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
package org.openmuc.framework.driver.dlms.settings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceAddress extends GenericSetting {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAddress.class);

    @Option(value = "t", mandatory = true, range = "serial|tcp")
    private String connectionType = null;

    @Option(value = "h", range = "inet_address")
    private InetAddress hostAddress = null;

    @Option(value = "p", range = "int")
    private int port = 4059;

    @Option(value = "hdlc", range = "boolean")
    private boolean useHdlc = false;

    @Option(value = "sp")
    private String serialPort = "";

    @Option(value = "bd", range = "int")
    private int baudrate = 9600;

    @Option("d")
    private long baudRateChangeDelay = 0;

    @Option("eh")
    private boolean enableBaudRateHandshake = false;

    @Option("iec")
    private String iec21Address = "";

    @Option("pd")
    private int physicalDeviceAddress = 0;

    public DeviceAddress(String deviceAddress) throws ArgumentSyntaxException {
        int addressLength = parseFields(deviceAddress);
        if (connectionType.equalsIgnoreCase("tcp")) {
            if (addressLength == 1) {
                logger.info(MessageFormat.format(
                        "No device address set in configuration, default values will be used: host address = localhost; port = {0}",
                        port));
            }
            if (hostAddress == null) {
                try {
                    hostAddress = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    throw new ArgumentSyntaxException("Could not set default host address: localhost");
                }
            }
        }
        else if (connectionType.equalsIgnoreCase("serial")) {
            if (serialPort.isEmpty()) {
                throw new ArgumentSyntaxException("No serial port given. e.g. Linux: /dev/ttyUSB0 or Windows: COM12 ");
            }
        }
        else {
            throw new ArgumentSyntaxException(
                    "Only 'tcp' and 'serial' are supported connection types, given is: " + connectionType);
        }
    }

    public String getConnectionType() {
        return connectionType;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }

    public boolean useHdlc() {
        return useHdlc;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public long getBaudRateChangeDelay() {
        return baudRateChangeDelay;
    }

    public boolean enableBaudRateHandshake() {
        return enableBaudRateHandshake;
    }

    public String getIec21Address() {
        return iec21Address;
    }

    public int getPhysicalDeviceAddress() {
        return physicalDeviceAddress;
    }

}
