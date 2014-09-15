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
package org.openmuc.framework.server.restws.servlets;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.server.restws.Activator;
import org.openmuc.framework.server.restws.JsonHelper;
import org.openmuc.framework.server.restws.PathHandler;
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

@SuppressWarnings("serial")
public class DeviceResourceServlet extends HttpServlet {
    private final static Logger logger = LoggerFactory.getLogger(DeviceResourceServlet.class);
    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootCfg;

    @Override
    public void init() throws ServletException {
        this.dataAccess = Activator.getDataAccess();
        this.configService = Activator.getConfigService();
        this.rootCfg = configService.getConfig();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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

                out.println(JsonHelper.ListToJsonArray(devices));
            } else {
                devId = pathInfo.replace("/", "");
                List<Channel> deviceChannels = new ArrayList<Channel>();

                if (devices.contains(devId)) {

                    Collection<ChannelConfig> channelConfig = new ArrayList<ChannelConfig>();

                    channelConfig = rootCfg.getDevice(devId).getChannels();
                    for (ChannelConfig chCf : channelConfig) {
                        deviceChannels.add(dataAccess.getChannel(chCf.getId()));
                    }

                    out.println(JsonHelper.ChannelRecordListToJsonArray(deviceChannels));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            devId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];
            doGetConfigInfo(out, devId, configField, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void doGetConfigInfo(PrintWriter out,
                                 String devId,
                                 String configField,
                                 HttpServletResponse response)
            throws IOException {
        DeviceConfig deviceConfig;
        deviceConfig = rootCfg.getDevice(devId);
        if (deviceConfig != null) {
            if (configField.equals("deviceAddress")) {
                if (deviceConfig.getDeviceAddress() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.getDeviceAddress()));
                }

            } else if (configField.equals("description")) {
                if (deviceConfig.getDescription() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.getDescription()));
                }

            } else if (configField.equals("samplingTimeout")) {
                if (deviceConfig.getSamplingTimeout() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.getSamplingTimeout()
                                                                         .toString()));
                }
            } else if (configField.equals("settings")) {
                if (deviceConfig.getSettings() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.getSettings()));
                }
            } else if (configField.equals("interfaceAddress")) {
                if (deviceConfig.getInterfaceAddress() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper
                                        .ConfigValueToJson(configField,
                                                           deviceConfig.getInterfaceAddress()
                                                                       .toString()));
                }

            } else if (configField.equals("connectRetryInterval")) {
                if (deviceConfig.getConnectRetryInterval() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.getConnectRetryInterval()
                                                                         .toString()));
                }
            } else if (configField.equals("isDisabled")) {
                if (deviceConfig.isDisabled() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             deviceConfig.isDisabled().toString()));
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        }

    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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

        ArrayList<Channel> deviceChannels = new ArrayList<Channel>();
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

        String line, text = "";
        try {
            while ((line = br.readLine()) != null) {
                text += line;
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        in.close();

        if (!request.getContentType().equals("application/json")) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {

            if (!devices.contains(devId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Collection<ChannelConfig> channelConfig = new ArrayList<ChannelConfig>();

                channelConfig = rootCfg.getDevice(devId).getChannels();
                for (ChannelConfig chCf : channelConfig) {
                    deviceChannels.add(dataAccess.getChannel(chCf.getId()));
                }
                ArrayList<Value> values = JsonHelper.ChannelsJsonToValues(text, deviceChannels);
                List<WriteValueContainer> writeValues = new ArrayList<WriteValueContainer>();

                if (values != null) {
                    int i = 0;
                    ArrayList<Flag> flags = new ArrayList<Flag>();
                    for (Channel drv : deviceChannels) {
                        WriteValueContainer wvc = drv.getWriteContainer();
                        wvc.setValue(values.get(i));
                        writeValues.add(wvc);
                        ++i;
                    }
                    dataAccess.write(writeValues);
                    for (WriteValueContainer wvc : writeValues) {
                        flags.add(wvc.getFlag());
                    }
                    out.println("SEND: " + JsonHelper.ChannelFlagsToJson(deviceChannels, flags));

                } else {

                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            devId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];

            if (devices.contains(devId)) {
                try {
                    String configValue = JsonHelper.JsonToConfigValue(text);
                    doPutConfigInfo(out, configValue, devId, configField, response);
                }
                catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        }
    }

    private void doPutConfigInfo(PrintWriter out,
                                 String configValue,
                                 String devId,
                                 String configField,
                                 HttpServletResponse response)
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

            } else if (configField.equals("interfaceAddress")) {
                deviceConfig.setInterfaceAddress(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("isDisabled")) {
                deviceConfig.setDisabled(Boolean.getBoolean(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("setId")) {
                try {
                    deviceConfig.setId(configValue);
                }
                catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("addChannel")) {
                try {
                    deviceConfig.addChannel(configValue);
                }
                catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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

        String line, text = "";
        try {
            while ((line = br.readLine()) != null) {
                text += line;
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        devId = JsonHelper.JsonToConfigValue(text);
        devId = devId.replace("\"", "");
        in.close();
        if (!request.getContentType().equals("application/json") || devId == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfo.equals("/delete") || pathInfo.equals("/delete/")) {

            try {
                DeviceConfig deviceConfig;
                deviceConfig = rootCfg.getDevice(devId);
                deviceConfig.delete();
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            }
            catch (ConfigWriteException e) {
                e.printStackTrace();
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    public void destroy() {

    }
}
