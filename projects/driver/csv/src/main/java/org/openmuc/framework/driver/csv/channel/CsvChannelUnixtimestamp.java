package org.openmuc.framework.driver.csv.channel;

import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CsvChannelUnixtimestamp extends CsvTimeChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvChannelUnixtimestamp.class);

    public CsvChannelUnixtimestamp(List<String> data, boolean rewind, long[] timestamps) {
        super(data, rewind, timestamps);
    }

    @Override
    public double readValue(long samplingTime) throws CsvException {
        lastReadIndex = searchNextIndex(samplingTime);
        double value = Double.parseDouble(data.get(lastReadIndex));
        return value;
    }

}
