package org.openmuc.framework.driver.aggregator.types;

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.aggregator.AggregationException;
import org.openmuc.framework.driver.aggregator.AggregatorChannel;
import org.openmuc.framework.driver.aggregator.AggregatorUtil;
import org.openmuc.framework.driver.aggregator.ChannelAddress;

public class LastAggregation extends AggregatorChannel {

    public LastAggregation(ChannelAddress simpleAddress, DataAccessService dataAccessService)
            throws AggregationException {
        super(simpleAddress, dataAccessService);
    }

    /**
     * Performs aggregation
     */
    @Override
    public double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException {

        Record record;

        try {
            List<Record> recordList = getLoggedRecords(currentTimestamp, endTimestamp);
            record = AggregatorUtil.getLastRecordOfList(recordList);
        } catch (Exception e) {
            throw new AggregationException(e.getMessage());
        }

        return record.getValue().asDouble();
    }

}
