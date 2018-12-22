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
import java.util.Hashtable;
import java.util.List;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleCoilsRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;

public abstract class ModbusConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(ModbusConnection.class);

    private ModbusTransaction transaction;
    // List do manage Channel Objects to avoid to check the syntax of each channel address for every read or write
    private final Hashtable<String, ModbusChannel> modbusChannels;

    private int requestTransactionId;
    private final int MAX_RETRIES_FOR_JAMOD = 0;
    private final int MAX_RETRIES_FOR_DRIVER = 3;

    public abstract void connect() throws ConnectionException;

    @Override
    public abstract void disconnect();

    public ModbusConnection() {

        transaction = null;
        modbusChannels = new Hashtable<>();
    }

    public synchronized void setTransaction(ModbusTransaction transaction) {
        this.transaction = transaction;

        // WORKAROUND: The jamod ModbusTCPTransaction.execute() tries maximum 3 times (default) to sent request and read
        // response. Problematic is a "java.net.SocketTimeoutException: Read timed out" while trying to get the
        // response. This exception is swallowed up by the library since it tries 3 times to get the data. Since the
        // jamod doesn't check the transaction id of the response, we assume that this causes the mismatch between
        // request and response.
        // To fix this we set the retries to 0 so the SocketTimeoutException isn't swallowed by the lib and we can
        // handle it, according to our needs
        // TODO: We might need to implement our own retry mechanism within the driver so that the first timeout doesn't
        // directly causes a ConnectionException
        this.transaction.setRetries(MAX_RETRIES_FOR_JAMOD);
    }

    public Value readChannel(ModbusChannel channel) throws ModbusException {

        if (logger.isDebugEnabled()) {
            logger.debug("read channel: " + channel.getChannelAddress());
        }

        Value value = null;

        switch (channel.getFunctionCode()) {
        case FC_01_READ_COILS:
            value = ModbusDriverUtil.getBitVectorsValue(readCoils(channel));
            break;
        case FC_02_READ_DISCRETE_INPUTS:
            value = ModbusDriverUtil.getBitVectorsValue(readDiscreteInputs(channel));
            break;
        case FC_03_READ_HOLDING_REGISTERS:
            value = ModbusDriverUtil.getRegistersValue(readHoldingRegisters(channel), channel.getDatatype());
            break;
        case FC_04_READ_INPUT_REGISTERS:
            value = ModbusDriverUtil.getRegistersValue(readInputRegisters(channel), channel.getDatatype());
            break;
        default:
            throw new RuntimeException("FunctionCode " + channel.getFunctionCode() + " not supported yet");
        }

        return value;
    }

    public Object readChannelGroupHighLevel(List<ChannelRecordContainer> containers, Object containerListHandle,
            String samplingGroup) throws ConnectionException {

        // NOTE: containerListHandle is null if something changed in configuration!!!

        ModbusChannelGroup channelGroup = null;

        // use existing channelGroup
        if (containerListHandle != null) {
            if (containerListHandle instanceof ModbusChannelGroup) {
                channelGroup = (ModbusChannelGroup) containerListHandle;
            }
        }

        // create new channelGroup
        if (channelGroup == null) {
            ArrayList<ModbusChannel> channelList = new ArrayList<>();
            for (ChannelRecordContainer container : containers) {
                channelList.add(getModbusChannel(container.getChannelAddress(), EAccess.READ));
            }
            channelGroup = new ModbusChannelGroup(samplingGroup, channelList);
        }

        // read all channels of the group
        try {
            readChannelGroup(channelGroup, containers);

        } catch (ModbusIOException e) {
            logger.error("ModbusIOException while reading samplingGroup:" + samplingGroup, e);
            disconnect();
            throw new ConnectionException(e);
        } catch (ModbusException e) {
            logger.error("Unable to read ChannelGroup", e);

            // set channel values and flag, otherwise the datamanager will throw a null pointer exception
            // and the framework collapses.
            setChannelsWithErrorFlag(containers);
        }
        return channelGroup;
    }

    private void readChannelGroup(ModbusChannelGroup channelGroup, List<ChannelRecordContainer> containers)
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

        if (logger.isDebugEnabled()) {
            logger.debug("write channel: " + channel.getChannelAddress());
        }

        switch (channel.getFunctionCode()) {
        case FC_05_WRITE_SINGLE_COIL:
            writeSingleCoil(channel, value.asBoolean());
            break;
        case FC_15_WRITE_MULITPLE_COILS:
            writeMultipleCoils(channel, ModbusDriverUtil.getBitVectorFromByteArray(value));
            break;
        case FC_06_WRITE_SINGLE_REGISTER:
            writeSingleRegister(channel, new SimpleRegister(value.asShort()));
            break;
        case FC_16_WRITE_MULTIPLE_REGISTERS:
            writeMultipleRegisters(channel, ModbusDriverUtil.valueToRegisters(value, channel.getDatatype()));
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

    // TODO refactoring - to evaluate the transaction id the execution should be part of the modbus tcp connection and
    // not part of the common modbusConnection since RTU has no transaction id
    private ModbusResponse executeReadTransaction() throws ModbusException {

        ModbusResponse response = null;

        if (transaction instanceof ModbusTCPTransaction) {
            // see: performModbusTCPReadTransactionWithRetry()
            response = performModbusReadTransaction();
        }
        else {
            // other than modbus TCP
            response = performModbusReadTransaction();

        }

        if (response == null) {
            throw new ModbusException("received response object is null");
        }
        else {
            printResponseTraceMsg(response);
        }

        return response;
    }

    private ModbusResponse performModbusReadTransaction() throws ModbusException {
        printRequestTraceMsg();
        transaction.execute();
        ModbusResponse response = transaction.getResponse();
        return response;
    }

    // FIXME concept with retry is not working after a java.net.SocketTimeoutException: Read timed out
    // Problem is that the Transaction.excecute() increments the Transaction ID with each execute. Example: Request is
    // sent with Transaction ID 30, then a timeout happens, when using the retry mechanism below it will resend the
    // request. the jamod increases the transaction id again to 31 but then it receives the response for id 30. From the
    // time a timeout happened the response id will be always smaller than the request id, since the jamod doesn't
    // provide a method to read a response without sending a request.
    private ModbusResponse performModbusTCPReadTransactionWithRetry() throws ModbusException {

        ModbusResponse response = null;

        // NOTE: see comments about max retries in setTransaction()
        int retries = 0;

        while (retries < MAX_RETRIES_FOR_DRIVER) {
            // +1 because id is incremented within transaction execution

            // int requestId = transaction.getTransactionID() + 1;
            printRequestTraceMsg();

            try {
                transaction.execute();
            } catch (ModbusIOException e) {
                // logger.trace("caught ModbusIOException, probably timeout, retry");
                retries++;
                checkRetryCondition(retries);
                continue;
            }

            if (isTransactionIdMatching()) {
                response = transaction.getResponse();
                break;
            }
            else {
                retries++;
                checkRetryCondition(retries);
            }

        }

        return response;
    }

    /**
     * 
     * @param retries
     * @throws ModbusIOException
     *             if max number of retries is reached, which indicates an IO problem.
     */
    private void checkRetryCondition(int retries) throws ModbusIOException {
        if (retries == MAX_RETRIES_FOR_DRIVER) {
            logger.trace("Failed to get response. Retry " + retries + "/" + MAX_RETRIES_FOR_DRIVER);
            throw new ModbusIOException("Unable to get response. Max number of retries reached");
        }
        else {
            logger.trace("Failed to get response. Retry " + retries + "/" + MAX_RETRIES_FOR_DRIVER);
        }
    }

    private boolean isTransactionIdMatching() {

        boolean isMatching = false;

        int requestId = transaction.getRequest().getTransactionID();
        int responseId = transaction.getResponse().getTransactionID();

        if (requestId == responseId) {
            isMatching = true;
        }
        else {
            logger.warn("Mismatching transaction IDs: request (" + requestId + ") / response (" + responseId
                    + "). Retrying transaction...");
        }

        return isMatching;
    }

    private void executeWriteTransaction() throws ModbusException {
        printRequestTraceMsg();
        transaction.execute();
        // FIXME evaluate response
        ModbusResponse response = transaction.getResponse();
        printResponseTraceMsg(response);
    }

    private synchronized BitVector readCoils(int startAddress, int count, int unitID) throws ModbusException {
        ReadCoilsRequest readCoilsRequest = new ReadCoilsRequest();
        readCoilsRequest.setReference(startAddress);
        readCoilsRequest.setBitCount(count);
        readCoilsRequest.setUnitID(unitID);

        if (transaction instanceof ModbusSerialTransaction) {
            readCoilsRequest.setHeadless();
        }

        transaction.setRequest(readCoilsRequest);
        ModbusResponse response = executeReadTransaction();
        BitVector bitvector = ((ReadCoilsResponse) response).getCoils();
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
        ReadInputDiscretesRequest readInputDiscretesRequest = new ReadInputDiscretesRequest();
        readInputDiscretesRequest.setReference(startAddress);
        readInputDiscretesRequest.setBitCount(count);
        readInputDiscretesRequest.setUnitID(unitID);

        if (transaction instanceof ModbusSerialTransaction) {
            readInputDiscretesRequest.setHeadless();
        }

        transaction.setRequest(readInputDiscretesRequest);
        ModbusResponse response = executeReadTransaction();
        BitVector bitvector = ((ReadInputDiscretesResponse) response).getDiscretes();
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
        ReadMultipleRegistersRequest readHoldingRegisterRequest = new ReadMultipleRegistersRequest();
        readHoldingRegisterRequest.setReference(startAddress);
        readHoldingRegisterRequest.setWordCount(count);
        readHoldingRegisterRequest.setUnitID(unitID);

        if (transaction instanceof ModbusSerialTransaction) {
            readHoldingRegisterRequest.setHeadless();
        }

        transaction.setRequest(readHoldingRegisterRequest);
        ModbusResponse response = executeReadTransaction();
        return ((ReadMultipleRegistersResponse) response).getRegisters();
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
        ReadInputRegistersRequest readInputRegistersRequest = new ReadInputRegistersRequest();
        readInputRegistersRequest.setReference(startAddress);
        readInputRegistersRequest.setWordCount(count);
        readInputRegistersRequest.setUnitID(unitID);

        if (transaction instanceof ModbusSerialTransaction) {
            readInputRegistersRequest.setHeadless();
        }

        transaction.setRequest(readInputRegistersRequest);
        ModbusResponse response = executeReadTransaction();
        InputRegister[] registers = ((ReadInputRegistersResponse) response).getRegisters();
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
        WriteCoilRequest writeCoilRequest = new WriteCoilRequest();
        writeCoilRequest.setReference(channel.getStartAddress());
        writeCoilRequest.setCoil(state);
        writeCoilRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeCoilRequest);
        executeWriteTransaction();
    }

    public synchronized void writeMultipleCoils(ModbusChannel channel, BitVector coils) throws ModbusException {
        WriteMultipleCoilsRequest writeMultipleCoilsRequest = new WriteMultipleCoilsRequest();
        writeMultipleCoilsRequest.setReference(channel.getStartAddress());
        writeMultipleCoilsRequest.setCoils(coils);
        writeMultipleCoilsRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeMultipleCoilsRequest);
        executeWriteTransaction();
    }

    public synchronized void writeSingleRegister(ModbusChannel channel, Register register) throws ModbusException {
        WriteSingleRegisterRequest writeSingleRegisterRequest = new WriteSingleRegisterRequest();
        writeSingleRegisterRequest.setReference(channel.getStartAddress());
        writeSingleRegisterRequest.setRegister(register);
        writeSingleRegisterRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeSingleRegisterRequest);
        executeWriteTransaction();
    }

    public synchronized void writeMultipleRegisters(ModbusChannel channel, Register[] registers)
            throws ModbusException {
        WriteMultipleRegistersRequest writeMultipleRegistersRequest = new WriteMultipleRegistersRequest();
        writeMultipleRegistersRequest.setReference(channel.getStartAddress());
        writeMultipleRegistersRequest.setRegisters(registers);
        writeMultipleRegistersRequest.setUnitID(channel.getUnitId());
        transaction.setRequest(writeMultipleRegistersRequest);
        executeWriteTransaction();
    }

    // FIXME transaction ID unsupported by RTU since it is headless... create own debug for RTU
    private void printRequestTraceMsg() {

        if (logger.isTraceEnabled()) {
            logger.trace(createRequestTraceMsg());
        }
    }

    // FIXME: This debug message should be inside the transaction.execute() of the jamod.
    // The problem is, that the hex message (especially the transaction ID) is set within the execute method. The hex
    // message here shows a wrong transaction id.
    private String createRequestTraceMsg() {

        ModbusRequest request = transaction.getRequest();

        // Transaction ID is incremented within the transaction.execute command. To view correct transaction Id in debug
        // output the value is incremented by one
        requestTransactionId = transaction.getTransactionID() + 1;

        String traceMsg = "";

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("REQUEST: " + request.getHexMessage() + "\n");
            sb.append("- transaction ID: " + requestTransactionId + "\n");
            sb.append("- protocol ID   : " + request.getProtocolID() + "\n");
            sb.append("- data length   : " + request.getDataLength() + "\n");
            sb.append("- unit ID       : " + request.getUnitID() + "\n");
            sb.append("- function code : " + request.getFunctionCode() + "\n");
            sb.append("- is headless   : " + request.isHeadless() + "\n");
            sb.append("- max retries   : " + transaction.getRetries());

            if (transaction instanceof ModbusTCPTransaction) {
                sb.append("\n   (NOTE: incorrect transaction Id displayed in hex message due to issue with jamod)");
            }

            traceMsg = sb.toString();
        } catch (

        Exception e) {
            logger.trace("Unable to create debug message from request", e);
        }

        return traceMsg;
    }

    private void printResponseTraceMsg(ModbusResponse response) {
        if (logger.isTraceEnabled()) {
            logger.trace(createResponseTraceMsg(response));
        }
    }

    private String createResponseTraceMsg(ModbusResponse response) {

        int responseTransactionId = response.getTransactionID();

        if (transaction instanceof ModbusTCPTransaction) {
            if (responseTransactionId > (requestTransactionId + MAX_RETRIES_FOR_DRIVER)) {
                logger.warn("responseTransactionId > (lastRequestTransactionId + MAX_RETRIES)");
            }
        }

        String traceMsg = "";

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("RESPONSE: " + response.getHexMessage() + "\n");
            sb.append("- transaction ID: " + responseTransactionId + "\n");
            sb.append("- protocol ID   : " + response.getProtocolID() + "\n");
            sb.append("- unit ID       : " + response.getUnitID() + "\n");
            sb.append("- function code : " + response.getFunctionCode() + "\n");
            sb.append("- length        : " + response.getDataLength() + "\n");
            sb.append("- is headless   : " + response.isHeadless() + "\n");
            sb.append("- max retries   : " + transaction.getRetries());

            traceMsg = sb.toString();
        } catch (Exception e) {
            logger.trace("Unable to create debug message from received response", e);
        }

        return traceMsg;
    }

}
