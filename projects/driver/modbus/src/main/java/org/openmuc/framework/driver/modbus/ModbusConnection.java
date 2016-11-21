/*
 * Copyright 2011-16 Fraunhofer ISE
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

import java.util.Hashtable;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteMultipleCoilsRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.BitVector;

public abstract class ModbusConnection implements Connection {

    private final ReadCoilsRequest readCoilsRequest;
    private final ReadInputDiscretesRequest readInputDiscretesRequest;
    private final WriteCoilRequest writeCoilRequest;
    private final WriteMultipleCoilsRequest writeMultipleCoilsRequest;
    private final ReadInputRegistersRequest readInputRegistersRequest;
    private final ReadMultipleRegistersRequest readHoldingRegisterRequest;
    private final WriteSingleRegisterRequest writeSingleRegisterRequest;
    private final WriteMultipleRegistersRequest writeMultipleRegistersRequest;

    private ModbusTransaction transaction;
    private final ModbusDriverUtil util;
    // List do manage Channel Objects to avoid to check the syntax of each channel address for every read or write
    private final Hashtable<String, ModbusChannel> modbusChannels;

    public abstract void connect() throws Exception;

    @Override
    public abstract void disconnect();

    public ModbusConnection() {

        transaction = null;
        util = new ModbusDriverUtil();
        modbusChannels = new Hashtable<>();

        readCoilsRequest = new ReadCoilsRequest();
        readInputDiscretesRequest = new ReadInputDiscretesRequest();
        readInputRegistersRequest = new ReadInputRegistersRequest();
        readHoldingRegisterRequest = new ReadMultipleRegistersRequest();
        writeCoilRequest = new WriteCoilRequest();
        writeMultipleCoilsRequest = new WriteMultipleCoilsRequest();
        writeSingleRegisterRequest = new WriteSingleRegisterRequest();
        writeMultipleRegistersRequest = new WriteMultipleRegistersRequest();
    }

    public void setTransaction(ModbusTransaction transaction) {
        this.transaction = transaction;
    }

    public Value readChannel(ModbusChannel channel) throws ModbusException {
        Value value = null;

        switch (channel.getFunctionCode()) {
        case FC_01_READ_COILS:
            value = util.getBitVectorsValue(readCoils(channel));
            break;
        case FC_02_READ_DISCRETE_INPUTS:
            value = util.getBitVectorsValue(readDiscreteInputs(channel));
            break;
        case FC_03_READ_HOLDING_REGISTERS:
            value = util.getRegistersValue(readHoldingRegisters(channel), channel.getDatatype());
            break;
        case FC_04_READ_INPUT_REGISTERS:
            value = util.getRegistersValue(readInputRegisters(channel), channel.getDatatype());
            break;
        default:
            throw new RuntimeException("FunctionCode " + channel.getFunctionCode() + " not supported yet");
        }

        return value;
    }

    public void readChannelGroup(ModbusChannelGroup channelGroup, List<ChannelRecordContainer> containers)
            throws ModbusException {

        switch (channelGroup.getFunctionCode()) {
        case FC_01_READ_COILS:
            BitVector coils = readCoils(channelGroup);
            channelGroup.setChannelValues(coils, containers);
            break;
        case FC_02_READ_DISCRETE_INPUTS:
            BitVector discretInput = readDiscreteInputs(channelGroup);
            channelGroup.setChannelValues(discretInput, containers);
            break;
        case FC_03_READ_HOLDING_REGISTERS:
            Register[] registers = readHoldingRegisters(channelGroup);
            channelGroup.setChannelValues(registers, containers);
            break;
        case FC_04_READ_INPUT_REGISTERS:
            InputRegister[] inputRegisters = readInputRegisters(channelGroup);
            channelGroup.setChannelValues(inputRegisters, containers);
            break;
        default:
            throw new RuntimeException("FunctionCode " + channelGroup.getFunctionCode() + " not supported yet");
        }
    }

    public void writeChannel(ModbusChannel channel, Value value) throws ModbusException, RuntimeException {

        switch (channel.getFunctionCode()) {
        case FC_05_WRITE_SINGLE_COIL:
            writeSingleCoil(channel, value.asBoolean());
            break;
        case FC_15_WRITE_MULITPLE_COILS:
            writeMultipleCoils(channel, util.getBitVectorFromByteArray(value));
            break;
        case FC_06_WRITE_SINGLE_REGISTER:
            writeSingleRegister(channel, new SimpleRegister(value.asShort()));
            break;
        case FC_16_WRITE_MULTIPLE_REGISTERS:
            writeMultipleRegisters(channel, util.valueToRegisters(value, channel.getDatatype()));
            break;
        default:
            throw new RuntimeException("FunctionCode " + channel.getFunctionCode().toString() + " not supported yet");
        }
    }

    public void setChannelsWithErrorFlag(List<ChannelRecordContainer> containers) {
        for (ChannelRecordContainer container : containers) {
            container.setRecord(new Record(null, null, Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE));
        }
    }

    protected ModbusChannel getModbusChannel(String channelAddress, EAccess access) {

        ModbusChannel modbusChannel = null;

        // check if the channel object already exists in the list
        if (modbusChannels.containsKey(channelAddress)) {
            modbusChannel = modbusChannels.get(channelAddress);

            // if the channel object exists the access flag might has to be updated
            // (this is case occurs when the channel is readable and writable)
            if (!modbusChannel.getAccessFlag().equals(access)) {
                modbusChannel.update(access);
            }
        }
        // create a new channel object
        else {
            modbusChannel = new ModbusChannel(channelAddress, access);
            modbusChannels.put(channelAddress, modbusChannel);
        }

        return modbusChannel;

    }

    private synchronized BitVector readCoils(int startAddress, int count, int unitID) throws ModbusException {
        readCoilsRequest.setReference(startAddress);
        readCoilsRequest.setBitCount(count);
        readCoilsRequest.setUnitID(unitID);
        transaction.setRequest(readCoilsRequest);
        transaction.execute();
        BitVector bitvector = ((ReadCoilsResponse) transaction.getResponse()).getCoils();
        bitvector.forceSize(count);
        return bitvector;

    }

    public BitVector readCoils(ModbusChannel channel) throws ModbusException {
        return readCoils(channel.getStartAddress(), channel.getCount(), channel.getUnitId());
    }

    public BitVector readCoils(ModbusChannelGroup channelGroup) throws ModbusException {
        return readCoils(channelGroup.getStartAddress(), channelGroup.getCount(), channelGroup.getUnitId());
    }

    private synchronized BitVector readDiscreteInputs(int startAddress, int count, int unitID) throws ModbusException {
        readInputDiscretesRequest.setReference(startAddress);
        readInputDiscretesRequest.setBitCount(count);
        readInputDiscretesRequest.setUnitID(unitID);
        transaction.setRequest(readInputDiscretesRequest);
        transaction.execute();
        BitVector bitvector = ((ReadInputDiscretesResponse) transaction.getResponse()).getDiscretes();
        bitvector.forceSize(count);
        return bitvector;
    }

    public BitVector readDiscreteInputs(ModbusChannel channel) throws ModbusException {
        return readDiscreteInputs(channel.getStartAddress(), channel.getCount(), channel.getUnitId());
    }

    public BitVector readDiscreteInputs(ModbusChannelGroup channelGroup) throws ModbusException {
        return readDiscreteInputs(channelGroup.getStartAddress(), channelGroup.getCount(), channelGroup.getUnitId());
    }

    private synchronized Register[] readHoldingRegisters(int startAddress, int count, int unitID)
            throws ModbusException {
        readHoldingRegisterRequest.setReference(startAddress);
        readHoldingRegisterRequest.setWordCount(count);
        readHoldingRegisterRequest.setUnitID(unitID);
        transaction.setRequest(readHoldingRegisterRequest);
        transaction.execute();
        return ((ReadMultipleRegistersResponse) transaction.getResponse()).getRegisters();
    }

    public Register[] readHoldingRegisters(ModbusChannel channel) throws ModbusException {
        return readHoldingRegisters(channel.getStartAddress(), channel.getCount(), channel.getUnitId());
    }

    public Register[] readHoldingRegisters(ModbusChannelGroup channelGroup) throws ModbusException {
        return readHoldingRegisters(channelGroup.getStartAddress(), channelGroup.getCount(), channelGroup.getUnitId());
    }

    /**
     * Read InputRegisters
     * 
     */
    private synchronized InputRegister[] readInputRegisters(int startAddress, int count, int unitID)
            throws ModbusIOException, ModbusSlaveException, ModbusException {
        readInputRegistersRequest.setReference(startAddress);
        readInputRegistersRequest.setWordCount(count);
        readInputRegistersRequest.setUnitID(unitID);
        transaction.setRequest(readInputRegistersRequest);
        transaction.execute();
        InputRegister[] registers = ((ReadInputRegistersResponse) transaction.getResponse()).getRegisters();
        return registers;
    }

    /**
     * Read InputRegisters for a channel
     * 
     * @param channel
     *            Modbus channel
     * @return input register array
     * @throws ModbusException
     *             if an modbus error occurs
     */
    public InputRegister[] readInputRegisters(ModbusChannel channel) throws ModbusException {
        return readInputRegisters(channel.getStartAddress(), channel.getCount(), channel.getUnitId());
    }

    /**
     * Read InputRegisters for a channelGroup
     * 
     * @param channelGroup
     *            modbus channel group
     * @return the input register array
     * @throws ModbusException
     *             if an modbus error occurs
     */
    public InputRegister[] readInputRegisters(ModbusChannelGroup channelGroup) throws ModbusException {
        return readInputRegisters(channelGroup.getStartAddress(), channelGroup.getCount(), channelGroup.getUnitId());
    }

    public synchronized void writeSingleCoil(ModbusChannel channel, boolean state) throws ModbusException {
        writeCoilRequest.setReference(channel.getStartAddress());
        writeCoilRequest.setCoil(state);
        writeCoilRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeCoilRequest);
        transaction.execute();
    }

    public synchronized void writeMultipleCoils(ModbusChannel channel, BitVector coils) throws ModbusException {
        writeMultipleCoilsRequest.setReference(channel.getStartAddress());
        writeMultipleCoilsRequest.setCoils(coils);
        writeMultipleCoilsRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeMultipleCoilsRequest);
        transaction.execute();
    }

    public synchronized void writeSingleRegister(ModbusChannel channel, Register register) throws ModbusException {

        writeSingleRegisterRequest.setReference(channel.getStartAddress());
        writeSingleRegisterRequest.setRegister(register);
        writeSingleRegisterRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeSingleRegisterRequest);
        transaction.execute();
    }

    public synchronized void writeMultipleRegisters(ModbusChannel channel, Register[] registers)
            throws ModbusException {
        writeMultipleRegistersRequest.setReference(channel.getStartAddress());
        writeMultipleRegistersRequest.setRegisters(registers);
        writeMultipleRegistersRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeMultipleRegistersRequest);
        transaction.execute();
    }

}
