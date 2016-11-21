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
package org.openmuc.framework.driver.dlms;

import java.io.IOException;
import java.net.UnknownHostException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jdlms.client.ClientConnectionSettings;
import org.openmuc.jdlms.client.IClientConnection;
import org.openmuc.jdlms.client.IClientConnectionFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DlmsDriver implements DriverService {
    private final static Logger logger = LoggerFactory.getLogger(DlmsDriver.class);

    private final IClientConnectionFactory connectionFactory = new OsgiClientConnectionFactory();
    private final AddressParser addressParser = new AddressParser();

    private final static DriverInfo info = new DriverInfo("dlms", // id
            // description
            "This is a driver to communicate with smart meter over the IEC 62056 DLMS/COSEM protocol.",
            // device address
            "N.A.",
            // parameters
            "N.A",
            // channel address
            "N.A",
            // device scan settings
            "N.A");

    public DlmsDriver() {
        logger.debug("DLMS Driver instantiated. Expecting rxtxserial.so in: " + System.getProperty("java.library.path")
                + " for serial (HDLC) connections.");
    }

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        throw new UnsupportedOperationException();

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ConnectionException, ArgumentSyntaxException {

        SettingsHelper settingsHelper = new SettingsHelper(settings);

        IClientConnection connection;
        try {
            ClientConnectionSettings<?> params = addressParser.parse(deviceAddress, settingsHelper);
            connection = connectionFactory.createClientConnection(params);
        } catch (UnknownHostException uhEx) {
            throw new ConnectionException("Device " + deviceAddress + " not found");
        } catch (IOException ioEx) {
            throw new ConnectionException("Cannot create connection object. Reason: " + ioEx);
        }

        logger.debug("Connecting to device:" + deviceAddress);
        try {
            connection.connect(DlmsConnection.timeout, settingsHelper.getPassword());
        } catch (IOException ex) {
            throw new ConnectionException(ex.getMessage());
        }
        logger.debug("Connected to device: " + deviceAddress);

        DlmsConnection handle = new DlmsConnection(connection, settingsHelper);

        return handle;
    }

}
