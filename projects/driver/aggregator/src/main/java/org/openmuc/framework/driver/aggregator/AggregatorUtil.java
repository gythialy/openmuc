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

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.aggregator.exeptions.SomethingWrongWithRecordException;

/**
 * Collection of methods
 */
public class AggregatorUtil {

	/**
	 * Performs some tests on the record and returns its value as double on success <br>
	 * - tests if record != null <br>
	 * - tests if flag is valid <br>
	 * - tests if value != null <br>
	 * 
	 * @param record
	 * @return the value as double
	 * @throws SomethingWrongWithRecordException
	 */
	public static double getDoubleRecordValue(Record record) throws SomethingWrongWithRecordException {

		double result;

		if (record != null) {
			Flag flag = record.getFlag();
			if (flag == Flag.VALID) {
				Value value = record.getValue();
				if (value != null) {
					result = value.asDouble();
				}
				else {
					throw new SomethingWrongWithRecordException("Value is null");
				}
			}
			else {
				throw new SomethingWrongWithRecordException("Flag != Valid - " + flag.toString());
			}
		}
		else {
			throw new SomethingWrongWithRecordException("Record is null");
		}

		return result;
	}

	public static double getWaitForLatestRecordValue(Channel sourceChannel, Record lastRecord) {

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
