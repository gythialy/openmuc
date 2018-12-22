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

package org.openmuc.framework.driver.modbus;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.util.BitVector;

/**
 * Represents a group of channels which is used for a multiple read request
 */
public class ModbusChannelGroup {

    private static final Logger logger = LoggerFactory.getLogger(ModbusChannelGroup.class);

    private static final int INVALID = -1;

    private EPrimaryTable primaryTable;
    private final ArrayList<ModbusChannel> channels;

    /** Start address to read from */
    private int startAddress;

    /** Number of Registers/Coils to be read from startAddress */
    private int count;

    private int unitId;
    private EFunctionCode functionCode;
    private final String samplingGroup;

    public ModbusChannelGroup(String samplingGroup, ArrayList<ModbusChannel> channels) {
        this.samplingGroup = samplingGroup;
        this.channels = channels;
        setPrimaryTable();
        setUnitId();
        setStartAddress();
        setCount();
        setFunctionCode();
    }

    public String getInfo() {
        String info = "SamplingGroup: '" + samplingGroup + "' Channels: ";
        for (ModbusChannel channel : channels) {
            info += channel.getStartAddress() + ":" + channel.getDatatype() + ", ";
        }
        return info;
    }

    private void setFunctionCode() {

        boolean init = false;
        EFunctionCode tempFunctionCode = null;

        for (ModbusChannel channel : channels) {
            if (!init) {
                tempFunctionCode = channel.getFunctionCode();
                init = true;
            }
            else {
                if (!tempFunctionCode.equals(channel.getFunctionCode())) {
                    throw new RuntimeException("FunctionCodes of all channels within the samplingGroup '"
                            + samplingGroup + "' are not equal! Change your openmuc config.");
                }
            }
        }

        functionCode = tempFunctionCode;
    }

    /**
     * Checks if the primary table of all channels of the sampling group is equal and sets the value for the channel
     * group.
     */
    private void setPrimaryTable() {

        boolean init = false;
        EPrimaryTable tempPrimaryTable = null;

        for (ModbusChannel channel : channels) {
            if (!init) {
                tempPrimaryTable = channel.getPrimaryTable();
                init = true;
            }
            else {
                if (!tempPrimaryTable.equals(channel.getPrimaryTable())) {
                    throw new RuntimeException("Primary tables of all channels within the samplingGroup '"
                            + samplingGroup + "' are not equal! Change your openmuc config.");
                }
            }
        }

        primaryTable = tempPrimaryTable;
    }

    private void setUnitId() {
        int idOfFirstChannel = INVALID;
        for (ModbusChannel channel : channels) {
            if (idOfFirstChannel == INVALID) {
                idOfFirstChannel = channel.getUnitId();
            }
            else {
                if (channel.getUnitId() != idOfFirstChannel) {

                    // TODO ???
                    // channel 1 device 1 = unitId 1
                    // channel 1 device 2 = unitId 2
                    // Does openmuc calls the read method for channels of different devices?
                    // If so, then the check for UnitID has to be modified. Only channels of the same device
                    // need to have the same unitId...
                    throw new RuntimeException("UnitIds of all channels within the samplingGroup '" + samplingGroup
                            + "' are not equal! Change your openmuc config.");
                }
            }
        }
        unitId = idOfFirstChannel;
    }

    /**
     * StartAddress is the smallest channel address of the group
     */
    private void setStartAddress() {

        startAddress = INVALID;
        for (ModbusChannel channel : channels) {
            if (startAddress == INVALID) {
                startAddress = channel.getStartAddress();
            }
            else {
                startAddress = Math.min(startAddress, channel.getStartAddress());
            }
        }
    }

    /**
     *
     */
    private void setCount() {

        int maximumAddress = startAddress;

        for (ModbusChannel channel : channels) {
            maximumAddress = Math.max(maximumAddress, channel.getStartAddress() + channel.getCount());
        }

        count = maximumAddress - startAddress;
    }

    public void setChannelValues(InputRegister[] inputRegisters, List<ChannelRecordContainer> containers) {

        for (ModbusChannel channel : channels) {
            // determine start index of the registers which contain the values of the channel
            int registerIndex = channel.getStartAddress() - getStartAddress();
            // create a temporary register array
            InputRegister[] registers = new InputRegister[channel.getCount()];
            // copy relevant registers for the channel
            System.arraycopy(inputRegisters, registerIndex, registers, 0, channel.getCount());

            // now we have a register array which contains the value of the channel
            ChannelRecordContainer container = searchContainer(channel.getChannelAddress(), containers);

            long receiveTime = System.currentTimeMillis();

            Value value = ModbusDriverUtil.getRegistersValue(registers, channel.getDatatype());

            if (logger.isTraceEnabled()) {
                logger.trace("response value channel " + channel.getChannelAddress() + ": " + value.toString());
            }

            container.setRecord(new Record(value, receiveTime));
        }
    }

    public void setChannelValues(BitVector bitVector, List<ChannelRecordContainer> containers) {

        for (ModbusChannel channel : channels) {

            long receiveTime = System.currentTimeMillis();

            // determine start index of the registers which contain the values of the channel
            int index = channel.getStartAddress() - getStartAddress();

            BooleanValue value = new BooleanValue(bitVector.getBit(index));
            ChannelRecordContainer container = searchContainer(channel.getChannelAddress(), containers);
            container.setRecord(new Record(value, receiveTime));
        }
    }

    private ChannelRecordContainer searchContainer(String channelAddress, List<ChannelRecordContainer> containers) {
        for (int i = 0, n = containers.size(); i < n; i++) {
            ChannelRecordContainer container = containers.get(i);
            if (container.getChannelAddress().equalsIgnoreCase(channelAddress)) {
                return container;
            }
        }
        throw new RuntimeException("No ChannelRecordContainer found for channelAddress " + channelAddress);
    }

    public boolean isEmpty() {
        boolean result = true;
        if (channels.size() != 0) {
            result = false;
        }
        return result;
    }

    public EPrimaryTable getPrimaryTable() {
        return primaryTable;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getCount() {
        return count;
    }

    public int getUnitId() {
        return unitId;
    }

    public EFunctionCode getFunctionCode() {
        return functionCode;
    }

    public ArrayList<ModbusChannel> getChannels() {
        return channels;
    }

}
