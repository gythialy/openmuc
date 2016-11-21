package org.openmuc.framework.driver.csv.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.ESampleMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSettings extends GenericSetting {

    private final static Logger logger = LoggerFactory.getLogger(DeviceSettings.class);

    protected String samplingmode = "";
    protected String rewind = "false";

    private ESampleMode samplingModeParam;
    private boolean rewindParam = false;

    public static enum Option implements OptionI {
        SAMPLINGMODE("samplingmode", String.class, true),
        REWIND("rewind", String.class, false);

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

    public DeviceSettings(String deviceScanSettings) throws ArgumentSyntaxException {

        // sollte super aufrufen, der fields parsed

        int addressLength = parseFields(deviceScanSettings, Option.class);

        if (addressLength == 0) {
            logger.info("No Sampling mode given");
        }

        try {
            samplingModeParam = ESampleMode.valueOf(samplingmode.toUpperCase());
        } catch (Exception e) {
            throw new ArgumentSyntaxException("wrong sampling mode");
        }

        try {
            rewindParam = Boolean.parseBoolean(rewind);
        } catch (Exception e) {
            throw new ArgumentSyntaxException("wrong rewind parameter syntax");
        }

    }

    public ESampleMode samplingMode() {
        return samplingModeParam;
    }

    public boolean rewind() {
        return rewindParam;
    }

}
