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
package org.openmuc.framework.driver.modbus.util;

/**
 * Since Java supports only signed data types, this class provides methods to convert byte arrays in signed and unsigned
 * integers and vice versa (UNIT8, INT8, UNIT16, INT16, UINT32, INT32). These conversions are usually needed when
 * receiving messages from a hardware or sending messages to a hardware respectively.
 * 
 */
public class DatatypeConversion {

    private static final int INT64_BYTE_LENGTH = 8;
    private static final int INT32_BYTE_LENGTH = 4;
    private static final int INT16_BYTE_LENGTH = 2;
    private static final int INT8_BYTE_LENGTH = 1;

    public static enum ByteSelect {
        LOW_BYTE,
        HIGH_BYTE
    }

    public static enum EndianInput {
        BYTES_ARE_LITTLE_ENDIAN,
        BYTES_ARE_BIG_ENDIAN
    }

    public static enum EndianOutput {
        BYTES_AS_LITTLE_ENDIAN,
        BYTES_AS_BIG_ENDIAN
    }

    /**
     * Reverses the byte order. If the given byte[] are in big endian order you will get little endian order and vice
     * versa
     * <p>
     * Example:<br>
     * input: bytes = {0x0A, 0x0B, 0x0C, 0x0D}<br>
     * output: bytes = {0x0D, 0x0C, 0x0B, 0x0A}<br>
     * 
     * @param bytes
     *            byte[] to reverse
     */
    public static void reverseByteOrder(byte[] bytes) {
        int indexLength = bytes.length - 1;
        int halfLength = bytes.length / 2;
        for (int i = 0; i < halfLength; i++) {
            int index = indexLength - i;
            byte temp = bytes[i];
            bytes[i] = bytes[index];
            bytes[index] = temp;
        }
    }

    /**
     * Reverses the byte order. <br>
     * Equal to reverseByteOrder Method but it doesn't change the input bytes. Method is working on a copy of input
     * bytes so it does
     * 
     * @param bytes
     *            byte[] to reverse
     * @return reversed bytes
     */
    public static byte[] reverseByteOrderNewArray(byte[] bytes) {
        byte[] reversedBytes = bytes.clone();
        reverseByteOrder(reversedBytes);
        return reversedBytes;
    }

