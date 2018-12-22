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

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WriteTask extends DeviceTask implements ConnectedTask {

    private static final Logger logger = LoggerFactory.getLogger(WriteTask.class);

    private final CountDownLatch writeTaskFinishedSignal;
    List<WriteValueContainerImpl> writeValueContainers;

    public WriteTask(DataManager dataManager, Device device, List<WriteValueContainerImpl> writeValueContainers,
            CountDownLatch writeTaskFinishedSignal) {
        this.dataManager = dataManager;
        this.device = device;
        this.writeTaskFinishedSignal = writeTaskFinishedSignal;
        this.writeValueContainers = writeValueContainers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        try {
            device.connection.write((List<ChannelValueContainer>) ((List<?>) writeValueContainers), null);
        } catch (UnsupportedOperationException e) {
            for (WriteValueContainerImpl valueContainer : writeValueContainers) {
                valueContainer.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
            }
        } catch (ConnectionException e) {
            // Connection to device lost. Signal to device instance and end task without notifying DataManager
            logger.warn("Connection to device {} lost because {}. Trying to reconnect...", device.deviceConfig.getId(),
                    e.getMessage());
            for (WriteValueContainerImpl valueContainer : writeValueContainers) {
                valueContainer.setFlag(Flag.CONNECTION_EXCEPTION);
            }
            writeTaskFinishedSignal.countDown();
            synchronized (dataManager.disconnectedDevices) {
                dataManager.disconnectedDevices.add(device);
            }
            dataManager.interrupt();
            return;
        } catch (Exception e) {
            logger.warn("unexpected exception thrown by write funtion of driver ", e);
            for (WriteValueContainerImpl valueContainer : writeValueContainers) {
                valueContainer.setFlag(Flag.DRIVER_THREW_UNKNOWN_EXCEPTION);
            }
        }

        writeTaskFinishedSignal.countDown();
        synchronized (dataManager.tasksFinished) {
            dataManager.tasksFinished.add(this);
        }
        dataManager.interrupt();

    }

    @Override
    public DeviceTaskType getType() {
        return DeviceTaskType.WRITE;
    }

    /**
     * Writes entries, that the device is not connected.
     */
    @Override
    public void deviceNotConnected() {
        for (WriteValueContainerImpl valueContainer : writeValueContainers) {
            valueContainer.setFlag(Flag.COMM_DEVICE_NOT_CONNECTED);
        }
        writeTaskFinishedSignal.countDown();
    }

}
