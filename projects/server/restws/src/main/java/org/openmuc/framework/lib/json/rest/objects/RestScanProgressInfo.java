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
package org.openmuc.framework.lib.json.rest.objects;

public class RestScanProgressInfo {
    private int scanProgress = -1;
    private boolean isScanFinished = true;
    private boolean isScanInterrupted = false;
    private String scanError = null;

    public int getScanProgress() {
        return scanProgress;
    }

    public boolean isScanFinished() {
        return isScanFinished;
    }

    public boolean isScanInterrupted() {
        return isScanInterrupted;
    }

    public String getScanError() {
        return scanError;
    }

    public void setScanProgress(int scanProgress) {
        this.scanProgress = scanProgress;
    }

    public void setScanFinished(boolean isScanFinished) {
        this.isScanFinished = isScanFinished;
    }

    public void setScanInterrupted(boolean isScanInterrupted) {
        this.isScanInterrupted = isScanInterrupted;
    }

    public void setScanError(String scanError) {
        this.scanError = scanError;
    }
}
