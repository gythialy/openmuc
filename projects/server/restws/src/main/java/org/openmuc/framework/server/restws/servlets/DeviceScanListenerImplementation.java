package org.openmuc.framework.server.restws.servlets;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DeviceScanListener;
import org.openmuc.framework.lib.json.restObjects.RestScanProgressInfo;

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
    synchronized public void scanFinished() {
        notify();
        restScanProgressInfo.setScanFinished(true);
    }

    @Override
    synchronized public void scanInterrupted() {
        notify();
        restScanProgressInfo.setScanInterrupted(true);
    }

    @Override
    synchronized public void scanError(String message) {
        notify();
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
            }
        }
        return scannedDevicesList;
    }

}
