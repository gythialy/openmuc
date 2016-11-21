package org.openmuc.framework.driver.aggregator.types;

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.aggregator.AggregationException;
import org.openmuc.framework.driver.aggregator.AggregatorChannel;
import org.openmuc.framework.driver.aggregator.AggregatorConstants;
import org.openmuc.framework.driver.aggregator.AggregatorUtil;
import org.openmuc.framework.driver.aggregator.ChannelAddress;

public class PulseEnergyAggregation extends AggregatorChannel {

    private static final double SHORT_MAX = 65535.0;

    private static final int INDEX_PULSES_WH = 1;
    private static final int INDEX_MAX_COUNTER = 2;

    public PulseEnergyAggregation(ChannelAddress simpleAddress, DataAccessService dataAccessService)
            throws AggregationException {
        super(simpleAddress, dataAccessService);
    }

    /**
     * Performs aggregation
     */
    @Override
    public double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException {

        double value = 0;

        try {
            List<Record> recordList = getLoggedRecords(currentTimestamp, endTimestamp);
            value = getPulsesEnergy(channelAddress, sourceChannel, recordList, aggregatedChannel);
        } catch (Exception e) {
            throw new AggregationException(e.getMessage());
        }

        return value;
    }

    private double getPulsesEnergy(ChannelAddress simpleAdress, Channel sourceChannel, List<Record> recordList,
            Channel aggregatedChannel) throws AggregationException, AggregationException {

        double aggregatedValue;
        double pulsesPerWh;
        double maxCounterValue;

        // parse type address params. length = 3: <type,pulsePerWh,maxCounterValue>
        String[] typeParams = simpleAdress.getAggregationType().split(AggregatorConstants.TYPE_PARAM_SEPARATOR);

        if (typeParams.length == 3) {
            pulsesPerWh = Double.valueOf(typeParams[INDEX_PULSES_WH]);
            maxCounterValue = Double.valueOf(typeParams[INDEX_MAX_COUNTER]);
        }
        else {
            throw new AggregationException("Wrong parameters for PULSE_ENERGY.");
        }

        if (pulsesPerWh > 0) {
            if (maxCounterValue <= 0) {
                maxCounterValue = SHORT_MAX; // if negative or null then set default value
            }
            aggregatedValue = getImpulsValue(sourceChannel, recordList, aggregatedChannel.getSamplingInterval(),
                    pulsesPerWh, maxCounterValue);
        }
        else {
            throw new AggregationException("Parameter pulses per Wh has to be greater then 0.");
        }

        return aggregatedValue;
    }

    private double getImpulsValue(Channel sourceChannel, List<Record> recordList, long samplingInterval,
            double pulsesPerX, double maxCounterValue) throws AggregationException {

        if (recordList.size() < 1) {
            throw new AggregationException("List holds less than 1 records, calculation of pulses not possible.");
        }

        Record lastRecord = AggregatorUtil.getLastRecordOfList(recordList);
        double past = lastRecord.getValue().asDouble();
        double actual = getWaitForLatestRecordValue(sourceChannel, lastRecord);
        double power = calcPulsesValue(actual, past, pulsesPerX, samplingInterval, maxCounterValue);
        return power;
    }

    private double calcPulsesValue(double actualPulses, double pulsesHist, double pulsesPerX, long loggingInterval,
            double maxCounterValue) {

        double power;
        double pulses = actualPulses - pulsesHist;

        if (pulses >= 0.0) {
            pulses = actualPulses - pulsesHist;
        }
        else {
            pulses = (maxCounterValue - pulsesHist) + actualPulses;
        }
        power = pulses / pulsesPerX * (loggingInterval / 1000.);

        return power;
    }

    private double getWaitForLatestRecordValue(Channel sourceChannel, Record lastRecord) {

        double returnValue;

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } while (sourceChannel.getLatestRecord().getTimestamp().equals(lastRecord.getTimestamp()));

        returnValue = sourceChannel.getLatestRecord().getValue().asDouble();
        return returnValue;
    }

}
