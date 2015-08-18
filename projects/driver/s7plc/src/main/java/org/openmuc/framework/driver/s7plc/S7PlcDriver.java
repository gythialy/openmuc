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

import java.io.IOException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.libnodave.Connection;
import com.libnodave.Interface;

public final class S7PlcDriver implements DriverService {

	private final static Logger logger = LoggerFactory.getLogger(S7PlcDriver.class);

	private final static DriverInfo info = new DriverInfo("s7plc", // id
			// description
			"Driver for WinAC / s7 PLC.",
			// device address
			"Synopsis: <ip>:<port>",
			// settings
			"N.A.",
			// channel address
			"Synopsis: DB<area_address>.<offset>:<type>\n<type> can be float, unit8, int8, uint16, int16, uint32, int32, double or bit(x)",
			// device scan settings
			"N.A.");

	private final int c = 1;

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();

	}

	@Override
	public org.openmuc.framework.driver.spi.Connection connect(String deviceAddress, String settings)
			throws ArgumentSyntaxException, ConnectionException {

		int index = deviceAddress.indexOf(":");
		if (index == -1) {
			throw new ArgumentSyntaxException();
		}

		Interface ifc;
		try {
			ifc = new Interface("IF" + c, deviceAddress.substring(0, index), Integer.parseInt(deviceAddress
					.substring(index + 1)));
		} catch (NumberFormatException e) {
			throw new ArgumentSyntaxException();
		} catch (IOException e) {
			throw new ConnectionException(e);
		}

		ifc.setTimeout(5000000);

		int mpi = 2;
		int rack = 0;
		int slot = 2;

		// if (url.getParameter("mpi") != null) {
		// mpi = new Integer(url.getParameter("mpi"));
		// System.out.println("mpi: " + mpi);
		// }
		// if (url.getParameter("rack") != null) {
		// rack = new Integer(url.getParameter("rack"));
		// System.out.println("rack: " + rack);
		// }
		// if (url.getParameter("slot") != null) {
		// slot = new Integer(url.getParameter("slot"));
		// System.out.println("slot: " + slot);
		// }

		Connection con = new Connection(ifc, mpi, rack, slot);

		if (con.connectPLC() == true) {
			logger.debug("plcs7: connection established to " + deviceAddress);
			return new S7PlcConnection(con, ifc);
		}
		else {
			throw new ConnectionException();
		}

	}

}
