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

package org.openmuc.framework.driver.modbus;

import net.wimpi.modbus.ModbusException;
import org.openmuc.framework.config.*;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
import org.openmuc.framework.driver.modbus.rtu.ModbusConfigurationException;
import org.openmuc.framework.driver.modbus.rtu.ModbusRTUDevice;
import org.openmuc.framework.driver.modbus.tcp.ModbusTCPDevice;
import org.openmuc.framework.driver.spi.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public final class ModbusDriver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(ModbusDriver.class);

    private final static DriverInfo info = new DriverInfo("modbus",
                                                          "ModbusTCP and ModbusRTU are supported.",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?");

    private final static int timeout = 10000;

    /**
     * List do manage Channel Objects to avoid to check the syntax of each channel address for every read or write
     */
    private Hashtable<String, ModbusChannel> modbusChannels;

    protected void activate(ComponentContext context) {
        modbusChannels = new Hashtable<String, ModbusChannel>();
        logger.debug("Activated Modbus Driver");
    }

    protected void deactivate(ComponentContext context) {
        // TODO Disconnect of all devices
        // TODO maintain an internal device list
        logger.debug("Deactivated Modbus Driver");
    }

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {
        throw new UnsupportedOperationException();

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ConnectionException {

        ModbusDevice device = null;

        if (settings.equals("")) {
            throw new ConnectionException(
                    "no device settings found in config. Please specify settings.");
        } else {
            String[] settingsArray = settings.split(":");
            String mode = settingsArray[0];
            if (mode.equals("RTU")) {
                try {
                    device = new ModbusRTUDevice(deviceAddress, settingsArray, timeout);
                }
                catch (ModbusConfigurationException e) {
                    e.printStackTrace();
                    throw new ConnectionException(
                            "Unable to connect due to wrong configuration. Check configuration file.");
                }
            } else if (mode.equals("TCP")) {
                device = new ModbusTCPDevice(deviceAddress);
            } else {
                throw new ConnectionException("unknown mode.");
            }

        }

        device.connect();

        return device;
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        ((ModbusDevice) connection.getConnectionHandle()).disconnect();
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException {

        // TODO add Throw ConnectionException --> ModbusIOException?

        ModbusDevice device = (ModbusDevice) connection.getConnectionHandle();

        if (samplingGroup.isEmpty()) {
            // reads channels one by one
            return readChannels(device, containers, timeout);
        } else {
            // reads whole samplingGroup at once
            return readChannelGroup(device,
                                    containers,
                                    containerListHandle,
                                    samplingGroup,
                                    timeout);
        }

    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        ModbusDevice device = (ModbusDevice) connection.getConnectionHandle();

        for (ChannelValueContainer container : containers) {

            ModbusChannel modbusChannel = getModbusTcpChannel(container.getChannelAddress(),
                                                              EAccess.WRITE);
            if (modbusChannel != null) {
                try {
                    device.writeChannel(modbusChannel, container.getValue());
                    container.setFlag(Flag.VALID); /* No Exception occured. */
                }
                catch (ModbusException modbusException) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    modbusException.printStackTrace();
                    throw new ConnectionException("Unable to write data on channel address: "
                                                  + container.getChannelAddress());
                }
                catch (Exception e) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    e.printStackTrace();
                    logger.error("Unable to write data on channel address: "
                                 + container.getChannelAddress());
                }
            } else {
                // TODO
                container.setFlag(Flag.UNKNOWN_ERROR);
                logger.error("Unable to write data on channel address: "
                             + container.getChannelAddress()
                             + "ModbusTcpChannel = null");
            }
        }

        return null;

    }

    /**
     * Reads all requested channels one by one.
     *
     * @param device
     * @param containers
     * @param timeout
     * @return Object
     */
    private Object readChannels(ModbusDevice device,
                                List<ChannelRecordContainer> containers,
                                int timeout) {

        new Date().getTime();

        for (ChannelRecordContainer container : containers) {
            long receiveTime = System.currentTimeMillis();
            ModbusChannel channel = getModbusTcpChannel(container.getChannelAddress(),
                                                        EAccess.READ);
            Value value;
            try {
                value = device.readChannel(channel, timeout);

                logger.debug("{}: value = '{}'", channel.getChannelAddress(), value.toString());

                container.setRecord(new Record(value, receiveTime));
            }
            catch (ModbusException e) {
                e.printStackTrace();
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
            }
        }

        // logger.debug("### readChannels duration in ms = " + ((new Date().getTime()) - startTime));

        return null;
    }

    /**
     * Read the all channels of the samplingGroup at once.
     *
     * @param device
     * @param containers
     * @param containerListHandle
     * @param samplingGroup
     * @param timeout
     * @return Object
     */
    private Object readChannelGroup(ModbusDevice device, List<ChannelRecordContainer> containers,
                                    Object containerListHandle, String samplingGroup, int timeout) {

        new Date().getTime();

        // NOTE: containerListHandle is null if something changed in configuration!!!

        ModbusChannelGroup channelGroup = null;

        // use existing channelGroup
        if (containerListHandle != null) {
            if (containerListHandle instanceof ModbusChannelGroup) {
                channelGroup = (ModbusChannelGroup) containerListHandle;
            }
        }

        // create new channelGroup
        if (channelGroup == null) {
            ArrayList<ModbusChannel> channelList = new ArrayList<ModbusChannel>();
            for (ChannelRecordContainer container : containers) {
                channelList.add(getModbusTcpChannel(container.getChannelAddress(), EAccess.READ));
            }
            channelGroup = new ModbusChannelGroup(samplingGroup, channelList);
        }

        // read all channels of the group
        try {
            device.readChannelGroup(channelGroup, containers, timeout);

        }
        catch (ModbusException e) {
            e.printStackTrace();

            // set channel values and flag, otherwise the datamanager will throw a null pointer exception
            // and the framework collapses.
            device.setChannelsWithErrorFlag(containers);
        }

        // logger.debug("### readChannelGroup duration in ms = " + ((new Date().getTime()) - startTime));

        return channelGroup;
    }

    private ModbusChannel getModbusTcpChannel(String channelAddress, EAccess access) {

        ModbusChannel modbusChannel = null;

        // check if the channel object already exists in the list
        if (modbusChannels.containsKey(channelAddress)) {
            modbusChannel = modbusChannels.get(channelAddress);

            // if the channel object exists the access flag might has to be updated
            // (this is case occurs when the channel is readable and writable)
            if (!modbusChannel.getAccessFlag().equals(access)) {
                modbusChannel.update(access);
            }
        }
        // create a new channel object
        else {
            modbusChannel = new ModbusChannel(channelAddress, access);
            modbusChannels.put(channelAddress, modbusChannel);
        }

        return modbusChannel;

    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        // not supported
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        // not supported
        throw new UnsupportedOperationException();
    }
}
