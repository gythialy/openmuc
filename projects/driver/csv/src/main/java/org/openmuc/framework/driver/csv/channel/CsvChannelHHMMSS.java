package org.openmuc.framework.driver.csv.channel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.openmuc.framework.driver.csv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvChannelHHMMSS extends CsvTimeChannel {

    private static final Logger logger = LoggerFactory.getLogger(CsvChannelHHMMSS.class);

    public CsvChannelHHMMSS(List<String> data, boolean rewind, long[] timestamps) {
        super(data, rewind, timestamps);
    }

    @Override
    public double readValue(long samplingTime) throws CsvException {
        int hhmmss = convertTimestamp(samplingTime);
        lastReadIndex = searchNextIndex(hhmmss);
        double value = Double.parseDouble(data.get(lastReadIndex));
        return value;
    }

    private int convertTimestamp(long samplingTime) {
        // TODO add local
        GregorianCalendar cal = new GregorianCalendar(Locale.GERMANY);
        cal.setTime(new Date(samplingTime));

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        // convert sampling time (unixtimestamp) to sampling time (hhmmss)
        // 14:25:34
        // 140000 + 2500 + 34 = 142534
        int hhmmss = hour * 10000 + minute * 100 + second;
        return hhmmss;
    }

}
