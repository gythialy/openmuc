/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.driver.canopen;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.jcanopen.datatypes.NumericDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frederic Robra
 */
public class SDOObject {

    private static Logger logger = LoggerFactory.getLogger(SDOObject.class);

    private final int nodeId;
    private final int index;
    private final short subIndex;
    private NumericDataType numericDataType = null;

    public SDOObject(String channelAddressSyntax) throws ArgumentSyntaxException {
        String[] address = channelAddressSyntax.split(":");
        if (address == null || address.length < 4 || !address[0].equals("SDO")) {
            throw new ArgumentSyntaxException("channel is not a sdo");
        }

        nodeId = Transforms.parseHexOrDecValue(address[1]);
        index = Transforms.parseHexOrDecValue(address[2]);
        subIndex = (short) Transforms.parseHexOrDecValue(address[3]);
        if (address.length > 4) {
            logger.trace("parsing data type: {}", address[4]);
            numericDataType = Transforms.parseDataType(address[4]);
        }
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getIndex() {
        return index;
    }

    public short getSubIndex() {
        return subIndex;
    }

    public NumericDataType getNumericDataType() {
        return numericDataType;
    }

}
