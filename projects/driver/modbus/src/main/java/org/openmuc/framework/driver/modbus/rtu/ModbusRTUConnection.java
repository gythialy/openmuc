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
package org.openmuc.framework.driver.modbus.rtu;

import java.util.Enumeration;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.ModbusChannel;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;
import org.openmuc.framework.driver.modbus.ModbusConnection;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

/**
 * 
 * TODO
 * 
 */
public class ModbusRTUConnection extends ModbusConnection {

    private final static Logger logger = LoggerFactory.getLogger(ModbusRTUConnection.class);

    private static final int ENCODING = 1;
    private static final int BAUDRATE = 2;
    private static final int DATABITS = 3;
    private static final int PARITY = 4;
    private static final int STOPBITS = 5;
    private static final int ECHO = 6;
    private static final int FLOWCONTROL_IN = 7;
    private static final int FLOWCONTEOL_OUT = 8;

    private static final String SERIAL_ENCODING_RTU = "SERIAL_ENCODING_RTU";

    private static final String ECHO_TRUE = "ECHO_TRUE";
    private static final String ECHO_FALSE = "ECHO_FALSE";

    private final SerialConnection connection;
    private ModbusSerialTransaction transaction;

    public ModbusRTUConnection(String deviceAddress, String[] settings, int timeout)
            throws ModbusConfigurationException {

        super();

        SerialParameters params = setParameters(deviceAddress, settings, timeout);
        connection = new SerialConnection(params);

        try {
            connect();
            transaction = new ModbusSerialTransaction(connection);
            transaction.setSerialConnection(connection);
            setTransaction(transaction);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ModbusConfigurationException("Wrong Modbus RTU configuration. Check configuration file");
        }
        logger.info("Modbus Device: " + deviceAddress + " connected");
    }

    @Override
    public void connect() throws Exception {
        if (!connection.isOpen()) {
            connection.open();
        }
    }

    @Override
    public void disconnect() {
        if (connection.isOpen()) {
            connection.close();
        }
    }

    public void setReceiveTimeout(int timeout) {
        connection.setReceiveTimeout(timeout);
    }

    private SerialParameters setParameters(String address, String[] settings, int timeout)
            throws ModbusConfigurationException {

        SerialParameters params = new SerialParameters();

        checkIfAddressIsAvailbale(address);
        params.setPortName(address);

        if (settings.length == 9) {
            setEncoding(params, settings[ENCODING]);
            setBaudrate(params, settings[BAUDRATE]);
            setDatabits(params, settings[DATABITS]);
            setParity(params, settings[PARITY]);
            setStopbits(params, settings[STOPBITS]);
            setEcho(params, settings[ECHO]);
            setFlowControlIn(params, settings[FLOWCONTROL_IN]);
            setFlowControlOut(params, settings[FLOWCONTEOL_OUT]);
        }
        else {
            throw new ModbusConfigurationException("Settings parameter missing. Specify all settings parameter");
        }

        return params;
    }

    /**
     * Checks if the gnu.io.rxtx is able to find the specified address e.g. /dev/ttyUSB0
     * 
     * @param address
     * @throws ModbusConfigurationException
     */
    private void checkIfAddressIsAvailbale(String address) throws ModbusConfigurationException {
        Enumeration ports = CommPortIdentifier.getPortIdentifiers();

        boolean result = false;

        while (ports.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier) ports.nextElement();
            if (cpi.getName().equalsIgnoreCase(address)) {
                result = true;
                break;
            }
        }

