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
package org.openmuc.framework.driver.iec62056p21;

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.j62056.DataMessage;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.Iec21Port;
import org.openmuc.j62056.Iec21Port.Builder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class Iec62056Driver implements DriverService {

    private static final Logger logger = LoggerFactory.getLogger(Iec62056Driver.class);

    private static final String BAUD_RATE_CHANGE_DELAY = "-d";
    private static final String TIMEOUT_PARAM = "-t";
    private static final String RETRIES_PARAM = "-r";
    private static final String INITIAL_BAUD_RATE = "-bd";
    private static final String DEVICE_ADDRESS = "-a";
    private static final String FIXED_BAUD_RATE = "-fbd";
    private static final String REQUEST_START_CHARACTER = "-rsc";
    private static final String READ_STANDARD = "-rs";

    private String serialPortName = "";
    private int baudRateChangeDelay = 0;
    private int timeout = 2000;
    private int retries = 1;
    private int initialBaudRate = -1;
    private boolean fixedBaudRate = false;
    private static final boolean VERBOSE = false;
    private String deviceAddress = "";
    private String requestStartCharacter = "";
    private boolean readStandard = false;

    private static final String DRIVER_ID = "iec62056p21";
    private static final String DRIVER_DESCRIPTION = "This driver can read meters using IEC 62056-21 Mode A, B and C.";
    private static final String DEVICE_ADDRESS_SYNTAX = "Synopsis: <serial_port>\nExamples: /dev/ttyS0 (Unix), COM1 (Windows)";

    private static final String SETTINGS = "[" + BAUD_RATE_CHANGE_DELAY + " <baud_rate_change_delay>] [" + TIMEOUT_PARAM
            + " <timeout>] [" + RETRIES_PARAM + " <number_of_read_retries>] [" + INITIAL_BAUD_RATE
            + " <initial_baud_rate>] [" + DEVICE_ADDRESS + " <device_address>] [" + FIXED_BAUD_RATE + "] ["
            + REQUEST_START_CHARACTER + " <request_message_start_character>]\n" + BAUD_RATE_CHANGE_DELAY
            + " sets the waiting time between a baud rate change, default: 0; \n" + TIMEOUT_PARAM
            + " sets the response timeout, default: 2000\n" + INITIAL_BAUD_RATE
            + " sets a initial baud rate e.g. for devices with modem configuration, default: 300\n" + DEVICE_ADDRESS
            + " is mostly needed for devices with RS485, default: empty\n" + FIXED_BAUD_RATE
            + " activates fixed baud rate, default: deactivated\n" + REQUEST_START_CHARACTER
            + " Used for manufacture specific request messages\n" + READ_STANDARD
            + " Reads the standard message and the manufacture specific message. Only if " + REQUEST_START_CHARACTER
            + " is set\n";
    private static final String SETTINGS_SYNTAX = "Synopsis: " + SETTINGS;
    private static final String CHANNEL_ADDRESS_SYNTAX = "Synopsis: <data_set_id>";
    private static final String DEVICE_SCAN_SETTINGS_SYNTAX = "Synopsis: <serial_port> " + SETTINGS;

    private static final DriverInfo INFO = new DriverInfo(DRIVER_ID, DRIVER_DESCRIPTION, DEVICE_ADDRESS_SYNTAX,
            SETTINGS_SYNTAX, CHANNEL_ADDRESS_SYNTAX, DEVICE_SCAN_SETTINGS_SYNTAX);

    @Override
    public DriverInfo getInfo() {
        return INFO;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        handleScanParameter(settings);

        Iec21Port iec21Port = null;
        Builder iec21PortBuilder = getConfiguredBuilder();

        try {
            iec21Port = iec21PortBuilder.buildAndOpen();
        } catch (IOException e) {
            throw new ScanException("Failed to open serial port: " + e.getMessage());
        }

        try {
            DataMessage dataMessage = iec21Port.read();
            List<DataSet> dataSets = dataMessage.getDataSets();
            StringBuilder deviceSettings = new StringBuilder();

            if (baudRateChangeDelay > 0) {
                deviceSettings.append(' ').append(BAUD_RATE_CHANGE_DELAY).append(' ').append(baudRateChangeDelay);
            }
            String deviceSettingsString = deviceSettings.toString().trim();

            listener.deviceFound(new DeviceScanInfo(serialPortName, deviceSettingsString,
                    dataSets.get(0).getAddress().replaceAll("\\p{Cntrl}", "")));

        } catch (IOException e) {
            throw new ScanException(e);
        } finally {
            iec21Port.close();
        }

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        serialPortName = deviceAddress;
        handleParameter(settings);
        Builder configuredBuilder = getConfiguredBuilder();
        return new Iec62056Connection(configuredBuilder, retries, readStandard, requestStartCharacter);
    }

    private Builder getConfiguredBuilder() {
        return new Iec21Port.Builder(serialPortName).setBaudRateChangeDelay(baudRateChangeDelay)
                .setTimeout(timeout)
                .enableFixedBaudrate(fixedBaudRate)
                .setInitialBaudrate(initialBaudRate)
                .setDeviceAddress(deviceAddress)
                .enableVerboseMode(VERBOSE)
                .setRequestStartCharacters(requestStartCharacter);
    }

    private void handleScanParameter(String settings) throws ArgumentSyntaxException {
        if (settings.isEmpty()) {
            throw new ArgumentSyntaxException("No parameter given. At least serial port is needed");
        }
        String[] args = settings.split("\\s+", 0);

        serialPortName = args[0].trim();
        if (serialPortName.isEmpty()) {
            throw new ArgumentSyntaxException(
                    "The <serial_port> has to be specified in the settings, as first parameter");
        }

        parseArguments(args, true);
        if (requestStartCharacter.isEmpty()) {
            readStandard = false;
        }

    }

    private void handleParameter(String settings) throws ArgumentSyntaxException {
        String[] args = settings.split("\\s+", 0);

        parseArguments(args, false);
        if (requestStartCharacter.isEmpty()) {
            readStandard = false;
        }
    }

    private void parseArguments(String[] args, boolean isScan) throws ArgumentSyntaxException {
        int i = isScan ? 1 : 0;

        for (; i < args.length; i++) {

            if (args[i].equals(BAUD_RATE_CHANGE_DELAY)) {
                ++i;
                baudRateChangeDelay = getIntValue(args, i, BAUD_RATE_CHANGE_DELAY);
            }
            else if (args[i].equals(RETRIES_PARAM)) {
                ++i;
                retries = getIntValue(args, i, RETRIES_PARAM);
            }
            else if (args[i].equals(TIMEOUT_PARAM)) {
                ++i;
                timeout = getIntValue(args, i, TIMEOUT_PARAM);
            }
            else if (args[i].equals(INITIAL_BAUD_RATE)) {
                ++i;
                initialBaudRate = getIntValue(args, i, INITIAL_BAUD_RATE);
            }
            else if (args[i].equals(DEVICE_ADDRESS)) {
                ++i;
                deviceAddress = getStringValue(args, i, DEVICE_ADDRESS);
            }
            else if (args[i].equals(FIXED_BAUD_RATE)) {
                fixedBaudRate = true;
            }
            else if (args[i].equals(REQUEST_START_CHARACTER)) {
                ++i;
                requestStartCharacter = getStringValue(args, i, REQUEST_START_CHARACTER);
            }
            else if (args[i].equals(READ_STANDARD)) {
                readStandard = true;
            }
            else {
                throw new ArgumentSyntaxException("Found unknown argument in settings: " + args[i]);
            }
        }
    }

    private int getIntValue(String[] args, int i, String parameter) throws ArgumentSyntaxException {
        int ret;
        checkParameter(args, i, parameter);
        try {
            ret = Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            throw new ArgumentSyntaxException("Specified value of parameter'" + parameter + "' is not an integer");
        }
        return ret;
    }

    private String getStringValue(String[] args, int i, String parameter) throws ArgumentSyntaxException {
        String ret;
        checkParameter(args, i, parameter);
        ret = args[i];
        return ret;
    }

    private void checkParameter(String[] args, int i, String parameter) throws ArgumentSyntaxException {
        if (i == args.length) {
            throw new ArgumentSyntaxException("No value was specified after the " + parameter + " parameter");
        }
    }

}
