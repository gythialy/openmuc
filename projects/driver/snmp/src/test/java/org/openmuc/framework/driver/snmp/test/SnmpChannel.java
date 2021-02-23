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
package org.openmuc.framework.driver.snmp.test;

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FutureValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.dataaccess.ReadRecordContainer;
import org.openmuc.framework.dataaccess.RecordListener;
import org.openmuc.framework.dataaccess.WriteValueContainer;

public class SnmpChannel implements Channel {

    private String id;
    private String address;
    private String description;
    private String unit;
    private ValueType valueType;
    private int samplingInterval;
    private int samplingTimeOffset;
    private int samplingTimeout;
    private String deviceAddress;
    private String settings;

    SnmpChannel() {
    }

    SnmpChannel(String deviceAddress, String address) {
        this.address = address;
        this.deviceAddress = deviceAddress;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getChannelAddress() {
        return address;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getSettings() {
        return settings;
    }

    @Override
    public String getLoggingSettings() {
        return "";
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public int getSamplingInterval() {
        return samplingInterval;
    }

    @Override
    public int getSamplingTimeOffset() {
        return samplingTimeOffset;
    }

    @Override
    public int getSamplingTimeout() {
        return samplingTimeout;
    }

    @Override
    public int getLoggingInterval() {
        return 0;
    }

    @Override
    public int getLoggingTimeOffset() {
        return 0;
    }

    @Override
    public String getDriverName() {
        return null;
    }

    @Override
    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String getDeviceName() {
        return null;
    }

    @Override
    public String getDeviceDescription() {
        return null;
    }

    @Override
    public ChannelState getChannelState() {
        return null;
    }

    @Override
    public DeviceState getDeviceState() {
        return null;
    }

    @Override
    public void addListener(RecordListener listener) {
    }

    @Override
    public void removeListener(RecordListener listener) {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public Record getLatestRecord() {
        return null;
    }

    @Override
    public void setLatestRecord(Record record) {
    }

    @Override
    public Flag write(Value value) {
        return null;
    }

    @Override
    public void writeFuture(List<FutureValue> values) {
    }

    @Override
    public WriteValueContainer getWriteContainer() {
        return null;
    }

    @Override
    public Record read() {
        return null;
    }

    @Override
    public ReadRecordContainer getReadContainer() {
        return null;
    }

    @Override
    public Record getLoggedRecord(long time) throws DataLoggerNotAvailableException, IOException {
        return null;
    }

    @Override
    public List<Record> getLoggedRecords(long startTime) throws DataLoggerNotAvailableException, IOException {
        return null;
    }

    @Override
    public List<Record> getLoggedRecords(long startTime, long endTime)
            throws DataLoggerNotAvailableException, IOException {
        return null;
    }

    @Override
    public double getScalingFactor() {
        return 0;
    }

}
