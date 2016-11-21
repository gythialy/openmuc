package org.openmuc.framework.driver.aggregator;

public class AggregatorConstants {

    public static final int MAX_ADDRESS_PARTS_LENGTH = 3;
    public static final int MIN_ADDRESS_PARTS_LENGTH = 2;

    public static final String ADDRESS_SEPARATOR = ":";
    public static final String TYPE_PARAM_SEPARATOR = ",";

    public static final int ADDRESS_SOURCE_CHANNEL_ID_INDEX = 0;
    public static final int ADDRESS_AGGREGATION_TYPE_INDEX = 1;
    public static final int ADDRESS_QUALITY_INDEX = 2;
    public static final double DEFAULT_QUALITY = 0.0;

    public static final String AGGREGATION_TYPE_LAST = "LAST";
    public static final String AGGREGATION_TYPE_AVG = "AVG";
    public static final String AGGREGATION_TYPE_DIFF = "DIFF";
    public static final String AGGREGATION_TYPE_PULS_ENERGY = "PULS_ENERGY";

}
