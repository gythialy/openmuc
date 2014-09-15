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
package org.openmuc.framework.driver.knx;

import gnu.io.CommPortIdentifier;
import org.openmuc.framework.config.*;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.log.LogManager;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

public class KnxDriver implements DriverService {

    public static final String ADDRESS_SCHEME_KNXIP = "knxip";
    public static final String ADDRESS_SCHEME_RC1180 = "knxrc1180";
    private static Logger logger = LoggerFactory.getLogger(KnxDriver.class);

    private final static int timeout = 10000;

    private final static DriverInfo info = new DriverInfo("knx",
                                                          "Driver to read and write KNX groupaddresses",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?");

    protected void activate(ComponentContext context) {
        if (logger.isDebugEnabled()) {
            LogManager.getManager().addWriter("", new KnxLogWriter()); // Add calimero logger
        }
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivated KNX Driver");
    }

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {

        String[] args = settings.split("\\s+");

        if (args.length > 0) {
            boolean natAware = false;
            boolean mcastResponse = false;
            if (args.length > 1) {
                logger.debug("Applying settings: " + args[1]);
                natAware = args[1].contains("nat");
                mcastResponse = args[1].contains("mcast");
            }
            KnxIpDiscover discover;
            try {
                discover = new KnxIpDiscover(args[0], natAware, mcastResponse);
                discover.startSearch(0, listener);
            }
            catch (IOException e) {
                throw new ScanException(e);
            }
        } else {
            try {
                Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
                while (ports.hasMoreElements()) {
                    CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
                    String name = port.getName();
                    String description = "settings could be: address=1.1.1;serialNumber=0123456789AB";
                    listener.deviceFound(new DeviceScanInfo(null,
                                                            ADDRESS_SCHEME_RC1180 + "://" + name,
                                                            "",
                                                            description));
                }
            }
            catch (Exception e) {
                logger.warn("serial communication failed");
            }
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        return ((KnxConnection) connection.getConnectionHandle()).listKnownChannels();
    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        return new KnxConnection(interfaceAddress, deviceAddress, settings, timeout);
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        ((KnxConnection) connection.getConnectionHandle()).disconnect();
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        KnxConnection knxConnection = (KnxConnection) connection.getConnectionHandle();

        for (ChannelRecordContainer container : containers) {
            try {

                KnxGroupDP groupDP = null;
                if (container.getChannelHandle() == null) {
                    groupDP = createKnxGroupDP(container.getChannelAddress());
                    logger.debug("New datapoint: " + groupDP);
                    container.setChannelHandle(groupDP);
                } else {
                    groupDP = (KnxGroupDP) container.getChannelHandle();
                }

                Record record = knxConnection.read(groupDP, timeout);
                container.setRecord(record);
            }
            catch (KNXTimeoutException e1) {
                logger.debug(e1.getMessage());
                container.setRecord(new Record(null, System.currentTimeMillis(), Flag.TIMEOUT));
            }
            catch (KNXException e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        for (ChannelRecordContainer container : containers) {
            if (container.getChannelHandle() == null) {
                try {
                    container.setChannelHandle(createKnxGroupDP(container.getChannelAddress()));
                }
                catch (KNXException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("Start listening for " + containers.size() + " channels");
        ((KnxConnection) connection.getConnectionHandle()).startListening(containers, listener);
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        KnxConnection knxConnection = (KnxConnection) connection.getConnectionHandle();

        for (ChannelValueContainer container : containers) {
            try {
                KnxGroupDP groupDP = null;
                if (container.getChannelHandle() == null) {
                    groupDP = createKnxGroupDP(container.getChannelAddress());
                    logger.debug("New datapoint: " + groupDP);
                    container.setChannelHandle(groupDP);
                } else {
                    groupDP = (KnxGroupDP) container.getChannelHandle();
                }

                groupDP.getKnxValue().setOpenMucValue(container.getValue());
                boolean state = knxConnection.write(groupDP, timeout);
                if (state) {
                    container.setFlag(Flag.VALID);
                } else {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                }
            }
            catch (KNXException e) {
                logger.warn(e.getMessage());
            }
        }

        return null;
    }

    private static KnxGroupDP createKnxGroupDP(String channelAddress) throws KNXException {
        String[] address = channelAddress.split(":");
        GroupAddress main = new GroupAddress(address[0]);
        String dptID = address[1];
        KnxGroupDP dp = new KnxGroupDP(main, channelAddress, dptID);
        if (address.length == 4) {
            boolean AET = address[2].equals("1");
            String value = address[3];
            if (value.length() == 12) {
                byte[] SNorDoA = new byte[6];
                value = value.toLowerCase();
                for (int i = 0; i < 6; i++) {
                    String hexValue = value.substring(i * 2, (i * 2) + 2);
                    SNorDoA[i] = (byte) Integer.parseInt(hexValue, 16);
                }
                dp.setAddress(AET, SNorDoA);
            }
        }
        return dp;
    }
}
