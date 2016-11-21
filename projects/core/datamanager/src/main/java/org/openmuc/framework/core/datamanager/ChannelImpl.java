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

package org.openmuc.framework.core.datamanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.TypeConversionException;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.dataaccess.ReadRecordContainer;
import org.openmuc.framework.dataaccess.RecordListener;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChannelImpl implements Channel {

    private final static Logger logger = LoggerFactory.getLogger(ChannelImpl.class);

    volatile Record latestRecord;
    ChannelRecordContainerImpl driverChannel;
    volatile ChannelConfigImpl config;
    ChannelCollection samplingCollection;
    ChannelCollection loggingCollection;
    private final Set<RecordListener> listeners = new LinkedHashSet<>();
    private final DataManager dataManager;
    volatile Object handle;
    private Timer timer = null;
    private List<Record> futureValues;

    public ChannelImpl(DataManager dataManager, ChannelConfigImpl config, ChannelState initState, Flag initFlag,
            long currentTime, List<LogChannel> logChannels) {
        this.dataManager = dataManager;
        this.config = config;
        this.futureValues = new ArrayList<>();

        if (config.disabled) {
            config.state = ChannelState.DISABLED;
            latestRecord = new Record(Flag.DISABLED);
        }
        else if (!config.isListening() && config.samplingInterval < 0) {
            config.state = initState;
            latestRecord = new Record(Flag.SAMPLING_AND_LISTENING_DISABLED);
        }
        else {
            config.state = initState;
            latestRecord = new Record(null, null, initFlag);
        }

        if (config.loggingInterval != ChannelConfig.LOGGING_INTERVAL_DEFAULT) {
            dataManager.addToLoggingCollections(this, currentTime);
            logChannels.add(config);
        }
    }

    @Override
    public String getId() {
        return config.id;
    }

    @Override
    public String getChannelAddress() {
        return config.channelAddress;
    }

    @Override
    public String getDescription() {
        return config.description;
    }

    @Override
    public String getUnit() {
        return config.unit;
    }

    @Override
    public ValueType getValueType() {
        return config.valueType;
    }

    @Override
    public double getScalingFactor() {
        if (config.scalingFactor == null) {
            return 1;
        }
        return config.scalingFactor;
    }

    @Override
    public int getSamplingInterval() {
        return config.samplingInterval;
    }

    @Override
    public int getSamplingTimeOffset() {
        return config.samplingTimeOffset;
    }

    @Override
    public int getLoggingInterval() {
        return config.loggingInterval;
    }

    @Override
    public int getLoggingTimeOffset() {
        return config.loggingTimeOffset;
    }

    @Override
    public String getDriverName() {
        return config.deviceParent.driverParent.id;
    }

    @Override
    public String getDeviceAddress() {
        return config.deviceParent.deviceAddress;
    }

    @Override
    public String getDeviceName() {
        return config.deviceParent.id;
    }

    @Override
    public String getDeviceDescription() {
        return config.deviceParent.description;
    }

    @Override
    public ChannelState getChannelState() {
        return config.state;
    }

    @Override
    public DeviceState getDeviceState() {
        return config.deviceParent.device.getState();
    }

    @Override
    public void addListener(RecordListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(RecordListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public Record getLatestRecord() {
        return latestRecord;
    }

    @Override
    public void setLatestRecord(Record record) {
        setNewRecord(record);
    }

    @Override
    public Record getLoggedRecord(long timestamp) throws DataLoggerNotAvailableException, IOException {
        List<Record> records = dataManager.getDataLogger().getRecords(config.id, timestamp, timestamp);
        if (records.size() > 0) {
            return records.get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public List<Record> getLoggedRecords(long startTime) throws DataLoggerNotAvailableException, IOException {
        return dataManager.getDataLogger().getRecords(config.id, startTime, System.currentTimeMillis());
    }

    @Override
    public List<Record> getLoggedRecords(long startTime, long endTime)
            throws DataLoggerNotAvailableException, IOException {
        Long currentTime = System.currentTimeMillis();
        List<Record> toReturn = dataManager.getDataLogger().getRecords(config.id, startTime, endTime);
        for (Record record : futureValues) {
            if (record.getTimestamp() >= currentTime) {
                if (record.getTimestamp() <= endTime) {
                    toReturn.add(record);
                }
                else {
                    break;
                }
            }
        }
        return toReturn;
    }

    Record setNewRecord(Record record) {

        Record convertedRecord = null;

        if (record.getFlag() == Flag.VALID) {
            Double scalingFactor = config.scalingFactor;
            Double scalingOffset = config.valueOffset;

            if (scalingFactor != null) {
                try {
                    record = new Record(new DoubleValue(record.getValue().asDouble() * scalingFactor),
                            record.getTimestamp(), record.getFlag());
                } catch (TypeConversionException e) {
                    logger.error("Unable to apply scaling factor to channel " + config.id
                            + " because a TypeConversionError occured.", e);
                }
            }
            if (scalingOffset != null) {
                try {
                    record = new Record(new DoubleValue(record.getValue().asDouble() + scalingOffset),
                            record.getTimestamp(), record.getFlag());
                } catch (TypeConversionException e) {
                    logger.error("Unable to apply scaling offset to channel " + config.id
                            + " because a TypeConversionError occured.", e);
                }
            }

            try {
                switch (config.valueType) {
                case BOOLEAN:
                    convertedRecord = new Record(new BooleanValue(record.getValue().asBoolean()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case BYTE:
                    convertedRecord = new Record(new ByteValue(record.getValue().asByte()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case SHORT:
                    convertedRecord = new Record(new ShortValue(record.getValue().asShort()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case INTEGER:
                    convertedRecord = new Record(new IntValue(record.getValue().asInt()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case LONG:
                    convertedRecord = new Record(new LongValue(record.getValue().asLong()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case FLOAT:
                    convertedRecord = new Record(new FloatValue(record.getValue().asFloat()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case DOUBLE:
                    convertedRecord = new Record(new DoubleValue(record.getValue().asDouble()), record.getTimestamp(),
                            record.getFlag());
                    break;
                case BYTE_ARRAY:
                    convertedRecord = new Record(new ByteArrayValue(record.getValue().asByteArray()),
                            record.getTimestamp(), record.getFlag());
                    break;
                case STRING:
                    convertedRecord = new Record(new StringValue(record.getValue().toString()), record.getTimestamp(),
                            record.getFlag());
                    break;
                }
            } catch (TypeConversionException e) {
                logger.error("Unable to convert value to configured value type because a TypeConversionError occured.",
                        e);
                convertedRecord = record;
            }

        }
        else {
            convertedRecord = new Record(latestRecord.getValue(), latestRecord.getTimestamp(), record.getFlag());
        }

        latestRecord = convertedRecord;
        notifyListeners();

        return convertedRecord;
    }

    private void notifyListeners() {
        if (listeners.size() != 0) {
            synchronized (listeners) {
                for (RecordListener listener : listeners) {
                    config.deviceParent.device.dataManager.executor
                            .execute(new ListenerNotifier(listener, latestRecord));
                }
            }
        }
    }

    ChannelRecordContainerImpl createChannelRecordContainer() {
        return new ChannelRecordContainerImpl(this);
    }

    void setFlag(Flag flag) {
        if (flag != latestRecord.getFlag()) {
            latestRecord = new Record(latestRecord.getValue(), latestRecord.getTimestamp(), flag);
            notifyListeners();
        }
    }

    public void setNewDeviceState(ChannelState state, Flag flag) {
        if (config.disabled) {
            config.state = ChannelState.DISABLED;
            setFlag(Flag.DISABLED);
        }
        else if (!config.isListening() && config.samplingInterval < 0) {
            config.state = state;
            setFlag(Flag.SAMPLING_AND_LISTENING_DISABLED);
        }
        else {
            config.state = state;
            setFlag(flag);
        }
    }

    @Override
    public Flag write(Value value) {

        if (config.deviceParent.driverParent.getId().equals("virtual")) {
            setLatestRecord(new Record(value, System.currentTimeMillis()));
            return Flag.VALID;
        }

        CountDownLatch writeTaskFinishedSignal = new CountDownLatch(1);
        WriteValueContainerImpl writeValueContainer = new WriteValueContainerImpl(this);

        Value adjustedValue = value;

        Double valueOffset = config.valueOffset;
        Double scalingFactor = config.scalingFactor;

        if (valueOffset != null) {
            adjustedValue = new DoubleValue(adjustedValue.asDouble() - valueOffset);
        }
        if (scalingFactor != null) {
            adjustedValue = new DoubleValue(adjustedValue.asDouble() / scalingFactor);
        }
        writeValueContainer.setValue(adjustedValue);

        List<WriteValueContainerImpl> writeValueContainerList = new ArrayList<>(1);
        writeValueContainerList.add(writeValueContainer);
        WriteTask writeTask = new WriteTask(dataManager, config.deviceParent.device, writeValueContainerList,
                writeTaskFinishedSignal);
        synchronized (dataManager.newWriteTasks) {
            dataManager.newWriteTasks.add(writeTask);
        }
        dataManager.interrupt();
        try {
            writeTaskFinishedSignal.await();
        } catch (InterruptedException e) {
        }

        latestRecord = new Record(value, System.currentTimeMillis(), writeValueContainer.getFlag());
        notifyListeners();

        return writeValueContainer.getFlag();
    }

    @Override
    public synchronized void write(List<Record> values) {
        this.futureValues = values;

        Collections.sort(values, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer("Timer ChannelImpl " + config.getId());

        long currentTimestamp = System.currentTimeMillis();

        for (final Record value : futureValues) {

            if ((currentTimestamp - value.getTimestamp()) < 1000l) {

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        write(value.getValue());
                    }
                }, new Date(value.getTimestamp()));
            }
        }
    }

    @Override
    public Record read() {
        CountDownLatch readTaskFinishedSignal = new CountDownLatch(1);

        ChannelRecordContainerImpl readValueContainer = new ChannelRecordContainerImpl(this);
        List<ChannelRecordContainerImpl> readValueContainerList = new ArrayList<>(1);
        readValueContainerList.add(readValueContainer);

        ReadTask readTask = new ReadTask(dataManager, config.deviceParent.device, readValueContainerList,
                readTaskFinishedSignal);
        synchronized (dataManager.newReadTasks) {
            dataManager.newReadTasks.add(readTask);
        }
        dataManager.interrupt();

        try {
            readTaskFinishedSignal.await();
        } catch (InterruptedException e) {
        }

        return setNewRecord(readValueContainer.record);
    }

    @Override
    public boolean isConnected() {
        if (config.state == ChannelState.CONNECTED || config.state == ChannelState.SAMPLING
                || config.state == ChannelState.LISTENING) {
            return true;
        }
        return false;
    }

    @Override
    public WriteValueContainer getWriteContainer() {
        return new WriteValueContainerImpl(this);
    }

    @Override
    public ReadRecordContainer getReadContainer() {
        return new ChannelRecordContainerImpl(this);
    }

}
