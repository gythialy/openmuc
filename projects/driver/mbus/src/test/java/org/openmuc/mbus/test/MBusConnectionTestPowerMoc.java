package org.openmuc.mbus.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.mbus.MBusConnection;
import org.openmuc.framework.driver.mbus.MBusSerialInterface;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MBusConnection.class)
public class MBusConnectionTestPowerMoc {

    private final Map<String, MBusSerialInterface> interfaces = new HashMap<>();
    private final static byte[] NZR_ANSWER = { 104, 50, 50, 104, 8, 5, 114, 8, 6, 16, 48, 82, 59, 1, 2, 2, 0, 0, 0, 4,
            3, -25, 37, 0, 0, 4, -125, 127, -25, 37, 0, 0, 2, -3, 72, 54, 9, 2, -3, 91, 0, 0, 2, 43, 0, 0, 12, 120, 8,
            6, 16, 48, 15, 63, -79, 22 };
    private final static byte[] SIEMENS_UH50_ANSWER = { 0x68, (byte) 0xf8, (byte) 0xf8, 0x68, 0x8, (byte) 100, 0x72,
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

    public MBusConnection mockingConnection(String mBusAdresse) throws Exception {

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(vds);

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, mBusAdresse, interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = mBusAdresse.trim().split(":");
        Integer mBusAddress;
        SecondaryAddress secondaryAddress = null;
        if (deviceAddressTokens[1].length() == 16) {
            mBusAddress = 0xfd;
            secondaryAddress = SecondaryAddress.getFromHexString(deviceAddressTokens[1]);
        }
        else {
            mBusAddress = Integer.decode(deviceAddressTokens[1]);
        }

        MBusConnection mBusConnection = new MBusConnection(serialIntervace, mBusAddress, secondaryAddress);

        return mBusConnection;

    }

    private ChannelRecordContainer mockChannelRecordContainer(String channelAddress) {
        final String channel = channelAddress;
        return new ChannelRecordContainer() {
            Value longValue = new LongValue(9073);
            Record record = new Record(longValue, System.currentTimeMillis());

            @Override
            public Record getRecord() {
                // TODO Auto-generated method stub

                return record;
            }

            @Override
            public Channel getChannel() {
                // TODO Auto-generated method stub
                Channel c = PowerMockito.mock(Channel.class);
                return c;
            }

            @Override
            public void setRecord(Record record) {
                // TODO Auto-generated method stub
                this.record = record;
            }

            @Override
            public void setChannelHandle(Object handle) {
                // TODO Auto-generated method stub

            }

            @Override
            public Object getChannelHandle() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getChannelAddress() {
                // TODO Auto-generated method stub
                return channel;
            }

            @Override
            public ChannelRecordContainer copy() {
                // TODO Auto-generated method stub
                return mockChannelRecordContainer(channel);
            }
        };
    }

    @Test
    public void testScanForChannels() throws Exception {
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        Assert.assertEquals(mBusConnection.scanForChannels(null).get(0).getValueType(), ValueType.LONG);
    }

    @Test
    public void testScanForChannelsByteArray() throws Exception {

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(SIEMENS_UH50_ANSWER, 6, SIEMENS_UH50_ANSWER.length - 6,
                null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        System.out.println("Get Sec Address" + vds.getSecondaryAddress().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(vds);

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        Assert.assertEquals(mBusConnection.scanForChannels(null).get(22).getValueType(), ValueType.BYTE_ARRAY);
    }

    @Test
    public void testReadWithSec() throws Exception {

        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:74973267a7320404");
        crc.add(mockChannelRecordContainer("04:03"));
        crc.add(mockChannelRecordContainer("02:fd5b"));

        mBusConnection.read(crc, null, null);

    }

    @Test
    public void testRead() throws Exception {

        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        crc.add(mockChannelRecordContainer("04:03"));
        crc.add(mockChannelRecordContainer("02:fd5b"));

        mBusConnection.read(crc, null, null);

    }

    @Test
    public void testReadBcdDateLong() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(SIEMENS_UH50_ANSWER, 6, SIEMENS_UH50_ANSWER.length - 6,
                null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(vds);

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        crc.add(mockChannelRecordContainer("09:74"));
        crc.add(mockChannelRecordContainer("42:6c"));
        crc.add(mockChannelRecordContainer("8c01:14"));

        mBusConnection.read(crc, null, null);

    }

    @Test
    public void testReadWrongChannelAddressAtContainer() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        crc.add(mockChannelRecordContainer("X04:03:5ff0"));

        mBusConnection.read(crc, null, null);
        Assert.assertEquals(crc.get(0).getRecord().getFlag(), Flag.VALID);

    }

    @Test
    public void testReadWithX() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        crc.add(mockChannelRecordContainer("X04:03"));

        mBusConnection.read(crc, null, null);

    }

    @Test
    public void testReadAndDisconnect() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        crc.add(mockChannelRecordContainer("04:03"));
        mBusConnection.read(crc, null, null);
        mBusConnection.disconnect();

    }

    @Test
    public void testDisconnect() throws Exception {

        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        mBusConnection.disconnect();
        mBusConnection.disconnect();

    }

    @Test(expected = ConnectionException.class)
    public void testDisconnectRead() throws Exception {
        List<ChannelRecordContainer> crc = new LinkedList<>();
        MBusConnection mBusConnection = mockingConnection("/dev/ttyS100:5");
        mBusConnection.disconnect();
        mBusConnection.read(crc, null, null);

    }

    @Test(expected = ConnectionException.class)
    public void testReadThrowsIOException() throws Exception {
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new IOException());

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        List<ChannelRecordContainer> crc = new LinkedList<>();
        crc.add(mockChannelRecordContainer("04:03"));

        mBusConnection.read(crc, null, null);
        Assert.assertTrue(crc.get(0).getRecord().getFlag().equals(Flag.DRIVER_ERROR_TIMEOUT));
    }

    @Test
    public void testReadThrowsTimeoutException() throws Exception {

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new TimeoutException());

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        List<ChannelRecordContainer> crc = new LinkedList<>();
        crc.add(mockChannelRecordContainer("04:03"));

        mBusConnection.read(crc, null, null);
        Assert.assertTrue(crc.get(0).getRecord().getFlag().equals(Flag.DRIVER_ERROR_TIMEOUT));
    }

    @Test
    public void testScanThrowsTimeoutException() throws Exception {

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new TimeoutException());

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        List<ChannelRecordContainer> crc = new LinkedList<>();
        crc.add(mockChannelRecordContainer("04:03"));

        Assert.assertNull(mBusConnection.scanForChannels(null));

    }

    @Test(expected = ConnectionException.class)
    public void testScanThrowsIOException() throws Exception {

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        VariableDataStructure vds = new VariableDataStructure(NZR_ANSWER, 6, NZR_ANSWER.length - 6, null, null);
        vds.decode();
        System.out.println(vds.getDataRecords().get(0).getDataValue().toString());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new IOException());

        MBusSerialInterface serialIntervace = new MBusSerialInterface(mockedMBusSap, "/dev/ttyS100:5", interfaces);
        serialIntervace.increaseConnectionCounter();
        String[] deviceAddressTokens = "/dev/ttyS100:5".trim().split(":");
        MBusConnection mBusConnection = new MBusConnection(serialIntervace, Integer.parseInt(deviceAddressTokens[1]),
                null);
        List<ChannelRecordContainer> crc = new LinkedList<>();
        crc.add(mockChannelRecordContainer("04:03"));

        mBusConnection.scanForChannels(null);

    }

}
