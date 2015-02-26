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
package org.openmuc.framework.datalogger.ascii;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class IESDataFormatUtils {

    public static final String SEPARATOR = ";\t";
    public static final String COMMENT = "#";
    public static final String ERROR = "err";
    public static final String COL_NUM = "col_no";
    public static final String DATATYPE_NAME = "value_type";
    public static final String COMMENT_NAME = "comment";
    public static final String VALUETYPE_ENDSIGN = ". ";
    public static final String VALUETYPE_SIZE_SEPARATOR = ",";
    public static final String HEXADECIMAL = "0x";

    public static final int SIZE_LEADING_SIGN = 1;
    public static final int VALUE_SIZE_DOUBLE = 8 + SIZE_LEADING_SIGN;
    public static final int VALUE_SIZE_INTEGER = 11 + SIZE_LEADING_SIGN;
    public static final int VALUE_SIZE_LONG = 20 + SIZE_LEADING_SIGN;
    public static final int VALUE_SIZE_SHORT = 6 + SIZE_LEADING_SIGN;
    public static final int VALUE_SIZE_MINIMAL = 5 + SIZE_LEADING_SIGN;

    public static final int NUM_OF_TIME_TYPES_IN_HEADER = 3;

    /**
     * Convert a double value into a string with the maximal allowed length of maxlength.
     *
     * @param value
     * @param maxLength The maximal allowed length with all signs.
     * @return a double as string with max length.
     * @throws WrongScalingException
     */
    public static String convertDoubleToStringWithMaxLength(double value, int maxLength) throws WrongScalingException {
        String ret;
        String format;

        long lValue = (long) (value * 10000.0);
        if (lValue >= 0) {
            format = "+###0.000";
            if (lValue >> 63 != 0) {
                lValue *= -1l;
            }
        } else {
            format = "###0.000";
        }
        value = lValue / 10000.0;

        DecimalFormat df = new DecimalFormat(format, new DecimalFormatSymbols(Locale.ENGLISH));
        ret = df.format(value);

        if (ret.length() > maxLength) {
            throw new WrongScalingException("Double too large for convertion into " + maxLength + " max length! Try to scale value.");
        }
        return ret;
    }
}
