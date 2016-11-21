package org.openmuc.framework.driver.csv;

import java.io.IOException;
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

public class CsvDeviceConnection implements Connection {

    private final static Logger logger = LoggerFactory.getLogger(CsvDeviceConnection.class);
    private static final String COMMENT = "#";
    private HashMap<String, CsvChannel> channelMap = new HashMap<String, CsvChannel>();

    /** Key = column name, Value = List of all values */
    private Map<String, List<String>> data;
    private DeviceSettings settings;

    public CsvDeviceConnection(String deviceAddress, String deviceSettings)
            throws ConnectionException, ArgumentSyntaxException {

        logger.debug("#### deviceAddress: " + deviceAddress);
        settings = new DeviceSettings(deviceSettings);

        try {
            data = CsvFileReader.readCsvFile(deviceAddress);
            channelMap = ChannelFactory.createChannelMap(data, settings);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }

    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {

        logger.debug("#### scan for channels called. settings: " + settings);

        List<ChannelScanInfo> channels = new ArrayList<ChannelScanInfo>();
        String channelId;
        Iterator<String> keys = data.keySet().iterator();

        while (keys.hasNext()) {
            channelId = (String) keys.next();
            ChannelScanInfo channel = new ChannelScanInfo(channelId, channelId, ValueType.DOUBLE, null);
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

                // TODO nicht für jeden channel prüfen (gilt für device

                double value = Double.NaN;

                try {
                    if (settings.samplingMode().equals(ESampleMode.HHMMSS)) {
                        value = channel.readValue(samplingTime);
                    }
                    else if (settings.samplingMode().equals(ESampleMode.UNIXTIMESTAMP)) {
                        value = channel.readValue(samplingTime);
                    }
                    else if (settings.samplingMode().equals(ESampleMode.LINE)) {
                        value = channel.readValue(samplingTime);
                    }
                    else {
                        throw new ConnectionException(
                                "SamplingMode: '" + settings.samplingMode() + "' not supported yet!");
                    }

                    container.setRecord(new Record(new DoubleValue(value), samplingTime, Flag.VALID));

                } catch (NoValueReceivedYetException e) {
                    logger.warn("NoValueReceivedYetException", e);
                    container.setRecord(new Record(new DoubleValue(value), samplingTime, Flag.NO_VALUE_RECEIVED_YET));
                } catch (TimeTravelException e) {
                    logger.warn("TimeTravelException", e);
                    container.setRecord(
                            new Record(new DoubleValue(value), samplingTime, Flag.DRIVER_ERROR_READ_FAILURE));
                } catch (CsvException e) {
                    logger.error("TimeTravelException", e);
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
        // TODO Auto-generated method stub

    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect() {

    }

}
