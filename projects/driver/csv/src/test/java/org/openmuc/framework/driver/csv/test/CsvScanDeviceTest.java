package org.openmuc.framework.driver.csv.test;

import java.util.List;

import org.junit.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.csv.CsvDeviceConnection;
import org.openmuc.framework.driver.csv.CsvDriver;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvScanDeviceTest {

    private static final Logger logger = LoggerFactory.getLogger(CsvScanDeviceTest.class);

    String dir = System.getProperty("user.dir");

    @Test
    public void testDeviceScan()
            throws ArgumentSyntaxException, UnsupportedOperationException, ScanException, ScanInterruptedException {
        CsvDriver csvDriver = new CsvDriver();

        String settings = "path=" + dir + "/src/test/resources";

        csvDriver.scanForDevices(settings, new DriverDeviceScanListener() {

            @Override
            public void scanProgressUpdate(int progress) {
                logger.info("Scan progress: " + progress + " %");
            }

            @Override
            public void deviceFound(DeviceScanInfo scanInfo) {
                logger.info(scanInfo.toString());
            }
        });
    }

    @Test
    public void testChannelScan() {
        CsvDriver csvDriver = new CsvDriver();

        String deviceAddress = dir + "/src/test/resources/SmartHomeTest.csv";

        try {

            String settings = "SAMPLINGMODE=hhmmss";
            CsvDeviceConnection csvConnection = (CsvDeviceConnection) csvDriver.connect(deviceAddress, settings);

            List<ChannelScanInfo> channelsScanInfos = csvConnection.scanForChannels("");

            for (ChannelScanInfo info : channelsScanInfos) {
                logger.info("Channel: " + info.getChannelAddress());
            }

        } catch (ArgumentSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ScanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
