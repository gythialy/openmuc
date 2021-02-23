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
package org.openmuc.framework.driver.iec60870;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
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

        assertEquals(expectedHost, testSetting.hostAddress().getHostAddress());
        assertEquals(expectedPort, testSetting.port());
        assertEquals(expectedCa, testSetting.commonAddress());
    }

    @Test
    public void testDeviceAddress_OK_with_one_option() throws ArgumentSyntaxException {
        String expectedHost = "192.168.1.5";
        String string = "h" + TS + expectedHost;

        System.out.println("testDeviceAddress_OK: " + string);

        DeviceAddress testSetting = new DeviceAddress(string);

        assertEquals(expectedHost, testSetting.hostAddress().getHostAddress());
    }

    @Test
    public void testDeviceSettings_OK() throws ArgumentSyntaxException {
        int expectedMFT = 123;
        int expectedCFL = 321;
        String string = "mft " + TS + expectedMFT + SEP + "cfl " + TS + expectedCFL;

        System.out.println("testDeviceSettings_OK: " + string);

        DeviceSettings testSetting = new DeviceSettings(string);

        assertEquals(expectedMFT, testSetting.messageFragmentTimeout());
        assertEquals(expectedCFL, testSetting.cotFieldLength());
    }

    @Test
    public void test_syntax() throws ArgumentSyntaxException {
        // System.out.println(DeviceSettings.syntax(DeviceSettings.class));
        // System.out.println(DeviceAddress.syntax(DeviceAddress.class));
        // System.out.println(ChannelAddress.syntax(ChannelAddress.class));
    }

}
