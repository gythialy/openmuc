/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.datalogger.spi;

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.data.Record;

public interface DataLoggerService {

    String getId();

    void setChannelsToLog(List<LogChannel> channels);

    /**
     * Called by data manager to tell the logger that it should log the given records
     * <p>
     * NOTE: Implementation of this method should be non blocking to avoid blocking in the data manager.
     *
     * @param containers
     *            containers to log
     * @param timestamp
     *            logging timestamp
     */
    void log(List<LoggingRecord> containers, long timestamp);

    void logEvent(List<LoggingRecord> containers, long timestamp);

    boolean logSettingsRequired();

    /**
     * Returns a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code> for
     * the channel with the given <code>channelId</code>.
     *
     * @param channelId
     *            the channel ID.
     * @param startTime
     *            the starting time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @param endTime
     *            the ending time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @return a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code> for
     *         the channel with the given <code>channelId</code>.
     * @throws IOException
     *             if any kind of error occurs accessing the logged data.
     */
    List<Record> getRecords(String channelId, long startTime, long endTime) throws IOException;

    /**
     * Returns the Record with the highest timestamp available in all logged data for the channel with the given
     * <code>channelId</code>. If there are multiple Records with the same timestamp, results may not be consistent.
     * Null if no Record was found.
     * 
     * @param channelId
     *            the channel ID.
     * @return the Record with the highest timestamp available in all logged data for the channel with the given
     *         <code>channelId</code>
     * @throws IOException
     */
    Record getLatestLogRecord(String channelId) throws IOException;
}
