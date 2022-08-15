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
package org.openmuc.framework.driver.csv.channel;

import java.util.List;

import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CsvTimeChannel implements CsvChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvTimeChannel.class);

    /** remember index of last valid sampled value */
    protected int lastReadIndex = 0;

    protected int maxIndex;
    protected List<String> data;
    protected boolean rewind;
    protected boolean isInitialised = false;
    long[] timestamps;
    long firstTimestamp;
    long lastTimestamp;

    public CsvTimeChannel(List<String> data, boolean rewind, long[] timestamps) {
        this.data = data;
        this.maxIndex = data.size() - 1;
        this.rewind = rewind;
        this.timestamps = timestamps;
        this.firstTimestamp = timestamps[0];
        this.lastTimestamp = timestamps[timestamps.length - 1];
    }

    protected int searchNextIndex(long samplingTime) throws CsvException {
        int index;

        if (isWithinTimeperiod(samplingTime)) {
            index = handleWithinTimeperiod(samplingTime);
        }
        else { // is outside time period
            index = handleOutsideTimeperiod(samplingTime);
        }

        if (!isInitialised) {
            isInitialised = true;
        }
        return index;
    }

    private int handleWithinTimeperiod(long samplingTime) throws CsvException {
        if (isBehindLastReadIndex(samplingTime)) {
            return getIndexByRegularSearch(samplingTime);
        }
        else if (isBeforeLastReadIndex(samplingTime)) {
            return handleBeforeLastReadIndex(samplingTime);
        }
        else { // is same timestamp
            return lastReadIndex;
        }
    }

    private int handleBeforeLastReadIndex(long samplingTime) throws CsvException {
        if (rewind) {
            rewindIndex();
            return getIndexByRegularSearch(samplingTime);
        }
        else { // rewind disabled
            throw new TimeTravelException(
                    "Current sampling time is before the last sampling time. Since rewind is disabled, driver can't get value for current sampling time.");
        }
    }

    private int handleOutsideTimeperiod(long samplingTime) throws CsvException {
        if (isBeforeFirstTimestamp(samplingTime)) {
            return handleOutsideTimeperiodEarly(samplingTime);
        }
        else { // is after last timestamp
            LOGGER.warn(
                    "Current sampling time is behind last available timestamp of csv file. Returning value corresponding to last timestamp in file.");
            return maxIndex;
        }
    }

    /**
     * Search in chronological order beginning from last read index. This is the regular case since the samplingTime
     * will normally increase with each read called*
     */
    private int getIndexByRegularSearch(long samplingTime) {

        long nextTimestamp;
        int nextIndex;

        do {
            nextIndex = lastReadIndex + 1;
            if (nextIndex > maxIndex) {
                return maxIndex;
            }
            nextTimestamp = timestamps[nextIndex];
            lastReadIndex = nextIndex;
        } while (samplingTime > nextTimestamp);

        if (samplingTime == nextTimestamp) {
            return nextIndex;
        }
        else {
            return nextIndex - 1;
        }

    }

    private boolean isBeforeLastReadIndex(long samplingTime) {
        if (samplingTime < timestamps[lastReadIndex]) {
            return true;
        }
        else {
            return false;
        }
    }

    private void rewindIndex() {
        lastReadIndex = 0;
    }

    private boolean isBehindLastReadIndex(long samplingTime) {
        if (samplingTime > timestamps[lastReadIndex]) {
            return true;
        }
        else {
            return false;
        }
    }

    private int handleOutsideTimeperiodEarly(long samplingTime) throws CsvException {
        if (isInitialised) {
            throw new TimeTravelException(
                    "Illogical time jump for sampling time. Driver can't find corresponding value in csv file.");
        }
        else {
            throw new NoValueReceivedYetException("Sampling time before first timestamp of csv file.");
        }
    }

    private boolean isWithinTimeperiod(long samplingTime) {
        if (samplingTime >= firstTimestamp && samplingTime <= lastTimestamp) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean isBeforeFirstTimestamp(long samplingTime) {

        if (samplingTime < firstTimestamp) {
            return true;
        }
        else {
            return false;
        }
    }

}
