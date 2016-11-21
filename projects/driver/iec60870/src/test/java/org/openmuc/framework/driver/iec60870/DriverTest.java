package org.openmuc.framework.driver.iec60870;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.iec60870.settings.DeviceAddress;
import org.openmuc.framework.driver.iec60870.settings.DeviceSettings;

public class DriverTest {

    private final String SEP = ";";
    private final String TS = "=";

    @Test
    public void testDeviceAddress_OK() throws ArgumentSyntaxException {
        String expectedHost = "192.168.1.5";
        int expectedPort = 1265;
        int expectedCa = 5;
        String string = "p " + TS + expectedPort + SEP + "h " + TS + "  " + expectedHost + SEP + "   ca    " + TS
                + expectedCa;

        System.out.println("testDeviceAddress_OK: " + string);

        DeviceAddress testSetting = new DeviceAddress(string);

        Assert.assertEquals(expectedHost, testSetting.hostAddress().getHostAddress());
        Assert.assertEquals(expectedPort, testSetting.port());
        Assert.assertEquals(expectedCa, testSetting.commonAddress());
    }

    @Test
    public void testDeviceAddress_OK_with_one_option() throws ArgumentSyntaxException {
        String expectedHost = "192.168.1.5";
        String string = "h" + TS + expectedHost;

        System.out.println("testDeviceAddress_OK: " + string);

        DeviceAddress testSetting = new DeviceAddress(string);

        Assert.assertEquals(expectedHost, testSetting.hostAddress().getHostAddress());
    }

    @Test
    public void testDeviceSettings_OK() throws ArgumentSyntaxException {
        int expectedMFT = 123;
        int expectedCFL = 321;
        String string = "mft " + TS + expectedMFT + SEP + "cfl " + TS + expectedCFL;

        System.out.println("testDeviceSettings_OK: " + string);

        DeviceSettings testSetting = new DeviceSettings(string);

        Assert.assertEquals(expectedMFT, testSetting.messageFragmentTimeout());
        Assert.assertEquals(expectedCFL, testSetting.cotFieldLength());
    }

    @Test
    public void test_syntax() throws ArgumentSyntaxException {
        // System.out.println(DeviceSettings.syntax(DeviceSettings.class));
        // System.out.println(DeviceAddress.syntax(DeviceAddress.class));
        // System.out.println(ChannelAddress.syntax(ChannelAddress.class));
    }

}
