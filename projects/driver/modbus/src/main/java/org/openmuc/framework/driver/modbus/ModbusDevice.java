/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.framework.driver.modbus;

import net.wimpi.modbus.ModbusException;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;

import java.util.List;

public abstract class ModbusDevice {

    public abstract void connect() throws ConnectionException;

    public abstract void disconnect();

    public abstract Value readChannel(ModbusChannel channel, int timeout) throws ModbusException;

    public abstract void readChannelGroup(ModbusChannelGroup channelGroup,
                                          List<ChannelRecordContainer> containers,
                                          int timeout) throws ModbusException;

    public abstract void writeChannel(ModbusChannel channel, Value value)
            throws ModbusException, RuntimeException;

    public abstract void setChannelsWithErrorFlag(List<ChannelRecordContainer> containers);
}
