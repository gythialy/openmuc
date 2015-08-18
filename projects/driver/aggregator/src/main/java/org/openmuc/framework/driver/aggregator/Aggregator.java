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

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.driver.aggregator.exeptions.AggregationException;
import org.openmuc.framework.driver.aggregator.exeptions.ChannelNotAccessibleException;
import org.openmuc.framework.driver.aggregator.exeptions.SomethingWrongWithRecordException;
import org.openmuc.framework.driver.aggregator.exeptions.WrongChannelAddressFormatException;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver which performs aggregation of logged values from a channel. It uses the DriverService and the
 * DataAccessService. It is therefore a kind of OpenMUC driver/application mix. The aggregator is fully configurable
 * through the channel config file.
 * <p>
 * Example: <br>
 * Channel A (channelA) is sampled and logged every 10 seconds.
 * 
 * <pre>
 * &lt;channelid="channelA"&gt;
 *   &lt;samplingInterval&gt;10s&lt;/samplingInterval&gt;
 *   &lt;loggingInterval&gt;10s&lt;/loggingInterval&gt;
 * &lt;/channel&gt;
 * </pre>
 * 
 * 
 * Now you want a channel B (channelB) which contains the same values as channel A but in a 1 minute resolution by using
 * the 'average' as aggregation type. You can achieve this by simply adding the aggregator driver to your channel config
 * file and define a the channel B as follows:
 * 
 * <pre>
 * &lt;driver id="aggregator"&gt;
 *   &lt;device id="aggregatordevice"&gt;
 *     &lt;channelid="channelB"&gt;
 *       &lt;channelAddress&gt;channelA:avg&lt;/channelAddress&gt;
 *       &lt;samplingInterval&gt;60s&lt;/samplingInterval&gt;
 *       &lt;loggingInterval&gt;60s&lt;/loggingInterval&gt;
 *     &lt;/channel&gt;
 *   &lt;/device&gt;
 * &lt;/driver&gt;
 * </pre>
 * 
 * The new (aggregated) channel has the id channelB. The channel address consists of the channel id of the original
 * channel and the aggregation type which is channelA:avg in this example. OpenMUC calls the read method of the
 * aggregator every minute. The aggregator then gets all logged records from channelA of the last minute, calculates the
 * average and sets this value for the record of channelB.
 * 
 * <p>
 * 
 * NOTE: It's recommended to specify the samplingTimeOffset for channelB. It should be between samplingIntervalB -
 * samplingIntervalA and samplingIntervalB. In this example: 50 < offset < 60. This constraint ensures that values are
 * AGGREGATED CORRECTLY. At hh:mm:55 the aggregator gets the logged values of channelA and at hh:mm:60 respectively
 * hh:mm:00 the aggregated value is logged.
 * 
 * <pre>
 * &lt;driver id="aggregator"&gt;
 *   &lt;device id="aggregatordevice"&gt;
 *     &lt;channelid="channelB"&gt;
 *       &lt;channelAddress&gt;channelA:avg&lt;/channelAddress&gt;
 *       &lt;samplingInterval&gt;60s&lt;/samplingInterval&gt;
 *       &lt;samplingTimeOffset&gt;55s&lt;/samplingTimeOffset&gt;
 *       &lt;loggingInterval&gt;60s&lt;/loggingInterval&gt;
 *     &lt;/channel&gt;
 *   &lt;/device&gt;
 * &lt;/driver&gt;
 * </pre>
 * 
 * 
 */
// TODO: might add an adjustable percentage of errors in records accepted
// TODO: works only on double values so far
// TODO: some Unit / integration tests whould be nice
public class Aggregator implements DriverService, Connection {

	private final static Logger logger = LoggerFactory.getLogger(Aggregator.class);

	private DataAccessService dataAccessService;

	protected void setDataAccessService(DataAccessService dataAccessService) {
		this.dataAccessService = dataAccessService;
	}

	protected void unsetDataAccessService(DataAccessService dataAccessService) {
		this.dataAccessService = null;
	}

