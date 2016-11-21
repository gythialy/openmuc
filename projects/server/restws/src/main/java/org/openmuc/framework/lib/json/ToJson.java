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
package org.openmuc.framework.lib.json;

import java.util.List;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.lib.json.restObjects.RestChannelConfig;
import org.openmuc.framework.lib.json.restObjects.RestChannelConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestDeviceConfig;
import org.openmuc.framework.lib.json.restObjects.RestDeviceConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestDriverConfig;
import org.openmuc.framework.lib.json.restObjects.RestDriverConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestRecord;
import org.openmuc.framework.lib.json.restObjects.RestScanProgressInfo;
import org.openmuc.framework.lib.json.restObjects.RestUserConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ToJson {

    private final Gson gson;
    private final JsonObject jsonObject;

    public ToJson() {

        GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
        gson = gsonBuilder.create();
        jsonObject = new JsonObject();
    }

    public JsonObject getJsonObject() {

        return jsonObject;
    }

    public void addJsonObject(String propertyName, JsonObject jsonObject) {

        this.jsonObject.add(propertyName, jsonObject);
    }

    @Override
    public String toString() {
        return gson.toJson(jsonObject);
    }

    public void addRecord(Record record, ValueType valueType) throws ClassCastException {

        jsonObject.add(Const.RECORD, getRecordAsJsonElement(record, valueType));
    }

    public void addRecordList(List<Record> recordList, ValueType valueType) throws ClassCastException {

        JsonArray jsa = new JsonArray();

        if (recordList != null) {
            for (Record record : recordList) {
                jsa.add(getRecordAsJsonElement(record, valueType));
            }
        }
        jsonObject.add(Const.RECORDS, jsa);
    }

    public void addChannelRecordList(List<Channel> channels) throws ClassCastException {

        JsonArray jsa = new JsonArray();

        for (Channel channel : channels) {
            jsa.add(channelRecordToJson(channel));
        }
        jsonObject.add(Const.RECORDS, jsa);
    }

    public void addDeviceState(DeviceState deviceState) {

        jsonObject.addProperty(Const.STATE, deviceState.name());
    }

    public void addNumber(String propertyName, Number value) {

        jsonObject.addProperty(propertyName, value);
    }

    public void addBoolean(String propertyName, boolean value) {

        jsonObject.addProperty(propertyName, value);
    }

    public void addString(String propertyName, String value) {

        jsonObject.addProperty(propertyName, value);
    }

    public void addStringList(String propertyName, List<String> stringList) {

        jsonObject.add(propertyName, gson.toJsonTree(stringList).getAsJsonArray());
    }

    public void addDriverList(List<DriverConfig> driverConfigList) {

        JsonArray jsa = new JsonArray();

        for (DriverConfig driverConfig : driverConfigList) {
            jsa.add(gson.toJsonTree(driverConfig.getId()));
        }
        jsonObject.add(Const.DRIVERS, jsa);
    }

    public void addDeviceList(List<DeviceConfig> deviceConfigList) {

        JsonArray jsa = new JsonArray();

        for (DeviceConfig deviceConfig : deviceConfigList) {
            jsa.add(gson.toJsonTree(deviceConfig.getId()));
        }
        jsonObject.add(Const.DEVICES, jsa);
    }

    public void addChannelList(List<Channel> channelList) {

        JsonArray jsa = new JsonArray();

        for (Channel channelConfig : channelList) {
            jsa.add(gson.toJsonTree(channelConfig.getId()));
        }
        jsonObject.add(Const.CHANNELS, jsa);
    }

    public void addDriverInfo(DriverInfo driverInfo) {

        jsonObject.add(Const.INFOS, gson.toJsonTree(driverInfo));
    }

    public void addDriverConfig(DriverConfig config) {

        RestDriverConfig restConfig = RestDriverConfigMapper.getRestDriverConfig(config);
        jsonObject.add(Const.CONFIGS, gson.toJsonTree(restConfig, RestDriverConfig.class).getAsJsonObject());
    }

    public void addDeviceConfig(DeviceConfig config) {

        RestDeviceConfig restConfig = RestDeviceConfigMapper.getRestDeviceConfig(config);
        jsonObject.add(Const.CONFIGS, gson.toJsonTree(restConfig, RestDeviceConfig.class).getAsJsonObject());
    }

    public void addChannelConfig(ChannelConfig config) {

        RestChannelConfig restConfig = RestChannelConfigMapper.getRestChannelConfig(config);
        jsonObject.add(Const.CONFIGS, gson.toJsonTree(restConfig, RestChannelConfig.class).getAsJsonObject());
    }

    public void addDeviceScanProgressInfo(RestScanProgressInfo restScanProgressInfo) {

        jsonObject.add(Const.SCAN_PROGRESS_INFO, gson.toJsonTree(restScanProgressInfo));
    }

    public void addDeviceScanInfoList(List<DeviceScanInfo> deviceScanInfoList) {

        JsonArray jsa = new JsonArray();
        for (DeviceScanInfo deviceScanInfo : deviceScanInfoList) {
            JsonObject jso = new JsonObject();
            jso.addProperty(Const.ID, deviceScanInfo.getId());
            jso.addProperty(Const.DEVICEADDRESS, deviceScanInfo.getDeviceAddress());
            jso.addProperty(Const.SETTINGS, deviceScanInfo.getSettings());
            jso.addProperty(Const.DESCRIPTION, deviceScanInfo.getDescription());
            jsa.add(jso);
        }
        jsonObject.add(Const.DEVICES, jsa);
    }

    public void addChannelScanInfoList(List<ChannelScanInfo> channelScanInfoList) {

        JsonArray jsa = new JsonArray();
        for (ChannelScanInfo channelScanInfo : channelScanInfoList) {
            JsonObject jso = new JsonObject();
            jso.addProperty(Const.CHANNELADDRESS, channelScanInfo.getChannelAddress());
            jso.addProperty(Const.VALUETYPE, channelScanInfo.getValueType().name());
            jso.addProperty(Const.VALUETYPELENGTH, channelScanInfo.getValueTypeLength());
            jso.addProperty(Const.DESCRIPTION, channelScanInfo.getDescription());
            jso.addProperty(Const.METADATA, channelScanInfo.getMetaData());
            jsa.add(jso);
        }
        jsonObject.add(Const.CHANNELS, jsa);
    }

    public void addRestUserConfig(RestUserConfig restUserConfig) {

        jsonObject.add(Const.CONFIGS, gson.toJsonTree(restUserConfig, RestUserConfig.class).getAsJsonObject());
    }

    public static JsonObject getDriverConfigAsJsonObject(DriverConfig config) {

        RestDriverConfig restConfig = RestDriverConfigMapper.getRestDriverConfig(config);
        Gson gson = new Gson();
        return gson.toJsonTree(restConfig, RestDriverConfig.class).getAsJsonObject();
    }

    public static JsonObject getDeviceConfigAsJsonObject(DeviceConfig config) {

        RestDeviceConfig restConfig = RestDeviceConfigMapper.getRestDeviceConfig(config);
        Gson gson = new Gson();
        return gson.toJsonTree(restConfig, RestDeviceConfig.class).getAsJsonObject();
    }

    public static JsonObject getChannelConfigAsJsonObject(ChannelConfig config) {

        RestChannelConfig restConfig = RestChannelConfigMapper.getRestChannelConfig(config);
        Gson gson = new Gson();
        return gson.toJsonTree(restConfig, RestChannelConfig.class).getAsJsonObject();
    }

    private JsonObject channelRecordToJson(Channel channel) throws ClassCastException {

        JsonObject jso = new JsonObject();

        jso.addProperty(Const.ID, channel.getId());
        jso.addProperty(Const.VALUETYPE, channel.getValueType().toString());
        jso.add(Const.RECORD, getRecordAsJsonElement(channel.getLatestRecord(), channel.getValueType()));
        return jso;
    }

    private JsonElement getRecordAsJsonElement(Record record, ValueType valueType) throws ClassCastException {

        return gson.toJsonTree(getRestRecord(record, valueType), RestRecord.class);
    }

    private RestRecord getRestRecord(Record rc, ValueType valueType) throws ClassCastException {

        Value value = rc.getValue();
        Flag flag = rc.getFlag();
        RestRecord rrc = new RestRecord();

        rrc.setTimestamp(rc.getTimestamp());

        flag = handleInfinityAndNaNValue(value, valueType, flag);
        if (flag != Flag.VALID) {
            rrc.setFlag(flag);
            rrc.setValue(null);
            return rrc;
        }

        rrc.setFlag(flag);
        setRestRecordValue(valueType, value, rrc);

        return rrc;
    }

    private void setRestRecordValue(ValueType valueType, Value value, RestRecord rrc) throws ClassCastException {

        if (value == null) {
            rrc.setValue(null);
        }
        else {
            switch (valueType) {
            case FLOAT:
                rrc.setValue(value.asFloat());
                break;
            case DOUBLE:
                rrc.setValue(value.asDouble());
                break;
            case SHORT:
                rrc.setValue(value.asShort());
                break;
            case INTEGER:
                rrc.setValue(value.asInt());
                break;
            case LONG:
                rrc.setValue(value.asLong());
                break;
            case BYTE:
                rrc.setValue(value.asByte());
                break;
            case BOOLEAN:
                rrc.setValue(value.asBoolean());
                break;
            case BYTE_ARRAY:
                rrc.setValue(value.asByteArray());
                break;
            case STRING:
                rrc.setValue(value.asString());
                break;
            default:
                rrc.setValue(null);
                break;
            }
        }
    }

    private Flag handleInfinityAndNaNValue(Value value, ValueType valueType, Flag flag) {

        if (value != null) {
            switch (valueType) {
            case DOUBLE:
                if (Double.isInfinite(value.asDouble())) {
                    return Flag.VALUE_IS_INFINITY;
                }
                else if (Double.isNaN(value.asDouble())) {
                    return Flag.VALUE_IS_NAN;
                }
                break;
            case FLOAT:
                if (Float.isInfinite(value.asFloat())) {
                    return Flag.VALUE_IS_INFINITY;
                }
                else if (Float.isNaN(value.asFloat())) {
                    return Flag.VALUE_IS_NAN;
                }
                break;
            default:
                // is not a floating point number
                return flag;
            }
        }
        return flag;
    }

}
