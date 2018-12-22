package org.openmuc.framework.driver.csv.channel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel to return value of next line in the file. Timestamps are ignored. It always starts with the first line, which
 * can be useful for simulation since every time the framework is started it starts with the same values.
 */
public class CsvChannelLine implements CsvChannel {

    private static final Logger logger = LoggerFactory.getLogger(CsvChannelLine.class);

    private int lastReadIndex = 0;
    private final int maxIndex;
    private final List<String> data;
    private boolean rewind = false;

    public CsvChannelLine(String id, List<String> data, boolean rewind) {
        this.data = data;
        this.maxIndex = data.size() - 1;
        this.rewind = rewind;
    }

    @Override
    public double readValue(long sampleTime) {

        lastReadIndex++;
        if (lastReadIndex > maxIndex) {
            if (rewind) {
                lastReadIndex = 0;
            }
            else {
                // once maximum is reached it always returns the maximum (value of last line in file)
                lastReadIndex = maxIndex;
            }
        }

        double value = Double.parseDouble(data.get(lastReadIndex));
        return value;
    }

}
