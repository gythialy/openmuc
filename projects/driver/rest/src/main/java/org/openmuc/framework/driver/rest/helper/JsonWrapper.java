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
package org.openmuc.framework.driver.rest.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.lib.json.Const;
import org.openmuc.framework.lib.json.FromJson;
import org.openmuc.framework.lib.json.ToJson;
import org.openmuc.framework.lib.json.rest.objects.RestChannel;

import com.google.gson.JsonElement;

public class JsonWrapper {

    public String fromRecord(Record remoteRecord, ValueType valueType) {
        ToJson toJson = new ToJson();
        toJson.addRecord(remoteRecord, valueType);

        return toJson.toString();
    }

    public List<ChannelScanInfo> tochannelScanInfos(InputStream stream) throws IOException {
        String jsonString = getStringFromInputStream(stream);
        FromJson fromJson = new FromJson(jsonString);
        List<RestChannel> channelList = fromJson.getRestChannelList();
        ArrayList<ChannelScanInfo> channelScanInfos = new ArrayList<>();

        for (RestChannel restChannel : channelList) {
            // TODO: get channel config list with valueTypeLength, description, ...
            ChannelScanInfo channelScanInfo = new ChannelScanInfo(restChannel.getId(), "", restChannel.getValueType(),
                    0);
            channelScanInfos.add(channelScanInfo);
        }

        return channelScanInfos;
    }

    public Record toRecord(InputStream stream, ValueType valueType) throws IOException {
        String jsonString = getStringFromInputStream(stream);
        FromJson fromJson = new FromJson(jsonString);

        return fromJson.getRecord(valueType);
    }

    public long toTimestamp(InputStream stream) throws IOException {
        String jsonString = getStringFromInputStream(stream);
        FromJson fromJson = new FromJson(jsonString);
        JsonElement timestamp = fromJson.getJsonObject().get(Const.TIMESTAMP);
        if (timestamp == null) {
            return -1;
        }
        return timestamp.getAsNumber().longValue();
    }

    private String getStringFromInputStream(InputStream stream) throws IOException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = streamReader.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }

        return responseStrBuilder.toString();
    }
}
