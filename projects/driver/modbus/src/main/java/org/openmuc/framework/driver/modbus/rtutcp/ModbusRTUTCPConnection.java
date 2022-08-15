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
package org.openmuc.framework.driver.modbus.rtutcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
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

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;

/**
 * TODO
 */
public class ModbusRTUTCPConnection extends ModbusConnection {

    private static final Logger logger = LoggerFactory.getLogger(ModbusRTUTCPConnection.class);

    private RTUTCPMasterConnection connection;
    private ModbusRTUTCPTransaction transaction;

    public ModbusRTUTCPConnection(String deviceAddress, int timeoutMs) {

        super();
        ModbusTCPDeviceAddress address = new ModbusTCPDeviceAddress(deviceAddress);
        try {
            connection = new RTUTCPMasterConnection(InetAddress.getByName(address.getIp()), address.getPort());
            connection.setTimeout(timeoutMs);
            connect();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        logger.info("Modbus Device: " + deviceAddress + " connected");
    }

    @Override
    public void connect() throws ConnectionException {

        if (connection != null && !connection.isConnected()) {
            try {
                connection.connect();
            } catch (Exception e) {
                throw new ConnectionException(e);
            }
            transaction = new ModbusRTUTCPTransaction(connection);
            setTransaction(transaction);
            if (!connection.isConnected()) {
                throw new ConnectionException("unable to connect");
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

                    if (logger.isTraceEnabled()) {
                        logger.trace("Value of response: " + value.toString());
                    }

                    container.setRecord(new Record(value, receiveTime));

                } catch (ModbusIOException e) {
                    logger.error("ModbusIOException while reading channel:" + channel.getChannelAddress(), e);
                    disconnect();
                    throw new ConnectionException("Try to solve issue with reconnect.");

                } catch (ModbusException e) {
                    logger.error("ModbusException while reading channel: " + channel.getChannelAddress(), e);
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));

                } catch (Exception e) {
                    // catch all possible exceptions and provide info about the channel
                    logger.error("Exception while reading channel: " + channel.getChannelAddress(), e);
                    container.setRecord(new Record(Flag.UNKNOWN_ERROR));
                }
                if (!connection.isConnected()) {
                    throw new ConnectionException("Lost connection.");
                }
            }
        }
        // reads whole samplingGroup at once
        else {
            readChannelGroupHighLevel(containers, containerListHandle, samplingGroup);
            if (!connection.isConnected()) {
                throw new ConnectionException("Lost connection.");
            }
        }

        return null;
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelValueContainer container : containers) {

            ModbusChannel channel = getModbusChannel(container.getChannelAddress(), EAccess.WRITE);

            try {
                writeChannel(channel, container.getValue());
                container.setFlag(Flag.VALID);

            } catch (ModbusIOException e) {
                logger.error("ModbusIOException while writing channel:" + channel.getChannelAddress(), e);
                disconnect();
                throw new ConnectionException("Try to solve issue with reconnect.");

            } catch (ModbusException e) {
                logger.error("ModbusException while writing channel: " + channel.getChannelAddress(), e);
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);

            } catch (Exception e) {
                logger.error("Exception while writing channel: " + channel.getChannelAddress(), e);
                container.setFlag(Flag.UNKNOWN_ERROR);
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
