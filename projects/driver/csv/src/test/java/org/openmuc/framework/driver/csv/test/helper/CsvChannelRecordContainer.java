package org.openmuc.framework.driver.csv.test.helper;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class CsvChannelRecordContainer implements ChannelRecordContainer {

    private Record record;
    private String channelAddress;

    public CsvChannelRecordContainer(String channelAddress) {
        this.channelAddress = channelAddress;
    }

    @Override
    public Record getRecord() {
        return record;
    }

    @Override
    public Channel getChannel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getChannelAddress() {
        return this.channelAddress;
    }

    @Override
    public Object getChannelHandle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setChannelHandle(Object handle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRecord(Record record) {
        this.record = record;
    }

    @Override
    public ChannelRecordContainer copy() {
        // TODO Auto-generated method stub
        return null;
    }

}
