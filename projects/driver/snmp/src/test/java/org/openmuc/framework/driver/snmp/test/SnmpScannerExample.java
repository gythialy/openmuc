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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.snmp.SnmpDriver;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverScanSettingVariableNames;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;

public class SnmpScannerExample {

    /**
     * @param args
     */
    public static void main(String[] args) {

        SnmpDriver myDriver = new SnmpDriver();
        String settings = SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=adminadmin:"
                + SnmpDriverScanSettingVariableNames.STARTIP + "=192.168.1.0:"
                + SnmpDriverScanSettingVariableNames.ENDIP + "=192.168.10.0";

        class TestListener implements DriverDeviceScanListener {

            @Override
            public void scanProgressUpdate(int progress) {
            }

            @Override
            public void deviceFound(DeviceScanInfo device) {
                System.out.println("-----------------------------");
                System.out.println("New device found: ");
                System.out.println("Address: " + device.getDeviceAddress());
                System.out.println("Description: " + device.getDescription());
                System.out.println("-----------------------------");
            }

        }
        ;
        TestListener listener = new TestListener();
        try {
            myDriver.scanForDevices(settings, listener);
            Thread.sleep(100);
        } catch (InterruptedException iex) {
            System.out.println("Request cancelled: " + iex.getMessage());

        } catch (ArgumentSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ScanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ScanInterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
