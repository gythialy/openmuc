package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.config.DriverConfig;

public class RestDriverConfigMapper {

    public static RestDriverConfig getRestDriverConfig(DriverConfig dc) {
        RestDriverConfig rdc = new RestDriverConfig();
        rdc.setId(dc.getId());
        rdc.setConnectRetryInterval(dc.getConnectRetryInterval());
        rdc.setDisabled(dc.isDisabled());
        rdc.setSamplingTimeout(dc.getSamplingTimeout());
        return rdc;
    }

    public static void setDriverConfig(DriverConfig dc, RestDriverConfig rdc) {
        // dc.setId(rdc.getId());
        dc.setConnectRetryInterval(rdc.getConnectRetryInterval());
        dc.setDisabled(rdc.isDisabled());
        dc.setSamplingTimeout(rdc.getSamplingTimeout());
    }
}
