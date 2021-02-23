/*
 * Copyright 2011-2021 Fraunhofer ISE
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

import static org.openmuc.framework.driver.aggregator.AggregatorConstants.ADDRESS_QUALITY_INDEX;
import static org.openmuc.framework.driver.aggregator.AggregatorConstants.ADDRESS_SEPARATOR;
import static org.openmuc.framework.driver.aggregator.AggregatorConstants.AGGREGATION_TYPE_AVG;
import static org.openmuc.framework.driver.aggregator.AggregatorConstants.DEFAULT_QUALITY;
import static org.openmuc.framework.driver.aggregator.AggregatorConstants.MAX_ADDRESS_PARTS_LENGTH;
import static org.openmuc.framework.driver.aggregator.AggregatorConstants.MIN_ADDRESS_PARTS_LENGTH;

import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.aggregator.types.AverageAggregation;
import org.openmuc.framework.driver.aggregator.types.DiffAggregation;
import org.openmuc.framework.driver.aggregator.types.LastAggregation;
import org.openmuc.framework.driver.aggregator.types.PulseEnergyAggregation;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

/**
 * Creates a AggregatorChannel instance according to the aggregationType
 */
public class AggregatorChannelFactory {

    public static AggregatorChannel createAggregatorChannel(ChannelRecordContainer container,
            DataAccessService dataAccessService) throws AggregationException {

        AggregatorChannel aggregatorChannel = null;
        ChannelAddress simpleAddress = createAddressFrom(container);
        aggregatorChannel = createByAddress(simpleAddress, dataAccessService);
        return aggregatorChannel;
    }

    /**
     * Creates a AggregatorChannel instance according to the aggregationType
     * 
     * Note: Add new types here if necessary
     * 
     * @throws AggregationException
     */
    private static AggregatorChannel createByAddress(ChannelAddress channelAddress, DataAccessService dataAccessService)
            throws AggregationException {

        String aggregationType = channelAddress.getAggregationType();

        switch (aggregationType) {
        case AGGREGATION_TYPE_AVG:
            return new AverageAggregation(channelAddress, dataAccessService);
        case AggregatorConstants.AGGREGATION_TYPE_LAST:
            return new LastAggregation(channelAddress, dataAccessService);
        case AggregatorConstants.AGGREGATION_TYPE_DIFF:
            return new DiffAggregation(channelAddress, dataAccessService);
        case AggregatorConstants.AGGREGATION_TYPE_PULS_ENERGY:
            return new PulseEnergyAggregation(channelAddress, dataAccessService);
        default:
            throw new AggregationException("Unsupported aggregationType: " + aggregationType + " in channel "
                    + channelAddress.getContainer().getChannelAddress());
        }
    }

    /**
     * Returns the "type" parameter from address
     * 
     * @throws WrongChannelAddressFormatException
     */
    private static ChannelAddress createAddressFrom(ChannelRecordContainer container) throws AggregationException {

        String address = container.getChannelAddress();
        String[] addressParts = address.split(ADDRESS_SEPARATOR);
        int addressPartsLength = addressParts.length;

        if (addressPartsLength > MAX_ADDRESS_PARTS_LENGTH || addressPartsLength < MIN_ADDRESS_PARTS_LENGTH) {
            throw new AggregationException("Invalid number of channel address parameters.");
        }

        String sourceChannelId = addressParts[AggregatorConstants.ADDRESS_SOURCE_CHANNEL_ID_INDEX];
        String aggregationType = addressParts[AggregatorConstants.ADDRESS_AGGREGATION_TYPE_INDEX].toUpperCase();
        double quality = extractQuality(addressPartsLength, addressParts);

        return new ChannelAddress(container, sourceChannelId, aggregationType, quality);

    }

    private static double extractQuality(int addressPartsLength, String[] addressParts) {
        double quality = -1;

        if (addressPartsLength == MAX_ADDRESS_PARTS_LENGTH) {
            quality = Double.valueOf(addressParts[ADDRESS_QUALITY_INDEX]);
        }

        // use the default value if the previous parsing failed or the parsed quality value is invalid
        if (quality < 0.0 || quality > 1.0) {
            quality = DEFAULT_QUALITY;
        }

        return quality;
    }
}
