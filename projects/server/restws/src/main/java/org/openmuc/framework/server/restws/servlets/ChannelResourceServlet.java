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
import org.openmuc.framework.server.restws.Activator;
import org.openmuc.framework.server.restws.JsonHelper;
import org.openmuc.framework.server.restws.PathHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ChannelResourceServlet extends HttpServlet {

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
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void doGetConfigInfo(PrintWriter out,
                                 String chId,
                                 String configField,
                                 HttpServletResponse response)
            throws IOException {
        ChannelConfig channelConfig;
        channelConfig = rootCfg.getChannel(chId);

        if (channelConfig != null) {
            if (configField.equals("channelAddress")) {
                if (channelConfig.getChannelAddress() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getChannelAddress()));
                }

            } else if (configField.equals("description")) {
                if (channelConfig.getDescription() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getDescription()));
                }

            } else if (configField.equals("samplingGroup")) {
                if (channelConfig.getSamplingGroup() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getSamplingGroup()));
                }
            } else if (configField.equals("unit")) {
                if (channelConfig.getUnit() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField, channelConfig.getUnit()));
                }
            } else if (configField.equals("loggingInterval")) {
                if (channelConfig.getLoggingInterval() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper
                                        .ConfigValueToJson(configField,
                                                           channelConfig.getLoggingInterval()
                                                                        .toString()));
                }

            } else if (configField.equals("loggingTimeOffset")) {
                if (channelConfig.getLoggingTimeOffset() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getLoggingTimeOffset()
                                                                          .toString()));
                }
            } else if (configField.equals("samplingInterval")) {
                if (channelConfig.getSamplingInterval() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getSamplingInterval()
                                                                          .toString()));
                }
            } else if (configField.equals("samplingTimeOffset")) {
                if (channelConfig.getSamplingTimeOffset() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getSamplingTimeOffset()
                                                                          .toString()));
                }
            } else if (configField.equals("scalingFactor")) {
                if (channelConfig.getScalingFactor() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getScalingFactor()
                                                                          .toString()));
                }

            } else if (configField.equals("valueOffset")) {
                if (channelConfig.getValueOffset() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getValueOffset()
                                                                          .toString()));
                }
            } else if (configField.equals("valueType")) {
                if (channelConfig.getValueOffset() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.getValueType().name()
                                                                          .toString()));
                }
            } else if (configField.equals("valueTypeLength")) {
                if (channelConfig.getValueTypeLength() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper
                                        .ConfigValueToJson(configField,
                                                           channelConfig.getValueTypeLength()
                                                                        .toString()));
                }
            } else if (configField.equals("isDisabled")) {
                if (channelConfig.isDisabled() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.isDisabled()
                                                                          .toString()));
                }
            } else if (configField.equals("isListening")) {
                if (channelConfig.isListening() == null) {
                    out.println(JsonHelper.ConfigValueToJson(configField, "null"));
                } else {
                    out.println(JsonHelper.ConfigValueToJson(configField,
                                                             channelConfig.isListening()
                                                                          .toString()));
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
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

        List<String> ids = dataAccess.getAllIds();

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

        Value value = JsonHelper.JsonToValue(text);
        String configValue = JsonHelper.JsonToConfigValue(text);
        in.close();
        if (!request.getContentType().equals("application/json") || configValue == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (PathHandler.isValidRequest(pathInfo, queryStr) && value != null) {
            Flag flag = null;
            String chId = pathInfo.replace("/", "");

            if (ids.contains(chId)) {
                Channel channel = dataAccess.getChannel(chId);
                flag = channel.write(value);
                out.println(JsonHelper.FlagToJson(flag));

            }

        } else if (PathHandler.isValidConfigRequest(pathInfo, queryStr)) {
            String chId = pathInfo.split("\\/")[1];
            String configField = pathInfo.split("\\/")[2];

            if (ids.contains(chId)) {
                try {
                    doPutConfigInfo(out, configValue, chId, configField, response);
                }
                catch (ConfigWriteException cwe) {
                    cwe.printStackTrace();
                }
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void doPutConfigInfo(PrintWriter out,
                                 String configValue,
                                 String chId,
                                 String configField,
                                 HttpServletResponse response)
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
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void doGetHistory(PrintWriter out,
                              String chId,
                              String fromParameter,
                              String untilParameter) {
        long fromTimeStamp = 0, untilTimeStamp = 0;

        List<String> ids = dataAccess.getAllIds();
        List<Record> records = null;

        if (ids.contains(chId)) {
            Channel chnl = dataAccess.getChannel(chId);

            try {
                fromTimeStamp = Long.parseLong(fromParameter);
                untilTimeStamp = Long.parseLong(untilParameter);
            }
            catch (NumberFormatException ex) {
            }

            try {
                records = chnl.getLoggedRecords(fromTimeStamp, untilTimeStamp);
            }
            catch (DataLoggerNotAvailableException e) {

                e.printStackTrace();
            }
            catch (IOException e) {

                e.printStackTrace();
            }

            out.println(JsonHelper.RecordListToJsonArray(records));
        }
    }

    private void doGetSpecificChannel(PrintWriter out, String chId, HttpServletResponse response)
            throws IOException {

        List<String> ids = dataAccess.getAllIds();

        if (ids.contains(chId)) {
            Record rc = dataAccess.getChannel(chId).getLatestRecord();
            out.println(JsonHelper.RecordToJson(rc));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void doGetAllChannels(PrintWriter out) {

        List<String> ids = dataAccess.getAllIds();
        List<Channel> channels = new ArrayList<Channel>(ids.size());

        for (String id : ids) {
            channels.add(dataAccess.getChannel(id));
        }

        out.println(JsonHelper.ChannelRecordListToJsonArray(channels));
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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

        String line, text = "";
        try {
            while ((line = br.readLine()) != null) {
                text += line;
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        chId = JsonHelper.JsonToConfigValue(text);
        chId = chId.replace("\"", "");
        in.close();
        if (!request.getContentType().equals("application/json") || chId == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else if (pathInfo.equals("/delete") || pathInfo.equals("/delete/")) {

            ChannelConfig channelConfig;
            channelConfig = rootCfg.getChannel(chId);
            try {

                channelConfig.delete();
                configService.setConfig(rootCfg);
                configService.writeConfigToFile();
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
            catch (ConfigWriteException e) {
                e.printStackTrace();
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }
}
