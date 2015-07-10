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
package org.openmuc.framework.driver.s7plc;

public class S7PlcDevice {

	private Integer port;

	public S7PlcDevice(String addr) {
		setSlaveAddress(addr);
	}

	/**
	 * 
	 * @param deviceAddress
	 *            e.g. localhost:1502
	 */
	private void setSlaveAddress(String deviceAddress) {
		String[] address = deviceAddress.toLowerCase().split(":");

		port = 102;

		if (address.length == 1) {
		}
		else if (address.length == 2) {
			port = Integer.parseInt(address[1]);
		}
		else {
			throw new RuntimeException("Invalid device address: '" + deviceAddress
					+ "'! Use following format: [ip:port] like localhost:1502 or 127.0.0.1:1502");
		}
	}
}
