package org.openmuc.framework.driver.aggregator.types;

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.aggregator.AggregationException;
import org.openmuc.framework.driver.aggregator.AggregatorChannel;
import org.openmuc.framework.driver.aggregator.AggregatorUtil;
import org.openmuc.framework.driver.aggregator.ChannelAddress;

public class DiffAggregation extends AggregatorChannel {

    public DiffAggregation(ChannelAddress simpleAddress, DataAccessService dataAccessService)
            throws AggregationException {
        super(simpleAddress, dataAccessService);
    }

    @Override
    public double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException {

        try {
            List<Record> recordList = getLoggedRecords(currentTimestamp, endTimestamp);
            return calcDiffBetweenLastAndFirstRecord(recordList);
        } catch (AggregationException e) {
            throw e;
        } catch (Exception e) {
            throw new AggregationException(e.getMessage());
        }

    }

    /**
     * Calculates the difference between the last and first value of the list. <br>
     * Can be used to determine the energy per interval
     */
    private static double calcDiffBetweenLastAndFirstRecord(List<Record> recordList) throws AggregationException {
        if (recordList.size() < 2) {
            throw new AggregationException("List holds less than 2 records, calculation of difference not possible.");
        }
        double end = AggregatorUtil.findLastRecordIn(recordList).getValue().asDouble();
        double start = recordList.get(0).getValue().asDouble();

        return end - start;
    }

}
