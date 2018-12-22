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

package org.openmuc.framework.dataaccess;

import java.util.List;

/**
 * Service interface to get access to the measurement and control data of connected communication devices.
 */
public interface DataAccessService {

    Channel getChannel(String id);

    Channel getChannel(String id, ChannelChangeListener channelChangeListener);

    /**
     * Get the list of all channel IDs.
     * 
     * @return the list of all channel IDs.
     */
    List<String> getAllIds();

    List<LogicalDevice> getLogicalDevices(String type);

    List<LogicalDevice> getLogicalDevices(String type, LogicalDeviceChangeListener logicalDeviceChangeListener);

    /**
     * Execute the read on the read value containers.
     * 
     * @param values
     *            a list of ReadRecordContainer
     * @see Channel#getReadContainer()
     */
    void read(List<ReadRecordContainer> values);

    /**
     * Execute the write on the write value containers.
     * 
     * @param values
     *            a list of WriteValueContainer.
     * 
     * @see Channel#getWriteContainer()
     */
    void write(List<WriteValueContainer> values);

}
