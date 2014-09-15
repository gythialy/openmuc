/*
 * Copyright 2011-14 Fraunhofer ISE
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

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class StartListeningTask extends DeviceTask {

    private final static Logger logger = LoggerFactory.getLogger(StartListeningTask.class);

    List<ChannelRecordContainerImpl> selectedChannels;

    public StartListeningTask(DataManager dataManager,
                              Device device,
                              List<ChannelRecordContainerImpl> selectedChannels) {
        this.dataManager = dataManager;
        this.device = device;
        this.selectedChannels = selectedChannels;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        try {
            device.deviceConfig.driverParent.activeDriver.startListening(device.connection,
                                                                         (List<ChannelRecordContainer>) ((List<?>) selectedChannels),
                                                                         dataManager);
        }
        catch (UnsupportedOperationException e) {
            for (ChannelRecordContainer channelRecordContainer : selectedChannels) {
                ((ChannelRecordContainerImpl) channelRecordContainer).channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
            }
        }
        catch (ConnectionException e) {
            // Connection to device lost. Signal to device instance and end task without notifying DataManager
            logger.warn("Connection to device {} lost because {}. Trying to reconnect...",
                        device.deviceConfig.id,
                        e.getMessage());
            device.disconnectedSignal();
            return;
        }
        catch (Exception e) {
            logger.error("unexpected exception by startListeningFor funtion of driver: "
                         + device.deviceConfig.driverParent.id);
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
    public void setDeviceState() {
        device.state = DeviceState.STARTING_TO_LISTEN;
    }

}
