package org.openmuc.framework.driver.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.csv.channel.ChannelFactory;
import org.openmuc.framework.driver.csv.channel.CsvChannel;
import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;
import org.openmuc.framework.driver.csv.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds complete data of the CSV file.
 */
public class CsvDeviceConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(CsvDeviceConnection.class);

    /** Map holds all data of the csv file */
    private HashMap<String, CsvChannel> channelMap = new HashMap<>();

    /** Map containing 'column name' as key and 'list of all column data' as value */
    private final Map<String, List<String>> data;

    private final DeviceSettings settings;

    public CsvDeviceConnection(String deviceAddress, String deviceSettings)
            throws ConnectionException, ArgumentSyntaxException {

        settings = new DeviceSettings(deviceSettings);
        data = CsvFileReader.readCsvFile(deviceAddress);
        channelMap = ChannelFactory.createChannelMap(data, settings);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {

        logger.info("Scan for channels called. Settings: " + settings);

        List<ChannelScanInfo> channels = new ArrayList<>();
        String channelId;
        Iterator<String> keys = data.keySet().iterator();

        while (keys.hasNext()) {
            channelId = keys.next();
            final ChannelScanInfo channel = new ChannelScanInfo(channelId, channelId, ValueType.DOUBLE, null);
            channels.add(channel);
        }

        return channels;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        long samplingTime = System.currentTimeMillis();

        for (ChannelRecordContainer container : containers) {
            CsvChannel channel = channelMap.get(container.getChannelAddress());
            if (channel == null) {
                throw new ConnectionException("channel not found");
            }
            else {

                double value = Double.NaN;

                try {

                    value = channel.readValue(samplingTime);
                    container.setRecord(new Record(new DoubleValue(value), samplingTime, Flag.VALID));

                } catch (NoValueReceivedYetException e) {
                    logger.warn("NoValueReceivedYetException", e);
                    container.setRecord(new Record(new DoubleValue(value), samplingTime, Flag.NO_VALUE_RECEIVED_YET));
                } catch (TimeTravelException e) {
                    logger.warn("TimeTravelException", e);
                    container.setRecord(
                            new Record(new DoubleValue(value), samplingTime, Flag.DRIVER_ERROR_READ_FAILURE));
                } catch (CsvException e) {
                    logger.error("CsvException", e);
                    container.setRecord(
                            new Record(new DoubleValue(value), samplingTime, Flag.DRIVER_THREW_UNKNOWN_EXCEPTION));
                }

            }
        }

        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        // nothing to do here, no open file stream since complete file is read during connection.
    }

}
