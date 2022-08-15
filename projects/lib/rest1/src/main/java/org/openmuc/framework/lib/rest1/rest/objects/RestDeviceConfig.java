/*
 * Copyright 2011-2022 Fraunhofer ISE
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
package org.openmuc.framework.lib.rest1.rest.objects;

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
