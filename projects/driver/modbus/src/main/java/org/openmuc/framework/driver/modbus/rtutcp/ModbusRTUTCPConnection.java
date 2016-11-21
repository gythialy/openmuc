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
package org.openmuc.framework.driver.modbus.rtutcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
import org.openmuc.framework.driver.modbus.ModbusChannelGroup;
import org.openmuc.framework.driver.modbus.ModbusConnection;
import org.openmuc.framework.driver.modbus.rtutcp.bonino.ModbusRTUTCPTransaction;
import org.openmuc.framework.driver.modbus.rtutcp.bonino.RTUTCPMasterConnection;
import org.openmuc.framework.driver.modbus.tcp.ModbusTCPDeviceAddress;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;

/**
 * TODO
 */
public class ModbusRTUTCPConnection extends ModbusConnection {

    private final static Logger logger = LoggerFactory.getLogger(ModbusRTUTCPConnection.class);

    private RTUTCPMasterConnection connection;
    private ModbusRTUTCPTransaction transaction;

    public ModbusRTUTCPConnection(String deviceAddress, int timeoutInSeconds) {

        super();
        ModbusTCPDeviceAddress address = new ModbusTCPDeviceAddress(deviceAddress);
        try {
            connection = new RTUTCPMasterConnection(InetAddress.getByName(address.getIp()), address.getPort());
            connect();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        logger.info("Modbus Device: " + deviceAddress + " connected");
    }

    @Override
    public void connect() throws Exception {

        if (connection != null && !connection.isConnected()) {
            connection.connect();
            transaction = new ModbusRTUTCPTransaction(connection);
            setTransaction(transaction);
            if (!connection.isConnected()) {
                throw new Exception("unable to connect");
            }
        }
    }

    @Override
    public void disconnect() {
        logger.info("Disconnect Modbus TCP device");
        if (connection != null && connection.isConnected()) {
            connection.close();
            transaction = null;
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        // reads channels one by one
        if (samplingGroup.isEmpty()) {
            for (ChannelRecordContainer container : containers) {
                long receiveTime = System.currentTimeMillis();
                ModbusChannel channel = getModbusChannel(container.getChannelAddress(), EAccess.READ);
                Value value;
                try {
                    value = readChannel(channel);
                    container.setRecord(new Record(value, receiveTime));
                } catch (ModbusIOException e) {
                    logger.error("Unable to read channel: " + container.getChannelAddress(), e);
                    disconnect();
                    throw new ConnectionException("Try to reconnect to solve ModbusIOException");
                } catch (ModbusException e) {
                    logger.error("Unable to read channel: " + container.getChannelAddress(), e);
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
                } catch (Exception e) {
                    // catch all possible exceptions and provide info about the channel
                    logger.error("Unable to read channel: " + container.getChannelAddress(), e);
                    container.setRecord(new Record(Flag.UNKNOWN_ERROR));
                }
            }
        }
        // reads whole samplingGroup at once
        else {
            // TODO test channel group
            logger.warn("Reading samplingGroup is not tested yet!");
            readChannelGroupHighLevel(containers, containerListHandle, samplingGroup);
        }

        return null;
    }

    private Object readChannelGroupHighLevel(List<ChannelRecordContainer> containers, Object containerListHandle,
            String samplingGroup) {

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
            ArrayList<ModbusChannel> channelList = new ArrayList<>();
            for (ChannelRecordContainer container : containers) {
                channelList.add(getModbusChannel(container.getChannelAddress(), EAccess.READ));
            }
            channelGroup = new ModbusChannelGroup(samplingGroup, channelList);
        }

        // read all channels of the group
        try {
            readChannelGroup(channelGroup, containers);

        } catch (ModbusException e) {
            e.printStackTrace();

            // set channel values and flag, otherwise the datamanager will throw a null pointer exception
            // and the framework collapses.
            setChannelsWithErrorFlag(containers);
        }

        // logger.debug("### readChannelGroup duration in ms = " + ((new Date().getTime()) - startTime));

        return channelGroup;
    }

    // private ModbusChannel getModbusChannel(String channelAddress, EAccess access) {
    //
    // ModbusChannel modbusChannel = null;
    //
    // // check if the channel object already exists in the list
    // if (modbusChannels.containsKey(channelAddress)) {
    // modbusChannel = modbusChannels.get(channelAddress);
    //
    // // if the channel object exists the access flag might has to be updated
    // // (this is case occurs when the channel is readable and writable)
    // if (!modbusChannel.getAccessFlag().equals(access)) {
    // modbusChannel.update(access);
    // }
    // }
    // // create a new channel object
    // else {
    // modbusChannel = new ModbusChannel(channelAddress, access);
    // modbusChannels.put(channelAddress, modbusChannel);
    // }
    //
    // return modbusChannel;
    //
    // }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelValueContainer container : containers) {

            ModbusChannel modbusChannel = getModbusChannel(container.getChannelAddress(), EAccess.WRITE);
            if (modbusChannel != null) {
                try {
                    writeChannel(modbusChannel, container.getValue());
                    container.setFlag(Flag.VALID);
                } catch (ModbusException modbusException) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    modbusException.printStackTrace();
                    throw new ConnectionException(
                            "Unable to write data on channel address: " + container.getChannelAddress());
                } catch (Exception e) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    e.printStackTrace();
                    logger.error("Unable to write data on channel address: " + container.getChannelAddress());
                }
            }
            else {
                // TODO
                container.setFlag(Flag.UNKNOWN_ERROR);
                logger.error("Unable to write data on channel address: " + container.getChannelAddress()
                        + "modbusChannel = null");
            }
        }

        return null;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
