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
import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.server.restws.JsonHelper;
import org.openmuc.framework.server.restws.PathHandler;
import org.openmuc.framework.server.restws.RestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelResourceServlet extends HttpServlet {

    private static final long serialVersionUID = -702876016040151438L;
    private final static Logger logger = LoggerFactory.getLogger(ChannelResourceServlet.class);

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
        String chId, fromParameter, untilParameter, configField;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        if (PathHandler.isValidRequest(pathInfo, queryStr)) {
            if (pathInfo.equals("/")) {
                doGetAllChannels(out);
            } else {
                chId = pathInfo.replace("/", "");
                doGetSpecificChannel(out, chId, response);
            }
        } else if (PathHandler.isValidHistoryRequest(pathInfo, queryStr)) {
            chId = pathInfo.replace("/", "").replace("history", "");
            fromParameter = request.getParameter("from");
            untilParameter = request.getParameter("until");
            doGetHistory(out, chId, fromParameter, untilParameter);
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            chId = pathInfo.split("\\/")[1];
            configField = pathInfo.split("\\/")[2];
            doGetConfigInfo(out, chId, configField, response);
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        out.flush();
        out.close();
    }

    private void doGetConfigInfo(PrintWriter out, String chId, String configField, HttpServletResponse response) throws IOException {
        ChannelConfig channelConfig = rootCfg.getChannel(chId);
        if (channelConfig != null) {
            JsonArray jsa = new JsonArray(); // TODO:
            jsa.add(JsonHelper.channelConfigToJsonObject(channelConfig).get(configField));
            out.print(jsa);
        } else {
            logger.warn("Requested rest channel is not available, ChannelID = " + chId);
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

        List<String> ids = dataAccess.getAllIds();

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String text = lib.buildString(br);
        Value value = null;
        String configValue = JsonHelper.jsonToConfigValue(text);
        in.close();
        if (!request.getContentType().equalsIgnoreCase("application/json") || configValue == null) {
            logger.warn(
                    "Requested rest was not a json media type. MediaType = " + request.getContentType() + " configValue = " + configValue);
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr)) {
            Flag flag = null;
            String chId = pathInfo.replace("/", "");

            if (ids.contains(chId)) {
                Channel channel = dataAccess.getChannel(chId);
                value = JsonHelper.jsonToValue(channel.getValueType(), text);
                if (value == null) {
                    logger.warn("Value from channel \"" + channel.getId() + "\" is null.");
                } else {
                    flag = channel.write(value);
                    out.println(JsonHelper.flagToJson(flag));
                }
            }
        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            String chId = pathInfo.split("\\/")[1];
            String configField = pathInfo.split("\\/")[2];

            if (ids.contains(chId)) {
                try {
                    doPutConfigInfo(out, configValue, chId, configField, response);
                } catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void doPutConfigInfo(PrintWriter out, String configValue, String chId, String configField, HttpServletResponse response)
            throws IOException, ConfigWriteException {

        ChannelConfig channelConfig;
        channelConfig = rootCfg.getChannel(chId);
        configValue = configValue.replace("\"", "");

        if (channelConfig != null) {
            if (configField.equals("channelAddress")) {
                channelConfig.setChannelAddress(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("description")) {
                channelConfig.setDescription(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("samplingGroup")) {
                channelConfig.setSamplingGroup(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("unit")) {
                channelConfig.setUnit(configValue);
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("loggingInterval")) {
                channelConfig.setLoggingInterval(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("loggingTimeOffset")) {
                channelConfig.setLoggingTimeOffset(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("samplingInterval")) {
                channelConfig.setSamplingInterval(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("samplingTimeOffset")) {
                channelConfig.setSamplingTimeOffset(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("scalingFactor")) {
                channelConfig.setScalingFactor(Double.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("valueOffset")) {
                channelConfig.setValueOffset(Double.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("valueType")) {
                channelConfig.setValueType(ValueType.valueOf(configField));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("valueTypeLength")) {
                channelConfig.setValueTypeLength(Integer.valueOf(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("isDisabled")) {
                channelConfig.setDisabled(Boolean.getBoolean(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else if (configField.equals("isListening")) {
                channelConfig.setListening(Boolean.getBoolean(configValue));
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } else {
                logger.warn("Requested rest configService is not available, configServiceID = " + configField);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void doGetHistory(PrintWriter out, String chId, String fromParameter, String untilParameter) {
        long fromTimeStamp = 0, untilTimeStamp = 0;

        List<String> ids = dataAccess.getAllIds();
        List<Record> records = null;

        if (ids.contains(chId)) {
            Channel channel = dataAccess.getChannel(chId);

            try {
                fromTimeStamp = Long.parseLong(fromParameter);
                untilTimeStamp = Long.parseLong(untilParameter);
            } catch (NumberFormatException ex) {
            }

            try {
                records = channel.getLoggedRecords(fromTimeStamp, untilTimeStamp);
            } catch (DataLoggerNotAvailableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.println(JsonHelper.recordListToJsonArray(channel.getValueType(), records));
        }
    }

    private void doGetSpecificChannel(PrintWriter out, String chId, HttpServletResponse response) throws IOException {

        Channel channel = dataAccess.getChannel(chId);
        if (channel != null) {
            Record rc = channel.getLatestRecord();
            out.println(JsonHelper.recordToJson(channel.getValueType(), rc));
        } else {
            logger.warn("Requested rest channel is not available, ChannelID = " + chId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void doGetAllChannels(PrintWriter out) {

        List<String> ids = dataAccess.getAllIds();
        List<Channel> channels = new ArrayList<Channel>(ids.size());

        for (String id : ids) {
            channels.add(dataAccess.getChannel(id));
        }
        out.println(JsonHelper.channelRecordListToJsonArray(channels));
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.getWriter();
        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        String chId;

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        InputStream in = request.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        chId = JsonHelper.jsonToConfigValue(lib.buildString(br));
        in.close();

        if (chId == null) {
            logger.warn("Channel id is null");
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
        } else if (!request.getContentType().equalsIgnoreCase("application/json")) {
            logger.warn("Requested rest was not a json media type. MediaType = " + request.getContentType() + " and ChannelID = " + chId);
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfo.equalsIgnoreCase("/delete") || pathInfo.equalsIgnoreCase("/delete/")) {

            ChannelConfig channelConfig;
            chId = chId.replace("\"", "");
            channelConfig = rootCfg.getChannel(chId);
            try {
                channelConfig.delete();
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (ConfigWriteException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("Requested rest path is not available, Path Info = " + request.getPathInfo());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
