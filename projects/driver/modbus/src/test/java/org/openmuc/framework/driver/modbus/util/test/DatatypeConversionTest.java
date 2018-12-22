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
package org.openmuc.framework.driver.modbus.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.openmuc.framework.driver.modbus.util.DatatypeConversion;
import org.openmuc.framework.driver.modbus.util.DatatypeConversion.EndianInput;
import org.openmuc.framework.driver.modbus.util.DatatypeConversion.EndianOutput;

/**
 * This test case test the datatype conversion. It covers tests from datatype to byte[] and vice versa
 * 
 * TODO: Currently not all conversions are tested
 */
public class DatatypeConversionTest {

    // BE = BigEndian;
    // LE = LittleEndian

    byte[] bytes8_Value_MaxPositive_BE = DatatypeConverter.parseHexBinary("7FFFFFFFFFFFFFFF");
    byte[] bytes8_Value_2_BE = DatatypeConverter.parseHexBinary("0000000000000002");
    byte[] bytes8_Value_1_BE = DatatypeConverter.parseHexBinary("0000000000000001");
    byte[] bytes8_Value_0_BE = DatatypeConverter.parseHexBinary("0000000000000000");
    byte[] bytes8_Value_Minus_1_BE = DatatypeConverter.parseHexBinary("FFFFFFFFFFFFFFFF");
    byte[] bytes8_Value_Minus_2_BE = DatatypeConverter.parseHexBinary("FFFFFFFFFFFFFFFE");
    byte[] bytes8_Value_MaxNegative_BE = DatatypeConverter.parseHexBinary("8000000000000000");

    byte[] bytes4_Value_MaxPositive_BE = DatatypeConverter.parseHexBinary("7FFFFFFF");
    byte[] bytes4_Value_2_BE = DatatypeConverter.parseHexBinary("00000002");
    byte[] bytes4_Value_1_BE = DatatypeConverter.parseHexBinary("00000001");
    byte[] bytes4_Value_0_BE = DatatypeConverter.parseHexBinary("00000000");
    byte[] bytes4_Value_Minus_1_BE = DatatypeConverter.parseHexBinary("FFFFFFFF");
    byte[] bytes4_Value_Minus_2_BE = DatatypeConverter.parseHexBinary("FFFFFFFE");
    byte[] bytes4_Value_MaxNegative_BE = DatatypeConverter.parseHexBinary("80000000");

    byte[] bytes2_Value_MaxPositive_BE = DatatypeConverter.parseHexBinary("7FFF");
    byte[] bytes2_Value_2_BE = DatatypeConverter.parseHexBinary("0002");
    byte[] bytes2_Value_1_BE = DatatypeConverter.parseHexBinary("0001");
    byte[] bytes2_Value_0_BE = DatatypeConverter.parseHexBinary("0000");
    byte[] bytes2_Value_Minus_1_BE = DatatypeConverter.parseHexBinary("FFFF");
    byte[] bytes2_Value_Minus_2_BE = DatatypeConverter.parseHexBinary("FFFE");
    byte[] bytes2_Value_MaxNegative_BE = DatatypeConverter.parseHexBinary("8000");

