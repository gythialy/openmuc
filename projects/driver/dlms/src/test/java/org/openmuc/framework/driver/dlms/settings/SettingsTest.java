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
package org.openmuc.framework.driver.dlms.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

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
        assertTrue(m1.matches(), pat);

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
