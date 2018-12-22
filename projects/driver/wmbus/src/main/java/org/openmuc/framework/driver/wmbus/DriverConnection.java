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
package org.openmuc.framework.driver.wmbus;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusConnection;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
public class DriverConnection implements Connection {
    private final SecondaryAddress secondaryAddress;
    private final WMBusSerialInterface serialInterface;
    private final WMBusConnection con;
    private List<ChannelRecordContainer> containersToListenFor = new ArrayList<>();

    public DriverConnection(WMBusConnection con, SecondaryAddress secondaryAddress, String keyString,
            WMBusSerialInterface serialInterface) throws ArgumentSyntaxException {
        this.con = con;
        this.serialInterface = serialInterface;
        this.secondaryAddress = secondaryAddress;

        if (keyString != null) {

            byte[] keyAsBytes;
            try {
                keyAsBytes = DatatypeConverter.parseHexBinary(keyString);
            } catch (IllegalArgumentException e) {
                serialInterface.connectionClosedIndication(secondaryAddress);
                throw new ArgumentSyntaxException("The key could not be converted to a byte array.");
            }
            this.con.addKey(secondaryAddress, keyAsBytes);
        }
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        con.removeKey(secondaryAddress);

        synchronized (serialInterface) {
            serialInterface.connectionClosedIndication(secondaryAddress);
        }

    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        containersToListenFor = containers;
        serialInterface.listener = listener;
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    public List<ChannelRecordContainer> getContainersToListenFor() {
        return containersToListenFor;
    }

}
