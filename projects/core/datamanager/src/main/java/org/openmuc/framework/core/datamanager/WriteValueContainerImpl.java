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
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;

public final class WriteValueContainerImpl implements WriteValueContainer, ChannelValueContainer {

    private final ChannelImpl channel;
    private Value value = null;
    private Flag flag = Flag.DRIVER_ERROR_UNSPECIFIED;
    private Object channelHandle;
    private final String channelAddress;

    public WriteValueContainerImpl(ChannelImpl channel) {
        this.channel = channel;
        this.channelAddress = channel.config.getChannelAddress();
        this.channelHandle = channel.handle;
    }

    @Override
    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public Flag getFlag() {
        return flag;
    }

    @Override
    public ChannelImpl getChannel() {
        return channel;
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
    public void setFlag(Flag flag) {
        this.flag = flag;
    }

}
