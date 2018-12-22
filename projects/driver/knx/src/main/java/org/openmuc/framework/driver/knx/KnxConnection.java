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
package org.openmuc.framework.driver.knx;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.knx.value.KnxValue;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

public class KnxConnection implements Connection {

    private static Logger logger = LoggerFactory.getLogger(KnxConnection.class);
    private static final int DEFAULT_PORT = 3671;
    private static final int DEFAULT_TIMEOUT = 2;

    private KNXNetworkLink knxNetworkLink;
    private ProcessCommunicator processCommunicator;
    private KnxProcessListener processListener;
    private int responseTimeout;
    private String name;

    KnxConnection(String deviceAddress, String settings, int timeout)
            throws ArgumentSyntaxException, ConnectionException {

        URI interfaceURI = null;
        URI deviceURI = null;
        boolean isKNXIP;

        try {
            String[] deviceAddressSubStrings = deviceAddress.split(";");
            if (deviceAddressSubStrings.length == 2) {
                interfaceURI = new URI(deviceAddressSubStrings[0]);
                deviceURI = new URI(deviceAddressSubStrings[1]);
                isKNXIP = true;
            }
            else {
                deviceURI = new URI(deviceAddress);
                isKNXIP = false;
            }
        } catch (URISyntaxException e) {
            logger.error("wrong format of interface address in deviceAddress");
            throw new ArgumentSyntaxException();
        }

        IndividualAddress address = new IndividualAddress(0);
        byte[] serialNumber = new byte[6];
        if (settings != null) {
            String[] settingsArray = settings.split(";");
            for (String arg : settingsArray) {
                int p = arg.indexOf('=');
                if (p != -1) {
                    String key = arg.substring(0, p).toLowerCase().trim();
                    String value = arg.substring(p + 1).trim();
                    if (key.equalsIgnoreCase("address")) {
                        try {
                            address = new IndividualAddress(value);
                            logger.debug("setting individual address to " + address);
                        } catch (KNXFormatException e) {
                            logger.warn("wrong format of individual address in settings");
                        }
                    }
                    else if (key.equalsIgnoreCase("serialnumber")) {
                        if (value.length() == 12) {
                            value = value.toLowerCase();
                            for (int i = 0; i < 6; i++) {
                                String hexValue = value.substring(i * 2, (i * 2) + 2);
                                serialNumber[i] = (byte) Integer.parseInt(hexValue, 16);
                            }
                            logger.debug("setting serial number to " + DataUnitBuilder.toHex(serialNumber, ":"));
                        }
                    }
                }
            }
        }

        if (isKNXIP && isSchemeOk(deviceURI, KnxDriver.ADDRESS_SCHEME_KNXIP)
                && isSchemeOk(interfaceURI, KnxDriver.ADDRESS_SCHEME_KNXIP)) {
            name = interfaceURI.getHost() + " - " + deviceURI.getHost();
            logger.debug("connecting over KNX/IP from " + name.replace("-", "to"));
            connectNetIP(interfaceURI, deviceURI, address);
        }
        else {
            logger.error("wrong format of device URI in deviceAddress");
            throw new ArgumentSyntaxException();
        }

        try {
            processCommunicator = new ProcessCommunicatorImpl(knxNetworkLink);
            processListener = new KnxProcessListener();
            processCommunicator.addProcessListener(processListener);
            setResponseTimeout(timeout);
        } catch (KNXLinkClosedException e) {
            throw new ConnectionException(e);
        }
    }

    private boolean isSchemeOk(URI uri, String scheme) {

        boolean isSchemeOK = uri.getScheme().equalsIgnoreCase(scheme);
        if (!isSchemeOK) {
            logger.error("Scheme is not OK. Is " + uri.getScheme() + " should be ", scheme);
        }

        return isSchemeOK;
    }

    private void connectNetIP(URI localUri, URI remoteUri, IndividualAddress address) throws ConnectionException {

        try {
            String localIP = localUri.getHost();
            int localPort = localUri.getPort() < 0 ? DEFAULT_PORT : localUri.getPort();

            String remoteIP = remoteUri.getHost();
            int remotePort = remoteUri.getPort() < 0 ? DEFAULT_PORT : remoteUri.getPort();

            int serviceMode = KNXNetworkLinkIP.TUNNELING;
            InetSocketAddress localSocket = new InetSocketAddress(localIP, localPort);
            InetSocketAddress remoteSocket = new InetSocketAddress(remoteIP, remotePort);
            boolean useNAT = true;
            KNXMediumSettings settings = new TPSettings();
            settings.setDeviceAddress(address);

            knxNetworkLink = new KNXNetworkLinkIP(serviceMode, localSocket, remoteSocket, useNAT, settings);
        } catch (KNXException e) {
            logger.error("Connection failed: " + e.getMessage());
            throw new ConnectionException(e);
        } catch (InterruptedException e) {
            throw new ConnectionException(e);
        }

    }

