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

package org.openmuc.framework.driver.iec61850;

import java.io.IOException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beanit.iec61850bean.ClientAssociation;
import com.beanit.iec61850bean.ClientSap;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServiceError;

@Component
public final class Iec61850Driver implements DriverService {

    private static final Logger logger = LoggerFactory.getLogger(Iec61850Driver.class);

    private static final DriverInfo info = new DriverInfo("iec61850", // id
            // description
            "This driver can be used to access IEC 61850 MMS devices",
            // device address
            "Synopsis: <host>[:<port>]\nThe default port is 102.",
            // parameters
            "Synopsis: [-a <authentication_parameter>] [-lt <local_t-selector>] [-rt <remote_t-selector>]",
            // channel address
            "Synopsis: <bda_reference>:<fc>",
            // device scan settings
            "N.A.");

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
            throws ArgumentSyntaxException, ConnectionException {

        DeviceAddress deviceAdress = new DeviceAddress(deviceAddress);
        DeviceSettings deviceSettings = new DeviceSettings(settings);

        ClientSap clientSap = new ClientSap();
        clientSap.setTSelLocal(deviceSettings.getTSelLocal());
        clientSap.setTSelLocal(deviceSettings.getTSelRemote());

        ClientAssociation clientAssociation;
        try {
            clientAssociation = clientSap.associate(deviceAdress.getAdress(), deviceAdress.getRemotePort(),
                    deviceSettings.getAuthentication(), null);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
        ServerModel serverModel;
        try {
            serverModel = clientAssociation.retrieveModel();
        } catch (ServiceError e) {
            clientAssociation.close();
            throw new ConnectionException("Service error retrieving server model" + e.getMessage(), e);
        } catch (IOException e) {
            clientAssociation.close();
            throw new ConnectionException("IOException retrieving server model: " + e.getMessage(), e);
        }

        return new Iec61850Connection(clientAssociation, serverModel);
    }
}
