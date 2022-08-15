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
package org.openmuc.framework.driver.csv.exceptions;

/**
 * Exception for illogical time jumps. <br>
 * <br>
 * Scenario 1: the csv file contains ONLY values within a time period form 100000 o'clock till 110000 o'clock. The
 * driver has successfully sampled a value within this period. Now the time has jumped and the next sampling time is
 * 090000 o'clock (so before the first entry of the csv file). The driver therefore can't find a value for this sampling
 * time.
 * 
 */
public class TimeTravelException extends CsvException {

    private static final long serialVersionUID = 6718058510080266888L;

    public TimeTravelException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
