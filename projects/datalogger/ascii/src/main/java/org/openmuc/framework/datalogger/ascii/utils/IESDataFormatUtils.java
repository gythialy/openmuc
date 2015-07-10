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
package org.openmuc.framework.datalogger.ascii.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.openmuc.framework.datalogger.ascii.exceptions.WrongScalingException;

public class IESDataFormatUtils {

	// private final static Logger logger = LoggerFactory.getLogger(IESDataFormatUtils.class);

	/**
	 * Convert a double value into a string with the maximal allowed length of maxlength.
	 * 
	 * @param value
	 * @param maxLength
	 *            The maximal allowed length with all signs.
	 * @return a double as string with max length.
	 * @throws WrongScalingException
	 */
	public static String convertDoubleToStringWithMaxLength(double value, int maxLength) throws WrongScalingException {

		String ret;

		String format;
		long lValue = (long) (value * 10000.0);
		value = lValue / 10000.0;

		if (lValue >= 0) {

			if (lValue >> 63 != 0) {
				value *= -1l;
			}
			format = '+' + getFormat(value);
		}
		else {
			format = getFormat(value);
		}

		DecimalFormat df = new DecimalFormat(format, new DecimalFormatSymbols(Locale.ENGLISH));
		ret = df.format(value);

		if (ret.length() > maxLength) {
			throw new WrongScalingException("Double too large for convertion into " + maxLength
					+ " max length! Try to scale value.");
		}
		return ret;
	}

	private static String getFormat(double value) {

		long lValue = (long) value;
		String format;

		if (lValue > 999999 || lValue < -999999) {
			format = "#######0";
		}
		else if (lValue > 99999 || lValue < -99999) {
			format = "#####0.0";
		}
		else if (lValue > 9999 || lValue < -9999) {
			format = "####0.00";
		}
		else {
			format = "###0.000";
		}

		return format;
	}
}
