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
package org.openmuc.framework.driver.iec60870;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.iec60870.settings.ChannelAddress;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.InformationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Iec60870Listener implements ConnectionEventListener {

    private RecordsReceivedListener listener;
    private List<ChannelRecordContainer> containers;
    private List<ChannelAddress> channelAddresses = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(Iec60870Listener.class);
    private String driverId;
    private Connection connection;

    public synchronized void registerOpenMucListener(List<ChannelRecordContainer> containers,
            RecordsReceivedListener listener, String driverId, Connection connection) throws ConnectionException {

        this.containers = containers;
        this.listener = listener;
        this.driverId = driverId;
        this.connection = connection;
        Iterator<ChannelRecordContainer> containerIterator = containers.iterator();

        while (containerIterator.hasNext()) {
            ChannelRecordContainer channelRecordContainer = containerIterator.next();
            try {
                ChannelAddress channelAddress = new ChannelAddress(channelRecordContainer.getChannelAddress());
                channelAddresses.add(channelAddress);
            } catch (ArgumentSyntaxException e) {
                logger.error(
                        "ChannelId: " + channelRecordContainer.getChannel().getId() + "; Message: " + e.getMessage());
            }
        }
    }

    public synchronized void unregisterOpenMucListener() {
        containers = null;
        listener = null;
        channelAddresses = null;
    }

    @Override
    public synchronized void newASdu(ASdu aSdu) {
        logger.debug("Got new ASdu");
        if (logger.isTraceEnabled()) {
            logger.trace(aSdu.toString());
        }
        if (listener != null) {
            long timestamp = System.currentTimeMillis();

            if (!aSdu.isTestFrame()) {
                Iterator<ChannelAddress> channelAddressIterator = channelAddresses.iterator();
                int i = 0;

                while (channelAddressIterator.hasNext()) {
                    ChannelAddress channelAddress = channelAddressIterator.next();

                    if (aSdu.getCommonAddress() == channelAddress.commonAddress()
                            && aSdu.getTypeIdentification().getId() == channelAddress.typeId()) {
                        processRecords(aSdu, timestamp, i, channelAddress);
                    }
                    ++i;
                }
            }
        }
        else {
            logger.warn("Listener object is null.");
        }
    }

    private void processRecords(ASdu aSdu, long timestamp, int i, ChannelAddress channelAddress) {
        for (InformationObject informationObject : aSdu.getInformationObjects()) {
            if (informationObject.getInformationObjectAddress() == channelAddress.ioa()) {
                Record record = Iec60870DataHandling.handleInformationObject(aSdu, timestamp, channelAddress,
                        informationObject);
                newRecords(i, record);
            }
        }
    }

    private void newRecords(int i, Record record) {
        if (logger.isTraceEnabled()) {
            logger.trace("Set new Record: " + record.toString());
        }
        listener.newRecords(creatNewChannelRecordContainer(containers.get(i), record));
    }

    private List<ChannelRecordContainer> creatNewChannelRecordContainer(ChannelRecordContainer container,
            Record record) {
        List<ChannelRecordContainer> channelRecordContainerList = new ArrayList<>();
        container.setRecord(record);
        channelRecordContainerList.add(container);
        return channelRecordContainerList;
    }

    @Override
    public void connectionClosed(IOException e) {
        logger.info("Connection was closed by server.");
        listener.connectionInterrupted(driverId, connection);
    }

}
