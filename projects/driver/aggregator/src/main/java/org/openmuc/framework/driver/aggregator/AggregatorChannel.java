package org.openmuc.framework.driver.aggregator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.TypeConversionException;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;

public abstract class AggregatorChannel {

    public abstract double aggregate(long currentTimestamp, long endTimestamp) throws AggregationException;

    protected ChannelAddress channelAddress;
    protected Channel aggregatedChannel;
    protected Channel sourceChannel;

    protected long sourceLoggingInterval;
    protected long aggregationInterval;
    protected long aggregationSamplingTimeOffset;

    // TODO dataAccessService wird über viele ebenen durchgereicht, wie kann ich das vermeiden?
    // Brauche den dataAccessService eigentlich nur hier

    public AggregatorChannel(ChannelAddress channelAddress, DataAccessService dataAccessService)
            throws AggregationException {

        this.channelAddress = channelAddress;
        this.aggregatedChannel = channelAddress.getContainer().getChannel();
        this.sourceChannel = dataAccessService.getChannel(channelAddress.getSourceChannelId());

        checkIfChannelsNotNull();

        // NOTE: logging, not sampling interval because aggregator accesses logged values
        this.sourceLoggingInterval = sourceChannel.getLoggingInterval();
        this.aggregationInterval = aggregatedChannel.getSamplingInterval();
        this.aggregationSamplingTimeOffset = aggregatedChannel.getSamplingTimeOffset();

        checkIntervals();
    }

    public List<Record> getLoggedRecords(long currentTimestamp, long endTimestamp)
            throws DataLoggerNotAvailableException, IOException, AggregationException {

        long startTimestamp = currentTimestamp - aggregationInterval;
        List<Record> records = sourceChannel.getLoggedRecords(startTimestamp, endTimestamp);

        // for debugging - KEEP IT!
        // if (records.size() > 0) {
        // SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        // for (Record r : records) {
        // logger.debug("List records: " + sdf.format(r.getTimestamp()) + " " + r.getValue().asDouble());
        // }
        // logger.debug("start: " + sdf.format(startTimestamp) + " timestamp = " + startTimestamp);
        // logger.debug("end: " + sdf.format(endTimestamp) + " timestamp = " + endTimestamp);
        // logger.debug("List start: " + sdf.format(records.get(0).getTimestamp()));
        // logger.debug("List end: " + sdf.format(records.get(records.size() - 1).getTimestamp()));
        // }

        checkNumberOfRecords(records);

        return records;
    }

    private void checkIfChannelsNotNull() throws AggregationException {

        if (aggregatedChannel == null) {
            throw new AggregationException("aggregatedChannel is null");
        }

        if (sourceChannel == null) {
            throw new AggregationException("sourceChannel is null");
        }
    }

    /**
     * Checks limitations of the sampling/aggregating intervals and sourceSamplingOffset
     * 
     * @param sourceLoggingInterval
     * @param aggregationInterval
     * @param sourceSamplingOffset
     * @throws AggregationException
     */
    private void checkIntervals()

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

    private void checkNumberOfRecords(List<Record> records) throws AggregationException {

        // The check if intervals are multiples of each other is done in the checkIntervals Method

        removeErrorRecords(records);

        long expectedNumberOfRecords = Math.round((double) aggregationInterval / sourceLoggingInterval);
        long necessaryRecords = Math.round(expectedNumberOfRecords * channelAddress.getQuality());
        int validRecords = records.size();

        if (validRecords < necessaryRecords) {
            throw new AggregationException("Insufficent number of logged records for channel "
                    + channelAddress.getContainer().getChannel().getId() + ". Valid logged records: " + validRecords
                    + " Expected: " + necessaryRecords + " (at least)" + " quality:" + channelAddress.getQuality()
                    + " aggregationInterval:" + aggregationInterval + "ms sourceLoggingInterval:"
                    + sourceLoggingInterval + "ms");
        }

    }

    /**
     * Removes invalid records from the list. All records remaining a valid DOUBLE records.
     * 
     * NOTE: directly manipulates the records object for all future operations!
     */
    private void removeErrorRecords(List<Record> records) {

        for (Iterator<Record> recordIterator = records.iterator(); recordIterator.hasNext();) {
            Record record = recordIterator.next();
            if (record == null) {
                recordIterator.remove();
            }
            else {
                // check if the value is null or the flag isn't valid
                if (record.getValue() == null || !record.getFlag().equals(Flag.VALID)) {
                    recordIterator.remove();
                }
                else {
                    try {
                        // check if the value can be converted to double
                        record.getValue().asDouble();
                    } catch (TypeConversionException e) {
                        // remove record since it can't be cast to double for further processing
                        recordIterator.remove();
                    }
                }
            }
        }
    }
}
