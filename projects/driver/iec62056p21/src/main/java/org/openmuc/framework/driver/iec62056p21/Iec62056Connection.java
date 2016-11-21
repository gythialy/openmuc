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
package org.openmuc.framework.driver.iec62056p21;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.j62056.DataSet;

public class Iec62056Connection implements Connection {

    private final org.openmuc.j62056.Connection connection;

    public Iec62056Connection(org.openmuc.j62056.Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ScanException, ConnectionException {

        List<DataSet> dataSets;
        try {
            dataSets = connection.read();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new ScanException(e1);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new ScanException(e);
        }

        if (dataSets == null) {
            throw new ScanException("Read timeout.");
        }

        List<ChannelScanInfo> scanInfos = new ArrayList<>(dataSets.size());

        for (DataSet dataSet : dataSets) {
            try {
                Double.parseDouble(dataSet.getValue());
                scanInfos.add(new ChannelScanInfo(dataSet.getId(), "", ValueType.DOUBLE, null));
            } catch (NumberFormatException e) {
                scanInfos.add(new ChannelScanInfo(dataSet.getId(), "", ValueType.STRING, dataSet.getValue().length()));
            }

        }

        return scanInfos;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        List<DataSet> dataSets;
        try {
            dataSets = connection.read();
        } catch (IOException e) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.DRIVER_ERROR_READ_FAILURE));
            }
            return null;
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException("Read timed out: " + e.getMessage());
        }

        if (dataSets == null) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.TIMEOUT));
            }
            return null;
        }

        long time = System.currentTimeMillis();
        for (ChannelRecordContainer container : containers) {
            for (DataSet dataSet : dataSets) {
                if (dataSet.getId().equals(container.getChannelAddress())) {
                    String value = dataSet.getValue();
                    if (value != null) {
                        try {
                            container.setRecord(
                                    new Record(new DoubleValue(Double.parseDouble(dataSet.getValue())), time));
                        } catch (NumberFormatException e) {
                            container.setRecord(new Record(new StringValue(dataSet.getValue()), time));
                        }
                    }
                    break;
                }
            }
        }

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
        connection.close();
    }
}
