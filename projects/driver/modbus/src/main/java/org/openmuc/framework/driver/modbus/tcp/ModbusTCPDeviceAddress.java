/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.driver.modbus.tcp;

import com.ghgande.j2mod.modbus.Modbus;

public class ModbusTCPDeviceAddress {

    private String ip;
    private int port;

    public ModbusTCPDeviceAddress(String deviceAddress) {
        String[] address = deviceAddress.toLowerCase().split(":");

        if (address.length == 1) {
            ip = address[0];
            port = Modbus.DEFAULT_PORT;
        }
        else if (address.length == 2) {
            ip = address[0];
            port = Integer.parseInt(address[1]);
        }
        else {
            throw new RuntimeException("Invalid device address: '" + deviceAddress
                    + "'! Use following format: [ip:port] like localhost:1502 or 127.0.0.1:1502");
        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
