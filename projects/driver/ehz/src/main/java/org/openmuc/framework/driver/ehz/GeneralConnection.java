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

import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frederic Robra
 * 
 */
public abstract class GeneralConnection implements org.openmuc.framework.driver.spi.Connection {

    protected static Logger logger = LoggerFactory.getLogger(GeneralConnection.class);
    protected String name;

    protected final static int timeout = 10000;

    public abstract void close();

    public abstract void read(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException;

    public abstract List<ChannelScanInfo> listChannels(int timeout);

    public abstract boolean isWorking();

    protected void handleChannelRecordContainer(List<ChannelRecordContainer> containers, Map<String, Double> values,
            long timestamp) {
        for (ChannelRecordContainer container : containers) {
            String address = container.getChannelAddress();
            if (values.containsKey(address)) {
                Value value = new DoubleValue(values.get(address));
                Record record = new Record(value, timestamp, Flag.VALID);
                container.setRecord(record);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openmuc.framework.driver.spi.DriverService#scanForChannels(org.openmuc.framework.driver.spi.DeviceConnection,
     * int)
     */
    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        return listChannels(20000);
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        read(containers, timeout);
        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        close();
    }

}
