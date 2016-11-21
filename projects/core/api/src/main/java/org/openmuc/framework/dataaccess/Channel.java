/*
 * Copyright 2011-16 Fraunhofer ISE
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

package org.openmuc.framework.dataaccess;

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;

/**
 * The <code>Channel</code> class is used to access a single data field of a communication device. A desired channel can
 * be obtained using the <code>DataAccessService</code>. A channel instance can be used to
 * <ul>
 * <li>Access the latest record. That is the latest data record that the framework either sampled or received by
 * listening or an application set using <code>setLatestRecord</code>.</li>
 * <li>Directly read/write data from/to the corresponding communication device.</li>
 * <li>Access historical data that was stored by a data logger such as SlotsDB.</li>
 * <li>Get configuration information about this channel such as its unit.</li>
 * </ul>
 * <p>
 * Note that only the call of the read or write functions will actually result in a corresponding read or write request
 * being sent to the communication device.
 */
public interface Channel {

    /**
     * Returns the ID of this channel. The ID is usually a meaningful string. It is used to get Channel objects using
     * the <code>DataAccessService</code>.
     * 
     * @return the ID of this channel.
     */
    String getId();

    /**
     * Returns the address of this channel. Returns the empty string if not configured.
     * 
     * @return the address of this channel.
     */
    String getChannelAddress();

    /**
     * Returns the description of this channel. Returns the empty string if not configured.
     * 
     * @return the description of this channel.
     */
    String getDescription();

    /**
     * Returns the unit of this channel. Returns the empty string if not configured. The unit is used for informational
     * purposes only. Neither the framework nor any driver does value conversions based on the configured unit.
     * 
     * @return the unit of this channel.
     */
    String getUnit();

    /**
     * Returns the value type of this channel. The value type specifies how the value of the latest record of a channel
     * is stored. A data logger is encouraged to store values using the configured value type if it supports that value
     * type.
     * <p>
     * Usually an application does not need to know the value type of the channel because it can use the value type of
     * its choice by using the corresponding function of {@link Value} (e.g. {@link Value#asDouble()}). Necessary
     * conversions will be done transparently.
     * <p>
     * If no value type was configured, the default {@link ValueType#DOUBLE} is used.
     * 
     * @return the value type of this channel.
     */
    ValueType getValueType();

    /**
     * Returns the scaling factor. Returns 1.0 if the scaling factor is not configured.
     * <p>
     * The scaling factor is applied in the following cases:
     * <ul>
     * <li>Values received by this channel's driver or from apps through {@link #setLatestRecord(Record)} are multiplied
     * with the scaling factor before they are stored in the latest record.</li>
     * <li>Values written (e.g. using {@link #write(Value)}) are divided by the scaling factor before they are handed to
     * the driver for transmission.</li>
     * </ul>
     * 
     * @return the scaling factor
     */
    double getScalingFactor();

    /**
     * Returns the channel's configured sampling interval in milliseconds. Returns -1 if not configured.
     * 
     * @return the channel's configured sampling interval in milliseconds.
     */
    int getSamplingInterval();

    /**
     * Returns the channel's configured sampling time offset in milliseconds. Returns the default of 0 if not
     * configured.
     * 
     * @return the channel's configured sampling time offset in milliseconds.
     */
    int getSamplingTimeOffset();

    /**
     * Returns the channel's configured logging interval in milliseconds. Returns -1 if not configured.
     * 
     * @return the channel's configured logging interval in milliseconds.
     */
    int getLoggingInterval();

    /**
     * Returns the channel's configured logging time offset in milliseconds. Returns the default of 0 if not configured.
     * 
     * @return the channel's configured logging time offset in milliseconds.
     */
    int getLoggingTimeOffset();

    /**
     * Returns the unique name of the communication driver that is used by this channel to read/write data.
     * 
     * @return the unique name of the communication driver that is used by this channel to read/write data.
     */
    String getDriverName();

    /**
     * Returns the channel's device address.
     * 
     * @return the channel's device address.
     */
    String getDeviceAddress();

    /**
     * Returns the name of the communication device that this channel belongs to. The empty string if not configured.
     * 
     * @return the name of the communication device that this channel belongs to.
     */
    String getDeviceName();

    /**
     * Returns the description of the communication device that this channel belongs to. The empty string if not
     * configured.
     * 
     * @return the description of the communication device that this channel belongs to.
     */
    String getDeviceDescription();

    /**
     * Returns the current channel state.
     * 
     * @return the current channel state.
     */
    ChannelState getChannelState();

    /**
     * Returns the current state of the communication device that this channel belongs to.
     * 
     * @return the current state of the communication device that this channel belongs to.
     */
    DeviceState getDeviceState();

    /**
     * Adds a listener that is notified of new records received by sampling or listening.
     * 
     * @param listener
     *            the record listener that is notified of new records.
     */
    void addListener(RecordListener listener);

    /**
     * Removes a record listener.
     * 
     * @param listener
     *            the listener shall be removed.
     */
    void removeListener(RecordListener listener);

