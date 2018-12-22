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
package org.openmuc.framework.server.modbus.register;

import java.nio.ByteBuffer;

import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.server.modbus.ModbusServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public abstract class MappingInputRegister implements InputRegister {
    private static Logger logger = LoggerFactory.getLogger(ModbusServer.class);
    boolean useUnscaledValues;
    protected Channel channel;
    protected int highByte;
    protected int lowByte;

    public MappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        this.channel = channel;
        this.highByte = byteHigh;
        this.lowByte = byteLow;

        try {
            String scalingProperty = System.getProperty("org.openmuc.framework.server.modbus.useUnscaledValues");
            useUnscaledValues = Boolean.parseBoolean(scalingProperty);
        } catch (Exception e) {
            /* will stick to default setting. */
        }
    }

    @Override
    public int getValue() {
        int toReturn = ByteBuffer.wrap(toBytes())
                .getShort(); /*
                              * toBytes always only contains two bytes. So cast from short.
                              */
        return toReturn;
    }

    @Override
    public int toUnsignedShort() {
        short shortVal = ByteBuffer.wrap(toBytes()).getShort();
        int toReturn = shortVal & 0xFFFF;
        return toReturn;
    }

    @Override
    public short toShort() {
        short toReturn = ByteBuffer.wrap(toBytes()).getShort();
        return toReturn;
    }

    @Override
    public abstract byte[] toBytes();
}