    byte[] bytes8_Value_MaxPositive_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_MaxPositive_BE);
    byte[] bytes8_Value_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_2_BE);
    byte[] bytes8_Value_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_1_BE);
    byte[] bytes8_Value_0_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_0_BE);
    byte[] bytes8_Value_Minus_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_Minus_1_BE);
    byte[] bytes8_Value_Minus_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_Minus_2_BE);
    byte[] bytes8_Value_MaxNegative_LE = DatatypeConversion.reverseByteOrderNewArray(bytes8_Value_MaxNegative_BE);

    byte[] bytes4_Value_MaxPositive_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_MaxPositive_BE);
    byte[] bytes4_Value_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_2_BE);
    byte[] bytes4_Value_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_1_BE);
    byte[] bytes4_Value_0_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_0_BE);
    byte[] bytes4_Value_Minus_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_Minus_1_BE);
    byte[] bytes4_Value_Minus_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_Minus_2_BE);
    byte[] bytes4_Value_MaxNegative_LE = DatatypeConversion.reverseByteOrderNewArray(bytes4_Value_MaxNegative_BE);

    byte[] bytes2_Value_MaxPositive_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_MaxPositive_BE);
    byte[] bytes2_Value_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_2_BE);
    byte[] bytes2_Value_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_1_BE);
    byte[] bytes2_Value_0_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_0_BE);
    byte[] bytes2_Value_Minus_1_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_Minus_1_BE);
    byte[] bytes2_Value_Minus_2_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_Minus_2_BE);
    byte[] bytes2_Value_MaxNegative_LE = DatatypeConversion.reverseByteOrderNewArray(bytes2_Value_MaxNegative_BE);

    byte[] bytes1_Value_MaxPositive = new byte[] { (byte) 0x7F };
    byte[] bytes1_Value_2 = new byte[] { (byte) 0x02 };
    byte[] bytes1_Value_1 = new byte[] { (byte) 0x01 };
    byte[] bytes1_Value_0 = new byte[] { (byte) 0x00 };
    byte[] bytes1_Value_Minus_1 = new byte[] { (byte) 0xFF };
    byte[] bytes1_Value_Minus_2 = new byte[] { (byte) 0xFE };
    byte[] bytes1_Value_MaxNegative = new byte[] { (byte) 0x80, };

    final long MAX_UNSIGNED_INT32 = 4294967295L;
    final int MAX_UNSIGNED_INT16 = 65535;

    final int MAX_SIGNED_INT16 = 32767;
    final int MIN_SIGNED_INT16 = -32768;

    final int MAX_SIGNED_INT8 = 127;
    final int MIN_SIGNED_INT8 = -128;

    @Test
    public void test_reverseByteOrder() {
        byte[] array1 = DatatypeConverter.parseHexBinary("00000002");
        byte[] array1Reverse = DatatypeConverter.parseHexBinary("02000000");
        DatatypeConversion.reverseByteOrder(array1);
        assertTrue(Arrays.equals(array1, array1Reverse));

        byte[] array2 = DatatypeConverter.parseHexBinary("00000002");
        byte[] array2Reverse = DatatypeConverter.parseHexBinary("02000000");
        assertTrue(Arrays.equals(array2Reverse, DatatypeConversion.reverseByteOrderNewArray(array2)));
    }

    @Test
    public void test_bytes_To_SignedInt64_BigEndian() {

        long signedInt64;

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_MaxPositive_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(Long.MAX_VALUE, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_2_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(2, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_1_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(1, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_0_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(0, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_Minus_1_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-1, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_Minus_2_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-2, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_MaxNegative_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(Long.MIN_VALUE, signedInt64);

    }

    public void test_bytes_To_SignedInt64_LittleEndian() {

        long signedInt64;

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_MaxPositive_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(Long.MAX_VALUE, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_2_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(2, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_1_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(1, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_0_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(0, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_Minus_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-1, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_Minus_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-2, signedInt64);

        signedInt64 = DatatypeConversion.bytes_To_SignedInt64(bytes8_Value_MaxNegative_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(Long.MIN_VALUE, signedInt64);

    }

    @Test
    public void test_signedInt64_To_Bytes_BigEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt64_To_Bytes(Long.MAX_VALUE, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_MaxPositive_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(2L, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_2_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(1L, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_1_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(0L, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_0_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(-1L, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_Minus_1_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(-2L, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_Minus_2_BE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(Long.MIN_VALUE, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_MaxNegative_BE));
    }

    @Test
    public void test_signedInt64_To_Bytes_LittleEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt64_To_Bytes(Long.MAX_VALUE, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_MaxPositive_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(2L, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_2_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(1L, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_1_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(0L, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_0_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(-1L, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_Minus_1_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(-2L, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_Minus_2_LE));

        bytes = DatatypeConversion.singedInt64_To_Bytes(Long.MIN_VALUE, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes8_Value_MaxNegative_LE));
    }

    @Test
    public void test_bytes_To_SignedInt32_BigEndian() {

        int signedInt32;

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_MaxPositive_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(Integer.MAX_VALUE, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_2_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(2, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_1_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(1, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_0_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(0, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_Minus_1_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-1, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_Minus_2_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-2, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_MaxNegative_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(Integer.MIN_VALUE, signedInt32);

    }

    @Test
    public void test_bytes_To_SignedInt32_LittelEndian() {

        int signedInt32;

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_MaxPositive_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(Integer.MAX_VALUE, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_2_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(2, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_1_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(1, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_0_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(0, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_Minus_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-1, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_Minus_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-2, signedInt32);

        signedInt32 = DatatypeConversion.bytes_To_SignedInt32(bytes4_Value_MaxNegative_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(Integer.MIN_VALUE, signedInt32);

    }

    @Test
    public void test_bytes_To_UnignedInt32_BigEndian() {

        long unsignedInt32;

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_MaxPositive_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x7FFFFFFF"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_2_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x00000002"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_1_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x00000001"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_0_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x00000000"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_Minus_1_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0xFFFFFFFF"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_Minus_2_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0xFFFFFFFE"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_MaxNegative_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x80000000"), unsignedInt32);

    }

    @Test
    public void test_bytes_To_UnignedInt32_LittleEndian() {

        long unsignedInt32;

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_MaxPositive_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x7FFFFFFF"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x00000002"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x00000001"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_0_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x00000000"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_Minus_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0xFFFFFFFF"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_Minus_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0xFFFFFFFE"), unsignedInt32);

        unsignedInt32 = DatatypeConversion.bytes_To_UnsignedInt32(bytes4_Value_MaxNegative_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x80000000"), unsignedInt32);

    }

    @Test
    public void test_signedInt32_To_Bytes_BigEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt32_To_Bytes(Integer.MAX_VALUE, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_MaxPositive_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_2_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_1_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(0, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_0_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(-1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_Minus_1_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(-2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_Minus_2_BE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(Integer.MIN_VALUE, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_MaxNegative_BE));
    }

    @Test
    public void test_signedInt32_To_Bytes_LittleEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt32_To_Bytes(Integer.MAX_VALUE, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_MaxPositive_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_2_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_1_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(0, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_0_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(-1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_Minus_1_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(-2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_Minus_2_LE));

        bytes = DatatypeConversion.singedInt32_To_Bytes(Integer.MIN_VALUE, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_MaxNegative_LE));
    }

    @Test
    public void test_unsignedInt32_To_Bytes_BigEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(MAX_UNSIGNED_INT32, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, DatatypeConverter.parseHexBinary("FFFFFFFF")));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_2_BE));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_1_BE));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(0, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_0_BE));
    }

    @Test
    public void test_unsignedInt32_To_Bytes_LittleEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(MAX_UNSIGNED_INT32, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, DatatypeConverter.parseHexBinary("FFFFFFFF")));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_2_LE));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_1_LE));

        bytes = DatatypeConversion.unsingedInt32_To_Bytes(0, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes4_Value_0_LE));
    }

    @Test
    public void test_bytes_To_SignedInt16_BigEndian() {

        int signedInt16;

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_MaxPositive_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(32767, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_2_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(2, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_1_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(1, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_0_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(0, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_Minus_1_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-1, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_Minus_2_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-2, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_MaxNegative_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals(-32768, signedInt16);

    }

    @Test
    public void test_bytes_To_SignedInt16_LittleEndian() {

        int signedInt16;

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_MaxPositive_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(32767, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_2_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(2, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_1_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(1, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_0_LE, EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(0, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_Minus_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-1, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_Minus_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-2, signedInt16);

        signedInt16 = DatatypeConversion.bytes_To_SignedInt16(bytes2_Value_MaxNegative_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals(-32768, signedInt16);

    }

    @Test
    public void test_bytes_To_UnignedInt16_BigEndian() {

        long unsignedInt16;

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_MaxPositive_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x7FFF"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_2_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x0002"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_1_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x0001"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_0_BE, EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x0000"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_Minus_1_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0xFFFF"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_Minus_2_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0xFFFE"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_MaxNegative_BE,
                EndianInput.BYTES_ARE_BIG_ENDIAN);
        assertEquals((long) Long.decode("0x8000"), unsignedInt16);

    }

    @Test
    public void test_bytes_To_UnignedInt16_LittleEndian() {

        long unsignedInt16;

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_MaxPositive_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x7FFF"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x0002"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x0001"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_0_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x0000"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_Minus_1_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0xFFFF"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_Minus_2_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0xFFFE"), unsignedInt16);

        unsignedInt16 = DatatypeConversion.bytes_To_UnsignedInt16(bytes2_Value_MaxNegative_LE,
                EndianInput.BYTES_ARE_LITTLE_ENDIAN);
        assertEquals((long) Long.decode("0x8000"), unsignedInt16);

    }

    @Test
    public void test_signedInt16_To_Bytes_BigEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt16_To_Bytes(MAX_SIGNED_INT16, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_MaxPositive_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_2_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_1_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(0, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_0_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(-1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_Minus_1_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(-2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_Minus_2_BE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(MIN_SIGNED_INT16, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_MaxNegative_BE));
    }

    @Test
    public void test_signedInt16_To_Bytes_LittleEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt16_To_Bytes(MAX_SIGNED_INT16, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_MaxPositive_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_2_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_1_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(0, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_0_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(-1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_Minus_1_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(-2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_Minus_2_LE));

        bytes = DatatypeConversion.singedInt16_To_Bytes(MIN_SIGNED_INT16, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_MaxNegative_LE));
    }

    @Test
    public void test_unsignedInt16_To_Bytes_BigEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(MAX_UNSIGNED_INT16, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, DatatypeConverter.parseHexBinary("FFFF")));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(2, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_2_BE));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(1, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_1_BE));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(0, EndianOutput.BYTES_AS_BIG_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_0_BE));
    }

    @Test
    public void test_unsignedInt16_To_Bytes_LittleEndian() {

        byte[] bytes;

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(MAX_UNSIGNED_INT16, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, DatatypeConverter.parseHexBinary("FFFF")));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(2, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_2_LE));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(1, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_1_LE));

        bytes = DatatypeConversion.unsingedInt16_To_Bytes(0, EndianOutput.BYTES_AS_LITTLE_ENDIAN);
        assertTrue(Arrays.equals(bytes, bytes2_Value_0_LE));
    }

    @Test
    public void test_bytes_To_SignedInt8() {

        int signedInt8;

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_MaxPositive);
        assertEquals(127, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_2);
        assertEquals(2, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_1);
        assertEquals(1, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_0);
        assertEquals(0, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_Minus_1);
        assertEquals(-1, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_Minus_2);
        assertEquals(-2, signedInt8);

        signedInt8 = DatatypeConversion.bytes_To_SignedInt8(bytes1_Value_MaxNegative);
        assertEquals(-128, signedInt8);

    }

    @Test
    public void test_bytes_To_UnignedInt8() {

        long unsignedInt8;

        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_MaxPositive);
        // assertEquals((long) Long.decode("0x7F"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_2);
        // assertEquals((long) Long.decode("0x02"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_1);
        // assertEquals((long) Long.decode("0x01"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_0);
        // assertEquals((long) Long.decode("0x00"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_Minus_1);
        // assertEquals((long) Long.decode("0xFF"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_Minus_2);
        // assertEquals((long) Long.decode("0xFE"), unsignedInt8);
        //
        // unsignedInt8 = DatatypeConversion.bytes_To_UnsignedInt8(bytes1_Value_MaxNegative);
        // assertEquals((long) Long.decode("0x80"), unsignedInt8);

    }

    @Test
    public void test_signedInt8_To_Bytes() {

        byte[] bytes;

        bytes = DatatypeConversion.singedInt8_To_Bytes(MAX_SIGNED_INT8);
        assertTrue(Arrays.equals(bytes, bytes1_Value_MaxPositive));

        bytes = DatatypeConversion.singedInt8_To_Bytes(2);
        assertTrue(Arrays.equals(bytes, bytes1_Value_2));

        bytes = DatatypeConversion.singedInt8_To_Bytes(1);
        assertTrue(Arrays.equals(bytes, bytes1_Value_1));

        bytes = DatatypeConversion.singedInt8_To_Bytes(0);
        assertTrue(Arrays.equals(bytes, bytes1_Value_0));

        bytes = DatatypeConversion.singedInt8_To_Bytes(-1);
        assertTrue(Arrays.equals(bytes, bytes1_Value_Minus_1));

        bytes = DatatypeConversion.singedInt8_To_Bytes(-2);
        assertTrue(Arrays.equals(bytes, bytes1_Value_Minus_2));

        bytes = DatatypeConversion.singedInt8_To_Bytes(MIN_SIGNED_INT8);
        assertTrue(Arrays.equals(bytes, bytes1_Value_MaxNegative));
    }

    @Test
    public void test_unsignedInt8_To_Bytes() {

        final int UNSIGNED_INT8_MAX = 255;
        byte[] MAX_UNSINGND_INT8_BYTE = new byte[] { (byte) 0xFF };

        byte[] bytes;

        bytes = DatatypeConversion.unsingedInt8_To_Bytes(UNSIGNED_INT8_MAX);
        assertTrue(Arrays.equals(bytes, MAX_UNSINGND_INT8_BYTE));

        bytes = DatatypeConversion.unsingedInt8_To_Bytes(2);
        assertTrue(Arrays.equals(bytes, bytes1_Value_2));

        bytes = DatatypeConversion.unsingedInt8_To_Bytes(1);
        assertTrue(Arrays.equals(bytes, bytes1_Value_1));

        bytes = DatatypeConversion.unsingedInt8_To_Bytes(0);
        assertTrue(Arrays.equals(bytes, bytes1_Value_0));
    }

}
