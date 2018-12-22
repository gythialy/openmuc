/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.driver.iec60870.settings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceAddress extends GenericSetting {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAddress.class);

    protected int common_address = 1;
    protected InetAddress host_address = null;
    protected int port = 2404;

    protected static enum Option implements OptionI {
        COMMON_ADDRESS("ca", Integer.class, false),
        PORT("p", Integer.class, false),
        HOST_ADDRESS("h", InetAddress.class, false);

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

    public DeviceAddress(String deviceAddress) throws ArgumentSyntaxException {
        int addressLength = parseFields(deviceAddress, Option.class);

        if (addressLength == 0) {
            logger.info(MessageFormat.format(
                    "No device address setted in configuration, default values will be used: host address = localhost; port = {0}",
                    port));
        }
        if (host_address == null) {
            try {
                host_address = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new ArgumentSyntaxException("Could not set default host address: localhost");
            }
        }
    }

    /**
     * The common address of device
     * 
     * @return common address as integer
     */
    public int commonAddress() {
        return common_address;
    }

    /**
     * IP host address of device
     * 
     * @return the host address
     */
    public InetAddress hostAddress() {
        return host_address;
    }

    /**
     * TCP port of device
     * 
     * @return the port as integer
     */
    public int port() {
        return port;
    }

}
