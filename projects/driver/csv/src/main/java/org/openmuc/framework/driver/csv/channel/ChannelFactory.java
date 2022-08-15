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
package org.openmuc.framework.driver.csv.channel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.settings.DeviceSettings;

public class ChannelFactory {

    public static HashMap<String, CsvChannel> createChannelMap(Map<String, List<String>> csvMap,
            DeviceSettings settings) throws ArgumentSyntaxException {

        HashMap<String, CsvChannel> channelMap = new HashMap<>();

        switch (settings.samplingMode()) {
        case UNIXTIMESTAMP:
            channelMap = ChannelFactory.createMapUnixtimestamp(csvMap);
            break;

        case HHMMSS:
            channelMap = ChannelFactory.createMapHHMMSS(csvMap, settings.rewind());
            break;

        case LINE:
            channelMap = ChannelFactory.createMapLine(csvMap, settings.rewind());
            break;

        default:
            break;
        }

        return channelMap;
    }

    public static HashMap<String, CsvChannel> createMapUnixtimestamp(Map<String, List<String>> csvMap)
            throws ArgumentSyntaxException {

        HashMap<String, CsvChannel> channelMap = new HashMap<>();

        String channelAddress;
        Iterator<String> keys = csvMap.keySet().iterator();
        boolean rewind = false;

        while (keys.hasNext()) {
            channelAddress = keys.next();
            List<String> data = csvMap.get(channelAddress);
            long[] timestamps = getTimestamps(csvMap);
            channelMap.put(channelAddress, new CsvChannelUnixtimestamp(data, rewind, timestamps));
        }

        return channelMap;
    }

    public static HashMap<String, CsvChannel> createMapHHMMSS(Map<String, List<String>> csvMap, boolean rewind)
            throws ArgumentSyntaxException {
        HashMap<String, CsvChannel> channelMap = new HashMap<>();

        String channelAddress;
        Iterator<String> keys = csvMap.keySet().iterator();

        while (keys.hasNext()) {
            channelAddress = keys.next();
            List<String> data = csvMap.get(channelAddress);
            long[] timestamps = getHours(csvMap);
            channelMap.put(channelAddress, new CsvChannelHHMMSS(data, rewind, timestamps));
        }

        return channelMap;
    }

    public static HashMap<String, CsvChannel> createMapLine(Map<String, List<String>> csvMap, boolean rewind) {
        HashMap<String, CsvChannel> channelMap = new HashMap<>();
        String channelAddress;
        Iterator<String> keys = csvMap.keySet().iterator();

        while (keys.hasNext()) {
            channelAddress = keys.next();
            List<String> data = csvMap.get(channelAddress);
            channelMap.put(channelAddress, new CsvChannelLine(channelAddress, data, rewind));
        }

        return channelMap;
    }

    /**
     * Convert timestamps from List String to long[]
     * 
     * @throws ArgumentSyntaxException
     */
    private static long[] getTimestamps(Map<String, List<String>> csvMap) throws ArgumentSyntaxException {
        List<String> timestampsList = csvMap.get("unixtimestamp");

        if (timestampsList == null || timestampsList.isEmpty()) {
            throw new ArgumentSyntaxException("unixtimestamp column not availiable in file or empty");
        }

        long[] timestamps = new long[timestampsList.size()];
        for (int i = 0; i < timestampsList.size(); i++) {
            timestamps[i] = Long.parseLong(timestampsList.get(i));
        }
        return timestamps;
    }

    private static long[] getHours(Map<String, List<String>> csvMap) throws ArgumentSyntaxException {
        List<String> hoursList = csvMap.get("hhmmss");

        if (hoursList == null || hoursList.isEmpty()) {
            throw new ArgumentSyntaxException("hhmmss column not availiable in file or empty");
        }

        long[] hours = new long[hoursList.size()];
        for (int i = 0; i < hoursList.size(); i++) {
            hours[i] = Long.parseLong(hoursList.get(i));
        }
        return hours;
    }

}
