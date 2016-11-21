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
package org.openmuc.framework.driver.iec60870;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.driver.iec60870.settings.DeviceAddress;
import org.openmuc.framework.driver.iec60870.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Iec60870Connection implements Connection {

    private org.openmuc.j60870.Connection clientConnection;

    private final static Logger logger = LoggerFactory.getLogger(Iec60870Connection.class);

    private final DeviceAddress deviceAddress;
    private final DeviceSettings deviceSettings;

    public Iec60870Connection(DeviceAddress deviceAddress, DeviceSettings deviceSettings) throws ConnectionException {
        this.deviceAddress = deviceAddress;
        this.deviceSettings = deviceSettings;

        ClientConnectionBuilder clientConnectionBuilder = new ClientConnectionBuilder(this.deviceAddress.hostAddress());

        try {
            setupClientSap(clientConnectionBuilder, deviceSettings);
            logger.debug("Try to connect to: " + this.deviceAddress.hostAddress().getHostAddress() + ':'
                    + this.deviceAddress.port());
            clientConnection = clientConnectionBuilder.connect();
            logger.info("Driver-IEC60870: successful connected to " + this.deviceAddress.hostAddress().getHostAddress()
                    + ":" + this.deviceAddress.port());
        } catch (IOException e) {
            throw new ConnectionException(MessageFormat.format("Was not able to connect to {0}:{1}. {2}",
                    this.deviceAddress.hostAddress().getHostName(), this.deviceAddress.port(), e.getMessage()));
        }
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        try {
            clientConnection.startDataTransfer(new Iec60870Listener(), deviceSettings.stardtConTimeout());
        } catch (IOException | TimeoutException e) {
            throw new ConnectionException(e);
        }
        Iec60870Listener iec60870Listener = new Iec60870Listener();
        iec60870Listener.registerOpenMucListener(containers, listener);
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        if (clientConnection != null) {
            clientConnection.close();
        }
        logger.info("Disconnected IEC 60870 driver.");
    }

    private void setupClientSap(ClientConnectionBuilder clientSap, DeviceSettings deviceSettings) {
        clientSap.setPort(this.deviceAddress.port());

        if (deviceSettings.commonAddressFieldLength() > 0) {
            clientSap.setCommonAddressFieldLength(deviceSettings.commonAddressFieldLength());
        }
        else if (deviceSettings.cotFieldLength() > 0) {
            clientSap.setCotFieldLength(deviceSettings.cotFieldLength());
        }
        else if (deviceSettings.ioaFieldLength() > 0) {
            clientSap.setIoaFieldLength(deviceSettings.ioaFieldLength());
        }
        else if (deviceSettings.maxIdleTime() > 0) {
            clientSap.setMaxIdleTime(deviceSettings.maxIdleTime());
        }
        else if (deviceSettings.maxTimeNoAckReceived() > 0) {
            clientSap.setMaxTimeNoAckReceived(deviceSettings.maxTimeNoAckReceived());
        }
        else if (deviceSettings.maxTimeNoAckSent() > 0) {
            clientSap.setMaxTimeNoAckSent(deviceSettings.maxTimeNoAckSent());
        }
        else if (deviceSettings.maxUnconfirmedIPdusReceived() > 0) {
            clientSap.setMaxUnconfirmedIPdusReceived(deviceSettings.maxUnconfirmedIPdusReceived());
        }
    }

}
