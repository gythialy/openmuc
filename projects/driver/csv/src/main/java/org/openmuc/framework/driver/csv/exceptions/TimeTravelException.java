package org.openmuc.framework.driver.csv.exceptions;

import org.openmuc.framework.driver.csv.CsvException;

/**
 * Exception for illogical time jumps. <br>
 * <br>
 * Scenario 1: the csv file contains ONLY values within a time period form 100000 o'clock till 110000 o'clock. The
 * driver has successfully sampled a value within this period. Now the time has jumped and the next sampling time is
 * 090000 o'clock (so before the first entry of the csv file). The driver therefore can't find a value for this sampling
 * time.
 * 
 */
public class TimeTravelException extends CsvException {

    private static final long serialVersionUID = 6718058510080266888L;

    public TimeTravelException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
