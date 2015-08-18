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

import org.openmuc.framework.driver.aggregator.exeptions.WrongChannelAddressFormatException;

/**
 * Parses the channel address and checks the address syntax
 */
public class AggregatorChannelAddress {

	private static final int MAX_ADDRESS_PARTS_LENGTH = 4;
	private static final int MIN_ADDRESS_PARTS_LENGTH = 2;
	private static final String ADDRESS_SEPARATOR = ":";

	private static final int ADDRESS_SOURCECHANNELID_INDEX = 0;
	private static final int ADDRESS_AGGREGATIONTYPE_INDEX = 1;
	private static final int ADDRESS_OPTIONAL_SETIING_INDEX_1 = 2;
	private static final int ADDRESS_OPTIONAL_SETIING_INDEX_2 = 3;

	private EAggregationType aggregationType;
	private String sourceChannelId;
	private double optionalSetting1 = 0.;
	private double optionalSetting2 = 0.;

	public AggregatorChannelAddress(String address) throws WrongChannelAddressFormatException {

		String[] addressParts = address.split(ADDRESS_SEPARATOR);
		int addressPartsLength = addressParts.length;

		if (addressPartsLength <= MAX_ADDRESS_PARTS_LENGTH && addressPartsLength >= MIN_ADDRESS_PARTS_LENGTH) {
			sourceChannelId = addressParts[ADDRESS_SOURCECHANNELID_INDEX];
			aggregationType = parseAggregationType(addressParts[ADDRESS_AGGREGATIONTYPE_INDEX]);

			if (addressPartsLength >= ADDRESS_OPTIONAL_SETIING_INDEX_1 + 1) {
				optionalSetting1 = parseOptionalSetting(addressParts[ADDRESS_OPTIONAL_SETIING_INDEX_1]);
			}
			if (addressPartsLength == ADDRESS_OPTIONAL_SETIING_INDEX_2 + 1) {
				optionalSetting2 = parseOptionalSetting(addressParts[ADDRESS_OPTIONAL_SETIING_INDEX_2]);
			}
		}
		else {
			throw new WrongChannelAddressFormatException("");
		}

	}

	private EAggregationType parseAggregationType(String typeAsString) throws WrongChannelAddressFormatException {

		EAggregationType type = EAggregationType.getEnumfromString(typeAsString);
		if (type == null) {
			throw new WrongChannelAddressFormatException("Aggregation type: " + typeAsString + " not supported.");
		}
		return type;
	}

	private Double parseOptionalSetting(String optionalSettingAsString) throws WrongChannelAddressFormatException {

		Double optionalSetting = Double.parseDouble(optionalSettingAsString);
		if (optionalSetting == null) {
			throw new WrongChannelAddressFormatException("Optional setting: " + optionalSettingAsString
					+ " not supported. Is not a long number");
		}
		return optionalSetting;
	}

	public EAggregationType getAggregationType() {

		return aggregationType;
	}

	public String getSourceChannelId() {

		return sourceChannelId;
	}

	public double getOptionalSeting1() {

		return optionalSetting1;
	}

	public double getOptionalSeting2() {

		return optionalSetting2;
	}

}
