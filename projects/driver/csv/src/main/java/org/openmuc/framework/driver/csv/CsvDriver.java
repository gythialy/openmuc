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
package org.openmuc.framework.driver.csv;

import java.io.File;
import java.util.Arrays;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.csv.settings.DeviceScanSettings;
import org.openmuc.framework.driver.csv.settings.DeviceSettings;
import org.openmuc.framework.driver.csv.settings.GenericSetting;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver to read data from CSV file.
 * <p>
 * Three sampling modes are available:
 * <ul>
 * <li>LINE: starts from begin of file. With every sampling it reads the next line. Timestamps ignored</li>
 * <li>UNIXTIMESTAMP: With every sampling it reads the line with the closest unix timestamp regarding to sampling
 * timestamp</li>
 * <li>HHMMSS: With every sampling it reads the line with the closest time HHMMSS regarding to sampling timestamp</li>
 * </ul>
 */
@Component
public class CsvDriver implements DriverService {

    private static final Logger logger = LoggerFactory.getLogger(CsvDriver.class);

    private static final String DEFAULT_DEVICE_SETTINGS = DeviceSettings.Option.SAMPLINGMODE.name() + "="
            + ESamplingMode.LINE.toString();

    private boolean isDeviceScanInterrupted = false;

    @Override
    public DriverInfo getInfo() {

        final String ID = "csv";
        final String DESCRIPTION = "Driver to read out csv files.";
        final String DEVICE_ADDRESS = "csv file path e.g. /home/usr/bin/openmuc/csv/meter.csv";
        final String DEVICE_SETTINGS = GenericSetting.syntax(DeviceSettings.class) + "\n samplingmode: "
                + Arrays.toString(ESamplingMode.values()).toLowerCase()
                + " Example: samplingmode=line;rewind=true Default: " + DEFAULT_DEVICE_SETTINGS.toLowerCase();
        final String CHANNEL_ADDRESS = "column header";
        final String DEVICE_SCAN_SETTINGS = GenericSetting.syntax(DeviceScanSettings.class)
                + " path of directory containing csv files e.g: path=/home/usr/openmuc/framework/csv-driver/";

        return new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, DEVICE_SETTINGS, CHANNEL_ADDRESS, DEVICE_SCAN_SETTINGS);

    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        logger.info("Scan for CSV files. Settings: {}", settings);

        resetDeviceScanInterrupted();

        final DeviceScanSettings deviceScanSettings = new DeviceScanSettings(settings);
        final File[] listOfFiles = deviceScanSettings.path().listFiles();

        if (listOfFiles != null) {

            final double numberOfFiles = listOfFiles.length;
            double fileCounter = 0;
            int idCounter = 0;

            for (File file : listOfFiles) {

                // check if device scan was interrupted
                if (isDeviceScanInterrupted) {
                    break;
                }

                if (file.isFile()) {
                    if (file.getName().endsWith("csv")) {

                        String deviceId = "csv_device_" + idCounter;

                        listener.deviceFound(new DeviceScanInfo(deviceId, file.getAbsolutePath(),
                                DEFAULT_DEVICE_SETTINGS.toLowerCase(), file.getName()));
                    } // else: do nothing, non csv files are ignored
                } // else: do nothing, folders are ignored

                fileCounter++;
                listener.scanProgressUpdate((int) (fileCounter / numberOfFiles * 100.0));
                idCounter++;
            }
        }
    }

    private void resetDeviceScanInterrupted() {
        isDeviceScanInterrupted = false;
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        isDeviceScanInterrupted = true;
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        File csvFile = new File(deviceAddress);
        if (!csvFile.exists()) {
            throw new ArgumentSyntaxException("CSV driver - file not found: " + deviceAddress);
        }

        CsvDeviceConnection csvConnection = new CsvDeviceConnection(deviceAddress, settings);
        logger.info("Device connected: {}", deviceAddress);
        return csvConnection;
    }

}
