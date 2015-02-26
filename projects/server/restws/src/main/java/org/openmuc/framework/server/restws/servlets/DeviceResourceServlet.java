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
package org.openmuc.framework.server.restws.servlets;

import com.google.gson.JsonArray;
import org.openmuc.framework.config.*;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.server.restws.Const;
import org.openmuc.framework.server.restws.JsonHelper;
import org.openmuc.framework.server.restws.PathHandler;
import org.openmuc.framework.server.restws.RestServer;
import org.openmuc.framework.server.restws.objects.RestChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeviceResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 4619892734239871891L;
    private final static Logger logger = LoggerFactory.getLogger(DeviceResourceServlet.class);

    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootCfg;

    private final ServletLib lib = new ServletLib();

    @Override
    public void init() throws ServletException {
        this.dataAccess = RestServer.getDataAccessService();
        this.configService = RestServer.getConfigService();
        this.rootCfg = configService.getConfig();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        String devId, configField;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }
        if (PathHandler.isValidRequest(pathInfo, queryStr)) {

            List<String> devices = new ArrayList<String>();

            Collection<DriverConfig> driverConfig = new ArrayList<DriverConfig>();
            driverConfig = rootCfg.getDrivers();

            Collection<DeviceConfig> deviceConfig = new ArrayList<DeviceConfig>();

            for (DriverConfig drvCfg : driverConfig) {
                String driverId = drvCfg.getId();
                deviceConfig.addAll(rootCfg.getDriver(driverId).getDevices());
            }
            for (DeviceConfig devCfg : deviceConfig) {
                devices.add(devCfg.getId());
            }

            if (pathInfo.equals("/")) {
                out.println(JsonHelper.deviceListToJsonObject(devices));
            } else {
                String[] pathInfoArray = pathInfo.replaceFirst("/", "").split("/");
                devId = pathInfoArray[0].replace("/", "");
                List<Channel> deviceChannels = new ArrayList<Channel>();

                if (devices.contains(devId)) {

                    Collection<ChannelConfig> channelConfig = new ArrayList<ChannelConfig>();

                    channelConfig = rootCfg.getDevice(devId).getChannels();
                    for (ChannelConfig chCf : channelConfig) {
                        deviceChannels.add(dataAccess.getChannel(chCf.getId()));
                    }
                    DeviceState deviceState = configService.getDeviceState(devId);
                    if (pathInfoArray.length > 1 && pathInfoArray[1].equals(Const.CHANNELS)) {
                        out.println(JsonHelper.channelListWithDeviceStateToJsonObject(deviceChannels, deviceState));
                    } else if (pathInfoArray.length == 1) {
                        out.println(JsonHelper.channelRecordListWithDeviceStateToJsonObject(deviceChannels, deviceState));
                    } else {
                        String message = "Requested rest device is not available, DeviceID = " + devId;
                        logger.warn(message);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
                    }
                } else {
                    logger.warn("Requested rest device is not available, DeviceID = " + devId);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            devId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];
            doGetConfigInfo(out, devId, configField, response);
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        out.flush();
        out.close();
    }

    private void doGetConfigInfo(PrintWriter out, String devId, String configField, HttpServletResponse response) throws IOException {

        DeviceConfig deviceConfig;
        deviceConfig = rootCfg.getDevice(devId);
        if (deviceConfig != null) {
            JsonArray jsa = new JsonArray();
            jsa.add(JsonHelper.deviceConfigToJsonObject(deviceConfig).get(configField));
            out.print(jsa);
        } else {
            logger.warn("Requested rest device is not available, DeviceID = " + devId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        String devId = pathInfo.replace("/", ""), configField;

        List<String> devices = new ArrayList<String>();

        Collection<DriverConfig> driverConfig = new ArrayList<DriverConfig>();
        driverConfig = rootCfg.getDrivers();

        Collection<DeviceConfig> deviceConfig = new ArrayList<DeviceConfig>();

        for (DriverConfig drvCfg : driverConfig) {
            String driverId = drvCfg.getId();
            deviceConfig.addAll(rootCfg.getDriver(driverId).getDevices());
        }
        for (DeviceConfig devCfg : deviceConfig) {
            devices.add(devCfg.getId());
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String text = lib.buildString(br);
        in.close();

        if (!request.getContentType().equals("application/json")) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType());
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {

            if (!devices.contains(devId)) {
                logger.warn("Requested rest device is not available, DeviceID = " + devId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                ArrayList<RestChannel> values = JsonHelper.channelsJsonToRecords(text);
                if (values != null) {
                    while (values.iterator().hasNext()) {
                        RestChannel restC = values.iterator().next();
                        String id = restC.getId();
                        Record rec = restC.getRecord();

                        Channel ch = dataAccess.getChannel(id);
                        if (ch != null) {
                            ch.setLatestRecord(rec);
                        } else {
                            logger.warn("Requested rest channel is not available, ChannelID = " + id);
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            break;
                        }
                    }
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            devId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];

            if (devices.contains(devId)) {
                try {
                    String configValue = JsonHelper.jsonToConfigValue(text);
                    doPutConfigInfo(out, configValue, devId, configField, response);
                } catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        }
        out.flush();
        out.close();
    }

    private void doPutConfigInfo(PrintWriter out, String configValue, String devId, String configField, HttpServletResponse response)
            throws ConfigWriteException, IOException {

        DeviceConfig deviceConfig;
        deviceConfig = rootCfg.getDevice(devId);
        configValue = configValue.replace("\"", "");

        if (deviceConfig != null) {
            if (configField.equals("deviceAddress")) {
                deviceConfig.setDeviceAddress(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("description")) {
                deviceConfig.setDescription(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("samplingTimeout")) {
                deviceConfig.setSamplingTimeout(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("settings")) {
                deviceConfig.setSettings(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("connectRetryInterval")) {
                deviceConfig.setConnectRetryInterval(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("isDisabled")) {
                deviceConfig.setDisabled(Boolean.getBoolean(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("setId")) {
                try {
                    deviceConfig.setId(configValue);
                } catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("addChannel")) {
                try {
                    deviceConfig.addChannel(configValue);
                } catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else {
                logger.warn("Requested rest configService is not available, configServiceID = " + configField);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.getWriter();
        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        String devId;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        devId = JsonHelper.jsonToConfigValue(lib.buildString(br));
        in.close();
        if (devId == null) {
            logger.warn("Device id is null");
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
        } else if (!request.getContentType().equals("application/json")) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType() + " and DeviceID =  " + devId);
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfo.equals("/delete") || pathInfo.equals("/delete/")) {

            try {
                DeviceConfig deviceConfig;
                devId = devId.replace("\"", "");
                deviceConfig = rootCfg.getDevice(devId);
                deviceConfig.delete();
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } catch (ConfigWriteException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("Requested device is not available, DeviceID = " + devId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    public void destroy() {

    }

}
