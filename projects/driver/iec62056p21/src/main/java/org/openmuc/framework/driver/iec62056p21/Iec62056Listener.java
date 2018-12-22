package org.openmuc.framework.driver.iec62056p21;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.j62056.DataMessage;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.ModeDListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Iec62056Listener implements ModeDListener {

    private static final Logger logger = LoggerFactory.getLogger(Iec62056Listener.class);

    private RecordsReceivedListener listener;
    private List<ChannelRecordContainer> containers;

    public synchronized void registerOpenMucListener(List<ChannelRecordContainer> containers,
            RecordsReceivedListener listener) throws ConnectionException {
        this.listener = listener;
        this.containers = containers;
    }

    public synchronized void unregisterOpenMucListener() {
        listener = null;
        containers = null;
    }

    @Override
    public void newDataMessage(DataMessage dataMessage) {
        long time = System.currentTimeMillis();
        List<DataSet> dataSets = dataMessage.getDataSets();
        newRecord(dataSets, time);
    }

    private synchronized void newRecord(List<DataSet> dataSets, long time) {
        List<ChannelRecordContainer> newContainers = new ArrayList<>();

        for (ChannelRecordContainer container : containers) {
            for (DataSet dataSet : dataSets) {
                if (dataSet.getAddress().equals(container.getChannelAddress())) {
                    String value = dataSet.getValue();
                    if (value != null) {
                        try {
                            container.setRecord(
                                    new Record(new DoubleValue(Double.parseDouble(dataSet.getValue())), time));
                            newContainers.add(container);
                        } catch (NumberFormatException e) {
                            container.setRecord(new Record(new StringValue(dataSet.getValue()), time));
                        }
                    }
                    break;
                }
            }
        }
        listener.newRecords(newContainers);
    }

    @Override
    public void exceptionWhileListening(Exception e) {
        logger.info("Exception while listening. Message: " + e.getMessage());
    }

}
