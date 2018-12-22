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

    @Override
    public double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException {

        try {
            List<Record> recordList = getLoggedRecords(currentTimestamp, endTimestamp);
            return getPulsesEnergy(channelAddress, sourceChannel, recordList, aggregatedChannel);
        } catch (AggregationException e) {
            throw e;
        } catch (Exception e) {
            throw new AggregationException(e.getMessage());
        }

    }

    private static double getPulsesEnergy(ChannelAddress simpleAdress, Channel sourceChannel, List<Record> recordList,
            Channel aggregatedChannel) throws AggregationException, AggregationException {

        // parse type address params. length = 3: <type,pulsePerWh,maxCounterValue>
        String[] typeParams = simpleAdress.getAggregationType().split(AggregatorConstants.TYPE_PARAM_SEPARATOR);

        if (typeParams.length != 3) {
            throw new AggregationException("Wrong parameters for PULSE_ENERGY.");
        }

        final double pulsesPerWh = Double.valueOf(typeParams[INDEX_PULSES_WH]);
        double maxCounterValue = Double.valueOf(typeParams[INDEX_MAX_COUNTER]);

        if (pulsesPerWh <= 0) {
            throw new AggregationException("Parameter pulses per Wh has to be greater then 0.");
        }

        if (maxCounterValue <= 0) {
            maxCounterValue = SHORT_MAX; // if negative or null then set default value
        }

        return calcImpulsValue(sourceChannel, recordList, aggregatedChannel.getSamplingInterval(), pulsesPerWh,
                maxCounterValue);
    }

    private static double calcImpulsValue(Channel sourceChannel, List<Record> recordList, long samplingInterval,
            double pulsesPerX, double maxCounterValue) throws AggregationException {

        if (recordList.isEmpty()) {
            throw new AggregationException("List holds less than 1 records, calculation of pulses not possible.");
        }

        Record lastRecord = AggregatorUtil.findLastRecordIn(recordList);
        double past = lastRecord.getValue().asDouble();
        double actual = retrieveLatestRecordValueWithTs(sourceChannel, lastRecord);

        return calcPulsesValue(actual, past, pulsesPerX, samplingInterval, maxCounterValue);
    }

    private static double calcPulsesValue(double actualPulses, double pulsesHist, double pulsesPerX,
            long loggingInterval, double maxCounterValue) {

        double pulses = actualPulses - pulsesHist;

        if (pulses >= 0.0) {
            pulses = actualPulses - pulsesHist;
        }
        else {
            pulses = (maxCounterValue - pulsesHist) + actualPulses;
        }
        return pulses / pulsesPerX * (loggingInterval / 1000.);
    }

    private static double retrieveLatestRecordValueWithTs(Channel srcChannel, Record lastRecord) {
        final long timestamp = lastRecord.getTimestamp();
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (srcChannel.getLatestRecord().getTimestamp() == timestamp);

        return srcChannel.getLatestRecord().getValue().asDouble();
    }

}
