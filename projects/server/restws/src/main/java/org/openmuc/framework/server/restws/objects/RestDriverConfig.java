package org.openmuc.framework.server.restws.objects;

public class RestDriverConfig {

    private String id;
    private Integer samplingTimeout = null;
    private Integer connectRetryInterval = null;
    private Boolean disabled = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}
