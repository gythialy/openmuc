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

package org.openmuc.framework.driver.ehz;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.ehz.iec62056_21.DataSet;
import org.openmuc.framework.driver.ehz.iec62056_21.IecReceiver;
import org.openmuc.framework.driver.ehz.iec62056_21.ModeDMessage;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;

/**
 * @author Frederic Robra
 * 
 */
public class IecConnection extends GeneralConnection {

    private IecReceiver receiver;

    public IecConnection(String deviceAddress, int timeout) throws ConnectionException {
        name = "IEC - " + deviceAddress + " - ";
        try {
            receiver = new IecReceiver(deviceAddress);
        } catch (Exception e) {
            throw new ConnectionException(name + "serial port not found");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#close()
     */
    @Override
    public void close() {
        receiver.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#read(java.util.List, int)
     */
    @Override
    public void read(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException {
        logger.trace(name + "reading channels");
        try {
            long timestamp = System.currentTimeMillis();

            byte[] frame = receiver.receiveMessage(timeout);
            ModeDMessage message = new ModeDMessage(frame);
            message.parse();
            List<String> dataSets = message.getDataSets();

            Map<String, Double> values = new LinkedHashMap<>();
            for (String data : dataSets) {
                DataSet dataSet = new DataSet(data);
                String address = dataSet.getAddress();
                double value = dataSet.getVal();
                values.put(address, value);
                logger.trace(name + address + " = " + value);
            }

            handleChannelRecordContainer(containers, values, timestamp);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(name + "read failed");
            close();
            throw new ConnectionException(e);
        } catch (ParseException e) {
            logger.error(name + "parsing failed");
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#listChannels(int)
     */
    @Override
    public List<ChannelScanInfo> listChannels(int timeout) {
        List<ChannelScanInfo> channelInfos = new LinkedList<>();

        logger.debug(name + "scanning channels");
        try {
            byte[] frame = receiver.receiveMessage(timeout);
            ModeDMessage message = new ModeDMessage(frame);
            message.parse();
            List<String> dataSets = message.getDataSets();

            for (String data : dataSets) {
                DataSet dataSet = new DataSet(data);
                String channelAddress = dataSet.getAddress();
                String description = "Current value: " + dataSet.getVal() + dataSet.getUnit();
                ValueType valueType = ValueType.DOUBLE;
                Integer valueTypeLength = null;
                Boolean readable = true;
                Boolean writable = false;
                ChannelScanInfo channelInfo = new ChannelScanInfo(channelAddress, description, valueType,
                        valueTypeLength, readable, writable);
                channelInfos.add(channelInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.warn(name + "read failed");
        }
        return channelInfos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#isWorking()
     */
    @Override
    public boolean isWorking() {
        try {
            byte[] frame = receiver.receiveMessage(1000);
            ModeDMessage message = new ModeDMessage(frame);
            message.parse();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
