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

package org.openmuc.framework.core.datamanager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class DeviceConfigImpl implements DeviceConfig {

    private String id;
    private String description;
    private String deviceAddress;
    private String settings;

    private Integer samplingTimeout;
    private Integer connectRetryInterval;
    private Boolean disabled;

    Device device;

    final HashMap<String, ChannelConfigImpl> channelConfigsById = new LinkedHashMap<>();

    DriverConfigImpl driverParent;

    public DeviceConfigImpl(String id, DriverConfigImpl driverParent) {
        this.id = id;
        this.driverParent = driverParent;
    }

    DeviceConfigImpl clone(DriverConfigImpl clonedParentConfig) {
        DeviceConfigImpl configClone = new DeviceConfigImpl(id, clonedParentConfig);

        configClone.description = description;
        configClone.deviceAddress = deviceAddress;
        configClone.settings = settings;
        configClone.samplingTimeout = samplingTimeout;
        configClone.connectRetryInterval = connectRetryInterval;
        configClone.disabled = disabled;

        for (ChannelConfigImpl channelConfig : channelConfigsById.values()) {
            configClone.channelConfigsById.put(channelConfig.getId(), channelConfig.clone(configClone));
        }
        return configClone;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) throws IdCollisionException {
        if (id == null) {
            throw new IllegalArgumentException("The device ID may not be null");
        }
        ChannelConfigImpl.checkIdSyntax(id);

        if (driverParent.rootConfigParent.deviceConfigsById.containsKey(id)) {
            throw new IdCollisionException("Collision with device ID:" + id);
        }

        driverParent.deviceConfigsById.put(id, driverParent.deviceConfigsById.remove(this.id));
        driverParent.rootConfigParent.deviceConfigsById.put(id,
                driverParent.rootConfigParent.deviceConfigsById.remove(this.id));

        this.id = id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public void setDeviceAddress(String address) {
        deviceAddress = address;
    }

    @Override
    public String getSettings() {
        return settings;
    }

    @Override
    public void setSettings(String settings) {
        this.settings = settings;
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
    public ChannelConfig addChannel(String channelId) throws IdCollisionException {

        if (channelId == null) {
            throw new IllegalArgumentException("The channel ID may not be null");
        }

        ChannelConfigImpl.checkIdSyntax(channelId);

        if (driverParent.rootConfigParent.channelConfigsById.containsKey(channelId)) {
            throw new IdCollisionException("Collision with channel ID: " + channelId);
        }

        ChannelConfigImpl newChannel = new ChannelConfigImpl(channelId, this);

        driverParent.rootConfigParent.channelConfigsById.put(channelId, newChannel);
        channelConfigsById.put(channelId, newChannel);
        return newChannel;
    }

    @Override
    public ChannelConfig getChannel(String channelId) {
        return channelConfigsById.get(channelId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ChannelConfig> getChannels() {
        return (Collection<ChannelConfig>) (Collection<?>) Collections
                .unmodifiableCollection(channelConfigsById.values());
    }

    @Override
    public void delete() {
        driverParent.deviceConfigsById.remove(id);
        clear();
    }

    void clear() {
        for (ChannelConfigImpl channelConfig : channelConfigsById.values()) {
            channelConfig.clear();
        }
        channelConfigsById.clear();
        driverParent.rootConfigParent.deviceConfigsById.remove(id);
        driverParent = null;
    }

    @Override
    public DriverConfig getDriver() {
        return driverParent;
    }

    static void addDeviceFromDomNode(Node deviceConfigNode, DriverConfig parentConfig) throws ParseException {

        String id = ChannelConfigImpl.getAttributeValue(deviceConfigNode, "id");
        if (id == null) {
            throw new ParseException("device has no id attribute");
        }

        DeviceConfigImpl config;
        try {
            config = (DeviceConfigImpl) parentConfig.addDevice(id);
        } catch (Exception e) {
            throw new ParseException(e);
        }

        NodeList deviceChildren = deviceConfigNode.getChildNodes();

        try {
            for (int i = 0; i < deviceChildren.getLength(); i++) {
                Node childNode = deviceChildren.item(i);
                String childName = childNode.getNodeName();

                if (childName.equals("#text")) {
                    continue;
                }
                else if (childName.equals("channel")) {
                    ChannelConfigImpl.addChannelFromDomNode(childNode, config);
                }
                else if (childName.equals("description")) {
                    config.setDescription(childNode.getTextContent());
                }
                else if (childName.equals("deviceAddress")) {
                    config.setDeviceAddress(childNode.getTextContent());
                }
                else if (childName.equals("settings")) {
                    config.setSettings(childNode.getTextContent());
                }
                else if (childName.equals("samplingTimeout")) {
                    config.setSamplingTimeout(ChannelConfigImpl.timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("connectRetryInterval")) {
                    config.setConnectRetryInterval(ChannelConfigImpl.timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("disabled")) {
                    config.disabled = Boolean.parseBoolean(childNode.getTextContent());
                }
                else {
                    throw new ParseException("found unknown tag:" + childName);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        }

    }

    Element getDomElement(Document document) {
        Element parentElement = document.createElement("device");
        parentElement.setAttribute("id", id);

        Element childElement;

        if (description != null) {
            childElement = document.createElement("description");
            childElement.setTextContent(description);
            parentElement.appendChild(childElement);
        }

        if (deviceAddress != null) {
            childElement = document.createElement("deviceAddress");
            childElement.setTextContent(deviceAddress);
            parentElement.appendChild(childElement);
        }

        if (settings != null) {
            childElement = document.createElement("settings");
            childElement.setTextContent(settings);
            parentElement.appendChild(childElement);
        }

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
            if (disabled) {
                childElement.setTextContent("true");
            }
            else {
                childElement.setTextContent("false");
            }
            parentElement.appendChild(childElement);
        }

        for (ChannelConfigImpl channelConfig : channelConfigsById.values()) {
            parentElement.appendChild(channelConfig.getDomElement(document));
        }

        return parentElement;
    }

    DeviceConfigImpl cloneWithDefaults(DriverConfigImpl clonedParentConfig) {

        DeviceConfigImpl configClone = new DeviceConfigImpl(id, clonedParentConfig);

        if (description == null) {
            configClone.description = DESCRIPTION_DEFAULT;
        }
        else {
            configClone.description = description;
        }

        if (deviceAddress == null) {
            configClone.deviceAddress = DEVICE_ADDRESS_DEFAULT;
        }
        else {
            configClone.deviceAddress = deviceAddress;
        }

        if (settings == null) {
            configClone.settings = SETTINGS_DEFAULT;
        }
        else {
            configClone.settings = settings;
        }

        if (samplingTimeout == null) {
            configClone.samplingTimeout = clonedParentConfig.samplingTimeout;
        }
        else {
            configClone.samplingTimeout = samplingTimeout;
        }

        if (connectRetryInterval == null) {
            configClone.connectRetryInterval = clonedParentConfig.connectRetryInterval;
        }
        else {
            configClone.connectRetryInterval = connectRetryInterval;
        }

        if (disabled == null || clonedParentConfig.disabled) {
            configClone.disabled = clonedParentConfig.disabled;
        }
        else {
            configClone.disabled = disabled;
        }

        for (ChannelConfigImpl channelConfig : channelConfigsById.values()) {
            configClone.channelConfigsById.put(channelConfig.getId(), channelConfig.cloneWithDefaults(configClone));
        }
        return configClone;
    }

}