        if (!result) {
            String availablePorts = getAvailablePorts();
            throw new ModbusConfigurationException("gnu.io.rxtx is unable to detect address: " + address
                    + ". Available addresses are: '" + availablePorts + "'");
        }
    }

    private String getAvailablePorts() {

        String availablePorts = "";

        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier) ports.nextElement();
            availablePorts += cpi.getName() + "; ";
        }

        return availablePorts;
    }

    private void setFlowControlIn(SerialParameters params, String flowControlIn) throws ModbusConfigurationException {
        if (flowControlIn.equalsIgnoreCase("FLOWCONTROL_NONE")) {
            params.setFlowControlIn(SerialPort.FLOWCONTROL_NONE);
        }
        else if (flowControlIn.equalsIgnoreCase("FLOWCONTROL_RTSCTS_IN")) {
            params.setFlowControlIn(SerialPort.FLOWCONTROL_RTSCTS_IN);
        }
        else if (flowControlIn.equalsIgnoreCase("FLOWCONTROL_XONXOFF_IN")) {
            params.setFlowControlIn(SerialPort.FLOWCONTROL_XONXOFF_IN);
        }
        else {
            throw new ModbusConfigurationException("Unknown flow control in setting. Check configuration file");
        }

    }

    private void setFlowControlOut(SerialParameters params, String flowControlOut) throws ModbusConfigurationException {
        if (flowControlOut.equalsIgnoreCase("FLOWCONTROL_NONE")) {
            params.setFlowControlOut(SerialPort.FLOWCONTROL_NONE);
        }
        else if (flowControlOut.equalsIgnoreCase("FLOWCONTROL_RTSCTS_OUT")) {
            params.setFlowControlOut(SerialPort.FLOWCONTROL_RTSCTS_OUT);
        }
        else if (flowControlOut.equalsIgnoreCase("FLOWCONTROL_XONXOFF_OUT")) {
            params.setFlowControlOut(SerialPort.FLOWCONTROL_XONXOFF_OUT);
        }
        else {
            throw new ModbusConfigurationException("Unknown flow control out setting. Check configuration file");
        }

    }

    private void setEcho(SerialParameters params, String echo) throws ModbusConfigurationException {
        if (echo.equalsIgnoreCase(ECHO_TRUE)) {
            params.setEcho(true);
        }
        else if (echo.equalsIgnoreCase(ECHO_FALSE)) {
            params.setEcho(false);
        }
        else {
            throw new ModbusConfigurationException("Unknown echo setting. Check configuration file");
        }

    }

    private void setStopbits(SerialParameters params, String stopbits) throws ModbusConfigurationException {
        if (stopbits.equalsIgnoreCase("STOPBITS_1")) {
            params.setStopbits(SerialPort.STOPBITS_1);
        }
        else if (stopbits.equalsIgnoreCase("STOPBITS_1_5")) {
            params.setStopbits(SerialPort.STOPBITS_1_5);
        }
        else if (stopbits.equalsIgnoreCase("STOPBITS_2")) {
            params.setStopbits(SerialPort.STOPBITS_2);
        }
        else {
            throw new ModbusConfigurationException("Unknown stobit setting. Check configuration file");
        }

    }

    private void setParity(SerialParameters params, String parity) throws ModbusConfigurationException {
        if (parity.equalsIgnoreCase("PARITY_EVEN")) {
            params.setParity(SerialPort.PARITY_EVEN);
        }
        else if (parity.equalsIgnoreCase("PARITY_MARK")) {
            params.setParity(SerialPort.PARITY_MARK);
        }
        else if (parity.equalsIgnoreCase("PARITY_NONE")) {
            params.setParity(SerialPort.PARITY_NONE);
        }
        else if (parity.equalsIgnoreCase("PARITY_ODD")) {
            params.setParity(SerialPort.PARITY_ODD);
        }
        else if (parity.equalsIgnoreCase("PARITY_SPACE")) {
            params.setParity(SerialPort.PARITY_SPACE);
        }
        else {
            throw new ModbusConfigurationException("Unknown parity setting. Check configuration file");
        }

    }

    private void setDatabits(SerialParameters params, String databits) throws ModbusConfigurationException {
        if (databits.equalsIgnoreCase("DATABITS_5")) {
            params.setDatabits(SerialPort.DATABITS_5);
        }
        else if (databits.equalsIgnoreCase("DATABITS_6")) {
            params.setDatabits(SerialPort.DATABITS_6);
        }
        else if (databits.equalsIgnoreCase("DATABITS_7")) {
            params.setDatabits(SerialPort.DATABITS_7);
        }
        else if (databits.equalsIgnoreCase("DATABITS_8")) {
            params.setDatabits(SerialPort.DATABITS_8);
        }
        else {
            throw new ModbusConfigurationException("Unknown databit setting. Check configuration file");
        }
    }

    private void setBaudrate(SerialParameters params, String baudrate) {
        params.setBaudRate(baudrate);
    }

    private void setEncoding(SerialParameters params, String encoding) throws ModbusConfigurationException {
        if (encoding.equalsIgnoreCase(SERIAL_ENCODING_RTU)) {
            params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
        }
        else {
            throw new ModbusConfigurationException("Unknown encoding setting. Check configuration file");
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer container : containers) {
            long receiveTime = System.currentTimeMillis();
            ModbusChannel channel = getModbusChannel(container.getChannelAddress(), EAccess.READ);
            Value value;
            try {
                value = readChannel(channel);
                container.setRecord(new Record(value, receiveTime));
            } catch (ModbusException e) {
                e.printStackTrace();
                container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
            } catch (Exception e) {
                // catch all possible exceptions and provide info about the channel
                logger.error("Unable to read channel: " + container.getChannelAddress(), e);
                container.setRecord(new Record(Flag.UNKNOWN_ERROR));
            }
        }

        // logger.debug("### readChannels duration in ms = " + ((new Date().getTime()) - startTime));

        return null;
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        for (ChannelValueContainer container : containers) {

            ModbusChannel modbusChannel = getModbusChannel(container.getChannelAddress(), EAccess.WRITE);
            if (modbusChannel != null) {
                try {
                    writeChannel(modbusChannel, container.getValue());
                    container.setFlag(Flag.VALID);
                } catch (ModbusException modbusException) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    modbusException.printStackTrace();
                    throw new ConnectionException(
                            "Unable to write data on channel address: " + container.getChannelAddress());
                } catch (Exception e) {
                    container.setFlag(Flag.UNKNOWN_ERROR);
                    e.printStackTrace();
                    logger.error("Unable to write data on channel address: " + container.getChannelAddress());
                }
            }
            else {
                // TODO
                container.setFlag(Flag.UNKNOWN_ERROR);
                logger.error("Unable to write data on channel address: " + container.getChannelAddress()
                        + "modbusChannel = null");
            }
        }

        return null;

    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
