package org.openmuc.framework.driver.csv.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.CsvDeviceConnection;
import org.openmuc.framework.driver.csv.test.helper.CsvChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvDriverReadHHMMSSTest {

    private final static Logger logger = LoggerFactory.getLogger(CsvDriverReadHHMMSSTest.class);

    static String dir = System.getProperty("user.dir");
    static CsvDeviceConnection connection;

    @BeforeClass
    public static void initTest() throws ConnectionException, ArgumentSyntaxException {
        String deviceAddress = dir + "/src/test/resources/SmartHomeTest.csv";
        String deviceSettings = "samplingmode=hhmmss";
        connection = new CsvDeviceConnection(deviceAddress, deviceSettings);
    }

    @Test
    public void testRead() {

        List<ChannelRecordContainer> containers = new ArrayList<ChannelRecordContainer>();
        containers.add(new CsvChannelRecordContainer("hhmmss"));
        containers.add(new CsvChannelRecordContainer("grid_power"));

        try {
            connection.read(containers, null, null);

            for (ChannelRecordContainer container : containers) {
                System.out.println("##### " + container.getChannelAddress() + " " + container.getRecord().getValue());
            }

        } catch (UnsupportedOperationException | ConnectionException e) {
            e.printStackTrace();
        }

    }

}
