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

import org.openmuc.framework.dataaccess.DeviceState;

public enum DeviceTaskType {
    CONNECT(DeviceState.CONNECTING),
    DISCONNECT(DeviceState.DISCONNECTING),
    SAMPLE(DeviceState.READING),
    READ(DeviceState.READING),
    WRITE(DeviceState.WRITING),
    START_LISTENING_FOR(DeviceState.STARTING_TO_LISTEN);

    private DeviceState resultingState;

    private DeviceTaskType(DeviceState resultingState) {
        this.resultingState = resultingState;
    }

    public DeviceState getResultingState() {
        return resultingState;
    }
}
