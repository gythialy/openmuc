/*
 * Copyright 2011-2021 Fraunhofer ISE
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
package org.openmuc.framework.server.restws.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.DriverNotAvailableException;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.lib.rest1.Const;
import org.openmuc.framework.lib.rest1.FromJson;
import org.openmuc.framework.lib.rest1.ToJson;
import org.openmuc.framework.lib.rest1.exceptions.MissingJsonObjectException;
import org.openmuc.framework.lib.rest1.exceptions.RestConfigIsNotCorrectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class DeviceResourceServlet extends GenericServlet {

    private static final String REQUESTED_REST_PATH_IS_NOT_AVAILABLE = "Requested rest path is not available.";
    private static final String REQUESTED_REST_DEVICE_IS_NOT_AVAILABLE = "Requested rest device is not available.";
    private static final long serialVersionUID = 4619892734239871891L;
    private static final Logger logger = LoggerFactory.getLogger(DeviceResourceServlet.class);

    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootConfig;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String deviceID, configField;
            String pathInfo = pathAndQueryString[0];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            List<String> deviceList = doGetDeviceList();

            response.setStatus(HttpServletResponse.SC_OK);

            ToJson json = new ToJson();

            if (pathInfo.equals("/")) {
                json.addStringList(Const.DEVICES, deviceList);
            }
            else {
                deviceID = pathInfoArray[0].replace("/", "");

                if (deviceList.contains(deviceID)) {

                    List<Channel> deviceChannelList = doGetDeviceChannelList(deviceID);
                    DeviceState deviceState = configService.getDeviceState(deviceID);

                    if (pathInfoArray.length == 1) {
                        json.addChannelRecordList(deviceChannelList);
                        json.addDeviceState(deviceState);
                    }
                    else if (pathInfoArray[1].equalsIgnoreCase(Const.STATE)) {
                        json.addDeviceState(deviceState);
                    }
                    else if (pathInfoArray.length > 1 && pathInfoArray[1].equals(Const.CHANNELS)) {
                        json.addChannelList(deviceChannelList);
                        json.addDeviceState(deviceState);
                    }
                    else if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                        doGetConfigs(json, deviceID, response);
                    }
                    else if (pathInfoArray.length == 3 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                        configField = pathInfoArray[2];
                        doGetConfigField(json, deviceID, configField, response);
                    }
                    else if (pathInfoArray[1].equalsIgnoreCase(Const.SCAN)) {
                        String settings = request.getParameter(Const.SETTINGS);
                        List<ChannelScanInfo> channelScanInfoList = scanForAllChannels(deviceID, settings, response);
                        json.addChannelScanInfoList(channelScanInfoList);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                "Requested rest device is not available or unknown option.", " DeviceID = ", deviceID);
                    }
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            REQUESTED_REST_DEVICE_IS_NOT_AVAILABLE, " DeviceID = ", deviceID);
                }
            }
            sendJson(json, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            String deviceID = pathInfoArray[0].replace("/", "");
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfoArray.length == 1) {
                setAndWriteDeviceConfig(deviceID, response, json, false);
            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }

        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            String deviceID = pathInfoArray[0].replace("/", "");
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfoArray.length < 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }
            else {

                DeviceConfig deviceConfig = rootConfig.getDevice(deviceID);

                if (deviceConfig != null && pathInfoArray.length == 2
                        && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                    setAndWriteDeviceConfig(deviceID, response, json, true);
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
                }
            }
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[0];
            String deviceID = null;
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);

            deviceID = pathInfoArray[0].replace("/", "");
            DeviceConfig deviceConfig = rootConfig.getDevice(deviceID);

            if (pathInfoArray.length != 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available", " Path Info = ", request.getPathInfo());
            }
            else if (deviceConfig == null) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Device \"" + deviceID + "\" does not exist.");
            }
            else {
                try {
                    deviceConfig.delete();
                    configService.setConfig(rootConfig);
                    configService.writeConfigToFile();

                    if (rootConfig.getDriver(deviceID) == null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                logger, "Not able to delete driver ", deviceID);
                    }
                } catch (ConfigWriteException e) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                            "Not able to write into config.");
                }
            }
        }
    }

    private List<ChannelScanInfo> scanForAllChannels(String deviceID, String settings, HttpServletResponse response) {
        List<ChannelScanInfo> channelList = new ArrayList<>();
        List<ChannelScanInfo> scannedDevicesList;
        String deviceIDString = " deviceId = ";

        try {
            scannedDevicesList = configService.scanForChannels(deviceID, settings);

            for (ChannelScanInfo scannedDevice : scannedDevicesList) {
                channelList.add(scannedDevice);
            }

        } catch (UnsupportedOperationException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Device does not support scanning.", deviceIDString, deviceID);
        } catch (DriverNotAvailableException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    REQUESTED_REST_DEVICE_IS_NOT_AVAILABLE, deviceIDString, deviceID);
        } catch (ArgumentSyntaxException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "Argument syntax was wrong.", deviceIDString, deviceID, " Settings = ", settings);
        } catch (ScanException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Error while scan device channels", deviceIDString, deviceID, " Settings = ", settings);
        }
        return channelList;
    }

    private List<Channel> doGetDeviceChannelList(String deviceID) {
        List<Channel> deviceChannelList = new ArrayList<>();
        Collection<ChannelConfig> channelConfig;

        channelConfig = rootConfig.getDevice(deviceID).getChannels();
        for (ChannelConfig chCf : channelConfig) {
            deviceChannelList.add(dataAccess.getChannel(chCf.getId()));
        }
        return deviceChannelList;
    }

    private List<String> doGetDeviceList() {
        List<String> deviceList = new ArrayList<>();

        Collection<DriverConfig> driverConfig;
        driverConfig = rootConfig.getDrivers();

        Collection<DeviceConfig> deviceConfig = new ArrayList<>();

        for (DriverConfig drvCfg : driverConfig) {
            String driverId = drvCfg.getId();
            deviceConfig.addAll(rootConfig.getDriver(driverId).getDevices());
        }
        for (DeviceConfig devCfg : deviceConfig) {
            deviceList.add(devCfg.getId());
        }
        return deviceList;
    }

    private void doGetConfigField(ToJson json, String deviceID, String configField, HttpServletResponse response)
            throws IOException {
        DeviceConfig deviceConfig = rootConfig.getDevice(deviceID);

        if (deviceConfig != null) {
            JsonObject jsoConfigAll = ToJson.getDeviceConfigAsJsonObject(deviceConfig);
            if (jsoConfigAll == null) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Could not find JSON object \"configs\"");
            }
            else {
                JsonElement jseConfigField = jsoConfigAll.get(configField);

                if (jseConfigField == null) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "Requested rest config field is not available.", " configField = ", configField);
                }
                else {
                    JsonObject jso = new JsonObject();
                    jso.add(configField, jseConfigField);
                    json.addJsonObject(Const.CONFIGS, jso);
                }
            }
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    "Requested rest channel is not available.", " ChannelID = ", deviceID);
        }
    }

    private void doGetConfigs(ToJson json, String deviceID, HttpServletResponse response) throws IOException {
        DeviceConfig deviceConfig;
        deviceConfig = rootConfig.getDevice(deviceID);

        if (deviceConfig != null) {
            json.addDeviceConfig(deviceConfig);
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    REQUESTED_REST_DEVICE_IS_NOT_AVAILABLE, " DeviceID = ", deviceID);
        }
    }

    private boolean setAndWriteDeviceConfig(String deviceID, HttpServletResponse response, FromJson json,
            boolean isHTTPPut) {

        try {
            if (isHTTPPut) {
                return setAndWriteHttpPutDeviceConfig(deviceID, response, json);
            }
            else {
                return setAndWriteHttpPostDeviceConfig(deviceID, response, json);
            }
        } catch (JsonSyntaxException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                    "JSON syntax is wrong.");
        } catch (ConfigWriteException e) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Could not write device \"", deviceID, "\".");
        } catch (RestConfigIsNotCorrectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "Not correct formed device config json.", " JSON = ", json.getJsonObject().toString());
        } catch (Error e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    e.getMessage());
        } catch (MissingJsonObjectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger, e.getMessage());
        } catch (IllegalStateException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger, e.getMessage());
        }
        return false;
    }

    private boolean setAndWriteHttpPutDeviceConfig(String deviceID, HttpServletResponse response, FromJson json)
            throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException,
            MissingJsonObjectException, IllegalStateException {

        boolean ok = false;

        DeviceConfig deviceConfig = rootConfig.getDevice(deviceID);
        if (deviceConfig != null) {
            try {
                json.setDeviceConfig(deviceConfig, deviceID);
            } catch (IdCollisionException e) {
            }
            configService.setConfig(rootConfig);
            configService.writeConfigToFile();
            response.setStatus(HttpServletResponse.SC_OK);
            ok = true;
        }
        else {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Not able to access to device ", deviceID);
        }
        return ok;
    }

    private synchronized boolean setAndWriteHttpPostDeviceConfig(String deviceID, HttpServletResponse response,
            FromJson json) throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException, Error,
            MissingJsonObjectException, IllegalStateException {

        boolean ok = false;
        DriverConfig driverConfig;

        DeviceConfig deviceConfig = rootConfig.getDevice(deviceID);

        JsonObject jso = json.getJsonObject();
        String driverID = jso.get(Const.DRIVER).getAsString();

        if (driverID != null) {
            driverConfig = rootConfig.getDriver(driverID);
        }
        else {
            throw new Error("No driver ID in JSON");
        }

        if (driverConfig == null) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Driver does not exists: ", driverID);
        }
        else if (deviceConfig != null) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Device already exists: ", deviceID);
        }
        else {
            try {
                deviceConfig = driverConfig.addDevice(deviceID);
                json.setDeviceConfig(deviceConfig, deviceID);
            } catch (IdCollisionException e) {
            }
            configService.setConfig(rootConfig);
            configService.writeConfigToFile();
            response.setStatus(HttpServletResponse.SC_OK);
            ok = true;
        }
        return ok;
    }

    private void setConfigAccess() {
        this.dataAccess = handleDataAccessService(null);
        this.configService = handleConfigService(null);
        this.rootConfig = handleRootConfig(null);
    }

}
