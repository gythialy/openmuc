/*
 * Copyright 2011-2022 Fraunhofer ISE
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
package org.openmuc.framework.driver.mbus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DriverConnection.class)
public class DriverConnectionTest {

    private final int delay = 100; // in ms

    private final Map<String, ConnectionInterface> interfaces = new HashMap<>();
    private static final byte[] NZR_ANSWER = { 104, 50, 50, 104, 8, 5, 114, 8, 6, 16, 48, 82, 59, 1, 2, 2, 0, 0, 0, 4,
            3, -25, 37, 0, 0, 4, -125, 127, -25, 37, 0, 0, 2, -3, 72, 54, 9, 2, -3, 91, 0, 0, 2, 43, 0, 0, 12, 120, 8,
            6, 16, 48, 15, 63, -79, 22 };
    private static final byte[] SIEMENS_UH50_ANSWER = { 0x68, (byte) 0xf8, (byte) 0xf8, 0x68, 0x8, (byte) 100, 0x72,
            0x74, (byte) 0x97, 0x32, 0x67, (byte) 0xa7, 0x32, 0x4, 0x4, 0x0, 0x0, 0x0, 0x0, 0x9, 0x74, 0x4, 0x9, 0x70,
            0x4, 0x0c, 0x6, 0x44, 0x5, 0x5, 0x0, 0x0c, 0x14, 0x69, 0x37, 0x32, 0x0, 0x0b, 0x2d, 0x71, 0x0, 0x0, 0x0b,
            0x3b, 0x50, 0x13, 0x0, 0x0a, 0x5b, 0x43, 0x0, 0x0a, 0x5f, 0x39, 0x0, 0x0a, 0x62, 0x46, 0x0, 0x4c, 0x14, 0x0,
            0x0, 0x0, 0x0, 0x4c, 0x6, 0x0, 0x0, 0x0, 0x0, 0x0c, 0x78, 0x74, (byte) 0x97, 0x32, 0x67, (byte) 0x89, 0x10,
            0x71, 0x60, (byte) 0x9b, 0x10, 0x2d, 0x62, 0x5, 0x0, (byte) 0xdb, 0x10, 0x2d, 0x0, 0x0, 0x0, (byte) 0x9b,
            0x10, 0x3b, 0x20, 0x22, 0x0, (byte) 0x9a, 0x10, 0x5b, 0x76, 0x0, (byte) 0x9a, 0x10, 0x5f, 0x66, 0x0, 0x0c,
            0x22, 0x62, 0x32, 0x0, 0x0, 0x3c, 0x22, 0x56, 0x4, 0x0, 0x0, 0x7c, 0x22, 0x0, 0x0, 0x0, 0x0, 0x42, 0x6c,
            0x1, 0x1, (byte) 0x8c, 0x20, 0x6, 0x0, 0x0, 0x0, 0x0, (byte) 0x8c, 0x30, 0x6, 0x0, 0x0, 0x0, 0x0,
            (byte) 0x8c, (byte) 0x80, 0x10, 0x6, 0x0, 0x0, 0x0, 0x0, (byte) 0xcc, 0x20, 0x6, 0x0, 0x0, 0x0, 0x0,
            (byte) 0xcc, 0x30, 0x6, 0x0, 0x0, 0x0, 0x0, (byte) 0xcc, (byte) 0x80, 0x10, 0x6, 0x0, 0x0, 0x0, 0x0,
            (byte) 0x9a, 0x11, 0x5b, 0x69, 0x0, (byte) 0x9a, 0x11, 0x5f, 0x64, 0x0, (byte) 0x9b, 0x11, 0x3b, 0x20, 0x16,
            0x0, (byte) 0x9b, 0x11, 0x2d, 0x62, 0x5, 0x0, (byte) 0xbc, 0x1, 0x22, 0x56, 0x4, 0x0, 0x0, (byte) 0x8c, 0x1,
            0x6, 0x10, 0x62, 0x4, 0x0, (byte) 0x8c, 0x21, 0x6, 0x0, 0x0, 0x0, 0x0, (byte) 0x8c, 0x31, 0x6, 0x0, 0x0,
            0x0, 0x0, (byte) 0x8c, (byte) 0x81, 0x10, 0x6, 0x0, 0x0, 0x0, 0x0, (byte) 0x8c, 0x1, 0x14, 0x44, 0x27, 0x26,
            0x0, 0x4, 0x6d, 0x2a, 0x14, (byte) 0xba, 0x17, 0x0f, 0x21, 0x4, 0x0, 0x10, (byte) 0xa0, (byte) 0xa9, 0x16 };

    private DriverConnection newConnection(String mBusAdresse) throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        PowerMockito.when(con.read(anyInt())).thenReturn(vds);

        ConnectionInterface serialIntervace = new ConnectionInterface(con, mBusAdresse, delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = mBusAdresse.trim().split(":");

        Integer mBusAddress;
        SecondaryAddress secondaryAddress = null;
        if (deviceAddressTokens[1].length() == 16) {
            mBusAddress = 0xfd;
            byte[] addressData = Helper.hexToBytes(deviceAddressTokens[1]);
            secondaryAddress = SecondaryAddress.newFromLongHeader(addressData, 0);
        }
        else {
            mBusAddress = Integer.decode(deviceAddressTokens[1]);
        }

        DriverConnection mBusConnection = new DriverConnection(serialIntervace, mBusAddress, secondaryAddress, delay);

        return mBusConnection;

    }

    private static ChannelRecordContainer newChannelRecordContainer(String channelAddress) {
        final String channel = channelAddress;
        return new ChannelRecordContainer() {
            Value longValue = new LongValue(9073);
            Record record = new Record(longValue, System.currentTimeMillis());

            @Override
            public Record getRecord() {
                return record;
            }

            @Override
            public Channel getChannel() {
                Channel c = PowerMockito.mock(Channel.class);
                return c;
            }

            @Override
            public void setRecord(Record record) {
                this.record = record;
            }

            @Override
            public void setChannelHandle(Object handle) {
            }

            @Override
            public Object getChannelHandle() {
                return null;
            }

            @Override
            public String getChannelAddress() {
                return channel;
            }

            @Override
            public ChannelRecordContainer copy() {
                return newChannelRecordContainer(channel);
            }
        };
    }

    @Test
    public void testScanForChannels() throws Exception {
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        mBusConnection.disconnect();
        assertEquals(ValueType.LONG, mBusConnection.scanForChannels(null).get(0).getValueType());
    }

    @Test
    public void testScanForChannelsByteArray() throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(SIEMENS_UH50_ANSWER, 6, SIEMENS_UH50_ANSWER.length - 6,
                null, null);
        vds.decode();
        when(con.read(anyInt())).thenReturn(vds);

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };
        DriverConnection mBusConnection = new DriverConnection(serialIntervace,
                Integer.parseInt(deviceAddressTokens[1]), null, delay);

        mBusConnection.disconnect();
        List<ChannelScanInfo> scanForChannels = mBusConnection.scanForChannels(null);
        for (ChannelScanInfo info : scanForChannels) {
            System.out.println(info.getDescription() + " " + info.getUnit());

        }
        ValueType actual = mBusConnection.scanForChannels(null).get(22).getValueType();
        assertEquals(ValueType.LONG, actual);
    }

    @Test
    public void testReadWithSec() throws Exception {
        DriverConnection con = newConnection("/dev/ttyS100:74973267a7320404");

        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("04:03"),
                newChannelRecordContainer("02:fd5b"));

        con.read(records, null, null);

    }

    @Test
    public void testRead() throws Exception {

        DriverConnection con = newConnection("/dev/ttyS100:5");
        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("04:03"),
                newChannelRecordContainer("02:fd5b"));

        con.read(records, null, null);

    }

    @Test
    public void testReadBcdDateLong() throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(SIEMENS_UH50_ANSWER, 6, SIEMENS_UH50_ANSWER.length - 6,
                null, null);
        vds.decode();

        when(con.read(anyInt())).thenReturn(vds);

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };
        DriverConnection mBusConnection = new DriverConnection(serialIntervace,
                Integer.parseInt(deviceAddressTokens[1]), null, delay);

        List<ChannelRecordContainer> records = new LinkedList<>();
        records.add(newChannelRecordContainer("09:74"));
        records.add(newChannelRecordContainer("42:6c"));
        records.add(newChannelRecordContainer("8c01:14"));

        mBusConnection.read(records, null, null);

    }

    @Test
    public void testReadWrongChannelAddressAtContainer() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        crc.add(newChannelRecordContainer("X04:03:5ff0"));

        mBusConnection.read(crc, null, null);
        assertEquals(Flag.VALID, crc.get(0).getRecord().getFlag());

    }

    @Test
    public void testReadWithX() throws Exception {
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("X04:03"));

        mBusConnection.read(records, null, null);

    }

    @Test
    public void testReadAndDisconnect() throws Exception {
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("X04:03"));
        mBusConnection.read(records, null, null);
        mBusConnection.disconnect();
    }

    @Test
    public void testDisconnect() throws Exception {
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        mBusConnection.disconnect();
        mBusConnection.disconnect();
    }

    @Test(expected = ConnectionException.class)
    public void testDisconnectRead() throws Exception {
        DriverConnection mBusConnection = newConnection("/dev/ttyS100:5");
        mBusConnection.disconnect();

        List<ChannelRecordContainer> crc = Collections.emptyList();
        mBusConnection.read(crc, null, null);

    }

    @Test
    public void testReadThrowsIOException() throws Exception {
        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        when(con.read(anyInt())).thenThrow(new IOException());

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();

        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };
        int address = Integer.parseInt(deviceAddressTokens[1]);
        DriverConnection driverCon = new DriverConnection(serialIntervace, address, null, delay);

        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("04:03"));

        driverCon.read(records, null, null);
        Flag actualFlag = records.get(0).getRecord().getFlag();
        assertEquals(Flag.DRIVER_ERROR_TIMEOUT, actualFlag);
    }

    @Test
    public void testReadThrowsTimeoutException() throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        when(con.read(anyInt())).thenThrow(new SerialPortTimeoutException());

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };

        int address = Integer.parseInt(deviceAddressTokens[1]);
        DriverConnection driverCon = new DriverConnection(serialIntervace, address, null, delay);

        List<ChannelRecordContainer> records = Arrays.asList(newChannelRecordContainer("04:03"));
        driverCon.read(records, null, null);

        assertEquals(Flag.DRIVER_ERROR_TIMEOUT, records.get(0).getRecord().getFlag());
    }

    @Test(expected = ConnectionException.class)
    public void testScanThrowsTimeoutException() throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        when(con.read(anyInt())).thenThrow(new SerialPortTimeoutException());

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };
        DriverConnection driverCon = new DriverConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null, delay);

        driverCon.scanForChannels(null);
    }

    @Test(expected = ConnectionException.class)
    public void testScanThrowsIOException() throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        when(con.read(anyInt())).thenThrow(new IOException());

        ConnectionInterface serialIntervace = new ConnectionInterface(con, "/dev/ttyS100:5", delay, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = { "/dev/ttyS100", "5" };
        DriverConnection mBusConnection = new DriverConnection(serialIntervace,
                Integer.parseInt(deviceAddressTokens[1]), null, delay);

        mBusConnection.scanForChannels(null);
    }

}
