package org.openmuc.framework.driver.csv.test;

import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.settings.DeviceScanSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceScanSettingsTest {

    private final static Logger logger = LoggerFactory.getLogger(DeviceScanSettingsTest.class);

    // Tests expected to be OK

    String dir = System.getProperty("user.dir");

    @Test
    public void testArgumentCorrectEndingWithSlash() throws ArgumentSyntaxException {
        String settings = "path=" + dir + "/src/test/resources";
        DeviceScanSettings scanSettings = new DeviceScanSettings(settings);
    }

    @Test
    public void testArgumentCorrectendingWithoutSlash() throws ArgumentSyntaxException {
        String settings = "path=" + dir + "/src/test/resources/";
        DeviceScanSettings scanSettings = new DeviceScanSettings(settings);
    }

    // Tests expected to FAIL

    @Test(expected = ArgumentSyntaxException.class)
    public void testArgumentsNull() throws ArgumentSyntaxException {
        String arguments = null;
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testArgumentsEmptyString() throws ArgumentSyntaxException {
        String arguments = "";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testWrongArgument() throws ArgumentSyntaxException {
        String arguments = "paaaaath";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testArgumentIncomplete1() throws ArgumentSyntaxException {
        String arguments = "path";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testArgumentIncomplete2() throws ArgumentSyntaxException {
        String arguments = "path=";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testWrongArgumentPathDoesNotExist() throws ArgumentSyntaxException {
        String arguments = "path=/home/does_not_exist";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

    @Test(expected = ArgumentSyntaxException.class)
    public void testWrongArgumentNoDirctory() throws ArgumentSyntaxException {
        String arguments = "path=/home/mmittels/git/openmuc/projects/driver/csv/resources/CsvTestDevice_1.csv";
        DeviceScanSettings scanSettings = new DeviceScanSettings(arguments);
    }

}
