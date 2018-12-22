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
package org.openmuc.framework.server.modbus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.server.modbus.register.BooleanMappingInputRegister;
import org.openmuc.framework.server.modbus.register.DoubleMappingInputRegister;
import org.openmuc.framework.server.modbus.register.FloatMappingInputRegister;
import org.openmuc.framework.server.modbus.register.IntegerMappingInputRegister;
import org.openmuc.framework.server.modbus.register.LinkedMappingHoldingRegister;
import org.openmuc.framework.server.modbus.register.LongMappingInputRegister;
import org.openmuc.framework.server.modbus.register.ShortMappingInputRegister;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusCoupler;
import com.ghgande.j2mod.modbus.net.ModbusTCPListener;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

@Component
public class ModbusServer implements ServerService {
    private static Logger logger = LoggerFactory.getLogger(ModbusServer.class);
    private ModbusTCPListener listener;
    private Thread listenThread;
    private ServerSettings settings;

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        logger.info("Activating Modbus Server");
        settings = new ServerSettings();

        bindMappings(new ArrayList<ServerMappingContainer>());
        startServer(new SimpleProcessImage());
    }

    private void startServer(SimpleProcessImage spi) {
        ModbusCoupler.getReference().setProcessImage(spi);
        ModbusCoupler.getReference().setMaster(settings.isMaster());
        ModbusCoupler.getReference().setUnitID(settings.getUnitId());

        if (listener == null) {
            try {
                listener = new ModbusTCPListener(3, InetAddress.getByName(settings.getAddress()));
                listener.setPort(settings.getPort());
                listenThread = listener.listen();
                listenThread.setName("modbusServerListener");
            } catch (UnknownHostException e) {
                logger.error(e.getMessage());
            }
        }

    }

    @Deactivate
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

            EPrimaryTable primaryTable = EPrimaryTable
                    .getEnumfromString(serverAddress.substring(0, serverAddress.indexOf(":")));
            int modbusAddress = Integer
                    .parseInt(serverAddress.substring(serverAddress.indexOf(":") + 1, serverAddress.lastIndexOf(":")));
            String dataType = serverAddress.substring(serverAddress.lastIndexOf(":") + 1);

            switch (primaryTable) {
            case INPUT_REGISTERS:
                addInputRegisters(spi, modbusAddress, ValueType.valueOf(dataType), container.getChannel());
                break;
            case HOLDING_REGISTERS:
                addHoldingRegisters(spi, modbusAddress, ValueType.valueOf(dataType), container.getChannel());
                break;
            case COILS:
                // TODO: create for coils
                break;
            case DISCRETE_INPUTS:
                // TODO: create for discrete inputs
                break;
            default:
            }
        }

        startServer(spi);
    }

    private void addHoldingRegisters(SimpleProcessImage spi, int modbusAddress, ValueType valueType, Channel channel) {
        while (spi.getRegisterCount() <= modbusAddress + 4) {
            spi.addRegister(new SimpleRegister());
        }

        switch (valueType) {
        case DOUBLE:
            Register eightByteDoubleRegister3 = new LinkedMappingHoldingRegister(
                    new DoubleMappingInputRegister(channel, 6, 7), channel, null, valueType, 6, 7);
            Register eightByteDoubleRegister2 = new LinkedMappingHoldingRegister(
                    new DoubleMappingInputRegister(channel, 4, 5), channel,
                    (LinkedMappingHoldingRegister) eightByteDoubleRegister3, valueType, 4, 5);
            Register eightByteDoubleRegister1 = new LinkedMappingHoldingRegister(
                    new DoubleMappingInputRegister(channel, 2, 3), channel,
                    (LinkedMappingHoldingRegister) eightByteDoubleRegister2, valueType, 2, 3);
            Register eightByteDoubleRegister0 = new LinkedMappingHoldingRegister(
                    new DoubleMappingInputRegister(channel, 0, 1), channel,
                    (LinkedMappingHoldingRegister) eightByteDoubleRegister1, valueType, 0, 1);
            spi.setRegister(modbusAddress, eightByteDoubleRegister0);
            spi.setRegister(modbusAddress + 1, eightByteDoubleRegister1);
            spi.setRegister(modbusAddress + 2, eightByteDoubleRegister2);
            spi.setRegister(modbusAddress + 3, eightByteDoubleRegister3);
            break;
        case LONG:
            Register eightByteLongRegister3 = new LinkedMappingHoldingRegister(
                    new LongMappingInputRegister(channel, 6, 7), channel, null, valueType, 6, 7);
            Register eightByteLongRegister2 = new LinkedMappingHoldingRegister(
                    new LongMappingInputRegister(channel, 4, 5), channel,
                    (LinkedMappingHoldingRegister) eightByteLongRegister3, valueType, 4, 5);
            Register eightByteLongRegister1 = new LinkedMappingHoldingRegister(
                    new LongMappingInputRegister(channel, 2, 3), channel,
                    (LinkedMappingHoldingRegister) eightByteLongRegister2, valueType, 2, 3);
            Register eightByteLongRegister0 = new LinkedMappingHoldingRegister(
                    new LongMappingInputRegister(channel, 0, 1), channel,
                    (LinkedMappingHoldingRegister) eightByteLongRegister1, valueType, 0, 1);
            spi.setRegister(modbusAddress, eightByteLongRegister0);
            spi.setRegister(modbusAddress + 1, eightByteLongRegister1);
            spi.setRegister(modbusAddress + 2, eightByteLongRegister2);
            spi.setRegister(modbusAddress + 3, eightByteLongRegister3);
            break;
        case INTEGER:
            Register fourByteIntRegister1 = new LinkedMappingHoldingRegister(
                    new IntegerMappingInputRegister(channel, 2, 3), channel, null, valueType, 2, 3);
            Register fourByteIntRegister0 = new LinkedMappingHoldingRegister(
                    new IntegerMappingInputRegister(channel, 0, 1), channel,
                    (LinkedMappingHoldingRegister) fourByteIntRegister1, valueType, 0, 1);
            spi.setRegister(modbusAddress, fourByteIntRegister0);
            spi.setRegister(modbusAddress + 1, fourByteIntRegister1);
            break;
        case FLOAT:
            Register fourByteFloatRegister1 = new LinkedMappingHoldingRegister(
                    new FloatMappingInputRegister(channel, 2, 3), channel, null, valueType, 2, 3);
            Register fourByteFloatRegister0 = new LinkedMappingHoldingRegister(
                    new FloatMappingInputRegister(channel, 0, 1), channel,
                    (LinkedMappingHoldingRegister) fourByteFloatRegister1, valueType, 0, 1);
            spi.setRegister(modbusAddress, fourByteFloatRegister0);
            spi.setRegister(modbusAddress + 1, fourByteFloatRegister1);
            break;
        case SHORT:
            Register twoByteShortRegister = new LinkedMappingHoldingRegister(
                    new ShortMappingInputRegister(channel, 0, 1), channel, null, valueType, 0, 1);
            spi.setRegister(modbusAddress, twoByteShortRegister);
            break;
        case BOOLEAN:
            Register twoByteBooleanRegister = new LinkedMappingHoldingRegister(
                    new BooleanMappingInputRegister(channel, 0, 1), channel, null, valueType, 0, 1);
            spi.setRegister(modbusAddress, twoByteBooleanRegister);
            break;
        default:
            // TODO
        }
    }

    private void addInputRegisters(SimpleProcessImage spi, int modbusAddress, ValueType valueType, Channel channel) {
        while (spi.getInputRegisterCount() <= modbusAddress + 4) {
            spi.addInputRegister(new SimpleInputRegister());
        }

        switch (valueType) {
        case DOUBLE:
            for (int i = 0; i < 4; i++) {
                spi.setInputRegister(modbusAddress + i, new DoubleMappingInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case LONG:
            for (int i = 0; i < 4; i++) {
                spi.setInputRegister(modbusAddress + i, new LongMappingInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case INTEGER:
            for (int i = 0; i < 2; i++) {
                spi.setInputRegister(modbusAddress + i, new IntegerMappingInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case FLOAT:
            for (int i = 0; i < 2; i++) {
                spi.setInputRegister(modbusAddress + i, new FloatMappingInputRegister(channel, 2 * i, 2 * i + 1));
            }
            break;
        case SHORT:
            spi.setInputRegister(modbusAddress, new ShortMappingInputRegister(channel, 0, 1));
            break;
        case BOOLEAN:
            spi.setInputRegister(modbusAddress, new BooleanMappingInputRegister(channel, 0, 1));
            break;
        default:
            // TODO
        }
    }

    public enum EPrimaryTable {
        COILS,
        DISCRETE_INPUTS,
        INPUT_REGISTERS,
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
                throw new RuntimeException(
                        enumAsString + " is not supported. Use one of the following supported primary tables: "
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
