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

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.DriverNotAvailableException;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
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

public class DriverResourceServlet extends GenericServlet {

    private static final String REQUESTED_REST_PATH_IS_NOT_AVAILABLE = "Requested rest path is not available.";
    private static final String DRIVER_ID = " driverID = ";
    private static final String PATH_INFO = " Path Info = ";
    private static final long serialVersionUID = -2223282905555493215L;

    private static final Logger logger = LoggerFactory.getLogger(DriverResourceServlet.class);

    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootConfig;

    private DeviceScanListenerImplementation scanListener = new DeviceScanListenerImplementation();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];

            List<String> driversList = new ArrayList<>();
            Collection<DriverConfig> driverConfigList = rootConfig.getDrivers();

            for (DriverConfig drv : driverConfigList) {
                driversList.add(drv.getId());
            }

            ToJson json = new ToJson();

            if (pathInfo.equals("/")) {
                json.addStringList(Const.DRIVERS, driversList);
            }
            else {
                String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
                String driverID = pathInfoArray[0].replace("/", "");

                List<Channel> driverChannelsList;
                List<String> driverDevicesList = new ArrayList<>();

                if (driversList.contains(driverID)) {

                    Collection<ChannelConfig> channelConfigList = new ArrayList<>();
                    Collection<DeviceConfig> deviceConfigList;
                    DriverConfig drv = rootConfig.getDriver(driverID);

                    deviceConfigList = drv.getDevices();
                    setDriverDevicesListAndChannelConfigList(driverDevicesList, channelConfigList, deviceConfigList);
                    driverChannelsList = getDriverChannelList(channelConfigList);

                    response.setStatus(HttpServletResponse.SC_OK);
                    boolean driverIsRunning = configService.getIdsOfRunningDrivers().contains(driverID);

                    if (pathInfoArray.length > 1) {
                        if (pathInfoArray[1].equalsIgnoreCase(Const.CHANNELS)) {
                            json.addChannelList(driverChannelsList);
                            json.addBoolean(Const.RUNNING, driverIsRunning);
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.RUNNING)) {
                            json.addBoolean(Const.RUNNING, driverIsRunning);
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.INFOS)) {
                            DriverInfo driverInfo;
                            try {
                                driverInfo = configService.getDriverInfo(driverID);
                                json.addDriverInfo(driverInfo);
                            } catch (DriverNotAvailableException e) {
                                logger.error("Driver info not available, because driver {} doesn't exist.", driverID);
                            }
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.DEVICES)) {
                            json.addStringList(Const.DEVICES, driverDevicesList);
                            json.addBoolean(Const.RUNNING, driverIsRunning);
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.SCAN)) {
                            List<DeviceScanInfo> deviceScanInfoList = new ArrayList<>();
                            scanListener = new DeviceScanListenerImplementation(deviceScanInfoList);

                            String settings = request.getParameter(Const.SETTINGS);
                            deviceScanInfoList = scanForAllDrivers(driverID, settings, scanListener, response);
                            json.addDeviceScanInfoList(deviceScanInfoList);
                        }

                        else if (pathInfoArray[1].equalsIgnoreCase(Const.SCAN_PROGRESS_INFO)) {
                            json.addDeviceScanProgressInfo(scanListener.getRestScanProgressInfo());
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS) && pathInfoArray.length == 2) {
                            doGetConfigs(json, driverID, response);
                        }
                        else if (pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS) && pathInfoArray.length == 3) {
                            doGetConfigField(json, driverID, pathInfoArray[2], response);
                        }
                        else {
                            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                    REQUESTED_REST_PATH_IS_NOT_AVAILABLE, PATH_INFO, request.getPathInfo());
                        }
                    }
                    else if (pathInfoArray.length == 1) {
                        json.addChannelRecordList(driverChannelsList);
                        json.addBoolean(Const.RUNNING, driverIsRunning);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                REQUESTED_REST_PATH_IS_NOT_AVAILABLE, PATH_INFO, request.getPathInfo());
                    }

                }
                else {
                    driverNotAvailable(response, driverID);
                }
            }
            sendJson(json, response);
        }
    }

    private static void driverNotAvailable(HttpServletResponse response, String driverID) {
        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                "Requested rest driver is not available.", DRIVER_ID, driverID);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString == null) {
            return;
        }

        setConfigAccess();

        String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];

        String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
        String driverID = pathInfoArray[0].replace("/", "");

        String json = ServletLib.getJsonText(request);

        if (pathInfoArray.length < 1) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
        }
        else {
            DriverConfig driverConfig = rootConfig.getDriver(driverID);

            if (driverConfig != null && pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                setAndWriteDriverConfig(driverID, response, json);
            }
            else if (driverConfig != null && pathInfoArray.length == 2
                    && pathInfoArray[1].equalsIgnoreCase(Const.SCAN_INTERRUPT)) {
                interruptScanProcess(driverID, response, json);
            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }
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
            String driverID = pathInfoArray[0].replace("/", "");

            String json = ServletLib.getJsonText(request);

            if (pathInfoArray.length != 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }
            else {
                try {
                    rootConfig.addDriver(driverID);
                    configService.setConfig(rootConfig);
                    configService.writeConfigToFile();

                    setAndWriteDriverConfig(driverID, response, json);

                } catch (IdCollisionException e) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                            "Driver \"" + driverID + "\" already exist");
                } catch (ConfigWriteException e) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                            "Could not write driver \"", driverID, "\".");
                    e.printStackTrace();
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
            String driverID = null;

            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            driverID = pathInfoArray[0].replace("/", "");

            DriverConfig driverConfig = rootConfig.getDriver(driverID);

            if (pathInfoArray.length != 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, PATH_INFO, request.getPathInfo());
            }
            else if (driverConfig == null) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Driver \"" + driverID + "\" does not exist.");
            }
            else {
                try {
                    driverConfig.delete();
                    configService.setConfig(rootConfig);
                    configService.writeConfigToFile();

                    if (rootConfig.getDriver(driverID) == null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                logger, "Not able to delete driver ", driverID);
                    }
                } catch (ConfigWriteException e) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                            "Not able to write into config.");
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean setAndWriteDriverConfig(String driverID, HttpServletResponse response, String json) {
        boolean ok = false;

        try {
            DriverConfig driverConfig = rootConfig.getDriver(driverID);
            if (driverConfig != null) {
                try {
                    FromJson fromJson = new FromJson(json);
                    fromJson.setDriverConfig(driverConfig, driverID);
                } catch (IdCollisionException e) {

                }
                configService.setConfig(rootConfig);
                configService.writeConfigToFile();
                response.setStatus(HttpServletResponse.SC_OK);
                ok = true;
            }
            else {
                ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                        "Not able to access to driver ", driverID);
            }
        } catch (JsonSyntaxException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                    "JSON syntax is wrong.");
        } catch (MissingJsonObjectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger, e.getMessage());
        } catch (ConfigWriteException e) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Could not write driver \"", driverID, "\".");
            logger.debug(e.getMessage());
        } catch (RestConfigIsNotCorrectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "Not correct formed driver config json.", " JSON = ", json);
        } catch (IllegalStateException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger, e.getMessage());
        }
        return ok;
    }

    private void doGetConfigs(ToJson json, String drvId, HttpServletResponse response) {
        DriverConfig driverConfig = rootConfig.getDriver(drvId);

        if (driverConfig != null) {
            json.addDriverConfig(driverConfig);
        }
        else {
            driverNotAvailable(response, drvId);
        }
    }

    private void doGetConfigField(ToJson json, String drvId, String configField, HttpServletResponse response)
            throws IOException {
        DriverConfig driverConfig = rootConfig.getDriver(drvId);

        if (driverConfig != null) {
            JsonObject jsoConfigAll = ToJson.getDriverConfigAsJsonObject(driverConfig);
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
            driverNotAvailable(response, drvId);
        }
    }

    private void interruptScanProcess(String driverID, HttpServletResponse response, String json) {
        try {
            configService.interruptDeviceScan(driverID);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (UnsupportedOperationException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Driver does not support scan interrupting.", DRIVER_ID, driverID);
        } catch (DriverNotAvailableException e) {
            driverNotAvailable(response, driverID);
        }
    }

    private List<DeviceScanInfo> scanForAllDrivers(String driverID, String settings,
            DeviceScanListenerImplementation scanListener, HttpServletResponse response) {
        List<DeviceScanInfo> scannedDevicesList = new ArrayList<>();

        try {
            configService.scanForDevices(driverID, settings, scanListener);
            scannedDevicesList = scanListener.getScannedDevicesList();

        } catch (UnsupportedOperationException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Driver does not support scanning.", DRIVER_ID, driverID);
        } catch (DriverNotAvailableException e) {
            driverNotAvailable(response, driverID);
        }

        return scannedDevicesList;
    }

    private List<Channel> getDriverChannelList(Collection<ChannelConfig> channelConfig) {
        List<Channel> driverChannels = new ArrayList<>();

        for (ChannelConfig chCf : channelConfig) {
            driverChannels.add(dataAccess.getChannel(chCf.getId()));
        }

        return driverChannels;
    }

    private void setDriverDevicesListAndChannelConfigList(List<String> driverDevices,
            Collection<ChannelConfig> channelConfig, Collection<DeviceConfig> deviceConfig) {
        for (DeviceConfig dvCf : deviceConfig) {
            driverDevices.add(dvCf.getId());
            channelConfig.addAll(dvCf.getChannels());
        }
    }

    private void setConfigAccess() {
        this.dataAccess = handleDataAccessService(null);
        this.configService = handleConfigService(null);
        this.rootConfig = handleRootConfig(null);
    }

}
