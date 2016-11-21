/*
 * Copyright 2011-16 Fraunhofer ISE
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
package org.openmuc.framework.driver.rest;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;

@Component
public class RestDriverImpl implements DriverService {

    // private final static Logger logger = LoggerFactory.getLogger(RestDriverImpl.class);

    private final static int timeout = 10000;

    private final static String ID = "rest";
    private final static String DESCRIPTION = "Driver to connect this OpenMUC instance with another, remote OpenMUC instance with rest.";
    private final static String DEVICE_ADDRESS = "https://adress:port or http://adress:port";
    private final static String SETTINGS = "username:password";
    private final static String CHANNEL_ADDRESS = "/rest/channels/channelid";
    private final static String DEVICE_SCAN_SETTINGS = "N.A.";

    private final static DriverInfo info = new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, SETTINGS, CHANNEL_ADDRESS,
            DEVICE_SCAN_SETTINGS);

    public RestDriverImpl() {
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        RestConnection connection;

        String HTTP = "http://";
        String HTTPS = "https://";

        if (settings == null || settings.isEmpty() || settings.trim().isEmpty() || !settings.contains(":")) {
            throw new ArgumentSyntaxException("Invalid User Credentials provided in settings: " + settings
                    + ". Expected Format: username:password");
        }
        if (deviceAddress == null || deviceAddress.isEmpty() || deviceAddress.trim().isEmpty()
                || !deviceAddress.contains(":")) {
            throw new ArgumentSyntaxException("Invalid address or port: " + deviceAddress
                    + ". Expected Format: https://adress:port or http://adress:port");
        }
        else if (deviceAddress.startsWith(HTTP) || deviceAddress.startsWith(HTTPS)) {
            connection = new RestConnection(deviceAddress, settings, timeout);
            connection.connect();
            return connection;
        }
        else {
            throw new ConnectionException("Invalid address or port: " + deviceAddress
                    + ". Expected Format: https://adress:port or http://adress:port");
        }

    }

    @Override
    public DriverInfo getInfo() {

        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException {

        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {

        throw new UnsupportedOperationException();
    }

}
