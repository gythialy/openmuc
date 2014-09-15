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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class DriverResourceServlet extends HttpServlet {

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

                out.println(JsonHelper.ListToJsonArray(drivers));
            } else {
                List<Channel> driverChannels = new ArrayList<Channel>();
                drvId = pathInfo.replace("/", "");
                if (drivers.contains(drvId)) {

                    Collection<DeviceConfig> deviceConfig = new ArrayList<DeviceConfig>();
                    deviceConfig = rootCfg.getDriver(drvId).getDevices();

                    Collection<ChannelConfig> channelConfig = new ArrayList<ChannelConfig>();

                    for (DeviceConfig dvCf : deviceConfig) {
                        channelConfig.addAll(dvCf.getChannels());
                    }
                    for (ChannelConfig chCf : channelConfig) {
                        driverChannels.add(dataAccess.getChannel(chCf.getId()));
                    }

                    out.println(JsonHelper.ChannelRecordListToJsonArray(driverChannels));

                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }

            }

        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            drvId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];
            doGetConfigInfo(out, drvId, configField, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void doGetConfigInfo(PrintWriter out,
                                 String drvId,
                                 String configField,
                                 HttpServletResponse response)
            throws IOException {
        DriverConfig driverConfig;
        driverConfig = rootCfg.getDriver(drvId);

        if (driverConfig != null) {
            if (configField.equals("samplingTimeout")) {
                if (driverConfig.getSamplingTimeout() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             driverConfig.getSamplingTimeout()
                                                                         .toString()));
                }
            } else if (configField.equals("devices")) {
                if (driverConfig.getDevices() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    List<String> devices = new ArrayList<String>();
                    for (DeviceConfig devCfg : driverConfig.getDevices()) {

                        devices.add(devCfg.getId());
                    }
                    out.println(JsonHelper.ListToJsonArray(devices));
                }
            } else if (configField.equals("connectRetryInterval")) {
                if (driverConfig.getConnectRetryInterval() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             driverConfig.getConnectRetryInterval()
                                                                         .toString()));
                }
            } else if (configField.equals("isDisabled")) {
                if (driverConfig.isDisabled() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             driverConfig.isDisabled().toString()));
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

        ArrayList<Channel> driverChannels = new ArrayList<Channel>();
        String drvId = pathInfo.replace("/", ""), configField;

        List<String> drivers = new ArrayList<String>();
        Collection<DriverConfig> driverConfig = new ArrayList<DriverConfig>();
        driverConfig = rootCfg.getDrivers();

        for (DriverConfig drv : driverConfig) {
            drivers.add(drv.getId());
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

            if (!drivers.contains(drvId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Collection<DeviceConfig> deviceConfig = new ArrayList<DeviceConfig>();
                deviceConfig = rootCfg.getDriver(drvId).getDevices();

                List<ChannelConfig> channelConfigs = new ArrayList<ChannelConfig>();

                for (DeviceConfig dvCf : deviceConfig) {
                    channelConfigs.addAll(dvCf.getChannels());
                }
                for (ChannelConfig chCf : channelConfigs) {
                    driverChannels.add(dataAccess.getChannel(chCf.getId()));
                }

                ArrayList<Value> values = JsonHelper.ChannelsJsonToValues(text, driverChannels);
                List<WriteValueContainer> writeValues = new ArrayList<WriteValueContainer>();

                if (values != null) {
                    int i = 0;
                    ArrayList<Flag> flags = new ArrayList<Flag>();
                    for (Channel drv : driverChannels) {
                        WriteValueContainer wvc = drv.getWriteContainer();
                        wvc.setValue(values.get(i));
                        writeValues.add(wvc);
                        ++i;
                    }
                    dataAccess.write(writeValues);
                    for (WriteValueContainer wvc : writeValues) {
                        flags.add(wvc.getFlag());
                    }
                    out.println(JsonHelper.ChannelFlagsToJson(driverChannels, flags));

                } else {

                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            drvId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];

            if (drivers.contains(drvId)) {
                try {
                    String configValue = JsonHelper.JsonToConfigValue(text);
                    doPutConfigInfo(out, configValue, drvId, configField, response);
                }
                catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        }
    }

    private void doPutConfigInfo(PrintWriter out,
                                 String configValue,
                                 String drvId,
                                 String configField,
                                 HttpServletResponse response)
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
                }
                catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("addDevice")) {
                try {
                    driverConfig.addDevice(configValue);
                }
                catch (IdCollisionException e) {
                    e.printStackTrace();
                }
                out.println(configValue);
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
        String drvId;

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
        drvId = JsonHelper.JsonToConfigValue(text);
        drvId = drvId.replace("\"", "");
        in.close();
        if (!request.getContentType().equals("application/json") || drvId == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfo.equals("/delete") || pathInfo.equals("/delete/")) {

            try {
                DriverConfig driverConfig;
                driverConfig = rootCfg.getDriver(drvId);
                driverConfig.delete();
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
