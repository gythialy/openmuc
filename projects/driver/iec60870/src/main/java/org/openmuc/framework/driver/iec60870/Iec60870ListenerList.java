package org.openmuc.framework.driver.iec60870;

import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ConnectionEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
