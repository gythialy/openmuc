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

import java.util.List;

import org.openmuc.framework.data.ValueType;

public interface ChannelConfig {

    static final Boolean DISABLED_DEFAULT = false;
    static final String DESCRIPTION_DEFAULT = "";
    static final String CHANNEL_ADDRESS_DEFAULT = "";
    static final String UNIT_DEFAULT = "";
    static final ValueType VALUE_TYPE_DEFAULT = ValueType.DOUBLE;
    static final int BYTE_ARRAY_SIZE_DEFAULT = 10;
    static final int STRING_SIZE_DEFAULT = 10;
    static final boolean LISTENING_DEFAULT = false;
    static final int SAMPLING_INTERVAL_DEFAULT = -1;
    static final int SAMPLING_TIME_OFFSET_DEFAULT = 0;
    static final String SAMPLING_GROUP_DEFAULT = "";
    static final int LOGGING_INTERVAL_DEFAULT = -1;
    static final int LOGGING_TIME_OFFSET_DEFAULT = 0;

    String getId();

    void setId(String id) throws IdCollisionException;

    String getDescription();

    void setDescription(String description);

    String getChannelAddress();

    void setChannelAddress(String address);

    String getUnit();

    void setUnit(String unit);

    ValueType getValueType();

    void setValueType(ValueType type);

    Integer getValueTypeLength();

    void setValueTypeLength(Integer maxLength);

    Double getScalingFactor();

    void setScalingFactor(Double factor);

    Double getValueOffset();

    void setValueOffset(Double offset);

    Boolean isListening();

    void setListening(Boolean listening);

    Integer getSamplingInterval();

    void setSamplingInterval(Integer interval);

    Integer getSamplingTimeOffset();

    void setSamplingTimeOffset(Integer offset);

    String getSamplingGroup();

    void setSamplingGroup(String group);

    Integer getLoggingInterval();

    void setLoggingInterval(Integer interval);

    Integer getLoggingTimeOffset();

    void setLoggingTimeOffset(Integer offset);

    Boolean isDisabled();

    void setDisabled(Boolean disabled);

    void delete();

    DeviceConfig getDevice();

    List<ServerMapping> getServerMappings();

    void addServerMapping(ServerMapping serverMapping);

    void deleteServerMappings(String id);
}
