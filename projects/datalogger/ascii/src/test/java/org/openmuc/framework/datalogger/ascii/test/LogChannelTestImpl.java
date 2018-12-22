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
package org.openmuc.framework.datalogger.ascii.test;

import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LogChannel;

public class LogChannelTestImpl implements LogChannel {

    private final String id;
    private final String description;
    private final String unit;
    private final ValueType valueType;
    private final Integer loggingInterval;
    private final Integer loggingTimeOffset;
    private Integer valueLength;

    public LogChannelTestImpl(String id, String description, String unit, ValueType valueType, Integer loggingInterval,
            Integer loggingTimeOffset) {

        this.id = id;
        this.description = description;
        this.unit = unit;
        this.valueType = valueType;
        this.loggingInterval = loggingInterval;
        this.loggingTimeOffset = loggingTimeOffset;
    }

    public LogChannelTestImpl(String id, String description, String unit, ValueType valueType, Integer loggingInterval,
            Integer loggingTimeOffset, int valueLength) {

        this(id, description, unit, valueType, loggingInterval, loggingTimeOffset);
        this.valueLength = valueLength;
    }

    @Override
    public String getId() {

        return id;
    }

    @Override
    public String getDescription() {

        return description;
    }

    @Override
    public String getUnit() {

        return unit;
    }

    @Override
    public ValueType getValueType() {

        return valueType;
    }

    @Override
    public Integer getValueTypeLength() {

        return valueLength;
    }

    @Override
    public Integer getLoggingInterval() {

        return loggingInterval;
    }

    @Override
    public Integer getLoggingTimeOffset() {

        return loggingTimeOffset;
    }
}
