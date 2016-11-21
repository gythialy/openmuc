/*
 * Copyright 2011-16 Fraunhofer ISE
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

import org.openmuc.jmbus.MBusSap;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
public class MBusSerialInterface {

    private int connectionCounter = 0;
    private final MBusSap mBusSap;
    private boolean open = true;
    private final String serialPortName;
    private final Map<String, MBusSerialInterface> interfaces;

    public MBusSerialInterface(MBusSap mBusSap, String serialPortName, Map<String, MBusSerialInterface> interfaces) {
        this.mBusSap = mBusSap;
        this.serialPortName = serialPortName;
        this.interfaces = interfaces;
        interfaces.put(serialPortName, this);
    }

    public MBusSap getMBusSap() {
        return mBusSap;
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
            mBusSap.close();
            open = false;
            interfaces.remove(serialPortName);
        }
    }

    public String getInterfaceAddress() {
        return serialPortName;
    }

}
