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
package org.openmuc.framework.server.restws;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.server.restws.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class JsonHelper {

    public static String FlagToJson(Flag flag) {
        JsonObject jso = new JsonObject();

        jso.writeStartObject();
        jso.writeObjectField("flag");
        jso.writeObjectValue(flag.toString());
        jso.writeEndObject();

        return jso.getJsonText();
    }

    public static Value JsonToValue(String jsontext) {
        Value value = new DoubleValue(0.0);

        JsonObjectMap jom = new JsonObjectMap();
        jom = JsonReader.JsonStreamToMap(jsontext);
        if (jom == null) {
            return null;
        }

        ArrayList<JsonFieldInformation> arrayList = new ArrayList<JsonFieldInformation>(jom.keySet());
        ListIterator<JsonFieldInformation> iter = arrayList.listIterator();
        JsonFieldInformation jfi;
        int i = 0;
        while (iter.hasNext()) {
            jfi = iter.next();
            if (jfi.getFieldLevel() != 1) {
                return null;
            }
            if (jfi.getFieldName().equals("value") && jfi.getFieldJsonTextType()
                                                         .equals(JsonTextType.JsonNumber)) {
                value = new DoubleValue(Double.valueOf((jom.get(jfi))));

            }
            ++i;
        }
        if (i != 1) {
            return null;
        } else {
            return value;
        }

    }

    public static Record JsonToRecord(String jsontext) {

        Record rc;
        Value value = new DoubleValue(0.0);

        JsonObjectMap jom = new JsonObjectMap();
        jom = JsonReader.JsonStreamToMap(jsontext);
        if (jom == null) {
            return null;
        }

        ArrayList<JsonFieldInformation> arrayList = new ArrayList<JsonFieldInformation>(jom.keySet());
        ListIterator<JsonFieldInformation> iter = arrayList.listIterator();
        JsonFieldInformation jfi;
        int i = 0;
        while (iter.hasNext()) {
            jfi = iter.next();
            if (jfi.getFieldLevel() != 1) {
                return null;
            }
            if (jfi.getFieldName().equals("value") && jfi.getFieldJsonTextType()
                                                         .equals(JsonTextType.JsonNumber)) {
                value = new DoubleValue(Double.valueOf((jom.get(jfi))));
                ++i;
            } else if (jfi.getFieldName().equals("flag")
                       && ((jfi.getFieldJsonTextType().equals(JsonTextType.JsonString))
                           || (jfi.getFieldJsonTextType()
                                  .equals(JsonTextType.JsonNull)))) {
                if (jom.get(jfi).equals("\"VALID\"") || jom.get(jfi).equals("null")) {
                    ++i;
                } else {
                    return null;
                }
            } else if (jfi.getFieldName().equals("timestamp")
                       && jfi.getFieldJsonTextType().equals(JsonTextType.JsonNumber)) {
                if (!jom.get(jfi).matches("-?\\d+")) {
                    return null;
                }
                ++i;
            }
        }
        if (i != 3) {
            return null;
        } else {
            rc = new Record(value, System.currentTimeMillis());
            return rc;
        }

    }

    public static String RecordToJson(Record rc) {

        JsonObject jso = new JsonObject();

        jso.writeStartObject();
        jso.writeObjectField("value");
        try {
            jso.writeObjectValue(rc.getValue().toString());
            jso.writeObjectField("flag");
            jso.writeObjectValue(rc.getFlag().toString());
            jso.writeObjectField("timestamp");
            jso.writeObjectValue(rc.getTimestamp());
        }
        catch (NullPointerException e) {
            jso.writeObjectValue("-");
            jso.writeObjectField("flag");
            jso.writeObjectValue(rc.getFlag().toString());
            jso.writeObjectField("timestamp");
            jso.writeObjectValue(0);
        }
        jso.writeEndObject();

        return jso.getJsonText();

    }

    public static String RecordListToJsonArray(List<Record> records) {

        JsonArray jsa = new JsonArray();
        JsonObject jso = new JsonObject();

        jsa.writeStartArray();

        for (Record rc : records) {

            jso.setJsonText(RecordToJson(rc));
            jsa.writeArrayValue(jso);
        }

        jsa.writeEndArray();

        return jsa.getJsonText();
    }

    private static JsonObject ChannelRecordToJson(Channel ch, Record rc) {

        JsonObject jst = new JsonObject();

        jst.writeStartObject();
        jst.writeObjectField("id");
        jst.writeObjectValue(ch.getId());
        jst.writeObjectMember("record");
        jst.writeObjectField("value");
        try {
            jst.writeObjectValue(rc.getValue().toString());
            jst.writeObjectField("flag");
            jst.writeObjectValue(rc.getFlag().toString());
            jst.writeObjectField("timestamp");
            jst.writeObjectValue(rc.getTimestamp());
        }
        catch (NullPointerException e) {
            jst.writeObjectValue("0");
            jst.writeObjectField("flag");
            jst.writeObjectValue(rc.getFlag().toString());
            jst.writeObjectField("timestamp");
            jst.writeObjectValue(0);
        }
        jst.writeEndObject();
        jst.writeEndObject();

        return jst;

    }

    public static String ChannelRecordListToJsonArray(List<Channel> channels) {
        JsonArray jst = new JsonArray();
        jst.writeStartArray();
        for (Channel ch : channels) {
            jst.writeArrayValue(ChannelRecordToJson(ch, ch.getLatestRecord()));
        }
        jst.writeEndArray();

        return jst.getJsonText();
    }

    public static String ListToJsonArray(List<String> devices) {

        JsonArray jsa = new JsonArray();

        jsa.writeStartArray();

        for (String dev : devices) {
            jsa.writeArrayValue(dev);
        }

        jsa.writeEndArray();

        return jsa.getJsonText();
    }

    public static ArrayList<Value> ChannelsJsonToValues(String jsontext, List<Channel> dChannels) {
        ArrayList<Value> values = new ArrayList<Value>();

        JsonObjectMap jom = new JsonObjectMap();
        jom = JsonReader.JsonStreamToMap(jsontext);
        if (jom == null) {
            return null;
        }

        JsonFieldInformation jfi;
        for (Channel drCh : dChannels) {
            ArrayList<JsonFieldInformation> arrayList = new ArrayList<JsonFieldInformation>(jom.keySet());
            ListIterator<JsonFieldInformation> iter = arrayList.listIterator();
            int i = 0;
            int size = 0;
            while (iter.hasNext()) {
                jfi = iter.next();
                if (jfi.getFieldLevel() != 1) {
                    return null;
                }
                if (jfi.getFieldName().equals(drCh.getId())
                    && jfi.getFieldJsonTextType().equals(JsonTextType.JsonNumber)) {
                    values.add(new DoubleValue(Double.valueOf((jom.get(jfi)))));
                    ++i;
                }
                size++;

            }
            if (i != 1 || size != dChannels.size()) {
                return null;
            }
        }
        return values;
    }

    public static String ChannelFlagsToJson(ArrayList<Channel> channels, ArrayList<Flag> flags) {
        JsonObject jso = new JsonObject();
        jso.writeStartObject();
        int i = 0;
        for (Channel chId : channels) {
            jso.writeObjectField(chId.getId());
            jso.writeObjectValue(flags.get(i).toString());
            ++i;
        }

        jso.writeEndObject();

        return jso.getJsonText();
    }

    public static String ConfigValueToJson(String configField, String value) {
        JsonObject jso = new JsonObject();

        jso.writeStartObject();
        jso.writeObjectField(configField);

        jso.writeObjectValue(value);
        jso.writeEndObject();

        return jso.getJsonText();

    }

    public static String JsonToConfigValue(String jsontext) {
        String configValue = "";

        JsonObjectMap jom = new JsonObjectMap();
        jom = JsonReader.JsonStreamToMap(jsontext);
        if (jom == null) {
            return null;
        }

        ArrayList<JsonFieldInformation> arrayList = new ArrayList<JsonFieldInformation>(jom.keySet());
        ListIterator<JsonFieldInformation> iter = arrayList.listIterator();
        JsonFieldInformation jfi;
        int i = 0;
        while (iter.hasNext()) {
            jfi = iter.next();
            if (jfi.getFieldLevel() != 1) {
                return null;
            }
            if (jfi.getFieldName().equals("value")
                && (jfi.getFieldJsonTextType().equals(JsonTextType.JsonNumber)
                    || jfi.getFieldJsonTextType()
                          .equals(JsonTextType.JsonString))) {
                configValue = jom.get(jfi);

            }
            ++i;
        }
        if (i != 1) {
            return null;
        } else {
            return configValue;
        }
    }
}
