/*
 * Copyright 2011-16 Fraunhofer ISE
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.snmp.SnmpDriver;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.spi.ConnectionException;

public class SnmpTest {

    private static SnmpDriver snmpDriver;
    private static String correctSetting;

    @BeforeClass
    public static void beforeClass() {
        snmpDriver = new SnmpDriver();
        correctSetting = SnmpDriverSettingVariableNames.USERNAME + "=username:"
                + SnmpDriverSettingVariableNames.SECURITYNAME + "=securityname:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=password:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=privacy";
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testInvalidSettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        String settings = SnmpDriverSettingVariableNames.SECURITYNAME + "=security:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testNullSettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        snmpDriver.connect("1.1.1.1/1", null);
        snmpDriver.connect("1.1.1.1/1", null);
        snmpDriver.connect("1.1.1.1/1", null);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testEmptySettingStringNumber() throws ConnectionException, ArgumentSyntaxException {

        snmpDriver.connect("1.1.1.1/1", "");
        snmpDriver.connect("1.1.1.1/1", "");
        snmpDriver.connect("1.1.1.1/1", "");
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testInvalidSettingStringFormat() throws ConnectionException, ArgumentSyntaxException {

        String settings = SnmpDriverSettingVariableNames.USERNAME + "=username&"
                + SnmpDriverSettingVariableNames.SECURITYNAME + "=username:"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);

        settings = SnmpDriverSettingVariableNames.USERNAME + ":username&" + SnmpDriverSettingVariableNames.SECURITYNAME
                + "=username:" + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=pass:"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=pass";
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);
        snmpDriver.connect("1.1.1.1/1", settings);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testInvalidDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect("1.1.1.1:1", correctSetting);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testNullDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect(null, correctSetting);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testEmptyDeviceAddress() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect("", correctSetting);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testIncorrectSnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect("1.1.1.1/1", correctSetting);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testNullSnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect("1.1.1.1/1", correctSetting);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testEmptySnmpVersoin() throws ConnectionException, ArgumentSyntaxException {
        snmpDriver.connect("1.1.1.1/1", correctSetting);
    }

    @AfterClass
    public static void afterClass() {
        snmpDriver = null;
    }
}
