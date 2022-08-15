/*
 * Copyright 2011-2022 Fraunhofer ISE
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
package org.openmuc.framework.driver.csv;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;

public class CsvScanDeviceTest {

    private final String dir = System.getProperty("user.dir");

    @Test
    public void testDeviceScan()
            throws ArgumentSyntaxException, UnsupportedOperationException, ScanException, ScanInterruptedException {
        CsvDriver csvDriver = new CsvDriver();

        String settings = "path=" + dir + "/src/test/resources";

        csvDriver.scanForDevices(settings, new DriverDeviceScanListener() {

            @Override
            public void scanProgressUpdate(int progress) {
                System.out.println("Scan progress: " + progress + " %");
            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                System.out.println(scanInfo.toString());
            }
        });
    }

    @Test
    public void testChannelScan() {
        CsvDriver csvDriver = new CsvDriver();

        String deviceAddress = dir + "/src/test/resources/test_data.csv";

        try {

            String settings = "SAMPLINGMODE=hhmmss";
            CsvDeviceConnection csvConnection = (CsvDeviceConnection) csvDriver.connect(deviceAddress, settings);

            List<ChannelScanInfo> channelsScanInfos = csvConnection.scanForChannels("");

            for (ChannelScanInfo info : channelsScanInfos) {
                System.out.println("Channel: " + info.getChannelAddress());
            }

        } catch (ArgumentSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ScanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
