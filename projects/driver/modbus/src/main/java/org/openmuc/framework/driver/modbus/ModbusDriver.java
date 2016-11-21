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

package org.openmuc.framework.driver.modbus;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.modbus.rtu.ModbusConfigurationException;
import org.openmuc.framework.driver.modbus.rtu.ModbusRTUConnection;
import org.openmuc.framework.driver.modbus.rtutcp.ModbusRTUTCPConnection;
import org.openmuc.framework.driver.modbus.tcp.ModbusTCPConnection;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of the modbus driver.
 */
@Component
public final class ModbusDriver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(ModbusDriver.class);

    private final static DriverInfo info = new DriverInfo(
            // id
            "modbus",
            // description
            "Driver to communicate with devices via Modbus protocol. The driver supports TCP, RTU and RTU over TCP.",
            // device address
            "The device address dependes on the selected type\n RTU: <serial port> e.g. /dev/ttyS0\n "
                    + "TCP: <ip>[:<port>]\n RTUTCP: <ip>[:<port>]",
            // settings
            "Synopsis: <type> \nThe type of connection: RTU (serial), TCP (ethernet) or RTUTCP (serial over ethernet)",
            // channel address
            "Synopsis: <UnitId>:<PrimaryTable>:<Address>:<Datatyp>",
            // device scan settings
            "");

    // TODO get it from channel.xml
    private final static int timeoutInMillisecons = 10000;

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public Connection connect(String deviceAddress, String settings) throws ConnectionException {

        // TODO refactor exception handling in this method

        ModbusConnection connection;

        if (settings.equals("")) {
            throw new ConnectionException("no device settings found in config. Please specify settings.");
        }
        else {
            String[] settingsArray = settings.split(":");
            String mode = settingsArray[0];
            if (mode.equalsIgnoreCase("RTU")) {
                try {
                    connection = new ModbusRTUConnection(deviceAddress, settingsArray, timeoutInMillisecons);
                } catch (ModbusConfigurationException e) {
                    logger.error("Unable to create ModbusRTUConnection", e);
                    throw new ConnectionException();
                }
            }
            else if (mode.equalsIgnoreCase("TCP")) {
                connection = new ModbusTCPConnection(deviceAddress, timeoutInMillisecons);
            }
            else if (mode.equalsIgnoreCase("RTUTCP")) {
                // try {
                connection = new ModbusRTUTCPConnection(deviceAddress, timeoutInMillisecons);
                // } catch (ModbusConfigurationException e) {
                // logger.error("Unable to create ModbusRTUTCPConnection", e);
                // throw new ConnectionException();
                // }
            }
            else {
                throw new ConnectionException("Unknown Mode. Use RTU, TCP or RTUTCP.");
            }
        }
        return connection;

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

}
