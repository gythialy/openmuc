package org.openmuc.framework.driver.dlms.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class SettingsTest {

    @Test
    public void testChannelAddress() throws Exception {
        check(ChannelAddress.class, 2);
    }

    @Test
    public void testDeviceAddress() throws Exception {
        check(DeviceAddress.class, 10);
    }

    @Test
    public void testDeviceSetting() throws Exception {
        check(DeviceSettings.class, 12);
    }

    private static void check(Class<? extends GenericSetting> clazz, int numArgs) {
        Pattern p = Pattern.compile("(\\w+:) *(\\[?\\w+=.*\\]?)+$");
        String pat = GenericSetting.strSyntaxFor(clazz);
        Matcher m1 = p.matcher(pat);
        assertTrue(pat, m1.matches());

        String[] str = pat.substring(m1.group(1).length()).trim().replaceAll("(\\[|\\])", "").split(";");

        assertEquals(numArgs, str.length);

        Set<String> keys = new HashSet<>();
        for (String pair : str) {
            String[] s = pair.split("=");
            assertEquals(2, s.length);
            keys.add(s[0]);
        }

        assertEquals(numArgs, keys.size());
    }

    @Test
    public void test2() throws Exception {
        String t = "serial";
        String sp = "/dev/ttyUSB0";
        int bd = 19200;
        boolean eh = true;
        int port = 1234;
        String str = MessageFormat.format("t={0};sp={1};bd={2};\r\b hdlc=true;\t eh={3};  p={4}", t, sp, bd, eh, port);
        DeviceAddress deviceAddress = new DeviceAddress(str);

        assertEquals(t, deviceAddress.getConnectionType());
        assertEquals(sp, deviceAddress.getSerialPort());
        assertEquals(bd, deviceAddress.getBaudrate());
        assertEquals(eh, deviceAddress.enableBaudRateHandshake());
        assertEquals(port, deviceAddress.getPort());
    }
}
