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
package org.openmuc.framework.server.modbus;

import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.net.ModbusTCPListener;
import net.wimpi.modbus.procimg.*;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModbusServer implements ServerService {
    private static Logger logger = LoggerFactory.getLogger(ModbusServer.class);
    private ModbusTCPListener listener;

    protected void activate(ComponentContext context) throws IOException {
        logger.info("Activating Modbus Server");
        bindMappings(new ArrayList<ServerMappingContainer>());
        startServer(new SimpleProcessImage());
    }

    private void startServer(SimpleProcessImage spi) {
        ModbusCoupler.getReference().setProcessImage(spi);
        ModbusCoupler.getReference().setMaster(false);
        ModbusCoupler.getReference().setUnitID(15);

        if (listener == null) {
            try {
                listener = new ModbusTCPListener(3, InetAddress.getByName("localhost"));
                listener.setPort(1502);
                listener.start();
            }
            catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating Modbus Server");
        listener.stop();
        listener = null;
    }

    @Override
    public String getId() {
        return "modbus";
    }

    @Override
    public void updatedConfiguration(List<ServerMappingContainer> mappings) {
        bindMappings(mappings);
    }

    @Override
    public void serverMappings(List<ServerMappingContainer> mappings) {
        bindMappings(mappings);
    }

    private void bindMappings(List<ServerMappingContainer> mappings) {
        SimpleProcessImage spi = new SimpleProcessImage();

        for (final ServerMappingContainer container : mappings) {

            String serverAddress = container.getServerMapping().getServerAddress();

            EPrimaryTable primaryTable = EPrimaryTable.getEnumfromString(serverAddress.substring(0,
                                                                                                 serverAddress
                                                                                                         .indexOf(
                                                                                                                 ":")));
            int modbusAddress = Integer.parseInt(serverAddress.substring(serverAddress.indexOf(":")
                                                                         + 1,
                                                                         serverAddress.lastIndexOf(
                                                                                 ":")));
            String dataType = serverAddress.substring(serverAddress.lastIndexOf(":") + 1);

            switch (primaryTable) {
            case INPUT_REGISTERS:
                addInputRegisters(spi,
                                  modbusAddress,
                                  ValueType.valueOf(dataType),
                                  container.getChannel());
                break;
            case HOLDING_REGISTERS:
                addHoldingRegisters(spi,
                                    modbusAddress,
                                    ValueType.valueOf(dataType),
                                    container.getChannel());
                break;
            case COILS:
                // TODO: create for coils
                break;
            case DISCRETE_INPUTS:
                // TODO: create for DI's
                break;
            default:
            }
        }

        startServer(spi);
    }

    private void addHoldingRegisters(SimpleProcessImage spi,
                                     int modbusAddress,
                                     ValueType valueType,
                                     Channel channel) {
        while (spi.getRegisterCount() <= modbusAddress + 4) {
            spi.addRegister(new SimpleRegister());
        }

        switch (valueType) {
        case DOUBLE:
        case LONG:
            Register eightByteRegister3 = new LinkedHoldingRegister(channel, null, valueType, 6, 7);
            Register eightByteRegister2 = new LinkedHoldingRegister(channel,
                                                                    (LinkedHoldingRegister) eightByteRegister3,
                                                                    valueType,
                                                                    4,
                                                                    5);
            Register eightByteRegister1 = new LinkedHoldingRegister(channel,
                                                                    (LinkedHoldingRegister) eightByteRegister2,
                                                                    valueType,
                                                                    2,
                                                                    3);
            Register eightByteRegister0 = new LinkedHoldingRegister(channel,
                                                                    (LinkedHoldingRegister) eightByteRegister1,
                                                                    valueType,
                                                                    0,
                                                                    1);
            spi.setRegister(modbusAddress, eightByteRegister0);
            spi.setRegister(modbusAddress + 1, eightByteRegister1);
            spi.setRegister(modbusAddress + 2, eightByteRegister2);
            spi.setRegister(modbusAddress + 3, eightByteRegister3);
            break;
        case INTEGER:
        case FLOAT:
            Register fourByteRegister1 = new LinkedHoldingRegister(channel, null, valueType, 2, 3);
            Register fourByteRegister0 = new LinkedHoldingRegister(channel,
                                                                   (LinkedHoldingRegister) fourByteRegister1,
                                                                   valueType,
                                                                   0,
                                                                   1);
            spi.setRegister(modbusAddress, fourByteRegister0);
            spi.setRegister(modbusAddress + 1, fourByteRegister1);
            break;
        case SHORT:
        case BOOLEAN:
            Register twoByteRegister = new LinkedHoldingRegister(channel, null, valueType, 0, 1);
            spi.setRegister(modbusAddress, twoByteRegister);
            break;
        default:
            // TODO
        }
    }

    private void addInputRegisters(SimpleProcessImage spi,
                                   int modbusAddress,
                                   ValueType valueType,
                                   Channel channel) {
        while (spi.getInputRegisterCount() <= modbusAddress + 4) {
            spi.addInputRegister(new SimpleInputRegister());
        }

        switch (valueType) {
        case DOUBLE:
        case LONG:
            for (int i = 0; i < 4; i++) {
                spi.setInputRegister(modbusAddress + i,
                                     createInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case INTEGER:
        case FLOAT:
            for (int i = 0; i < 2; i++) {
                spi.setInputRegister(modbusAddress + i,
                                     createInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case SHORT:
        case BOOLEAN:
            spi.setInputRegister(modbusAddress, createInputRegister(channel, 0, 1));
            break;
        default:
            // TODO
        }
    }

    private InputRegister createInputRegister(final Channel channel,
                                              final int bytesFrom,
                                              final int bytesTo) {
        // TODO: Differ between InputRegisters (for read) and HoldingRegisters (for write)
        return new InputRegister() {

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
            public byte[] toBytes() {
                byte[] bytes = channel.getLatestRecord().getValue().asByteArray();
                byte[] toReturn = {bytes[bytesFrom], bytes[bytesTo]};
                return toReturn;
            }

            @Override
            public int getValue() {
                int toReturn = ByteBuffer.wrap(toBytes()).getInt();
                return toReturn;
            }
        };
    }

    public enum EPrimaryTable {
        COILS, //
        DISCRETE_INPUTS, //
        INPUT_REGISTERS, //
        HOLDING_REGISTERS;

        public static EPrimaryTable getEnumfromString(String enumAsString) {
            EPrimaryTable returnValue = null;
            if (enumAsString != null) {
                for (EPrimaryTable value : EPrimaryTable.values()) {
                    if (enumAsString.toUpperCase().equals(value.toString())) {
                        returnValue = EPrimaryTable.valueOf(enumAsString.toUpperCase());
                        break;
                    }
                }
            }
            if (returnValue == null) {
                throw new RuntimeException(enumAsString
                                           + " is not supported. Use one of the following supported primary tables: "
                                           + getSupportedValues());
            }
            return returnValue;
        }

        /**
         * @return all supported values as a comma separated string
         */
        public static String getSupportedValues() {
            String supported = "";
            for (EPrimaryTable value : EPrimaryTable.values()) {
                supported += value.toString() + ", ";
            }
            return supported;
        }
    }
}
