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
package org.openmuc.framework.driver.aggregator;

import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class ChannelAddress {

    private final String sourceChannelId;
    private final String aggregationType;
    /*
     * Range: [0, 1]
     */
    private final double quality;
    private final ChannelRecordContainer container;

    public ChannelAddress(ChannelRecordContainer container, String sourceChannelId, String aggregationType,
            double quality) {
        this.container = container;
        this.sourceChannelId = sourceChannelId;
        this.aggregationType = aggregationType;
        this.quality = quality;
    }

    public String getSourceChannelId() {
        return sourceChannelId;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public double getQuality() {
        return quality;
    }

    public ChannelRecordContainer getContainer() {
        return container;
    }

}
