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
import org.openmuc.framework.webui.channelconfigurator.*;
import org.openmuc.framework.webui.spi.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederic Robra
 */
public class DriversConfigurator extends Configurator {

    private static Logger logger = LoggerFactory.getLogger(DriversConfigurator.class);

    private DeviceScanner deviceScanner;

    /**
     * @param config
     */
    public DriversConfigurator(Config config) {
        super(config);
    }

    @Override
    public Content getContent(String localPath, HttpServletRequest request) throws ProcessRequestException, IdCollisionException {

        Content content = new Content();
        ConfigService configService = config.getConfigService();
        RootConfig rootConfig = configService.getConfig();
        ResourceLoader loader = config.getLoader();

        if (localPath.endsWith("/edit")) {
            String driverId = Util.tryParseString(request, "driverId");
            List<String> runningDrivers = configService.getIdsOfRunningDrivers();
            List<String> runningDriversNotConfigured = new ArrayList<String>(runningDrivers.size());
            for (String runningDriver : runningDrivers) {
                if (rootConfig.getDriver(runningDriver) == null) {
                    runningDriversNotConfigured.add(runningDriver);
                }
            }
            if (driverId != null) {
                DriverConfig driver = rootConfig.getDriver(driverId);
                content.setTitle("Edit " + driverId);
                content.addToContext("driver", driver);
            } else {
                content.setTitle("Add new driver");
            }
            content.setHtml(loader.getResourceAsString("/drivers/edit.html"));

            content.addToContext("runningDriversNotConfigured", runningDriversNotConfigured);
        } else if (localPath.endsWith("/update")) {
            String oldId = Util.tryParseString(request, "originalid");
            String driverId = Util.parseString(request, "id");

            DriverConfig newDriver = null;

            if (oldId == null) {
                newDriver = rootConfig.addDriver(driverId);
            } else {
                newDriver = rootConfig.getDriver(oldId);
                if (oldId.equals(driverId) == false) {
                    newDriver.setId(driverId);
                }
            }

            newDriver.setSamplingTimeout(Util.tryParseInt(request.getParameter("samplingTimeout")));
            newDriver.setConnectRetryInterval(Util.tryParseInt(request.getParameter("connectRetry")));

            String disabledValue = request.getParameter("disabled");
            if (disabledValue.equals("true")) {
                newDriver.setDisabled(true);
            } else if (disabledValue.equals("false")) {
                newDriver.setDisabled(false);
            } else {
                newDriver.setDisabled(null);
            }

            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.DRIVERS.getPath());
        } else if (localPath.endsWith("/delete")) {
            String driverId = Util.parseString(request, "driverId");
            rootConfig.getDriver(driverId).delete();
            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.DRIVERS.getPath());
        } else if (localPath.endsWith("/scan")) {
            String driverId = Util.parseString(request, "driverId");
            String settings = request.getParameter("settings");

            DriverInfo driverInfo = null;
            try {
                driverInfo = configService.getDriverInfo(driverId);
            } catch (DriverNotAvailableException e1) {
                throw new ProcessRequestException("Driver " + driverId + " not available");
            }
            content.setTitle("Scan " + driverId);
            content.setHtml(loader.getResourceAsString("drivers/scan.html"));

            content.addToContext("driverInfo", driverInfo);
            content.addToContext("driverId", driverId);

            if (settings != null) {
                try {
                    try {
                        configService.interruptDeviceScan(driverId);
                    } catch (UnsupportedOperationException e) {
                    }
                    deviceScanner = new DeviceScanner(driverId);
                    configService.scanForDevices(driverId, settings, deviceScanner.getListener());
                } catch (DriverNotAvailableException e) {
                    deviceScanner = null;
                    throw new ProcessRequestException("Driver " + driverId + " not available");
                }

                content.addToContext("settings", settings);
            }

        } else if (localPath.endsWith("/scanupdates")) {
            logger.debug("scan update");
            StringBuilder jsonString = new StringBuilder();

            if (deviceScanner != null) {
                jsonString.append("[");
                for (DeviceScanInfo scannedDevice : deviceScanner.getNewDevices()) {
                    jsonString.append("{\"deviceID\":\"").append(scannedDevice.getId()).append("\",\"description\":\"")
                              .append(scannedDevice.getDescription()).append("\",\"deviceAddress\":\"")
                              .append(scannedDevice.getDeviceAddress()).append("\",\"settings\":\"").append(scannedDevice.getSettings())
                              .append("\"},");
                }
                try {
                    jsonString.deleteCharAt(jsonString.lastIndexOf(","));
                } catch (StringIndexOutOfBoundsException e) {
                }
                jsonString.append("]");
            }
            content = Content.createAjax(jsonString.toString());
        } else if (localPath.endsWith("/scanprogress")) {
            StringBuilder jsonString = new StringBuilder();
            jsonString.append("{\"scanProgress\":\"");
            if (deviceScanner == null || deviceScanner.isScanInterrupted() || deviceScanner.isScanError()) {
                jsonString.append("--- ");
            } else if (deviceScanner.isScanFinished()) {
                jsonString.append(100);
            } else {
                jsonString.append(deviceScanner.getScanProgress());
            }

            if (deviceScanner != null && deviceScanner.getScanErrorMessage() != null) {
                jsonString.append("\",\"errorMessage\":\"").append(deviceScanner.getScanErrorMessage());
            }
            jsonString.append("\"}");
            content = Content.createAjax(jsonString.toString());
        } else if (localPath.endsWith("/stopscan")) {
            String driverId = Util.parseString(request, "driverId");

            try {
                configService.interruptDeviceScan(driverId);
            } catch (Exception e) {
                logger.warn("scan could not be stopped");
            }
        } else if (localPath.endsWith("/addscan")) {
            String driverId = Util.parseString(request, "driverId");

            try {
                configService.interruptDeviceScan(driverId);
            } catch (Exception e) {
                logger.warn("scan could not be stopped");
            }

            String[] devices = request.getParameterValues("devices");
            for (String device : devices) {
                String deviceId = Util.parseString(request, device + "deviceId");
                String description = Util.tryParseString(request, device + "description");
                String deviceAddress = Util.tryParseString(request, device + "deviceAddress");
                String settings = Util.tryParseString(request, device + "settings");
                DriverConfig driver = rootConfig.getDriver(driverId);
                DeviceConfig newDevice = driver.addDevice(deviceId);
                newDevice.setDescription(description);
                newDevice.setDeviceAddress(deviceAddress);
                newDevice.setSettings(settings);
            }
            configService.setConfig(rootConfig);
            config.save();
            content = Content.createRedirect(MenuItem.DEVICES.getPath());
        } else { /* drivers */
            content.setTitle("Driver List");
            content.setHtml(loader.getResourceAsString("drivers/drivers.html"));
            content.setMenuItem(MenuItem.DRIVERS);

            content.addToContext("drivers", rootConfig.getDrivers());
            content.addToContext("runningDrivers", configService.getIdsOfRunningDrivers());
        }

        return content;
    }

}
