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

package org.openmuc.framework.driver.iec61850;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.ClientSap;
import org.openmuc.openiec61850.ServerModel;
import org.openmuc.openiec61850.ServiceError;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class Iec61850Driver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(Iec61850Driver.class);

    private final static DriverInfo info = new DriverInfo("iec61850", // id
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

        String[] deviceAddresses = deviceAddress.split(":");

        if (deviceAddresses.length < 1 || deviceAddresses.length > 2) {
            throw new ArgumentSyntaxException("Invalid device address syntax.");
        }

        String remoteHost = deviceAddresses[0];
        InetAddress address;
        try {
            address = InetAddress.getByName(remoteHost);
        } catch (UnknownHostException e) {
            throw new ConnectionException("Unknown host: " + remoteHost, e);
        }

        int remotePort = 102;
        if (deviceAddresses.length == 2) {
            try {
                remotePort = Integer.parseInt(deviceAddresses[1]);
            } catch (NumberFormatException e) {
                throw new ArgumentSyntaxException("The specified port is not an integer");
            }
        }

        ClientSap clientSap = new ClientSap();

        String authentication = null;

        if (!settings.isEmpty()) {
            String[] args = settings.split("\\s+", 0);
            if (args.length > 6) {
                throw new ArgumentSyntaxException(
                        "Less than one or more than four arguments in the settings are not allowed.");
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-a")) {
                    i++;
                    if (i == args.length) {
                        throw new ArgumentSyntaxException(
                                "No authentication parameter was specified after the -a parameter");
                    }
                    authentication = args[i];
                }
                else if (args[i].equals("-lt")) {

                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        clientSap.setTSelLocal(new byte[0]);
                    }
                    else {
                        i++;
                        byte[] tSelLocal = new byte[args[i].length()];
                        for (int j = 0; j < args[i].length(); j++) {
                            tSelLocal[j] = (byte) args[i].charAt(j);
                        }
                        clientSap.setTSelLocal(tSelLocal);
                    }
                }
                else if (args[i].equals("-rt")) {

                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        clientSap.setTSelRemote(new byte[0]);
                    }
                    else {
                        i++;
                        byte[] tSelRemote = new byte[args[i].length()];
                        for (int j = 0; j < args[i].length(); j++) {
                            tSelRemote[j] = (byte) args[i].charAt(j);
                        }
                        clientSap.setTSelRemote(tSelRemote);
                    }
                }
                else {
                    throw new ArgumentSyntaxException("Unexpected argument: " + args[i]);
                }
            }
        }

        ClientAssociation clientAssociation;
        try {
            clientAssociation = clientSap.associate(address, remotePort, authentication, null);
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
