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

import org.openmuc.framework.data.ValueType;

public class ChannelScanInfo {

    private final String channelAddress;
    private final String description;
    private final ValueType valueType;
    private final Integer valueTypeLength;
    private final Boolean readable;
    private final Boolean writable;
    private final String metaData;
    private final String unit;

    public ChannelScanInfo(String channelAddress, String description, ValueType valueType, Integer valueTypeLength) {
        this(channelAddress, description, valueType, valueTypeLength, true, true);
    }

    public ChannelScanInfo(String channelAddress, String description, ValueType valueType, Integer valueTypeLength,
            Boolean readable, Boolean writable) {
        this(channelAddress, description, valueType, valueTypeLength, readable, writable, "");
    }

    public ChannelScanInfo(String channelAddress, String description, ValueType valueType, Integer valueTypeLength,
            Boolean readable, Boolean writable, String metaData) {
        this(channelAddress, description, valueType, valueTypeLength, readable, writable, metaData, "");
    }

    public ChannelScanInfo(String channelAddress, String description, ValueType valueType, Integer valueTypeLength,
            Boolean readable, Boolean writable, String metaData, String unit) {
        if (channelAddress == null || channelAddress.isEmpty()) {
            throw new IllegalArgumentException("Channel Address may not be empty.");
        }
        this.channelAddress = channelAddress;
        this.description = description;
        this.valueType = valueType;
        this.valueTypeLength = valueTypeLength;
        this.readable = readable;
        this.writable = writable;
        this.metaData = metaData;
        this.unit = unit;
    }

    public String getChannelAddress() {
        return channelAddress;
    }

    public String getDescription() {
        return description;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Integer getValueTypeLength() {
        return valueTypeLength;
    }

    public Boolean isReadable() {
        return readable;
    }

    public Boolean isWritable() {
        return writable;
    }

    public String getMetaData() {
        return metaData;
    }

    public String getUnit() {
        return unit;
    }
}
