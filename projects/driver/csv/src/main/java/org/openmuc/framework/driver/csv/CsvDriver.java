package org.openmuc.framework.driver.csv;

import java.io.File;
import java.util.Arrays;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.csv.settings.DeviceScanSettings;
import org.openmuc.framework.driver.csv.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO wo legen wir die csv dateien ab damit sie im release zugänglich sind?
//a) in treiber integrieren (die wären dann fix) (settings leer lassen?)
//b) nutzer könnten über settings anderes verzeichnis angeben

// datei mit chronologisch aufsteigenden zeitstempeln oder ohne zeitstempel
@Component
public class CsvDriver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(CsvDriver.class);

    // Settings mode realtime, nextline-rewind, nextline
    // Settings seperator = ;
    // Settings comment = #

    private final static String DEFAULT_SETTINGS = DeviceSettings.Option.SAMPLINGMODE.name() + "="
            + ESampleMode.LINE.toString();

    private final static String ID = "csv";
    private final static String DESCRIPTION = "Driver to read out csv files.";
    private final static String DEVICE_ADDRESS = "csv file path e.g. /home/usr/bin/openmuc/csv/meter.csv";
    private final static String DEVICE_SETTINGS = DeviceSettings.syntax(DeviceSettings.class) + "\n samplingmode: "
            + Arrays.toString(ESampleMode.values()).toLowerCase() + " Example: samplingmode=line;rewind=true Default: "
            + DEFAULT_SETTINGS.toLowerCase();
    private final static String CHANNEL_ADDRESS = "column header of csv file";
    private final static String DEVICE_SCAN_SETTINGS = DeviceScanSettings.syntax(DeviceScanSettings.class)
            + " path of directory containing csv files e.g: path=/home/usr/bin/openmuc/csv/.";

    private final static DriverInfo DRIVER_INFO = new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, DEVICE_SETTINGS,
            CHANNEL_ADDRESS, DEVICE_SCAN_SETTINGS);

    private boolean isDeviceScanInterrupted = false;

    @Override
    public DriverInfo getInfo() {
        return DRIVER_INFO;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        logger.info("Scan for CSV files. Settings: " + settings);

        // reset interrupted flag on start of scan
        isDeviceScanInterrupted = false;

        DeviceScanSettings deviceScanSettings = new DeviceScanSettings(settings);
        File[] listOfFiles = deviceScanSettings.path().listFiles();

        if (listOfFiles != null) {

            double numberOfFiles = listOfFiles.length;
            double fileCounter = 0;

            int idCounter = 0;

            for (File file : listOfFiles) {
                if (isDeviceScanInterrupted) {
                    break;
                }

                if (file.isFile()) {
                    if (file.getName().endsWith("csv")) {

                        String deviceId = "csv_device_" + idCounter;

                        listener.deviceFound(new DeviceScanInfo(deviceId, file.getAbsolutePath(),
                                DEFAULT_SETTINGS.toLowerCase(), file.getName()));
                    } // else: do nothing, non csv files are ignored
                } // else: do nothing, folders are ignored

                fileCounter++;
                listener.scanProgressUpdate((int) (fileCounter / numberOfFiles * 100.0));
                idCounter++;
            }
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        isDeviceScanInterrupted = true;
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        CsvDeviceConnection csvConnection = new CsvDeviceConnection(deviceAddress, settings);
        logger.debug("csv driver connected");
        return csvConnection;
    }

}
