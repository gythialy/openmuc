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
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.lib.json.exceptions.MissingJsonObjectException;
import org.openmuc.framework.lib.json.exceptions.RestConfigIsNotCorrectException;
import org.openmuc.framework.lib.json.restObjects.RestChannel;
import org.openmuc.framework.lib.json.restObjects.RestChannelConfig;
import org.openmuc.framework.lib.json.restObjects.RestChannelConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestDeviceConfig;
import org.openmuc.framework.lib.json.restObjects.RestDeviceConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestDriverConfig;
import org.openmuc.framework.lib.json.restObjects.RestDriverConfigMapper;
import org.openmuc.framework.lib.json.restObjects.RestRecord;
import org.openmuc.framework.lib.json.restObjects.RestUserConfig;
import org.openmuc.framework.lib.json.restObjects.RestValue;

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

        Record record = null;
        JsonElement jse = jsonObject.get(Const.RECORD);

        if (!jse.isJsonNull()) {
            record = getRecord(gson.fromJson(jse, RestRecord.class), valueType);
        }
        return record;
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
        if (recordList.size() == 0) {
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

        if (!jse.isJsonNull()) {
            RestChannelConfigMapper.setChannelConfig(channelConfig, gson.fromJson(jse, RestChannelConfig.class), id);
        }
        else {
            throw new MissingJsonObjectException();
        }
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
        if (resultList.size() == 0) {
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

    public ArrayList<RestChannel> getRestChannelArrayList() throws ClassCastException {

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
        if (recordList.size() == 0) {
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

    private Record getRecord(RestRecord rrc, ValueType type) throws ClassCastException {

        Object value = rrc.getValue();
        Value retValue = null;
        if (value != null) {
            retValue = getValue(type, value);
        }
        return new Record(retValue, rrc.getTimestamp(), rrc.getFlag());
    }

    private Value getValue(ValueType type, Object value) throws ClassCastException {
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
            @SuppressWarnings("unchecked")
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
