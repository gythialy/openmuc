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
package org.openmuc.framework.driver.snmp.test;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class SnmpChannelRecordContainer implements ChannelRecordContainer {

    private Record snmpRecord;
    private SnmpChannel snmpChannel;

    SnmpChannelRecordContainer() {
    }

    SnmpChannelRecordContainer(SnmpChannel channel) {
        snmpChannel = channel;
    }

    SnmpChannelRecordContainer(Record record, SnmpChannel channel) {
        snmpChannel = channel;
        snmpRecord = record;
    }

    @Override
    public Record getRecord() {
        return snmpRecord;
    }

    @Override
    public Channel getChannel() {
        return snmpChannel;
    }

    @Override
    public String getChannelAddress() {
        return snmpChannel.getChannelAddress();
    }

    @Override
    public Object getChannelHandle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setChannelHandle(Object handle) {
        snmpChannel = (SnmpChannel) handle;
    }

    @Override
    public void setRecord(Record record) {
        snmpRecord = new Record(record.getValue(), record.getTimestamp(), record.getFlag());
    }

    @Override
    public ChannelRecordContainer copy() {
        SnmpChannelRecordContainer clone = new SnmpChannelRecordContainer();
        clone.setChannelHandle(snmpChannel);
        clone.setRecord(snmpRecord);

        return clone;
    }

}
