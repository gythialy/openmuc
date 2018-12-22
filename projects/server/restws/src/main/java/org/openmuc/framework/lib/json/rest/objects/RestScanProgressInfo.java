package org.openmuc.framework.lib.json.rest.objects;

public class RestScanProgressInfo {
    private int scanProgress = -1;
    private boolean isScanFinished = true;
    private boolean isScanInterrupted = false;
    private String scanError = null;

    public int getScanProgress() {
        return scanProgress;
    }

    public void setScanProgress(int scanProgress) {
        this.scanProgress = scanProgress;
    }

    public boolean isScanFinished() {
        return isScanFinished;
    }

    public void setScanFinished(boolean isScanFinished) {
        this.isScanFinished = isScanFinished;
    }

    public boolean isScanInterrupted() {
        return isScanInterrupted;
    }

    public void setScanInterrupted(boolean isScanInterrupted) {
        this.isScanInterrupted = isScanInterrupted;
    }

    public String getScanError() {
        return scanError;
    }

    public void setScanError(String scanError) {
        this.scanError = scanError;
    }
}
