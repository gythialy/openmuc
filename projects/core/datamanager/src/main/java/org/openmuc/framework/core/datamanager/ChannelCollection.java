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

import java.util.LinkedList;
import java.util.List;

public final class ChannelCollection {

    List<ChannelImpl> channels = new LinkedList<>();
    int interval;
    int timeOffset;
    String samplingGroup;
    Device device;
    Action action;

    public ChannelCollection(Integer interval, Integer timeOffset, String samplingGroup, Device device) {
        this.interval = interval;
        this.timeOffset = timeOffset;
        this.samplingGroup = samplingGroup;
        this.device = device;
    }

    public long calculateNextActionTime(long timestamp) {
        return ((interval - (((timestamp % (24 * 60 * 60 * 1000)) - timeOffset) % interval)) + timestamp);
    }

}
