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
package org.openmuc.framework.driver.csv;

public enum ESamplingMode {

    /**
     * Csv file must contain a column with the name <i>unixtimestamp</i>, values must be in milliseconds. During
     * sampling the driver searches the closest unixtimestamp which is &gt;= the sampling timestamp. Therefore, the
     * driver keeps returning the same value x for sampling timestamps until the next unixtimestamp of the file is
     * reached.
     */
    UNIXTIMESTAMP,

    /**
     * Csv file must contain a column with the name <i>hhmmss</i>, values must be in the format: hhmmss.
     */
    HHMMSS,

    /**
     * Starts sampling from the first line of the csv file. Timestamps are ignored and each sampling reads the next line
     */
    LINE
}
