/*
 * Copyright 2011-14 Fraunhofer ISE
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Frederic Robra
 */
public class DevicesConfigurator extends Configurator {

    private static Logger logger = LoggerFactory.getLogger(DevicesConfigurator.class);

    public DevicesConfigurator(Config config) {
        super(config);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openmuc.framework.webui.channelconfigurator.config.Configurator#getContent(java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Content getContent(String localPath, HttpServletRequest request)
            throws ProcessRequestException,
            IdCollisionException {

        Content content = new Content();
        ConfigService configService = config.getConfigService();
        RootConfig rootConfig = configService.getConfig();
        ResourceLoader loader = config.getLoader();

        if (localPath.endsWith("/edit")) {
            String driverId = Util.parseString(request, "driverId");
            String deviceId = Util.tryParseString(request, "deviceId");

            DeviceConfig device = null;
            String title = null;
            if (deviceId != null) {
                device = rootConfig.getDevice(deviceId);
                title = "Edit device \"" + deviceId + "\"";
            } else {
                title = "Add new device to driver " + driverId;
            }

            content.setHtml(loader.getResourceAsString("devices/edit.html"));
            content.setTitle(title);
            try {
                content.addToContext("driverInfo", configService.getDriverInfo(driverId));
            }
            catch (DriverNotAvailableException e) {
                logger.debug(e.getMessage());
            }
            content.addToContext("driverId", driverId);
            content.addToContext("device", device);
        } else if (localPath.endsWith("/update")) {
            String deviceId = Util.parseString(request, "deviceId");
            logger.debug("updating " + deviceId);
            String originalDeviceId = Util.tryParseString(request, "originalDeviceId");

            DeviceConfig newDevice = null;
            if (originalDeviceId == null) {
                String driverId = Util.parseString(request, "driverId");
                DriverConfig driver = rootConfig.getDriver(driverId);
                newDevice = driver.addDevice(deviceId);
            } else {
                newDevice = rootConfig.getDevice(originalDeviceId);
                if (!originalDeviceId.equals(deviceId)) {
                    newDevice.setId(deviceId);
                }
            }

            newDevice.setDescription(Util.tryParseString(request, "description"));
            newDevice.setInterfaceAddress(Util.tryParseString(request, "interfaceAddress"));
            newDevice.setDeviceAddress(Util.tryParseString(request, "deviceAddress"));
            newDevice.setSettings(Util.tryParseString(request, "settings"));
            newDevice.setSamplingTimeout(Util.tryParseInt(request.getParameter("samplingTimeout")));
            newDevice.setConnectRetryInterval(Util.tryParseInt(request.getParameter("connectRetry")));

            String disabledValue = Util.tryParseString(request, "disabled");
            if (disabledValue == null) {
                newDevice.setDisabled(null);
            } else if (disabledValue.equals("true")) {
                newDevice.setDisabled(true);
            } else if (disabledValue.equals("false")) {
                newDevice.setDisabled(false);
            }

            configService.setConfig(rootConfig);
            config.save();

            content = Content.createRedirect(MenuItem.DEVICES.getPath());
        } else if (localPath.endsWith("/delete")) {
            String deviceId = Util.parseString(request, "deviceId");

            DeviceConfig device = rootConfig.getDevice(deviceId);
            if (device == null) {
                throw new ProcessRequestException("Device with ID \"" + deviceId + "\" not found.");
            }
            device.delete();
            configService.setConfig(rootConfig);
            config.save();

            content = Content.createRedirect(MenuItem.DEVICES.getPath());
        } else if (localPath.endsWith("/scan")) {
            String driverId = Util.parseString(request, "driverId");
            String deviceId = Util.parseString(request, "deviceId");

            DeviceConfig device = rootConfig.getDevice(deviceId);

            List<ChannelScanInfo> infoList = Collections.emptyList();
            try {
                infoList = configService.scanForChannels(deviceId, "");
            }
            catch (UnsupportedOperationException e) {
                throw new ProcessRequestException("Scanning channels not supported by driver "
                                                  + driverId);
            }
            catch (DriverNotAvailableException e) {
                throw new ProcessRequestException("Driver " + driverId + " not available");
            }
            catch (ScanException e) {
                throw new ProcessRequestException("Scan error: " + e.getMessage());
            }
            catch (ArgumentSyntaxException e) {
                throw new ProcessRequestException("Settings syntax invalid: " + e.getMessage());
            }

            Iterator<ChannelScanInfo> iter = infoList.iterator();
            while (iter.hasNext()) {
                ChannelScanInfo channelInfo = iter.next();
                if (device.getChannel(channelInfo.getChannelAddress()) != null) {
                    iter.remove();
                }
            }
            content.setTitle("Scanned channels of " + device.getId());
            content.setHtml(loader.getResourceAsString("devices/scan.html"));
            content.addToContext("driverId", driverId);
            content.addToContext("deviceId", deviceId);
            content.addToContext("channelInfoList", infoList);
        } else if (localPath.endsWith("/addscan")) {
            String deviceId = Util.parseString(request, "deviceId");

            String[] channels = request.getParameterValues("channels");
            for (String channel : channels) {
                String channelId = Util.parseString(request, channel + "channelId");
                String address = Util.tryParseString(request, channel + "address");
                String description = Util.tryParseString(request, channel + "description");
                String valueType = Util.tryParseString(request, channel + "valueType");
                Integer valueTypeLength = Util.tryParseInt(request.getParameter(channel
                                                                                + "valueLength"));

                DeviceConfig deviceConfig = rootConfig.getDevice(deviceId);
                ChannelConfig newChannel = deviceConfig.addChannel(channelId);
                newChannel.setChannelAddress(address);
                newChannel.setDescription(description);
                if (valueType == null || valueType.equals("")) {
                    newChannel.setValueType(null);
                } else {
                    newChannel.setValueType(ValueType.valueOf(valueType));
                }
                newChannel.setValueTypeLength(valueTypeLength);
            }
            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.CHANNELS.getPath());
        } else {
            content.setTitle("Devices");
            content.setHtml(loader.getResourceAsString("devices/devices.html"));
            content.setMenuItem(MenuItem.DEVICES);
            content.addToContext("driverList", rootConfig.getDrivers());
            content.addToContext("configService", configService);
        }
        return content;
    }

}
