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

        String channelId;
        Iterator<String> keys = csvMap.keySet().iterator();
        boolean rewind = false;

        while (keys.hasNext()) {
            channelId = keys.next();
            List<String> data = csvMap.get(channelId);
            long[] timestamps = getTimestamps(csvMap);
            channelMap.put(channelId, new CsvChannelUnixtimestamp(data, rewind, timestamps));
        }

        return channelMap;
    }

    public static HashMap<String, CsvChannel> createMapHHMMSS(Map<String, List<String>> csvMap, boolean rewind)
            throws ArgumentSyntaxException {
        HashMap<String, CsvChannel> channelMap = new HashMap<>();

        String channelId;
        Iterator<String> keys = csvMap.keySet().iterator();

        while (keys.hasNext()) {
            channelId = keys.next();
            List<String> data = csvMap.get(channelId);
            long[] timestamps = getHours(csvMap);
            channelMap.put(channelId, new CsvChannelHHMMSS(data, rewind, timestamps));
        }

        return channelMap;
    }

    public static HashMap<String, CsvChannel> createMapLine(Map<String, List<String>> csvMap, boolean rewind) {
        HashMap<String, CsvChannel> channelMap = new HashMap<>();
        String channelId;
        Iterator<String> keys = csvMap.keySet().iterator();

        while (keys.hasNext()) {
            channelId = keys.next();
            List<String> data = csvMap.get(channelId);
            channelMap.put(channelId, new CsvChannelLine(channelId, data, rewind));
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
