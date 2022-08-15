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
package org.openmuc.framework.core.datamanager;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadTask extends DeviceTask implements ConnectedTask {

    private static final Logger logger = LoggerFactory.getLogger(ReadTask.class);

    private final CountDownLatch readTaskFinishedSignal;
    List<ChannelRecordContainerImpl> channelRecordContainers;
    protected boolean methodNotExceptedExceptionThrown = false;
    protected boolean unknownDriverExceptionThrown = false;
    protected volatile boolean disabled = false;

    boolean startedLate = false;

    public ReadTask(DataManager dataManager, Device device, List<ChannelRecordContainerImpl> selectedChannels,
            CountDownLatch readTaskFinishedSignal) {
        this.dataManager = dataManager;
        this.device = device;
        channelRecordContainers = selectedChannels;
        this.readTaskFinishedSignal = readTaskFinishedSignal;
    }

    @Override
    public final void run() {

        try {
            executeRead();
        } catch (UnsupportedOperationException e) {
            methodNotExceptedExceptionThrown = true;
        } catch (ConnectionException e) {
            // Connection to device lost. Signal to device instance and end task without notifying DataManager
            logger.warn("Connection to device {} lost because {}. Trying to reconnect...", device.deviceConfig.getId(),
                    e.getMessage());

            for (ChannelRecordContainerImpl driverChannel : channelRecordContainers) {
                driverChannel.setRecord(new Record(Flag.ACCESS_METHOD_NOT_SUPPORTED));
            }
            readTaskFinishedSignal.countDown();
            synchronized (dataManager.disconnectedDevices) {
                dataManager.disconnectedDevices.add(device);
            }
            dataManager.interrupt();
            return;
        } catch (Exception e) {
            logger.warn("unexpected exception thrown by read funtion of driver ", e);
            unknownDriverExceptionThrown = true;
        }

        taskFinished();
    }

    @Override
    public final DeviceTaskType getType() {
        return DeviceTaskType.READ;
    }

    @Override
    public final void deviceNotConnected() {
        for (ChannelRecordContainer recordContainer : channelRecordContainers) {
            recordContainer.setRecord(new Record(Flag.COMM_DEVICE_NOT_CONNECTED));
        }
        taskAborted();
    }

    @SuppressWarnings("unchecked")
    protected void executeRead() throws UnsupportedOperationException, ConnectionException {
        device.connection.read((List<ChannelRecordContainer>) ((List<?>) channelRecordContainers), true, "");
    }

    protected void taskFinished() {
        disabled = true;
        long now = System.currentTimeMillis();
        if (methodNotExceptedExceptionThrown) {
            for (ChannelRecordContainerImpl driverChannel : channelRecordContainers) {
                driverChannel.setRecord(new Record(null, now, Flag.ACCESS_METHOD_NOT_SUPPORTED));
            }
        }
        else if (unknownDriverExceptionThrown) {
            for (ChannelRecordContainerImpl driverChannel : channelRecordContainers) {
                driverChannel.setRecord(new Record(null, now, Flag.DRIVER_THREW_UNKNOWN_EXCEPTION));
            }
        }

        readTaskFinishedSignal.countDown();

        synchronized (dataManager.tasksFinished) {
            dataManager.tasksFinished.add(this);
        }
        dataManager.interrupt();
    }

    protected void taskAborted() {
        readTaskFinishedSignal.countDown();
    }
}
