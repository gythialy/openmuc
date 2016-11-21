package org.openmuc.framework.driver.aggregator;

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
        ChannelAddress simpleAddress = getAddress(container);
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

        AggregatorChannel aggregatorChannel = null;

        String aggregationType = channelAddress.getAggregationType();

        if (aggregationType.equals(AggregatorConstants.AGGREGATION_TYPE_AVG)) {
            aggregatorChannel = new AverageAggregation(channelAddress, dataAccessService);
        }
        else if (aggregationType.equals(AggregatorConstants.AGGREGATION_TYPE_LAST)) {
            aggregatorChannel = new LastAggregation(channelAddress, dataAccessService);
        }
        else if (aggregationType.equals(AggregatorConstants.AGGREGATION_TYPE_DIFF)) {
            aggregatorChannel = new DiffAggregation(channelAddress, dataAccessService);
        }
        else if (aggregationType.startsWith(AggregatorConstants.AGGREGATION_TYPE_PULS_ENERGY)) {
            aggregatorChannel = new PulseEnergyAggregation(channelAddress, dataAccessService);
        }
        else {
            throw new AggregationException("Unsupported aggregationType: " + aggregationType + " in channel "
                    + channelAddress.getContainer().getChannelAddress());
        }

        return aggregatorChannel;
    }

    /**
     * Returns the "type" parameter from address
     * 
     * @throws WrongChannelAddressFormatException
     */
    private static ChannelAddress getAddress(ChannelRecordContainer container) throws AggregationException {

        String address = container.getChannelAddress();
        String[] addressParts = address.split(AggregatorConstants.ADDRESS_SEPARATOR);
        int addressPartsLength = addressParts.length;

        ChannelAddress simpleAddress = null;

        if (addressPartsLength <= AggregatorConstants.MAX_ADDRESS_PARTS_LENGTH
                && addressPartsLength >= AggregatorConstants.MIN_ADDRESS_PARTS_LENGTH) {

            String sourceChannelId = addressParts[AggregatorConstants.ADDRESS_SOURCE_CHANNEL_ID_INDEX];
            String aggregationType = addressParts[AggregatorConstants.ADDRESS_AGGREGATION_TYPE_INDEX].toUpperCase();
            double quality = getQuality(addressPartsLength, addressParts);
            simpleAddress = new ChannelAddress(container, sourceChannelId, aggregationType, quality);
        }
        else {
            throw new AggregationException("Invalid number of channel address parameters.");
        }

        return simpleAddress;
    }

    private static double getQuality(int addressPartsLength, String[] addressParts) {
        double quality = -1;

        if (addressPartsLength == AggregatorConstants.MAX_ADDRESS_PARTS_LENGTH) {
            quality = Double.valueOf(addressParts[AggregatorConstants.ADDRESS_QUALITY_INDEX]);
        }

        // use the default value if the previous parsing failed or the parsed quality value is invalid
        if (quality < 0.0 || quality > 1.0) {
            quality = AggregatorConstants.DEFAULT_QUALITY;
        }

        return quality;
    }
}