    /**
     * Returns <code>true</code> if a connection to the channel's communication device exist.
     * 
     * @return <code>true</code> if a connection to the channel's communication device exist.
     */
    boolean isConnected();

    /**
     * Returns the latest record of this channel. Every channel holds its latest record in memory. There exist three
     * possible source for the latest record:
     * <ul>
     * <li>It may be provided by a communication driver that was configured to sample or listen on the channel. In this
     * case the timestamp of the record represents the moment in time that the value was received by the driver.</li>
     * <li>An application may also set the latest record using <code>setLatestRecord</code>.</li>
     * <li>Finally values written using <code>write</code> are also stored as the latest record</li>
     * </ul>
     * 
     * Note that the latest record is never <code>NULL</code>. When a channel is first created its latest record is
     * automatically initialized with a flag that indicates that its value is not valid.
     * 
     * @return the latest record.
     */
    Record getLatestRecord();

    /**
     * Sets the latest record of this channel. This function should only be used with channels that are neither sampling
     * nor listening. Using this function it is possible to realize "virtual" channels that get their data not from
     * drivers but from applications in the framework.
     * <p>
     * Note that the framework treats the passed record in exactly the same way as if it had been received from a
     * driver. In particular that means:
     * <ul>
     * <li>If data logging is enabled for this channel the latest record is being logged by the registered loggers.</li>
     * <li>Other applications can access the value set by this function using <code>getLatestRecord</code>.</li>
     * <li>Applications are notified of the new record if they registered as listeners using <code>addListener</code>.
     * <li>If a scaling factor has been configured for this channel then the value passed to this function is scaled.
     * </li>
     * </ul>
     * 
     * @param record
     *            the record to be set.
     */
    void setLatestRecord(Record record);

    /**
     * Writes the given value to the channel's corresponding data field in the connected communication device. If an
     * error occurs, the returned <code>Flag</code> will indicate this.
     * 
     * @param value
     *            the value that is to be written
     * @return the flag indicating whether the value was successfully written ( <code>Flag.VALID</code>) or not (any
     *         other flag).
     */
    Flag write(Value value);

    /**
     * Schedules a List&lt;records&gt; with future timestamps as write tasks <br>
     * This function will schedule single write tasks to the provided timestamps.<br>
     * Once this function is called, previously scheduled write tasks will be erased.<br>
     * 
     * @param records
     *            each record contains the value that is to be written and the timestamp indicating when it should be
     *            written. The flag of the record is ignored.
     */
    void write(List<Record> records);

    /**
     * Returns a <code>WriteValueContainer</code> that corresponds to this channel. This container can be passed to the
     * write function of <code>DataAccessService</code> to write several values in one transaction.
     * 
     * @return a <code>WriteValueContainer</code> that corresponds to this channel.
     */
    WriteValueContainer getWriteContainer();

    /**
     * Actively reads a value from the channel's corresponding data field in the connected communication device. If an
     * error occurs it will be indicated in the returned record's flag.
     * 
     * @return the record containing the value read, the time the value was received and a flag indicating success (
     *         <code>Flag.VALID</code>) or a an error (any other flag).
     */
    Record read();

    /**
     * Returns a <code>ReadRecordContainer</code> that corresponds to this channel. This container can be passed to the
     * <code>read</code> function of <code>DataAccessService</code> to read several values in one transaction.
     * 
     * @return a <code>ReadRecordContainer</code> that corresponds to this channel.
     */
    ReadRecordContainer getReadContainer();

    /**
     * Returns the logged data record whose timestamp equals the given <code>time</code>. Note that it is the data
     * logger's choice whether it stores values using the timestamp that the driver recorded when it received it or the
     * timestamp at which the value is to be logged. If the former is the case then this function is not useful because
     * it is impossible for an application to know the exact time at which a value was received. In this case use
     * <code>getLoggedRecords</code> instead.
     * 
     * @param time
     *            the time in milliseconds since midnight, January 1, 1970 UTC.
     * @return the record that has been stored by the framework's data logger at the given <code>timestamp</code>.
     *         Returns <code>null</code> if no record exists for this point in time.
     * @throws DataLoggerNotAvailableException
     *             if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException
     *             if any kind of error occurs accessing the logged data.
     */
    Record getLoggedRecord(long time) throws DataLoggerNotAvailableException, IOException;

    /**
     * Returns a list of all logged data records with timestamps from <code>startTime</code> up until now.
     * 
     * @param startTime
     *            the starting time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @return a list of all logged data records with timestamps from <code>startTime</code> up until now.
     * @throws DataLoggerNotAvailableException
     *             if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException
     *             if any kind of error occurs accessing the logged data.
     */
    List<Record> getLoggedRecords(long startTime) throws DataLoggerNotAvailableException, IOException;

    /**
     * Returns a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code>
     * inclusive.
     * 
     * @param startTime
     *            the starting time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @param endTime
     *            the ending time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @return a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code>
     *         inclusive.
     * @throws DataLoggerNotAvailableException
     *             if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException
     *             if any kind of error occurs accessing the logged data.
     */
    List<Record> getLoggedRecords(long startTime, long endTime) throws DataLoggerNotAvailableException, IOException;

}
