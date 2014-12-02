/*
 * Copyright 2011-14 Fraunhofer ISE
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

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.*;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.j62056.Connection;
import org.openmuc.j62056.DataSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public final class Iec62056Driver implements DriverService {

    private final static DriverInfo info = new DriverInfo("iec62056p21",
                                                          // id
                                                          // description
                                                          "This driver can read meters using IEC 62056-21 Mode C.",
                                                          // interface address
                                                          "N.A.",
                                                          // device address
                                                          "Synopsis: <serial_port>\nExamples: /dev/ttyS0 (Unix), COM1 (Windows)",
                                                          // parameters
                                                          "N.A.",
                                                          // channel address
                                                          "Synopsis: <data_set_id>",
                                                          // device scan parameters
                                                          "Synopsis: <serial_port> [-e] [-d <baud_rate_change_delay>]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)\n-e = enable handling of echos caused by optical tranceivers\n-d <baud_rate_change_delay> = delay of baud rate change in ms. Default is 0. USB to serial converters often require a delay of up to 250ms.");

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {

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
            } else if (args[i].equals("-d")) {
                i++;
                if (i == args.length) {
                    throw new ArgumentSyntaxException(
                            "No baudRateChangeDelay was specified after the -d parameter");
                }
                try {
                    baudRateChangeDelay = Integer.parseInt(args[i]);
                }
                catch (NumberFormatException e) {
                    throw new ArgumentSyntaxException(
                            "Specified baudRateChangeDelay is not an integer.");
                }
            } else {
                serialPortName = args[i];
            }
        }

        if (serialPortName.isEmpty()) {
            throw new ArgumentSyntaxException(
                    "The <serial_port> has to be specified in the settings");
        }

        Connection connection = new Connection(serialPortName, echoHandling, baudRateChangeDelay);
        try {
            connection.open();
        }
        catch (IOException e) {
            throw new ScanException(e);
        }

        try {
            List<DataSet> dataSets = connection.read();
            String deviceSettings;
            if (echoHandling) {
                if (baudRateChangeDelay > 0) {
                    deviceSettings = "-e -d " + baudRateChangeDelay;
                } else {
                    deviceSettings = "-e";
                }
            } else {
                if (baudRateChangeDelay > 0) {
                    deviceSettings = "-d " + baudRateChangeDelay;
                } else {
                    deviceSettings = "";
                }
            }

            listener.deviceFound(new DeviceScanInfo(null,
                                                    serialPortName,
                                                    deviceSettings,
                                                    dataSets.get(0).getId()
                                                            .replaceAll("\\p{Cntrl}", "")));

        }
        catch (IOException e) {
            e.printStackTrace();
            throw new ScanException(e);
        }
        catch (TimeoutException e) {
            e.printStackTrace();
            throw new ScanException(e);
        }
        finally {
            connection.close();
        }

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ScanException, ConnectionException {

        List<DataSet> dataSets;
        try {
            dataSets = ((Connection) connection.getConnectionHandle()).read();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            throw new ScanException(e1);
        }
        catch (TimeoutException e) {
            e.printStackTrace();
            throw new ScanException(e);
        }

        if (dataSets == null) {
            throw new ScanException("Read timeout.");
        }

        List<ChannelScanInfo> scanInfos = new ArrayList<ChannelScanInfo>(dataSets.size());

        for (DataSet dataSet : dataSets) {
            try {
                Double.parseDouble(dataSet.getValue());
                scanInfos.add(new ChannelScanInfo(dataSet.getId(), "", ValueType.DOUBLE, null));
            }
            catch (NumberFormatException e) {
                scanInfos.add(new ChannelScanInfo(dataSet.getId(),
                                                  "",
                                                  ValueType.STRING,
                                                  dataSet.getValue().length()));
            }

        }

        return scanInfos;
    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        boolean echoHandling = false;
        int baudRateChangeDelay = 0;

        if (!settings.equals("")) {

            String[] args = settings.split("\\s+");
            if (args.length > 2) {
                throw new ArgumentSyntaxException(
                        "More than two arguments in the settings are not allowed.");
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-e")) {
                    echoHandling = true;
                } else if (args[i].equals("-d")) {
                    i++;
                    if (i == args.length) {
                        throw new ArgumentSyntaxException(
                                "No baudRateChangeDelay was specified after the -d parameter");
                    }
                    try {
                        baudRateChangeDelay = Integer.parseInt(args[i]);
                    }
                    catch (NumberFormatException e) {
                        throw new ArgumentSyntaxException(
                                "Specified baudRateChangeDelay is not an integer.");
                    }
                } else {
                    throw new ArgumentSyntaxException("Found unknown argument in settings: "
                                                      + args[i]);
                }
            }
        }

        Connection connection = new Connection(deviceAddress, echoHandling, baudRateChangeDelay);
        try {
            connection.open();
        }
        catch (IOException e) {
            throw new ConnectionException("Unable to open local serial port: " + deviceAddress, e);
        }

        try {
            connection.read();
        }
        catch (IOException e) {
            connection.close();
            throw new ConnectionException("IOException trying to read meter: "
                                          + deviceAddress
                                          + ": "
                                          + e.getMessage(),
                                          e);
        }
        catch (TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException("Read timed out: " + e.getMessage());
        }
        return connection;

    }

    @Override
    public void disconnect(DeviceConnection connection) {
        ((Connection) connection.getConnectionHandle()).close();
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        Connection deviceConnection = (Connection) connection.getConnectionHandle();

        List<DataSet> dataSets;
        try {
            dataSets = deviceConnection.read();
        }
        catch (IOException e) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.DRIVER_ERROR_READ_FAILURE));
            }
            return null;
        }
        catch (TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException("Read timed out: " + e.getMessage());
        }

        if (dataSets == null) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.TIMEOUT));
            }
            return null;
        }

        long time = System.currentTimeMillis();
        for (ChannelRecordContainer container : containers) {
            for (DataSet dataSet : dataSets) {
                if (dataSet.getId().equals(container.getChannelAddress())) {
                    String value = dataSet.getValue();
                    if (value != null) {
                        try {
                            container.setRecord(new Record(new DoubleValue(Double.parseDouble(
                                    dataSet.getValue())),
                                                           time));
                        }
                        catch (NumberFormatException e) {
                            container.setRecord(new Record(new StringValue(dataSet.getValue()),
                                                           time));
                        }
                    }
                    break;
                }
            }
        }

        return null;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
