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

package org.openmuc.framework.config;

import java.util.Collection;

public interface DeviceConfig {

    public static final String DESCRIPTION_DEFAULT = "";
    public static final String DEVICE_ADDRESS_DEFAULT = "";
    public static final String SETTINGS_DEFAULT = "";
    public static final Boolean DISABLED_DEFAULT = false;

    public String getId();

    public void setId(String id) throws IdCollisionException;

    public String getDescription();

    public void setDescription(String description);

    public String getDeviceAddress();

    public void setDeviceAddress(String address);

    public String getSettings();

    public void setSettings(String settings);

    public Integer getSamplingTimeout();

    public void setSamplingTimeout(Integer timeout);

    public Integer getConnectRetryInterval();

    public void setConnectRetryInterval(Integer interval);

    public Boolean isDisabled();

    public void setDisabled(Boolean disabled);

    public ChannelConfig addChannel(String channelId) throws IdCollisionException;

    public ChannelConfig getChannel(String channelId);

    public Collection<ChannelConfig> getChannels();

    public void delete();

    public DriverConfig getDriver();

}
