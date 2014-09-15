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

package org.openmuc.framework.driver.ehz;

import gnu.io.CommPortIdentifier;
import org.openmuc.framework.config.*;
import org.openmuc.framework.driver.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;

public class EhzDriver implements DriverService {

    private static Logger logger = LoggerFactory.getLogger(EhzDriver.class);

    private static final String ADDR_IEC = "iec";
    private static final String ADDR_SML = "sml";

    private final static DriverInfo info = new DriverInfo("ehz",
                                                          "Driver for IEC 62056-21 and SML",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?");

    private final static int timeout = 10000;

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {

        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            String serialPort = port.getName();
            logger.trace("searching for device at " + serialPort);
            URI deviceAddress = null;
            Connection connection = null;
            if (deviceAddress == null) {
                try {
                    connection = new IecConnection(serialPort, 2000);
                    if (connection.isWorking()) {
                        logger.info("found iec device at " + serialPort);
                        deviceAddress = new URI(ADDR_IEC + "://" + serialPort);
                    }
                    connection.close();
                }
                catch (Exception e) {
                    logger.trace(serialPort + " is no iec device");
                }
            }
            if (deviceAddress == null) {
                try {
                    connection = new SmlConnection(serialPort);
                    if (connection.isWorking()) {
                        logger.info("found sml device at " + serialPort);
                        deviceAddress = new URI(ADDR_SML + "://" + serialPort);
                    }
                    connection.close();
                }
                catch (Exception e) {
                    logger.trace(serialPort + " is no sml device");
                }
            }
            if (deviceAddress != null) {
                listener.deviceFound(new DeviceScanInfo("", deviceAddress.toString(), "", ""));
            } else {
                logger.info("no ehz device found at " + serialPort);
            }
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openmuc.framework.driver.spi.DriverService#scanForChannels(org.openmuc.framework.driver.spi.DeviceConnection,
     * int)
     */
    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        return ((Connection) connection.getConnectionHandle()).listChannels(20000);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openmuc.framework.driver.spi.DriverService#connect(java.lang.String, java.lang.String, java.lang.String,
     * int)
     */
    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        logger.trace("trying to connect to " + deviceAddress);
        try {
            URI device = new URI(deviceAddress);

            if (device.getScheme().equals(ADDR_IEC)) {
                logger.trace("connecting to iec device");
                return new IecConnection(device.getPath(), timeout);
            } else if (device.getScheme().equals(ADDR_SML)) {
                logger.trace("connecting to sml device");
                return new SmlConnection(device.getPath());
            }
            throw new ConnectionException("unrecognized address scheme " + device.getScheme());
        }
        catch (URISyntaxException e) {
            throw new ConnectionException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openmuc.framework.driver.spi.DriverService#disconnect(org.openmuc.framework.driver.spi.DeviceConnection)
     */
    @Override
    public void disconnect(DeviceConnection connection) {
        ((Connection) connection.getConnectionHandle()).close();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openmuc.framework.driver.spi.DriverService#read(org.openmuc.framework.driver.spi.DeviceConnection,
     * java.util.List, java.lang.Object, java.lang.String, int)
     */
    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        Connection con = ((Connection) connection.getConnectionHandle());

        con.read(containers, timeout);

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openmuc.framework.driver.spi.DriverService#startListening(org.openmuc.framework.driver.spi.DeviceConnection,
     * java.util.List, org.openmuc.framework.driver.spi.RecordsReceivedListener)
     */
    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openmuc.framework.driver.spi.DriverService#write(org.openmuc.framework.driver.spi.DeviceConnection,
     * java.util.List, java.lang.Object, int)
     */
    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
