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
package org.openmuc.framework.driver.snmp.test;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.snmp.SnmpDriver;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice.SNMPVersion;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;

public class UsecaseExample {

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {

            SnmpDriver snmpDriver = new SnmpDriver();
            // SNMPVersion=V2c:COMMUNITY=root:SECURITYNAME=root:AUTHENTICATIONPASSPHRASE=adminadmin:PRIVACYPASSPHRASE=adminadmin
            String settings = SnmpDriverSettingVariableNames.SNMP_VERSION + "=" + SNMPVersion.V2c + ":"
                    + SnmpDriverSettingVariableNames.USERNAME + "=root:" + SnmpDriverSettingVariableNames.SECURITYNAME
                    + "=root:" + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=adminadmin:"
                    + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=adminadmin";
            System.out.println(settings);
            SnmpDevice myDevice = (SnmpDevice) snmpDriver.connect("192.168.1.1/161", settings);

            List<ChannelRecordContainer> containers = new ArrayList<>();

            SnmpChannel ch1 = new SnmpChannel("192.168.1.1/161", "1.3.6.1.2.1.1.1.0");
            SnmpChannel ch2 = new SnmpChannel("192.168.1.1/161", "1.3.6.1.2.1.25.1.1.0");
            SnmpChannel ch3 = new SnmpChannel("192.168.1.1/161", "1.3.6.1.2.1.1.5.0");
            containers.add(new SnmpChannelRecordContainer(ch1));
            containers.add(new SnmpChannelRecordContainer(ch2));
            containers.add(new SnmpChannelRecordContainer(ch3));

            myDevice.read(containers, null, null);

            for (ChannelRecordContainer container : containers) {
                if (container.getRecord() != null) {
                    System.out.println(container.getRecord().getValue());
                }
            }

        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ArgumentSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
