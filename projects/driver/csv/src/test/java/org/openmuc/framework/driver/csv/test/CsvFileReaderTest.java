package org.openmuc.framework.driver.csv.test;

import java.io.IOException;

import org.junit.Test;
import org.openmuc.framework.driver.csv.CsvFileReader;

public class CsvFileReaderTest {

    String dir = System.getProperty("user.dir");

    @Test
    public void test() {

        String fileName = dir + "/src/test/resources/SmartHomeTest.csv";

        try {
            CsvFileReader.readCsvFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
