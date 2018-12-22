package org.openmuc.framework.driver.csv.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.CsvDeviceConnection;
import org.openmuc.framework.driver.csv.test.helper.CsvChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamplingModeTest {

    private static final Logger logger = LoggerFactory.getLogger(SamplingModeTest.class);

    private static final String DIR = System.getProperty("user.dir");

    @Test
    public void testHHMMSSMode() {

        try {
            String deviceAddress = DIR + "/src/test/resources/SmartHomeTest.csv";
            String deviceSettings = "samplingmode=hhmmss";
            CsvDeviceConnection connection = new CsvDeviceConnection(deviceAddress, deviceSettings);

            List<ChannelRecordContainer> containers = new ArrayList<>();
            containers.add(new CsvChannelRecordContainer("hhmmss"));
            containers.add(new CsvChannelRecordContainer("power_grid"));

            connection.read(containers, null, null);

            for (ChannelRecordContainer container : containers) {
                System.out.println("##### " + container.getChannelAddress() + " " + container.getRecord().getValue());
            }

        } catch (UnsupportedOperationException | ConnectionException | ArgumentSyntaxException e) {
            logger.error("", e);
            Assert.assertTrue(false);
        }

    }

    @Test
    public void testLineMode() {

        try {
            String deviceAddress = DIR + "/src/test/resources/SmartHomeTest.csv";
            String deviceSettings = "samplingmode=line";
            CsvDeviceConnection connection = new CsvDeviceConnection(deviceAddress, deviceSettings);

            List<ChannelRecordContainer> containers = new ArrayList<>();
            containers.add(new CsvChannelRecordContainer("hhmmss"));
            containers.add(new CsvChannelRecordContainer("power_grid"));

            connection.read(containers, null, null);

            for (ChannelRecordContainer container : containers) {
                System.out.println("##### " + container.getChannelAddress() + " " + container.getRecord().getValue());
            }

        } catch (UnsupportedOperationException | ConnectionException | ArgumentSyntaxException e) {
            logger.error("", e);
            Assert.assertTrue(false);
        }

    }

}
