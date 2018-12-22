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
package org.openmuc.framework.driver.knx;

import org.openmuc.framework.driver.knx.value.KnxValue;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.exception.KNXException;

public class KnxGroupDP extends CommandDP {

    private final KnxValue value;

    public KnxGroupDP(GroupAddress main, String name, String dptID) throws KNXException {
        super(main, name, Integer.parseInt(dptID.split("\\.")[0]), dptID);
        value = KnxValue.createKnxValue(dptID);
    }

    public KnxValue getKnxValue() {
        return value;
    }

}
