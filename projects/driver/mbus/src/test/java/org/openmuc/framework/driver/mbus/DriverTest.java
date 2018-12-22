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
package org.openmuc.framework.driver.mbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Driver.class, MBusConnection.class })
public class DriverTest {

    @Test
    public void testGetDriverInfo() {
        assertEquals("mbus", new Driver().getInfo().getId());
    }

    /*
     * Test the connect Method of MBusDriver without the functionality of jMBus Called the {@link #connect(String
     * channelAdress, String bautrate) connect} Method
     */
    @Test
    public void testConnectSucceed() throws Exception {
        String channelAdress = "/dev/ttyS100:5";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test
    public void testConnectSucceedWithSecondary() throws Exception {
        String channelAdress = "/dev/ttyS100:74973267a7320404";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test
    public void testConnectionBautrateIsEmpty() throws Exception {
        String channelAdress = "/dev/ttyS100:5";
        String bautrate = "";
        connect(channelAdress, bautrate);
    }

    @Test
    public void TestConnectTwoTimes() throws Exception {
        String channelAdress = "/dev/ttyS100:5";
        String bautrate = "2400";
        Driver mdriver = new Driver();
        MBusConnection mockedMBusSap = PowerMockito.mock(MBusConnection.class);
        PowerMockito.whenNew(MBusConnection.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        Assert.assertNotNull(mdriver.connect(channelAdress, bautrate));
        Assert.assertNotNull(mdriver.connect(channelAdress, bautrate));
    }

    /*
     * This Testmethod will test the connect Method of MBus Driver, without testing jMBus Library functions. With
     * Mockito and PowerMockito its possible to do this. At first it will create an MBusDriver Objekt. Then we mocking
     * an MBusSap Objects without functionality. If new MBusSap will created, it will return the mocked Object
     * "mockedMBusSap". If the linkReset Method will called, it will do nothing. If the read Method will call, we return
     * null.
     */
    private void connect(String deviceAddress, String bautrate) throws Exception {
        Driver driver = new Driver();
        MBusConnection con = mock(MBusConnection.class);
        MBusSerialBuilder builder = mock(MBusSerialBuilder.class);

        whenNew(MBusSerialBuilder.class).withAnyArguments().thenReturn(builder);
        when(builder.setBaudrate(anyInt())).thenReturn(builder);
        when(builder.setTimeout(anyInt())).thenReturn(builder);
        when(builder.setParity(any(Parity.class))).thenReturn(builder);
        when(builder.build()).thenReturn(con);

        doNothing().when(con).linkReset(anyInt());
        when(con.read(anyInt())).thenReturn(null);
        assertNotNull(driver.connect(deviceAddress, bautrate));
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectionArgumentSyntaxExceptionNoPortSet() throws Exception {
        String channelAdress = "/dev/ttyS100:";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectWithWrongSecondary() throws Exception {
        String channelAdress = "/dev/ttyS100:74973267a20404";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectionChannelAddressEmpty() throws Exception {
        String channelAdress = "";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectionArgumentSyntaxExceptionChannelAddressWrongSyntax() throws Exception {
        String channelAdress = "/dev/ttyS100:a";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectionArgumentSyntaxExceptionToManyArguments() throws Exception {
        String channelAdress = "/dev/ttyS100:5:1";
        String bautrate = "2400";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testConnectionArgumentSyntaxExceptionBautIsNotANumber() throws Exception {
        String channelAdress = "/dev/ttyS100:5";
        String bautrate = "asd";
        connect(channelAdress, bautrate);
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapLinkResetThrowsIOException() throws Exception {
        Driver mdriver = new Driver();
        MBusConnection mockedMBusSap = PowerMockito.mock(MBusConnection.class);
        PowerMockito.whenNew(MBusConnection.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new IOException()).when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        mdriver.connect("/dev/ttyS100:5", "2400:lr");
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapReadThrowsTimeoutException() throws Exception {
        Driver mdriver = new Driver();
        MBusConnection mockedMBusSap = PowerMockito.mock(MBusConnection.class);
        PowerMockito.whenNew(MBusConnection.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new SerialPortTimeoutException()).when(mockedMBusSap).read(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapReadThrowsTimeoutExceptionAtSecondRun() throws Exception {
        MBusConnection con = mock(MBusConnection.class);
        whenNew(MBusConnection.class).withAnyArguments().thenReturn(con);
        doNothing().when(con).linkReset(anyInt());
        when(con.read(anyInt())).thenReturn(null);

        Driver mdriver = new Driver();
        assertNotNull(mdriver.connect("/dev/ttyS100:5", "2400"));
        doThrow(new SerialPortTimeoutException()).when(con).read(anyInt());
        doNothing().when(con).linkReset(anyInt());
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    // ******************* SCAN TESTS ********************//

    private static void scan(String settings) throws Exception {

        MBusConnection con = mock(MBusConnection.class);
        PowerMockito.whenNew(MBusConnection.class).withAnyArguments().thenReturn(con);
        PowerMockito.when(con.read(1)).thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        PowerMockito.when(con.read(250)).thenThrow(new SerialPortTimeoutException());
        PowerMockito.when(con.read(anyInt())).thenThrow(new SerialPortTimeoutException());

        Driver mdriver = new Driver();
        mdriver.interruptDeviceScan();
        mdriver.scanForDevices(settings, mock(DriverDeviceScanListener.class));

    }

    @Test
    public void testScanForDevices() throws Exception {

        scan("/dev/ttyS100:2400");
    }

    @Test
    public void testScanForDevicesWithOutBautRate() throws Exception {

        scan("/dev/ttyS100");
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void scanEmptySettings() throws Exception {
        scan("");
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testScanForDevicesBautrateIsNotANumber() throws Exception {
        scan("/dev/ttyS100:aaa");
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void scanToManyArgs() throws Exception {
        scan("/dev/ttyS100:2400:assda");
    }

    @Test(expected = ScanInterruptedException.class)
    public void testInterrupedException() throws Exception {
        final Driver mdriver = new Driver();
        DriverDeviceScanListener ddsl = mock(DriverDeviceScanListener.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                mdriver.interruptDeviceScan();
                return null;
            }
        }).when(ddsl).deviceFound(any(DeviceScanInfo.class));

        MBusConnection con = mock(MBusConnection.class);
        when(con.read(anyInt())).thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        whenNew(MBusConnection.class).withAnyArguments().thenReturn(con);
        doNothing().when(con).linkReset(Matchers.anyInt());
        PowerMockito.when(con.read(Matchers.anyInt())).thenReturn(null);

        assertNotNull(mdriver.connect("/dev/ttyS100:5", "2400"));
        mdriver.scanForDevices("/dev/ttyS100:2400", ddsl);

    }

    private static MBusConnection mockNewBuilderCon() throws IOException, InterruptedIOException, Exception {
        MBusSerialBuilder builder = mock(MBusSerialBuilder.class);

        MBusConnection con = mock(MBusConnection.class);
        doNothing().when(con).linkReset(Matchers.anyInt());
        when(con.read(Matchers.anyInt())).thenReturn(null);
        whenNew(MBusSerialBuilder.class).withAnyArguments().thenReturn(builder);
        when(builder.setBaudrate(anyInt())).thenReturn(builder);
        when(builder.setTimeout(anyInt())).thenReturn(builder);
        when(builder.setParity(any(Parity.class))).thenReturn(builder);
        when(builder.build()).thenReturn(con);

        return con;
    }

    @Test(expected = ScanException.class)
    public void scanConOpenIOException() throws Exception {
        MBusSerialBuilder builder = mock(MBusSerialBuilder.class);

        whenNew(MBusSerialBuilder.class).withAnyArguments().thenReturn(builder);
        when(builder.setBaudrate(anyInt())).thenReturn(builder);
        when(builder.setTimeout(anyInt())).thenReturn(builder);
        when(builder.build()).thenThrow(new IOException());

        new Driver().scanForDevices("/dev/ttyS100:2400", mock(DriverDeviceScanListener.class));
    }

    @Test(expected = ScanException.class)
    public void testScanMBusSapReadThrowsIOException() throws Exception {

        MBusConnection con = mockNewBuilderCon();
        when(con.read(1)).thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        when(con.read(250)).thenThrow(new SerialPortTimeoutException());
        when(con.read(anyInt())).thenThrow(new IOException());

        final Driver mdriver = new Driver();
        class InterruptScanThread implements Runnable {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    mdriver.interruptDeviceScan();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        new InterruptScanThread().run();
        mdriver.scanForDevices("/dev/ttyS100:2400", mock(DriverDeviceScanListener.class));
    }

}
