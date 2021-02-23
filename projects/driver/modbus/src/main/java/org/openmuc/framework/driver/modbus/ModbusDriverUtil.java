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
package org.openmuc.framework.driver.modbus;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.modbus.util.DatatypeConversion;
import org.openmuc.framework.driver.modbus.util.DatatypeConversion.EndianInput;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.ghgande.j2mod.modbus.util.ModbusUtil;

public class ModbusDriverUtil {

    private ModbusDriverUtil() {
    }

    public static Value getBitVectorsValue(BitVector bitVector) {

        Value readValue;
        if (bitVector.size() == 1) {

            readValue = new BooleanValue(bitVector.getBit(0)); // read single bit
        }
        else {
            readValue = new ByteArrayValue(bitVector.getBytes()); // read multiple bits
        }
        return readValue;
    }

    public static BitVector getBitVectorFromByteArray(Value value) {
        BitVector bv = new BitVector(value.asByteArray().length * 8);
        bv.setBytes(value.asByteArray());
        return bv;
    }

    /**
     * Converts the registers into the datatyp of the channel
     * 
     * @param registers
     *            input register array
     * @param datatype
     *            Edatatype
     * @return the corresponding Value Object
     */

    public static Value getRegistersValue(InputRegister[] registers, EDatatype datatype) {
        byte[] registerAsByteArray = inputRegisterToByteArray(registers);
        return getValueFromByteArray(registerAsByteArray, datatype);
    }

    public static Value getValueFromByteArray(byte[] registerAsByteArray, EDatatype datatype) {

        Value registerValue = null;

        switch (datatype) {
        // case INT8:
        // int int8 = DatatypeConversion.bytes_To_SignedInt8(registerAsByteArray);
        // registerValue = new IntValue(int8);
        // break;
        case SHORT:
        case INT16:
            registerValue = new ShortValue(ModbusUtil.registerToShort(registerAsByteArray));
            break;
        case INT32:
            registerValue = new IntValue(ModbusUtil.registersToInt(registerAsByteArray));

            break;
        // case INT64:
        // registerValue = new LongValue(ModbusUtil.registersToLong(registerAsByteArray));
        // break;

        // case UINT8:
        // // TODO might need both: big/little endian support in settings
        // int uint8 = DatatypeConversion.bytes_To_UnsignedInt8(registerAsByteArray, 0);
        // registerValue = new IntValue(uint8);
        // break;
        case UINT16:
            // TODO might need both: big/little endian support in settings
            // int uint16 = DatatypeConversion.bytes_To_UnsignedInt16(registerAsByteArray,
            // EndianInput.BYTES_ARE_BIG_ENDIAN);
            // registerValue = new IntValue(uint16);
            registerValue = new IntValue(ModbusUtil.registerToUnsignedShort(registerAsByteArray));
            break;
        case UINT32:
            // TODO might need both: big/little endian support in settings
            long uint32 = DatatypeConversion.bytes_To_UnsignedInt32(registerAsByteArray,
                    EndianInput.BYTES_ARE_BIG_ENDIAN);
            registerValue = new LongValue(uint32);
            break;
        case FLOAT:
            registerValue = new FloatValue(ModbusUtil.registersToFloat(registerAsByteArray));
            break;
        case DOUBLE:
            registerValue = new DoubleValue(ModbusUtil.registersToDouble(registerAsByteArray));
            break;
        case LONG:
            registerValue = new LongValue(ModbusUtil.registersToLong(registerAsByteArray));
            break;
        case BYTEARRAY:
            registerValue = new ByteArrayValue(registerAsByteArray);
            break;
        case BYTEARRAYLONG:
            registerValue = new LongValue(
                    DatatypeConversion.bytes_To_SignedInt64(registerAsByteArray, EndianInput.BYTES_ARE_LITTLE_ENDIAN));
            break;
        // case BYTE_HIGH:
        // registerValue = new IntValue(registerAsByteArray[1] & 0xFF);
        // break;
        // case BYTE_LOW:
        // registerValue = new IntValue(registerAsByteArray[0] & 0xFF);
        // break;
        default:
            throw new RuntimeException("Datatype " + datatype.toString() + " not supported yet");
        }

        return registerValue;
    }

    public static Register[] valueToRegisters(Value value, EDatatype datatype) {

        Register[] registers;

        switch (datatype) {

        case SHORT:
        case INT16:
            registers = byteArrayToRegister(ModbusUtil.shortToRegister(value.asShort()));
            break;
        case INT32:
            registers = byteArrayToRegister(ModbusUtil.intToRegisters(value.asInt()));
            break;
        case UINT16:
            byte[] registerBytesUint16 = DatatypeConversion.unsingedInt16_To_Bytes(value.asInt(),
                    DatatypeConversion.EndianOutput.BYTES_AS_BIG_ENDIAN);
            registers = byteArrayToRegister(registerBytesUint16);
            break;
        case UINT32:
            byte[] registerBytesUint32 = DatatypeConversion.unsingedInt32_To_Bytes(value.asLong(),
                    DatatypeConversion.EndianOutput.BYTES_AS_BIG_ENDIAN);
            registers = byteArrayToRegister(registerBytesUint32);
            break;
        case DOUBLE:
            registers = byteArrayToRegister(ModbusUtil.doubleToRegisters(value.asDouble()));
            break;
        case FLOAT:
            registers = byteArrayToRegister(ModbusUtil.floatToRegisters(value.asFloat()));
            break;
        case LONG:
            registers = byteArrayToRegister(ModbusUtil.longToRegisters(value.asLong()));
            break;
        case BYTEARRAY:
            registers = byteArrayToRegister(value.asByteArray());
            break;
        // case BYTE_HIGH:
        // case BYTE_LOW:
        default:
            throw new RuntimeException("Datatype " + datatype.toString() + " not supported yet");
        }

        return registers;
    }

    /**
     * Converts an array of input registers into a byte array
     * 
     * @param inputRegister
     *            inputRegister array
     * @return the InputRegister[] as byte[]
     */
    private static byte[] inputRegisterToByteArray(InputRegister[] inputRegister) {
        byte[] registerAsBytes = new byte[inputRegister.length * 2]; // one register = 2 bytes
        byte inputRegisterByteLength = 2;
        for (int i = 0; i < inputRegister.length; i++) {
            System.arraycopy(inputRegister[i].toBytes(), 0, registerAsBytes, i * inputRegisterByteLength,
                    inputRegisterByteLength);
        }
        return registerAsBytes;
    }

    // TODO check byte order e.g. is an Integer!
    // TODO only works for even byteArray.length!
    private static Register[] byteArrayToRegister(byte[] byteArray) throws RuntimeException {

        // TODO byteArray might has a odd number of bytes...
        SimpleRegister[] register;

        if (byteArray.length % 2 == 0) {
            register = new SimpleRegister[byteArray.length / 2];
            int j = 0;
            // for (int i = 0; i < byteArray.length; i++) {
            for (int i = 0; i < byteArray.length / 2; i++) {
                register[i] = new SimpleRegister(byteArray[j], byteArray[j + 1]);
                j = j + 2;
            }
        }
        else {
            throw new RuntimeException("conversion from byteArray to Register is not working for odd number of bytes");
        }
        return register;
    }
}
