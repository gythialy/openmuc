/*
 * Copyright 2011-2021 Fraunhofer ISE
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

    private static final Logger logger = LoggerFactory.getLogger(ModbusDriver.class);
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    @Override
    public DriverInfo getInfo() {

        final String ID = "modbus";

        final String DESCRIPTION = "Driver to communicate with devices via Modbus protocol. The driver supports TCP, RTU and RTU over TCP.";

        final String TCP_ADDRESS = "  TCP: <ip>[:<port>] (e.g. 192.168.30.103:502)";
        final String RTUTCP_ADDRESS = "  RTUTCP: <ip>[:<port>] (e.g. 192.168.30.103:502)";
        final String RTU_ADDRESS = "  RTU: <serial port> (e.g. /dev/ttyS0)";
        final String DEVICE_ADDRESS = "The device address dependes on the selected type: \n" + TCP_ADDRESS + "\n"
                + RTUTCP_ADDRESS + "\n" + RTU_ADDRESS;

        // FIXME auto generate settings string from class to avoid inconsistency

        // FIXME OpenMUC passes only the connection settings to the driver. Driver is unable to access the
        // samplingTimeout specified in channels.xml. As workaround the timeout is added to the device settings for the
        // modbus driver. timeoutInMs used for:
        // TCP: m_Socket.setSoTimeout(m_Timeout);
        // RTU: m_SerialPort.enableReceiveTimeout(ms);

        final String TCP_SETTINGS = "  TCP[:timeout=<timoutInMs>] (e.g. TCP or TCP:timeout=3000)";
        final String RTUTCP_SETTINGS = "  RTUTCP[:timeout=<timoutInMs>] ";
        final String RTU_SETTINGS = "  RTU:<ENCODING>:<BAUDRATE>:<DATABITS>:<PARITY>:<STOPBITS>:<ECHO>:<FLOWCONTROL_IN>:<FLOWCONTEOL_OUT>[:timeout=<timoutInMs>]";
        final String DEVICE_SETTINGS = "Device settings depend on selected type: \n" + TCP_SETTINGS + "\n"
                + RTUTCP_SETTINGS + "\n" + RTU_SETTINGS;

        final String CHANNEL_ADDRESS = "<UnitId>:<PrimaryTable>:<Address>:<Datatyp>";

        final String DEVICE_SCAN_SETTINGS = "Device scan is not supported.";

        return new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, DEVICE_SETTINGS, CHANNEL_ADDRESS, DEVICE_SCAN_SETTINGS);

    }

    @Override
    public Connection connect(String deviceAddress, String settings) throws ConnectionException {

        ModbusConnection connection;

        // TODO consider retries in sampling timeout (e.g. one time 12000 ms or three times 4000 ms)
        // FIXME quite inconvenient/complex to get the timeout from config, since the driver doesn't know the device id!

        if (settings.equals("")) {
            throw new ConnectionException("no device settings found in config. Please specify settings.");
        }
        else {
            String[] settingsArray = settings.split(":");
            String mode = settingsArray[0];

            int timeoutMs = getTimeoutFromSettings(settingsArray);

            if (mode.equalsIgnoreCase("RTU")) {
                try {
                    connection = new ModbusRTUConnection(deviceAddress, settingsArray, timeoutMs);
                } catch (ModbusConfigurationException e) {
                    logger.error("Unable to create ModbusRTUConnection", e);
                    throw new ConnectionException();
                }
            }
            else if (mode.equalsIgnoreCase("TCP")) {

                connection = new ModbusTCPConnection(deviceAddress, timeoutMs);
            }
            else if (mode.equalsIgnoreCase("RTUTCP")) {
                connection = new ModbusRTUTCPConnection(deviceAddress, timeoutMs);
            }
            else {
                throw new ConnectionException("Unknown Mode. Use RTU, TCP or RTUTCP.");
            }
        }
        return connection;

    }

    // FIXME 1: better timeout handling and general settings parsing for drivers
    // FIXME 2: this should be the max timeout. the duration of each read channel should be subtracted from the max
    // timeout
    private int getTimeoutFromSettings(String[] settingsArray) {

        int timeoutMs = DEFAULT_TIMEOUT_MS;

        try {
            for (String setting : settingsArray) {
                if (setting.startsWith("timeout")) {
                    String[] timeoutParam = setting.split("=");
                    timeoutMs = validateTimeout(timeoutParam);
                }
            }

        } catch (Exception e) {
            logger.warn(
                    "Unable to parse timeout from settings. Using default timeout of " + DEFAULT_TIMEOUT_MS + " ms.");
        }

        logger.info("Set sampling timeout to " + timeoutMs + " ms.");

        return timeoutMs;
    }

    private int validateTimeout(String[] timeoutParam) {

        int timeoutMs = Integer.valueOf(timeoutParam[1]).intValue();

        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Invalid SamplingTimeout is smaller or equal 0.");
        }

        return timeoutMs;
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
