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
package org.openmuc.framework.driver.mbus;

import java.util.Map;

import org.openmuc.jmbus.MBusConnection;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
public class SerialInterface {

    private int connectionCounter = 0;
    private final MBusConnection mBusConnection;
    private boolean open = true;
    private final String serialPortName;
    private final Map<String, SerialInterface> interfaces;

    public SerialInterface(MBusConnection mBusConnection, String serialPortName,
            Map<String, SerialInterface> interfaces) {
        this.mBusConnection = mBusConnection;
        this.serialPortName = serialPortName;
        this.interfaces = interfaces;
        interfaces.put(serialPortName, this);
    }

    public MBusConnection getMBusConnection() {
        return mBusConnection;
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
            interfaces.remove(serialPortName);
        }
    }

    public String getInterfaceAddress() {
        return serialPortName;
    }

}
