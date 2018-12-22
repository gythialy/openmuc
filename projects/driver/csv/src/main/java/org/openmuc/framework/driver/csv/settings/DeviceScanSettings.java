package org.openmuc.framework.driver.csv.settings;

import java.io.File;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceScanSettings extends GenericSetting {

    private static final Logger logger = LoggerFactory.getLogger(DeviceScanSettings.class);

    protected String path = null;

    private File file;

    protected static enum Option implements OptionI {
        PATH("path", String.class, true);

        private String prefix;
        private Class<?> type;
        private boolean mandatory;

        private Option(String prefix, Class<?> type, boolean mandatory) {
            this.prefix = prefix;
            this.type = type;
            this.mandatory = mandatory;
        }

        @Override
        public String prefix() {
            return this.prefix;
        }

        @Override
        public Class<?> type() {
            return this.type;
        }

        @Override
        public boolean mandatory() {
            return this.mandatory;
        }
    }

    public DeviceScanSettings(String deviceScanSettings) throws ArgumentSyntaxException {

        // FIXME, ganzen null und empty abfragen sind nervig und fehleranfällig - needs refactoring.

        if (deviceScanSettings == null || deviceScanSettings.isEmpty()) {
            throw new ArgumentSyntaxException("No scan settings specified.");
        }
        else {
            // TODO braucht man das? Dirk?
            int addressLength = parseFields(deviceScanSettings, Option.class);
            if (addressLength == 0) {
                logger.info("No path given");
                throw new ArgumentSyntaxException("<path> argument not found in settings.");
            }
        }

        if (path == null) {
            throw new ArgumentSyntaxException("<path> argument not found in settings.");
        }
        else {
            if (!path.isEmpty()) {
                file = new File(path);
                if (!file.isDirectory()) {
                    throw new ArgumentSyntaxException("<path> argument must point to a directory.");
                }
            }
            else {
                throw new ArgumentSyntaxException("<path> argument must point to a directory.");
            }
        }
    }

    public File path() {
        return file;
    }

}
