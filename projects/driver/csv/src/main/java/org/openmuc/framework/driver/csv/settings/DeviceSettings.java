/*
 * Copyright 2011-2021 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.csv.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.csv.ESamplingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSettings extends GenericSetting {

    private static final Logger logger = LoggerFactory.getLogger(DeviceSettings.class);

    protected String samplingmode = "";
    protected String rewind = "false";

    private ESamplingMode samplingModeParam;
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

        int addressLength = parseFields(deviceScanSettings, Option.class);

        if (addressLength == 0) {
            logger.info("No Sampling mode given");
        }

        try {
            samplingModeParam = ESamplingMode.valueOf(samplingmode.toUpperCase());
        } catch (Exception e) {
            throw new ArgumentSyntaxException("wrong sampling mode");
        }

        try {
            rewindParam = Boolean.parseBoolean(rewind);
        } catch (Exception e) {
            throw new ArgumentSyntaxException("wrong rewind parameter syntax");
        }

    }

    public ESamplingMode samplingMode() {
        return samplingModeParam;
    }

    public boolean rewind() {
        return rewindParam;
    }

}
