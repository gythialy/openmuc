/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.lib.rest1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.lib.rest1.exceptions.MissingJsonObjectException;
import org.openmuc.framework.lib.rest1.exceptions.RestConfigIsNotCorrectException;
import org.openmuc.framework.lib.rest1.rest.objects.RestChannel;
import org.openmuc.framework.lib.rest1.rest.objects.RestChannelConfig;
import org.openmuc.framework.lib.rest1.rest.objects.RestChannelConfigMapper;
import org.openmuc.framework.lib.rest1.rest.objects.RestDeviceConfig;
import org.openmuc.framework.lib.rest1.rest.objects.RestDeviceConfigMapper;
import org.openmuc.framework.lib.rest1.rest.objects.RestDriverConfig;
import org.openmuc.framework.lib.rest1.rest.objects.RestDriverConfigMapper;
import org.openmuc.framework.lib.rest1.rest.objects.RestRecord;
import org.openmuc.framework.lib.rest1.rest.objects.RestUserConfig;
import org.openmuc.framework.lib.rest1.rest.objects.RestValue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class FromJson {

    private final Gson gson;
    private final JsonObject jsonObject;

    public FromJson(String jsonString) {

        GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
        gson = gsonBuilder.create();
        jsonObject = gson.fromJson(jsonString, JsonObject.class);
    }

    public Gson getGson() {

        return gson;
    }

    public JsonObject getJsonObject() {

        return jsonObject;
    }

    public Record getRecord(ValueType valueType) throws ClassCastException {
        JsonElement jse = jsonObject.get(Const.RECORD);
        if (jse.isJsonNull()) {
            return null;
        }

        return convertRestRecordToRecord(gson.fromJson(jse, RestRecord.class), valueType);
    }

    public ArrayList<Record> getRecordArrayList(ValueType valueType) throws ClassCastException {

        ArrayList<Record> recordList = new ArrayList<>();

        JsonElement jse = jsonObject.get(Const.RECORDS);
        if (jse != null && jse.isJsonArray()) {
            JsonArray jsa = jse.getAsJsonArray();

            Iterator<JsonElement> iteratorJsonArray = jsa.iterator();
            while (iteratorJsonArray.hasNext()) {
                recordList.add(getRecord(valueType));
            }
        }
        if (recordList.isEmpty()) {
            recordList = null;
        }
        return recordList;
    }

    public Value getValue(ValueType valueType) throws ClassCastException {

        Value value = null;
        JsonElement jse = jsonObject.get(Const.RECORD);

        if (!jse.isJsonNull()) {
            Record record = getRecord(valueType);
            if (record != null) {
                value = record.getValue();
            }
        }
        return value;
    }

    public boolean isRunning() {

        return jsonObject.get(Const.RUNNING).getAsBoolean();
    }

    public DeviceState getDeviceState() {

        DeviceState ret = null;
        JsonElement jse = jsonObject.get(Const.STATE);

        if (!jse.isJsonNull()) {
            ret = gson.fromJson(jse, DeviceState.class);
        }
        return ret;
    }

    public void setChannelConfig(ChannelConfig channelConfig, String id) throws JsonSyntaxException,
            IdCollisionException, RestConfigIsNotCorrectException, MissingJsonObjectException {
        JsonElement jse = jsonObject.get(Const.CONFIGS);

        if (jse.isJsonNull()) {
            throw new MissingJsonObjectException();
        }

        RestChannelConfigMapper.setChannelConfig(channelConfig, gson.fromJson(jse, RestChannelConfig.class), id);
    }

    public void setDeviceConfig(DeviceConfig deviceConfig, String id) throws JsonSyntaxException, IdCollisionException,
            RestConfigIsNotCorrectException, MissingJsonObjectException {

        JsonElement jse = jsonObject.get(Const.CONFIGS);

        if (!jse.isJsonNull()) {
            RestDeviceConfigMapper.setDeviceConfig(deviceConfig, gson.fromJson(jse, RestDeviceConfig.class), id);
        }
        else {
            throw new MissingJsonObjectException();
        }
    }

    public void setDriverConfig(DriverConfig driverConfig, String id) throws JsonSyntaxException, IdCollisionException,
            RestConfigIsNotCorrectException, MissingJsonObjectException {

        JsonElement jse = jsonObject.get(Const.CONFIGS);

        if (!jse.isJsonNull()) {
            RestDriverConfigMapper.setDriverConfig(driverConfig, gson.fromJson(jse, RestDriverConfig.class), id);
        }
        else {
            throw new MissingJsonObjectException();
        }
    }

    public ArrayList<String> getStringArrayList(String listName) {

        ArrayList<String> resultList = new ArrayList<>();

        JsonElement jse = jsonObject.get(listName);
        if (jse != null && jse.isJsonArray()) {
            JsonArray jsa = jse.getAsJsonArray();

            Iterator<JsonElement> iteratorJsonArray = jsa.iterator();
            while (iteratorJsonArray.hasNext()) {
                resultList.add(iteratorJsonArray.next().toString());
            }
        }
        if (resultList.isEmpty()) {
            resultList = null;
        }
        return resultList;
    }

    public String[] getStringArray(String listName) {

        String stringArray[] = null;

        JsonElement jse = jsonObject.get(listName);
        if (!jse.isJsonNull() && jse.isJsonArray()) {
            stringArray = gson.fromJson(jse, String[].class);
        }
        return stringArray;
    }

    public List<RestChannel> getRestChannelList() {

        ArrayList<RestChannel> recordList = new ArrayList<>();
        JsonElement jse = jsonObject.get("records");
        JsonArray jsa;

        if (!jse.isJsonNull() && jse.isJsonArray()) {

            jsa = jse.getAsJsonArray();
            Iterator<JsonElement> jseIterator = jsa.iterator();

            while (jseIterator.hasNext()) {
                JsonObject jsoIterated = jseIterator.next().getAsJsonObject();
                RestChannel rc = gson.fromJson(jsoIterated, RestChannel.class);
                recordList.add(rc);
            }
        }
        if (recordList.isEmpty()) {
            return null;
        }
        return recordList;
    }

    public RestUserConfig getRestUserConfig() {

        JsonObject jso = jsonObject.get(Const.CONFIGS).getAsJsonObject();
        return gson.fromJson(jso, RestUserConfig.class);
    }

    public List<DeviceScanInfo> getDeviceScanInfoList() {

        List<DeviceScanInfo> returnValue = new ArrayList<>();
        JsonElement jse = jsonObject.get(Const.CHANNELS); // TODO: another name?
        JsonArray jsa;

        if (jse.isJsonArray()) {
            jsa = jse.getAsJsonArray();
            Iterator<JsonElement> jseIterator = jsa.iterator();

            while (jseIterator.hasNext()) {
                JsonObject jso = jseIterator.next().getAsJsonObject();
                String id = getString(jso.get(Const.ID));
                String deviceAddress = getString(jso.get(Const.DEVICEADDRESS));
                String settings = getString(jso.get(Const.SETTINGS));
                String description = getString(jso.get(Const.DESCRIPTION));
                returnValue.add(new DeviceScanInfo(id, deviceAddress, settings, description));
            }
        }
        else {
            returnValue = null;
        }
        return returnValue;
    }

    public List<ChannelScanInfo> getChannelScanInfoList() {

        List<ChannelScanInfo> returnValue = new ArrayList<>();
        JsonElement jse = jsonObject.get(Const.CHANNELS); // TODO: another name?
        JsonArray jsa;

        if (jse.isJsonArray()) {
            jsa = jse.getAsJsonArray();
            Iterator<JsonElement> jseIterator = jsa.iterator();

            while (jseIterator.hasNext()) {
                JsonObject jso = jseIterator.next().getAsJsonObject();
                String channelAddress = getString(jso.get(Const.CHANNELADDRESS));
                ValueType valueType = ValueType.valueOf(getString(jso.get(Const.VALUETYPE)));
                int valueTypeLength = getInt(jso.get(Const.VALUETYPELENGTH));
                String description = getString(jso.get(Const.DESCRIPTION));
                boolean readable = getBoolean(jso.get(Const.READABLE));
                boolean writeable = getBoolean(jso.get(Const.WRITEABLE));
                String metadata = getString(jso.get(Const.METADATA));

                returnValue.add(new ChannelScanInfo(channelAddress, description, valueType, valueTypeLength, readable,
                        writeable, metadata));
            }
        }
        else {
            returnValue = null;
        }
        return returnValue;
    }

    private String getString(JsonElement jse) {
        if (jse != null) {
            return jse.getAsString();
        }
        else {
            return "";
        }
    }

    private int getInt(JsonElement jse) {
        if (jse != null) {
            return jse.getAsInt();
        }
        else {
            return 0;
        }
    }

    private boolean getBoolean(JsonElement jse) {
        if (jse != null) {
            return jse.getAsBoolean();
        }
        else {
            return true;
        }
    }

    private Record convertRestRecordToRecord(RestRecord rrc, ValueType type) throws ClassCastException {
        Object value = rrc.getValue();
        Flag flag = rrc.getFlag();
        Value retValue = null;

        if (value != null) {
            retValue = convertValueToMucValue(type, value);
        }
        if (flag == null) {
            return new Record(retValue, rrc.getTimestamp());
        }
        else {
            return new Record(retValue, rrc.getTimestamp(), rrc.getFlag());
        }
    }

    private Value convertValueToMucValue(ValueType type, Object value) throws ClassCastException {
        // TODO: check all value types, if it is really a float, double, ...

        if (value.getClass().isInstance(new RestValue())) {
            value = ((RestValue) value).getValue();
        }

        switch (type) {
        case FLOAT:
            return new FloatValue(((Double) value).floatValue());
        case DOUBLE:
            return new DoubleValue((Double) value);
        case SHORT:
            return new ShortValue(((Double) value).shortValue());
        case INTEGER:
            return new IntValue(((Double) value).intValue());
        case LONG:
            return new LongValue(((Double) value).longValue());
        case BYTE:
            return new ByteValue(((Double) value).byteValue());
        case BOOLEAN:
            return new BooleanValue((Boolean) value);
        case BYTE_ARRAY:
            @SuppressWarnings("unchecked")
            List<Double> arrayList = ((ArrayList<Double>) value);
            byte[] byteArray = new byte[arrayList.size()];
            for (int i = 0; i < arrayList.size(); ++i) {
                byteArray[i] = arrayList.get(i).byteValue();
            }
            return new ByteArrayValue(byteArray);
        case STRING:
            return new StringValue((String) value);
        default:
            // should not occur
            return new StringValue(value.toString());
        }
    }

}
