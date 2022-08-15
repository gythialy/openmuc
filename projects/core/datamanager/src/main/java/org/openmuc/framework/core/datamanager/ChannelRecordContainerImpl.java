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

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public final class ChannelRecordContainerImpl implements ChannelRecordContainer {

    private static final Record defaulRecord = new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);

    private final ChannelImpl channel;
    private final String channelAddress;
    private Record record;
    private Object channelHandle;

    public ChannelRecordContainerImpl(ChannelImpl channel) {
        this(channel, defaulRecord);
    }

    private ChannelRecordContainerImpl(ChannelImpl channel, Record record) {
        this.channel = channel;
        this.channelAddress = channel.config.getChannelAddress();
        this.channelHandle = channel.handle;
        this.record = record;
    }

    @Override
    public String getChannelAddress() {
        return channelAddress;
    }

    @Override
    public Object getChannelHandle() {
        return channelHandle;
    }

    @Override
    public void setChannelHandle(Object handle) {
        channelHandle = handle;
    }

    @Override
    public ChannelRecordContainer copy() {
        Record copiedRecord = new Record(record.getValue(), record.getTimestamp(), record.getFlag());

        return new ChannelRecordContainerImpl(channel, copiedRecord);
    }

    @Override
    public ChannelImpl getChannel() {
        return channel;
    }

    @Override
    public Record getRecord() {
        return record;
    }

    @Override
    public void setRecord(Record record) {
        this.record = record;
    }
}
