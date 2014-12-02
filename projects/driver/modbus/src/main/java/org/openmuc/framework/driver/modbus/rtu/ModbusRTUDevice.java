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
package org.openmuc.framework.driver.modbus.rtu;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.BitVector;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel;
import org.openmuc.framework.driver.modbus.ModbusChannelGroup;
import org.openmuc.framework.driver.modbus.ModbusDevice;
import org.openmuc.framework.driver.modbus.ModbusDriverUtil;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModbusRTUDevice extends ModbusDevice {

    private final static Logger logger = LoggerFactory.getLogger(ModbusRTUDevice.class);

    private ModbusRTUConnection connection = null;
    private final ModbusDriverUtil util;
    private final String deviceAddress;
    private final String[] settings;
    private final int timeout;

    public ModbusRTUDevice(String deviceAddress, String[] settings, int timeout) {
        this.deviceAddress = deviceAddress;
        this.settings = settings;
        this.timeout = timeout;
        util = new ModbusDriverUtil();
    }

    @Override
    public void connect() throws ConnectionException {

        logger.info("Modbus RTU connect called for device: '" + deviceAddress + "'");

        try {
            connection = new ModbusRTUConnection(deviceAddress, settings, timeout);
            connection.connect();
            logger.info("Device connected");

        }
        catch (ModbusConfigurationException e) {
            e.printStackTrace();
            throw new ConnectionException(
                    "Unable to open Modbus RTU connection for device with address "
                    + deviceAddress);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw new ConnectionException(
                    "Unable to open Modbus RTU connection for device with address "
                    + deviceAddress);
        }

    }

    @Override
    public void disconnect() {
        connection.disconnect();
    }

    /**
     * Read a single channel
     *
     * @param channel
     * @return a single Value for the ModbusTcpChannel
     * @throws ModbusException
     */
    @Override
    public Value readChannel(ModbusChannel channel, int timeout) throws ModbusException {
        Value value = null;

        connection.setReceiveTimeout(timeout);

        switch (channel.getFunctionCode()) {
        case FC_01_READ_COILS:
            value = util.getBitVectorsValue(connection.readCoils(channel));
            break;
        case FC_02_READ_DISCRETE_INPUTS:
            value = util.getBitVectorsValue(connection.readDiscreteInputs(channel));
            break;
        case FC_03_READ_HOLDING_REGISTERS:
            value = util.getRegistersValue(connection.readHoldingRegisters(channel),
                                           channel.getDatatype());
            break;
        case FC_04_READ_INPUT_REGISTERS:
            value = util.getRegistersValue(connection.readInputRegisters(channel),
                                           channel.getDatatype());
            break;
        default:
            throw new RuntimeException("FunctionCode "
                                       + channel.getFunctionCode()
                                       + " not supported yet");
        }

        return value;
    }

    /**
     * Read a group of channels
     *
     * @param channelGroup
     * @throws ModbusException
     */
    @Override
    public void readChannelGroup(ModbusChannelGroup channelGroup,
                                 List<ChannelRecordContainer> containers,
                                 int timeout)
            throws ModbusException {

        connection.setReceiveTimeout(timeout);

        switch (channelGroup.getFunctionCode()) {
        case FC_01_READ_COILS:
            BitVector coils = connection.readCoils(channelGroup);
            channelGroup.setChannelValues(coils, this, containers);
            break;
        case FC_02_READ_DISCRETE_INPUTS:
            BitVector discretInput = connection.readDiscreteInputs(channelGroup);
            channelGroup.setChannelValues(discretInput, this, containers);
            break;
        case FC_03_READ_HOLDING_REGISTERS:
            Register[] registers = connection.readHoldingRegisters(channelGroup);
            channelGroup.setChannelValues(registers, this, containers);
            break;
        case FC_04_READ_INPUT_REGISTERS:
            InputRegister[] inputRegisters = connection.readInputRegisters(channelGroup);
            channelGroup.setChannelValues(inputRegisters, this, containers);
            break;
        default:
            throw new RuntimeException("FunctionCode "
                                       + channelGroup.getFunctionCode()
                                       + " not supported yet");
        }
    }

    @Override
    public void writeChannel(ModbusChannel channel, Value value)
            throws ModbusException, RuntimeException {

        switch (channel.getFunctionCode()) {
        case FC_05_WRITE_SINGLE_COIL:
            connection.writeSingleCoil(channel, value.asBoolean());
            break;
        case FC_15_WRITE_MULITPLE_COILS:
            connection.writeMultipleCoils(channel, util.getBitVectorFromByteArray(value));
            break;
        case FC_06_WRITE_SINGLE_REGISTER:
            connection.writeSingleRegister(channel, new SimpleRegister(value.asShort()));
            break;
        case FC_16_WRITE_MULTIPLE_REGISTERS:
            connection.writeMultipleRegisters(channel,
                                              util.valueToRegisters(value, channel.getDatatype()));
            break;
        default:
            throw new RuntimeException("FunctionCode "
                                       + channel.getFunctionCode().toString()
                                       + " not supported yet");
        }
    }

    @Override
    public void setChannelsWithErrorFlag(List<ChannelRecordContainer> containers) {
        for (ChannelRecordContainer container : containers) {
            container.setRecord(new Record(null,
                                           null,
                                           Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE));
        }
    }

}
