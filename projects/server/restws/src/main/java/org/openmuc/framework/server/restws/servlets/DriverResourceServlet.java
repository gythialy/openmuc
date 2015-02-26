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

public class DriverResourceServlet extends HttpServlet {

    private static final long serialVersionUID = -2223282905555493215L;
    private final static Logger logger = LoggerFactory.getLogger(DriverResourceServlet.class);

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
        String drvId = "", configField;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }
        if (PathHandler.isValidRequest(pathInfo, queryStr)) {
            List<String> drivers = new ArrayList<String>();

            Collection<DriverConfig> driverConfig = new ArrayList<DriverConfig>();
            driverConfig = rootCfg.getDrivers();

            for (DriverConfig drv : driverConfig) {
                drivers.add(drv.getId());
            }

            if (pathInfo.equals("/")) {
                out.println(JsonHelper.driverListToJsonObject(drivers));
            } else {
                String[] pathInfoArray = pathInfo.replaceFirst("/", "").split("/");
                drvId = pathInfoArray[0].replace("/", "");
                List<Channel> driverChannels = new ArrayList<Channel>();
                List<String> driverDevices = new ArrayList<String>();

                if (drivers.contains(drvId)) {
                    Collection<DeviceConfig> deviceConfig = new ArrayList<DeviceConfig>();
                    DriverConfig drv = rootCfg.getDriver(drvId);
                    deviceConfig = drv.getDevices();

                    Collection<ChannelConfig> channelConfig = new ArrayList<ChannelConfig>();

                    for (DeviceConfig dvCf : deviceConfig) {
                        driverDevices.add(dvCf.getId());
                        channelConfig.addAll(dvCf.getChannels());
                    }
                    for (ChannelConfig chCf : channelConfig) {
                        driverChannels.add(dataAccess.getChannel(chCf.getId()));
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    boolean driverIsRunning = configService.getIdsOfRunningDrivers().contains(drvId);

                    if (pathInfoArray.length > 1) {
                        if (pathInfoArray[1].equals(Const.CHANNELS)) {
                            out.println(JsonHelper.channelListWithRunningFlagToJsonObject(driverChannels, driverIsRunning));
                        } else if (pathInfoArray[1].equals(Const.DEVICES)) {
                            out.println(JsonHelper.deviceListWithRunningFlagToJsonObject(driverDevices, driverIsRunning));
                        }
                    } else if (pathInfoArray.length == 1) {
                        out.println(JsonHelper.channelRecordListWithRunningFlagToJsonObject(driverChannels, driverIsRunning));
                    } else {
                        logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Requested rest path is not available.");
                    }
                } else {
                    String message = "Requested rest driver is not available, DriverID = " + drvId;
                    logger.warn(message);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            drvId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];
            doGetConfigInfo(out, drvId, configField, response);
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        out.flush();
        out.close();
    }

    private void doGetConfigInfo(PrintWriter out, String drvId, String configField, HttpServletResponse response) throws IOException {

        DriverConfig driverConfig;
        driverConfig = rootCfg.getDriver(drvId);

        if (driverConfig != null) {
            JsonArray jsa = new JsonArray();
            jsa.add(JsonHelper.driverConfigToJsonObject(driverConfig).get(configField));
            out.print(jsa);
        } else {
            logger.warn("Requested rest driver is not available, DriverID = " + drvId);
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
        String drvId = pathInfo.replace("/", ""), configField;

        List<String> drivers = new ArrayList<String>();
        Collection<DriverConfig> driverConfig = new ArrayList<DriverConfig>();
        driverConfig = rootCfg.getDrivers();

        for (DriverConfig drv : driverConfig) {
            drivers.add(drv.getId());
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String text = lib.buildString(br);
        in.close();
        if (!request.getContentType().equals("application/json")) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType());
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {

            if (!drivers.contains(drvId)) {
                logger.warn("Requested rest driver is not available, DriverID = " + drvId);
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
            drvId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];

            if (drivers.contains(drvId)) {
                try {
                    String configValue = JsonHelper.jsonToConfigValue(text);
                    doPutConfigInfo(out, configValue, drvId, configField, response);
                } catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        }
        out.flush();
        out.close();
    }

    private void doPutConfigInfo(PrintWriter out, String configValue, String drvId, String configField, HttpServletResponse response)
            throws ConfigWriteException, IOException {

        DriverConfig driverConfig;
        driverConfig = rootCfg.getDriver(drvId);
        configValue = configValue.replace("\"", "");

        if (driverConfig != null) {
            if (configField.equals("samplingTimeout")) {
                driverConfig.setSamplingTimeout(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("connectRetryInterval")) {
                driverConfig.setConnectRetryInterval(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("isDisabled")) {
                driverConfig.setDisabled(Boolean.getBoolean(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("setId")) {
                try {
                    driverConfig.setId(configValue);
                } catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("addDevice")) {
                try {
                    driverConfig.addDevice(configValue);
                } catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                out.println(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else {
                logger.warn("Requested rest configService is not available, configServiceID = " + configField);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.getWriter();

        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String value = JsonHelper.jsonToConfigValue(lib.buildString(br));
        in.close();
        if (value == null) {
            logger.warn("Value is null");
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
        } else if (!request.getContentType().equals("application/json")) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType() + " value = " + value);
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {
            if (pathInfo.equals("/")) {
                try {
                    value = value.replace("\"", "");
                    rootCfg.addDriver(value);
                    configService.setConfig(rootCfg);
                    configService.writeConfigToFile();
                } catch (IdCollisionException e) {
                    e.printStackTrace();
                } catch (ConfigWriteException e) {
                    e.printStackTrace();
                }
            } else {
                logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.getWriter();
        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        String drvId = null;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        String[] pathInfoArray = pathInfo.replaceFirst("/", "").split("/");

        drvId = pathInfoArray[0].replace("/", "");

        if (!request.getContentType().equals("application/json") || !PathHandler.isValidRequest(pathInfo, queryStr)) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType() + " DriverID =  " + drvId);
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfoArray.length != 1) {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                DriverConfig driverConfig;
                driverConfig = rootCfg.getDriver(drvId);
                if (driverConfig == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Driver \"" + drvId + "\" does not exist.");
                } else {
                    driverConfig.delete();
                    configService.setConfig(rootCfg);
                    configService.writeConfigToFile();

                    if (rootCfg.getDriver(drvId) == null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not able to delete driver " + drvId);
                    }
                }

            } catch (ConfigWriteException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ConfigWriteException");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {

    }

}
