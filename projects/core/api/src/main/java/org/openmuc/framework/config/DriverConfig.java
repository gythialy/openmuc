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

package org.openmuc.framework.config;

import java.util.Collection;

public interface DriverConfig {

    static final int SAMPLING_TIMEOUT_DEFAULT = 0;
    static final int CONNECT_RETRY_INTERVAL_DEFAULT = 60000;
    static final boolean DISABLED_DEFAULT = false;

    String getId();

    void setId(String id) throws IdCollisionException;

    Integer getSamplingTimeout();

    void setSamplingTimeout(Integer timeout);

    Integer getConnectRetryInterval();

    void setConnectRetryInterval(Integer interval);

    Boolean isDisabled();

    void setDisabled(Boolean disabled);

    DeviceConfig addDevice(String deviceId) throws IdCollisionException;

    DeviceConfig getDevice(String deviceId);

    Collection<DeviceConfig> getDevices();

    void delete();

}
