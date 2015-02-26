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
package org.openmuc.framework.server.restws;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.data.*;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.server.restws.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JsonHelper {

    private final static Logger logger = LoggerFactory.getLogger(JsonHelper.class);

    private static Gson gson = new Gson();

    public static Gson getGson() {
        return gson;
    }

    public static JsonObject flagToJson(Flag flag) {

        JsonObject jso = new JsonObject();
        jso.addProperty(Const.FLAG, flag.toString());
        return jso;
    }

    public static Value jsonToValue(ValueType type, String jsontext) {

        return getValue(type, gson.fromJson(jsontext, RestValue.class));
    }

    public static String jsonToIdAsString(String jsontext) {

        return gson.fromJson(jsontext, RestId.class).getId();
    }

    public static Record jsonToRecord(ValueType type, String jsontext) {

        return getRecord(gson.fromJson(jsontext, RestRecord.class), type);
    }

    public static JsonObject recordToJson(ValueType type, Record rc) {

        return gson.toJsonTree(getRestRecord(rc, type), RestRecord.class).getAsJsonObject();
    }

    public static JsonArray recordListToJsonArray(ValueType type, List<Record> records) {

        JsonArray jsa = new JsonArray();

        if (records != null) {
            for (Record rc : records) {
                jsa.add((recordToJson(type, rc)));
            }
        }
        return jsa;
    }

    private static JsonObject channelRecordToJson(Channel ch, Record rc) {

        JsonObject jso = new JsonObject();

        jso.addProperty(Const.ID, ch.getId());
        jso.add(Const.RECORDS, recordToJson(ch.getValueType(), rc));
        return jso;
    }

    public static JsonArray channelRecordListToJsonArray(List<Channel> channels) {

        JsonArray jsa = new JsonArray();

        for (Channel ch : channels) {
            jsa.add(channelRecordToJson(ch, ch.getLatestRecord()));
        }
        return jsa;
    }

    public static JsonObject channelRecordListWithRunningFlagToJsonObject(List<Channel> channels, boolean isRunning) {

        JsonObject jso = new JsonObject();

        jso.add(Const.RECORDS, channelRecordListToJsonArray(channels));
        jso.addProperty(Const.RUNNING, isRunning);
        return jso;
    }

    public static JsonObject channelRecordListWithDeviceStateToJsonObject(List<Channel> channels, DeviceState state) {

        JsonObject jso = new JsonObject();

        jso.add(Const.RECORDS, channelRecordListToJsonArray(channels));
        jso.addProperty(Const.STATE, state.name());
        return jso;
    }

    public static JsonObject channelListWithRunningFlagToJsonObject(List<Channel> channelList, boolean isRunning) {

        JsonObject jso = new JsonObject();

        jso.add(Const.CHANNELS, channelListToJsonArray(channelList));
        jso.addProperty(Const.RUNNING, isRunning);
        return jso;
    }

    public static JsonObject deviceListWithRunningFlagToJsonObject(List<String> deviceList, boolean isRunning) {

        JsonObject jso = new JsonObject();

        jso.add(Const.DEVICES, stringListToJsonArray(deviceList));
        jso.addProperty(Const.RUNNING, isRunning);
        return jso;
    }

    public static JsonObject channelListWithDeviceStateToJsonObject(List<Channel> channelList, DeviceState state) {

        JsonObject jso = new JsonObject();

        jso.add(Const.CHANNELS, channelListToJsonArray(channelList));
        jso.addProperty(Const.STATE, state.name());
        return jso;
    }

    public static JsonArray stringListToJsonArray(List<String> devices) {

        return gson.toJsonTree(devices).getAsJsonArray();
    }

    public static JsonObject deviceListToJsonObject(List<String> devices) {

        JsonObject jso = new JsonObject();
        jso.add(Const.DEVICES, stringListToJsonArray(devices));
        return jso;
    }

    public static JsonObject driverListToJsonObject(List<String> drivers) {

        JsonObject jso = new JsonObject();
        jso.add(Const.DRIVERS, stringListToJsonArray(drivers));
        return jso;
    }

    public static JsonArray channelListToJsonArray(List<Channel> channelList) {

        List<String> list = new ArrayList<String>();

        for (Channel channel : channelList) {
            list.add(channel.getId());
        }
        return stringListToJsonArray(list);
    }

    public static JsonObject isRunningToJsonArray(boolean isDisabled) {

        JsonObject jso = new JsonObject();
        jso.addProperty(Const.RUNNING, isDisabled);
        return jso;
    }

    public static ArrayList<RestChannel> channelsJsonToRecords(String jsontext) {
        ArrayList<RestChannel> recList = new ArrayList<RestChannel>();
        JsonArray jsa = gson.fromJson(jsontext, JsonArray.class);
        RestChannel rc = new RestChannel();
        while (jsa.iterator().hasNext()) {
            rc = gson.fromJson(jsa.iterator().next().getAsJsonObject(), RestChannel.class);
            recList.add(rc);
        }
        if (recList.size() == 0) {
            return null;
        }
        return recList;
    }

    public static JsonObject channelFlagsToJson(ArrayList<Channel> channels, ArrayList<Flag> flags) {

        JsonObject jso = new JsonObject();

        int i = 0;
        for (Channel chId : channels) {
            jso.addProperty(chId.getId(), flags.get(i).toString());
            ++i;
        }
        return jso;
    }

    public static JsonObject channelConfigToJsonObject(ChannelConfig config) {

        RestChannelConfig restConfig = RestChannelConfigMapper.getRestChannelConfig(config);
        return (gson.toJsonTree(restConfig, RestChannelConfig.class)).getAsJsonObject();
    }

    public static JsonObject deviceConfigToJsonObject(DeviceConfig config) {

        RestDeviceConfig restConfig = RestDeviceConfigMapper.getRestDeviceConfig(config);
        return (gson.toJsonTree(restConfig, RestDeviceConfig.class)).getAsJsonObject();
    }

    public static JsonObject driverConfigToJsonObject(DriverConfig config) {

        RestDriverConfig restConfig = RestDriverConfigMapper.getRestDriverConfig(config);
        return (gson.toJsonTree(restConfig, RestDriverConfig.class)).getAsJsonObject();
    }

    public static JsonObject configValueToJson(String configField, String value) {

        JsonObject jso = new JsonObject();

        jso.addProperty(configField, value);
        return jso;
    }

    public static String jsonToConfigValue(String jsontext) {

        return getValue(ValueType.STRING, gson.fromJson(jsontext, RestValue.class)).asString();
    }

    private static RestRecord getRestRecord(Record rc, ValueType type) {

        RestRecord rrc = new RestRecord();
        rrc.setFlag(rc.getFlag());
        rrc.setTimestamp(rc.getTimestamp());
        Value value = rc.getValue();

        if (rc.getFlag() != Flag.VALID) {
            rrc.setValue(null);
        } else {
            setRestRecordValue(type, value, rrc);
        }
        return rrc;
    }

    private static void setRestRecordValue(ValueType valueType, Value value, RestRecord rrc) {

        if (value == null) {
            rrc.setValue(null);
        } else {
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

    private static Record getRecord(RestRecord rrc, ValueType type) {

        Object value = rrc.getValue();
        Value retValue = null;
        if (value != null) {
            retValue = getValue(type, value);
        }
        return new Record(retValue, rrc.getTimestamp(), rrc.getFlag());
    }

    private static Value getValue(ValueType type, Object value) {
        // TODO: check all value types, if it is really a float, double, ...

        if (value.getClass().isInstance(new RestValue())) {
            value = ((RestValue) value).getValue();
        }
        Value retValue = null;
        switch (type) {
            case FLOAT:
                FloatValue fvalue = new FloatValue(((Double) value).floatValue());
                retValue = fvalue;
                break;
            case DOUBLE:
                DoubleValue dValue = new DoubleValue((Double) value);
                retValue = dValue;
                break;
            case SHORT:
                ShortValue shValue = new ShortValue(((Double) value).shortValue());
                retValue = shValue;
                break;
            case INTEGER:
                IntValue iValue = new IntValue(((Double) value).intValue());
                retValue = iValue;
                break;
            case LONG:
                LongValue lValue = new LongValue(((Double) value).longValue());
                retValue = lValue;
                break;
            case BYTE:
                ByteValue byValue = new ByteValue(((Double) value).byteValue());
                retValue = byValue;
                break;
            case BOOLEAN:
                BooleanValue boValue = new BooleanValue((Boolean) value);
                retValue = boValue;
                break;
            case BYTE_ARRAY:
                ArrayList<Double> arrayList = ((ArrayList<Double>) value);
                byte[] byteArray = new byte[arrayList.size()];
                for (int i = 0; i < arrayList.size(); ++i) {
                    byteArray[i] = arrayList.get(i).byteValue();
                }
                ByteArrayValue baValue = new ByteArrayValue(byteArray);
                retValue = baValue;
                break;
            case STRING:
                StringValue stValue = new StringValue((String) value);
                retValue = stValue;
                break;
            default:
                break;
        }
        return retValue;
    }

}
