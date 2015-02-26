/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.webui.channelconfigurator.conf;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.webui.channelconfigurator.*;
import org.openmuc.framework.webui.spi.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Frederic Robra
 */
public class ChannelsConfigurator extends Configurator {

    private static Logger logger = LoggerFactory.getLogger(ChannelsConfigurator.class);

    /**
     * @param config
     */
    public ChannelsConfigurator(Config config) {
        super(config);
    }

    @Override
    public Content getContent(String localPath, HttpServletRequest request) throws ProcessRequestException, IdCollisionException {

        Content content = new Content();
        ConfigService configService = config.getConfigService();
        RootConfig rootConfig = configService.getConfig();
        ResourceLoader loader = config.getLoader();

        if (localPath.endsWith("/edit")) {
            String deviceId = Util.parseString(request, "deviceId");
            String channelId = Util.tryParseString(request, "channelId");

            ChannelConfig channel = null;
            String title = null;
            if (channelId != null) {
                channel = rootConfig.getChannel(channelId);
                if (channel == null) {
                    throw new ProcessRequestException("Channel with ID \"" + channelId + "\" not found.");
                }
                title = "Edit channel \"" + channelId + "\"";
            } else {
                title = "Add new channel to " + deviceId;
            }
            content.setTitle(title);
            content.setHtml(loader.getResourceAsString("channels/edit.html"));
            ;
            try {
                String driverId = rootConfig.getDevice(deviceId).getDriver().getId();
                content.addToContext("driverInfo", configService.getDriverInfo(driverId));
            } catch (DriverNotAvailableException e) {
                logger.debug(e.getMessage());
            }
            content.addToContext("deviceId", deviceId);
            content.addToContext("values", ValueType.values());
            content.addToContext("channel", channel);
        } else if (localPath.endsWith("/update")) {
            String channelId = Util.parseString(request, "channelId");
            String originalId = Util.tryParseString(request, "originalId");

            ChannelConfig newChannel;
            if (originalId == null) {
                String deviceId = Util.parseString(request, "deviceId");
                DeviceConfig device = rootConfig.getDevice(deviceId);
                newChannel = device.addChannel(channelId);
            } else {
                newChannel = rootConfig.getChannel(originalId);
                if (!originalId.equals(channelId)) {
                    newChannel.setId(channelId);
                }
            }

            Integer samplingInterval = Util.tryParseInt(request.getParameter("samplingInterval"));
            String listening = Util.tryParseString(request, "listening");
            if (samplingInterval != null) {
                newChannel.setSamplingInterval(samplingInterval);
                newChannel.setListening(false);
            } else if (listening != null) {
                newChannel.setSamplingInterval(null);
                if ("true".equals(listening)) {
                    newChannel.setListening(true);
                } else if ("false".equals(listening)) {
                    newChannel.setListening(false);
                }
            }

            if (samplingInterval != null && samplingInterval > 0) {
                if ("true".equals(listening)) {
                    throw new ProcessRequestException("Listening and sampling may not be enabled at the same time.");
                } else {
                    if ("false".equals(listening)) {
                        newChannel.setListening(false);
                    } else {
                        newChannel.setListening(null);
                    }
                    newChannel.setSamplingInterval(samplingInterval);
                }
            } else {
                newChannel.setSamplingInterval(samplingInterval);
                if ("true".equals(listening)) {
                    newChannel.setListening(true);
                } else if ("false".equals(listening)) {
                    newChannel.setListening(false);
                } else {
                    newChannel.setListening(null);
                }
            }

            newChannel.setDescription(Util.tryParseString(request, "description"));
            newChannel.setChannelAddress(Util.tryParseString(request, "channelAddress"));

            String valueType = Util.tryParseString(request, "valueType");
            if (valueType == null || valueType.equals("")) {
                newChannel.setValueType(null);
            } else {
                newChannel.setValueType(ValueType.valueOf(valueType));
            }
            newChannel.setValueTypeLength(Util.tryParseInt(request.getParameter("valueLength")));
            newChannel.setScalingFactor(Util.tryParseDouble(request.getParameter("scalingFactor")));
            newChannel.setValueOffset(Util.tryParseDouble(request.getParameter("scalingOffset")));
            newChannel.setUnit(Util.tryParseString(request, "unit"));
            newChannel.setLoggingInterval(Util.tryParseInt(request.getParameter("loggingInterval")));
            newChannel.setLoggingTimeOffset(Util.tryParseInt(request.getParameter("loggingOffset")));
            newChannel.setSamplingTimeOffset(Util.tryParseInt(request.getParameter("samplingOffset")));
            newChannel.setSamplingGroup(Util.tryParseString(request, "samplingGroup"));

            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.CHANNELS.getPath());
        } else if (localPath.endsWith("/delete")) {
            String channelId = Util.parseString(request, "channelId");

            ChannelConfig channel = rootConfig.getChannel(channelId);
            if (channel == null) {
                throw new ProcessRequestException("Channel with ID \"" + channelId + "\" not found.");
            }

            channel.delete();
            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.CHANNELS.getPath());
        } else { /* channels */
            content.setTitle("Channels");
            content.setHtml(loader.getResourceAsString("channels/channels.html"));
            content.setMenuItem(MenuItem.CHANNELS);
            content.addToContext("driverList", rootConfig.getDrivers());
        }

        return content;
    }

}
