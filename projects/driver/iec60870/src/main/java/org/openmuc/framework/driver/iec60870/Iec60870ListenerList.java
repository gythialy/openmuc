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
package org.openmuc.framework.driver.iec60870;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ConnectionEventListener;

class Iec60870ListenerList implements ConnectionEventListener {
    List<ConnectionEventListener> connectionEventListeners = new ArrayList<>();

    Iec60870ListenerList() {
    }

    void addListener(ConnectionEventListener connectionEventListener) {
        connectionEventListeners.add(connectionEventListener);
    }

    void removeAllListener() {
        connectionEventListeners.clear();
    }

    @Override
    public void newASdu(ASdu aSdu) {
        for (ConnectionEventListener connectionEventListener : connectionEventListeners) {
            connectionEventListener.newASdu(aSdu);
        }
    }

    @Override
    public void connectionClosed(IOException e) {
        for (ConnectionEventListener connectionEventListener : connectionEventListeners) {
            connectionEventListener.connectionClosed(e);
        }
    }

}
