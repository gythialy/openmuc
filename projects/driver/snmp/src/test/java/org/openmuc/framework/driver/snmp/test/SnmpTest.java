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
package org.openmuc.framework.driver.snmp.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.snmp.SnmpDriver;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.spi.ConnectionException;

public class SnmpTest {

    private static SnmpDriver snmpDriver;
    private static String correctSetting;

    @BeforeAll
    public static void beforeClass() {
        snmpDriver = new SnmpDriver();
        correctSetting = SnmpDriverSettingVariableNames.USERNAME + "=username:"
                + SnmpDriverSettingVariableNames.SECURITYNAME + "=securityname:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=password:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=privacy";
    }

    @Test
    public void testInvalidSettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        String settings = SnmpDriverSettingVariableNames.SECURITYNAME + "=security:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", settings));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", settings));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", settings));
    }

    @Test
    public void testNullSettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", null));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", null));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", null));
    }

    @Test
    public void testEmptySettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", ""));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", ""));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", ""));
    }

    @Test
    public void testInvalidSettingStringFormat() throws ConnectionException, ArgumentSyntaxException {

        String settings = SnmpDriverSettingVariableNames.USERNAME + "=username&"
                + SnmpDriverSettingVariableNames.SECURITYNAME + "=username:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        String finalSettings = settings;
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings));

        settings = SnmpDriverSettingVariableNames.USERNAME + ":username&" + SnmpDriverSettingVariableNames.SECURITYNAME
                + "=username:" + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        String finalSettings1 = settings;
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings1));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings1));
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", finalSettings1));
    }

    @Test
    public void testInvalidDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1:1", correctSetting));
    }

    @Test
    public void testNullDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect(null, correctSetting));
    }

    @Test
    public void testEmptyDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("", correctSetting));
    }

    @Test
    public void testIncorrectSnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", correctSetting));
    }

    @Test
    public void testNullSnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", correctSetting));
    }

    @Test
    public void testEmptySnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        assertThrows(ArgumentSyntaxException.class, () -> snmpDriver.connect("1.1.1.1/1", correctSetting));
    }

    @AfterAll
    public static void afterClass() {
        snmpDriver = null;
    }
}
