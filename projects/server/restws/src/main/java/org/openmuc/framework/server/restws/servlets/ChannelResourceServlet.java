/*
 * Copyright 2011-18 Fraunhofer ISE
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

    private static final String REST_PATH = " Rest Path = ";
    private static final String REQUESTED_REST_PATH_IS_NOT_AVAILABLE = "Requested rest path is not available";
    private static final String APPLICATION_JSON = "application/json";
    private static final long serialVersionUID = -702876016040151438L;
    private static final Logger logger = LoggerFactory.getLogger(ChannelResourceServlet.class);

    private DataAccessService dataAccess;
    private ConfigService configService;
    private RootConfig rootConfig;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);
        java.util.Date time = new java.util.Date(request.getSession().getLastAccessedTime());

        if (pathAndQueryString == null) {
            return;
        }

        setConfigAccess();

        String pathInfo = pathAndQueryString[0];
        String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);

        response.setStatus(HttpServletResponse.SC_OK);

        ToJson json = new ToJson();

        if (pathInfo.equals("/")) {
            doGetAllChannels(json);
        }
        else {
            readSpecificChannels(request, response, pathInfoArray, json);
        }
        sendJson(json, response);
    }

    private void readSpecificChannels(HttpServletRequest request, HttpServletResponse response, String[] pathInfoArray,
            ToJson json) throws IOException {
        String channelId = pathInfoArray[0].replace("/", "");
        java.util.Date time = new java.util.Date(request.getSession().getLastAccessedTime());
        if (pathInfoArray.length == 1) {
            doGetSpecificChannel(json, channelId, response);
        }
        else if (pathInfoArray.length == 2) {
            if (pathInfoArray[1].equalsIgnoreCase(Const.TIMESTAMP)) {
                doGetSpecificChannelField(json, channelId, Const.TIMESTAMP, response);
            }
            else if (pathInfoArray[1].equalsIgnoreCase(Const.FLAG)) {
                doGetSpecificChannelField(json, channelId, Const.FLAG, response);
            }
            else if (pathInfoArray[1].equalsIgnoreCase(Const.VALUE_STRING)) {
                doGetSpecificChannelField(json, channelId, Const.VALUE_STRING, response);
            }
            else if (pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                doGetConfigs(json, channelId, response);
            }
            else if (pathInfoArray[1].startsWith(Const.HISTORY)) {
                String fromParameter = request.getParameter("from");
                String untilParameter = request.getParameter("until");
                doGetHistory(json, channelId, fromParameter, untilParameter, response);
            }
            else if (pathInfoArray[1].equalsIgnoreCase(Const.DRIVER_ID)) {
                doGetDriverId(json, channelId, response);
            }
            else if (pathInfoArray[1].equalsIgnoreCase(Const.DEVICE_ID)) {
                doGetDeviceId(json, channelId, response);
            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }
        }
        else if (pathInfoArray.length == 3 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
            String configField = pathInfoArray[2];
            doGetConfigField(json, channelId, configField, response);
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);
        java.util.Date time = new java.util.Date(request.getSession().getLastAccessedTime());

        if (pathAndQueryString == null) {
            return;
        }

        setConfigAccess();

        String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
        String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
        String channelId = pathInfoArray[0].replace("/", "");
        FromJson json = new FromJson(ServletLib.getJsonText(request));

        if (pathInfoArray.length == 1) {
            setAndWriteChannelConfig(channelId, response, json, false);
        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);
        java.util.Date time = new java.util.Date(request.getSession().getLastAccessedTime());

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            String channelId = pathInfoArray[0].replace("/", "");
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfoArray.length < 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
            }
            else {
                ChannelConfig channelConfig = getChannelConfig(channelId, response);

                if (channelConfig != null) {

                    if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.CONFIGS)) {
                        setAndWriteChannelConfig(channelId, response, json, true);
                    }
                    else if (pathInfoArray.length == 2 && pathInfoArray[1].equalsIgnoreCase(Const.LATESTRECORD)) {
                        doSetRecord(channelId, response, json);
                    }
                    else if (pathInfoArray.length == 1) {
                        doWriteChannel(channelId, response, json);
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                REQUESTED_REST_PATH_IS_NOT_AVAILABLE, REST_PATH, request.getPathInfo());
                    }
                }
            }
        }
    }

    @Override
    public synchronized void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);
        java.util.Date time = new java.util.Date(request.getSession().getLastAccessedTime());

        if (pathAndQueryString != null) {

            setConfigAccess();

            String pathInfo = pathAndQueryString[0];
            String channelId;
            String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);
            ChannelConfig channelConfig;

            channelId = pathInfoArray[0].replace("/", "");
            channelConfig = getChannelConfig(channelId, response);

            if (channelConfig == null) {
                return;
            }

            if (pathInfoArray.length != 1) {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        REQUESTED_REST_PATH_IS_NOT_AVAILABLE, " Path Info = ", request.getPathInfo());
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
                    logger.warn("Failed to write config.", e);
                }
            }
        }
    }

    private ChannelConfig getChannelConfig(String channelId, HttpServletResponse response) {
        ChannelConfig channelConfig = rootConfig.getChannel(channelId);
        if (channelConfig == null) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    "Requested rest channel is not available.", " ChannelID = ", channelId);
        }
        return channelConfig;
    }

    private void doGetConfigField(ToJson json, String channelId, String configField, HttpServletResponse response)
            throws IOException {

        ChannelConfig channelConfig = getChannelConfig(channelId, response);

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
    }

    private void doGetDriverId(ToJson json, String channelId, HttpServletResponse response) {
        ChannelConfig channelConfig = getChannelConfig(channelId, response);

        if (channelConfig != null) {
            String driverId = channelConfig.getDevice().getDriver().getId();
            json.addString(Const.DRIVER_ID, driverId);
        }
    }

    private void doGetDeviceId(ToJson json, String channelId, HttpServletResponse response) {
        ChannelConfig channelConfig = getChannelConfig(channelId, response);

        if (channelConfig != null) {
            String deviceId = channelConfig.getDevice().getId();
            json.addString(Const.DEVICE_ID, deviceId);
        }

    }

    private void doGetConfigs(ToJson json, String channelId, HttpServletResponse response) {
        ChannelConfig channelConfig = getChannelConfig(channelId, response);

        if (channelConfig != null) {
            json.addChannelConfig(channelConfig);
        }
    }

    private void doGetHistory(ToJson json, String channelId, String fromParameter, String untilParameter,
            HttpServletResponse response) {
        long fromTimeStamp = 0;
        long untilTimeStamp = 0;

        List<String> channelIds = dataAccess.getAllIds();
        List<Record> records = null;

        if (channelIds.contains(channelId)) {
            Channel channel = dataAccess.getChannel(channelId);

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

    private boolean setAndWriteChannelConfig(String channelId, HttpServletResponse response, FromJson json,
            boolean isHTTPPut) {
        boolean ok = false;

        try {
            if (isHTTPPut) {
                ok = setAndWriteHttpPutChannelConfig(channelId, response, json);
            }
            else {
                ok = setAndWriteHttpPostChannelConfig(channelId, response, json);
            }
        } catch (JsonSyntaxException e) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_CONFLICT, logger,
                    "JSON syntax is wrong.");
        } catch (ConfigWriteException e) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_CONFLICT, logger,
                    "Could not write channel \"", channelId, "\".");
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

    private synchronized boolean setAndWriteHttpPutChannelConfig(String channelId, HttpServletResponse response,
            FromJson json) throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException,
            MissingJsonObjectException, IllegalStateException {
        boolean ok = false;

        ChannelConfig channelConfig = getChannelConfig(channelId, response);
        if (channelConfig != null) {
            try {
                json.setChannelConfig(channelConfig, channelId);

                configService.setConfig(rootConfig);
                configService.writeConfigToFile();
            } catch (IdCollisionException e) {

            }
            response.setStatus(HttpServletResponse.SC_OK);
            ok = true;
        }
        return ok;
    }

    private synchronized boolean setAndWriteHttpPostChannelConfig(String channelId, HttpServletResponse response,
            FromJson json) throws JsonSyntaxException, ConfigWriteException, RestConfigIsNotCorrectException, Error,
            MissingJsonObjectException, IllegalStateException {
        boolean ok = false;
        DeviceConfig deviceConfig;

        ChannelConfig channelConfig = rootConfig.getChannel(channelId);

        JsonObject jso = json.getJsonObject();
        JsonElement jsonElement = jso.get(Const.DEVICE);

        if (jsonElement == null) {
            ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_BAD_REQUEST, logger,
                    "Wrong json message syntax. Device statement is missing.");
        }
        String deviceID = jsonElement.getAsString();

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
                    "Channel already exists: ", channelId);
        }
        else {
            try {
                channelConfig = deviceConfig.addChannel(channelId);
                json.setChannelConfig(channelConfig, channelId);

                if ((channelConfig.getValueType() == ValueType.STRING
                        || channelConfig.getValueType() == ValueType.BYTE_ARRAY)
                        && channelConfig.getValueTypeLength() == null) {
                    ServletLib.sendHTTPErrorAndLogErr(response, HttpServletResponse.SC_NOT_ACCEPTABLE, logger,
                            "Channel ", channelId, " with value type ", channelConfig.getValueType().toString(),
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

    private void doGetSpecificChannelField(ToJson json, String chId, String field, HttpServletResponse response)
            throws IOException {
        Channel channel = dataAccess.getChannel(chId);
        if (channel != null) {
            Record record = channel.getLatestRecord();
            switch (field) {
            case Const.TIMESTAMP:
                json.addNumber(Const.TIMESTAMP, record.getTimestamp());
                break;
            case Const.FLAG:
                json.addString(Const.FLAG, record.getFlag().toString());
                break;
            case Const.VALUE_STRING:
                json.addValue(record.getValue(), channel.getValueType());
                break;
            default:
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest channel field is not available, ChannelID = " + chId + " Field: " + field);
                break;
            }
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

    private void doSetRecord(String channelId, HttpServletResponse response, FromJson json) throws ClassCastException {
        Channel channel = dataAccess.getChannel(channelId);
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

    private void doWriteChannel(String channelId, HttpServletResponse response, FromJson json) {
        Channel channel = dataAccess.getChannel(channelId);

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
