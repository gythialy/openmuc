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
package org.openmuc.framework.core.datamanager;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DeviceScanListener;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;

public class ScanForDevicesTask implements Runnable {
    private final DriverService driver;
    private final String settings;
    private final DeviceScanListener listener;

    public ScanForDevicesTask(DriverService driver, String settings, DeviceScanListener listener) {
        this.driver = driver;
        this.settings = settings;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            driver.scanForDevices(settings, new NonBlockingScanListener(listener));
        } catch (UnsupportedOperationException e) {
            listener.scanError("Device scan not supported by driver");
            return;
        } catch (ArgumentSyntaxException e) {
            listener.scanError("Scan settings syntax invalid: " + e.getMessage());
            return;
        } catch (ScanException e) {
            listener.scanError("IOException while scanning: " + e.getMessage());
            return;
        } catch (ScanInterruptedException e) {
            listener.scanInterrupted();
            return;
        }
        listener.scanFinished();
    }

    class NonBlockingScanListener implements DriverDeviceScanListener {
        List<DeviceScanInfo> scanInfos = new ArrayList<>();
        DeviceScanListener listener;

        public NonBlockingScanListener(DeviceScanListener listener) {
            this.listener = listener;
        }

        @Override
        public void scanProgressUpdate(int progress) {
            listener.scanProgress(progress);
        }

        @Override
        public void deviceFound(DeviceScanInfo scanInfo) {
            if (!scanInfos.contains(scanInfo)) {
                scanInfos.add(scanInfo);
                listener.deviceFound(scanInfo);
            }
        }
    }

}
