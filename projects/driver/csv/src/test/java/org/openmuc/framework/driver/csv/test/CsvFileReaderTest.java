package org.openmuc.framework.driver.csv.test;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.framework.driver.csv.CsvFileReader;
import org.openmuc.framework.driver.spi.ConnectionException;

public class CsvFileReaderTest {

    String dir = System.getProperty("user.dir");

    @Test
    public void test() {

        String fileName = dir + "/src/test/resources/SmartHomeTest.csv";

        try {
            CsvFileReader.readCsvFile(fileName);
            Assert.assertTrue(true);
        } catch (ConnectionException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

    }

}
