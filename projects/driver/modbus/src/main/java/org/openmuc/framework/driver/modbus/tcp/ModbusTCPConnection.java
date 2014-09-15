/*
 * Copyright 2011-14 Fraunhofer ISE
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

import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.openmuc.framework.driver.modbus.ModbusConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * TODO
 *
 * @author Marco Mittelsdorf
 */
public class ModbusTCPConnection extends ModbusConnection {

    private final static Logger logger = LoggerFactory.getLogger(ModbusTCPConnection.class);

    private InetAddress slaveAddress;
    private TCPMasterConnection connection;
    private ModbusTCPTransaction transaction;

    public ModbusTCPConnection(String addr) {

        try {
            slaveAddress = InetAddress.getByName(addr);
            connection = new TCPMasterConnection(slaveAddress);

        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public ModbusTCPConnection(String addr, int port) {
        this(addr);
        connection.setPort(port);
    }

    @Override
    public void connect() throws Exception {

        logger.debug("connect for Modbus TCP called");

        if (connection != null && !connection.isConnected()) {
            connection.connect();
            transaction = new ModbusTCPTransaction(connection);

            setTransaction(transaction);

            if (!connection.isConnected()) {
                throw new Exception("unable to connect");
            }
        }
    }

    @Override
    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.close();
            transaction = null;
        }
    }

    public void setTimeout(int timeout) {
        connection.setTimeout(timeout);
    }

}
