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
