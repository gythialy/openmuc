/*
 * Copyright 2011-2021 Fraunhofer ISE
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
package org.openmuc.framework.driver.knx.value;

import org.openmuc.framework.data.Value;

import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;

public abstract class KnxValue {

    protected DPTXlator dptXlator;

    public static KnxValue createKnxValue(String dptID) throws KNXException {
        int mainNumber = Integer.valueOf(dptID.split("\\.")[0]);

        switch (mainNumber) {
        case 1:
            return new KnxValueBoolean(dptID);
        case 2:
            return new KnxValue1BitControlled(dptID);
        case 3:
            return new KnxValue3BitControlled(dptID);
        case 5:
            return new KnxValue8BitUnsigned(dptID);
        case 7:
            return new KnxValue2ByteUnsigned(dptID);
        case 9:
            return new KnxValue2ByteFloat(dptID);
        case 10:
            return new KnxValueTime(dptID);
        case 11:
            return new KnxValueDate(dptID);
        case 12:
            return new KnxValue4ByteUnsigned(dptID);
        case 13:
            return new KnxValue4ByteSigned(dptID);
        case 14:
            return new KnxValue4ByteFloat(dptID);
        case 16:
            return new KnxValueString(dptID);
        case 19:
            return new KnxValueDateTime(dptID);
        default:
            throw new KNXException("unknown datapoint");
        }

    }

    public String getDPTValue() {
        return dptXlator.getValue();
    }

    public void setDPTValue(String value) throws KNXFormatException {
        dptXlator.setValue(value);
    }

    public void setData(byte[] data) {
        dptXlator.setData(data);
    }

    public abstract Value getOpenMucValue();

    public abstract void setOpenMucValue(Value value) throws KNXFormatException;
}
