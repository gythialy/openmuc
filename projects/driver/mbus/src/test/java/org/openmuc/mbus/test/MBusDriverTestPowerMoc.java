package org.openmuc.mbus.test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.mbus.MBusDriver;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.VariableDataStructure;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MBusDriver.class)
public class MBusDriverTestPowerMoc {

    @Test
    public void testGetDriverInfo() {
        MBusDriver mdriver = new MBusDriver();
        Assert.assertTrue(mdriver.getInfo().getId().equals("mbus"));
    }

    /**
     * Test the connect Method of MBusDriver without the functionality of jMBus Called the
     * {@link #connect(String channelAdress, String bautrate) connect} Method
     * 
     * @throws Exception
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
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doNothing().when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        Assert.assertNotNull(mdriver.connect(channelAdress, bautrate));
        Assert.assertNotNull(mdriver.connect(channelAdress, bautrate));
    }

    /**
     * This Testmethod will test the connect Method of MBus Driver, without testing jMBus Library functions. With
     * Mockito and PowerMockito its possible to do this. At first it will create an MBusDriver Objekt. Then we mocking
     * an MBusSap Objects without functionality. If new MBusSap will created, it will return the mocked Object
     * "mockedMBusSap". If the linkReset Method will called, it will do nothing. If the read Method will call, we return
     * null.
     * 
     * @param deviceAddress
     * @param bautrate
     * @throws IOException
     * @throws TimeoutException
     * @throws Exception
     * @throws ArgumentSyntaxException
     * @throws ConnectionException
     */
    private void connect(String deviceAddress, String bautrate) throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doNothing().when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        Assert.assertNotNull(mdriver.connect(deviceAddress, bautrate));
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
    public void testMBusSapOpenThrowsIllArgException() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new IOException()).when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapLinkResetThrowsIOException() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new IOException()).when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).open();
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapReadThrowsTimeoutException() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new TimeoutException()).when(mockedMBusSap).read(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).open();
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    @Test(expected = ConnectionException.class)
    public void testMBusSapReadThrowsTimeoutExceptionAtSecondRun() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doNothing().when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        Assert.assertNotNull(mdriver.connect("/dev/ttyS100:5", "2400"));
        PowerMockito.doThrow(new TimeoutException()).when(mockedMBusSap).read(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.doNothing().when(mockedMBusSap).open();
        mdriver.connect("/dev/ttyS100:5", "2400");
    }

    // ******************* SCAN TESTS ********************//

    public void scan(String settings) throws Exception {
        final MBusDriver mdriver = new MBusDriver();
        DriverDeviceScanListener ddsl = new DriverDeviceScanListener() {

            @Override
            public void scanProgressUpdate(int progress) {
                // TODO Auto-generated method stub
                System.out.println("Progress: " + progress + "%");

            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                System.out.println("Device Found: " + scanInfo.toString());

            }
        };

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.when(mockedMBusSap.read(1)).thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        PowerMockito.when(mockedMBusSap.read(250)).thenThrow(new TimeoutException());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new TimeoutException());
        class InterruptScanThread implements Runnable {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    mdriver.interruptDeviceScan();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }
        new InterruptScanThread().run();
        mdriver.scanForDevices(settings, ddsl);

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
    public void testScanForDevicesArgumentSyntaxException() throws Exception {
        // NO Setting is set!
        scan(new String());
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testScanForDevicesBautrateIsNotANumberArgumentSyntaxException() throws Exception {
        // Bautrate isn't a number
        scan("/dev/ttyS100:aaa");
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testScanForDevicesToManyArgumentsArgumentSyntaxException() throws Exception {
        // TO Many Arguments
        scan("/dev/ttyS100:2400:assda");
    }

    @Test(expected = ScanInterruptedException.class)
    public void testInterrupedException() throws Exception {
        final MBusDriver mdriver = new MBusDriver();
        DriverDeviceScanListener ddsl = new DriverDeviceScanListener() {

            @Override
            public void scanProgressUpdate(int progress) {
                // TODO Auto-generated method stub
                System.out.println("Progress: " + progress + "%");

            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                System.out.println("Device Found: " + scanInfo.toString());
                mdriver.interruptDeviceScan();
            }
        };

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt()))
                .thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doNothing().when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);

        Assert.assertNotNull(mdriver.connect("/dev/ttyS100:5", "2400"));
        mdriver.scanForDevices("/dev/ttyS100:2400", ddsl);

    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testScanMBusSapOpenThrowsIllArgException() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new IllegalArgumentException()).when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        mdriver.scanForDevices("/dev/ttyS100:2400", null);
    }

    @Test(expected = ScanException.class)
    public void testScanMBusSapOpenIOException() throws Exception {
        MBusDriver mdriver = new MBusDriver();
        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.doThrow(new IOException()).when(mockedMBusSap).open();
        PowerMockito.doNothing().when(mockedMBusSap).linkReset(Matchers.anyInt());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenReturn(null);
        mdriver.scanForDevices("/dev/ttyS100:2400", null);
    }

    @Test(expected = ScanException.class)
    public void testScanMBusSapReadThrowsIOException() throws Exception {
        final MBusDriver mdriver = new MBusDriver();
        DriverDeviceScanListener ddsl = new DriverDeviceScanListener() {

            @Override
            public void scanProgressUpdate(int progress) {
                // TODO Auto-generated method stub
                System.out.println("Progress: " + progress + "%");

            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                System.out.println("Device Found: " + scanInfo.toString());

            }
        };

        MBusSap mockedMBusSap = PowerMockito.mock(MBusSap.class);
        PowerMockito.whenNew(MBusSap.class).withAnyArguments().thenReturn(mockedMBusSap);
        PowerMockito.when(mockedMBusSap.read(1)).thenReturn(new VariableDataStructure(null, 0, 0, null, null));
        PowerMockito.when(mockedMBusSap.read(250)).thenThrow(new TimeoutException());
        PowerMockito.when(mockedMBusSap.read(Matchers.anyInt())).thenThrow(new IOException());
        class InterruptScanThread implements Runnable {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    mdriver.interruptDeviceScan();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }
        new InterruptScanThread().run();
        mdriver.scanForDevices("/dev/ttyS100:2400", ddsl);

    }

}
