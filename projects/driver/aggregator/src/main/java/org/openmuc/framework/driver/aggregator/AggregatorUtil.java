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
package org.openmuc.framework.driver.aggregator;

import java.util.List;

import org.openmuc.framework.data.Record;

/**
 * Collection of methods
 */
public class AggregatorUtil {

    // /**
    // * Performs some tests on the record and returns its value as double on success <br>
    // * - tests if record != null <br>
    // * - tests if flag is valid <br>
    // * - tests if value != null <br>
    // *
    // * @param record
    // * @return the value as double
    // * @throws SomethingWrongWithRecordException
    // */
    // public static double getDoubleRecordValue(Record record) throws AggregationException {
    //
    // double result;
    //
    // if (record != null) {
    // Flag flag = record.getFlag();
    // if (flag == Flag.VALID) {
    // Value value = record.getValue();
    // if (value != null) {
    // result = value.asDouble();
    // }
    // else {
    // throw new AggregationException("Value is null");
    // }
    // }
    // else {
    // throw new AggregationException("Flag != Valid - " + flag.toString());
    // }
    // }
    // else {
    // throw new AggregationException("Record is null");
    // }
    //
    // return result;
    // }

    /**
     * Returns the value of the last record of the list
     * <p>
     * Can be used for energy aggregation. Smart meter sums the energy automatically therefore the last value contains
     * the aggregated value
     * 
     * @param recordList
     *            List of Records
     * @return the value of the last record of the list
     * @throws AggregationException
     *             on error
     */
    public static Record findLastRecordIn(List<Record> recordList) throws AggregationException {
        if (recordList.isEmpty()) {
            throw new AggregationException("Record list is empty.");
        }

        return recordList.get(recordList.size() - 1);
    }

    /**
     * Don't let anyone instantiate this class.
     */
    private AggregatorUtil() {

    }
}
