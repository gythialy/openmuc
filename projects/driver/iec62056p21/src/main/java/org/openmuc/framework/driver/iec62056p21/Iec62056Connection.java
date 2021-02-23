/*
 * Copyright 2011-2021 Fraunhofer ISE
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
import org.openmuc.j62056.DataMessage;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.Iec21Port;
import org.openmuc.j62056.Iec21Port.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Iec62056Connection implements Connection {

    private Iec21Port iec21Port;
    private int retries = 0;

    private static final Logger logger = LoggerFactory.getLogger(Iec62056Connection.class);
    private final boolean readStandard;
    private final Builder configuredBuilder;
    private final String requestStartCharacter;

    public Iec62056Connection(Builder configuredBuilder, int retries, boolean readStandard,
            String requestStartCharacter) throws ConnectionException {
        this.configuredBuilder = configuredBuilder;
        this.readStandard = readStandard;
        this.requestStartCharacter = requestStartCharacter;

        if (retries > 0) {
            this.retries = retries;
        }

        try {
            iec21Port = configuredBuilder.buildAndOpen();
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
        try {
            iec21Port.read();
        } catch (IOException e) {
            iec21Port.close();
            throw new ConnectionException("IOException trying to read meter: " + e.getMessage(), e);
        }

        sleep(5000);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ScanException, ConnectionException {
        List<DataSet> dataSets;
        DataMessage dataMessage;

        try {
            dataMessage = iec21Port.read();
        } catch (IOException e) {
            throw new ScanException(e);
        }
        dataSets = dataMessage.getDataSets();

        if (dataSets == null) {
            throw new ScanException("Read timeout.");
        }

        List<ChannelScanInfo> scanInfos = new ArrayList<>(dataSets.size());

        for (DataSet dataSet : dataSets) {
            try {
                Double.parseDouble(dataSet.getValue());
                scanInfos.add(new ChannelScanInfo(dataSet.getAddress(), "", ValueType.DOUBLE, null));
            } catch (NumberFormatException e) {
                scanInfos.add(
                        new ChannelScanInfo(dataSet.getAddress(), "", ValueType.STRING, dataSet.getValue().length()));
            }

        }

        return scanInfos;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        List<DataSet> dataSets = new ArrayList<>();
        dataSets.addAll(read(containers));

        if (readStandard) {
            configuredBuilder.setRequestStartCharacters("/?");
            setPort(configuredBuilder);
            sleep(500);
            dataSets.addAll(read(containers));
            configuredBuilder.setRequestStartCharacters(requestStartCharacter);
            setPort(configuredBuilder);
        }

        setRecords(containers, dataSets);
        return null;
    }

    private List<DataSet> read(List<ChannelRecordContainer> containers) {
        List<DataSet> dataSetsRet = new ArrayList<>();
        DataMessage dataMessage;
        for (int i = 0; i <= retries; ++i) {
            try {
                dataMessage = iec21Port.read();
                List<DataSet> dataSets = dataMessage.getDataSets();
                if (dataSetsRet != null) {
                    i = retries;
                    dataSetsRet = dataSets;
                }
            } catch (IOException e) {
                if (i >= retries) {
                    for (ChannelRecordContainer container : containers) {
                        container.setRecord(new Record(Flag.DRIVER_ERROR_READ_FAILURE));
                    }
                }
            }
        }
        return dataSetsRet;
    }

    public static void setRecords(List<ChannelRecordContainer> containers, List<DataSet> dataSets) {
        long time = System.currentTimeMillis();

        for (ChannelRecordContainer container : containers) {
            for (DataSet dataSet : dataSets) {
                if (dataSet.getAddress().equals(container.getChannelAddress())) {
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
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        Iec62056Listener iec62056Listener = new Iec62056Listener();
        iec62056Listener.registerOpenMucListener(containers, listener);
        try {
            iec21Port.listen(iec62056Listener);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        if (iec21Port != null) {
            iec21Port.close();
        }
    }

    private void setPort(Builder configuredBuilder) throws ConnectionException {
        if (!iec21Port.isClosed()) {
            iec21Port.close();
        }
        try {
            iec21Port = configuredBuilder.buildAndOpen();
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    private void sleep(int sleeptime) {
        try { // FIXME: Sleep to avoid to early read after connection. Meters have some delay.
            Thread.sleep(sleeptime);
        } catch (InterruptedException e1) {
            logger.error(e1.getMessage());
        }
    }

}
