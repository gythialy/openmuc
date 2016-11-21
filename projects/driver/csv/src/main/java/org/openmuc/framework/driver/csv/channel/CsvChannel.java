package org.openmuc.framework.driver.csv.channel;

import org.openmuc.framework.driver.csv.CsvException;

public interface CsvChannel {

    public double readValue(long sampleTime) throws CsvException;

}
