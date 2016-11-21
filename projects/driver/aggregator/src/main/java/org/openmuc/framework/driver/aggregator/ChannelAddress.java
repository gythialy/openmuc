package org.openmuc.framework.driver.aggregator;

import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class ChannelAddress {

    private final String sourceChannelId;
    private final String aggregationType;
    private final double quality; // Range: 0.0 .. 1.0.
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
