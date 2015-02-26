package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.config.DeviceConfig;

public class RestDeviceConfigMapper {

    public static RestDeviceConfig getRestDeviceConfig(DeviceConfig dc) {
        RestDeviceConfig rdc = new RestDeviceConfig();
        rdc.setConnectRetryInterval(dc.getConnectRetryInterval());
        rdc.setDescription(dc.getDescription());
        rdc.setDeviceAddress(dc.getDeviceAddress());
        rdc.isDisabled(dc.isDisabled());
        // rdc.setId(dc.getId());
        rdc.setSamplingTimeout(dc.getSamplingTimeout());
        rdc.setSettings(dc.getSettings());
        return rdc;
    }

    public static void setDeviceConfig(DeviceConfig dc, RestDeviceConfig rdc) {
        dc.setConnectRetryInterval(rdc.getConnectRetryInterval());
        dc.setDescription(rdc.getDescription());
        dc.setDeviceAddress(rdc.getDeviceAddress());
        dc.setDisabled(rdc.getDisabled());
        // dc.setId(rdc.getId());
        dc.setSamplingTimeout(rdc.getSamplingTimeout());
        dc.setSettings(rdc.getSettings());
    }
}
