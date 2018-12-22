package org.openmuc.framework.driver.iec60870;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.iec60870.settings.ChannelAddress;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.InformationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class Iec60870ReadListener implements ConnectionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(Iec60870ReadListener.class);
    private final HashMap<String, ChannelAddress> channelAddressMap = new HashMap<>();
    private final HashMap<String, Record> recordMap = new HashMap<>();
    private List<ChannelRecordContainer> containers;
    private long timeout;
    private IOException ioException = null;
    private boolean isReadyReading = false;

    synchronized void setContainer(List<ChannelRecordContainer> containers) {

        this.containers = containers;
        Iterator<ChannelRecordContainer> containerIterator = containers.iterator();

        while (containerIterator.hasNext()) {
            ChannelRecordContainer channelRecordContainer = containerIterator.next();
            try {
                ChannelAddress channelAddress = new ChannelAddress(channelRecordContainer.getChannelAddress());
                channelAddressMap.put(channelRecordContainer.getChannel().getId(), channelAddress);
            } catch (ArgumentSyntaxException e) {
                logger.error(
                        "ChannelId: " + channelRecordContainer.getChannel().getId() + "; Message: " + e.getMessage());
            }
        }
    }

    void setReadTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public synchronized void newASdu(ASdu aSdu) {
        logger.debug("Got new ASdu");
        if (logger.isTraceEnabled()) {
            logger.trace(aSdu.toString());
        }
        long timestamp = System.currentTimeMillis();

        if (!aSdu.isTestFrame()) {

            Set<String> keySet = channelAddressMap.keySet();
            Iterator<String> iterator = keySet.iterator();

            while (iterator.hasNext()) {
                String channelId = iterator.next();
                ChannelAddress channelAddress = channelAddressMap.get(channelId);

                if (aSdu.getCommonAddress() == channelAddress.commonAddress()
                        && aSdu.getTypeIdentification().getId() == channelAddress.typeId()) {
                    processRecords(aSdu, timestamp, channelId, channelAddress);
                }
            }
            isReadyReading = true;
        }
    }

    @Override
    public void connectionClosed(IOException e) {
        logger.info("Connection was closed by server.");
        ioException = e;
    }

    void read() throws IOException {
        long sleepTime = 100;
        long time = 0;

        while (ioException == null && time < timeout && !isReadyReading) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.error("", e);
            }
            time += sleepTime;
        }

        if (ioException != null) {
            throw ioException;
        }

        for (ChannelRecordContainer channelRecordContainer : containers) {
            String channelId = channelRecordContainer.getChannel().getId();
            Record record = recordMap.get(channelId);
            if (record == null || record.getFlag() != Flag.VALID) {
                channelRecordContainer.setRecord(new Record(Flag.DRIVER_ERROR_TIMEOUT));
            } else {
                channelRecordContainer.setRecord(record);
            }
        }
        isReadyReading = false;
    }

    private void processRecords(ASdu aSdu, long timestamp, String channelId, ChannelAddress channelAddress) {
        for (InformationObject informationObject : aSdu.getInformationObjects()) {
            if (informationObject.getInformationObjectAddress() == channelAddress.ioa()) {
                Record record = Iec60870DataHandling.handleInformationObject(aSdu, timestamp, channelAddress,
                        informationObject);
                recordMap.put(channelId, record);
            }
        }
    }

}
