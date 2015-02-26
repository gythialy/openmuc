package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.config.ChannelConfig;

public class RestChannelConfigMapper {

    public static RestChannelConfig getRestChannelConfig(ChannelConfig cc) {
        RestChannelConfig rcc = new RestChannelConfig();
        rcc.setChannelAddress(cc.getChannelAddress());
        rcc.setDescription(cc.getDescription());
        rcc.setDisabled(cc.isDisabled());
        rcc.setId(cc.getId());
        rcc.setListening(cc.isListening());
        rcc.setLoggingInterval(cc.getLoggingInterval());
        rcc.setLoggingTimeOffset(cc.getLoggingTimeOffset());
        rcc.setSamplingGroup(cc.getSamplingGroup());
        rcc.setSamplingInterval(cc.getSamplingInterval());
        rcc.setSamplingTimeOffset(cc.getSamplingTimeOffset());
        rcc.setScalingFactor(cc.getScalingFactor());
        // rcc.setServerMappings(cc.getServerMappings());
        rcc.setUnit(cc.getUnit());
        rcc.setValueOffset(cc.getValueOffset());
        rcc.setValueType(cc.getValueType());
        rcc.setValueTypeLength(cc.getValueTypeLength());
        return rcc;
    }

    public static void setChannelConfig(ChannelConfig cc, RestChannelConfig rcc) {
        cc.setChannelAddress(rcc.getChannelAddress());
        cc.setDescription(rcc.getDescription());
        cc.setDisabled(rcc.isDisabled());
        // cc.setId(rcc.getId());
        cc.setListening(rcc.isListening());
        cc.setLoggingInterval(rcc.getLoggingInterval());
        cc.setLoggingTimeOffset(rcc.getLoggingTimeOffset());
        cc.setSamplingGroup(rcc.getSamplingGroup());
        cc.setSamplingInterval(rcc.getSamplingInterval());
        cc.setSamplingTimeOffset(rcc.getSamplingTimeOffset());
        cc.setScalingFactor(rcc.getScalingFactor());
        // cc.setServerMappings(rcc.getServerMappings());
        cc.setUnit(rcc.getUnit());
        cc.setValueOffset(rcc.getValueOffset());
        cc.setValueType(rcc.getValueType());
        cc.setValueTypeLength(rcc.getValueTypeLength());
    }

}
