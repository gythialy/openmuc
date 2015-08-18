/*
 * Copyright 2011-15 Fraunhofer ISE
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

import java.util.List;

import org.openmuc.framework.data.ValueType;

public interface ChannelConfig {

	public static final Boolean DISABLED_DEFAULT = false;
	public static final String DESCRIPTION_DEFAULT = "";
	public static final String CHANNEL_ADDRESS_DEFAULT = "";
	public static final String UNIT_DEFAULT = "";
	public static final ValueType VALUE_TYPE_DEFAULT = ValueType.DOUBLE;
	public static final int BYTE_ARRAY_SIZE_DEFAULT = 10;
	public static final int STRING_SIZE_DEFAULT = 10;
	public static final boolean LISTENING_DEFAULT = false;
	public static final int SAMPLING_INTERVAL_DEFAULT = -1;
	public static final int SAMPLING_TIME_OFFSET_DEFAULT = 0;
	public static final String SAMPLING_GROUP_DEFAULT = "";
	public static final int LOGGING_INTERVAL_DEFAULT = -1;
	public static final int LOGGING_TIME_OFFSET_DEFAULT = 0;

	public String getId();

	public void setId(String id) throws IdCollisionException;

	public String getDescription();

	public void setDescription(String description);

	public String getChannelAddress();

	public void setChannelAddress(String address);

	public String getUnit();

	public void setUnit(String unit);

	public ValueType getValueType();

	public void setValueType(ValueType type);

	public Integer getValueTypeLength();

	public void setValueTypeLength(Integer maxLength);

	public Double getScalingFactor();

	public void setScalingFactor(Double factor);

	public Double getValueOffset();

	public void setValueOffset(Double offset);

	public Boolean isListening();

	public void setListening(Boolean listening);

	public Integer getSamplingInterval();

	public void setSamplingInterval(Integer interval);

	public Integer getSamplingTimeOffset();

	public void setSamplingTimeOffset(Integer offset);

	public String getSamplingGroup();

	public void setSamplingGroup(String group);

	public Integer getLoggingInterval();

	public void setLoggingInterval(Integer interval);

	public Integer getLoggingTimeOffset();

	public void setLoggingTimeOffset(Integer offset);

	public Boolean isDisabled();

	public void setDisabled(Boolean disabled);

	public void delete();

	public DeviceConfig getDevice();

	public List<ServerMapping> getServerMappings();

	public void addServerMapping(ServerMapping serverMapping);

	public void deleteServerMappings(String id);
}