    private List<ChannelScanInfo> listKnownChannels() {
        List<ChannelScanInfo> informations = new ArrayList<>();
        Map<GroupAddress, byte[]> values = processListener.getCachedValues();
        Set<GroupAddress> keys = values.keySet();

        for (GroupAddress groupAddress : keys) {
            byte[] asdu = values.get(groupAddress);
            StringBuilder channelAddress = new StringBuilder();
            channelAddress.append(groupAddress.toString()).append(":1.001");

            StringBuilder description = new StringBuilder();
            description.append("Datapoint length: ").append(asdu.length);
            description.append("; Last datapoint ASDU: ").append(DataUnitBuilder.toHex(asdu, ":"));
            informations.add(new ChannelScanInfo(channelAddress.toString(), description.toString(), null, null));
        }
        return informations;
    }

    private void ensureOpenConnection() throws ConnectionException {
        if (!knxNetworkLink.isOpen()) {
            throw new ConnectionException();
        }
    }

    private Record read(KnxGroupDP groupDP, int timeout) throws ConnectionException, KNXException {
        ensureOpenConnection();
        Record record = null;
        setResponseTimeout(timeout);

        try {
            groupDP.getKnxValue().setDPTValue(processCommunicator.read(groupDP));

            record = new Record(groupDP.getKnxValue().getOpenMucValue(), System.currentTimeMillis());
        } catch (InterruptedException e) {
            throw new ConnectionException("Read failed for group address " + groupDP.getMainAddress(), e);
        } catch (final KNXLinkClosedException e) {
            throw new ConnectionException(e);
        }

        return record;
    }

    public boolean write(KnxGroupDP groupDP, int timeout) throws ConnectionException {
        ensureOpenConnection();
        setResponseTimeout(timeout);

        try {
            KnxValue value = groupDP.getKnxValue();
            processCommunicator.write(groupDP, value.getDPTValue());
            return true;
        } catch (final KNXLinkClosedException e) {
            throw new ConnectionException(e);
        } catch (KNXException e) {
            logger.warn("write failed");
            return false;
        }
    }

    private void setResponseTimeout(int timeout) {
        if (responseTimeout != timeout) {
            responseTimeout = timeout;
            int timeoutSec = (timeout / 1000);
            if (timeoutSec > 0) {
                processCommunicator.setResponseTimeout(timeoutSec);
            }
            else {
                processCommunicator.setResponseTimeout(DEFAULT_TIMEOUT);
            }
        }
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        return listKnownChannels();
    }

    @Override
    public void disconnect() {
        logger.debug("disconnecting from " + name);
        processCommunicator.detach();
        knxNetworkLink.close();
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer container : containers) {
            try {

                KnxGroupDP groupDP = null;
                if (container.getChannelHandle() == null) {
                    groupDP = createKnxGroupDP(container.getChannelAddress());
                    logger.debug("New datapoint: " + groupDP);
                    container.setChannelHandle(groupDP);
                }
                else {
                    groupDP = (KnxGroupDP) container.getChannelHandle();
                }

                Record record = read(groupDP, KnxDriver.timeout);
                container.setRecord(record);
            } catch (ArgumentSyntaxException e) {
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
                logger.error(e.getMessage(), "Channel-ID: " + container.getChannel().getId());
            } catch (KNXTimeoutException e1) {
                logger.debug(e1.getMessage());
                container.setRecord(new Record(null, System.currentTimeMillis(), Flag.TIMEOUT));
            } catch (KNXException e) {
                logger.warn(e.getMessage());
            }

        }

        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        for (ChannelRecordContainer container : containers) {
            if (container.getChannelHandle() == null) {
                try {
                    container.setChannelHandle(createKnxGroupDP(container.getChannelAddress()));
                } catch (ArgumentSyntaxException e) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
                    logger.error(e.getMessage() + "Channel-ID: " + container.getChannel().getId());
                } catch (KNXException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
        logger.info("Start listening for ", containers.size(), " channels");
        processListener.registerOpenMucListener(containers, listener);
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelValueContainer container : containers) {
            KnxGroupDP groupDP = null;
            try {
                if (container.getChannelHandle() == null) {
                    groupDP = createKnxGroupDP(container.getChannelAddress());
                    logger.debug("New datapoint: " + groupDP);
                    container.setChannelHandle(groupDP);
                }
                else {
                    groupDP = (KnxGroupDP) container.getChannelHandle();
                }

                groupDP.getKnxValue().setOpenMucValue(container.getValue());
                boolean state = write(groupDP, KnxDriver.timeout);
                if (state) {
                    container.setFlag(Flag.VALID);
                }
                else {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                }
            } catch (ArgumentSyntaxException e) {
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
                logger.error(e.getMessage());
            } catch (KNXException e) {
                logger.warn(e.getMessage());
            }
        }
        return null;
    }

    private static KnxGroupDP createKnxGroupDP(String channelAddress) throws KNXException, ArgumentSyntaxException {
        String[] address = channelAddress.split(":");
        KnxGroupDP dp = null;

        if (address.length != 2 && address.length != 4) {
            throw new ArgumentSyntaxException("Channel address has a wrong format. ");
        }
        else {
            GroupAddress main = new GroupAddress(address[0]);
            String dptID = address[1];
            dp = new KnxGroupDP(main, channelAddress, dptID);
            if (address.length == 4) {
                boolean AET = address[2].equals("1");
                String value = address[3];
            }
        }
        return dp;
    }

}
