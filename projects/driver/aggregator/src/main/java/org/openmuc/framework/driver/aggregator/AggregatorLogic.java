/*
 * Copyright 2011-15 Fraunhofer ISE
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

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.aggregator.exeptions.SomethingWrongWithRecordException;
import org.openmuc.framework.driver.aggregator.exeptions.WrongChannelAddressFormatException;

/**
 * Provides static methods which perform the aggregation according to the aggregation type.
 */
public class AggregatorLogic {

	final static double SHORT_MAX = 65535.0;

	/**
	 * Calculates the average of the all records
	 * 
	 * @param recordList
	 * @return the average
	 * @throws SomethingWrongWithRecordException
	 */
	public static double getAverage(List<Record> recordList) throws SomethingWrongWithRecordException {

		double sum = 0;

		for (Record record : recordList) {
			sum = sum + AggregatorUtil.getDoubleRecordValue(record);
		}

		double average = sum / recordList.size();

		return average;
	}

	/**
	 * Returns the value of the last record of the list
	 * <p>
	 * Can be used for energy aggregation. Smart meter sums the energy automatically therefore the last value contains
	 * the aggregated value
	 * 
	 * @param recordList
	 * @return the value of the last record of the list
	 * @throws SomethingWrongWithRecordException
	 */
	public static double getLast(List<Record> recordList) throws SomethingWrongWithRecordException {

		return AggregatorUtil.getDoubleRecordValue(getLastRecordOfList(recordList));
	}

	/**
	 * Calculates the difference between the last and first value of the list.
	 * <p>
	 * Can be used to determine the energy per interval
	 * 
	 * @param recordList
	 * @return the difference
	 * @throws SomethingWrongWithRecordException
	 */
	public static double getDiffBetweenLastAndFirstRecord(List<Record> recordList)
			throws SomethingWrongWithRecordException {

		if (recordList.size() < 2) {
			throw new SomethingWrongWithRecordException(
					"List holds less than 2 records, calculation of difference not possible.");
		}

		double end = AggregatorUtil.getDoubleRecordValue(getLastRecordOfList(recordList));
		double start = AggregatorUtil.getDoubleRecordValue(recordList.get(0));
		double diff = end - start;
		return diff;
	}

	public static double getPulsesEnergy(Channel sourceChannel, List<Record> recordList,
			AggregatorChannelAddress address, Channel aggregatedChannel) throws SomethingWrongWithRecordException,
			WrongChannelAddressFormatException {

		double aggregatedValue;
		double pulsesPerWh = address.getOptionalSeting1();
		double maxCounterValue = address.getOptionalSeting2();

		if (pulsesPerWh > 0) {
			if (pulsesPerWh <= 0) {
				maxCounterValue = SHORT_MAX; // if optionalLongSetting1 is negative or null then set default value
			}
			aggregatedValue = AggregatorLogic.getImpulsValue(sourceChannel, recordList,
					aggregatedChannel.getSamplingInterval(), pulsesPerWh, maxCounterValue);
		}
		else {
			throw new WrongChannelAddressFormatException(
					"optionalLongSetting1 (pulses per Wh) has to be greater then 0.");
		}

		return aggregatedValue;
	}

	private static double getImpulsValue(Channel sourceChannel, List<Record> recordList, long samplingInterval,
			double pulsesPerX, double maxCounterValue) throws SomethingWrongWithRecordException {

		if (recordList.size() < 1) {
			throw new SomethingWrongWithRecordException(
					"List holds less than 1 records, calculation of pulses not possible.");
		}

		Record lastRecord = getLastRecordOfList(recordList);
		double past = AggregatorUtil.getDoubleRecordValue(lastRecord);
		double actual = AggregatorUtil.getWaitForLatestRecordValue(sourceChannel, lastRecord);
		double power = calcPulsesValue(actual, past, pulsesPerX, samplingInterval, maxCounterValue);
		return power;
	}

	private static Record getLastRecordOfList(List<Record> recordList) throws SomethingWrongWithRecordException {

		if (recordList.isEmpty()) {
			throw new SomethingWrongWithRecordException("Empty record list.");
		}
		else if (recordList.size() == 1) {
			// only one record in list which is automatically the last one.
			return recordList.get(0);
		}
		else {
			return recordList.get(recordList.size() - 1);
		}
	}

	private static double calcPulsesValue(double actualPulses, double pulsesHist, double pulsesPerX,
			long loggingInterval, double maxCounterValue) {

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

}
