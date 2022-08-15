/*
 * Copyright 2011-2022 Fraunhofer ISE
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

        if (deviceScanSettings == null || deviceScanSettings.isEmpty()) {
            throw new ArgumentSyntaxException("No scan settings specified.");
        }
        else {
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
