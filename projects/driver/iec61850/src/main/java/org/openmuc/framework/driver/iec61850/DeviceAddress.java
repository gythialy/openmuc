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

package org.openmuc.framework.driver.iec61850;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.spi.ConnectionException;

public class DeviceAddress {
    private InetAddress address;
    private int remotePort = 102;

    public DeviceAddress(String deviceAddress) throws ArgumentSyntaxException, ConnectionException {
        String[] deviceAddresses = deviceAddress.split(":");

        if (deviceAddresses.length > 2) {
            throw new ArgumentSyntaxException("Invalid device address syntax.");
        }

        String remoteHost = deviceAddresses[0];
        try {
            address = InetAddress.getByName(remoteHost);
        } catch (UnknownHostException e) {
            throw new ConnectionException("Unknown host: " + remoteHost, e);
        }

        if (deviceAddresses.length == 2) {
            try {
                remotePort = Integer.parseInt(deviceAddresses[1]);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("The specified port is not an integer");
            }
        }
    }

    public InetAddress getAdress() {
        return address;
    }

    public int getRemotePort() {
        return remotePort;
    }

}
