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
package org.openmuc.framework.driver.mbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.ScanSecondaryAddress;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MBusDriver implements DriverService {
    private final static Logger logger = LoggerFactory.getLogger(MBusDriver.class);

    private final Map<String, MBusSerialInterface> interfaces = new HashMap<>();

    private final static String ID = "mbus";
    private final static String DESCRIPTION = "M-Bus (wired) is a protocol to read out meters.";
    private final static String DEVICE_ADDRESS = "Synopsis: <serial_port>:<mbus_address>\nExample for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)\n The mbus_address can either be the primary address or the secondary address";
    private final static String SETTINGS = "Synopsis: [<baud_rate>][:<timeout>]\nThe default baud rate is 2400. Default read timeout is 2500 ms. Example: 9600:t5000";
    private final static String CHANNEL_ADDRESS = "Synopsis: [X]<dib>:<vib>\nThe DIB and VIB fields in hexadecimal form seperated by a collon. If the channel address starts with an X then the specific data record will be selected for readout before reading it.";
    private final static String DEVICE_SCAN_SETTINGS = "Synopsis: <serial_port>[:<baud_rate>]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows)";
    // "Synopsis: <serial_port>[:<baud_rate>][:s]\nExamples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows); s
    // forsecondary address scan.";

    private final static DriverInfo info = new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, SETTINGS, CHANNEL_ADDRESS,
            DEVICE_SCAN_SETTINGS);

    private boolean interruptScan;
    private final boolean scanSecondary = false;

    private int timeout = 2500;
    private int baudRate = 2400;

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        interruptScan = false;

        String[] args = settings.split(":");
        if (settings.isEmpty() || args.length > 2) {
            throw new ArgumentSyntaxException(
                    "Less than one or more than two arguments in the settings are not allowed.");
        }

        setScanOptions(args);

        MBusSap mBusSap;
        if (!interfaces.containsKey(args[0])) {
            mBusSap = new MBusSap(args[0], baudRate);
            try {
                mBusSap.open();
            } catch (IllegalArgumentException e) {
                throw new ArgumentSyntaxException();
            } catch (IOException e) {
                throw new ScanException(e);
            }
        }
        else {
            mBusSap = interfaces.get(args[0]).getMBusSap();
        }

        mBusSap.setTimeout(timeout);

        try {
            if (scanSecondary) {
                List<SecondaryAddress> addresses = ScanSecondaryAddress.scan(mBusSap, "ffffffff");
                Iterator<SecondaryAddress> iterAddresses = addresses.iterator();
                while (iterAddresses.hasNext()) {
                    SecondaryAddress secondaryAddress = iterAddresses.next();

                    listener.deviceFound(new DeviceScanInfo(args[0] + ":" + secondaryAddress, "",
                            getScanDescription(secondaryAddress)));
                }
            }
            else {
                VariableDataStructure dataStructure = null;
                for (int i = 0; i <= 250; i++) {

                    if (interruptScan) {
                        throw new ScanInterruptedException();
                    }

                    if (i % 5 == 0) {
                        listener.scanProgressUpdate(i * 100 / 250);
                    }
                    logger.debug("scanning for meter with primary address {}", i);
                    try {
                        dataStructure = mBusSap.read(i);
                    } catch (TimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        throw new ScanException(e);
                    }
                    String description = "";
                    if (dataStructure != null) {
                        SecondaryAddress secondaryAddress = dataStructure.getSecondaryAddress();
                        description = getScanDescription(secondaryAddress);
                    }
                    listener.deviceFound(new DeviceScanInfo(args[0] + ":" + i, "", description));
                    logger.debug("found meter: {}", i);
                }
            }

        } finally {
            mBusSap.close();
        }

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        interruptScan = true;

    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        String[] deviceAddressTokens = deviceAddress.trim().split(":");

        if (deviceAddressTokens.length != 2) {
            throw new ArgumentSyntaxException("The device address does not consist of two parameters.");
        }
        String serialPortName = deviceAddressTokens[0];
        Integer mBusAddress;
        SecondaryAddress secondaryAddress = null;
        try {
            if (deviceAddressTokens[1].length() == 16) {
                mBusAddress = 0xfd;
                secondaryAddress = SecondaryAddress.getFromHexString(deviceAddressTokens[1]);
            }
            else {
                mBusAddress = Integer.decode(deviceAddressTokens[1]);
            }
        } catch (Exception e) {
            throw new ArgumentSyntaxException("Settings: mBusAddress (" + deviceAddressTokens[1]
                    + ") is not a int nor a 16 sign long hexadecimal secondary address");
        }

        MBusSerialInterface serialInterface;

        synchronized (this) {

            synchronized (interfaces) {

                serialInterface = interfaces.get(serialPortName);

                if (serialInterface == null) {

                    parseDeviceSettings(settings);

                    MBusSap mBusSap = new MBusSap(serialPortName, baudRate);

                    try {
                        mBusSap.open();
                    } catch (IOException e1) {
                        throw new ConnectionException("Unable to bind local interface: " + deviceAddressTokens[0]);
                    }
                    mBusSap.setTimeout(timeout);
                    serialInterface = new MBusSerialInterface(mBusSap, serialPortName, interfaces);

                }
            }

            synchronized (serialInterface) {
                try {
                    serialInterface.getMBusSap().linkReset(mBusAddress);
                    sleep(100); // for slow slaves
                    if (secondaryAddress != null) {
                        serialInterface.getMBusSap().selectComponent(secondaryAddress);
                        sleep(100);
                    }
                    serialInterface.getMBusSap().read(mBusAddress);

                } catch (IOException e) {
                    serialInterface.close();
                    throw new ConnectionException(e);
                } catch (TimeoutException e) {
                    if (serialInterface.getDeviceCounter() == 0) {
                        serialInterface.close();
                    }
                    throw new ConnectionException(e);
                }

                serialInterface.increaseConnectionCounter();

            }

        }

        return new MBusConnection(serialInterface, mBusAddress, secondaryAddress);

    }

    private void parseDeviceSettings(String settings) throws ArgumentSyntaxException {
        if (!settings.isEmpty()) {
            String[] settingArray = settings.split(":");

            for (String setting : settingArray) {
                if (setting.matches("^[t,T][0-9]*")) {
                    setting = setting.substring(1);
                    timeout = parseInt(setting, "Settings: Timeout is not a parsable number.");
                }
                else if (setting.matches("^[0-9]*")) {
                    baudRate = parseInt(setting, "Settings: Baudrate is not a parsable number.");
                }
                else {
                    throw new ArgumentSyntaxException("Settings: Unknown settings parameter. [" + setting + "]");
                }
            }

        }
    }

    private int parseInt(String setting, String errorMsg) throws ArgumentSyntaxException {
        int ret = 0;
        try {
            ret = Integer.parseInt(setting);
        } catch (NumberFormatException e) {
            throw new ArgumentSyntaxException(errorMsg + " [" + setting + "]");
        }
        return ret;
    }

    private void sleep(long millisec) throws ConnectionException {
        try {
            Thread.sleep(millisec);
        } catch (InterruptedException e) {
            throw new ConnectionException(e);
        }
    }

    private String getScanDescription(SecondaryAddress secondaryAddress) {
        String description = secondaryAddress.getManufacturerId() + '_' + secondaryAddress.getDeviceType() + '_'
                + secondaryAddress.getVersion();
        return description;
    }

    private void setScanOptions(String args[]) throws ArgumentSyntaxException {
        for (int i = 1; i < args.length; ++i) {
            // if (args[i] == "s") {
            // scanSecondary = true;
            // }
            // else {
            try {
                baudRate = Integer.parseInt(args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("Argument number " + i + " is not an integer");// nor option s.");
            }
            // }
        }
    }
}
