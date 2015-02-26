package org.openmuc.framework.server.restws.objects;

public class RestDeviceConfig {

    private String id;
    private String description = null;
    private String deviceAddress = null;
    private String settings = null;
    private Integer samplingTimeout = null;
    private Integer connectRetryInterval = null;
    private Boolean disabled = null;

    // Device device = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public Integer getSamplingTimeout() {
        return samplingTimeout;
    }

    public void setSamplingTimeout(Integer samplingTimeout) {
        this.samplingTimeout = samplingTimeout;
    }

    public Integer getConnectRetryInterval() {
        return connectRetryInterval;
    }

    public void setConnectRetryInterval(Integer connectRetryInterval) {
        this.connectRetryInterval = connectRetryInterval;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void isDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

}
