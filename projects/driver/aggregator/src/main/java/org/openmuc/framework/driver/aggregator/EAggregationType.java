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

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enum which defines the aggregation types
 * 
 */
public enum EAggregationType {

	AVG, //
	LAST, //
	DIFF, //
	PULSES_ENERGY; //

	private final static Logger logger = LoggerFactory.getLogger(EAggregationType.class);

	/**
	 * Parses the name of the aggregation type to the corresponding aggregation type object
	 * 
	 * @param enumAsString
	 * @return aggregation type object on success, otherwise null
	 */
	public static EAggregationType getEnumfromString(String enumAsString) {
		EAggregationType returnValue = null;
		if (enumAsString != null) {
			for (EAggregationType value : EAggregationType.values()) {
				if (enumAsString.equalsIgnoreCase(value.toString())) {
					returnValue = EAggregationType.valueOf(enumAsString.toUpperCase(Locale.ENGLISH));
					break;
				}
			}
		}
		if (returnValue == null) {
			logger.error(enumAsString + " is not supported. Use one of the following values: " + getSupportedValues());
		}
		return returnValue;
	}

	/**
	 * @return all supported values as a comma separated string.
	 */
	public static String getSupportedValues() {

		StringBuilder supported = new StringBuilder();
		for (EAggregationType value : EAggregationType.values()) {
			supported.append(value.toString());
			supported.append(", ");
		}
		return supported.toString();
	}

	/**
	 * Checks if the string matches with one of the enum values.
	 * 
	 * @param enumAsString
	 * @return true on success, otherwise false
	 */
	public static boolean isValidValue(String enumAsString) {

		boolean returnValue = false;
		for (EAggregationType type : EAggregationType.values()) {
			if (type.toString().equalsIgnoreCase(enumAsString)) {
				returnValue = true;
				break;
			}
		}
		return returnValue;
	}

}
