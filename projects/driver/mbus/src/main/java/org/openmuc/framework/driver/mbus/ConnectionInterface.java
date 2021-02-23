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
package org.openmuc.framework.driver.mbus;

import java.util.Map;

import org.openmuc.jmbus.MBusConnection;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
class ConnectionInterface {

    private int connectionCounter = 0;
    private MBusConnection mBusConnection;
    private boolean open = true;
    private final String connectionName;
    private int delay = 0;
    private Map<String, ConnectionInterface> interfaces;

    public ConnectionInterface(MBusConnection mBusConnection, String serialPortName, int delay,
            Map<String, ConnectionInterface> interfaces) {
        this.connectionName = serialPortName;
        generalConnectionInterface(mBusConnection, interfaces, delay);
    }

    public ConnectionInterface(MBusConnection mBusConnection, String host, int port, int delay,
            Map<String, ConnectionInterface> interfaces) {
        this.connectionName = host + port;
        generalConnectionInterface(mBusConnection, interfaces, delay);
    }

    private void generalConnectionInterface(MBusConnection mBusConnection, Map<String, ConnectionInterface> interfaces,
            int delay) {
        this.mBusConnection = mBusConnection;
        this.interfaces = interfaces;
        this.delay = delay;
        interfaces.put(connectionName, this);
    }

    public MBusConnection getMBusConnection() {
        return mBusConnection;
    }

    public int getDelay() {
        return delay;
    }

    public void increaseConnectionCounter() {
        connectionCounter++;
    }

    public void decreaseConnectionCounter() {
        connectionCounter--;
        if (connectionCounter == 0) {
            close();
        }
    }

    public int getDeviceCounter() {
        return connectionCounter;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        synchronized (interfaces) {
            mBusConnection.close();
            open = false;
            interfaces.remove(connectionName);
        }
    }

    public String getInterfaceAddress() {
        return connectionName;
    }

}
