package org.openmuc.framework.driver.csv.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.driver.csv.CsvException;
import org.openmuc.framework.driver.csv.channel.CsvChannelImplUnixtimestamp;
import org.openmuc.framework.driver.csv.exceptions.NoValueReceivedYetException;
import org.openmuc.framework.driver.csv.exceptions.TimeTravelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvTimeChannelUnixtimestampTest {

    private final static Logger logger = LoggerFactory.getLogger(CsvTimeChannelUnixtimestampTest.class);

    static List<String> data;
    static long[] timestamps;
    static double value;
    private static final long OFFSET = 1436306400000l;

    @BeforeClass
    public static void initTestClass() {
        data = new ArrayList<>();
        data.add("0.0");
        data.add("5.0");
        data.add("10.0");
        data.add("15.0");
        data.add("20.0");

        // TODO map hhmmss und timestamp
        // TODO ein gleiche parameter für CsvTimeChannelHourTest und CsvTimeChannelUnixtimestampTest
        timestamps = new long[] { 1436306400000l /* 20150708 000000 */, 1436306405000l /* 20150708 000005 */,
                1436306410000l /* 20150708 000010 */, 1436306415000l /* 20150708 000015 */,
                1436306420000l /* 20150708 000020 */ };

    }

    @Test
    public void testReadNextValueInbetween() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET + 6000l);
        Assert.assertTrue(String.valueOf(value).equals("5.0"));

        value = channel.readValue(OFFSET + 14000l);
        Assert.assertTrue(String.valueOf(value).equals("10.0"));
    }

    @Test
    public void testReadNextValueStart() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        value = channel.readValue(OFFSET + 5000l);
        Assert.assertTrue(String.valueOf(value).equals("5.0"));
    }

    @Test
    public void testReadNextValueEndNoRewind() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET + 20000);
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(OFFSET + 25000);
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        // timestamp before the last one, but rewind is false so timestamp is not considered and old value is returned
        try {
            value = channel.readValue(OFFSET);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

    @Test
    public void testReadNextValueEndWithRewind() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, true, timestamps);

        value = channel.readValue(OFFSET + 20000);
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(OFFSET + 25000);
        Assert.assertTrue(String.valueOf(value).equals("20.0"));

        value = channel.readValue(OFFSET);
        Assert.assertTrue(String.valueOf(value).equals("0.0"));
    }

    @Test
    public void testReadT1BeforeT2Valid() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, false, timestamps);

        try {
            value = channel.readValue(OFFSET - 5000l);
            Assert.assertTrue(false);
        } catch (NoValueReceivedYetException e) {
            Assert.assertTrue(true);
        }

        value = channel.readValue(OFFSET);
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

    }

    @Test
    public void testReadT1ValidT2BeforeDisabledRewind() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, false, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(OFFSET - 5000);
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testReadT1ValidT2BeforeEnabledRewind() throws CsvException {

        CsvChannelImplUnixtimestamp channel = new CsvChannelImplUnixtimestamp(data, true, timestamps);

        value = channel.readValue(OFFSET);
        Assert.assertTrue(String.valueOf(value).equals("0.0"));

        // sampling jumed back before first timestamp of file
        try {
            value = channel.readValue(OFFSET - 5000);
            Assert.assertTrue(false);
        } catch (TimeTravelException e) {
            Assert.assertTrue(true);
        }

    }

}