	@Override
	public DriverInfo getInfo() {

		String driverId = "aggregator";
		String description = "Is able to aggregate logged values of a channel and writes the aggregated value into a new channel. Different aggregation types supported.";
		String deviceAddressSyntax = "not needed";
		String parametersSyntax = "not needed";
		String channelAddressSyntax = "<id of channel which should be aggregated>:<type>[:<optionalLongSetting1>][:<optionalLongSetting2>] + \n Supported types: "
				+ EAggregationType.getSupportedValues();
		String deviceScanParametersSyntax = "not supported";

		return new DriverInfo(driverId, description, deviceAddressSyntax, parametersSyntax, channelAddressSyntax,
				deviceScanParametersSyntax);
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {

		long currentTimestamp = getCurrentTimestamp();
		long endTimestamp = getEndTimestamp(currentTimestamp);

		for (ChannelRecordContainer container : containers) {

			try {

				Channel aggregatedChannel = container.getChannel();
				AggregatorChannelAddress address = new AggregatorChannelAddress(aggregatedChannel.getChannelAddress());

				Channel sourceChannel = dataAccessService.getChannel(address.getSourceChannelId());
				checkIfChannelsNotNull(sourceChannel, aggregatedChannel);

				// NOTE: logging, not sampling interval because aggregator accesses logged values
				long sourceLoggingInterval = sourceChannel.getLoggingInterval();
				long aggregationInterval = aggregatedChannel.getSamplingInterval();
				long aggregationSamplingTimeOffset = aggregatedChannel.getSamplingTimeOffset();
				checkIntervals(sourceLoggingInterval, aggregationInterval, aggregationSamplingTimeOffset);

				long startTimestamp = currentTimestamp - aggregationInterval;
				List<Record> records = sourceChannel.getLoggedRecords(startTimestamp, endTimestamp);

				// for debugging - KEEP IT!
				// if (records.size() > 0) {
				// SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
				// for (Record r : records) {
				// logger.debug("List records: " + sdf.format(r.getTimestamp()) + " " + r.getValue().asDouble());
				// }
				// logger.debug("start:       " + sdf.format(startTimestamp) + "  timestamp = " + startTimestamp);
				// logger.debug("end:         " + sdf.format(endTimestamp) + "  timestamp = " + endTimestamp);
				// logger.debug("List start:  " + sdf.format(records.get(0).getTimestamp()));
				// logger.debug("List end:    " + sdf.format(records.get(records.size() - 1).getTimestamp()));
				// }

				checkNumberOfRecords(records, sourceLoggingInterval, aggregationInterval);

				double aggregatedValue = performAggregation(sourceChannel, records, address, aggregatedChannel);
				container.setRecord(new Record(new DoubleValue(aggregatedValue), currentTimestamp, Flag.VALID));

				// for debugging
				// logger.debug("set record: " + aggregatedValue + " valid.");

			} catch (WrongChannelAddressFormatException e) {
				logger.error("WrongChannelAddressFormatException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			} catch (ChannelNotAccessibleException e) {
				logger.error("ChannelNotAccessibleException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			} catch (DataLoggerNotAvailableException e) {
				logger.error("DataLoggerNotAvailableException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			} catch (IOException e) {
				logger.error("IOException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			} catch (SomethingWrongWithRecordException e) {
				logger.error("SomethingWrongWithRecordException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			} catch (AggregationException e) {
				logger.warn("AggregationException occurred " + e.getMessage());
				setRecordWithErrorFlag(container, currentTimestamp);
			}

		}
		return null;
	}

	/**
	 * @return the current timestamp where milliseconds are set to 000: e.g. 10:45:00.015 --> 10:45:00.000
	 */
	private long getCurrentTimestamp() {

		return (System.currentTimeMillis() / 1000) * 1000;
	}

	/**
	 * endTimestamp must be slightly before the currentTimestamp Example: Aggregate a channel from 10:30:00 to 10:45:00
	 * to 15 min values. 10:45:00 should be the timestamp of the aggregated value therefore the aggregator has to get
	 * logged values from 10:30:00,000 till 10:44:59,999. 10:45:00 is part of the next 15 min interval.
	 * 
	 * @param currentTimestamp
	 * @return
	 */
	private long getEndTimestamp(long currentTimestamp) {

		return currentTimestamp - 1;
	}

	/**
	 * Checks limitations of the sampling/aggregating intervals and sourceSamplingOffset
	 * 
	 * @param sourceLoggingInterval
	 * @param aggregationInterval
	 * @param sourceSamplingOffset
	 * @throws AggregationException
	 */
	private void checkIntervals(long sourceLoggingInterval, long aggregationInterval, long aggregationSamplingTimeOffset)
			throws AggregationException {

		// check 1
		// -------
		if (aggregationInterval < sourceLoggingInterval) {
			throw new AggregationException(
					"Sampling interval of aggregator channel must be bigger than logging interval of source channel");
		}

		// check 2
		// -------
		long remainder = aggregationInterval % sourceLoggingInterval;
		if (remainder != 0) {
			throw new AggregationException(
					"Sampling interval of aggregator channel must be a multiple of the logging interval of the source channel");
		}

		// check 3
		// -------
		if (sourceLoggingInterval < 1000) {
			// FIXME (priority low) milliseconds are cut from the endTimestamp (refer to read method). If the logging
			// interval of the source channel is smaller than 1 second this might lead to errors.
			throw new AggregationException("Logging interval of source channel musst be >= 1 second");
		}

	}

	private void checkNumberOfRecords(List<Record> records, long sourceSamplingInterval, long aggregationInterval)
			throws AggregationException {

		// The check if intervals are multiples of each other ist done in the checkIntervals Method
		long expectedNumberOfRecords = aggregationInterval / sourceSamplingInterval;

		if (records.size() != expectedNumberOfRecords) {
			throw new AggregationException("Expected " + expectedNumberOfRecords + " historical records but received "
					+ records.size());
		}

	}

	private void checkIfChannelsNotNull(Channel sourceChannel, Channel aggregatedChannel)
			throws ChannelNotAccessibleException {

		if (aggregatedChannel == null || sourceChannel == null) {
			throw new ChannelNotAccessibleException("");
		}
	}

	private void setRecordWithErrorFlag(ChannelRecordContainer container, long endTimestamp) {

		container.setRecord(new Record(null, endTimestamp, Flag.DRIVER_ERROR_READ_FAILURE));
	}

	private double performAggregation(Channel sourceChannel, List<Record> recordList, AggregatorChannelAddress address,
			Channel aggregatedChannel) throws SomethingWrongWithRecordException, WrongChannelAddressFormatException {

		double aggregatedValue;

		switch (address.getAggregationType()) {
		case AVG:
			aggregatedValue = AggregatorLogic.getAverage(recordList);
			break;
		case LAST:
			aggregatedValue = AggregatorLogic.getLast(recordList);
			break;
		case DIFF:
			aggregatedValue = AggregatorLogic.getDiffBetweenLastAndFirstRecord(recordList);
			break;
		case PULSES_ENERGY:
			aggregatedValue = AggregatorLogic.getPulsesEnergy(sourceChannel, recordList, address, aggregatedChannel);
			break;
		default:
			// TODO not the best exception type
			throw new WrongChannelAddressFormatException("Aggregation type: " + address.getAggregationType().name()
					+ " not implemented yet");
		}

		return aggregatedValue;
	}

	@Override
	public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
			throws UnsupportedOperationException, ConnectionException {

		throw new UnsupportedOperationException();
	}

	@Override
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {

		throw new UnsupportedOperationException();
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		throw new UnsupportedOperationException();
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {

		throw new UnsupportedOperationException();
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException,
			ArgumentSyntaxException, ScanException, ConnectionException {

		throw new UnsupportedOperationException();
	}

	@Override
	public Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException,
			ConnectionException {

		// no connection needed so far
		return this;
	}

	@Override
	public void disconnect() {

		// no disconnect needed so far
	}
}
