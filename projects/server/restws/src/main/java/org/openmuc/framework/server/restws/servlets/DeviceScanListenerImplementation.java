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
package org.openmuc.framework.server.restws.servlets;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DeviceScanListener;
import org.openmuc.framework.lib.rest1.rest.objects.RestScanProgressInfo;

class DeviceScanListenerImplementation implements DeviceScanListener {
    private final RestScanProgressInfo restScanProgressInfo = new RestScanProgressInfo();

    private final List<DeviceScanInfo> scannedDevicesList;

    DeviceScanListenerImplementation() {
        scannedDevicesList = new ArrayList<>();
    }

    DeviceScanListenerImplementation(List<DeviceScanInfo> scannedDevicesList) {
        restScanProgressInfo.setScanFinished(false);
        this.scannedDevicesList = scannedDevicesList;
    }

    @Override
    public void deviceFound(DeviceScanInfo scanInfo) {
        scannedDevicesList.add(scanInfo);
    }

    @Override
    public void scanProgress(int scanProgress) {
        restScanProgressInfo.setScanProgress(scanProgress);
    }

    @Override
    public synchronized void scanFinished() {
        notifyAll();
        restScanProgressInfo.setScanFinished(true);
    }

    @Override
    public synchronized void scanInterrupted() {
        notifyAll();
        restScanProgressInfo.setScanInterrupted(true);
    }

    @Override
    public synchronized void scanError(String message) {
        notifyAll();
        restScanProgressInfo.setScanError(message);
    }

    RestScanProgressInfo getRestScanProgressInfo() {
        return restScanProgressInfo;
    }

    synchronized List<DeviceScanInfo> getScannedDevicesList() {
        while (!restScanProgressInfo.isScanFinished() && !restScanProgressInfo.isScanInterrupted()
                && restScanProgressInfo.getScanError() == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return scannedDevicesList;
    }

}
