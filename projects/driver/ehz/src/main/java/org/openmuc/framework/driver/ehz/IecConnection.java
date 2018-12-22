/*
 * Copyright 2011-17 Fraunhofer ISE
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
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.ehz.iec62056_21.DataSet;
import org.openmuc.framework.driver.ehz.iec62056_21.IecReceiver;
import org.openmuc.framework.driver.ehz.iec62056_21.ModeDMessage;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IecConnection extends GeneralConnection {

    private IecReceiver receiver;

    private static Logger logger = LoggerFactory.getLogger(IecConnection.class);

    public IecConnection(String deviceAddress, int timeout) throws ConnectionException {
        try {
            receiver = new IecReceiver(deviceAddress);
        } catch (Exception e) {
            throw new ConnectionException("serial port not found");
        }
    }

    @Override
    public void disconnect() {
        receiver.close();
    }

    @Override
    public void read(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException {
        logger.trace("reading channels");
        long timestamp = System.currentTimeMillis();
        try {
            byte[] frame = receiver.receiveMessage(timeout);
            ModeDMessage message = ModeDMessage.parse(frame);
            List<String> dataSets = message.getDataSets();

            Map<String, Value> values = new LinkedHashMap<>();
            for (String ds : dataSets) {
                DataSet dataSet = new DataSet(ds);
                String address = dataSet.getAddress();
                Value value = dataSet.parseValueAsDouble();
                values.put(address, value);
                logger.trace("{} = {}", address, value);
            }

            handleChannelRecordContainer(containers, values, timestamp);
        } catch (IOException e) {
            logger.error("read failed", e);
            disconnect();
            throw new ConnectionException(e);
        } catch (ParseException e) {
            logger.error("parsing failed", e);
        }
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(int timeout) {
        List<ChannelScanInfo> channelInfos = new LinkedList<>();

        logger.debug("scanning channels");
        try {
            byte[] frame = receiver.receiveMessage(timeout);
            ModeDMessage message = ModeDMessage.parse(frame);
            List<String> dataSets = message.getDataSets();

            for (String data : dataSets) {
                DataSet dataSet = new DataSet(data);
                String channelAddress = dataSet.getAddress();
                String description = "Current value: " + dataSet.parseValueAsDouble() + dataSet.getUnit();
                ValueType valueType = ValueType.DOUBLE;
                Integer valueTypeLength = null;
                Boolean readable = true;
                Boolean writable = false;
                ChannelScanInfo channelInfo = new ChannelScanInfo(channelAddress, description, valueType,
                        valueTypeLength, readable, writable);
                channelInfos.add(channelInfo);
            }

        } catch (ParseException | IOException e) {
            logger.warn("read failed", e);
        }
        return channelInfos;
    }

    @Override
    public boolean works() {
        try {
            byte[] frame = receiver.receiveMessage(1000);
            ModeDMessage.parse(frame);
        } catch (IOException | ParseException e) {
            return false;
        }
        return true;

    }

}
