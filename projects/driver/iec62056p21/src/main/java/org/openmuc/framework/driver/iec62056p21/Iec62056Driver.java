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
package org.openmuc.framework.driver.iec62056p21;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.j62056.Connection;
import org.openmuc.j62056.DataSet;
import org.osgi.service.component.annotations.Component;

@Component
public final class Iec62056Driver implements DriverService {

    private final static DriverInfo info = new DriverInfo("iec62056p21", // id
            // description
            "This driver can read meters using IEC 62056-21 Mode C.",
            // device address
            "Synopsis: <serial_port>\nExamples: /dev/ttyS0 (Unix), COM1 (Windows)",
            // parameters
            "N.A.",
            // channel address
            "Synopsis: <data_set_id>",
            // device scan settings
            "Synopsis: <serial_port> [-e] [-d <baud_rate_change_delay>]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)\n-e = enable handling of echos caused by optical tranceivers\n-d <baud_rate_change_delay> = delay of baud rate change in ms. Default is 0. USB to serial converters often require a delay of up to 250ms.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        String[] args = settings.split("\\s+", 0);
        if (args.length < 1 || args.length > 4) {
            throw new ArgumentSyntaxException(
                    "Less than one or more than four arguments in the settings are not allowed.");
        }

        String serialPortName = "";
        boolean echoHandling = false;
        int baudRateChangeDelay = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-e")) {
                echoHandling = true;
            }
            else if (args[i].equals("-d")) {
                i++;
                if (i == args.length) {
                    throw new ArgumentSyntaxException("No baudRateChangeDelay was specified after the -d parameter");
                }
                try {
                    baudRateChangeDelay = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    throw new ArgumentSyntaxException("Specified baudRateChangeDelay is not an integer.");
                }
            }
            else {
                serialPortName = args[i];
            }
        }

        if (serialPortName.isEmpty()) {
            throw new ArgumentSyntaxException("The <serial_port> has to be specified in the settings");
        }

        Connection connection = new Connection(serialPortName, echoHandling, baudRateChangeDelay);
        try {
            connection.open();
        } catch (IOException e) {
            throw new ScanException(e);
        }

        try {
            List<DataSet> dataSets = connection.read();
            String deviceSettings;
            if (echoHandling) {
                if (baudRateChangeDelay > 0) {
                    deviceSettings = "-e -d " + baudRateChangeDelay;
                }
                else {
                    deviceSettings = "-e";
                }
            }
            else {
                if (baudRateChangeDelay > 0) {
                    deviceSettings = "-d " + baudRateChangeDelay;
                }
                else {
                    deviceSettings = "";
                }
            }

            listener.deviceFound(new DeviceScanInfo(serialPortName, deviceSettings,
                    dataSets.get(0).getId().replaceAll("\\p{Cntrl}", "")));

        } catch (IOException e) {
            e.printStackTrace();
            throw new ScanException(e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new ScanException(e);
        } finally {
            connection.close();
        }

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.openmuc.framework.driver.spi.Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        boolean echoHandling = false;
        int baudRateChangeDelay = 0;

        if (!settings.equals("")) {

            String[] args = settings.split("\\s+");
            if (args.length > 2) {
                throw new ArgumentSyntaxException("More than two arguments in the settings are not allowed.");
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-e")) {
                    echoHandling = true;
                }
                else if (args[i].equals("-d")) {
                    i++;
                    if (i == args.length) {
                        throw new ArgumentSyntaxException(
                                "No baudRateChangeDelay was specified after the -d parameter");
                    }
                    try {
                        baudRateChangeDelay = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentSyntaxException("Specified baudRateChangeDelay is not an integer.");
                    }
                }
                else {
                    throw new ArgumentSyntaxException("Found unknown argument in settings: " + args[i]);
                }
            }
        }

        Connection connection = new Connection(deviceAddress, echoHandling, baudRateChangeDelay);
        try {
            connection.open();
        } catch (IOException e) {
            throw new ConnectionException("Unable to open local serial port: " + deviceAddress, e);
        }

        try {
            connection.read();
        } catch (IOException e) {
            connection.close();
            throw new ConnectionException("IOException trying to read meter: " + deviceAddress + ": " + e.getMessage(),
                    e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException("Read timed out: " + e.getMessage());
        }
        return new Iec62056Connection(connection);

    }

}
