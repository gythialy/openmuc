/*
 * Copyright 2011-18 Fraunhofer ISE
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
import java.util.Arrays;
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
import org.openmuc.framework.data.FutureValue;
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

    private static final Logger logger = LoggerFactory.getLogger(ChannelImpl.class);

    private volatile Record latestRecord;
    volatile ChannelConfigImpl config;
    ChannelCollection samplingCollection;
    ChannelCollection loggingCollection;
    private final Set<RecordListener> listeners = new LinkedHashSet<>();
    private final DataManager dataManager;
    volatile Object handle;
    private Timer timer = null;
    private List<FutureValue> futureValues;

    public ChannelImpl(DataManager dataManager, ChannelConfigImpl config, ChannelState initState, Flag initFlag,
            long currentTime, List<LogChannel> logChannels) {
        this.dataManager = dataManager;
        this.config = config;
        this.futureValues = new ArrayList<>();

        if (config.isDisabled()) {
            config.state = ChannelState.DISABLED;
            latestRecord = new Record(Flag.DISABLED);
        }
        else if (!config.isListening() && config.getSamplingInterval() < 0) {
            config.state = initState;
            latestRecord = new Record(Flag.SAMPLING_AND_LISTENING_DISABLED);
        }
        else {
            config.state = initState;
            latestRecord = new Record(null, null, initFlag);
        }

        if (config.getLoggingInterval() != ChannelConfig.LOGGING_INTERVAL_DEFAULT) {
            dataManager.addToLoggingCollections(this, currentTime);
            logChannels.add(config);
        }
    }

    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public String getChannelAddress() {
        return config.getChannelAddress();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public String getUnit() {
        return config.getUnit();
    }

    @Override
    public ValueType getValueType() {
        return config.getValueType();
    }

    @Override
    public double getScalingFactor() {
        if (config.getScalingFactor() == null) {
            return 1d;
        }
        return config.getScalingFactor();
    }

    @Override
    public int getSamplingInterval() {
        return config.getSamplingInterval();
    }

    @Override
    public int getSamplingTimeOffset() {
        return config.getSamplingTimeOffset();
    }

    @Override
    public int getLoggingInterval() {
        return config.getLoggingInterval();
    }

    @Override
    public int getLoggingTimeOffset() {
        return config.getLoggingTimeOffset();
    }

    @Override
    public String getDriverName() {
        return config.deviceParent.driverParent.id;
    }

    @Override
    public String getDeviceAddress() {
        return config.deviceParent.getDeviceAddress();
    }

    @Override
    public String getDeviceName() {
        return config.deviceParent.getId();
    }

    @Override
    public String getDeviceDescription() {
        return config.deviceParent.getDescription();
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
        List<Record> records = dataManager.getDataLogger().getRecords(config.getId(), timestamp, timestamp);
        if (!records.isEmpty()) {
            return records.get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public List<Record> getLoggedRecords(long startTime) throws DataLoggerNotAvailableException, IOException {
        return dataManager.getDataLogger().getRecords(config.getId(), startTime, System.currentTimeMillis());
    }

    @Override
    public List<Record> getLoggedRecords(long startTime, long endTime)
            throws DataLoggerNotAvailableException, IOException {
        List<Record> toReturn = dataManager.getDataLogger().getRecords(config.getId(), startTime, endTime);

        // values in the future values list are sorted.
        Long currentTime = System.currentTimeMillis();
        for (FutureValue futureValue : futureValues) {
            if (futureValue.getWriteTime() >= currentTime) {
                if (futureValue.getWriteTime() <= endTime) {
                    Record futureValAsRec = new Record(futureValue.getValue(), futureValue.getWriteTime());
                    toReturn.add(futureValAsRec);
                }
                else {
                    break;
                }
            }
        }
        return toReturn;
    }

    Record setNewRecord(Record record) {

        Record convertedRecord;

        if (record.getFlag() == Flag.VALID) {
            convertedRecord = convertValidRecord(record);
        }
        else {
            convertedRecord = new Record(latestRecord.getValue(), latestRecord.getTimestamp(), record.getFlag());
        }

        latestRecord = convertedRecord;
        notifyListeners();

        return convertedRecord;
    }

    private Record convertValidRecord(Record record) {
        Double scalingFactor = config.getScalingFactor();
        Double scalingOffset = config.getValueOffset();

        if (scalingFactor != null) {
            try {
                record = new Record(new DoubleValue(record.getValue().asDouble() * scalingFactor),
                        record.getTimestamp(), record.getFlag());
            } catch (TypeConversionException e) {
                String msg = "Unable to apply scaling factor to channel " + config.getId()
                        + " because a TypeConversionError occurred.";
                logger.error(msg, e);
            }
        }
        if (scalingOffset != null) {
            try {
                record = new Record(new DoubleValue(record.getValue().asDouble() + scalingOffset),
                        record.getTimestamp(), record.getFlag());
            } catch (TypeConversionException e) {
                String msg = "Unable to apply scaling offset to channel " + config.getId()
                        + " because a TypeConversionError occurred.";
                logger.error(msg, e);
            }
        }

        try {
            switch (config.getValueType()) {
            case BOOLEAN:
                return new Record(new BooleanValue(record.getValue().asBoolean()), record.getTimestamp(),
                        record.getFlag());
            case BYTE:
                return new Record(new ByteValue(record.getValue().asByte()), record.getTimestamp(), record.getFlag());
            case SHORT:
                return new Record(new ShortValue(record.getValue().asShort()), record.getTimestamp(), record.getFlag());
            case INTEGER:
                return new Record(new IntValue(record.getValue().asInt()), record.getTimestamp(), record.getFlag());
            case LONG:
                return new Record(new LongValue(record.getValue().asLong()), record.getTimestamp(), record.getFlag());
            case FLOAT:
                return new Record(new FloatValue(record.getValue().asFloat()), record.getTimestamp(), record.getFlag());
            case DOUBLE:
                return new Record(new DoubleValue(record.getValue().asDouble()), record.getTimestamp(),
                        record.getFlag());
            case BYTE_ARRAY:
                return new Record(new ByteArrayValue(record.getValue().asByteArray()), record.getTimestamp(),
                        record.getFlag());
            case STRING:
            default:
                return new Record(new StringValue(record.getValue().toString()), record.getTimestamp(),
                        record.getFlag());
            }
        } catch (TypeConversionException e) {
            logger.error("Unable to convert value to configured value type because a TypeConversionError occured.", e);
            return new Record(Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION);
        }
    }

    private void notifyListeners() {
        if (listeners.isEmpty()) {
            return;
        }

        synchronized (listeners) {
            for (RecordListener listener : listeners) {
                config.deviceParent.device.dataManager.executor.execute(new ListenerNotifier(listener, latestRecord));
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
        if (config.isDisabled()) {
            config.state = ChannelState.DISABLED;
            setFlag(Flag.DISABLED);
        }
        else if (!config.isListening() && config.getSamplingInterval() < 0) {
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

        Double valueOffset = config.getValueOffset();
        Double scalingFactor = config.getScalingFactor();

        if (valueOffset != null) {
            adjustedValue = new DoubleValue(adjustedValue.asDouble() - valueOffset);
        }
        if (scalingFactor != null) {
            adjustedValue = new DoubleValue(adjustedValue.asDouble() / scalingFactor);
        }
        writeValueContainer.setValue(adjustedValue);

        List<WriteValueContainerImpl> writeValueContainerList = Arrays.asList(writeValueContainer);
        WriteTask writeTask = new WriteTask(dataManager, config.deviceParent.device, writeValueContainerList,
                writeTaskFinishedSignal);

        synchronized (dataManager.newWriteTasks) {
            dataManager.newWriteTasks.add(writeTask);
        }

        dataManager.interrupt();
        try {
            writeTaskFinishedSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long timestamp = System.currentTimeMillis();
        latestRecord = new Record(value, timestamp, writeValueContainer.getFlag());
        notifyListeners();

        return writeValueContainer.getFlag();
    }

    @Override
    public void writeFuture(List<FutureValue> values) {
        if (values == null) {
            throw new NullPointerException("Argument is not allowed to be null.");
        }

        this.futureValues = values;

        Collections.sort(values, new Comparator<FutureValue>() {
            @Override
            public int compare(FutureValue o1, FutureValue o2) {
                return o1.getWriteTime().compareTo(o2.getWriteTime());
            }
        });

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer("Timer ChannelImpl " + config.getId());

        long currentTimestamp = System.currentTimeMillis();

        for (final FutureValue value : futureValues) {

            if ((currentTimestamp - value.getWriteTime()) >= 1000l) {
                continue;
            }

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    write(value.getValue());
                }
            };

            Date scheduleTime = new Date(value.getWriteTime());
            timer.schedule(timerTask, scheduleTime);
        }

    }

    @Override
    @SuppressWarnings("deprecation")
    public void write(List<Record> values) {
        ArrayList<FutureValue> fValues = new ArrayList<>(values.size());
        for (Record record : values) {
            fValues.add(new FutureValue(record.getValue(), record.getTimestamp()));
        }
        writeFuture(fValues);
    }

    @Override
    public Record read() {
        CountDownLatch readTaskFinishedSignal = new CountDownLatch(1);

        ChannelRecordContainerImpl readValueContainer = new ChannelRecordContainerImpl(this);
        List<ChannelRecordContainerImpl> readValueContainerList = Arrays.asList(readValueContainer);

        ReadTask readTask = new ReadTask(dataManager, config.deviceParent.device, readValueContainerList,
                readTaskFinishedSignal);
        synchronized (dataManager.newReadTasks) {
            dataManager.newReadTasks.add(readTask);
        }
        dataManager.interrupt();

        try {
            readTaskFinishedSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return setNewRecord(readValueContainer.getRecord());
    }

    @Override
    public boolean isConnected() {
        return config.state == ChannelState.CONNECTED || config.state == ChannelState.SAMPLING
                || config.state == ChannelState.LISTENING;
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
