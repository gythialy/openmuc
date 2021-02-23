/*
 * Copyright 2011-2021 Fraunhofer ISE
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

public class AggregatorConstants {

    public static final int MAX_ADDRESS_PARTS_LENGTH = 3;
    public static final int MIN_ADDRESS_PARTS_LENGTH = 2;

    public static final String ADDRESS_SEPARATOR = ":";
    public static final String TYPE_PARAM_SEPARATOR = ",";

    public static final int ADDRESS_SOURCE_CHANNEL_ID_INDEX = 0;
    public static final int ADDRESS_AGGREGATION_TYPE_INDEX = 1;
    public static final int ADDRESS_QUALITY_INDEX = 2;
    public static final double DEFAULT_QUALITY = 0.0;

    public static final String AGGREGATION_TYPE_LAST = "LAST";
    public static final String AGGREGATION_TYPE_AVG = "AVG";
    public static final String AGGREGATION_TYPE_DIFF = "DIFF";
    public static final String AGGREGATION_TYPE_PULS_ENERGY = "PULS_ENERGY";

    /**
     * Don't let anyone instantiate this class.
     */
    private AggregatorConstants() {
    }
}
