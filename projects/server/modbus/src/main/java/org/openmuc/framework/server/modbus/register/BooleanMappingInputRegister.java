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

import org.openmuc.framework.dataaccess.Channel;

public class BooleanMappingInputRegister extends MappingInputRegister {

    public BooleanMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        if (channel.getLatestRecord().getValue().asBoolean() == true) {
            byte[] bytes = { (byte) 0x01 };
            return bytes;
        }
        else if (channel.getLatestRecord().getValue().asBoolean() == false) {
            byte[] bytes = { (byte) 0x00 };
            return bytes;
        }
        return null;
    }

}
