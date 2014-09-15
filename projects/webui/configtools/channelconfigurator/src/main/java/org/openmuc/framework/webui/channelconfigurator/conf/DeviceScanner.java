/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.framework.webui.channelconfigurator.conf;

import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DeviceScanListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard Device Scanner to use for scanning and collecting a List of DeviceScanInformation with the OpenMUC API.
 */
public class DeviceScanner {
    private String scanErrorMessage;
    private final String driverId;
    private boolean scanError;
    private boolean scanFinished;
    private boolean scanInterrupted;
    private int scanProcessPercentage;
    private final List<DeviceScanInfo> deviceScanResult;
    private List<DeviceScanInfo> newDevices;
    private final DeviceScanListener listener;

    public DeviceScanner(String driverId) {
        this.driverId = driverId;
        scanError = false;
        scanFinished = false;
        scanInterrupted = false;
        deviceScanResult = new ArrayList<DeviceScanInfo>();
        newDevices = new ArrayList<DeviceScanInfo>();

        listener = new DeviceScanListener() {

            @Override
            public void scanProgress(int progress) {
                scanProcessPercentage = progress;
            }

            @Override
            public void scanFinished() {
                scanFinished = true;
            }

            @Override
            public void scanError(String message) {
                scanError = true;
                scanErrorMessage = message;
            }

            @Override
            public void scanInterrupted() {
                scanInterrupted = true;
            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                deviceScanResult.add(scanInfo);
                newDevices.add(scanInfo);
            }

        };
    }

    /**
     * Get the scan error state.
     *
     * @return true if driver reported a scan error.
     */
    public boolean isScanError() {
        return scanError;
    }

    /**
     * Get the scan state.
     *
     * @return true if scan is finished.
     */
    public boolean isScanFinished() {
        return scanFinished;
    }

    /**
     * Get the scan interrupt state.
     *
     * @return true if scan is interrupted
     */
    public boolean isScanInterrupted() {
        return scanInterrupted;
    }

    /**
     * Get the current scan progress.
     *
     * @return 0 to 100 as percentual progress.
     */
    public int getScanProgress() {
        return scanProcessPercentage;
    }

    /**
     * Get the scan error message
     *
     * @return the scan error message
     */
    public String getScanErrorMessage() {
        return scanErrorMessage;
    }

    /**
     * Get a list of all already found, scanned devices.
     *
     * @return list of scanned devices.
     */
    public List<DeviceScanInfo> getScannedDevices() {
        return deviceScanResult;
    }

    /**
     * Gets a list of new devices since last call of getNewDevices. Empty if no new Devices have been found. Empty if
     * found devices have already been returned.
     *
     * @return a list of new devices since last call of getNewDevices.
     */
    public synchronized List<DeviceScanInfo> getNewDevices() {
        List<DeviceScanInfo> toReturn = newDevices;
        newDevices = new ArrayList<DeviceScanInfo>();
        return toReturn;
    }

    /**
     * Get the DeviceScanListener for the OpenMUC ConfigService. <br>
     * use configService.scanForDevices(..., deviceScanner.getListener());
     *
     * @return the internal DeviceScanListener Implementation
     */
    public DeviceScanListener getListener() {
        return listener;
    }

    /**
     * The driverID that this driverScanner is scanning.
     *
     * @return the driverID
     */
    public Object getDriverId() {
        return driverId;
    }
}
