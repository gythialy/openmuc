package org.openmuc.framework.driver.aggregator.types;

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.aggregator.AggregationException;
import org.openmuc.framework.driver.aggregator.AggregatorChannel;
import org.openmuc.framework.driver.aggregator.ChannelAddress;

public class AverageAggregation extends AggregatorChannel {

    public AverageAggregation(ChannelAddress simpleAddress, DataAccessService dataAccessService)
            throws AggregationException {
        super(simpleAddress, dataAccessService);
    }

    @Override
    public double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException {

        double value = 0;

        List<Record> recordList;
        try {
            recordList = getLoggedRecords(currentTimestamp, endTimestamp);
            value = getAverage(recordList);
        } catch (Exception e) {
            throw new AggregationException(e.getMessage());
        }

        return value;
    }

    /**
     * Calculates the average of the all records
     */
    private double getAverage(List<Record> recordList) throws AggregationException {

        double sum = 0;

        for (Record record : recordList) {
            sum = sum + record.getValue().asDouble();
        }

        double average = sum / recordList.size();

        return average;
    }

}
