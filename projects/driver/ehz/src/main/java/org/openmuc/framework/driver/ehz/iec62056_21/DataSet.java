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

package org.openmuc.framework.driver.ehz.iec62056_21;

public class DataSet {
    private String address = null;
    private String value = null;
    private String unit = null;

    public DataSet(String dataSet) {
        int bracket = dataSet.indexOf('(');

        address = dataSet.substring(0, bracket);

        dataSet = dataSet.substring(bracket);

        int separator = dataSet.indexOf('*');

        if (separator == -1) {
            value = dataSet.substring(1, dataSet.length() - 2);
        }
        else {
            value = dataSet.substring(1, separator);
            unit = dataSet.substring(separator + 1, dataSet.length() - 1);
        }
    }

    public String getAddress() {
        return address;
    }

    public double getVal() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

}
