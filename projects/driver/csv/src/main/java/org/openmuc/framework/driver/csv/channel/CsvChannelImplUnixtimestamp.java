package org.openmuc.framework.driver.csv.channel;

import java.util.List;

import org.openmuc.framework.driver.csv.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvChannelImplUnixtimestamp extends CsvTimeChannel {

    private final static Logger LOGGER = LoggerFactory.getLogger(CsvChannelImplUnixtimestamp.class);

    public CsvChannelImplUnixtimestamp(List<String> data, boolean rewind, long[] timestamps) {
        super(data, rewind, timestamps);
    }

    @Override
    public double readValue(long samplingTime) throws CsvException {
        lastReadIndex = searchNextIndex(samplingTime);
        double value = Double.parseDouble(data.get(lastReadIndex));
        return value;
    }

}
