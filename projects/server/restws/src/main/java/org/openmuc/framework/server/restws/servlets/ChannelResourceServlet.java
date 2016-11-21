/*
 * Copyright 2011-16 Fraunhofer ISE
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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.lib.json.Const;
import org.openmuc.framework.lib.json.FromJson;
import org.openmuc.framework.lib.json.ToJson;
import org.openmuc.framework.lib.json.exceptions.MissingJsonObjectException;
import org.openmuc.framework.lib.json.exceptions.RestConfigIsNotCorrectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class ChannelResourceServlet extends GenericServlet {

    private static final long serialVersionUID = -702876016040151438L;
    private final static Logger logger = LoggerFactory.getLogger(ChannelResourceServlet.class);

    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootConfig;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String channelID, fromParameter, untilParameter, configField;
            String pathInfo = pathAndQueryString[0];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);

            response.setStatus(HttpServletResponse.SC_OK);

            ToJson json = new ToJson();

            if (pathInfo.equals("/")) {
                doGetAllChannels(json);
            }
            else {
                channelID = pathInfoArray[0].replace("/", "");
                if (pathInfoArray.length == 1) {
                    doGetSpecificChannel(json, channelID, response);
                }
                else if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                    doGetConfigs(json, channelID, response);
                }
                else if (pathInfoArray.length == 3 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                    configField = pathInfoArray[2];
                    doGetConfigField(json, channelID, configField, response);
                }
                else if (pathInfoArray.length == 2 && pathInfoArray[1].startsWith(Const.HISTORY)) {
                    fromParameter = request.getParameter("from");
                    untilParameter = request.getParameter("until");
                    doGetHistory(json, channelID, fromParameter, untilParameter, response);
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
                }
            }
            sendJson(json, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            String channelID = pathInfoArray[0].replace("/", "");
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfoArray.length == 1) {
                setAndWriteChannelConfig(channelID, response, json, false);
            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
            }

        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            String channelID = pathInfoArray[0].replace("/", "");
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfoArray.length < 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
            }
            else {
                ChannelConfig channelConfig = rootConfig.getChannel(channelID);

                if (channelConfig != null) {

                    if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                        setAndWriteChannelConfig(channelID, response, json, true);
                    }
                    else if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.LATESTRECORD)) {
                        doSetRecord(channelID, response, json);
                    }
                    else if (pathInfoArray.length == 1) {
                        doWriteChannel(channelID, response, json);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
                    }
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "Requested channel is not available.", " Channel = ", channelID);
                }
            }
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[0];
            String channelId;
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            ChannelConfig channelConfig;

            channelId = pathInfoArray[0].replace("/", "");
            channelConfig = rootConfig.getChannel(channelId);

            if (pathInfoArray.length != 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available", " Path Info = ", request.getPathInfo());
            }
            else if (channelConfig == null) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Channel \"" + channelId + "\" does not exist.");
            }
            else {
                try {
                    channelConfig.delete();
                    configService.setConfig(rootConfig);
                    configService.writeConfigToFile();

                    if (rootConfig.getDriver(channelId) == null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                logger, "Not able to delete channel ", channelId);
                    }
                } catch (ConfigWriteException e) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                            "Not able to write into config.");
                    e.printStackTrace();
                }
            }
        }
    }

    private void doGetConfigField(ToJson json, String channelID, String configField, HttpServletResponse response)
            throws IOException {

        ChannelConfig channelConfig = rootConfig.getChannel(channelID);

        if (channelConfig != null) {
            JsonObject jsoConfigAll = ToJson.getChannelConfigAsJsonObject(channelConfig);
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
                    "Requested rest channel is not available.", " ChannelID = ", channelID);
        }
    }

    private void doGetConfigs(ToJson json, String channelID, HttpServletResponse response) throws IOException {

        ChannelConfig channelConfig = rootConfig.getChannel(channelID);

        if (channelConfig != null) {
            json.addChannelConfig(channelConfig);
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    "Requested rest channel is not available.", " ChannelID = ", channelID);
        }
    }

    private void doGetHistory(ToJson json, String channelID, String fromParameter, String untilParameter,
            HttpServletResponse response) {
        long fromTimeStamp = 0, untilTimeStamp = 0;

        List<String> channelIDs = dataAccess.getAllIds();
        List<Record> records = null;

        if (channelIDs.contains(channelID)) {
            Channel channel = dataAccess.getChannel(channelID);

            try {
                fromTimeStamp = Long.parseLong(fromParameter);
                untilTimeStamp = Long.parseLong(untilParameter);
            } catch (NumberFormatException ex) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_BAD_REQUEST, logger,
                        "From/To value is not a long number.");
            }

            try {
                records = channel.getLoggedRecords(fromTimeStamp, untilTimeStamp);
            } catch (DataLoggerNotAvailableException e) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                        e.getMessage());
            } catch (IOException e) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger, e.getMessage());
            }
            json.addRecordList(records, channel.getValueType());
        }
    }

    private boolean setAndWriteChannelConfig(String channelID, HttpServletResponse response, FromJson json,
            boolean isHTTPPut) {

        boolean ok = false;

        try {
            if (isHTTPPut) {
                ok = setAndWriteHttpPutChannelConfig(channelID, response, json);
            }
            else {
                ok = setAndWriteHttpPostChannelConfig(channelID, response, json);
            }
        } catch (JsonSyntaxException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                    "JSON syntax is wrong.");
        } catch (ConfigWriteException e) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Could not write channel \"", channelID, "\".");
            e.printStackTrace();
        } catch (RestConfigIsNotCorrectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "Not correct formed channel config json.", " JSON = ", json.getJsonObject().toString());
        } catch (Error e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    e.getMessage());
        } catch (MissingJsonObjectException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger, e.getMessage());
        } catch (IllegalStateException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger, e.getMessage());
        }
        return ok;
    }

    private boolean setAndWriteHttpPutChannelConfig(String channelID, HttpServletResponse response, FromJson json)
            throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException,
            MissingJsonObjectException, IllegalStateException {

        boolean ok = false;

        ChannelConfig channelConfig = rootConfig.getChannel(channelID);
        if (channelConfig != null) {
            try {
                json.setChannelConfig(channelConfig, channelID);

                configService.setConfig(rootConfig);
                configService.writeConfigToFile();
            } catch (IdCollisionException e) {

            }
            response.setStatus(HttpServletResponse.SC_OK);
            ok = true;
        }
        else {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, logger,
                    "Not able to access to channel ", channelID);
        }
        return ok;
    }

    private boolean setAndWriteHttpPostChannelConfig(String channelID, HttpServletResponse response, FromJson json)
            throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException, Error,
            MissingJsonObjectException, IllegalStateException {

        boolean ok = false;
        DeviceConfig deviceConfig;

        ChannelConfig channelConfig = rootConfig.getChannel(channelID);

        JsonObject jso = json.getJsonObject();
        String deviceID = jso.get(Const.DEVICE).getAsString();

        if (deviceID != null) {
            deviceConfig = rootConfig.getDevice(deviceID);
        }
        else {
            throw new Error("No device ID in JSON");
        }

        if (deviceConfig == null) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Device does not exists: ", deviceID);
        }
        else if (channelConfig != null) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Channel already exists: ", channelID);
        }
        else {
            try {
                channelConfig = deviceConfig.addChannel(channelID);
                json.setChannelConfig(channelConfig, channelID);

                if ((channelConfig.getValueType() == ValueType.STRING
                        || channelConfig.getValueType() == ValueType.BYTE_ARRAY)
                        && channelConfig.getValueTypeLength() == null) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                            "Channel ", channelID, " with value type ", channelConfig.getValueType().toString(),
                            ", missing valueTypeLength.");
                    channelConfig.delete();
                }
                else {
                    configService.setConfig(rootConfig);
                    configService.writeConfigToFile();
                }
            } catch (IdCollisionException e) {
            }
            response.setStatus(HttpServletResponse.SC_OK);
            ok = true;
        }
        return ok;
    }

    private void doGetSpecificChannel(ToJson json, String chId, HttpServletResponse response) throws IOException {

        Channel channel = dataAccess.getChannel(chId);
        if (channel != null) {
            Record record = channel.getLatestRecord();
            json.addRecord(record, channel.getValueType());
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    "Requested rest channel is not available, ChannelID = " + chId);
        }
    }

    private void doGetAllChannels(ToJson json) {

        List<String> ids = dataAccess.getAllIds();
        List<Channel> channels = new ArrayList<>(ids.size());

        for (String id : ids) {
            channels.add(dataAccess.getChannel(id));

        }
        json.addChannelRecordList(channels);
    }

    private void doSetRecord(String channelID, HttpServletResponse response, FromJson json) throws ClassCastException {

        Channel channel = dataAccess.getChannel(channelID);
        Record record = json.getRecord(channel.getValueType());

        if (record.getFlag() == null) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "No flag setted.");
        }
        else if (record.getValue() == null) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                    "No value setted.");
        }
        else {
            Long timestamp = record.getTimestamp();
            if (timestamp == null) {
                timestamp = System.currentTimeMillis();
            }
            Record rec = new Record(record.getValue(), timestamp, record.getFlag());
            channel.setLatestRecord(rec);
        }
    }

    private void doWriteChannel(String channelID, HttpServletResponse response, FromJson json) {

        Channel channel = dataAccess.getChannel(channelID);

        Value value = json.getValue(channel.getValueType());
        Flag flag = channel.write(value);

        if (flag != Flag.VALID) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Problems by writing to channel. Flag = " + flag.toString());
        }
    }

    private void setConfigAccess() {
        this.dataAccess = handleDataAccessService(null);
        this.configService = handleConfigService(null);
        this.rootConfig = handleRootConfig(null);
    }

}
