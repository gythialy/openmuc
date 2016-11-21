package org.openmuc.framework.driver.csv.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.driver.csv.CsvException;
import org.openmuc.framework.driver.csv.channel.CsvChannelImplHour;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvTimeChannelHourTest {

    private final static Logger logger = LoggerFactory.getLogger(CsvTimeChannelHourTest.class);

    static List<String> data;
    static long[] timestamps;
    static double value;

    @BeforeClass
    public static void initTestClass() {
        data = new ArrayList<>();
        data.add("0.0");
        data.add("5.0");
        data.add("10.0");
        data.add("15.0");
        data.add("20.0");

        timestamps = new long[] { 100000, 100005, 100010, 100015, 100020 };

    }

    // openmuc calls read with timestamp (ms)
    private long createTimestamp(long hhmmss) {
        GregorianCalendar cal = new GregorianCalendar(Locale.GERMANY);

        cal.setTimeInMillis(System.currentTimeMillis());

        // add leading zeros e.g. 90000 (9 o'clock) will be converted to 090000
        String time = String.format("%06d", hhmmss);

        String hourStr = time.substring(0, 2);
        String minuteStr = time.substring(2, 4);
        String secondStr = time.substring(4, 6);

        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourStr));
        cal.set(Calendar.MINUTE, Integer.parseInt(minuteStr));
        cal.set(Calendar.SECOND, Integer.parseInt(secondStr));
        cal.set(Calendar.MILLISECOND, 0);

        long ms = cal.getTimeInMillis();

        return ms;

    }

    @Test
    public void testReadNextValueInbetween() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, false, timestamps);

        value = channel.readValue(createTimestamp(100006));
        Assert.assertTrue(String.valueOf(value).equals("5.0"));

        value = channel.readValue(createTimestamp(100014));
        Assert.assertTrue(String.valueOf(value).equals("10.0"));
    }

    @Test
    public void testReadNextValueStart() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, false, timestamps);

        value = channel.readValue(createTimestamp(100000));
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        value = channel.readValue(createTimestamp(100005));
        Assert.assertTrue(String.valueOf(value).equals("5.0"));
    }

    @Test
    public void testReadNextValueEndNoRewind() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, false, timestamps);

        value = channel.readValue(createTimestamp(100020));
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(createTimestamp(100025));
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        // timestamp before the last one, but rewind is false so timestamp is not considered and old value is returned
        try {
            value = channel.readValue(createTimestamp(100000));
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

    @Test
    public void testReadNextValueEndWithRewind() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, true, timestamps);

        value = channel.readValue(createTimestamp(100020));
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(createTimestamp(100025));
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(createTimestamp(100000));
        Assert.assertTrue(String.valueOf(value).equals("0.0"));
    }

    @Test
    public void testReadT1BeforeT2Valid() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, false, timestamps);

        try {
            value = channel.readValue(createTimestamp(90000));
            Assert.assertTrue(false);
        } catch (NoValueReceivedYetException e) {
            Assert.assertTrue(true);
        }

        value = channel.readValue(createTimestamp(100000));
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

    }

    @Test
    public void testReadT1ValidT2BeforeDisabledRewind() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, false, timestamps);

        value = channel.readValue(createTimestamp(100000));
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(createTimestamp(90000));
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testReadT1ValidT2BeforeEnabledRewind() throws CsvException {

        CsvChannelImplHour channel = new CsvChannelImplHour(data, true, timestamps);

        value = channel.readValue(createTimestamp(100000));
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(createTimestamp(90000));
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

}
