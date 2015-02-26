package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.config.ServerMapping;
import org.openmuc.framework.data.ValueType;

import java.util.List;

public class RestChannelConfig {

    private String id = null;
    private String channelAddress = null;
    private String description = null;
    private String unit = null;
    private ValueType valueType = null;
    private Integer valueTypeLength = null;
    private Double scalingFactor = null;
    private Double valueOffset = null;
    private Boolean listening = null;
    private Integer samplingInterval = null;
    private Integer samplingTimeOffset = null;
    private String samplingGroup = null;
    private Integer loggingInterval = null;
    private Integer loggingTimeOffset = null;
    private Boolean disabled = null;
    private List<ServerMapping> serverMappings = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelAddress() {
        return channelAddress;
    }

    public void setChannelAddress(String channelAddress) {
        this.channelAddress = channelAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public Integer getValueTypeLength() {
        return valueTypeLength;
    }

    public void setValueTypeLength(Integer valueTypeLength) {
        this.valueTypeLength = valueTypeLength;
    }

    public Double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(Double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public Double getValueOffset() {
        return valueOffset;
    }

    public void setValueOffset(Double valueOffset) {
        this.valueOffset = valueOffset;
    }

    public Boolean isListening() {
        return listening;
    }

    public void setListening(Boolean listening) {
        this.listening = listening;
    }

    public Integer getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingInterval(Integer samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    public Integer getSamplingTimeOffset() {
        return samplingTimeOffset;
    }

    public void setSamplingTimeOffset(Integer samplingTimeOffset) {
        this.samplingTimeOffset = samplingTimeOffset;
    }

    public String getSamplingGroup() {
        return samplingGroup;
    }

    public void setSamplingGroup(String samplingGroup) {
        this.samplingGroup = samplingGroup;
    }

    public Integer getLoggingInterval() {
        return loggingInterval;
    }

    public void setLoggingInterval(Integer loggingInterval) {
        this.loggingInterval = loggingInterval;
    }

    public Integer getLoggingTimeOffset() {
        return loggingTimeOffset;
    }

    public void setLoggingTimeOffset(Integer loggingTimeOffset) {
        this.loggingTimeOffset = loggingTimeOffset;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public List<ServerMapping> getServerMappings() {
        return serverMappings;
    }

    public void setServerMappings(List<ServerMapping> serverMappings) {
        this.serverMappings = serverMappings;
    }
}
