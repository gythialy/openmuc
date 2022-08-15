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

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StartListeningTask extends DeviceTask implements ConnectedTask {

    private static final Logger logger = LoggerFactory.getLogger(StartListeningTask.class);

    List<ChannelRecordContainerImpl> selectedChannels;

    public StartListeningTask(DataManager dataManager, Device device,
            List<ChannelRecordContainerImpl> selectedChannels) {
        this.dataManager = dataManager;
        this.device = device;
        this.selectedChannels = selectedChannels;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        try {
            device.connection.startListening((List<ChannelRecordContainer>) ((List<?>) selectedChannels), dataManager);
        } catch (UnsupportedOperationException e) {
            for (ChannelRecordContainerImpl chRecContainer : selectedChannels) {
                chRecContainer.getChannel().setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
            }
        } catch (ConnectionException e) {
            // Connection to device lost. Signal to device instance and end task
            // without notifying DataManager
            logger.warn("Connection to device {} lost because {}. Trying to reconnect...", device.deviceConfig.getId(),
                    e.getMessage());
            device.disconnectedSignal();
            return;
        } catch (Exception e) {
            logger.error("unexpected exception by startListeningFor function of driver: "
                    + device.deviceConfig.driverParent.getId(), e);
            // TODO set flag?
        }

        synchronized (dataManager.tasksFinished) {
            dataManager.tasksFinished.add(this);
        }
        dataManager.interrupt();
    }

    @Override
    public DeviceTaskType getType() {
        return DeviceTaskType.START_LISTENING_FOR;
    }

    @Override
    public void deviceNotConnected() {
    }

}