    /**
     * Converts bytes to signed Int64
     * 
     * @param bytes
     *            8 bytes where byte[0] is most significant byte and byte[7] is the least significant byte
     * @param endian
     *            endian byte order
     * @return bytes as singed int 64
     */
    public static long bytes_To_SignedInt64(byte[] bytes, EndianInput endian) {
        if (bytes.length > 0 && bytes.length <= INT64_BYTE_LENGTH) {
            long returnValue = 0;
            int length = bytes.length - 1;

            if (endian.equals(EndianInput.BYTES_ARE_LITTLE_ENDIAN)) {
                reverseByteOrder(bytes);
            }

            for (int i = 0; i <= length; ++i) {
                int shift = length - i << 3;
                returnValue |= (long) (bytes[i] & 0xff) << shift;
            }
            return returnValue;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to convert bytes due to wrong number of bytes. Minimum 1 byte, maximum " + INT64_BYTE_LENGTH
                            + " bytes needed for conversion.");
        }
    }

    /**
     * Converts signed Int64 (long) to 8 bytes
     * 
     * @param value
     *            signed Int64
     * @param endian
     *            endian byte order
     * @return 8 bytes where the most significant byte is byte[0] and the least significant byte is byte[7]
     */
    public static byte[] singedInt64_To_Bytes(long value, EndianOutput endian) {
        byte[] bytes = new byte[INT64_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0xFF00000000000000L) >> 56);
        bytes[1] = (byte) ((value & 0x00FF000000000000L) >> 48);
        bytes[2] = (byte) ((value & 0x0000FF0000000000L) >> 40);
        bytes[3] = (byte) ((value & 0x000000FF00000000L) >> 32);
        bytes[4] = (byte) ((value & 0x00000000FF000000L) >> 24);
        bytes[5] = (byte) ((value & 0x0000000000FF0000L) >> 16);
        bytes[6] = (byte) ((value & 0x000000000000FF00L) >> 8);
        bytes[7] = (byte) (value & 0x00000000000000FFL);

        if (endian.equals(EndianOutput.BYTES_AS_LITTLE_ENDIAN)) {
            reverseByteOrder(bytes);
        }

        return bytes;
    }

    /**
     * Converts bytes to signed Int32
     * 
     * @param bytes
     *            4 bytes where byte[0] is most significant byte and byte[3] is the least significant byte
     * @param endian
     *            endian byte order
     * @return bytes as signed int 32
     */
    public static int bytes_To_SignedInt32(byte[] bytes, EndianInput endian) {
        if (bytes.length == INT32_BYTE_LENGTH) {
            int returnValue = 0;
            int length = bytes.length - 1;

            if (endian.equals(EndianInput.BYTES_ARE_LITTLE_ENDIAN)) {
                reverseByteOrder(bytes);
            }

            for (int i = 0; i <= length; ++i) {
                int shift = length - i << 3;
                returnValue |= (long) (bytes[i] & 0xff) << shift;
            }
            return returnValue;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to convert bytes due to wrong number of bytes. Minimum 1 byte, maximum " + INT32_BYTE_LENGTH
                            + " bytes needed for conversion.");
        }
    }

    /**
     * Converts bytes to unsigned Int32
     * 
     * @param bytes
     *            4 bytes where byte[0] is most significant byte and byte[3] least significant byte
     * @param endian
     *            endian byte order
     * @return unsigned Int32 as long
     */
    public static long bytes_To_UnsignedInt32(byte[] bytes, EndianInput endian) {
        if (bytes.length == INT32_BYTE_LENGTH) {
            if (endian.equals(EndianInput.BYTES_ARE_LITTLE_ENDIAN)) {
                reverseByteOrder(bytes);
            }

            int firstbyte = 0x000000FF & (bytes[0]);
            int secondByte = 0x000000FF & (bytes[1]);
            int thirdByte = 0x000000FF & (bytes[2]);
            int forthByte = 0x000000FF & (bytes[3]);
            return (firstbyte << 24 | secondByte << 16 | thirdByte << 8 | forthByte) & 0xFFFFFFFFL;
        }
        else {
            throw new IllegalArgumentException("Unable to convert bytes due to wrong number of bytes. "
                    + INT32_BYTE_LENGTH + " bytes needed for conversion.");
        }
    }

    /**
     * Converts unsigned Int32 to 4 bytes
     * 
     * @param value
     *            unsigned Int32 (long)
     * @param endian
     *            endian byte order
     * @return 4 bytes where the most significant byte is byte[0] and the least significant byte is byte[3]
     */
    public static byte[] unsingedInt32_To_Bytes(long value, EndianOutput endian) {

        if (value < 0) {
            throw new IllegalArgumentException("Invalid value: " + value + " Only positive values are allowed!");
        }

        byte[] bytes = new byte[INT32_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0xFF000000L) >> 24);
        bytes[1] = (byte) ((value & 0x00FF0000L) >> 16);
        bytes[2] = (byte) ((value & 0x0000FF00L) >> 8);
        bytes[3] = (byte) (value & 0x000000FFL);

        if (endian.equals(EndianOutput.BYTES_AS_LITTLE_ENDIAN)) {
            reverseByteOrder(bytes);
        }

        return bytes;
    }

    /**
     * Converts signed Int32 to 4 bytes
     * 
     * @param value
     *            signed Int32
     * @param endian
     *            endian byte order
     * @return 4 bytes where the most significant byte is byte[0] and the least significant byte is byte[3]
     */
    public static byte[] singedInt32_To_Bytes(int value, EndianOutput endian) {

        byte[] bytes = new byte[INT32_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0xFF000000L) >> 24);
        bytes[1] = (byte) ((value & 0x00FF0000L) >> 16);
        bytes[2] = (byte) ((value & 0x0000FF00L) >> 8);
        bytes[3] = (byte) (value & 0x000000FFL);

        if (endian.equals(EndianOutput.BYTES_AS_LITTLE_ENDIAN)) {
            reverseByteOrder(bytes);
        }

        return bytes;
    }

    /**
     * Converts bytes to signed Int16
     * 
     * @param bytes
     *            2 bytes where byte[0] is most significant byte and byte[1] is the least significant byte
     * @param endian
     *            endian byte order
     * @return signed Int16
     */
    public static int bytes_To_SignedInt16(byte[] bytes, EndianInput endian) {
        if (bytes.length == INT16_BYTE_LENGTH) {
            short returnValue = 0;
            int length = bytes.length - 1;

            if (endian.equals(EndianInput.BYTES_ARE_LITTLE_ENDIAN)) {
                reverseByteOrder(bytes);
            }

            for (int i = 0; i <= length; ++i) {
                int shift = length - i << 3;
                returnValue |= (long) (bytes[i] & 0xff) << shift;
            }
            return returnValue;
        }
        else {
            throw new IllegalArgumentException("Unable to convert bytes due to wrong number of bytes. "
                    + INT16_BYTE_LENGTH + " bytes needed for conversion.");
        }
    }

    /**
     * Converts bytes to unsigned Int16
     * 
     * @param bytes
     *            2 bytes where byte[0] is most significant byte and byte[1] least significant byte
     * @param endian
     *            endian byte order
     * @return unsigned Int16
     */
    public static int bytes_To_UnsignedInt16(byte[] bytes, EndianInput endian) {
        if (bytes.length == INT16_BYTE_LENGTH) {
            if (endian.equals(EndianInput.BYTES_ARE_LITTLE_ENDIAN)) {
                reverseByteOrder(bytes);
            }

            int firstbyte = 0x000000FF & (byte) 0x00;
            int secondByte = 0x000000FF & (byte) 0x00;
            int thirdByte = 0x000000FF & (bytes[0]);
            int forthByte = 0x000000FF & (bytes[1]);
            return (firstbyte << 24 | secondByte << 16 | thirdByte << 8 | forthByte) & 0xFFFFFFFF;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to convert bytes due to wrong number of bytes. Minimum 1, maximum " + INT16_BYTE_LENGTH
                            + " bytes needed for conversion.");
        }
    }

    /**
     * Converts unsigned Int16 to 2 bytes
     * 
     * @param value
     *            unsigned Int16
     * @param endian
     *            endian byte order
     * @return 2 bytes where the most significant byte is byte[0] and the least significant byte is byte[1]
     */
    public static byte[] unsingedInt16_To_Bytes(int value, EndianOutput endian) {

        if (value < 0) {
            throw new IllegalArgumentException("Invalid value: " + value + " Only positive values are allowed!");
        }

        byte[] bytes = new byte[INT16_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0x0000FF00) >> 8);
        bytes[1] = (byte) ((value & 0x000000FF));

        if (endian.equals(EndianOutput.BYTES_AS_LITTLE_ENDIAN)) {
            reverseByteOrder(bytes);
        }

        return bytes;
    }

    /**
     * Converts signed Int16 to 2 bytes
     * 
     * @param value
     *            signed Int16
     * @param endian
     *            endian byte order
     * @return 2 bytes where the most significant byte is byte[0] and the least significant byte is byte[1]
     */
    public static byte[] singedInt16_To_Bytes(int value, EndianOutput endian) {

        byte[] bytes = new byte[INT16_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0x0000FF00) >> 8);
        bytes[1] = (byte) ((value & 0x000000FF));

        if (endian.equals(EndianOutput.BYTES_AS_LITTLE_ENDIAN)) {
            reverseByteOrder(bytes);
        }

        return bytes;
    }

    /**
     * Converts bytes to signed Int8
     * 
     * @param bytes
     *            1 byte
     * @return signed Int8
     */
    public static int bytes_To_SignedInt8(byte[] bytes) {
        if (bytes.length == INT8_BYTE_LENGTH) {
            byte returnValue = 0;
            int length = bytes.length - 1;

            for (int i = 0; i <= length; ++i) {
                int shift = length - i << 3;
                returnValue |= (long) (bytes[i] & 0xff) << shift;
            }
            return returnValue;
        }
        else {
            throw new IllegalArgumentException("Unable to convert bytes due to wrong number of bytes. "
                    + INT8_BYTE_LENGTH + " bytes needed for conversion.");
        }
    }

    /**
     * Converts the specified byte of an byte array to unsigned int
     * 
     * @param data
     *            byte array which contains
     * @param index
     *            index of the byte which should be returned as unsigned int8. Index can be used for little and big
     *            endian support.
     * @return bytes as unsigned int 8
     */
    public static int bytes_To_UnsignedInt8(byte[] data, int index) {

        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index. Index must be >= 0");
        }

        if (index >= data.length) {
            throw new IndexOutOfBoundsException("Negative index. Index must be >= 0");
        }

        return bytes_To_UnsignedInt8(data[index]);

    }

    /**
     * Converts a single byte to unsigned Int8
     * 
     * @param singleByte
     *            byte to convert
     * 
     * @return unsigned Int8
     */
    public static int bytes_To_UnsignedInt8(byte singleByte) {
        int value = 0x000000FF & (singleByte);
        return value;
    }

    /**
     * Converts unsigned Int8 to 1 byte
     * 
     * @param value
     *            unsigned Int8
     * @return 1 byte
     */
    public static byte[] unsingedInt8_To_Bytes(int value) {

        if (value < 0) {
            throw new IllegalArgumentException("Invalid value: " + value + " Only positive values are allowed!");
        }

        byte[] bytes = new byte[INT8_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0x000000FF));
        return bytes;
    }

    /**
     * Converts signed Int8 to 1 bytes
     * 
     * @param value
     *            signed Int8
     * @return 1 byte
     */
    public static byte[] singedInt8_To_Bytes(int value) {

        byte[] bytes = new byte[INT8_BYTE_LENGTH];
        bytes[0] = (byte) ((value & 0x000000FF));

        return bytes;
    }

}
