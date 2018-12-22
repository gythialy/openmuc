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

package org.openmuc.framework.core.datamanager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.driver.spi.DriverService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class DriverConfigImpl implements DriverConfig {

    String id;
    Integer samplingTimeout = null;
    Integer connectRetryInterval = null;
    Boolean disabled = null;

    final HashMap<String, DeviceConfigImpl> deviceConfigsById = new LinkedHashMap<>();

    RootConfigImpl rootConfigParent;

    DriverService activeDriver = null;

    DriverConfigImpl(String id, RootConfigImpl rootConfigParent) {
        this.id = id;
        this.rootConfigParent = rootConfigParent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) throws IdCollisionException {
        if (id == null) {
            throw new IllegalArgumentException("The driver ID may not be null");
        }
        ChannelConfigImpl.checkIdSyntax(id);

        if (rootConfigParent.driverConfigsById.containsKey(id)) {
            throw new IdCollisionException("Collision with the driver ID:" + id);
        }
        this.rootConfigParent.driverConfigsById.remove(this.id);
        this.rootConfigParent.driverConfigsById.put(id, this);
        this.id = id;
    }

    @Override
    public Integer getSamplingTimeout() {
        return samplingTimeout;
    }

    @Override
    public void setSamplingTimeout(Integer timeout) {
        if (timeout != null && timeout < 0) {
            throw new IllegalArgumentException("A negative sampling timeout is not allowed");
        }
        samplingTimeout = timeout;
    }

    @Override
    public Integer getConnectRetryInterval() {
        return connectRetryInterval;
    }

    @Override
    public void setConnectRetryInterval(Integer interval) {
        if (interval != null && interval < 0) {
            throw new IllegalArgumentException("A negative connect retry interval is not allowed");
        }
        connectRetryInterval = interval;
    }

    @Override
    public Boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public DeviceConfig addDevice(String deviceId) throws IdCollisionException {

        if (deviceId == null) {
            throw new IllegalArgumentException("The device ID may not be null");
        }

        ChannelConfigImpl.checkIdSyntax(deviceId);

        if (rootConfigParent.deviceConfigsById.containsKey(deviceId)) {
            throw new IdCollisionException("Collision with device ID: " + deviceId);
        }

        DeviceConfigImpl newDevice = new DeviceConfigImpl(deviceId, this);

        rootConfigParent.deviceConfigsById.put(deviceId, newDevice);
        deviceConfigsById.put(deviceId, newDevice);

        return newDevice;
    }

    @Override
    public DeviceConfig getDevice(String deviceId) {
        return deviceConfigsById.get(deviceId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DeviceConfig> getDevices() {
        return (Collection<DeviceConfig>) (Collection<?>) Collections
                .unmodifiableCollection(deviceConfigsById.values());
    }

    @Override
    public void delete() {
        rootConfigParent.driverConfigsById.remove(id);
        for (DeviceConfigImpl deviceConfig : deviceConfigsById.values()) {
            deviceConfig.clear();
        }
        deviceConfigsById.clear();
        rootConfigParent = null;
    }

    static void addDriverFromDomNode(Node driverConfigNode, RootConfigImpl parentConfig) throws ParseException {

        String id = ChannelConfigImpl.getAttributeValue(driverConfigNode, "id");
        if (id == null) {
            throw new ParseException("driver has no id attribute");
        }

        DriverConfigImpl config;
        try {
            config = parentConfig.addDriver(id);
        } catch (IdCollisionException e) {
            throw new ParseException(e);
        }

        parseDiverNode(driverConfigNode, config);
    }

    private static void parseDiverNode(Node driverConfigNode, DriverConfigImpl config) throws ParseException {
        NodeList driverChildren = driverConfigNode.getChildNodes();

        try {
            for (int j = 0; j < driverChildren.getLength(); j++) {
                Node childNode = driverChildren.item(j);
                String childName = childNode.getNodeName();

                switch (childName) {
                case "#text":
                    continue;

                case "device":
                    DeviceConfigImpl.addDeviceFromDomNode(childNode, config);
                    break;

                case "samplingTimeout":
                    config.setSamplingTimeout(ChannelConfigImpl.timeStringToMillis(childNode.getTextContent()));
                    break;

                case "connectRetryInterval":
                    config.setConnectRetryInterval(ChannelConfigImpl.timeStringToMillis(childNode.getTextContent()));
                    break;

                case "disabled":
                    String disabledString = childNode.getTextContent();
                    config.disabled = Boolean.parseBoolean(disabledString);
                    break;
                default:
                    throw new ParseException("found unknown tag:" + childName);
                }

            }
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        }
    }

    Element getDomElement(Document document) {
        Element parentElement = document.createElement("driver");
        parentElement.setAttribute("id", id);

        Element childElement;

        if (samplingTimeout != null) {
            childElement = document.createElement("samplingTimeout");
            childElement.setTextContent(ChannelConfigImpl.millisToTimeString(samplingTimeout));
            parentElement.appendChild(childElement);
        }

        if (connectRetryInterval != null) {
            childElement = document.createElement("connectRetryInterval");
            childElement.setTextContent(ChannelConfigImpl.millisToTimeString(connectRetryInterval));
            parentElement.appendChild(childElement);
        }

        if (disabled != null) {
            childElement = document.createElement("disabled");
            childElement.setTextContent(disabled.toString());
            parentElement.appendChild(childElement);
        }

        for (DeviceConfigImpl deviceConfig : deviceConfigsById.values()) {
            parentElement.appendChild(deviceConfig.getDomElement(document));
        }

        return parentElement;
    }

    DriverConfigImpl clone(RootConfigImpl clonedParentConfig) {
        DriverConfigImpl configClone = new DriverConfigImpl(id, clonedParentConfig);

        configClone.samplingTimeout = samplingTimeout;
        configClone.connectRetryInterval = connectRetryInterval;
        configClone.disabled = disabled;

        for (DeviceConfigImpl deviceConfig : deviceConfigsById.values()) {
            configClone.deviceConfigsById.put(deviceConfig.getId(), deviceConfig.clone(configClone));
        }
        return configClone;
    }

    DriverConfigImpl cloneWithDefaults(RootConfigImpl clonedParentConfig) {
        DriverConfigImpl configClone = new DriverConfigImpl(id, clonedParentConfig);

        if (samplingTimeout == null) {
            configClone.samplingTimeout = SAMPLING_TIMEOUT_DEFAULT;
        }
        else {
            configClone.samplingTimeout = samplingTimeout;
        }

        if (connectRetryInterval == null) {
            configClone.connectRetryInterval = CONNECT_RETRY_INTERVAL_DEFAULT;
        }
        else {
            configClone.connectRetryInterval = connectRetryInterval;
        }

        if (disabled == null) {
            configClone.disabled = DISABLED_DEFAULT;
        }
        else {
            configClone.disabled = disabled;
        }

        for (DeviceConfigImpl deviceConfig : deviceConfigsById.values()) {
            configClone.deviceConfigsById.put(deviceConfig.getId(), deviceConfig.cloneWithDefaults(configClone));
        }
        return configClone;
    }

}
