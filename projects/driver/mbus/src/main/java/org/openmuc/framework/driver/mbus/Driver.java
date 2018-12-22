/*
 * Copyright 2011-18 Fraunhofer ISE
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
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.SecondaryAddressListener;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.VerboseMessageListener;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class Driver implements DriverService {

    private static final Logger logger = LoggerFactory.getLogger(Driver.class);

    private final Map<String, ConnectionInterface> interfaces = new HashMap<>();

    private static final String ID = "mbus";
    private static final String DESCRIPTION = "M-Bus (wired) is a protocol to read out meters.";
    private static final String DEVICE_ADDRESS = "Synopsis: <serial_port>:<mbus_address> or tcp:<host_address>:>port>"
            + "Example for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows) The mbus_address can either be the primary"
            + " address or the secondary address";
    private static final String SETTINGS = "Synopsis: [<baud_rate>][:t<timeout>][:lr][:ar]:[tc:<tcp_connection_timeout>]"
            + "The default baud rate is 2400. Default read timeout is 2500 ms. "
            + "Example: 9600:t5000. 'ar' means application reset and 'lr' link reset before readout.";
    private static final String CHANNEL_ADDRESS = "Synopsis: [X]<dib>:<vib> The DIB and VIB fields in hexadecimal form separated by a colon. "
            + "If the channel address starts with an X then the specific data record will be selected for readout before reading it.";
    private static final String DEVICE_SCAN_SETTINGS = "Synopsis: <serial_port>[:<baud_rate>][:s][:t<scan_timeout>]"
            + "Examples for <serial_port>: /dev/ttyS0 (Unix), COM1 (Windows); 's' for secondary address scan.>";

    private static final DriverInfo info = new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, SETTINGS, CHANNEL_ADDRESS,
            DEVICE_SCAN_SETTINGS);

    private static final String SECONDARY_ADDRESS_SCAN = "s";
    private static final String APPLICATION_RESET = "ar";
    private static final String LINK_RESET = "lr";
    private static final String SEPERATOR = ":";

    private static final String TCP = "tcp";

    boolean interruptScan;

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settingsString, DriverDeviceScanListener listener)
            throws ArgumentSyntaxException, ScanException, ScanInterruptedException {

        interruptScan = false;

        Settings settings = new Settings(settingsString, true);

        MBusConnection mBusConnection;
        if (!interfaces.containsKey(settings.scanConnectionAddress)) {
            try {
                if (settings.host.isEmpty()) {
                    mBusConnection = MBusConnection.newSerialBuilder(settings.scanConnectionAddress)
                            .setBaudrate(settings.baudRate)
                            .setTimeout(settings.timeout)
                            .build();
                }
                else {
                    mBusConnection = MBusConnection.newTcpBuilder(settings.host, settings.port)
                            .setTimeout(settings.timeout)
                            .setConnectionTimeout(settings.connectionTimeout)
                            .build();
                }
            } catch (IOException e) {
                throw new ScanException(e);
            }
            if (logger.isTraceEnabled()) {
                mBusConnection.setVerboseMessageListener(new VerboseMessageListenerImpl());
            }
        }
        else {
            mBusConnection = interfaces.get(settings.scanConnectionAddress).getMBusConnection();
        }

        try {
            if (settings.scanSecondary) {
                SecondaryAddressListenerImplementation secondaryAddressListenerImplementation = new SecondaryAddressListenerImplementation(
                        listener, settings.scanConnectionAddress);
                mBusConnection.scan("ffffffff", secondaryAddressListenerImplementation);
            }
            else {
                scanPrimaryAddress(listener, settings, mBusConnection);
            }

        } catch (IOException e) {
            logger.error("Failed to scan for devices.", e);
        } finally {
            mBusConnection.close();
        }

    }

    private void scanPrimaryAddress(DriverDeviceScanListener listener, Settings settings, MBusConnection mBusConnection)
            throws ScanInterruptedException, ScanException {
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
                dataStructure = mBusConnection.read(i);
            } catch (InterruptedIOException e) {
                logger.debug("No meter found on address {}", i);
                continue;
            } catch (IOException e) {
                throw new ScanException(e);
            }

            String description = "";
            if (dataStructure != null) {
                SecondaryAddress secondaryAddress = dataStructure.getSecondaryAddress();
                description = getScanDescription(secondaryAddress);
            }
            listener.deviceFound(new DeviceScanInfo(settings.scanConnectionAddress + ':' + i, "", description));
            logger.debug("Meter found on address {}", i);
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        interruptScan = true;
    }

    @Override
    public Connection connect(String deviceAddress, String settingsString)
            throws ArgumentSyntaxException, ConnectionException {

        String[] deviceAddressTokens = deviceAddress.trim().split(":");
        String serialPortName = "";

        boolean isTCP = false;
        String host = "";
        int port = 0;
        int offset;

        if (deviceAddressTokens[0].equalsIgnoreCase(TCP)) {
            host = deviceAddressTokens[1];
            try {
                port = Integer.parseInt(deviceAddressTokens[2]);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("Could not parse port.");
            }
            isTCP = true;
            offset = 2;
        }
        else {
            if (deviceAddressTokens.length != 2) {
                throw new ArgumentSyntaxException("The device address does not consist of two parameters.");
            }
            offset = 0;
            serialPortName = deviceAddressTokens[0 + offset];
        }

        Integer mBusAddress;
        SecondaryAddress secondaryAddress = null;
        try {
            if (deviceAddressTokens[1 + offset].length() == 16) {
                mBusAddress = 0xfd;
                byte[] saData = Helper.hexToBytes(deviceAddressTokens[1 + offset]);
                secondaryAddress = SecondaryAddress.newFromLongHeader(saData, 0);
            }
            else {
                mBusAddress = Integer.decode(deviceAddressTokens[1 + offset]);
            }
        } catch (Exception e) {
            throw new ArgumentSyntaxException("Settings: mBusAddress (" + deviceAddressTokens[1 + offset]
                    + ") is not a number between 0 and 255 nor a 16 sign long hexadecimal secondary address");
        }

        ConnectionInterface connectionInterface;
        Settings settings = new Settings(settingsString, false);

        synchronized (this) {

            synchronized (interfaces) {
                if (isTCP) {
                    connectionInterface = interfaces.get(host + port);
                }
                else {
                    connectionInterface = interfaces.get(serialPortName);
                }

                if (connectionInterface == null) {
                    MBusConnection connection;
                    try {
                        if (isTCP) {
                            connection = MBusConnection.newTcpBuilder(host, port)
                                    .setConnectionTimeout(settings.connectionTimeout)
                                    .setTimeout(settings.timeout)
                                    .build();
                        }
                        else {
                            connection = MBusConnection.newSerialBuilder(serialPortName)
                                    .setBaudrate(settings.baudRate)
                                    .setTimeout(settings.timeout)
                                    .build();

                        }
                    } catch (IOException e) {
                        throw new ConnectionException(
                                "Unable to bind local interface: " + deviceAddressTokens[0 + offset], e);
                    }

                    if (logger.isTraceEnabled()) {
                        connection.setVerboseMessageListener(new VerboseMessageListenerImpl());
                    }

                    if (isTCP) {
                        connectionInterface = new ConnectionInterface(connection, host, port, interfaces);
                    }
                    else {
                        connectionInterface = new ConnectionInterface(connection, serialPortName, interfaces);
                    }
                }
            }

            synchronized (connectionInterface) {

                try {
                    MBusConnection mBusConnection = connectionInterface.getMBusConnection();

                    if (secondaryAddress != null || settings.resetLink) {
                        try {
                            mBusConnection.linkReset(mBusAddress);
                            sleep(100); // for slow slaves
                        } catch (SerialPortTimeoutException e) {
                            if (secondaryAddress == null) {
                                serialPortTimeoutExceptionHandler(connectionInterface, e);
                            }
                        }
                    }

                    if (secondaryAddress != null && settings.resetApplication) {
                        mBusConnection.resetReadout(mBusAddress);
                        sleep(100);
                    }

                    if (secondaryAddress != null) {
                        mBusConnection.selectComponent(secondaryAddress);
                        sleep(100);
                    }
                    mBusConnection.read(mBusAddress);

                } catch (SerialPortTimeoutException e) {
                    serialPortTimeoutExceptionHandler(connectionInterface, e);
                } catch (IOException e) {
                    connectionInterface.close();
                    throw new ConnectionException(e);
                }

                connectionInterface.increaseConnectionCounter();
            }
        }
        DriverConnection driverCon = new DriverConnection(connectionInterface, mBusAddress, secondaryAddress);
        driverCon.setResetLink(settings.resetLink);
        driverCon.setResetApplication(settings.resetApplication);

        return driverCon;
    }

    private void serialPortTimeoutExceptionHandler(ConnectionInterface connectionInterface,
            SerialPortTimeoutException e) throws ConnectionException {
        if (connectionInterface.getDeviceCounter() == 0) {
            connectionInterface.close();
        }
        throw new ConnectionException(e);
    }

    private void sleep(long millisec) throws ConnectionException {
        try {
            Thread.sleep(millisec);
        } catch (InterruptedException e) {
            throw new ConnectionException(e);
        }
    }

    class SecondaryAddressListenerImplementation implements SecondaryAddressListener {
        private final DriverDeviceScanListener driverDeviceScanListener;
        private final String connectionAddress;

        private SecondaryAddressListenerImplementation(DriverDeviceScanListener driverDeviceScanListener,
                String connectionAddress) {
            this.driverDeviceScanListener = driverDeviceScanListener;
            this.connectionAddress = connectionAddress;
        }

        @Override
        public void newScanMessage(String message) {
            // Do nothing
        }

        @Override
        public void newDeviceFound(SecondaryAddress secondaryAddress) {
            driverDeviceScanListener.deviceFound(new DeviceScanInfo(
                    connectionAddress + SEPERATOR + Helper.bytesToHex(secondaryAddress.asByteArray()), "",
                    getScanDescription(secondaryAddress)));
        }

    }

    private String getScanDescription(SecondaryAddress secondaryAddress) {
        return "ManufactureId:" + secondaryAddress.getManufacturerId() + ";DeviceType:"
                + secondaryAddress.getDeviceType() + ";DeviceID:" + secondaryAddress.getDeviceId() + ";Version:"
                + secondaryAddress.getVersion();
    }

    private class Settings {

        private String scanConnectionAddress = "";
        private boolean scanSecondary = false;

        private boolean resetLink = false;
        private boolean resetApplication = false;

        private int timeout = 2500;
        private int baudRate = 2400;

        private String host = "";
        private int port;
        private int connectionTimeout = 10000;

        private Settings(String settings, boolean scan) throws ArgumentSyntaxException {
            if (scan) {
                setScanOptions(settings);
            }
            else {
                parseDeviceSettings(settings);
            }
        }

        private void setScanOptions(String settings) throws ArgumentSyntaxException {
            String[] args = settings.split(":");
            if (settings.isEmpty() || args.length > 5) {
                throw new ArgumentSyntaxException(
                        "Less than one or more than five arguments in the settings are not allowed.");
            }

            int i;
            if (args[0].equalsIgnoreCase(TCP)) {
                host = args[1];
                try {
                    port = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    throw new ArgumentSyntaxException("Error parsing tcp port");
                }
                i = 3;
            }
            else {
                scanConnectionAddress = args[0];
                i = 1;
            }

            for (; i < args.length; ++i) {
                if (args[i].equalsIgnoreCase(SECONDARY_ADDRESS_SCAN)) {
                    scanSecondary = true;
                }
                else if (args[i].matches("^[t,T][0-9]*")) {
                    String setting = args[i].substring(1);
                    timeout = parseInt(setting, "Timeout is not a parsable number.");
                }
                else {
                    try {
                        baudRate = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentSyntaxException("Argument " + (i + 1) + " is not an integer.");
                    }
                }
            }
        }

        private void parseDeviceSettings(String settings) throws ArgumentSyntaxException {
            if (!settings.isEmpty()) {
                String[] settingArray = settings.split(":");

                for (String setting : settingArray) {
                    if (setting.matches("^[t,T][0-9]*")) {
                        setting = setting.substring(1);
                        timeout = parseInt(setting, "Settings: Timeout is not a parsable number.");
                    }
                    if (setting.matches("^[tc,TC][0-9]*")) {
                        setting = setting.substring(1);
                        connectionTimeout = parseInt(setting, "Settings: Connection timeout is not a parsable number.");
                    }
                    else if (setting.equals(LINK_RESET)) {
                        resetLink = true;
                    }
                    else if (setting.equals(APPLICATION_RESET)) {
                        resetApplication = true;
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
    }

    private class VerboseMessageListenerImpl implements VerboseMessageListener {

        @Override
        public void newVerboseMessage(org.openmuc.jmbus.VerboseMessage debugMessage) {
            String msgDir = debugMessage.getMessageDirection().toString().toLowerCase();
            String msgHex = Helper.bytesToHex(debugMessage.getMessage());
            logger.trace("{} message: {}", msgDir, msgHex);
        }

    }

}
