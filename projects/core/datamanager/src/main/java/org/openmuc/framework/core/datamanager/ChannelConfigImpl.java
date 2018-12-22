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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.config.ServerMapping;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ChannelConfigImpl implements ChannelConfig, LogChannel {
    private static final Pattern timePattern = Pattern.compile("^([0-9]+)(ms|s|m|h)?$");

    private String id;
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

    ChannelImpl channel;
    DeviceConfigImpl deviceParent;

    ChannelState state;

    ChannelConfigImpl(String id, DeviceConfigImpl deviceParent) {
        this.id = id;
        this.deviceParent = deviceParent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) throws IdCollisionException {
        if (id == null) {
            throw new IllegalArgumentException("The channel ID may not be null");
        }
        ChannelConfigImpl.checkIdSyntax(id);

        if (deviceParent.driverParent.rootConfigParent.channelConfigsById.containsKey(id)) {
            throw new IdCollisionException("Collision with channel ID:" + id);
        }

        deviceParent.channelConfigsById.put(id, deviceParent.channelConfigsById.remove(this.id));
        deviceParent.driverParent.rootConfigParent.channelConfigsById.put(id,
                deviceParent.driverParent.rootConfigParent.channelConfigsById.remove(this.id));

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
    public String getChannelAddress() {
        return channelAddress;
    }

    @Override
    public void setChannelAddress(String address) {
        channelAddress = address;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public Integer getValueTypeLength() {
        return valueTypeLength;
    }

    @Override
    public void setValueTypeLength(Integer length) {
        valueTypeLength = length;
    }

    @Override
    public Double getScalingFactor() {
        return scalingFactor;
    }

    @Override
    public void setScalingFactor(Double factor) {
        scalingFactor = factor;
    }

    @Override
    public Double getValueOffset() {
        return valueOffset;
    }

    @Override
    public void setValueOffset(Double offset) {
        valueOffset = offset;
    }

    @Override
    public Boolean isListening() {
        return listening;
    }

    @Override
    public void setListening(Boolean listening) {
        if (samplingInterval != null && listening != null && listening && samplingInterval > 0) {
            throw new IllegalStateException("Listening may not be enabled while sampling is enabled.");
        }
        this.listening = listening;
    }

    @Override
    public Integer getSamplingInterval() {
        return samplingInterval;
    }

    @Override
    public void setSamplingInterval(Integer samplingInterval) {
        if (listening != null && samplingInterval != null && isListening() && samplingInterval > 0) {
            throw new IllegalStateException("Sampling may not be enabled while listening is enabled.");
        }
        this.samplingInterval = samplingInterval;
    }

    @Override
    public Integer getSamplingTimeOffset() {
        return samplingTimeOffset;
    }

    @Override
    public void setSamplingTimeOffset(Integer samplingTimeOffset) {
        if (samplingTimeOffset != null && samplingTimeOffset < 0) {
            throw new IllegalArgumentException("The sampling time offset may not be negative.");
        }
        this.samplingTimeOffset = samplingTimeOffset;
    }

    @Override
    public String getSamplingGroup() {
        return samplingGroup;
    }

    @Override
    public void setSamplingGroup(String group) {
        samplingGroup = group;
    }

    @Override
    public Integer getLoggingInterval() {
        return loggingInterval;
    }

    @Override
    public void setLoggingInterval(Integer loggingInterval) {
        this.loggingInterval = loggingInterval;
    }

    @Override
    public Integer getLoggingTimeOffset() {
        return loggingTimeOffset;
    }

    @Override
    public void setLoggingTimeOffset(Integer loggingTimeOffset) {
        if (loggingTimeOffset != null && loggingTimeOffset < 0) {
            throw new IllegalArgumentException("The logging time offset may not be negative.");
        }
        this.loggingTimeOffset = loggingTimeOffset;
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
    public void delete() {
        deviceParent.channelConfigsById.remove(id);
        clear();
    }

    @Override
    public List<ServerMapping> getServerMappings() {
        if (serverMappings != null) {
            return this.serverMappings;
        }
        else {
            return new ArrayList<>();
        }
    }

    void clear() {
        deviceParent.driverParent.rootConfigParent.channelConfigsById.remove(id);
        deviceParent = null;
    }

    @Override
    public DeviceConfig getDevice() {
        return deviceParent;
    }

    static void addChannelFromDomNode(Node channelConfigNode, DeviceConfig parentConfig) throws ParseException {

        String id = ChannelConfigImpl.getAttributeValue(channelConfigNode, "id");
        if (id == null) {
            throw new ParseException("channel has no id attribute");
        }

        ChannelConfigImpl config;

        try {
            config = (ChannelConfigImpl) parentConfig.addChannel(id);
        } catch (Exception e) {
            throw new ParseException(e);
        }

        NodeList channelChildren = channelConfigNode.getChildNodes();

        try {
            for (int i = 0; i < channelChildren.getLength(); i++) {
                Node childNode = channelChildren.item(i);
                String childName = childNode.getNodeName();

                if (childName.equals("#text")) {
                    continue;
                }
                else if (childName.equals("description")) {
                    config.setDescription(childNode.getTextContent());
                }
                else if (childName.equals("channelAddress")) {
                    config.setChannelAddress(childNode.getTextContent());
                }
                else if (childName.equals("serverMapping")) {
                    NamedNodeMap attributes = childNode.getAttributes();
                    Node nameAttribute = attributes.getNamedItem("id");

                    if (nameAttribute != null) {
                        config.addServerMapping(
                                new ServerMapping(nameAttribute.getTextContent(), childNode.getTextContent()));
                    }
                    else {
                        throw new ParseException("No id attribute specified for serverMapping.");
                    }
                }
                else if (childName.equals("unit")) {
                    config.setUnit(childNode.getTextContent());
                }
                else if (childName.equals("valueType")) {
                    String valueTypeString = childNode.getTextContent().toUpperCase();

                    try {
                        config.valueType = ValueType.valueOf(valueTypeString);
                    } catch (IllegalArgumentException e) {
                        throw new ParseException("found unknown channel value type:" + valueTypeString);
                    }

                    if (config.valueType == ValueType.BYTE_ARRAY || config.valueType == ValueType.STRING) {
                        String valueTypeLengthString = getAttributeValue(childNode, "length");
                        if (valueTypeLengthString == null) {
                            throw new ParseException(
                                    "length of " + config.valueType.toString() + " value type was not specified");
                        }
                        config.valueTypeLength = timeStringToMillis(valueTypeLengthString);
                    }

                }
                else if (childName.equals("scalingFactor")) {
                    config.setScalingFactor(Double.parseDouble(childNode.getTextContent()));
                }
                else if (childName.equals("valueOffset")) {
                    config.setValueOffset(Double.parseDouble(childNode.getTextContent()));
                }
                else if (childName.equals("listening")) {
                    String listeningString = childNode.getTextContent().toLowerCase();
                    if (listeningString.equals("true")) {
                        config.setListening(true);
                    }
                    else if (listeningString.equals("false")) {
                        config.setListening(false);
                    }
                    else {
                        throw new ParseException("\"listening\" tag contains neither \"true\" nor \"false\"");
                    }
                }
                else if (childName.equals("samplingInterval")) {
                    config.setSamplingInterval(timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("samplingTimeOffset")) {
                    config.setSamplingTimeOffset(timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("samplingGroup")) {
                    config.setSamplingGroup(childNode.getTextContent());
                }
                else if (childName.equals("loggingInterval")) {
                    config.setLoggingInterval(timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("loggingTimeOffset")) {
                    config.setLoggingTimeOffset(timeStringToMillis(childNode.getTextContent()));
                }
                else if (childName.equals("disabled")) {
                    config.setDisabled(Boolean.parseBoolean(childNode.getTextContent()));
                }
                else {
                    throw new ParseException("found unknown tag:" + childName);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        } catch (IllegalStateException e) {
            throw new ParseException(e);
        }
    }

    Element getDomElement(Document document) {
        Element parentElement = document.createElement("channel");
        parentElement.setAttribute("id", id);

        Element childElement;

        if (description != null) {
            childElement = document.createElement("description");
            childElement.setTextContent(description);
            parentElement.appendChild(childElement);
        }

        if (channelAddress != null) {
            childElement = document.createElement("channelAddress");
            childElement.setTextContent(channelAddress);
            parentElement.appendChild(childElement);
        }

        if (serverMappings != null) {
            for (ServerMapping serverMapping : serverMappings) {
                childElement = document.createElement("serverMapping");
                childElement.setAttribute("id", serverMapping.getId());
                childElement.setTextContent(serverMapping.getServerAddress());
                parentElement.appendChild(childElement);
            }
        }

        if (unit != null) {
            childElement = document.createElement("unit");
            childElement.setTextContent(unit);
            parentElement.appendChild(childElement);
        }

        if (valueType != null) {
            childElement = document.createElement("valueType");
            childElement.setTextContent(valueType.toString());

            if (valueTypeLength != null) {
                if (valueType == ValueType.BYTE_ARRAY || valueType == ValueType.STRING) {
                    childElement.setAttribute("length", valueTypeLength.toString());
                }
            }
            parentElement.appendChild(childElement);
        }

        if (scalingFactor != null) {
            childElement = document.createElement("scalingFactor");
            childElement.setTextContent(Double.toString(scalingFactor));
            parentElement.appendChild(childElement);
        }

        if (valueOffset != null) {
            childElement = document.createElement("valueOffset");
            childElement.setTextContent(Double.toString(valueOffset));
            parentElement.appendChild(childElement);
        }

        if (listening != null) {
            childElement = document.createElement("listening");
            childElement.setTextContent(listening.toString());
            parentElement.appendChild(childElement);
        }

        if (samplingInterval != null) {
            childElement = document.createElement("samplingInterval");
            childElement.setTextContent(millisToTimeString(samplingInterval));
            parentElement.appendChild(childElement);
        }

        if (samplingTimeOffset != null) {
            childElement = document.createElement("samplingTimeOffset");
            childElement.setTextContent(millisToTimeString(samplingTimeOffset));
            parentElement.appendChild(childElement);
        }

        if (samplingGroup != null) {
            childElement = document.createElement("samplingGroup");
            childElement.setTextContent(samplingGroup);
            parentElement.appendChild(childElement);
        }

        if (loggingInterval != null) {
            childElement = document.createElement("loggingInterval");
            childElement.setTextContent(millisToTimeString(loggingInterval));
            parentElement.appendChild(childElement);
        }

        if (loggingTimeOffset != null) {
            childElement = document.createElement("loggingTimeOffset");
            childElement.setTextContent(millisToTimeString(loggingTimeOffset));
            parentElement.appendChild(childElement);
        }

        if (disabled != null) {
            childElement = document.createElement("disabled");
            childElement.setTextContent(disabled.toString());
            parentElement.appendChild(childElement);
        }

        return parentElement;
    }

    ChannelConfigImpl clone(DeviceConfigImpl clonedParentConfig) {
        ChannelConfigImpl configClone = new ChannelConfigImpl(id, clonedParentConfig);

        configClone.description = description;
        configClone.channelAddress = channelAddress;
        configClone.serverMappings = serverMappings;
        configClone.unit = unit;
        configClone.valueType = valueType;
        configClone.valueTypeLength = valueTypeLength;
        configClone.scalingFactor = scalingFactor;
        configClone.valueOffset = valueOffset;
        configClone.listening = listening;
        configClone.samplingInterval = samplingInterval;
        configClone.samplingTimeOffset = samplingTimeOffset;
        configClone.samplingGroup = samplingGroup;
        configClone.loggingInterval = loggingInterval;
        configClone.loggingTimeOffset = loggingTimeOffset;
        configClone.disabled = disabled;

        return configClone;
    }

    ChannelConfigImpl cloneWithDefaults(DeviceConfigImpl clonedParentConfig) {
        ChannelConfigImpl configClone = new ChannelConfigImpl(id, clonedParentConfig);

        if (description == null) {
            configClone.description = ChannelConfig.DESCRIPTION_DEFAULT;
        }
        else {
            configClone.description = description;
        }

        if (channelAddress == null) {
            configClone.channelAddress = CHANNEL_ADDRESS_DEFAULT;
        }
        else {
            configClone.channelAddress = channelAddress;
        }

        if (serverMappings == null) {
            configClone.serverMappings = new ArrayList<>();
        }
        else {
            configClone.serverMappings = serverMappings;
        }

        if (unit == null) {
            configClone.unit = ChannelConfig.UNIT_DEFAULT;
        }
        else {
            configClone.unit = unit;
        }

        if (valueType == null) {
            configClone.valueType = ChannelConfig.VALUE_TYPE_DEFAULT;
        }
        else {
            configClone.valueType = valueType;
        }

        if (valueTypeLength == null) {
            if (valueType == ValueType.DOUBLE) {
                configClone.valueTypeLength = 8;
            }
            else if (valueType == ValueType.BYTE_ARRAY) {
                configClone.valueTypeLength = ChannelConfig.BYTE_ARRAY_SIZE_DEFAULT;
            }
            else if (valueType == ValueType.STRING) {
                configClone.valueTypeLength = ChannelConfig.STRING_SIZE_DEFAULT;
            }
            else if (valueType == ValueType.BYTE) {
                configClone.valueTypeLength = 1;
            }
            else if (valueType == ValueType.FLOAT) {
                configClone.valueTypeLength = 4;
            }
            else if (valueType == ValueType.SHORT) {
                configClone.valueTypeLength = 2;
            }
            else if (valueType == ValueType.INTEGER) {
                configClone.valueTypeLength = 4;
            }
            else if (valueType == ValueType.LONG) {
                configClone.valueTypeLength = 8;
            }
            else if (valueType == ValueType.BOOLEAN) {
                configClone.valueTypeLength = 1;
            }
        }
        else {
            configClone.valueTypeLength = valueTypeLength;
        }

        configClone.scalingFactor = scalingFactor;
        configClone.valueOffset = valueOffset;

        if (listening == null) {
            configClone.listening = ChannelConfig.LISTENING_DEFAULT;
        }
        else {
            configClone.listening = listening;
        }

        if (samplingInterval == null) {
            configClone.samplingInterval = ChannelConfig.SAMPLING_INTERVAL_DEFAULT;
        }
        else {
            configClone.samplingInterval = samplingInterval;
        }

        if (samplingTimeOffset == null) {
            configClone.samplingTimeOffset = ChannelConfig.SAMPLING_TIME_OFFSET_DEFAULT;
        }
        else {
            configClone.samplingTimeOffset = samplingTimeOffset;
        }

        if (samplingGroup == null) {
            configClone.samplingGroup = ChannelConfig.SAMPLING_GROUP_DEFAULT;
        }
        else {
            configClone.samplingGroup = samplingGroup;
        }

        if (loggingInterval == null) {
            configClone.loggingInterval = ChannelConfig.LOGGING_INTERVAL_DEFAULT;
        }
        else {
            configClone.loggingInterval = loggingInterval;
        }

        if (loggingTimeOffset == null) {
            configClone.loggingTimeOffset = ChannelConfig.LOGGING_TIME_OFFSET_DEFAULT;
        }
        else {
            configClone.loggingTimeOffset = loggingTimeOffset;
        }

        if (disabled == null) {
            configClone.disabled = clonedParentConfig.isDisabled();
        }
        else {
            if (clonedParentConfig.isDisabled()) {
                configClone.disabled = false;
            }
            else {
                configClone.disabled = disabled;
            }
        }

        return configClone;
    }

    static String getAttributeValue(Node element, String attributeName) {
        NamedNodeMap attributes = element.getAttributes();

        Node nameAttribute = attributes.getNamedItem(attributeName);

        if (nameAttribute == null) {
            return null;
        }
        return nameAttribute.getTextContent();
    }

    static String millisToTimeString(final int timeInMillis) {
        if (timeInMillis <= 0) {
            return "0";
        }
        if ((timeInMillis % 1000) != 0) {
            return timeToString("ms", timeInMillis);
        }

        int timeInS = timeInMillis / 1000;
        if ((timeInS % 60) == 0) {
            int timeInM = timeInS / 60;
            if ((timeInM % 60) == 0) {
                int timeInH = timeInM / 60;
                return timeToString("h", timeInH);
            }
            return timeToString("m", timeInM);
        }
        return timeToString("s", timeInS);
    }

    private static String timeToString(String timeUnit, int time) {
        return MessageFormat.format("{0,number,#}{1}", time, timeUnit);
    }

    static Integer timeStringToMillis(String timeString) throws ParseException {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        Matcher timeMatcher = timePattern.matcher(timeString);
        if (!timeMatcher.matches()) {
            throw new ParseException(MessageFormat.format("Unknown time string: ''{0}''.", timeString));
        }

        String timeNumStr = timeMatcher.group(1);
        Long timeNum = parseTimeNumFrom(timeNumStr);

        String timeUnit = timeMatcher.group(2);
        final TimeUnit milliseconds = TimeUnit.MILLISECONDS;

        if (timeUnit == null) {
            return timeNum.intValue();
        }

        switch (timeUnit) {
        case "s":
            return (int) milliseconds.convert(timeNum, TimeUnit.SECONDS);

        case "m":
            return (int) milliseconds.convert(timeNum, TimeUnit.MINUTES);

        case "h":
            return (int) milliseconds.convert(timeNum, TimeUnit.HOURS);

        case "ms":
            return timeNum.intValue();
        default:
            // can not reach this case: string pattern does not allow this.
            throw new ParseException("Unknown time unit: " + timeUnit);
        }

    }

    private static Long parseTimeNumFrom(String timeNumStr) throws ParseException {
        try {
            return Long.parseLong(timeNumStr);
        } catch (NumberFormatException e) {
            throw new ParseException(e);
        }
    }

    static void checkIdSyntax(String id) {
        if (id.matches("[a-zA-Z0-9_-]+")) {
            return;
        }

        String msg = MessageFormat.format(
                "Invalid ID: \"{0}\". An ID may not be the empty string and must contain only ASCII letters, digits, hyphens and underscores.",
                id);
        throw new IllegalArgumentException(msg);
    }

    public boolean isSampling() {
        return !disabled && samplingInterval != null && samplingInterval > 0;
    }

    @Override
    public void addServerMapping(ServerMapping serverMapping) {
        if (serverMappings == null) {
            serverMappings = new ArrayList<>();
        }
        serverMappings.add(serverMapping);
    }

    @Override
    public void deleteServerMappings(String id) {
        if (serverMappings != null) {
            List<ServerMapping> newMappings = new ArrayList<>();
            for (ServerMapping serverMapping : serverMappings) {
                if (!serverMapping.getId().equals(id)) {
                    newMappings.add(serverMapping);
                }
            }
            serverMappings = newMappings;
        }
    }
}
